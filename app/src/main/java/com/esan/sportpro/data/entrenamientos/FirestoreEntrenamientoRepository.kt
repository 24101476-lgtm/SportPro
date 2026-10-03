package com.esan.sportpro.data.entrenamientos

import com.esan.sportpro.data.esperarConfirmacion
import com.esan.sportpro.domain.entrenamientos.EjercicioBiblioteca
import com.esan.sportpro.domain.entrenamientos.Entrenamiento
import com.esan.sportpro.domain.entrenamientos.EntrenamientoRepository
import com.esan.sportpro.domain.entrenamientos.EstadoAsistencia
import com.esan.sportpro.domain.entrenamientos.ValidacionEntrenamiento
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/** Entrenamientos (`entrenamientos`) y biblioteca de ejercicios (`ejercicios`) — US-009 a US-013. */
@Singleton
class FirestoreEntrenamientoRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : EntrenamientoRepository {

    private val entrenamientos get() = firestore.collection("entrenamientos")
    private val biblioteca get() = firestore.collection("ejercicios")
    private val asistenciasEstadisticas get() = firestore.collection("asistencias_entrenamiento")

    override fun observarEntrenamientos(academiaId: String): Flow<List<Entrenamiento>> = callbackFlow {
        val registro = entrenamientos.whereEqualTo("academiaId", academiaId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val lista = snapshot?.documents?.mapNotNull { it.toObject<Entrenamiento>() }.orEmpty()
                    .sortedByDescending { it.clave }
                trySend(lista)
            }
        awaitClose { registro.remove() }
    }

    override suspend fun guardar(entrenamiento: Entrenamiento): Boolean {
        val datos = mapOf(
            "equipoId" to entrenamiento.equipoId,
            "equipoNombre" to entrenamiento.equipoNombre,
            "fecha" to entrenamiento.fecha.trim(),
            "hora" to entrenamiento.hora.trim(),
            "lugar" to entrenamiento.lugar.trim(),
            "objetivo" to entrenamiento.objetivo.trim(),
            "ejercicios" to entrenamiento.ejercicios.map {
                mapOf(
                    "nombre" to it.nombre.trim(),
                    "descripcion" to it.descripcion.trim(),
                    "duracionMin" to it.duracionMin,
                )
            },
        )
        return if (entrenamiento.id.isBlank()) {
            entrenamientos.document().set(
                datos + mapOf(
                    "academiaId" to entrenamiento.academiaId,
                    "asistencia" to emptyMap<String, String>(),
                    "asistenciaRegistrada" to false,
                    "creadoPor" to entrenamiento.creadoPor,
                ),
            ).esperarConfirmacion()
        } else {
            entrenamientos.document(entrenamiento.id).update(datos).esperarConfirmacion()
        }
    }

    override suspend fun registrarAsistencia(
        entrenamiento: Entrenamiento,
        asistencia: Map<String, String>,
    ): Boolean {
        val fecha = ValidacionEntrenamiento.parsearFecha(entrenamiento.fecha)
            ?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.let { Timestamp(Date.from(it)) }

        val lote = firestore.batch()
        lote.update(
            entrenamientos.document(entrenamiento.id),
            mapOf("asistencia" to asistencia, "asistenciaRegistrada" to true),
        )
        asistencia.forEach { (jugadorId, nombreEstado) ->
            val estado = EstadoAsistencia.desde(nombreEstado) ?: return@forEach
            // Id determinista: registrar de nuevo la asistencia sobrescribe, no duplica.
            lote.set(
                asistenciasEstadisticas.document("${entrenamiento.id}_$jugadorId"),
                mapOf(
                    "entrenamientoId" to entrenamiento.id,
                    "equipoId" to entrenamiento.equipoId,
                    "jugadorId" to jugadorId,
                    "estado" to estado.name,
                    "presente" to estado.participo,
                    "fecha" to fecha,
                ),
            )
        }
        return lote.commit().esperarConfirmacion()
    }

    override fun observarBiblioteca(academiaId: String): Flow<List<EjercicioBiblioteca>> = callbackFlow {
        val registro = biblioteca.whereEqualTo("academiaId", academiaId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val lista = snapshot?.documents?.mapNotNull { it.toObject<EjercicioBiblioteca>() }.orEmpty()
                    .sortedBy { it.nombre.lowercase() }
                trySend(lista)
            }
        awaitClose { registro.remove() }
    }

    override suspend fun guardarEnBiblioteca(ejercicio: EjercicioBiblioteca): Boolean =
        biblioteca.document().set(
            mapOf(
                "academiaId" to ejercicio.academiaId,
                "nombre" to ejercicio.nombre.trim(),
                "descripcion" to ejercicio.descripcion.trim(),
                "duracionMin" to ejercicio.duracionMin,
            ),
        ).esperarConfirmacion()
}
