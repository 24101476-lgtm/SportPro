package com.esan.sportpro.domain

import com.esan.sportpro.domain.partido.EquipoEvento
import com.esan.sportpro.domain.partido.EstadoEvento
import com.esan.sportpro.domain.partido.EventoPartido
import com.esan.sportpro.domain.partido.JugadorConvocado
import com.esan.sportpro.domain.partido.JugadorEspecial
import com.esan.sportpro.domain.partido.TiposEvento
import com.esan.sportpro.ui.partido.ReglasAlineacionEnVivo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Once en vivo: cambios, expulsiones y marcas junto a cada carta. */
class AlineacionEnVivoTest {

    private fun evento(
        id: String,
        tipo: String,
        minuto: Int,
        jugador: String? = null,
        secundario: String? = null,
        equipo: EquipoEvento = EquipoEvento.PROPIO,
        estado: EstadoEvento = EstadoEvento.VIGENTE,
    ) = EventoPartido(
        id, id, 1, tipo, tipo, minuto, equipo, jugador, jugador, secundario, secundario, null, null,
        "uid", "op", minuto * 1000L, estado, 1, false, false, false, null, null, emptyList(),
        null, null, null, null, false,
    )

    private val posiciones = listOf(
        "Arquero", "Lateral izquierdo", "Defensa central", "Defensa central", "Lateral derecho",
        "Volante mixto", "Volante defensivo", "Volante mixto", "Extremo", "Delantero", "Extremo",
    )

    /** t1..t11 titulares, s1..s3 suplentes. */
    private val convocados =
        (1..11).map { JugadorConvocado("t$it", "Titular $it", it, titular = true, posicion = posiciones[it - 1]) } +
            (1..3).map { JugadorConvocado("s$it", "Suplente $it", 11 + it, titular = false, posicion = "Delantero") }

    private fun calcular(eventos: List<EventoPartido>) =
        ReglasAlineacionEnVivo.calcular(convocados, eventos, "4-3-3", emptyMap(), emptyMap())

    @Test
    fun sinEventos_juegaElOnceTitular() {
        val a = calcular(emptyList())
        assertEquals((1..11).map { "t$it" }.toSet(), a.enCancha.map { it.id }.toSet())
        assertEquals(3, a.banco.size)
        assertTrue(a.fuera.isEmpty())
    }

    @Test
    fun cambio_elQueEntraOcupaElLugarDelQueSale() {
        val antes = calcular(emptyList()).enCancha.first { it.id == "t10" }
        val a = calcular(listOf(evento("c", TiposEvento.CAMBIO, 60, jugador = "t10", secundario = "s1")))
        val entra = a.enCancha.first { it.id == "s1" }
        assertEquals(antes.x, entra.x)
        assertEquals(antes.y, entra.y)
        assertFalse(a.enCancha.any { it.id == "t10" })
        assertEquals(listOf("t10"), a.fuera.map { it.id })
        assertEquals(60, entra.marcas?.entroMinuto)
        assertEquals(60, a.fuera.first().marcas?.salioMinuto)
        assertEquals(2, a.banco.size)
    }

    @Test
    fun roja_dejaLaCanchaYQuedaFuera() {
        val a = calcular(listOf(evento("r", TiposEvento.TARJETA_ROJA, 30, jugador = "t3")))
        assertEquals(10, a.enCancha.size)
        assertTrue(a.fuera.single().marcas!!.roja)
    }

    @Test
    fun marcas_golesAsistenciasYTarjetas() {
        val marcas = ReglasAlineacionEnVivo.marcas(
            listOf(
                evento("g1", TiposEvento.GOL, 10, jugador = "t10", secundario = "t9"),
                evento("g2", TiposEvento.GOL, 20, jugador = "t10"),
                evento("a1", TiposEvento.TARJETA_AMARILLA, 25, jugador = "t6"),
                // Los anulados y los del rival no cuentan.
                evento("g3", TiposEvento.GOL, 30, jugador = "t10", estado = EstadoEvento.ANULADO),
                evento("g4", TiposEvento.GOL, 35, jugador = JugadorEspecial.RIVAL, equipo = EquipoEvento.RIVAL),
            ),
        )
        assertEquals(2, marcas["t10"]?.goles)
        assertEquals(1, marcas["t9"]?.asistencias)
        assertEquals(1, marcas["t6"]?.amarillas)
        assertNull(marcas[JugadorEspecial.RIVAL])
    }

    @Test
    fun lugaresMovidosAMano_tienenPrioridad() {
        val a = ReglasAlineacionEnVivo.calcular(convocados, emptyList(), "4-3-3", mapOf("t1" to (0.2f to 0.3f)), emptyMap())
        val arquero = a.enCancha.first { it.id == "t1" }
        assertEquals(0.2f, arquero.x)
        assertEquals(0.3f, arquero.y)
    }
}
