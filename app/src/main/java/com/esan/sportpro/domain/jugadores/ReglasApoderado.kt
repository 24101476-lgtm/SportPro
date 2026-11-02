package com.esan.sportpro.domain.jugadores

import java.time.LocalDate

/** Estado del registro de un jugador en la academia. Se guarda en `jugadores.estadoRegistro`. */
enum class EstadoRegistro(val etiqueta: String) {
    COMPLETO("Registrado"),

    /** Menor de edad sin apoderado vinculado: el registro no está completo. */
    PENDIENTE_APODERADO("Pendiente de apoderado"),
}

/**
 * Regla de negocio (corrección de la semana 7): **un jugador menor de 18 años no queda registrado
 * hasta que tenga un apoderado vinculado** (US-007, vinculación aprobada).
 *
 * - Al guardar la ficha de un menor sin apoderado aprobado, queda en [EstadoRegistro.PENDIENTE_APODERADO].
 * - Mientras está pendiente no aparece en asistencia, convocatorias ni conteos del plantel.
 * - Al aprobar la vinculación de su primer apoderado pasa a [EstadoRegistro.COMPLETO]; si se
 *   revoca su único apoderado, vuelve a quedar pendiente.
 * - Los mayores de edad no necesitan apoderado.
 *
 * Además, la ficha de un menor exige los datos del apoderado (ver [ValidacionJugador]).
 */
object ReglasApoderado {
    /** Parentescos válidos para el apoderado de un menor. */
    val PARENTESCOS_APODERADO = listOf("Madre", "Padre", "Tutor legal", "Abuelo(a)", "Tío(a)", "Hermano(a) mayor")

    fun estadoPara(jugador: Jugador, apoderadosAprobados: Int, hoy: LocalDate = LocalDate.now()): EstadoRegistro =
        if (jugador.esMenor(hoy) && apoderadosAprobados < 1) EstadoRegistro.PENDIENTE_APODERADO else EstadoRegistro.COMPLETO

    fun pendiente(jugador: Jugador): Boolean = jugador.estadoRegistro == EstadoRegistro.PENDIENTE_APODERADO.name

    /** Puede ser convocado, marcar asistencia y contar en el plantel. */
    fun participa(jugador: Jugador): Boolean = jugador.activo && !pendiente(jugador)
}
