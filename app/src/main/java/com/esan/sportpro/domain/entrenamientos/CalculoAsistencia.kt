package com.esan.sportpro.domain.entrenamientos

/** Historial de participación de un jugador en los entrenamientos con asistencia registrada. */
data class ResumenAsistencia(
    val registrados: Int,
    val presentes: Int,
    val tardes: Int,
    val justificados: Int,
    val ausentes: Int,
) {
    /** Entrenamientos a los que el jugador asistió (presente o tarde). */
    val participaciones: Int get() = presentes + tardes

    /** Porcentaje de participación redondeado; 0 si todavía no hay sesiones registradas. */
    val porcentaje: Int
        get() = if (registrados == 0) 0 else Math.round(participaciones * 100f / registrados)
}

/** Cálculo del historial de asistencia a partir de las sesiones. Sin dependencias de Android. */
object CalculoAsistencia {

    /**
     * Solo cuentan las sesiones con asistencia registrada en las que el jugador figura en la lista;
     * una sesión anterior a su ingreso al equipo no lo penaliza.
     */
    fun resumen(jugadorId: String, entrenamientos: List<Entrenamiento>): ResumenAsistencia {
        val estados = entrenamientos
            .filter { it.asistenciaRegistrada }
            .mapNotNull { EstadoAsistencia.desde(it.asistencia[jugadorId]) }
        return ResumenAsistencia(
            registrados = estados.size,
            presentes = estados.count { it == EstadoAsistencia.PRESENTE },
            tardes = estados.count { it == EstadoAsistencia.TARDE },
            justificados = estados.count { it == EstadoAsistencia.JUSTIFICADO },
            ausentes = estados.count { it == EstadoAsistencia.AUSENTE },
        )
    }
}
