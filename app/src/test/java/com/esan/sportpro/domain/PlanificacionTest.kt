package com.esan.sportpro.domain

import com.esan.sportpro.domain.entrenamientos.CampoEntrenamiento
import com.esan.sportpro.domain.entrenamientos.DatosEntrenamiento
import com.esan.sportpro.domain.entrenamientos.Ejercicio
import com.esan.sportpro.domain.entrenamientos.Entrenamiento
import com.esan.sportpro.domain.entrenamientos.EstadoSesion
import com.esan.sportpro.domain.entrenamientos.ReglasPlanificacion
import com.esan.sportpro.domain.entrenamientos.ValidacionEntrenamiento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** US-010: planificación de sesiones de entrenamiento. */
class PlanificacionTest {

    private val hoy = LocalDate.of(2026, 11, 1)
    private val rondo = Ejercicio("Rondo", "", 20, ejercicioId = "b1")
    private val pase = Ejercicio("Pase", "", 30, ejercicioId = "b2")
    private val trote = Ejercicio("Trote", "", 10, ejercicioId = "b3")

    private fun datos() = DatosEntrenamiento(
        equipoId = "e1",
        fecha = "2026-11-05",
        hora = "16:00",
        duracion = "90",
        objetivo = "Salida con balón",
        ejercicios = listOf(rondo, pase),
    )

    @Test
    fun formulario_pideDuracionTotalEnRango() {
        assertTrue(ValidacionEntrenamiento.validar(datos(), hoy).isEmpty())
        listOf("", "10", "300", "noventa").forEach { duracion ->
            assertTrue("duración '$duracion'", CampoEntrenamiento.DURACION in ValidacionEntrenamiento.validar(datos().copy(duracion = duracion), hoy))
        }
    }

    @Test
    fun fechaPasada_conElMensajeDeLaHistoria() {
        val errores = ValidacionEntrenamiento.validar(datos().copy(fecha = "2026-10-31"), hoy)
        assertEquals("No puedes programar una sesión en una fecha pasada", errores[CampoEntrenamiento.FECHA])
        assertTrue(ValidacionEntrenamiento.validar(datos().copy(fecha = "2026-11-01"), hoy).isEmpty())
    }

    @Test
    fun sinEjercicios_conElMensajeDeLaHistoria() {
        val errores = ValidacionEntrenamiento.validar(datos().copy(ejercicios = emptyList()), hoy)
        assertEquals("Agrega al menos un ejercicio", errores[CampoEntrenamiento.EJERCICIOS])
    }

    @Test
    fun excesoDeDuracion_esAvisoConLosMinutos() {
        val largos = datos().copy(duracion = "45", ejercicios = listOf(rondo, pase))
        assertEquals(5, ReglasPlanificacion.excesoMinutos(largos))
        assertEquals("Los ejercicios superan la duración de la sesión en 5 minutos", ReglasPlanificacion.avisoExceso(largos))
        assertTrue(ValidacionEntrenamiento.validar(largos, hoy).isEmpty())
        assertNull(ReglasPlanificacion.avisoExceso(datos()))
    }

    @Test
    fun reordenarYAjustarDuracion_soloEnLaSesion() {
        val lista = listOf(rondo, pase, trote)
        assertEquals(listOf(pase, trote, rondo), ReglasPlanificacion.mover(lista, 0, 2))
        assertEquals(listOf(trote, rondo, pase), ReglasPlanificacion.mover(lista, 2, 0))
        assertEquals(lista, ReglasPlanificacion.mover(lista, 0, 5))
        val ajustada = ReglasPlanificacion.ajustarDuracion(lista, 1, 25)
        assertEquals(25, ajustada[1].duracionMin)
        assertEquals("b2", ajustada[1].ejercicioId)
        assertEquals(30, pase.duracionMin) // el original (biblioteca) no cambia
    }

