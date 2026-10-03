package com.esan.sportpro.domain.estadisticas

import java.time.LocalDate

/** US-026, criterio 9 — un punto del gráfico de barras de goles de los últimos 10 partidos. */
data class GolesPorPartidoPunto(
    val partidoId: String,
    val fecha: LocalDate,
    val rivalNombre: String,
    val golesEquipo: Int,
    val golesRival: Int,
)
