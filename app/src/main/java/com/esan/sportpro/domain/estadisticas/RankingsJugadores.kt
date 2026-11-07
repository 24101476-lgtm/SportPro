package com.esan.sportpro.domain.estadisticas

/**
 * US-026 — Rankings y "jugador destacado" calculados sobre la tabla de estadísticas del periodo.
 * Son funciones puras (sin Android ni Firebase) para poder probarlas con pruebas unitarias.
 * La fila "Sin asignar" nunca entra en un ranking: no es un jugador real.
 */
object RankingsJugadores {

    /**
     * Puntaje del jugador destacado: gol = 3, asistencia = 2, tarjeta amarilla = -1 y roja = -3.
     * Es una regla simple y explicable, no un dato guardado.
     */
    fun puntaje(jugador: EstadisticaJugador): Int =
        jugador.goles * 3 + jugador.asistencias * 2 - jugador.tarjetasAmarillas - jugador.tarjetasRojas * 3

    /** Jugador con mayor [puntaje] (desempate: más minutos). `null` si nadie suma puntos. */
    fun destacado(jugadores: List<EstadisticaJugador>): EstadisticaJugador? =
        jugadores
            .filter { !it.sinAsignar && puntaje(it) > 0 }
            .maxWithOrNull(compareBy<EstadisticaJugador> { puntaje(it) }.thenBy { it.minutosJugados })

    /** Los [n] primeros por [valor] descendente, sin contar a quien tiene 0 ni a "Sin asignar". */
    fun top(
        jugadores: List<EstadisticaJugador>,
        n: Int = 3,
        valor: (EstadisticaJugador) -> Double,
    ): List<EstadisticaJugador> =
        jugadores
            .filter { !it.sinAsignar && valor(it) > 0.0 }
            .sortedWith(compareByDescending<EstadisticaJugador> { valor(it) }.thenBy { it.jugadorNombre.lowercase() })
            .take(n)
}
