package com.esan.sportpro.domain.estadisticas

import java.time.LocalDate

/** US-026, criterio 4 — Frame 124_Estadisticas_Filtros: filtros combinables de la pantalla. */
data class FiltroEstadisticas(
    val temporada: String? = null,
    val tipoPartido: TipoPartido? = null,
    val fechaInicio: LocalDate? = null,
    val fechaFin: LocalDate? = null,
) {
    val tieneFiltrosActivos: Boolean
        get() = temporada != null || tipoPartido != null || fechaInicio != null || fechaFin != null
}
