package com.esan.sportpro.domain

import com.esan.sportpro.domain.ia.Afirmacion
import com.esan.sportpro.domain.ia.BloquesResumen
import com.esan.sportpro.domain.ia.ConstructorInsumo
import com.esan.sportpro.domain.ia.MencionNoVerificada
import com.esan.sportpro.domain.ia.ReglasResumen
import com.esan.sportpro.domain.ia.SalidaIa
import com.esan.sportpro.domain.partido.CatalogoBase
import com.esan.sportpro.domain.partido.EquipoEvento
import com.esan.sportpro.domain.partido.EstadoEvento
import com.esan.sportpro.domain.partido.EstadoPartido
import com.esan.sportpro.domain.partido.EventoPartido
import com.esan.sportpro.domain.partido.JugadorConvocado
import com.esan.sportpro.domain.partido.JugadorEspecial
import com.esan.sportpro.domain.partido.Partido
import com.esan.sportpro.domain.partido.TiposEvento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** US-023: insumo seudonimizado, verificación de la salida del modelo y reglas del resumen. */
class ResumenIaTest {

    private val convocados = listOf(
        JugadorConvocado("j9", "Luis Pérez", 9, titular = true),
        JugadorConvocado("j10", "Mateo Quispe Rojas", 10, titular = true),
        JugadorConvocado("j12", "Diego Torres", 12, titular = false),
    )

    private fun evento(
        id: String,
        tipo: String,
        minuto: Int,
        equipo: EquipoEvento? = EquipoEvento.PROPIO,
        jugador: String? = null,
        secundario: String? = null,
        estado: EstadoEvento = EstadoEvento.VIGENTE,
        incompleto: Boolean = false,
        observacion: String? = null,
        periodo: Int? = null,
    ) = EventoPartido(
        id = id, grupoId = id, version = 1, tipoId = tipo, tipoNombre = CatalogoBase.porId(tipo)?.nombre ?: tipo,
        minuto = minuto, equipo = equipo, jugadorId = jugador, jugadorNombre = jugador,
        jugadorSecundarioId = secundario, jugadorSecundarioNombre = secundario,
        resultadoPenal = null, observacion = observacion, operadorUid = "uid", operadorNombre = "op",
        registradoEnMs = minuto * 1000L, estado = estado, versionCatalogo = 1,
        incompleto = incompleto, fueraDeOrden = false, excedeCambios = false, eventoOrigenId = null,
        motivo = null, camposModificados = emptyList(), reemplazadoPor = null, anuladoPor = null,
        anuladoPorNombre = null, anuladoEnMs = null, pendienteSincronizar = false, periodo = periodo,
    )

    private fun partido(estado: EstadoPartido = EstadoPartido.ACTA_CERRADA) = Partido(
        id = "p1", nombreEquipo = "Halcones", nombreRival = "Tigres", categoria = "Sub-15",
        estado = estado, alineacionConfirmada = true, cambiosPermitidos = 5, duracionTiempoMin = 45,
        inicioPrimerTiempoMs = 0L, inicioSegundoTiempoMs = null, enDescanso = false,
        operadorUid = null, entrenadorUid = null, creadoPor = null, fechaMs = null,
    )

    private val eventosBase = listOf(
        evento("ini", TiposEvento.INICIO_PARTIDO, 0, equipo = null, periodo = 1),
        evento("g1", TiposEvento.GOL, 12, jugador = "j9", periodo = 1),
        evento("a1", TiposEvento.TARJETA_AMARILLA, 30, EquipoEvento.RIVAL, JugadorEspecial.RIVAL, periodo = 1),
        evento("g2", TiposEvento.GOL, 55, EquipoEvento.RIVAL, JugadorEspecial.RIVAL, periodo = 2),
        evento("g3", TiposEvento.GOL, 70, jugador = "j10", secundario = "j9", periodo = 2),
        evento("fin", TiposEvento.FIN_PARTIDO, 90, equipo = null, periodo = 2),
    )

    private fun insumo(eventos: List<EventoPartido> = eventosBase) =
        ConstructorInsumo.construir(partido(), eventos, convocados, CatalogoBase.tipos)

