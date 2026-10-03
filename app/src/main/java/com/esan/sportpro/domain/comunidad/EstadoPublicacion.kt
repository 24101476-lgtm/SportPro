package com.esan.sportpro.domain.comunidad

/**
 * Estado de una publicación (US-028, criterios 3 y 4).
 * - [PUBLICADA]: visible normalmente.
 * - [EN_REVISION]: publicación de un menor de edad con visibilidad [VisibilidadPublicacion.COMUNIDAD_ABIERTA]
 *   pendiente de aprobación del entrenador responsable, o contenido oculto automáticamente tras
 *   acumular reportes (US-030, criterio 4) mientras se revisa.
 * - [OCULTA]: ocultada por un moderador tras resolver un reporte.
 */
enum class EstadoPublicacion {
    PUBLICADA,
    EN_REVISION,
    OCULTA,
}
