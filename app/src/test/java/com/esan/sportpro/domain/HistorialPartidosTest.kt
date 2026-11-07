package com.esan.sportpro.domain

import com.esan.sportpro.domain.entrenamientos.EstadoAsistencia
import com.esan.sportpro.domain.entrenamientos.FiltroHistorial
import com.esan.sportpro.domain.entrenamientos.ReglasHistorial
import com.esan.sportpro.domain.entrenamientos.TipoActividad
import com.esan.sportpro.domain.partido.EquipoEvento
import com.esan.sportpro.domain.partido.EstadoEvento
import com.esan.sportpro.domain.partido.EstadoPartido
import com.esan.sportpro.domain.partido.EstadoRespuestaConvocatoria
import com.esan.sportpro.domain.partido.EventoPartido
import com.esan.sportpro.domain.partido.JugadorConvocado
import com.esan.sportpro.domain.partido.Partido
import com.esan.sportpro.domain.partido.RespuestaConvocatoria
import com.esan.sportpro.domain.partido.TiposEvento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** US-012, criterio 3: los partidos terminados también forman parte del historial. */
class HistorialPartidosTest {

    private val zona = ZoneId.of("America/Lima")

    /** 2026-10-31 18:30 en Lima. */
    private val fechaMs = LocalDate.of(2026, 10, 31).atTime(18, 30).atZone(zona).toInstant().toEpochMilli()

    private val alineacion = listOf(
        JugadorConvocado("titular", "Luis Pérez", 9, titular = true),
        JugadorConvocado("cambiado", "Juan Ríos", 10, titular = true),
        JugadorConvocado("entra", "Ana Soto", 14, titular = false),
        JugadorConvocado("banca", "Pedro Díaz", 18, titular = false),
    )

    private fun respuesta(id: String, estado: EstadoRespuestaConvocatoria, motivo: String? = null) =
        RespuestaConvocatoria(jugadorId = id, nombre = id, estado = estado, motivo = motivo)

    private val respuestas = listOf(
        respuesta("titular", EstadoRespuestaConvocatoria.CONFIRMADO),
        respuesta("lesionado", EstadoRespuestaConvocatoria.NO_DISPONIBLE, "Lesión de tobillo"),
        respuesta("silencio", EstadoRespuestaConvocatoria.SIN_RESPONDER),
        respuesta("descartado", EstadoRespuestaConvocatoria.CONFIRMADO),
    )

    private fun evento(id: String, tipo: String, minuto: Int, jugador: String? = null, secundario: String? = null, periodo: Int? = null) =
        EventoPartido(
            id = id, grupoId = id, version = 1, tipoId = tipo, tipoNombre = tipo, minuto = minuto,
            equipo = if (tipo == TiposEvento.FIN_PARTIDO) null else EquipoEvento.PROPIO,
            jugadorId = jugador, jugadorNombre = jugador,
            jugadorSecundarioId = secundario, jugadorSecundarioNombre = secundario,
            resultadoPenal = null, observacion = null, operadorUid = "uid", operadorNombre = "Operador",
            registradoEnMs = minuto * 1000L, estado = EstadoEvento.VIGENTE, versionCatalogo = 1,
            incompleto = false, fueraDeOrden = false, excedeCambios = false, eventoOrigenId = null,
            motivo = null, camposModificados = emptyList(), reemplazadoPor = null, anuladoPor = null,
            anuladoPorNombre = null, anuladoEnMs = null, pendienteSincronizar = false, periodo = periodo,
        )

    private val eventos = listOf(
        evento("cambio", TiposEvento.CAMBIO, 60, jugador = "cambiado", secundario = "entra", periodo = 2),
        evento("fin", TiposEvento.FIN_PARTIDO, 90, periodo = 2),
    )

