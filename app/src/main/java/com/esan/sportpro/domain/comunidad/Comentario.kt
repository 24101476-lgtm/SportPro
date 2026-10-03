package com.esan.sportpro.domain.comunidad

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/** Comentario de una publicación (US-028, criterio 4). Subcolección "publicaciones/{id}/comentarios". */
data class Comentario(
    @DocumentId
    val id: String = "",
    val publicacionId: String = "",
    val autorUid: String = "",
    val autorNombre: String = "",
    /** 1..300 caracteres (criterio de aceptación 4). */
    val texto: String = "",
    @ServerTimestamp
    val fechaCreacion: Date? = null,
    /** Eliminación lógica: se conserva el documento para no romper el contador del padre. */
    val eliminado: Boolean = false,
    val reportesCount: Int = 0,
)
