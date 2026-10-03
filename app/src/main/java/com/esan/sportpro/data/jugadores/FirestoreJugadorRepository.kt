package com.esan.sportpro.data.jugadores

import com.esan.sportpro.data.esperarConfirmacion
import com.esan.sportpro.domain.jugadores.Jugador
import com.esan.sportpro.domain.jugadores.JugadorRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Perfiles de jugador en la colección `jugadores` (US-006/US-007). */
@Singleton
class FirestoreJugadorRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : JugadorRepository {

    private val coleccion get() = firestore.collection("jugadores")

    override fun observarJugadores(academiaId: String): Flow<List<Jugador>> = callbackFlow {
        val registro = coleccion.whereEqualTo("academiaId", academiaId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val lista = snapshot?.documents?.mapNotNull { it.toObject<Jugador>() }.orEmpty()
                    .sortedWith(compareBy({ it.apellidos.lowercase() }, { it.nombres.lowercase() }))
                trySend(lista)
            }
        awaitClose { registro.remove() }
    }

    override suspend fun guardar(jugador: Jugador): Boolean {
        // Se escribe un mapa explícito (y no el objeto) para no persistir propiedades calculadas.
        val datos = mapOf(
            "equipoId" to jugador.equipoId,
            "equipoNombre" to jugador.equipoNombre,
            // Campo que lee el módulo de Estadísticas (StatsRepository.nombresDeJugadores).
            "nombre" to jugador.nombreCompleto,
            "nombres" to jugador.nombres.trim(),
            "apellidos" to jugador.apellidos.trim(),
            "fechaNacimiento" to jugador.fechaNacimiento.trim(),
            "posicion" to jugador.posicion,
            "dorsal" to jugador.dorsal,
            "estaturaCm" to jugador.estaturaCm,
            "pesoKg" to jugador.pesoKg,
            "telefono" to jugador.telefono.trim(),
            "correo" to jugador.correo.trim(),
            "contactoEmergenciaNombre" to jugador.contactoEmergenciaNombre.trim(),
            "contactoEmergenciaTelefono" to jugador.contactoEmergenciaTelefono.trim(),
            "contactoEmergenciaParentesco" to jugador.contactoEmergenciaParentesco.trim(),
        )
        return if (jugador.id.isBlank()) {
            coleccion.document().set(
                datos + mapOf(
                    "academiaId" to jugador.academiaId,
                    "activo" to true,
                    "creadoPor" to jugador.creadoPor,
                ),
            ).esperarConfirmacion()
        } else {
            coleccion.document(jugador.id).update(datos).esperarConfirmacion()
        }
    }

    override suspend fun cambiarEstado(jugadorId: String, activo: Boolean): Boolean =
        coleccion.document(jugadorId).update("activo", activo).esperarConfirmacion()
}
