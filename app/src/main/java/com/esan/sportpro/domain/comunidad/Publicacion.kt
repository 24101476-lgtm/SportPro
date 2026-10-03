package com.esan.sportpro.domain.comunidad

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Publicación del muro de comunidad (US-028). Colección Firestore: "publicaciones".
 * Los comentarios viven en la subcolección "publicaciones/{id}/comentarios" (ver [Comentario]),
 * para poder paginarlos y ocultarlos en bloque cuando se elimina la publicación (criterio 6)
 * sin tener que reescribir un arreglo potencialmente grande en el documento padre.
 *
 * TODO(US-004/005, academia): [autorEsMenorDeEdad] hoy se completa a mano por el cliente al
 * publicar porque el perfil de cuentas (con la fecha de nacimiento) todavía no existe; cuando
 * exista, calcular este valor desde el perfil real del autor en vez de confiar en el cliente.
 */
data class Publicacion(
    @DocumentId
    val id: String = "",
    val academiaId: String = "",
    val autorUid: String = "",
    val autorNombre: String = "",
    val autorEsMenorDeEdad: Boolean = false,
    /** 1..1000 caracteres (criterio de aceptación 1). */
    val texto: String = "",
    /** Hasta 3 URLs de descarga de Cloud Storage, máx. 5 MB cada imagen (criterio 1). */
    val imagenesUrls: List<String> = emptyList(),
    val visibilidad: VisibilidadPublicacion = VisibilidadPublicacion.SOLO_ACADEMIA,
    val estado: EstadoPublicacion = EstadoPublicacion.PUBLICADA,
    @ServerTimestamp
    val fechaPublicacion: Date? = null,
    /** true si se editó dentro de los primeros 15 minutos (criterio 6). */
    val editado: Boolean = false,
    /** Eliminación lógica: al eliminarla se ocultan también sus comentarios (criterio 6). */
    val eliminada: Boolean = false,
    /** uid -> nombre de [TipoReaccion], una entrada por usuario (criterio 4). */
    val reaccionesPorUsuario: Map<String, String> = emptyMap(),
    /** Contador desnormalizado, actualizado por el repositorio al agregar/quitar comentarios. */
    val comentariosCount: Int = 0,
    /** Contador desnormalizado de reportes recibidos (US-030, criterio 3). Oculta automáticamente al llegar a 3. */
    val reportesCount: Int = 0,
) {
    val reaccionesContadas: Map<TipoReaccion, Int>
        get() = reaccionesPorUsuario.values
            .mapNotNull { nombre -> runCatching { TipoReaccion.valueOf(nombre) }.getOrNull() }
            .groupingBy { it }
            .eachCount()
}
