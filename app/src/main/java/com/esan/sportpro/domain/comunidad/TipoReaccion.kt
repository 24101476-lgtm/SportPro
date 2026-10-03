package com.esan.sportpro.domain.comunidad

/** US-028, criterio 4: una reacción por usuario, que puede cambiarse o retirarse. */
enum class TipoReaccion(val etiqueta: String, val emoji: String) {
    ME_GUSTA("Me gusta", "👍"),
    APLAUSOS("Aplausos", "👏"),
    FUERZA("Fuerza", "💪"),
}
