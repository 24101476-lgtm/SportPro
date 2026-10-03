package com.esan.sportpro.domain.comunidad

import java.time.LocalDate

/** US-029, criterio 6: filtros combinables del buscador de avisos. */
data class FiltroAvisos(
    val categoria: String? = null,
    val posicion: PosicionJugador? = null,
    val fechaDesde: LocalDate? = null,
    val fechaHasta: LocalDate? = null,
    val distrito: String? = null,
) {
    val tieneFiltrosActivos: Boolean
        get() = categoria != null || posicion != null || fechaDesde != null || fechaHasta != null || distrito != null
}
