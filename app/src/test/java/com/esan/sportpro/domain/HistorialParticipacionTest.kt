package com.esan.sportpro.domain

import com.esan.sportpro.domain.academia.Equipo
import com.esan.sportpro.domain.cuentas.PerfilActual
import com.esan.sportpro.domain.entrenamientos.Entrenamiento
import com.esan.sportpro.domain.entrenamientos.EstadoAsistencia
import com.esan.sportpro.domain.entrenamientos.EstadoSesion
import com.esan.sportpro.domain.entrenamientos.FiltroHistorial
import com.esan.sportpro.domain.entrenamientos.MarcaAsistencia
import com.esan.sportpro.domain.entrenamientos.ReglasAsistencia
import com.esan.sportpro.domain.entrenamientos.ReglasHistorial
import com.esan.sportpro.domain.entrenamientos.TipoActividad
import com.esan.sportpro.domain.jugadores.EstadoVinculacion
import com.esan.sportpro.domain.jugadores.Jugador
import com.esan.sportpro.domain.jugadores.Vinculacion
import com.esan.sportpro.navigation.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** US-012: historial de participación del jugador. */
class HistorialParticipacionTest {

    private val hoy = LocalDate.of(2026, 11, 1)

    private fun sesion(
        id: String,
        fecha: String,
        estado: EstadoAsistencia?,
        sesion: EstadoSesion = EstadoSesion.REALIZADA,
        minutos: Int? = null,
        hora: String = "16:00",
    ) = Entrenamiento(
        id = id,
        equipoId = "e1",
        equipoNombre = "Barcelona FC · Sub-13",
        fecha = fecha,
        hora = hora,
        objetivo = "Sesión $id",
        estado = sesion.name,
        asistencia = estado?.let { mapOf("j1" to it.name) }.orEmpty(),
        asistenciaDetalle = estado?.let { mapOf("j1" to MarcaAsistencia(estado = it.name, minutosRetraso = minutos)) }.orEmpty(),
    )

    private val sesiones = listOf(
        sesion("s1", "2026-10-28", EstadoAsistencia.PRESENTE),
        sesion("s2", "2026-10-30", EstadoAsistencia.TARDE, minutos = 12),
        sesion("s3", "2026-09-15", EstadoAsistencia.JUSTIFICADO),
        sesion("s4", "2026-08-03", EstadoAsistencia.AUSENTE),
        sesion("s5", "2026-07-10", EstadoAsistencia.PRESENTE),
        sesion("s6", "2026-11-03", EstadoAsistencia.PRESENTE, sesion = EstadoSesion.PROGRAMADA),
        sesion("s7", "2026-10-20", EstadoAsistencia.PRESENTE, sesion = EstadoSesion.CANCELADA),
        sesion("s8", "2026-10-21", estado = null),
    )

    private val registros = ReglasHistorial.desdeEntrenamientos("j1", sesiones)

    @Test
    fun soloCuentanLasSesionesRealizadasEnLasQueFigura() {
        assertEquals(setOf("s1", "s2", "s3", "s4", "s5"), registros.map { it.id }.toSet())
        assertTrue(registros.all { it.tipo == TipoActividad.ENTRENAMIENTO })
    }

    @Test
    fun resumen_porcentajeEsAsistenciasMasTardanzasSobreConvocadas_conUnDecimal() {
        val resumen = ReglasHistorial.resumen(registros)
        assertEquals(5, resumen.convocadas)
        assertEquals(2, resumen.asistencias)
        assertEquals(1, resumen.tardanzas)
        assertEquals(1, resumen.justificadas)
        assertEquals(1, resumen.injustificadas)
        assertEquals(60.0, resumen.porcentaje, 0.0)
        assertEquals(66.7, ReglasHistorial.porcentaje(2, 3), 0.0)
        assertEquals("66.7 %", ReglasAsistencia.formatearPorcentaje(ReglasHistorial.porcentaje(2, 3)))
        assertEquals(0.0, ReglasHistorial.resumen(emptyList()).porcentaje, 0.0)
    }

    @Test
    fun lista_ordenadaDeLaMasRecienteALaMasAntigua_conMinutosDeRetraso() {
        val ordenados = ReglasHistorial.ordenar(registros)
        assertEquals(listOf("s2", "s1", "s3", "s4", "s5"), ordenados.map { it.id })
        assertEquals(12, ordenados.first().minutosRetraso)
        assertNull(ordenados[1].minutosRetraso)
    }

    @Test
    fun mismoDia_ordenaPorHora() {
        val dia = listOf(
            sesion("temprano", "2026-10-28", EstadoAsistencia.PRESENTE, hora = "08:00"),
            sesion("tarde", "2026-10-28", EstadoAsistencia.PRESENTE, hora = "18:00"),
        )
        val ordenados = ReglasHistorial.ordenar(ReglasHistorial.desdeEntrenamientos("j1", dia))
        assertEquals(listOf("tarde", "temprano"), ordenados.map { it.id })
    }

    @Test
    fun filtro_porRangoDeFechasInclusive() {
        val filtro = FiltroHistorial(desde = LocalDate.of(2026, 9, 15), hasta = LocalDate.of(2026, 10, 28))
        assertEquals(setOf("s1", "s3"), ReglasHistorial.filtrar(registros, filtro).map { it.id }.toSet())
    }