    private fun partido(estado: EstadoPartido = EstadoPartido.ACTA_CERRADA, fecha: Long? = fechaMs, hora: String? = null) = Partido(
        id = "p1", nombreEquipo = "Barcelona FC", nombreRival = "Tigres", categoria = "Sub-13",
        estado = estado, alineacionConfirmada = true, cambiosPermitidos = 5, duracionTiempoMin = 45,
        inicioPrimerTiempoMs = 1_000L, inicioSegundoTiempoMs = null, enDescanso = false,
        operadorUid = null, entrenadorUid = null, creadoPor = null, fechaMs = fecha, hora = hora,
        golesEquipo = 2, golesRival = 1,
    )

    private fun registro(jugadorId: String, partido: Partido = partido()) =
        ReglasHistorial.desdePartido(jugadorId, partido, alineacion, respuestas, eventos, zona)

    @Test
    fun titular_presenteConLosMinutosDelActa() {
        val r = registro("titular")!!
        assertEquals(TipoActividad.PARTIDO, r.tipo)
        assertEquals(EstadoAsistencia.PRESENTE, r.estado)
        assertEquals(90, r.minutosJugados)
        assertEquals(LocalDate.of(2026, 10, 31), r.fecha)
        assertEquals("18:30", r.hora)
        assertEquals("Barcelona FC · Sub-13", r.equipoNombre)
        assertEquals("vs Tigres · 2-1 · Jugó 90 min", r.descripcion)
    }

    @Test
    fun cambio_reparteLosMinutos() {
        assertEquals(60, registro("cambiado")!!.minutosJugados)
        assertEquals(30, registro("entra")!!.minutosJugados)
    }

    @Test
    fun suplenteQueNoIngreso_cuentaComoPresenteConCeroMinutos() {
        val r = registro("banca")!!
        assertEquals(EstadoAsistencia.PRESENTE, r.estado)
        assertEquals(0, r.minutosJugados)
        assertEquals("vs Tigres · 2-1 · Suplente, no ingresó", r.descripcion)
    }

    @Test
    fun noDisponible_esFaltaJustificadaConMotivo() {
        val r = registro("lesionado")!!
        assertEquals(EstadoAsistencia.JUSTIFICADO, r.estado)
        assertNull(r.minutosJugados)
        assertEquals("vs Tigres · 2-1 · No disponible: Lesión de tobillo", r.descripcion)
    }

    @Test
    fun sinResponderYFueraDeLaAlineacion_esFaltaInjustificada() {
        assertEquals(EstadoAsistencia.AUSENTE, registro("silencio")!!.estado)
    }

    @Test
    fun confirmadoQueElEntrenadorNoIncluyo_noCuenta() {
        assertNull(registro("descartado"))
    }

    @Test
    fun jugadorAjenoAlPartido_noCuenta() {
        assertNull(registro("otro"))
    }

    @Test
    fun soloPartidosTerminados() {
        assertEquals(EstadoAsistencia.PRESENTE, registro("titular", partido(EstadoPartido.FINALIZADO))!!.estado)
        assertNull(registro("titular", partido(EstadoPartido.PROGRAMADO)))
        assertNull(registro("titular", partido(EstadoPartido.EN_CURSO)))
        assertNull(registro("titular", partido(EstadoPartido.CANCELADO)))
        assertNull(registro("titular", partido(fecha = null)))
    }

    @Test
    fun horaProgramada_tienePrioridad() {
        assertEquals("16:00", registro("titular", partido(hora = "16:00"))!!.hora)
    }

    @Test
    fun partidosYEntrenamientos_seFiltranYCuentanJuntos() {
        val registros = listOfNotNull(registro("titular"), registro("lesionado"))
        val resumen = ReglasHistorial.resumen(ReglasHistorial.filtrar(registros, FiltroHistorial(tipo = TipoActividad.PARTIDO)))
        assertEquals(2, resumen.convocadas)
        assertEquals(1, resumen.asistencias)
        assertEquals(1, resumen.justificadas)
        assertEquals(50.0, resumen.porcentaje, 0.0)
        assertEquals(0, ReglasHistorial.filtrar(registros, FiltroHistorial(tipo = TipoActividad.ENTRENAMIENTO)).size)
    }
}
