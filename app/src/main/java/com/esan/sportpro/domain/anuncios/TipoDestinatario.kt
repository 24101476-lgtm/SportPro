package com.esan.sportpro.domain.anuncios

/** US-027, criterio 3. */
enum class TipoDestinatario(val etiqueta: String) {
    TODO_EL_CLUB("Todo el club"),
    EQUIPO_ESPECIFICO("Un equipo específico"),
    SOLO_ENTRENADORES("Solo entrenadores"),
    SOLO_JUGADORES("Solo jugadores"),
    SOLO_PADRES("Solo padres de familia"),
}
