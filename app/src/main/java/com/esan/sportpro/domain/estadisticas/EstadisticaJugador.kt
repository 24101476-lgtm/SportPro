package com.esan.sportpro.domain.estadisticas

/**
 * US-026, criterio 2 — estadísticas acumuladas de un jugador para el periodo/filtro seleccionado.
 *
 * Cuando [sinAsignar] es true, esta fila representa eventos registrados con "Jugador no
 * identificado" (criterio 6): se contabilizan solo a nivel de equipo y se muestran aparte,
 * nunca mezclados con un jugador real.
 */
data class EstadisticaJugador(
    val jugadorId: String,
    val jugadorNombre: String,
    val partidosJugados: Int = 0,
    val minutosJugados: Int = 0,
    val goles: Int = 0,
    val asistencias: Int = 0,
    val tarjetasAmarillas: Int = 0,
    val tarjetasRojas: Int = 0,
    val faltas: Int = 0,
    /** Porcentaje 0..100. */
    val porcentajeAsistenciaEntrenamientos: Double = 0.0,
    val sinAsignar: Boolean = false,
)
