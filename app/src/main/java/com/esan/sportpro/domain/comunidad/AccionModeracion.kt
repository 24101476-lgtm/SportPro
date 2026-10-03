package com.esan.sportpro.domain.comunidad

/** US-030, criterio 5: acciones disponibles en la bandeja de moderación. */
enum class AccionModeracion(val etiqueta: String) {
    MANTENER_PUBLICADO("Mantener publicado"),
    OCULTAR("Ocultar contenido"),
    OCULTAR_Y_ADVERTIR("Ocultar y advertir al autor"),
    OCULTAR_Y_SUSPENDER("Ocultar y suspender la cuenta"),
}
