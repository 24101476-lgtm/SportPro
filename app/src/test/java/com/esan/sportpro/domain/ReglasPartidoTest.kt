package com.esan.sportpro.domain

import com.esan.sportpro.domain.partido.BorradorEvento
import com.esan.sportpro.domain.partido.CatalogoBase
import com.esan.sportpro.domain.partido.EquipoEvento
import com.esan.sportpro.domain.partido.EstadoEvento
import com.esan.sportpro.domain.partido.EstadoPartido
import com.esan.sportpro.domain.partido.EventoPartido
import com.esan.sportpro.domain.partido.JugadorConvocado
import com.esan.sportpro.domain.partido.JugadorEspecial
import com.esan.sportpro.domain.partido.Partido
import com.esan.sportpro.domain.partido.ReglasPartido
import com.esan.sportpro.domain.partido.TiposEvento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Reglas del registro del partido en vivo: marcador, cambios, incompletos, duplicados y reloj. */
class ReglasPartidoTest {

    private val convocados = (1..16).map { JugadorConvocado("j$it", "Jugador $it", it, titular = it <= 11) }

    private fun evento(
        id: String,
        tipo: String,
        minuto: Int,
        equipo: EquipoEvento = EquipoEvento.PROPIO,
        jugador: String? = null,
        secundario: String? = null,
        estado: EstadoEvento = EstadoEvento.VIGENTE,
        incompleto: Boolean = false,
        registradoEn: Long = minuto * 1000L,
    ) = EventoPartido(
        id, id, 1, tipo, tipo, minuto, equipo, jugador, jugador, secundario, secundario, null, null,
        "uid", "op", registradoEn, estado, 1, incompleto, false, false, null, null, emptyList(),
        null, null, null, null, false,
    )

    private fun partido(inicio2: Long? = null) = Partido(
        "p", "A", "B", null, EstadoPartido.EN_CURSO, true, 5, 45, 0L, inicio2, false, null, null, null, null,
    )

    @Test
    fun golAnulado_noCuentaEnElMarcador() {
        val eventos = listOf(
            evento("a", TiposEvento.GOL, 10, jugador = "j9"),
            evento("b", TiposEvento.GOL, 20, EquipoEvento.RIVAL, JugadorEspecial.RIVAL),
            evento("c", TiposEvento.GOL, 30, jugador = "j9", estado = EstadoEvento.ANULADO),
        )
        assertEquals(ReglasPartido.Marcador(1, 1), ReglasPartido.marcador(eventos, CatalogoBase.tipos))
    }

    @Test
    fun cambioYRoja_actualizanJugadoresEnCancha() {
        val eventos = listOf(
            evento("a", TiposEvento.CAMBIO, 50, jugador = "j9", secundario = "j12"),
            evento("b", TiposEvento.TARJETA_ROJA, 60, jugador = "j5"),
        )
        val cancha = ReglasPartido.enCancha(convocados, eventos)
        assertEquals(10, cancha.size)
        assertTrue("j12" in cancha)
        assertFalse("j9" in cancha)
        assertFalse("j5" in cancha)
    }

    @Test
    fun cambio_validaQueSalgaUnJugadorEnCancha() {
        val tipo = CatalogoBase.porId(TiposEvento.CAMBIO)!!
        val eventos = listOf(evento("a", TiposEvento.CAMBIO, 50, jugador = "j9", secundario = "j12"))
        val borrador = BorradorEvento(tipo, 70, EquipoEvento.PROPIO, "j9", "J9", "j13", "J13", null, null)
        assertNotNull(ReglasPartido.validar(borrador, convocados, eventos))
        assertNull(ReglasPartido.validar(borrador.copy(jugadorId = "j1"), convocados, eventos))
    }

    @Test
    fun minutoFueraDeRango_esInvalido() {
        val tipo = CatalogoBase.porId(TiposEvento.FALTA)!!
        val borrador = BorradorEvento(tipo, 140, EquipoEvento.PROPIO, "j1", "J1", null, null, null, null)
        assertEquals("Minuto inválido", ReglasPartido.validar(borrador, convocados, emptyList()))
    }

    @Test
    fun jugadorNoIdentificado_dejaElEventoIncompleto() {
        val tipo = CatalogoBase.porId(TiposEvento.FALTA)!!
        val borrador = BorradorEvento(
            tipo, 5, EquipoEvento.PROPIO, JugadorEspecial.NO_IDENTIFICADO, null, null, null, null, null,
        )
        assertTrue(ReglasPartido.esIncompleto(borrador))
    }

    @Test
    fun mismoTipoMinutoYJugador_seMarcaComoDuplicado() {
        val eventos = listOf(
            evento("a", TiposEvento.GOL, 12, jugador = "j9"),
            evento("b", TiposEvento.GOL, 12, jugador = "j9"),
        )
        assertEquals(1, ReglasPartido.duplicados(eventos).size)
    }

