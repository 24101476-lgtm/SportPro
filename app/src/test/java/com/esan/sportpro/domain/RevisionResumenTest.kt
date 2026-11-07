package com.esan.sportpro.domain

import com.esan.sportpro.domain.cuentas.PerfilActual
import com.esan.sportpro.domain.ia.BloquesResumen
import com.esan.sportpro.domain.ia.CampoBloque
import com.esan.sportpro.domain.ia.ConstructorInsumo
import com.esan.sportpro.domain.ia.DiferenciasResumen
import com.esan.sportpro.domain.ia.EstadoResumen
import com.esan.sportpro.domain.ia.MencionNoVerificada
import com.esan.sportpro.domain.ia.ReglasRevision
import com.esan.sportpro.domain.ia.ResumenPartido
import com.esan.sportpro.domain.partido.CatalogoBase
import com.esan.sportpro.domain.partido.EquipoEvento
import com.esan.sportpro.domain.partido.EstadoEvento
import com.esan.sportpro.domain.partido.EstadoPartido
import com.esan.sportpro.domain.partido.EventoPartido
import com.esan.sportpro.domain.partido.JugadorConvocado
import com.esan.sportpro.domain.partido.JugadorEspecial
import com.esan.sportpro.domain.partido.Partido
import com.esan.sportpro.domain.partido.TiposEvento
import com.esan.sportpro.navigation.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** US-024: revisión, corrección y aprobación del resumen narrativo. */
class RevisionResumenTest {

    private val convocados = listOf(
        JugadorConvocado("j9", "Luis Pérez", 9, titular = true),
        JugadorConvocado("j10", "Mateo Quispe Rojas", 10, titular = true),
    )

    private fun evento(id: String, tipo: String, minuto: Int, equipo: EquipoEvento? = EquipoEvento.PROPIO, jugador: String? = null, periodo: Int) =
        EventoPartido(
            id = id, grupoId = id, version = 1, tipoId = tipo, tipoNombre = CatalogoBase.porId(tipo)?.nombre ?: tipo,
            minuto = minuto, equipo = equipo, jugadorId = jugador, jugadorNombre = jugador,
            jugadorSecundarioId = null, jugadorSecundarioNombre = null,
            resultadoPenal = null, observacion = null, operadorUid = "uid", operadorNombre = "op",
            registradoEnMs = minuto * 1000L, estado = EstadoEvento.VIGENTE, versionCatalogo = 1,
            incompleto = false, fueraDeOrden = false, excedeCambios = false, eventoOrigenId = null,
            motivo = null, camposModificados = emptyList(), reemplazadoPor = null, anuladoPor = null,
            anuladoPorNombre = null, anuladoEnMs = null, pendienteSincronizar = false, periodo = periodo,
        )

    private fun partido(operador: String? = "coach", entrenador: String? = null, creador: String? = null) = Partido(
        id = "p1", nombreEquipo = "Halcones", nombreRival = "Tigres", categoria = "Sub-15",
        estado = EstadoPartido.ACTA_CERRADA, alineacionConfirmada = true, cambiosPermitidos = 5, duracionTiempoMin = 45,
        inicioPrimerTiempoMs = 0L, inicioSegundoTiempoMs = null, enDescanso = false,
        operadorUid = operador, entrenadorUid = entrenador, creadoPor = creador, fechaMs = null,
    )

    private val eventos = listOf(
        evento("ini", TiposEvento.INICIO_PARTIDO, 0, equipo = null, periodo = 1),
        evento("g1", TiposEvento.GOL, 12, jugador = "j9", periodo = 1),
        evento("g2", TiposEvento.GOL, 55, EquipoEvento.RIVAL, JugadorEspecial.RIVAL, periodo = 2),
        evento("fin", TiposEvento.FIN_PARTIDO, 90, equipo = null, periodo = 2),
    )

    private val insumo = ConstructorInsumo.construir(partido(), eventos, convocados, CatalogoBase.tipos)

    private fun perfil(rol: UserRole, uid: String = "coach") = PerfilActual(uid, "Nombre", rol, "academia-demo")

    private fun resumen(
        estado: EstadoResumen = EstadoResumen.BORRADOR,
        version: Int = 1,
        final: BloquesResumen? = null,
        menciones: List<MencionNoVerificada> = emptyList(),
        aprobadoPor: String? = null,
    ) = ResumenPartido(
        id = "r$version", partidoId = "p1", version = version, estado = estado,
        bloques = BloquesResumen("• Gol de Luis (9) en el minuto 12.", "Halcones ganó 1-1."),
        modelo = "gemini-test", versionPrompt = "v1", solicitadoPor = "coach", creadoEnMs = 1L,
        mencionesNoVerificadas = menciones, advertencias = emptyList(), eventosUsados = emptyList(),
        marcador = "1-1", bloquesFinal = final, aprobadoPor = aprobadoPor,
    )

    // ------------------------------------------------------------------ Textos de la historia

