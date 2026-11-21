package com.esan.sportpro.data.mensualidades

import com.esan.sportpro.domain.mensualidades.EstadoMensualidad
import com.esan.sportpro.domain.mensualidades.Mensualidad
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Acceso a la colección "mensualidades" (US-008). Firestore ya tiene persistencia offline
 * habilitada a nivel de app ([com.esan.sportpro.di.FirebaseModule]), por lo que los listeners
 * de este repositorio siguen funcionando sin conexión con la última caché local.
 *
 * Registro **únicamente simulado**: no se integra pasarela de pago ni se procesan cobros reales.
 */
@Singleton
class MensualidadRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    private val coleccion get() = firestore.collection("mensualidades")

    /** Grilla del mes: solo registros vigentes (no anulados) de la academia, con filtros opcionales. */
    fun observarGrilla(
        academiaId: String,
        mes: Int,
        anio: Int,
        equipoId: String? = null,
        estado: EstadoMensualidad? = null,
    ): Flow<List<Mensualidad>> = callbackFlow {
        var query: Query = coleccion
            .whereEqualTo("academiaId", academiaId)
            .whereEqualTo("mes", mes)
            .whereEqualTo("anio", anio)
            .whereEqualTo("vigente", true)
        if (equipoId != null) {
            query = query.whereEqualTo("equipoId", equipoId)
        }
        if (estado != null) {
            // El filtro por "Vencido" se aplica en memoria porque ese estado se calcula en
            // pantalla (ver EstadoMensualidad.calcular) y nunca se persiste como tal.
            query = if (estado == EstadoMensualidad.VENCIDO) query else query.whereEqualTo("estadoRegistrado", estado.name)
        }
        val registro = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val lista = snapshot?.documents?.mapNotNull { it.toObject<Mensualidad>() }.orEmpty()
            trySend(lista)
        }
        awaitClose { registro.remove() }
    }

    /**
     * Grilla del entrenador: una consulta por cada equipo que dirige. Las reglas solo le dejan leer
     * las mensualidades de sus equipos y lo comprueban con el filtro de igualdad por `equipoId`.
     */
    fun observarGrillaDeEquipos(
        academiaId: String,
        equipoIds: List<String>,
        mes: Int,
        anio: Int,
    ): Flow<List<Mensualidad>> {
        if (equipoIds.isEmpty()) return flowOf(emptyList())
        return combine(equipoIds.distinct().map { observarGrilla(academiaId, mes, anio, equipoId = it) }) { listas ->
            listas.toList().flatten().distinctBy { it.id }
        }
    }

    /**
     * Mensualidades vigentes de los hijos de un padre. Una consulta por jugador (antes un `whereIn`):
     * las reglas comprueban el vínculo aprobado de cada `jugadorId`, y eso solo se puede verificar
     * con un filtro de igualdad.
     */
    fun observarMensualidadesDeJugadores(jugadorIds: List<String>): Flow<List<Mensualidad>> {
        if (jugadorIds.isEmpty()) return flowOf(emptyList())
        return combine(jugadorIds.distinct().map { observarVigentesDeJugador(it) }) { listas ->
            listas.toList().flatten()
        }
    }

    private fun observarVigentesDeJugador(jugadorId: String): Flow<List<Mensualidad>> = callbackFlow {
        val registro = coleccion
            .whereEqualTo("jugadorId", jugadorId)
            .whereEqualTo("vigente", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val lista = snapshot?.documents?.mapNotNull { it.toObject<Mensualidad>() }.orEmpty()
                trySend(lista)
            }
        awaitClose { registro.remove() }
    }

    /**
     * Historial completo (vigentes y anulados) de un jugador, más reciente primero.
     * [academiaId] lo necesitan el administrador y el entrenador: sus reglas comparan la academia.
     */
    fun observarHistorialDeJugador(jugadorId: String, academiaId: String? = null): Flow<List<Mensualidad>> = callbackFlow {
        var query: Query = coleccion.whereEqualTo("jugadorId", jugadorId)
        if (academiaId != null) query = query.whereEqualTo("academiaId", academiaId)
        val registro = query
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                // Se ordena en memoria: un orderBy junto al filtro por jugador exige un índice
                // compuesto en Firestore y, sin él, la consulta falla y cerraba la pantalla.
                val lista = snapshot?.documents?.mapNotNull { it.toObject<Mensualidad>() }.orEmpty()
                    .sortedByDescending { it.fechaRegistro }
                trySend(lista)
            }
        awaitClose { registro.remove() }
    }

    /**
     * Registra un pago simulado. Si ya existe un registro vigente para el mismo jugador/mes/año,
     * este se marca `vigente = false` y el nuevo documento queda enlazado mediante
     * [Mensualidad.anulaRegistroId] — nunca se elimina el anterior (criterio de aceptación 6).
     */
    /**
     * @return `true` si el servidor confirmó; `false` si quedó guardado en el dispositivo y se
     * sincronizará al volver la conexión (antes la pantalla se quedaba esperando sin fin).
     */
    suspend fun registrarPago(nuevo: Mensualidad, uidAdministrador: String): Boolean {
        require(nuevo.montoReferencial in 0.0..5000.0) { "Monto inválido" }
        require(nuevo.estadoRegistrado != EstadoMensualidad.VENCIDO) {
            "Vencido es un estado calculado; no se persiste directamente"
        }

        val vigenteAnterior = coleccion
            .whereEqualTo("academiaId", nuevo.academiaId)
            .whereEqualTo("jugadorId", nuevo.jugadorId)
            .whereEqualTo("mes", nuevo.mes)
            .whereEqualTo("anio", nuevo.anio)
            .whereEqualTo("vigente", true)
            .get()
            .await()
            .documents
            .firstOrNull()

        val aRegistrar = nuevo.copy(
            registradoPorUid = uidAdministrador,
            anulaRegistroId = vigenteAnterior?.id,
            vigente = true,
        )

        // Anular el anterior y crear el nuevo van en un mismo lote: o se aplican ambos o ninguno,
        // así nunca quedan dos registros vigentes ni un mes sin registro vigente.
        val lote = firestore.batch()
        vigenteAnterior?.let { lote.update(it.reference, "vigente", false) }
        lote.set(coleccion.document(), aRegistrar)
        val confirmacion = lote.commit()

        // Sin conexión Firestore aplica el lote localmente pero no confirma: no se bloquea la pantalla.
        return withTimeoutOrNull(ESPERA_CONFIRMACION_MS) { confirmacion.await(); true } ?: false
    }

    private companion object {
        const val ESPERA_CONFIRMACION_MS = 6_000L
    }
}