    @Test
    fun solapamiento_mismoEquipoYHorarioCruzado() {
        val existente = Entrenamiento(id = "s1", equipoId = "e1", fecha = "2026-11-05", hora = "17:00", duracionPlanificadaMin = 60)
        val sesiones = listOf(
            existente,
            Entrenamiento(id = "s2", equipoId = "e2", fecha = "2026-11-05", hora = "16:00", duracionPlanificadaMin = 90),
            Entrenamiento(id = "s3", equipoId = "e1", fecha = "2026-11-05", hora = "16:00", duracionPlanificadaMin = 90, estado = EstadoSesion.CANCELADA.name),
        )
        // 16:00-17:30 se cruza con 17:00-18:00 del mismo equipo.
        assertEquals("s1", ReglasPlanificacion.sesionSolapada("e1", "2026-11-05", "16:00", 90, sesiones)?.id)
        // 16:00-17:00 solo toca el inicio de la otra: no se solapa.
        assertNull(ReglasPlanificacion.sesionSolapada("e1", "2026-11-05", "16:00", 60, sesiones))
        // Al editar la propia sesión no choca consigo misma.
        assertNull(ReglasPlanificacion.sesionSolapada("e1", "2026-11-05", "17:00", 60, sesiones, idEditando = "s1"))
        // Otro día, sin solapamiento.
        assertNull(ReglasPlanificacion.sesionSolapada("e1", "2026-11-06", "17:00", 60, sesiones))
    }

    @Test
    fun sesionesAntiguas_usanLaSumaDeEjerciciosComoDuracion() {
        val antigua = Entrenamiento(ejercicios = listOf(rondo, pase))
        assertEquals(50, antigua.duracionSesionMin)
        assertEquals(90, antigua.copy(duracionPlanificadaMin = 90).duracionSesionMin)
    }

    @Test
    fun duplicar_copiaEjerciciosObjetivoYDuraciones_yNaceProgramada() {
        val original = Entrenamiento(
            id = "s1", academiaId = "a1", equipoId = "e1", equipoNombre = "Barcelona FC · Sub-13",
            fecha = "2026-11-05", hora = "16:00", lugar = "Cancha 2", objetivo = "Salida con balón",
            ejercicios = listOf(rondo, pase), duracionPlanificadaMin = 90,
            estado = EstadoSesion.REALIZADA.name, asistencia = mapOf("j1" to "PRESENTE"), asistenciaRegistrada = true,
        )
        val copia = ReglasPlanificacion.duplicar(original, "2026-11-12", creadoPor = "dt1")
        assertEquals("", copia.id)
        assertEquals("2026-11-12", copia.fecha)
        assertEquals(original.ejercicios, copia.ejercicios)
        assertEquals("Salida con balón", copia.objetivo)
        assertEquals(90, copia.duracionPlanificadaMin)
        assertEquals(EstadoSesion.PROGRAMADA, copia.estadoSesion)
        assertTrue(copia.asistencia.isEmpty())
        assertEquals("No puedes programar una sesión en una fecha pasada", ReglasPlanificacion.validarFechaDuplicado("2026-10-01", hoy))
        assertNull(ReglasPlanificacion.validarFechaDuplicado("2026-11-12", hoy))
    }

    @Test
    fun cancelar_exigeMotivoYSesionProgramada() {
        val programada = Entrenamiento(estado = EstadoSesion.PROGRAMADA.name)
        assertNull(ReglasPlanificacion.validarCancelacion(programada, "Lluvia intensa en la cancha"))
        assertNotNull(ReglasPlanificacion.validarCancelacion(programada, "Lluvia"))
        assertNotNull(ReglasPlanificacion.validarCancelacion(programada, "x".repeat(201)))
        assertNotNull(ReglasPlanificacion.validarCancelacion(programada.copy(estado = EstadoSesion.REALIZADA.name), "Lluvia intensa en la cancha"))
    }
}