    @Test
    fun segundoTiempo_cuentaTiempoAnadidoDespuesDel90() {
        val inicio2 = 1_000_000L
        val reloj = ReglasPartido.reloj(partido(inicio2), inicio2 + 48 * 60_000L)
        assertEquals(93, reloj.minuto)
        assertEquals(3, reloj.adicion)
        assertFalse(reloj.tiempoCumplido)
    }

    @Test
    fun segundoTiempo_seCierraSoloAlAgotarElTiempoAnadido() {
        val inicio2 = 1_000_000L
        val reloj = ReglasPartido.reloj(partido(inicio2), inicio2 + 90 * 60_000L)
        assertEquals(90 + ReglasPartido.ADICION_MAX, reloj.minuto)
        assertTrue(reloj.tiempoCumplido)
    }

    @Test
    fun primerTiempo_cuentaTiempoAnadidoHasta15Minutos() {
        val reloj = ReglasPartido.reloj(partido(), 70 * 60_000L)
        assertEquals(15, reloj.adicion)
        assertFalse(reloj.tiempoCumplido)
    }

    // ------------------------------------------------------------ Tiempo añadido del 1.er tiempo

    /** Caso real: el 1.er tiempo terminó en 45+15' (minuto 60) y el 2.º empezó en 45'. */
    private val partidoConAnadido = listOf(
        evento("ini", TiposEvento.INICIO_PARTIDO, 0, registradoEn = 0),
        evento("gol1", TiposEvento.GOL, 52, jugador = "j9", registradoEn = 3_000),
        evento("fin1", TiposEvento.FIN_PRIMER_TIEMPO, 60, registradoEn = 3_600),
        evento("ini2", TiposEvento.INICIO_SEGUNDO_TIEMPO, 45, registradoEn = 4_500),
        evento("gol2", TiposEvento.GOL, 50, jugador = "j9", registradoEn = 4_800),
    )

    @Test
    fun tiempoAnadidoDelPrimerTiempo_quedaAntesDelSegundoTiempo() {
        val orden = ReglasPartido.cronologia(partidoConAnadido).map { it.id }
        assertEquals(listOf("ini", "gol1", "fin1", "ini2", "gol2"), orden)
    }

    @Test
    fun eventoSinPeriodoGuardado_seDeducePorLaHoraDeRegistro() {
        val gol1 = partidoConAnadido.first { it.id == "gol1" }
        val gol2 = partidoConAnadido.first { it.id == "gol2" }
        assertEquals(1, ReglasPartido.periodoDe(gol1, partidoConAnadido))
        assertEquals(2, ReglasPartido.periodoDe(gol2, partidoConAnadido))
        // Si se guardó el tiempo, se respeta.
        assertEquals(1, ReglasPartido.periodoDe(gol2.copy(periodo = 1), partidoConAnadido))
    }

    @Test
    fun minutoEnTiempoAnadido_seMuestraComoLoAnotaElArbitro() {
        assertEquals("45+15'", ReglasPartido.textoMinuto(60, 1, 45))
        assertEquals("45'", ReglasPartido.textoMinuto(45, 2, 45))
        assertEquals("60'", ReglasPartido.textoMinuto(60, 2, 45))
        assertEquals("90+3'", ReglasPartido.textoMinuto(93, 2, 45))
    }

    @Test
    fun inicioDelSegundoTiempo_noEsFueraDeOrdenAunqueElPrimeroTuvieraAnadido() {
        val primerTiempo = partidoConAnadido.take(3)
        assertFalse(ReglasPartido.esFueraDeOrden(45, 2, primerTiempo))
        // Dentro del mismo tiempo sí se detecta.
        assertTrue(ReglasPartido.esFueraDeOrden(40, 1, primerTiempo))
        assertTrue(ReglasPartido.esFueraDeOrden(47, 2, partidoConAnadido))
    }

    @Test
    fun minutoDelSegundoTiempo_noPuedeSerMenorQueSuInicio() {
        assertNotNull(ReglasPartido.validarMinutoPeriodo(30, 2, 45))
        assertNull(ReglasPartido.validarMinutoPeriodo(45, 2, 45))
        assertNull(ReglasPartido.validarMinutoPeriodo(60, 1, 45))
        assertNotNull(ReglasPartido.validarMinutoPeriodo(61, 1, 45))
    }

    @Test
    fun periodoParaRegistro_sigueAlEstadoDelPartido() {
        assertEquals(1, ReglasPartido.periodoParaRegistro(TiposEvento.GOL, partido(), emptyList()))
        assertEquals(2, ReglasPartido.periodoParaRegistro(TiposEvento.GOL, partido(inicio2 = 1L), emptyList()))
        assertEquals(2, ReglasPartido.periodoParaRegistro(TiposEvento.INICIO_SEGUNDO_TIEMPO, partido(), emptyList()))
        assertEquals(1, ReglasPartido.periodoParaRegistro(TiposEvento.FIN_PRIMER_TIEMPO, partido(inicio2 = 1L), emptyList()))
    }
}
