package com.esan.sportpro.data.academia

import android.net.Uri
import com.esan.sportpro.data.esperarConfirmacion
import com.esan.sportpro.data.imagenes.CarpetaImagen
import com.esan.sportpro.data.imagenes.CompresorImagen
import com.esan.sportpro.data.imagenes.ImagenesSupabase
import com.esan.sportpro.domain.academia.Equipo
import com.esan.sportpro.domain.academia.EquipoRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Equipos en la colección `equipos` (US-004/US-005). */
@Singleton
class FirestoreEquipoRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val imagenes: ImagenesSupabase,
    private val compresor: CompresorImagen,
) : EquipoRepository {

    private val coleccion get() = firestore.collection("equipos")

    override fun observarEquipos(academiaId: String): Flow<List<Equipo>> = callbackFlow {
        // Solo un filtro de igualdad: así no hace falta un índice compuesto. El orden se aplica aquí.
        val registro = coleccion.whereEqualTo("academiaId", academiaId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val lista = snapshot?.documents?.mapNotNull { it.toObject<Equipo>() }.orEmpty()
                    .sortedWith(compareBy({ it.categoria }, { it.nombre.lowercase() }))
                trySend(lista)
            }
        awaitClose { registro.remove() }
    }

    override suspend fun guardar(equipo: Equipo, esNuevo: Boolean): Boolean {
        val datos = mapOf(
            "nombre" to equipo.nombre.trim(),
            "categoria" to equipo.categoria,
            "entrenadorNombre" to equipo.entrenadorNombre.trim(),
        )
        return if (esNuevo) {
            val documento = if (equipo.id.isBlank()) coleccion.document() else coleccion.document(equipo.id)
            documento.set(
                datos + mapOf(
                    "academiaId" to equipo.academiaId,
                    "activo" to true,
                    "creadoPor" to equipo.creadoPor,
                ),
            ).esperarConfirmacion()
        } else {
            coleccion.document(equipo.id).update(datos).esperarConfirmacion()
        }
    }

    override suspend fun cambiarEstado(equipoId: String, activo: Boolean): Boolean =
        coleccion.document(equipoId).update("activo", activo).esperarConfirmacion()

    override fun nuevoId(): String = coleccion.document().id

    // Igual que la foto del jugador: comprimir, subir a Supabase, guardar la URL y borrar la anterior.
    override suspend fun subirEscudo(equipoId: String, uriLocal: String): String? = runCatching {
        val bytes = compresor.comprimir(Uri.parse(uriLocal)) ?: return null
        val url = imagenes.subir(CarpetaImagen.ESCUDOS, bytes)
        val documento = coleccion.document(equipoId)
        val anterior = runCatching { documento.get().await().getString("escudoUrl") }.getOrNull()
        documento.update("escudoUrl", url).esperarConfirmacion()
        if (!anterior.isNullOrBlank()) runCatching { imagenes.eliminar(anterior) }
        url
    }.getOrNull()
}
