package com.esan.sportpro.domain.comunidad

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Reporte de contenido de la comunidad (US-030). Colección Firestore: "reportes".
 * Los campos de resolución (desde [resuelto] en adelante) quedan vacíos hasta que un
 * administrador resuelve el reporte desde la bandeja de moderación (criterio 6).
 */
data class Reporte(
    @DocumentId
    val id: String = "",
    val tipoContenido: TipoContenidoReportado = TipoContenidoReportado.PUBLICACION,
    val contenidoId: String = "",
    /** Para un comentario, el id de la publicación que lo contiene; permite navegar al contexto. */
    val publicacionId: String? = null,
    val motivo: MotivoReporte = MotivoReporte.OTRO,
    /** Hasta 300 caracteres, opcional. */
    val detalle: String? = null,
    val reportadoPorUid: String = "",
    val autorContenidoUid: String = "",
    @ServerTimestamp
    val fechaReporte: Date? = null,
    /**
     * true si el contenido reportado involucra a un menor de edad (criterio 7: priorizado en la
     * bandeja). Se copia de [Publicacion.autorEsMenorDeEdad] / equivalente al crear el reporte.
     */
    val involucraMenor: Boolean = false,
    val resuelto: Boolean = false,
    val accionTomada: AccionModeracion? = null,
    /** 10..300 caracteres, obligatorio al resolver (criterio 6). */
    val motivoResolucion: String? = null,
    val moderadorUid: String? = null,
    @ServerTimestamp
    val fechaResolucion: Date? = null,
)
