package com.esan.sportpro.domain

import com.esan.sportpro.domain.cuentas.PerfilActual
import com.esan.sportpro.domain.ia.BloquesResumen
import com.esan.sportpro.domain.ia.CampoEvaluacion
import com.esan.sportpro.domain.ia.DatosEvaluacion
import com.esan.sportpro.domain.ia.EstadoResumen
import com.esan.sportpro.domain.ia.EvaluacionResumen
import com.esan.sportpro.domain.ia.ReglasEvaluacion
import com.esan.sportpro.domain.ia.ResumenPartido
import com.esan.sportpro.navigation.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** US-025: evaluación documentada del resumen narrativo. */
class EvaluacionResumenTest {

    private val comentarioValido = "El resumen fue útil, aunque omitió el cambio del segundo tiempo."

    private fun datos(
        utilidad: String = "4",
        omisiones: String = "1",
        errores: String = "0",
        inventados: String = "0",
        comentario: String = comentarioValido,
        correcciones: String = "",
    ) = DatosEvaluacion(utilidad, omisiones, errores, inventados, comentario, correcciones)

    private fun perfil(rol: UserRole, uid: String) = PerfilActual(uid, "Nombre", rol, "academia-demo")

    private fun resumen(aprobadoPor: String? = "coach") = ResumenPartido(
        id = "r1", partidoId = "p1", version = 1,
        estado = if (aprobadoPor == null) EstadoResumen.BORRADOR else EstadoResumen.APROBADO,
        bloques = BloquesResumen("• Hecho", "Desarrollo"), modelo = "m", versionPrompt = "v1",
        solicitadoPor = "coach", creadoEnMs = 1L, mencionesNoVerificadas = emptyList(), advertencias = emptyList(),
        eventosUsados = emptyList(), marcador = "1-0", aprobadoPor = aprobadoPor,
    )

    private fun evaluacion(utilidad: Int, omisiones: Int = 0, errores: Int = 0, inventados: Int = 0, nombre: String = "Halcones vs Tigres") =
        EvaluacionResumen(
            id = "p_r", partidoId = "p", resumenId = "r", version = 1, partidoNombre = nombre,
            fechaPartidoMs = null, academiaId = "academia-demo", aprobadoPor = "coach", evaluadorUid = "coach",
            utilidad = utilidad, omisiones = omisiones, errores = errores, inventados = inventados,
            comentario = comentarioValido, correcciones = "", creadoEnMs = 1L,
        )

    // ------------------------------------------------------------------ Formulario

    @Test
    fun unFormularioCompletoEsValido() {
        assertTrue(ReglasEvaluacion.validar(datos()).isEmpty())
    }

    @Test
    fun laUtilidadVaDe1a5() {
        for (valor in listOf("", "0", "6", "abc", "-1")) {
            assertEquals(ReglasEvaluacion.MSG_UTILIDAD, ReglasEvaluacion.validar(datos(utilidad = valor))[CampoEvaluacion.UTILIDAD])
        }
        for (valor in listOf("1", "3", "5")) {
            assertNull(ReglasEvaluacion.validar(datos(utilidad = valor))[CampoEvaluacion.UTILIDAD])
        }
    }

    @Test
    fun lasCantidadesSonEnterosNoNegativos() {
        val errores = ReglasEvaluacion.validar(datos(omisiones = "-1", errores = "", inventados = "100"))
        assertEquals(ReglasEvaluacion.MSG_CANTIDAD, errores[CampoEvaluacion.OMISIONES])
        assertEquals(ReglasEvaluacion.MSG_CANTIDAD, errores[CampoEvaluacion.ERRORES])
        assertEquals(ReglasEvaluacion.MSG_CANTIDAD, errores[CampoEvaluacion.INVENTADOS])
        assertTrue(ReglasEvaluacion.validar(datos(omisiones = "0", errores = "0", inventados = "99")).isEmpty())
    }

    @Test
    fun elComentarioTiene20a500Caracteres() {
        assertEquals(ReglasEvaluacion.MSG_COMENTARIO, ReglasEvaluacion.validar(datos(comentario = "Muy corto"))[CampoEvaluacion.COMENTARIO])
        assertEquals(ReglasEvaluacion.MSG_COMENTARIO, ReglasEvaluacion.validar(datos(comentario = "  ${"a".repeat(19)}  "))[CampoEvaluacion.COMENTARIO])
        assertNull(ReglasEvaluacion.validar(datos(comentario = "a".repeat(20)))[CampoEvaluacion.COMENTARIO])
        assertNull(ReglasEvaluacion.validar(datos(comentario = "a".repeat(500)))[CampoEvaluacion.COMENTARIO])
        assertEquals(ReglasEvaluacion.MSG_COMENTARIO, ReglasEvaluacion.validar(datos(comentario = "a".repeat(501)))[CampoEvaluacion.COMENTARIO])
    }

    @Test
    fun lasCorreccionesTienenTope() {
        assertNull(ReglasEvaluacion.validar(datos(correcciones = ""))[CampoEvaluacion.CORRECCIONES])
        assertEquals(
            ReglasEvaluacion.MSG_CORRECCIONES,
            ReglasEvaluacion.validar(datos(correcciones = "a".repeat(ReglasEvaluacion.CORRECCIONES_MAX + 1)))[CampoEvaluacion.CORRECCIONES],
        )
    }