    private fun salida(
        hechos: List<Afirmacion> = emptyList(),
        desarrollo: List<Afirmacion> = emptyList(),
    ) = SalidaIa("gemini-test", hechos, desarrollo, emptyList())

    private fun hecho(texto: String, vararg ids: String) = Afirmacion(texto, ids.toList())

    // ------------------------------------------------------------------ Cuándo se puede generar

    @Test
    fun noSeGenera_sinActaCerradaConPocosEventosOSinConexion() {
        assertEquals(
            ReglasResumen.MOTIVO_SIN_ACTA,
            ReglasResumen.motivoNoGenerar(partido(EstadoPartido.FINALIZADO), eventosBase, true),
        )
        assertEquals(
            ReglasResumen.MOTIVO_POCOS_EVENTOS,
            ReglasResumen.motivoNoGenerar(partido(), eventosBase.take(2), true),
        )
        assertEquals(ReglasResumen.MOTIVO_SIN_CONEXION, ReglasResumen.motivoNoGenerar(partido(), eventosBase, false))
        assertNull(ReglasResumen.motivoNoGenerar(partido(), eventosBase, true))
    }

    @Test
    fun losEventosAnuladosNoCuentanParaElMinimoDeTres() {
        val eventos = eventosBase.take(3) + evento("x", TiposEvento.FALTA, 5, estado = EstadoEvento.ANULADO)
        assertNull(ReglasResumen.motivoNoGenerar(partido(), eventos, true))
        val pocos = eventosBase.take(2) + evento("x", TiposEvento.FALTA, 5, estado = EstadoEvento.ANULADO)
        assertEquals(ReglasResumen.MOTIVO_POCOS_EVENTOS, ReglasResumen.motivoNoGenerar(partido(), pocos, true))
    }

    // ------------------------------------------------------------------ Alias y privacidad

    @Test
    fun alias_usaElDorsal_yEvitaRepetidosOSinDorsal() {
        val lista = listOf(
            JugadorConvocado("a", "Ana Gómez", 7, true),
            JugadorConvocado("b", "Beto Ruiz", 7, true),
            JugadorConvocado("c", "Carlos Sáenz", null, false),
            JugadorConvocado("d", "Dario Vega", 5, false),
        )

        val alias = ConstructorInsumo.asignarAlias(lista).mapValues { it.value.alias }

        assertEquals("J05", alias["d"])
        assertEquals("J07", alias["a"])
        assertEquals(4, alias.values.toSet().size)
        assertTrue(alias.getValue("b").startsWith("JX"))
        assertTrue(alias.getValue("c").startsWith("JX"))
    }

    @Test
    fun insumo_noContieneNombresNiApellidos_ySoloIncluyeEventosVigentes() {
        val eventos = eventosBase + listOf(
            evento("anulado", TiposEvento.GOL, 20, jugador = "j12", estado = EstadoEvento.ANULADO),
            evento("viejo", TiposEvento.FALTA, 25, jugador = "j9", estado = EstadoEvento.REEMPLAZADO),
        )

        val insumo = insumo(eventos)
        val serializado = listOf(insumo.alineacion, insumo.eventos, insumo.partido).joinToString { it.toString() }

        assertEquals(eventosBase.map { it.id }, insumo.eventos.map { it.id })
        assertFalse(serializado.contains("Luis"))
        assertFalse(serializado.contains("Pérez"))
        assertFalse(serializado.contains("Quispe"))
        assertEquals("J09", insumo.eventos.first { it.id == "g1" }.jugador)
        assertEquals("J09", insumo.eventos.first { it.id == "g3" }.jugadorSecundario)
        assertEquals(2, insumo.partido.marcadorPropio)
        assertEquals(1, insumo.partido.marcadorRival)
    }

    @Test
    fun insumo_listaLosEventosIncompletos_yUsaNoIdentificado() {
        val eventos = eventosBase + evento("inc", TiposEvento.FALTA, 40, jugador = JugadorEspecial.NO_IDENTIFICADO, incompleto = true)

        val insumo = insumo(eventos)

        assertEquals(listOf("inc"), insumo.incompletos)
        assertEquals(JugadorEspecial.NO_IDENTIFICADO, insumo.eventos.first { it.id == "inc" }.jugador)
    }

