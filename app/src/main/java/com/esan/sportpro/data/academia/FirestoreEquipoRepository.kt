package com.esan.sportpro.data.academia

import com.esan.sportpro.data.esperarConfirmacion
import com.esan.sportpro.domain.academia.Equipo
import com.esan.sportpro.domain.academia.EquipoRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Equipos en la colección `equipos` (US-004/US-005). */
@Singleton
class FirestoreEquipoRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
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

    override suspend fun guardar(equipo: Equipo): Boolean {
        val datos = mapOf(
            "nombre" to equipo.nombre.trim(),
            "categoria" to equipo.categoria,
            "entrenadorNombre" to equipo.entrenadorNombre.trim(),
        )
        return if (equipo.id.isBlank()) {
            coleccion.document().set(
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
}
