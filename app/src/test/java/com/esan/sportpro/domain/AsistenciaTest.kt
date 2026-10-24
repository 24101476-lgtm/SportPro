package com.esan.sportpro.domain

import com.esan.sportpro.domain.entrenamientos.CalculoAsistencia
import com.esan.sportpro.domain.entrenamientos.CampoMarca
import com.esan.sportpro.domain.entrenamientos.Entrenamiento
import com.esan.sportpro.domain.entrenamientos.EstadoAsistencia
import com.esan.sportpro.domain.entrenamientos.EstadoSesion
import com.esan.sportpro.domain.entrenamientos.RegistroHistorialAsistencia
import com.esan.sportpro.domain.entrenamientos.ReglasAsistencia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Date

/** US-011: reglas del registro de asistencia. */
class ReglasAsistenciaTest {

    private val hoy = LocalDate.of(2026, 10, 24)

    @Test
    fun tarde_pideMinutosEntre1y120() {
        listOf("0", "121", "", "diez").forEach { minutos ->
            val errores = ReglasAsistencia.validarMarca(EstadoAsistencia.TARDE, minutos, "")
            assertTrue("minutos '$minutos'", CampoMarca.MINUTOS in errores)
        }
        assertTrue(ReglasAsistencia.validarMarca(EstadoAsistencia.TARDE, "1", "").isEmpty())
        assertTrue(ReglasAsistencia.validarMarca(EstadoAsistencia.TARDE, "120", "").isEmpty())
    }

    @Test
    fun faltaJustificada_pideMotivoEntre5y200Caracteres() {
        assertTrue(CampoMarca.MOTIVO in ReglasAsistencia.validarMarca(EstadoAsistencia.JUSTIFICADO, "", "Gripe".take(4)))
        assertTrue(CampoMarca.MOTIVO in ReglasAsistencia.validarMarca(EstadoAsistencia.JUSTIFICADO, "", "x".repeat(201)))
        assertTrue(ReglasAsistencia.validarMarca(EstadoAsistencia.JUSTIFICADO, "", "Cita médica").isEmpty())
    }

    @Test
    fun presenteYFaltaInjustificada_noPidenDatos() {
        assertFalse(ReglasAsistencia.requiereDetalle(EstadoAsistencia.PRESENTE))
        assertFalse(ReglasAsistencia.requiereDetalle(EstadoAsistencia.AUSENTE))
        assertTrue(ReglasAsistencia.validarMarca(EstadoAsistencia.AUSENTE, "", "").isEmpty())
    }

    @Test
    fun crearMarca_descartaDatosQueNoCorrespondenAlEstado() {
        val presente = ReglasAsistencia.crearMarca(EstadoAsistencia.PRESENTE, "15", "texto", uid = "dt1", nombre = "DT")
        assertNull(presente.minutosRetraso)
        assertEquals("", presente.motivo)

        val tarde = ReglasAsistencia.crearMarca(EstadoAsistencia.TARDE, " 15 ", "", uid = "dt1", nombre = "DT")
        assertEquals(15, tarde.minutosRetraso)
        assertEquals("TARDE", tarde.estado)
        assertEquals("dt1", tarde.marcadoPor)
    }

    @Test
    fun todosEmpiezanSinMarcar_yNoSeCierraHastaMarcarlos() {
        val plantel = listOf("j1", "j2", "j3")
        assertEquals(plantel, ReglasAsistencia.sinMarcar(plantel, emptyMap()))
        assertEquals(listOf("j3"), ReglasAsistencia.sinMarcar(plantel, mapOf("j1" to "PRESENTE", "j2" to "AUSENTE")))
        assertEquals("Quedan 3 jugadores sin marcar", ReglasAsistencia.mensajeSinMarcar(3))
        assertEquals("Queda 1 jugador sin marcar", ReglasAsistencia.mensajeSinMarcar(1))
    }

