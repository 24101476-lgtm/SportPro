package com.esan.sportpro.domain.estadisticas

import java.time.LocalDate

/** Un partido finalizado visto desde un jugador: resultado del equipo y lo que hizo él. */
data class PartidoDeJugador(
    val partidoId: String,
    val fecha: LocalDate,
    val rivalNombre: String,
    val golesEquipo: Int,
    val golesRival: Int,
    val goles: Int,
    val asistencias: Int,
    val minutos: Int,
) {
    val resultado: Char
        get() = when {
            golesEquipo > golesRival -> 'G'
            golesEquipo < golesRival -> 'P'
            else -> 'E'
        }
}

/**
 * Datos por partido del periodo que usan las vistas gráficas de Estadísticas: el minuto de cada
 * gol del equipo (para "¿Cuándo marcamos?") y los partidos de cada jugador.
 */
data class ResumenPartidos(
    val minutosDeGol: List<Int> = emptyList(),
    val partidosPorJugador: Map<String, List<PartidoDeJugador>> = emptyMap(),
) {
    companion object {
        /** Tramos de 15 minutos; el último junta el 76' en adelante (incluye descuento). */
        val TRAMOS: List<Pair<String, IntRange>> = listOf(
            "0–15" to 0..15,
            "16–30" to 16..30,
            "31–45" to 31..45,
            "46–60" to 46..60,
            "61–75" to 61..75,
            "76–90" to 76..Int.MAX_VALUE,
        )

        fun golesPorTramo(minutos: List<Int>): List<Int> =
            TRAMOS.map { (_, rango) -> minutos.count { it in rango } }
    }
}