    // ------------------------------------------------------------------ Quién evalúa

    @Test
    fun evaluanElEntrenadorQueAprobo_yElAdministrador() {
        assertTrue(ReglasEvaluacion.puedeEvaluar(perfil(UserRole.ENTRENADOR, "coach"), resumen("coach")))
        assertTrue(ReglasEvaluacion.puedeEvaluar(perfil(UserRole.ADMINISTRADOR, "admin"), resumen("coach")))
        assertFalse(ReglasEvaluacion.puedeEvaluar(perfil(UserRole.ENTRENADOR, "otro"), resumen("coach")))
        assertFalse(ReglasEvaluacion.puedeEvaluar(perfil(UserRole.JUGADOR, "coach"), resumen("coach")))
        assertFalse(ReglasEvaluacion.puedeEvaluar(perfil(UserRole.PADRE_DE_FAMILIA, "coach"), resumen("coach")))
        assertFalse(ReglasEvaluacion.puedeEvaluar(null, resumen("coach")))
    }

    @Test
    fun unResumenSinAprobarNoSeEvalua() {
        assertFalse(ReglasEvaluacion.puedeEvaluar(perfil(UserRole.ENTRENADOR, "coach"), resumen(aprobadoPor = null)))
        assertFalse(ReglasEvaluacion.puedeEvaluar(perfil(UserRole.ADMINISTRADOR, "admin"), resumen(aprobadoPor = null)))
    }

    @Test
    fun laEvaluacionQuedaPendienteHastaQueSeRegistra() {
        val coach = perfil(UserRole.ENTRENADOR, "coach")
        assertTrue(ReglasEvaluacion.pendiente(coach, resumen("coach"), null))
        assertFalse(ReglasEvaluacion.pendiente(coach, resumen("coach"), evaluacion(4)))
        assertFalse(ReglasEvaluacion.pendiente(coach, null, null))
        assertFalse(ReglasEvaluacion.pendiente(perfil(UserRole.ENTRENADOR, "otro"), resumen("coach"), null))
    }

    // ------------------------------------------------------------------ Consolidado y exportación

    @Test
    fun elConsolidadoPromediaLaUtilidadYSumaLosHallazgos() {
        val total = ReglasEvaluacion.consolidar(
            listOf(evaluacion(5, 1, 0, 0), evaluacion(4, 2, 1, 0), evaluacion(4, 0, 0, 3)),
        )
        assertEquals(3, total.evaluaciones)
        assertEquals(4.3, total.utilidadPromedio)
        assertEquals(3, total.omisiones)
        assertEquals(1, total.errores)
        assertEquals(3, total.inventados)
    }

    @Test
    fun sinEvaluacionesNoHayPromedio() {
        val total = ReglasEvaluacion.consolidar(emptyList())
        assertEquals(0, total.evaluaciones)
        assertNull(total.utilidadPromedio)
        assertEquals("–", ReglasEvaluacion.formatearPromedio(total.utilidadPromedio))
        assertEquals("4.3", ReglasEvaluacion.formatearPromedio(4.3))
    }

    @Test
    fun elPromedioSeRedondeaAUnDecimal() {
        // (5 + 4) / 2 = 4,5 y (5 + 5 + 4) / 3 = 4,666…
        assertEquals(4.5, ReglasEvaluacion.consolidar(listOf(evaluacion(5), evaluacion(4))).utilidadPromedio)
        assertEquals(4.7, ReglasEvaluacion.consolidar(listOf(evaluacion(5), evaluacion(5), evaluacion(4))).utilidadPromedio)
    }

    @Test
    fun elTextoExportableTrae_totales_detalleYCorrecciones() {
        val conCorrecciones = evaluacion(4, 1, 0, 2, "Halcones vs Tigres").copy(
            correcciones = "Hechos, se eliminó: «Dos.»\nDesarrollo, se agregó: «Tres.»",
        )
        val texto = ReglasEvaluacion.textoExportable(listOf(conCorrecciones, evaluacion(5, nombre = "Leones vs Pumas"))) { "fecha" }
        val lineas = texto.lines()
        assertEquals("EVALUACIÓN DEL RESUMEN NARRATIVO CON IA - SportPro", lineas.first())
        assertTrue("Evaluaciones registradas: 2" in lineas)
        assertTrue("Utilidad promedio: 4.5 de 5" in lineas)
        assertTrue("Omisiones detectadas: 1" in lineas)
        assertTrue("Datos inventados: 2" in lineas)
        assertTrue("1. Halcones vs Tigres (versión 1)" in lineas)
        assertTrue("   - Hechos, se eliminó: «Dos.»" in lineas)
        assertTrue("2. Leones vs Pumas (versión 1)" in lineas)
        assertFalse(texto.endsWith("\n"))
    }

    @Test
    fun elTextoExportableSinEvaluacionesSoloTraeLosTotales() {
        val texto = ReglasEvaluacion.textoExportable(emptyList()) { "fecha" }
        assertTrue(texto.contains("Evaluaciones registradas: 0"))
        assertFalse(texto.contains("Detalle por partido"))
    }
}
