package com.esan.sportpro.domain.estadisticas

/** US-026, criterio 5 — columnas por las que se puede ordenar la tabla de jugadores. */
enum class ColumnaEstadisticaJugador(val etiqueta: String) {
    JUGADOR("Jugador"),
    PARTIDOS("PJ"),
    MINUTOS("Min"),
    GOLES("Goles"),
    ASISTENCIAS("Asist."),
    TARJETAS_AMARILLAS("TA"),
    TARJETAS_ROJAS("TR"),
    FALTAS("Faltas"),
    ASISTENCIA_ENTRENAMIENTOS("% Entreno"),
}