    @Test
    fun porcentajeDelEquipo_cuentaPresentesYTardes_conUnDecimal() {
        val plantel = listOf("j1", "j2", "j3", "j4", "j5", "j6", "j7")
        val asistencia = mapOf(
            "j1" to "PRESENTE", "j2" to "PRESENTE", "j3" to "PRESENTE", "j4" to "PRESENTE",
            "j5" to "TARDE", "j6" to "JUSTIFICADO", "j7" to "AUSENTE",
        )
        assertEquals(71.4, ReglasAsistencia.porcentajeEquipo(plantel, asistencia), 0.0001)
        assertEquals(0.0, ReglasAsistencia.porcentajeEquipo(emptyList(), emptyMap()), 0.0)
        assertEquals("71.4 %", ReglasAsistencia.formatearPorcentaje(71.4))
    }

    @Test
    fun asistenciaSeTomaDesdeElDiaDeLaSesion_yMientrasNoEsteCerrada() {
        val deHoy = Entrenamiento(fecha = "2026-10-24", estado = "PROGRAMADA")
        assertTrue(ReglasAsistencia.puedeTomarAsistencia(deHoy, hoy))
        assertTrue(ReglasAsistencia.puedeTomarAsistencia(deHoy.copy(fecha = "2026-10-20"), hoy))
        assertFalse(ReglasAsistencia.puedeTomarAsistencia(deHoy.copy(fecha = "2026-10-25"), hoy))
        assertFalse(ReglasAsistencia.puedeTomarAsistencia(deHoy.copy(estado = "REALIZADA"), hoy))
        assertFalse(ReglasAsistencia.puedeTomarAsistencia(deHoy.copy(estado = "CANCELADA"), hoy))
    }

    @Test
    fun historial_masRecientePrimero_yPendientesArriba() {
        val viejo = RegistroHistorialAsistencia(id = "a", marcadoEn = Date(1_000), marcadoEnDispositivo = 900)
        val nuevo = RegistroHistorialAsistencia(id = "b", marcadoEn = Date(5_000), marcadoEnDispositivo = 4_000)
        val pendiente = RegistroHistorialAsistencia(id = "c", marcadoEn = null, marcadoEnDispositivo = 3_000)
        assertEquals(listOf("c", "b", "a"), ReglasAsistencia.ordenarHistorial(listOf(viejo, pendiente, nuevo)).map { it.id })
    }

    @Test
    fun conflicto_cuandoDosUsuariosMarcanAlMismoJugador() {
        val registros = listOf(
            RegistroHistorialAsistencia(id = "1", jugadorId = "j1", marcadoPor = "dt"),
            RegistroHistorialAsistencia(id = "2", jugadorId = "j1", marcadoPor = "asistente"),
            RegistroHistorialAsistencia(id = "3", jugadorId = "j2", marcadoPor = "dt"),
            RegistroHistorialAsistencia(id = "4", jugadorId = "j2", marcadoPor = "dt"),
        )
        assertEquals(setOf("j1"), ReglasAsistencia.jugadoresConConflicto(registros))
    }
}

/** Compatibilidad del estado de la sesión con documentos anteriores a US-011. */
class EstadoSesionTest {

    @Test
    fun sesionesAntiguas_conAsistenciaRegistrada_cuentanComoRealizadas() {
        assertEquals(EstadoSesion.REALIZADA, Entrenamiento(asistenciaRegistrada = true).estadoSesion)
        assertEquals(EstadoSesion.PROGRAMADA, Entrenamiento().estadoSesion)
        assertEquals(EstadoSesion.CANCELADA, Entrenamiento(estado = "CANCELADA").estadoSesion)
    }

    @Test
    fun historialDelJugador_soloCuentaSesionesRealizadas() {
        val realizada = Entrenamiento(estado = "REALIZADA", asistencia = mapOf("j1" to "TARDE"))
        val enCurso = Entrenamiento(estado = "PROGRAMADA", asistencia = mapOf("j1" to "AUSENTE"))
        val resumen = CalculoAsistencia.resumen("j1", listOf(realizada, enCurso))
        assertEquals(1, resumen.registrados)
        assertEquals(1, resumen.tardes)
    }

    @Test
    fun etiquetasDeLaHistoria() {
        assertEquals("Falta justificada", EstadoAsistencia.JUSTIFICADO.etiqueta)
        assertEquals("Falta injustificada", EstadoAsistencia.AUSENTE.etiqueta)
    }
}