    @Test
    fun observacion_reemplazaNombresYOcultaTelefonosDocumentosYCorreos() {
        val texto = "Luis se lesionó, llamar al 987654321 o a luis@correo.com. DNI 12345678. Mateo ayudó."

        val limpio = ConstructorInsumo.sanear(texto, ConstructorInsumo.asignarAlias(convocados).values)!!

        assertFalse(limpio.contains("Luis"))
        assertFalse(limpio.contains("Mateo"))
        assertFalse(limpio.contains("987654321"))
        assertFalse(limpio.contains("@"))
        assertFalse(limpio.contains("12345678"))
        assertTrue(limpio.contains("J09"))
        assertTrue(limpio.contains("J10"))
    }

    @Test
    fun observacion_seRecortaYConservaLosNumerosCortos() {
        val aliases = ConstructorInsumo.asignarAlias(convocados).values

        assertEquals(ConstructorInsumo.MAX_OBSERVACION, ConstructorInsumo.sanear("a".repeat(500), aliases)!!.length)
        assertEquals("Falta 12 - 15 metros", ConstructorInsumo.sanear("Falta 12 - 15 metros", aliases))
        assertNull(ConstructorInsumo.sanear("   ", aliases))
        assertNull(ConstructorInsumo.sanear(null, aliases))
    }

    // ------------------------------------------------------------------ Verificación

    @Test
    fun textoCorrecto_noGeneraMencionesNoVerificadas() {
        val s = salida(
            hechos = listOf(
                hecho("Al minuto 12 J09 abrió el marcador (1-0).", "g1"),
                hecho("Halcones ganó 2-1 a Tigres.", "g3"),
                hecho("Tigres recibió una tarjeta amarilla al 30'.", "a1"),
            ),
            desarrollo = listOf(hecho("El partido comenzó parejo. Luego J10 marcó el segundo gol al minuto 70.", "g3")),
        )

        assertTrue(ReglasResumen.verificar(s, insumo()).isEmpty())
    }

    @Test
    fun aliasInexistente_seMarca() {
        val s = salida(hechos = listOf(hecho("J77 anotó un gol.", "g1")))

        val menciones = ReglasResumen.verificar(s, insumo())

        assertEquals(listOf("J77"), menciones.map { it.texto })
    }

    @Test
    fun minutoSinEvento_seMarca_peroLosMinutosRegistradosNo() {
        val s = salida(hechos = listOf(hecho("Hubo un gol al minuto 77 y otro al 70'.", "g3")))

        val menciones = ReglasResumen.verificar(s, insumo()).map { it.texto }

        assertTrue(menciones.contains("minuto 77"))
        assertFalse(menciones.contains("70'"))
    }

    @Test
    fun tiempoAnadido_sevalidaConLaSuma() {
        val eventos = eventosBase + evento("g4", TiposEvento.GOL, 48, jugador = "j9", periodo = 1)
        val insumo = insumo(eventos)

        val bien = ReglasResumen.verificar(salida(hechos = listOf(hecho("J09 anotó al 45+3'.", "g4"))), insumo)
        val mal = ReglasResumen.verificar(salida(hechos = listOf(hecho("J09 anotó al 45+4'.", "g4"))), insumo)

        assertTrue(bien.isEmpty())
        assertEquals(listOf("45+4'"), mal.map { it.texto })
    }

    @Test
    fun marcador_aceptaLosParcialesPeroNoUnoImposible() {
        val s = salida(
            hechos = listOf(hecho("El partido terminó 2-1.", "g3")),
            desarrollo = listOf(hecho("Se llegó a ir ganando 1-0 y luego empataron 1-1, pero nunca estuvo 5-0.", "g1")),
        )

        val menciones = ReglasResumen.verificar(s, insumo()).map { it.texto }

        assertEquals(listOf("5-0"), menciones)
    }

