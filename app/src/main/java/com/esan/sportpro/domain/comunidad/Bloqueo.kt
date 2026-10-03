package com.esan.sportpro.domain.comunidad

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Bloqueo entre dos usuarios (US-030, criterio 8). Colección Firestore: "bloqueos". El id del
 * documento es `"${uidBloqueador}_${uidBloqueado}"` para poder comprobar existencia con una
 * lectura directa por id en vez de una consulta, y para evitar bloqueos duplicados.
 */
data class Bloqueo(
    @DocumentId
    val id: String = "",
    val uidBloqueador: String = "",
    val uidBloqueado: String = "",
    @ServerTimestamp
    val fecha: Date? = null,
)
