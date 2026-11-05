package com.esan.sportpro.domain.partido

import com.esan.sportpro.navigation.UserRole

/** Cifras de un equipo en la pestaña Estadísticas del seguimiento (US-021, criterio 8). */
data class EstadisticaEquipo(
    /** Eventos de juego atribuidos al equipo; base de la posesión de eventos. */
    val eventosJuego: Int,
    /** Porcentaje de eventos de juego del equipo; null si todavía no hay eventos de juego. */
    val posesionPct: Int?,
    val tiros: Int,
    val faltas: Int,
    val amarillas: Int,
    val rojas: Int,
)

data class EstadisticasPartido(
    val propio: EstadisticaEquipo,
    val rival: EstadisticaEquipo,
)

/** Situación de un convocado propio dentro del partido, para la pestaña Alineación. */
enum class SituacionJugador(val etiqueta: String) {
    EN_CANCHA("En cancha"),
    SUSTITUIDO("Sustituido"),
    EXPULSADO("Expulsado"),
    INGRESO("Ingresó"),
    EN_BANCA("En banca"),
}

data class JugadorEnAlineacion(
    val jugador: JugadorConvocado,
    val situacion: SituacionJugador,
)

/**
 * Reglas de la vista del espectador (US-021). Funciones puras, sin Firebase ni Android, para que
 * se prueben con pruebas unitarias.
 */
object ReglasSeguimiento {

    /** Eventos que no cuentan como "juego" para la posesión de eventos. */
    private val EVENTOS_FUERA_DE_JUEGO = setOf(
        TiposEvento.INICIO_PARTIDO,
        TiposEvento.FIN_PRIMER_TIEMPO,
        TiposEvento.INICIO_SEGUNDO_TIEMPO,
        TiposEvento.FIN_PARTIDO,
        TiposEvento.CAMBIO,
    )

    /**
     * Administrador y entrenador siguen cualquier partido de su academia. Jugador y padre solo los
     * de los equipos de [equiposPermitidos] (el del jugador, o los de sus hijos con vínculo
     * aprobado). Un partido sin equipo (práctica antigua) no es visible para ellos.
     */
    fun puedeSeguir(rol: UserRole, partido: Partido, equiposPermitidos: Set<String>?): Boolean = when (rol) {
        UserRole.ADMINISTRADOR, UserRole.ENTRENADOR -> true
        UserRole.JUGADOR, UserRole.PADRE_DE_FAMILIA -> {
            val equipoId = partido.equipoId
            equipoId != null && equiposPermitidos?.contains(equipoId) == true
        }
    }

    fun partidosVisibles(partidos: List<Partido>, rol: UserRole, equiposPermitidos: Set<String>?): List<Partido> =
        partidos.filter { puedeSeguir(rol, it, equiposPermitidos) }

    /**
     * Cronología del espectador (criterios 1 y 4): solo eventos vigentes, de más antiguo a más
     * reciente. Los anulados y las versiones reemplazadas desaparecen y el marcador, que sale de
     * los mismos eventos, se recalcula solo.
     */
    fun cronologiaEspectador(eventos: List<EventoPartido>): List<EventoPartido> =
        ReglasPartido.vigentes(eventos)

    /**
     * Estadísticas por equipo a partir de los eventos vigentes.
     * - Tiros: goles y penales. Un gol que nace de un penal convertido no se cuenta dos veces.
     *   El catálogo base no tiene un evento "Tiro" propio.
     * - Posesión de eventos: reparto porcentual de los eventos de juego (sin controles de tiempo
     *   ni cambios) entre ambos equipos.
     */
    fun estadisticas(eventos: List<EventoPartido>): EstadisticasPartido {
        val vigentes = ReglasPartido.vigentes(eventos)
        val propios = vigentes.filter { it.equipo == EquipoEvento.PROPIO }
        val rivales = vigentes.filter { it.equipo == EquipoEvento.RIVAL }
        val juegoPropio = propios.count { it.tipoId !in EVENTOS_FUERA_DE_JUEGO }
        val juegoRival = rivales.count { it.tipoId !in EVENTOS_FUERA_DE_JUEGO }
        val totalJuego = juegoPropio + juegoRival
        val pctPropio = if (totalJuego == 0) null else Math.round(juegoPropio * 100f / totalJuego)

        fun de(lista: List<EventoPartido>, juego: Int, pct: Int?) = EstadisticaEquipo(
            eventosJuego = juego,
            posesionPct = pct,
            tiros = lista.count { it.tipoId == TiposEvento.PENAL || (it.tipoId == TiposEvento.GOL && it.eventoOrigenId == null) },
            faltas = lista.count { it.tipoId == TiposEvento.FALTA },
            amarillas = lista.count { it.tipoId == TiposEvento.TARJETA_AMARILLA },
            rojas = lista.count { it.tipoId == TiposEvento.TARJETA_ROJA },
        )

        return EstadisticasPartido(
            propio = de(propios, juegoPropio, pctPropio),
            rival = de(rivales, juegoRival, pctPropio?.let { 100 - it }),
        )
    }

    /** Titulares primero y luego suplentes, cada uno con su situación según cambios y rojas vigentes. */
    fun alineacion(convocados: List<JugadorConvocado>, eventos: List<EventoPartido>): List<JugadorEnAlineacion> {
        fun real(id: String?): String? = id?.takeUnless { JugadorEspecial.esEspecial(it) }

        val propios = ReglasPartido.vigentes(eventos).filter { it.equipo == EquipoEvento.PROPIO }
        val cambios = propios.filter { it.tipoId == TiposEvento.CAMBIO }
        val expulsados = propios.filter { it.tipoId == TiposEvento.TARJETA_ROJA }.mapNotNull { real(it.jugadorId) }.toSet()
        val salieron = cambios.mapNotNull { real(it.jugadorId) }.toSet()
        val entraron = cambios.mapNotNull { real(it.jugadorSecundarioId) }.toSet()

        return convocados
            .sortedWith(
                compareByDescending<JugadorConvocado> { it.titular }
                    .thenBy { it.dorsal ?: Int.MAX_VALUE }
                    .thenBy { it.nombre },
            )
            .map { jugador ->
                val situacion = when {
                    jugador.id in expulsados -> SituacionJugador.EXPULSADO
                    jugador.titular -> if (jugador.id in salieron) SituacionJugador.SUSTITUIDO else SituacionJugador.EN_CANCHA
                    jugador.id in entraron -> if (jugador.id in salieron) SituacionJugador.SUSTITUIDO else SituacionJugador.INGRESO
                    else -> SituacionJugador.EN_BANCA
                }
                JugadorEnAlineacion(jugador, situacion)
            }
    }
}