    @Test
    fun filtro_porTipoDeActividad() {
        assertEquals(5, ReglasHistorial.filtrar(registros, FiltroHistorial(tipo = TipoActividad.ENTRENAMIENTO)).size)
        assertTrue(ReglasHistorial.filtrar(registros, FiltroHistorial(tipo = TipoActividad.PARTIDO)).isEmpty())
    }

    @Test
    fun periodoSinSesiones_daListaVacia_yElMensajeDeLaHistoria() {
        val filtro = FiltroHistorial(desde = LocalDate.of(2025, 1, 1), hasta = LocalDate.of(2025, 1, 31))
        assertTrue(ReglasHistorial.filtrar(registros, filtro).isEmpty())
        assertEquals("No hay registros en el periodo seleccionado", ReglasHistorial.MENSAJE_VACIO)
    }

    @Test
    fun rangoInvertido_esInvalido() {
        val invertido = FiltroHistorial(desde = LocalDate.of(2026, 10, 1), hasta = LocalDate.of(2026, 9, 1))
        assertEquals(ReglasHistorial.MENSAJE_RANGO_INVALIDO, ReglasHistorial.validarFiltro(invertido))
        assertNull(ReglasHistorial.validarFiltro(FiltroHistorial(desde = LocalDate.of(2026, 10, 1))))
    }

    @Test
    fun grafico_ultimosSeisMesesIncluidoElActual() {
        val meses = ReglasHistorial.mensual(registros, hoy)
        assertEquals(6, meses.size)
        assertEquals(YearMonth.of(2026, 6), meses.first().mes)
        assertEquals(YearMonth.of(2026, 11), meses.last().mes)
        assertEquals(listOf("Jun", "Jul", "Ago", "Set", "Oct", "Nov"), meses.map { it.etiqueta })
        val octubre = meses.first { it.mes == YearMonth.of(2026, 10) }
        assertEquals(2, octubre.convocadas)
        assertEquals(100.0, octubre.porcentaje, 0.0)
        assertEquals(0.0, meses.first { it.mes == YearMonth.of(2026, 8) }.porcentaje, 0.0)
        assertEquals(0, meses.last().convocadas)
    }

    @Test
    fun grafico_mesesDeOtroAnioLlevanElAnio() {
        val meses = ReglasHistorial.mensual(emptyList(), LocalDate.of(2027, 2, 10))
        assertEquals(listOf("Set 26", "Oct 26", "Nov 26", "Dic 26", "Ene", "Feb"), meses.map { it.etiqueta })
    }

    // ---- Criterio 5: quién ve el historial ---------------------------------------------------

    private val jugador = Jugador(id = "j1", academiaId = "a1", equipoId = "e1", usuarioUid = "uJugador")
    private val equipo = Equipo(id = "e1", academiaId = "a1", entrenadorNombre = "Carlos Ramírez")

    private fun perfil(rol: UserRole, uid: String = "u1", nombre: String = "Alguien") =
        PerfilActual(uid = uid, nombre = nombre, rol = rol, academiaId = "a1")

    @Test
    fun jugador_soloVeSuPropioHistorial() {
        assertTrue(ReglasHistorial.puedeVer(perfil(UserRole.JUGADOR, uid = "uJugador"), jugador, equipo))
        assertFalse(ReglasHistorial.puedeVer(perfil(UserRole.JUGADOR, uid = "otro"), jugador, equipo))
        assertFalse(ReglasHistorial.puedeVer(perfil(UserRole.JUGADOR, uid = ""), jugador.copy(usuarioUid = ""), equipo))
    }

    @Test
    fun padre_soloConVinculoAprobado() {
        val padre = perfil(UserRole.PADRE_DE_FAMILIA, uid = "p1")
        val aprobada = Vinculacion(padreUid = "p1", jugadorId = "j1", estado = EstadoVinculacion.APROBADA.name)
        assertTrue(ReglasHistorial.puedeVer(padre, jugador, equipo, listOf(aprobada)))
        assertFalse(ReglasHistorial.puedeVer(padre, jugador, equipo, listOf(aprobada.copy(estado = EstadoVinculacion.PENDIENTE.name))))
        assertFalse(ReglasHistorial.puedeVer(padre, jugador, equipo, listOf(aprobada.copy(padreUid = "otroPadre"))))
    }

    @Test
    fun entrenador_soloLosEquiposQueDirige() {
        assertTrue(ReglasHistorial.puedeVer(perfil(UserRole.ENTRENADOR, nombre = "carlos  ramirez"), jugador, equipo))
        assertFalse(ReglasHistorial.puedeVer(perfil(UserRole.ENTRENADOR, nombre = "Otro DT"), jugador, equipo))
        // Equipo sin entrenador asignado: lo ve cualquier entrenador de la academia.
        assertTrue(ReglasHistorial.puedeVer(perfil(UserRole.ENTRENADOR, nombre = "Otro DT"), jugador, equipo.copy(entrenadorNombre = "")))
        assertFalse(ReglasHistorial.puedeVer(perfil(UserRole.ENTRENADOR), jugador, null))
    }

    @Test
    fun administrador_veLaAcademia() {
        assertTrue(ReglasHistorial.puedeVer(perfil(UserRole.ADMINISTRADOR), jugador, equipo))
        assertFalse(ReglasHistorial.puedeVer(perfil(UserRole.ADMINISTRADOR), jugador.copy(academiaId = "otra"), equipo))
    }
}
