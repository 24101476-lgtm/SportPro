package com.esan.sportpro.data.entrenamientos

import android.net.Uri
import com.esan.sportpro.data.esperarConfirmacion
import com.esan.sportpro.data.imagenes.CompresorImagen
import com.esan.sportpro.domain.entrenamientos.EjercicioBiblioteca
import com.esan.sportpro.domain.entrenamientos.Entrenamiento
import com.esan.sportpro.domain.entrenamientos.EntrenamientoRepository
import com.esan.sportpro.domain.entrenamientos.EstadoAsistencia
import com.esan.sportpro.domain.entrenamientos.EstadoSesion
import com.esan.sportpro.domain.entrenamientos.MarcaJugador
import com.esan.sportpro.domain.entrenamientos.RegistroHistorialAsistencia
import com.esan.sportpro.domain.entrenamientos.SesionObservada
import com.esan.sportpro.domain.entrenamientos.ResultadoGuardarEjercicio
import com.esan.sportpro.domain.entrenamientos.ValidacionEntrenamiento
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.toObject
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Entrenamientos (`entrenamientos`) y biblioteca de ejercicios (`ejercicios`) — US-009 a US-013.
 * Las imágenes de la biblioteca se guardan en Cloud Storage: `ejercicios/{ejercicioId}/imagen.jpg`.
 */
