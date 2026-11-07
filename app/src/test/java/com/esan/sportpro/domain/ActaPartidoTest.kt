package com.esan.sportpro.domain

import com.esan.sportpro.domain.partido.CatalogoBase
import com.esan.sportpro.domain.partido.EquipoEvento
import com.esan.sportpro.domain.partido.EstadoEvento
import com.esan.sportpro.domain.partido.EstadoPartido
import com.esan.sportpro.domain.partido.EventoPartido
import com.esan.sportpro.domain.partido.JugadorConvocado
import com.esan.sportpro.domain.partido.JugadorEspecial
import com.esan.sportpro.domain.partido.Partido
import com.esan.sportpro.domain.partido.ReglasActa
import com.esan.sportpro.domain.partido.TiposEvento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** US-022: contenido del acta, minutos jugados y ventana de reapertura. */
class ActaPartidoTest {

    private val convocados = (1..14).map { JugadorConvocado("j$it", "Jugador $it", it, titular = it <= 11) }

    private fun evento(
        id: String,
        tipo: String,
        minuto: Int,
        equipo: EquipoEvento? = EquipoEvento.PROPIO,
        jugador: String? = null,
        secundario: String? = null,
        estado: EstadoEvento = EstadoEvento.VIGENTE,
        incompleto: Boolean = false,
        periodo: Int? = null,
        operador: String? = "Operador Uno",
        versionCatalogo: Int = 1,
        registradoEn: Long = minuto * 1000L,
    ) = EventoPartido(
        id = id, grupoId = id, version = 1, tipoId = tipo, tipoNombre = tipo, minuto = minuto,
        equipo = equipo, jugadorId = jugador, jugadorNombre = jugador,
        jugadorSecundarioId = secundario, jugadorSecundarioNombre = secundario,
        resultadoPenal = null, observacion = null, operadorUid = "uid", operadorNombre = operador,
        registradoEnMs = registradoEn, estado = estado, versionCatalogo = versionCatalogo,
        incompleto = incompleto, fueraDeOrden = false, excedeCambios = false, eventoOrigenId = null,
        motivo = null, camposModificados = emptyList(), reemplazadoPor = null, anuladoPor = null,
        anuladoPorNombre = null, anuladoEnMs = null, pendienteSincronizar = false, periodo = periodo,
    )

    private fun partido(
        estado: EstadoPartido = EstadoPartido.FINALIZADO,
        inicio: Long? = 1_000L,
        cerradaEn: Long? = null,
    ) = Partido(
        id = "p1", nombreEquipo = "Halcones", nombreRival = "Tigres", categoria = "Sub-15",
        estado = estado, alineacionConfirmada = true, cambiosPermitidos = 5, duracionTiempoMin = 45,
        inicioPrimerTiempoMs = inicio, inicioSegundoTiempoMs = null, enDescanso = false,
        operadorUid = null, entrenadorUid = null, creadoPor = null, fechaMs = null,
        actaCerradaEnMs = cerradaEn,
    )

    private fun minutosDe(id: String, eventos: List<EventoPartido>, partido: Partido = partido()) =
        ReglasActa.minutosJugados(partido, eventos, convocados).first { it.jugadorId == id }.minutos

    private val finDelPartido = evento("fin", TiposEvento.FIN_PARTIDO, 90, equipo = null, periodo = 2)

    // ------------------------------------------------------------------ Minutos jugados

    @Test
    fun titularQueNoSale_juegaTodoElPartido() {
        assertEquals(90, minutosDe("j1", listOf(finDelPartido)))
    }

    @Test
    fun suplenteNoIngresado_tieneCeroMinutos() {
        assertEquals(0, minutosDe("j12", listOf(finDelPartido)))
    }

    @Test
    fun cambio_repartaMinutosEntreQuienSaleYQuienEntra() {
        val eventos = listOf(
            evento("c", TiposEvento.CAMBIO, 60, jugador = "j1", secundario = "j12", periodo = 2),
            finDelPartido,
        )
        assertEquals(60, minutosDe("j1", eventos))
        assertEquals(30, minutosDe("j12", eventos))
    }

    @Test
    fun rojaDirecta_cortaLosMinutosDelExpulsado() {
        val eventos = listOf(
            evento("r", TiposEvento.TARJETA_ROJA, 70, jugador = "j3", periodo = 2),
            finDelPartido,
        )
        assertEquals(70, minutosDe("j3", eventos))
    }

    @Test
    fun tiempoAnadidoDelPrimerTiempo_seSumaAlSegundo() {
        val eventos = listOf(
            evento("f1", TiposEvento.FIN_PRIMER_TIEMPO, 48, equipo = null, periodo = 1),
            evento("c", TiposEvento.CAMBIO, 45, jugador = "j2", secundario = "j13", periodo = 2),
            finDelPartido,
        )
        assertEquals(93, minutosDe("j1", eventos))
        assertEquals(48, minutosDe("j2", eventos))
        assertEquals(45, minutosDe("j13", eventos))
    }

    @Test
    fun cambioAnuladoORival_noAfectaLosMinutos() {
        val eventos = listOf(
            evento("anulado", TiposEvento.CAMBIO, 60, jugador = "j1", secundario = "j12", periodo = 2, estado = EstadoEvento.ANULADO),
            evento("rival", TiposEvento.CAMBIO, 30, equipo = EquipoEvento.RIVAL, jugador = JugadorEspecial.RIVAL, periodo = 1),
            finDelPartido,
        )
        assertEquals(90, minutosDe("j1", eventos))
        assertEquals(0, minutosDe("j12", eventos))
    }

