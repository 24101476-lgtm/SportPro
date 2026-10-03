package com.esan.sportpro.domain.comunidad

/** US-030, criterio 5: duraciones disponibles al suspender una cuenta. */
enum class DuracionSuspension(val dias: Int, val etiqueta: String) {
    SIETE_DIAS(7, "7 días"),
    QUINCE_DIAS(15, "15 días"),
    TREINTA_DIAS(30, "30 días"),
}
