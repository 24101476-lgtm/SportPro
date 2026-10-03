package com.esan.sportpro.data.mensualidades

import com.esan.sportpro.domain.mensualidades.EstadoMensualidad
import com.esan.sportpro.domain.mensualidades.Mensualidad
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
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

    /** Mensualidades (todas, incluidas las ya vinculadas a jugadores del padre) vigentes de una lista de jugadores. */
    fun observarMensualidadesDeJugadores(jugadorIds: List<String>): Flow<List<Mensualidad>> = callbackFlow {
        if (jugadorIds.isEmpty()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        // Firestore limita whereIn a 30 valores; suficiente para los hijos vinculados a un padre.
        val registro = coleccion
            .whereIn("jugadorId", jugadorIds.take(30))
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

    /** Historial completo (vigentes y anulados) de un jugador, más reciente primero. */
    fun observarHistorialDeJugador(jugadorId: String): Flow<List<Mensualidad>> = callbackFlow {
        val registro = coleccion
            .whereEqualTo("jugadorId", jugadorId)
            .orderBy("fechaRegistro", Query.Direction.DESCENDING)
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
     * Registra un pago simulado. Si ya existe un registro vigente para el mismo jugador/mes/año,
     * este se marca `vigente = false` y el nuevo documento queda enlazado mediante
     * [Mensualidad.anulaRegistroId] — nunca se elimina el anterior (criterio de aceptación 6).
     */
    suspend fun registrarPago(nuevo: Mensualidad, uidAdministrador: String) {
        require(nuevo.montoReferencial in 0.0..5000.0) { "Monto inválido" }
        require(nuevo.estadoRegistrado != EstadoMensualidad.VENCIDO) {
            "Vencido es un estado calculado; no se persiste directamente"
        }

        val vigenteAnterior = coleccion
            .whereEqualTo("jugadorId", nuevo.jugadorId)
            .whereEqualTo("mes", nuevo.mes)
            .whereEqualTo("anio", nuevo.anio)
            .whereEqualTo("vigente", true)
            .get()
            .await()
            .documents
            .firstOrNull()

        if (vigenteAnterior != null) {
            vigenteAnterior.reference.update("vigente", false).await()
        }

        val aRegistrar = nuevo.copy(
            registradoPorUid = uidAdministrador,
            anulaRegistroId = vigenteAnterior?.id,
            vigente = true,
        )
        coleccion.add(aRegistrar).await()
    }
}