    @Test
    fun sinEventoFinDePartido_usaLaDuracionTotal() {
        assertEquals(90, minutosDe("j1", emptyList()))
    }

    // ------------------------------------------------------------------ Contenido del acta

    @Test
    fun acta_cuentaEventosYCalculaMarcadorSoloConVigentes() {
        val eventos = listOf(
            evento("g1", TiposEvento.GOL, 10, jugador = "j9"),
            evento("g2", TiposEvento.GOL, 20, EquipoEvento.RIVAL, JugadorEspecial.RIVAL),
            evento("g3", TiposEvento.GOL, 30, jugador = "j9", estado = EstadoEvento.ANULADO),
            evento("g4", TiposEvento.GOL, 35, jugador = "j9", estado = EstadoEvento.REEMPLAZADO),
            evento("a", TiposEvento.TARJETA_AMARILLA, 40, jugador = JugadorEspecial.NO_IDENTIFICADO, incompleto = true),
            finDelPartido,
        )

        val acta = ReglasActa.construir(partido(), eventos, convocados, CatalogoBase.tipos)

        assertEquals(1, acta.marcadorPropio)
        assertEquals(1, acta.marcadorRival)
        assertEquals(4, acta.eventosVigentes)
        assertEquals(1, acta.eventosAnulados)
        assertEquals(1, acta.eventosIncompletos)
    }

    @Test
    fun acta_indicaOperadorHorasRealesYVersionDelCatalogo() {
        val eventos = listOf(
            evento("ini", TiposEvento.INICIO_PARTIDO, 0, equipo = null, operador = "Ana", registradoEn = 5_000L),
            evento("g", TiposEvento.GOL, 12, operador = "Luis", versionCatalogo = 2),
            evento("fin", TiposEvento.FIN_PARTIDO, 90, equipo = null, periodo = 2, operador = "Ana", registradoEn = 9_000_000L),
        )

        val acta = ReglasActa.construir(partido(inicio = 2_000L), eventos, convocados, CatalogoBase.tipos)

        assertEquals(listOf("Ana", "Luis"), acta.operadores)
        assertEquals(2_000L, acta.inicioRealMs)
        assertEquals(9_000_000L, acta.finRealMs)
        assertEquals(listOf(1, 2), acta.versionesCatalogo)
    }

    @Test
    fun acta_sinFinNiOperador_dejaLosDatosVacios() {
        val acta = ReglasActa.construir(partido(inicio = null), listOf(evento("g", TiposEvento.GOL, 5, operador = null)), convocados, CatalogoBase.tipos)

        assertTrue(acta.operadores.isEmpty())
        assertNull(acta.inicioRealMs)
        assertNull(acta.finRealMs)
    }

    // ------------------------------------------------------------------ Reapertura

    private val horaMs = 3_600_000L

    @Test
    fun administrador_reabreDentroDeLas48Horas() {
        val p = partido(EstadoPartido.ACTA_CERRADA, cerradaEn = 1_000L)
        assertTrue(ReglasActa.puedeReabrir("ADMINISTRADOR", p, 1_000L + 47 * horaMs))
        assertTrue(ReglasActa.puedeReabrir("ADMINISTRADOR", p, 1_000L + 48 * horaMs))
    }

    @Test
    fun administrador_noReabrePasadasLas48Horas() {
        val p = partido(EstadoPartido.ACTA_CERRADA, cerradaEn = 1_000L)
        assertFalse(ReglasActa.puedeReabrir("ADMINISTRADOR", p, 1_000L + 48 * horaMs + 1))
    }

    @Test
    fun entrenadorOJugador_nuncaReabren() {
        val p = partido(EstadoPartido.ACTA_CERRADA, cerradaEn = 1_000L)
        assertFalse(ReglasActa.puedeReabrir("ENTRENADOR", p, 2_000L))
        assertFalse(ReglasActa.puedeReabrir("JUGADOR", p, 2_000L))
        assertFalse(ReglasActa.puedeReabrir(null, p, 2_000L))
    }

    @Test
    fun soloSeReabreUnActaCerradaConMarcaDeCierre() {
        assertFalse(ReglasActa.puedeReabrir("ADMINISTRADOR", partido(EstadoPartido.FINALIZADO, cerradaEn = 1_000L), 2_000L))
        assertFalse(ReglasActa.puedeReabrir("ADMINISTRADOR", partido(EstadoPartido.ACTA_CERRADA, cerradaEn = null), 2_000L))
        assertNull(ReglasActa.limiteReaperturaMs(partido(cerradaEn = null)))
        assertEquals(1_000L + 48 * horaMs, ReglasActa.limiteReaperturaMs(partido(cerradaEn = 1_000L)))
    }

    // ------------------------------------------------------------------ Compartir

    @Test
    fun textoCompartible_incluyeMarcadorMinutosYDatosDelActa() {
        val eventos = listOf(
            evento("g", TiposEvento.GOL, 10, jugador = "j9", operador = "Ana"),
            evento("c", TiposEvento.CAMBIO, 60, jugador = "j1", secundario = "j12", periodo = 2),
            finDelPartido,
        )
        val acta = ReglasActa.construir(partido(), eventos, convocados, CatalogoBase.tipos)

        val texto = ReglasActa.textoCompartible(acta) { "fecha$it" }

        assertTrue(texto.contains("Halcones 1 - 0 Tigres"))
        assertTrue(texto.contains("Operador: Ana, Operador Uno"))
        assertTrue(texto.contains("Catálogo de eventos: v1"))
        assertTrue(texto.contains("1 · Jugador 1 (titular): 60'"))
        assertTrue(texto.contains("12 · Jugador 12: 30'"))
        assertTrue(texto.contains("Inicio: fecha1000"))
    }
}