    @Test
    fun losTextosFijosSonLosDeLaHistoria() {
        assertEquals("Resumen generado con IA y revisado por el entrenador", ReglasRevision.LEYENDA_IA)
        assertEquals(
            "Confirmo que revisé el resumen y sus datos corresponden a lo ocurrido en el partido",
            ReglasRevision.TEXTO_CONFIRMACION,
        )
        assertEquals("Corrige las menciones no verificadas antes de aprobar", ReglasRevision.MSG_MENCIONES_PENDIENTES)
    }

    // ------------------------------------------------------------------ Validación de los bloques

    @Test
    fun hechosYDesarrolloSonObligatorios() {
        val errores = ReglasRevision.validar(BloquesResumen(" ", ""))
        assertEquals(ReglasRevision.MSG_SIN_HECHOS, errores[CampoBloque.HECHOS])
        assertEquals(ReglasRevision.MSG_SIN_DESARROLLO, errores[CampoBloque.DESARROLLO])
        assertTrue(ReglasRevision.validar(BloquesResumen("• Gol", "Texto", "")).isEmpty())
    }

    @Test
    fun hechosMasDesarrolloNoPasanDe400Palabras() {
        // "Gol" cuenta como 1 palabra: 1 + 400 pasa del tope; 1 + 399 llega justo a 400.
        val largo = List(400) { "palabra" }.joinToString(" ")
        assertEquals(ReglasRevision.MSG_LIMITE_PALABRAS, ReglasRevision.validar(BloquesResumen("Gol", largo))[CampoBloque.DESARROLLO])
        val justo = List(399) { "palabra" }.joinToString(" ")
        assertTrue(ReglasRevision.validar(BloquesResumen("Gol", justo)).isEmpty())
    }

    @Test
    fun laInterpretacionTieneUnTope() {
        val larga = "a".repeat(ReglasRevision.MAX_INTERPRETACION + 1)
        assertEquals(
            ReglasRevision.MSG_INTERPRETACION,
            ReglasRevision.validar(BloquesResumen("• Gol", "Texto", larga))[CampoBloque.INTERPRETACION],
        )
        assertTrue(ReglasRevision.validar(BloquesResumen("• Gol", "Texto", "a".repeat(ReglasRevision.MAX_INTERPRETACION))).isEmpty())
    }

    // ------------------------------------------------------------------ Menciones y aprobación

    @Test
    fun conMencionesPendientesNoSeAprueba() {
        val bloques = BloquesResumen("• Gol de Luis (9) en el minuto 12.", "Texto.")
        val mencion = MencionNoVerificada("minuto 77", "Minuto sin un evento registrado")
        assertEquals(ReglasRevision.MSG_MENCIONES_PENDIENTES, ReglasRevision.motivoNoAprobar(bloques, listOf(mencion)))
        assertNull(ReglasRevision.motivoNoAprobar(bloques, emptyList()))
        assertEquals(ReglasRevision.MSG_SIN_HECHOS, ReglasRevision.motivoNoAprobar(BloquesResumen("", "Texto."), listOf(mencion)))
    }

    @Test
    fun elTextoCorrectoNoTieneMenciones_ysusNombresDePilaSePermiten() {
        val bloques = BloquesResumen(
            "• Gol de Luis (9) en el minuto 12.\n• Gol del rival en el minuto 55.",
            "Halcones empató 1-1 con Tigres. Mateo (10) jugó todo el partido.",
        )
        assertTrue(ReglasRevision.mencionesVigentes(emptyList(), bloques, insumo).isEmpty())
    }

    @Test
    fun detectaMinutosYMarcadoresQueNoOcurrieron() {
        val bloques = BloquesResumen("• Gol de Luis (9) en el minuto 77.", "Halcones ganó 5-0.")
        val textos = ReglasRevision.mencionesVigentes(emptyList(), bloques, insumo).map { it.texto }
        assertTrue(textos.any { it.contains("77") })
        assertTrue("5-0" in textos)
    }

    @Test
    fun detectaNombresPropiosQueNoFiguranEnElPartido() {
        val bloques = BloquesResumen("• Gol de Luis (9) en el minuto 12.", "Hubo un gol de Ronaldo en el segundo tiempo.")
        val textos = ReglasRevision.mencionesVigentes(emptyList(), bloques, insumo).map { it.texto }
        assertTrue("Ronaldo" in textos)
    }

    @Test
    fun unaMencionPrevia_siguePendienteHastaQueSeCorrigeOElimina() {
        val previa = MencionNoVerificada("Gol de Luis (9) en el minuto 12.", "El hecho no cita un evento registrado")
        val igual = BloquesResumen("• Gol de Luis (9) en el minuto 12.", "Halcones empató 1-1.")
        assertEquals(1, ReglasRevision.mencionesVigentes(listOf(previa), igual, insumo).size)
        val corregido = BloquesResumen("• Luis (9) marcó en el minuto 12.", "Halcones empató 1-1.")
        assertTrue(ReglasRevision.mencionesVigentes(listOf(previa), corregido, insumo).isEmpty())
    }

