package com.esan.sportpro.data.comunidad

import com.esan.sportpro.domain.comunidad.Aviso
import com.esan.sportpro.domain.comunidad.FiltroAvisos
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Acceso a la colección "avisos" (US-029).
 *
 * Firestore solo admite condiciones de rango (`<`, `>`) sobre un único campo por consulta, y aquí
 * el buscador combina filtros de categoría, posición, rango de fechas y distrito a la vez
 * (criterio 6). Para no depender de un índice compuesto distinto por cada combinación posible, la
 * consulta de red solo aplica `oculto == false` (lo mínimo para no traer contenido retirado) y el
 * resto de los filtros —incluida la expiración automática del criterio 8— se aplican en memoria
 * sobre la lista ya descargada mediante [FiltroAvisos.coincideCon].
 */
@Singleton
class AvisoRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    private val coleccion get() = firestore.collection("avisos")

    fun observarAvisos(filtro: FiltroAvisos): Flow<List<Aviso>> = callbackFlow {
        val registro = coleccion
            .whereEqualTo("oculto", false)
            .orderBy("fechaPrueba", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val ahora = Date()
                val lista = snapshot?.documents
                    ?.mapNotNull { it.toObject<Aviso>() }
                    ?.filterNot { it.expiradoRespectoDe(ahora) }
                    ?.filter { coincideConFiltro(it, filtro) }
                    .orEmpty()
                trySend(lista)
            }
        awaitClose { registro.remove() }
    }

    private fun coincideConFiltro(aviso: Aviso, filtro: FiltroAvisos): Boolean {
        if (filtro.categoria != null && aviso.categoria != filtro.categoria) return false
        if (filtro.posicion != null && filtro.posicion.name !in aviso.posicionesBuscadas) return false
        if (filtro.distrito != null && !aviso.ubicacionDistrito.equals(filtro.distrito, ignoreCase = true)) return false
        val fechaPrueba = aviso.fechaPrueba?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()
        if (filtro.fechaDesde != null && (fechaPrueba == null || fechaPrueba.isBefore(filtro.fechaDesde))) return false
        if (filtro.fechaHasta != null && (fechaPrueba == null || fechaPrueba.isAfter(filtro.fechaHasta))) return false
        return true
    }

    /**
     * Publica un aviso. [academiaVerificada] debe venir ya resuelto por el llamador (criterio 5):
     * ver TODO en [Aviso] sobre reemplazar esta verificación por el módulo de Academia real.
     */
    suspend fun publicar(aviso: Aviso, academiaVerificada: Boolean): String {
        require(academiaVerificada) { "Solo academias verificadas pueden publicar avisos" }
        require(aviso.titulo.length in 5..80) { "El título debe tener entre 5 y 80 caracteres" }
        require(aviso.fechaPrueba != null && aviso.fechaPrueba.after(Date())) { "La fecha debe ser posterior a hoy" }
        val documento = coleccion.add(aviso).await()
        return documento.id
    }
}
