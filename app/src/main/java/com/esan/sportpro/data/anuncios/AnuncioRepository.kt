package com.esan.sportpro.data.anuncios

import com.esan.sportpro.domain.anuncios.Anuncio
import com.esan.sportpro.domain.anuncios.PrioridadAnuncio
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Resultado de publicar un anuncio (US-027, criterio de aceptación 10). */
data class ResultadoPublicacion(val anuncioId: String, val notificacionEnviada: Boolean)

/**
 * Acceso a la colección "anuncios" (US-027). Al publicar, el push real lo envía la Edge Function
 * `notificar` (Supabase) por FCM; si no se pudo enviar, [publicar] lo reporta con
 * `notificacionEnviada = false` para que la pantalla muestre el aviso del criterio 10.
 */
@Singleton
class AnuncioRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val notificador: com.esan.sportpro.data.notificaciones.NotificadorPush,
) {
    private val coleccion get() = firestore.collection("anuncios")
    private val preferencias get() = firestore.collection("preferencias_notificaciones")

    fun observarAnuncios(academiaId: String, incluirArchivados: Boolean = false): Flow<List<Anuncio>> = callbackFlow {
        val registro = coleccion
            .whereEqualTo("academiaId", academiaId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val lista = snapshot?.documents
                    ?.mapNotNull { it.toObject<Anuncio>() }
                    ?.filter { incluirArchivados || !it.archivado }
                    ?.sortedByDescending { it.fechaPublicacion }
                    .orEmpty()
                trySend(lista)
            }
        awaitClose { registro.remove() }
    }

    /** US-027, criterio 5: anuncios urgentes que este usuario aún no marcó como leídos. */
    fun observarAnunciosUrgentesNoLeidos(academiaId: String, uid: String): Flow<List<Anuncio>> = callbackFlow {
        val registro = coleccion
            .whereEqualTo("academiaId", academiaId)
            .whereEqualTo("prioridad", PrioridadAnuncio.URGENTE.name)
            .whereEqualTo("archivado", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val lista = snapshot?.documents
                    ?.mapNotNull { it.toObject<Anuncio>() }
                    ?.filter { uid !in it.lecturas }
                    .orEmpty()
                trySend(lista)
            }
        awaitClose { registro.remove() }
    }

    suspend fun publicar(anuncio: Anuncio): ResultadoPublicacion {
        require(anuncio.titulo.length in 5..80) { "El título debe tener entre 5 y 80 caracteres" }
        require(anuncio.mensaje.length in 10..500) { "El mensaje debe tener entre 10 y 500 caracteres" }

        val documento = coleccion.add(anuncio).await()
        // El push lo envía el servidor al tema de los destinatarios (academia, rol o equipo).
        val enviada = notificador.notificarAnuncio(documento.id)
        if (!enviada) runCatching { documento.update("envioNotificacionFallido", true).await() }
        return ResultadoPublicacion(anuncioId = documento.id, notificacionEnviada = enviada)
    }

    suspend fun marcarLeido(anuncioId: String, uid: String) {
        coleccion.document(anuncioId).update("lecturas", FieldValue.arrayUnion(uid)).await()
    }

    /** US-027, criterio 8: editable solo dentro de los primeros 15 minutos de publicado. */
    suspend fun editar(anuncio: Anuncio, nuevoTitulo: String, nuevoMensaje: String): Boolean {
        val fechaPublicacion = anuncio.fechaPublicacion ?: return false
        val minutosTranscurridos = Duration.between(fechaPublicacion.toInstant(), Instant.now()).toMinutes()
        if (minutosTranscurridos > 15 || anuncio.archivado) return false

        require(nuevoTitulo.length in 5..80) { "El título debe tener entre 5 y 80 caracteres" }
        require(nuevoMensaje.length in 10..500) { "El mensaje debe tener entre 10 y 500 caracteres" }

        coleccion.document(anuncio.id)
            .update(mapOf("titulo" to nuevoTitulo, "mensaje" to nuevoMensaje, "editado" to true))
            .await()
        return true
    }

    suspend fun archivar(anuncioId: String) {
        coleccion.document(anuncioId).update("archivado", true).await()
    }

    /** US-027, criterio 9: las notificaciones urgentes nunca pueden desactivarse (no hay setter para ellas). */
    fun observarPreferenciaInformativas(uid: String): Flow<Boolean> = callbackFlow {
        val registro = preferencias.document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            // Por defecto las notificaciones informativas están activas si el usuario no configuró nada.
            trySend(snapshot?.getBoolean("informativasActivas") ?: true)
        }
        awaitClose { registro.remove() }
    }

    suspend fun actualizarPreferenciaInformativas(uid: String, activo: Boolean) {
        preferencias.document(uid).set(mapOf("informativasActivas" to activo), SetOptions.merge()).await()
    }
}
