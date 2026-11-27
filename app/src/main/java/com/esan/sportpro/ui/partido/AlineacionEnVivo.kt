package com.esan.sportpro.ui.partido

import com.esan.sportpro.domain.partido.EquipoEvento
import com.esan.sportpro.domain.partido.EventoPartido
import com.esan.sportpro.domain.partido.JugadorConvocado
import com.esan.sportpro.domain.partido.JugadorEspecial
import com.esan.sportpro.domain.partido.ReglasPartido
import com.esan.sportpro.domain.partido.ResultadoPenal
import com.esan.sportpro.domain.partido.TiposEvento

/**
 * Lo que lleva un jugador en el partido en vivo; se muestra junto a su carta.
 * [entroMinuto] / [salioMinuto] vienen de los cambios.
 */
data class MarcasEnVivo(
    val goles: Int = 0,
    val asistencias: Int = 0,
    val amarillas: Int = 0,
    val roja: Boolean = false,
    val entroMinuto: Int? = null,
    val salioMinuto: Int? = null,
) {
    val vacia: Boolean
        get() = goles == 0 && asistencias == 0 && amarillas == 0 && !roja && entroMinuto == null && salioMinuto == null
}

/** El equipo propio en vivo: quién está en cancha (con su lugar), el banco y quién ya salió. */
data class AlineacionEnVivo(
    val enCancha: List<JugadorCanchaUi>,
    val banco: List<JugadorCanchaUi>,
    /** Sustituidos y expulsados: ya no vuelven a la cancha. */
    val fuera: List<JugadorCanchaUi>,
)

/**
 * Cálculos de la alineación en vivo, sin dependencias de Android para poder probarlos.
 * Solo cuentan los eventos vigentes del equipo propio, en orden cronológico.
 */
object ReglasAlineacionEnVivo {

    fun marcas(eventos: List<EventoPartido>): Map<String, MarcasEnVivo> {
        val mapa = HashMap<String, MarcasEnVivo>()
        fun cambiar(id: String?, cambio: MarcasEnVivo.() -> MarcasEnVivo) {
            if (id.isNullOrBlank() || JugadorEspecial.esEspecial(id)) return
            mapa[id] = (mapa[id] ?: MarcasEnVivo()).cambio()
        }
        propiosEnOrden(eventos).forEach { e ->
            when (e.tipoId) {
                TiposEvento.GOL -> {
                    cambiar(e.jugadorId) { copy(goles = goles + 1) }
                    cambiar(e.jugadorSecundarioId) { copy(asistencias = asistencias + 1) }
                }
                TiposEvento.PENAL ->
                    if (e.resultadoPenal == ResultadoPenal.CONVERTIDO) cambiar(e.jugadorId) { copy(goles = goles + 1) }
                TiposEvento.TARJETA_AMARILLA -> cambiar(e.jugadorId) { copy(amarillas = amarillas + 1) }
                TiposEvento.TARJETA_ROJA -> cambiar(e.jugadorId) { copy(roja = true) }
                TiposEvento.CAMBIO -> {
                    cambiar(e.jugadorId) { copy(salioMinuto = e.minuto) }
                    cambiar(e.jugadorSecundarioId) { copy(entroMinuto = e.minuto) }
                }
            }
        }
        return mapa
    }

    /**
     * Arma el once actual: los titulares parten de [formacion]; en cada cambio el que entra ocupa
     * el lugar del que salió, y el expulsado deja la cancha. [manuales] son los lugares que el
     * usuario movió a mano en la pizarra en vivo y tienen prioridad.
     */
    fun calcular(
        convocados: List<JugadorConvocado>,
        eventos: List<EventoPartido>,
        formacion: String,
        manuales: Map<String, Pair<Float, Float>>,
        fotos: Map<String, String>,
    ): AlineacionEnVivo {
        val marcas = marcas(eventos)
        val porId = convocados.associateBy { it.id }
        fun ui(j: JugadorConvocado, titular: Boolean, x: Float = 0.5f, y: Float = 0.5f) = JugadorCanchaUi(
            id = j.id,
            nombre = j.nombre,
            dorsal = j.dorsal,
            posicion = j.posicion,
            titular = titular,
            x = x,
            y = y,
            fotoUrl = fotos[j.id].orEmpty(),
            marcas = marcas[j.id]?.takeUnless { it.vacia },
        )

        val titulares = convocados.filter { it.titular }.map { ui(it, true) }
        val lugares = LinkedHashMap<String, Pair<Float, Float>>()
        distribuirEnFormacion(formacion, titulares).forEach { lugares[it.id] = it.x to it.y }

        propiosEnOrden(eventos).forEach { e ->
            when (e.tipoId) {
                TiposEvento.CAMBIO -> {
                    val lugar = e.jugadorId?.let { lugares.remove(it) } ?: (0.5f to 0.5f)
                    e.jugadorSecundarioId
                        ?.takeIf { !JugadorEspecial.esEspecial(it) && it in porId }
                        ?.let { lugares[it] = lugar }
                }
                TiposEvento.TARJETA_ROJA -> e.jugadorId?.let { lugares.remove(it) }
            }
        }
        // Coincide con la regla del registro de eventos (quién puede salir en un cambio).
        val idsEnCancha = ReglasPartido.enCancha(convocados, eventos)

        val enCancha = lugares.filterKeys { it in idsEnCancha }.mapNotNull { (id, lugar) ->
            val (x, y) = manuales[id] ?: lugar
            porId[id]?.let { ui(it, true, x, y) }
        }
        val disponibles = ReglasPartido.suplentesDisponibles(convocados, eventos).map { it.id }.toSet()
        val banco = convocados.filter { it.id in disponibles }.map { ui(it, false) }
        val fuera = convocados
            .filter { it.id !in idsEnCancha && it.id !in disponibles }
            .filter { marcas[it.id]?.let { m -> m.salioMinuto != null || m.roja } == true }
            .map { ui(it, false) }
        return AlineacionEnVivo(enCancha, banco, fuera)
    }

    private fun propiosEnOrden(eventos: List<EventoPartido>): List<EventoPartido> {
        val vigentes = ReglasPartido.vigentes(eventos).filter { it.equipo != EquipoEvento.RIVAL }.toSet()
        return ReglasPartido.cronologia(eventos).filter { it in vigentes }
    }
}
