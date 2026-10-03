package com.esan.sportpro.domain.comunidad

/** US-028, criterio 2: visibilidad elegida al publicar. Por defecto [SOLO_ACADEMIA]. */
enum class VisibilidadPublicacion(val etiqueta: String) {
    SOLO_ACADEMIA("Solo mi academia"),
    COMUNIDAD_ABIERTA("Comunidad abierta"),
}
