package com.esan.sportpro.data.comunidad

import android.net.Uri
import com.esan.sportpro.domain.comunidad.Comentario
import com.esan.sportpro.domain.comunidad.EstadoPublicacion
import com.esan.sportpro.domain.comunidad.Publicacion
import com.esan.sportpro.domain.comunidad.TipoReaccion
import com.esan.sportpro.domain.comunidad.VisibilidadPublicacion
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Tamaño de página del muro (US-028, criterio 5: "carga de 10 en 10"). */
const val TAMANO_PAGINA_MURO = 10

/**
 * Acceso a la colección "publicaciones" y su subcolección "comentarios" (US-028).
 *
 * El muro mezcla dos fuentes porque una publicación es visible si (a) pertenece a la academia del
 * usuario, sea cual sea su visibilidad, o (b) es de [VisibilidadPublicacion.COMUNIDAD_ABIERTA] de
 * cualquier academia (criterio: "conectar con otros equipos, academias y jugadores"). Firestore no
 * admite un OR directo entre un filtro de igualdad y uno distinto sobre dos campos en una sola
 * consulta, así que se combinan dos listeners y se deduplica por id.
 */
@Singleton
class PublicacionRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
) {
    private val coleccion get() = firestore.collection("publicaciones")
    private fun comentarios(publicacionId: String) = coleccion.document(publicacionId).collection("comentarios")

    /**
     * Primera página del muro (tiempo real). Para "cargar más" se usa [cargarSiguientePagina],
     * que es una consulta puntual (no realtime) paginada con `startAfter`, como es habitual en
     * Firestore para scroll infinito.
     */
    fun observarPrimeraPagina(academiaId: String, uidsBloqueados: List<String>): Flow<List<Publicacion>> = callbackFlow {
        val acumulado = mutableMapOf<String, Publicacion>()

        fun emitir() {
            val visibles = acumulado.values
                .filter { !it.eliminada && it.autorUid !in uidsBloqueados }
                .sortedByDescending { it.fechaPublicacion?.time ?: 0L }
                .take(TAMANO_PAGINA_MURO)
            trySend(visibles)
        }

        val deMiAcademia = coleccion
            .whereEqualTo("academiaId", academiaId)
            .orderBy("fechaPublicacion", Query.Direction.DESCENDING)
            .limit(TAMANO_PAGINA_MURO.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                snapshot?.documents?.mapNotNull { it.toObject<Publicacion>() }?.forEach { acumulado[it.id] = it }
                emitir()
            }
        val abiertas = coleccion
            .whereEqualTo("visibilidad", VisibilidadPublicacion.COMUNIDAD_ABIERTA.name)
            .orderBy("fechaPublicacion", Query.Direction.DESCENDING)
            .limit(TAMANO_PAGINA_MURO.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                snapshot?.documents?.mapNotNull { it.toObject<Publicacion>() }?.forEach { acumulado[it.id] = it }
                emitir()
            }
        awaitClose { deMiAcademia.remove(); abiertas.remove() }
    }

    /** Página siguiente a partir de la última publicación visible (scroll infinito, no realtime). */
    suspend fun cargarSiguientePagina(
        academiaId: String,
        ultima: Publicacion,
        uidsBloqueados: List<String>,
    ): List<Publicacion> {
        val fecha = ultima.fechaPublicacion ?: return emptyList()
        val deMiAcademia = coleccion
            .whereEqualTo("academiaId", academiaId)
            .orderBy("fechaPublicacion", Query.Direction.DESCENDING)
            .startAfter(fecha)
            .limit(TAMANO_PAGINA_MURO.toLong())
            .get().await().documents.mapNotNull { it.toObject<Publicacion>() }
        val abiertas = coleccion
            .whereEqualTo("visibilidad", VisibilidadPublicacion.COMUNIDAD_ABIERTA.name)
            .orderBy("fechaPublicacion", Query.Direction.DESCENDING)
            .startAfter(fecha)
            .limit(TAMANO_PAGINA_MURO.toLong())
            .get().await().documents.mapNotNull { it.toObject<Publicacion>() }
        return (deMiAcademia + abiertas)
            .distinctBy { it.id }
            .filter { !it.eliminada && it.autorUid !in uidsBloqueados }
            .sortedByDescending { it.fechaPublicacion?.time ?: 0L }
            .take(TAMANO_PAGINA_MURO)
    }

    /**
     * Sube hasta 3 imágenes a Cloud Storage (`comunidad/publicaciones/{uuid}.jpg`) y publica el
     * texto. Si una imagen falla, se mantiene el texto y se omite esa imagen (criterio 10):
     * [ResultadoPublicarPost.imagenesFallidas] le dice a la pantalla cuántas no se pudieron subir.
     */
    suspend fun publicar(borrador: Publicacion, imagenes: List<Uri>): ResultadoPublicarPost {
        require(borrador.texto.length in 1..1000) { "El texto debe tener entre 1 y 1000 caracteres" }
        require(imagenes.size <= 3) { "Máximo 3 imágenes" }

        val urlsSubidas = mutableListOf<String>()
        var fallidas = 0
        for (uri in imagenes) {
            runCatching {
                val referencia = storage.reference.child("comunidad/publicaciones/${UUID.randomUUID()}.jpg")
                referencia.putFile(uri).await()
                referencia.downloadUrl.await().toString()
            }.onSuccess { urlsSubidas.add(it) }.onFailure { fallidas++ }
        }

        // US-028, criterio 3: publicación de menor de edad no puede ser "comunidad abierta" y
        // queda en revisión hasta que el entrenador la apruebe.
        val requiereRevision = borrador.autorEsMenorDeEdad && borrador.visibilidad == VisibilidadPublicacion.COMUNIDAD_ABIERTA
        val aPublicar = borrador.copy(
            imagenesUrls = urlsSubidas,
            estado = if (requiereRevision) EstadoPublicacion.EN_REVISION else EstadoPublicacion.PUBLICADA,
        )
        val documento = coleccion.add(aPublicar).await()
        return ResultadoPublicarPost(publicacionId = documento.id, imagenesFallidas = fallidas, quedoEnRevision = requiereRevision)
    }

    /** US-028, criterio 6: editable solo dentro de los primeros 15 minutos de publicado. */
    suspend fun editar(publicacion: Publicacion, nuevoTexto: String): Boolean {
        val fecha = publicacion.fechaPublicacion ?: return false
        if (Duration.between(fecha.toInstant(), Instant.now()).toMinutes() > 15 || publicacion.eliminada) return false
        require(nuevoTexto.length in 1..1000) { "El texto debe tener entre 1 y 1000 caracteres" }
        coleccion.document(publicacion.id).update(mapOf("texto" to nuevoTexto, "editado" to true)).await()
        return true
    }

    /** Eliminación lógica; los comentarios de la subcolección dejan de mostrarse junto con ella (criterio 6). */
    suspend fun eliminar(publicacionId: String) {
        coleccion.document(publicacionId).update("eliminada", true).await()
    }

    fun observarComentarios(publicacionId: String): Flow<List<Comentario>> = callbackFlow {
        val registro = comentarios(publicacionId)
            .orderBy("fechaCreacion", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val lista = snapshot?.documents
                    ?.mapNotNull { it.toObject<Comentario>() }
                    ?.filter { !it.eliminado }
                    .orEmpty()
                trySend(lista)
            }
        awaitClose { registro.remove() }
    }

    suspend fun comentar(comentario: Comentario) {
        require(comentario.texto.length in 1..300) { "El comentario debe tener entre 1 y 300 caracteres" }
        comentarios(comentario.publicacionId).add(comentario).await()
        coleccion.document(comentario.publicacionId).update("comentariosCount", FieldValue.increment(1)).await()
    }

    /** `tipo == null` retira la reacción del usuario (criterio 4: "puede cambiarse o retirarse"). */
    suspend fun reaccionar(publicacionId: String, uid: String, tipo: TipoReaccion?) {
        val campo = "reaccionesPorUsuario.$uid"
        if (tipo == null) {
            coleccion.document(publicacionId).update(campo, FieldValue.delete()).await()
        } else {
            coleccion.document(publicacionId).update(campo, tipo.name).await()
        }
    }

    /**
     * US-028, criterio 7: si el texto contiene un patrón de 9 dígitos (celular peruano) se debe
     * advertir antes de publicar. No distingue separadores (espacios o guiones) dentro del número.
     */
    fun contieneNumeroTelefonico(texto: String): Boolean {
        val soloDigitos = texto.replace(Regex("[\\s-]"), "")
        return Regex("(?<!\\d)9\\d{8}(?!\\d)").containsMatchIn(soloDigitos)
    }
}

data class ResultadoPublicarPost(
    val publicacionId: String,
    val imagenesFallidas: Int,
    val quedoEnRevision: Boolean,
)