    // ------------------------------------------------------------------ Quién revisa y qué se publica

    @Test
    fun revisanElAdministradorYQuienOperaElPartido() {
        assertTrue(ReglasRevision.puedeRevisar(perfil(UserRole.ADMINISTRADOR, "admin"), partido()))
        assertTrue(ReglasRevision.puedeRevisar(perfil(UserRole.ENTRENADOR, "coach"), partido(operador = "coach")))
        assertTrue(ReglasRevision.puedeRevisar(perfil(UserRole.ENTRENADOR, "boss"), partido(operador = null, entrenador = "boss")))
        assertTrue(ReglasRevision.puedeRevisar(perfil(UserRole.ENTRENADOR, "yo"), partido(operador = null, creador = "yo")))
        assertFalse(ReglasRevision.puedeRevisar(perfil(UserRole.ENTRENADOR, "otro"), partido(operador = "coach")))
        assertFalse(ReglasRevision.puedeRevisar(perfil(UserRole.JUGADOR, "coach"), partido(operador = "coach")))
        assertFalse(ReglasRevision.puedeRevisar(perfil(UserRole.PADRE_DE_FAMILIA, "coach"), partido(operador = "coach")))
        assertFalse(ReglasRevision.puedeRevisar(null, partido()))
    }

    @Test
    fun seMuestraLaVersionAprobadaMasReciente() {
        val lista = listOf(
            resumen(EstadoResumen.BORRADOR, version = 3),
            resumen(EstadoResumen.APROBADO, version = 2),
            resumen(EstadoResumen.APROBADO, version = 1),
        )
        assertEquals(2, ReglasRevision.publicadoMasReciente(lista)?.version)
        assertNull(ReglasRevision.publicadoMasReciente(listOf(resumen(EstadoResumen.BORRADOR))))
    }

    @Test
    fun elTextoVigenteEsElDelEntrenadorSiLoEdito() {
        val editado = BloquesResumen("• Hecho corregido", "Desarrollo corregido", "Mi análisis")
        assertEquals(resumen().bloques, resumen().bloquesVigentes)
        assertEquals(editado, resumen(final = editado).bloquesVigentes)
        assertEquals("• Gol de Luis (9) en el minuto 12.", resumen(final = editado).bloques.hechos)
    }

    @Test
    fun laPublicacionDeComunidadTraeElMarcadorLaLeyendaYNoPasaDe1000Caracteres() {
        val hechos = List(80) { "• Evento número $it del partido con bastante texto para ocupar espacio" }.joinToString("\n")
        val largo = resumen(final = BloquesResumen(hechos, "Desarrollo", ""))
        val texto = ReglasRevision.textoParaComunidad(partido(), largo)
        assertTrue(texto.length <= 1000)
        assertTrue(texto.startsWith("Resumen del partido: Halcones 1 - 1 Tigres"))
        assertTrue(texto.endsWith(ReglasRevision.LEYENDA_IA))
        assertTrue(texto.contains("…"))

        val corto = ReglasRevision.textoParaComunidad(partido(), resumen())
        assertTrue(corto.contains("• Gol de Luis (9) en el minuto 12."))
        assertFalse(corto.contains("…"))
    }

    // ------------------------------------------------------------------ Diferencias (US-025, criterio 3)

    @Test
    fun sinCambiosNoHayCorrecciones() {
        val b = BloquesResumen("• Gol de Luis (9).\n• Fin del partido.", "El partido fue parejo. Terminó empatado.")
        assertEquals(DiferenciasResumen.SIN_CORRECCIONES, DiferenciasResumen.correcciones(b, b))
    }

    @Test
    fun unaFraseCambiadaSeInformaComoCorreccion() {
        val original = BloquesResumen("• Gol de Luis (9) en el minuto 12.\n• Fin del partido.", "Halcones ganó 3-1.")
        val final = BloquesResumen("• Gol de Luis (9) en el minuto 12.\n• Fin del partido.", "Halcones empató 1-1.")
        val texto = DiferenciasResumen.correcciones(original, final)
        assertEquals("Desarrollo, se corrigió: «Halcones ganó 3-1.» por «Halcones empató 1-1.»", texto)
    }

    @Test
    fun seInformanLasFrasesEliminadasYAgregadas() {
        val original = BloquesResumen("• Uno.\n• Dos.\n• Tres.", "Texto.", "")
        val final = BloquesResumen("• Uno.\n• Tres.\n• Cuatro.", "Texto.", "Faltó presión alta.")
        val lineas = DiferenciasResumen.correcciones(original, final).lines()
        assertTrue("Hechos, se eliminó: «Dos.»" in lineas)
        assertTrue("Hechos, se agregó: «Cuatro.»" in lineas)
        assertTrue("Interpretación, se agregó: «Faltó presión alta.»" in lineas)
        assertEquals(3, lineas.size)
    }
}