    @Test
    fun nombrePropioInventado_seMarca_peroEquiposYCategoriaNo() {
        val s = salida(
            hechos = listOf(hecho("El delantero Carlos anotó para Halcones frente a Tigres en la Sub-15.", "g1")),
        )

        val menciones = ReglasResumen.verificar(s, insumo()).map { it.texto }

        assertEquals(listOf("Carlos"), menciones)
    }

    @Test
    fun hechoSinCitaOConCitaInexistente_seMarcaCompleto() {
        val s = salida(
            hechos = listOf(
                hecho("Sin cita alguna.", ),
                hecho("Cita un evento que no existe.", "zzz"),
                hecho("Este sí cita bien.", "g1"),
            ),
        )

        val menciones = ReglasResumen.verificar(s, insumo()).map { it.texto }

        assertEquals(listOf("Sin cita alguna.", "Cita un evento que no existe."), menciones)
    }

    @Test
    fun observacionConInstruccionMaliciosa_noCambiaElMarcadorVerificado() {
        val s = salida(hechos = listOf(hecho("Halcones ganó 10-0.", "g1")))

        val menciones = ReglasResumen.verificar(s, insumo()).map { it.texto }

        assertEquals(listOf("10-0"), menciones)
    }

    // ------------------------------------------------------------------ Bloques y límite

    @Test
    fun bloques_hechosEnListaDesarrolloEnParrafos_yInterpretacionVacia() {
        val s = salida(
            hechos = listOf(hecho("Primer hecho.", "g1"), hecho("Segundo hecho.", "g2")),
            desarrollo = listOf(hecho("Párrafo uno.", "g1"), hecho("Párrafo dos.", "g3")),
        )

        val b = ReglasResumen.armarBloques(s)

        assertEquals("• Primer hecho.\n• Segundo hecho.", b.hechos)
        assertEquals("Párrafo uno.\n\nPárrafo dos.", b.desarrollo)
        assertEquals("", b.interpretacion)
    }

    @Test
    fun limiteDe400Palabras_seCuentaEntreHechosYDesarrollo() {
        val justo = BloquesResumen(hechos = List(150) { "a" }.joinToString(" "), desarrollo = List(250) { "b" }.joinToString(" "))
        val pasado = BloquesResumen(hechos = List(150) { "a" }.joinToString(" "), desarrollo = List(251) { "b" }.joinToString(" "))

        assertTrue(ReglasResumen.dentroDelLimite(justo))
        assertFalse(ReglasResumen.dentroDelLimite(pasado))
        assertEquals(3, ReglasResumen.contarPalabras("uno  dos\ntres"))
    }

    @Test
    fun segmentar_resaltaSoloLasMencionesNoVerificadas() {
        val menciones = listOf(MencionNoVerificada("J77", "x"), MencionNoVerificada("minuto 77", "y"))

        val segmentos = ReglasResumen.segmentar("J77 marcó al minuto 77 y J77 celebró.", menciones)

        assertEquals("J77 marcó al minuto 77 y J77 celebró.", segmentos.joinToString("") { it.texto })
        assertEquals(listOf("J77", "minuto 77", "J77"), segmentos.filter { it.noVerificado }.map { it.texto })
        assertFalse(segmentos.first { it.texto == " marcó al " }.noVerificado)
    }

    @Test
    fun segmentar_sinMencionesDevuelveUnSoloFragmentoNormal_yNoDuplicaSolapadas() {
        val sin = ReglasResumen.segmentar("Todo en orden.", emptyList())
        assertEquals(listOf(ReglasResumen.Segmento("Todo en orden.", false)), sin)

        val solapadas = ReglasResumen.segmentar("gol al minuto 77", listOf(MencionNoVerificada("minuto 77", "a"), MencionNoVerificada("77", "b")))
        assertEquals("gol al minuto 77", solapadas.joinToString("") { it.texto })
        assertEquals(listOf("minuto 77"), solapadas.filter { it.noVerificado }.map { it.texto })
    }

    @Test
    fun reidentificar_cambiaElAliasPorNombreDePilaYDorsal() {
        val insumo = insumo()

        val texto = ReglasResumen.reidentificar("J09 asistió a J10, y J99 no existe.", insumo.aliases)

        assertEquals("Luis (9) asistió a Mateo (10), y J99 no existe.", texto)
    }
}
