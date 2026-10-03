package com.esan.sportpro.domain.estadisticas

/**
 * US-026, criterio 3 — resumen agregado del equipo para el periodo/filtro seleccionado.
 * [diferenciaGol] y [promedioGolesPorPartido] se derivan, nunca se persisten.
 */
data class EstadisticaEquipo(
    val partidosJugados: Int = 0,
    val ganados: Int = 0,
    val empatados: Int = 0,
    val perdidos: Int = 0,
    val golesFavor: Int = 0,
    val golesContra: Int = 0,
) {
    val diferenciaGol: Int
        get() = golesFavor - golesContra

    val promedioGolesPorPartido: Double
        get() = if (partidosJugados > 0) golesFavor.toDouble() / partidosJugados else 0.0
}