@Singleton
class FirestoreEntrenamientoRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val compresor: CompresorImagen,
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
                    "ejercicioId" to it.ejercicioId,
                )
            },
        )
        return if (entrenamiento.id.isBlank()) {
            entrenamientos.document().set(
                datos + mapOf(
                    "academiaId" to entrenamiento.academiaId,
                    "asistencia" to emptyMap<String, String>(),
                    "asistenciaRegistrada" to false,
                    "estado" to EstadoSesion.PROGRAMADA.name,
                    "creadoPor" to entrenamiento.creadoPor,
                ),
            ).esperarConfirmacion()
        } else {
            entrenamientos.document(entrenamiento.id).update(datos).esperarConfirmacion()
        }
    }

    // ---- Asistencia (US-011) --------------------------------------------------------------

    override fun observarSesion(entrenamientoId: String): Flow<SesionObservada> = callbackFlow {
        // MetadataChanges.INCLUDE: avisa también cuando el servidor confirma las escrituras
        // locales (hasPendingWrites pasa a false), aunque los datos no cambien.
        val registro = entrenamientos.document(entrenamientoId)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                trySend(
                    SesionObservada(
                        entrenamiento = if (snapshot.exists()) snapshot.toObject<Entrenamiento>() else null,
                        cambiosPendientes = snapshot.metadata.hasPendingWrites(),
                        desdeCache = snapshot.metadata.isFromCache,
                    ),
                )
            }
        awaitClose { registro.remove() }
    }

    override fun observarHistorialAsistencia(entrenamientoId: String): Flow<List<RegistroHistorialAsistencia>> =
        callbackFlow {
            val registro = historial(entrenamientoId).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.mapNotNull { it.toObject<RegistroHistorialAsistencia>() }.orEmpty())
            }
            awaitClose { registro.remove() }
        }

    override suspend fun marcarAsistencia(entrenamientoId: String, marcas: List<MarcaJugador>): Boolean {
        if (marcas.isEmpty()) return true
        val documento = entrenamientos.document(entrenamientoId)
        val ahora = System.currentTimeMillis()
        val lote = firestore.batch()
        marcas.forEach { (jugadorId, jugadorNombre, marca) ->
            val detalle = mapOf(
                "estado" to marca.estado,
                "minutosRetraso" to marca.minutosRetraso,
                "motivo" to marca.motivo,
                "marcadoPor" to marca.marcadoPor,
                "marcadoPorNombre" to marca.marcadoPorNombre,
                // Hora del servidor: decide qué marca prevalece si dos usuarios marcan a la vez.
                "marcadoEn" to FieldValue.serverTimestamp(),
            )
            // FieldPath evita problemas si el id tuviera caracteres especiales.
            lote.update(
                documento,
                FieldPath.of("asistencia", jugadorId), marca.estado,
                FieldPath.of("asistenciaDetalle", jugadorId), detalle,
            )
            lote.set(
                historial(entrenamientoId).document(),
                detalle + mapOf(
                    "jugadorId" to jugadorId,
                    "jugadorNombre" to jugadorNombre,
                    "marcadoEnDispositivo" to ahora,
                ),
            )
        }
        return lote.commit().esperarConfirmacion()
    }

    override suspend fun cerrarSesion(
        entrenamiento: Entrenamiento,
        plantelIds: List<String>,
        porcentaje: Double,
    ): Boolean {
        val fecha = ValidacionEntrenamiento.parsearFecha(entrenamiento.fecha)
            ?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.let { Timestamp(Date.from(it)) }

        val lote = firestore.batch()
        lote.update(
            entrenamientos.document(entrenamiento.id),
            mapOf(
                "estado" to EstadoSesion.REALIZADA.name,
                "asistenciaRegistrada" to true,
                "porcentajeAsistencia" to porcentaje,
                "realizadaEn" to FieldValue.serverTimestamp(),
            ),
        )
        plantelIds.forEach { jugadorId ->
            val estado = EstadoAsistencia.desde(entrenamiento.asistencia[jugadorId]) ?: return@forEach
            // Id determinista: volver a cerrar la sesión sobrescribe, no duplica.
            lote.set(
                asistenciasEstadisticas.document("${entrenamiento.id}_$jugadorId"),
                mapOf(
                    "entrenamientoId" to entrenamiento.id,
                    "equipoId" to entrenamiento.equipoId,
                    "jugadorId" to jugadorId,
                    "estado" to estado.name,
                    "presente" to estado.participo,
                    "minutosRetraso" to entrenamiento.asistenciaDetalle[jugadorId]?.minutosRetraso,
                    "fecha" to fecha,
                ),
            )
        }
        return lote.commit().esperarConfirmacion()
    }

    private fun historial(entrenamientoId: String) =
        entrenamientos.document(entrenamientoId).collection("historial_asistencia")

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

    override suspend fun guardarEnBiblioteca(
        ejercicio: EjercicioBiblioteca,
        imagenLocal: String?,
        quitarImagen: Boolean,
    ): ResultadoGuardarEjercicio {
        val nuevo = ejercicio.id.isBlank()
        // El id se genera antes para poder guardar la imagen en ejercicios/{id}/imagen.jpg.
        val documento = if (nuevo) biblioteca.document() else biblioteca.document(ejercicio.id)

        var imagenUrl = if (quitarImagen) "" else ejercicio.imagenUrl
        var imagenFallida = false
        if (imagenLocal != null) {
            val subida = subirImagen(documento.id, Uri.parse(imagenLocal))
            if (subida != null) imagenUrl = subida else imagenFallida = true
        } else if (quitarImagen && ejercicio.imagenUrl.isNotBlank()) {
            runCatching { imagenEjercicio(documento.id).delete() }
        }

        val datos = mapOf(
            "nombre" to ejercicio.nombre.trim(),
            "objetivo" to ejercicio.objetivo.trim(),
            "categoria" to ejercicio.categoria,
            "duracionMin" to ejercicio.duracionMin,
            "minJugadores" to ejercicio.minJugadores,
            "materiales" to ejercicio.materiales.trim(),
            "descripcion" to ejercicio.descripcion.trim(),
            "imagenUrl" to imagenUrl,
        )
        val confirmado = if (nuevo) {
            documento.set(
                datos + mapOf(
                    "academiaId" to ejercicio.academiaId,
                    "activo" to true,
                    "creadoPor" to ejercicio.creadoPor,
                ),
            ).esperarConfirmacion()
        } else {
            documento.update(datos).esperarConfirmacion()
        }
        return ResultadoGuardarEjercicio(confirmado = confirmado, imagenFallida = imagenFallida)
    }

    override suspend fun cambiarEstadoEjercicio(ejercicioId: String, activo: Boolean): Boolean =
        biblioteca.document(ejercicioId).update("activo", activo).esperarConfirmacion()

    override suspend fun eliminarEjercicio(ejercicio: EjercicioBiblioteca): Boolean {
        val confirmado = biblioteca.document(ejercicio.id).delete().esperarConfirmacion()
        if (ejercicio.imagenUrl.isNotBlank()) runCatching { imagenEjercicio(ejercicio.id).delete() }
        return confirmado
    }

    private fun imagenEjercicio(ejercicioId: String) = storage.reference.child("ejercicios/$ejercicioId/imagen.jpg")

    /**
     * Comprime y sube la imagen. Cloud Storage no tiene cola offline como Firestore: sin conexión
     * la subida falla (o vence el tiempo) y el ejercicio se guarda igual, sin imagen.
     * @return la URL de descarga, o `null` si no se pudo subir.
     */
    private suspend fun subirImagen(ejercicioId: String, uri: Uri): String? = runCatching {
        val bytes = compresor.comprimir(uri) ?: return null
        withTimeoutOrNull(TIEMPO_MAXIMO_SUBIDA_MS) {
            val referencia = imagenEjercicio(ejercicioId)
            referencia.putBytes(bytes).await()
            referencia.downloadUrl.await().toString()
        }
    }.getOrNull()

    private companion object {
        const val TIEMPO_MAXIMO_SUBIDA_MS = 20_000L
    }
}
