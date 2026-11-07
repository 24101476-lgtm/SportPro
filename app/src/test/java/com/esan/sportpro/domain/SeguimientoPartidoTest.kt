package com.esan.sportpro.domain

import com.esan.sportpro.domain.partido.EquipoEvento
import com.esan.sportpro.domain.partido.EstadoEvento
import com.esan.sportpro.domain.partido.EstadoPartido
import com.esan.sportpro.domain.partido.EventoPartido
import com.esan.sportpro.domain.partido.JugadorConvocado
import com.esan.sportpro.domain.partido.JugadorEspecial
import com.esan.sportpro.domain.partido.Partido
import com.esan.sportpro.domain.partido.ReglasSeguimiento
import com.esan.sportpro.domain.partido.SituacionJugador
import com.esan.sportpro.domain.partido.TiposEvento
import com.esan.sportpro.navigation.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** US-021: cronología del espectador, estadísticas, alineación y acceso por equipo. */
class SeguimientoPartidoTest {

    private val convocados = (1..14).map { JugadorConvocado("j$it", "Jugador $it", it, titular = it <= 11) }

    private fun evento(
        id: String,
        tipo: String,
        minuto: Int,
        equipo: EquipoEvento? = EquipoEvento.PROPIO,
        jugador: String? = null,
        secundario: String? = null,
        estado: EstadoEvento = EstadoEvento.VIGENTE,
        origen: String? = null,
        periodo: Int? = null,
    ) = EventoPartido(
        id = id, grupoId = id, version = 1, tipoId = tipo, tipoNombre = tipo, minuto = minuto,
        equipo = equipo, jugadorId = jugador, jugadorNombre = jugador,
        jugadorSecundarioId = secundario, jugadorSecundarioNombre = secundario,
        resultadoPenal = null, observacion = null, operadorUid = "uid", operadorNombre = "op",
        registradoEnMs = minuto * 1000L, estado = estado, versionCatalogo = 1,
        incompleto = false, fueraDeOrden = false, excedeCambios = false, eventoOrigenId = origen,
        motivo = null, camposModificados = emptyList(), reemplazadoPor = null, anuladoPor = null,
        anuladoPorNombre = null, anuladoEnMs = null, pendienteSincronizar = false, periodo = periodo,
    )

    private fun partido(equipoId: String? = "eq1") = Partido(
        id = "p1", nombreEquipo = "Halcones", nombreRival = "Tigres", categoria = "Sub-15",
        estado = EstadoPartido.EN_CURSO, alineacionConfirmada = true, cambiosPermitidos = 5,
        duracionTiempoMin = 45, inicioPrimerTiempoMs = 0L, inicioSegundoTiempoMs = null,
        enDescanso = false, operadorUid = null, entrenadorUid = null, creadoPor = null, fechaMs = null,
        equipoId = equipoId,
    )

    // ------------------------------------------------------------------ Cronología

    @Test
    fun cronologia_excluyeAnuladosYReemplazados_yVaDeAntiguoAReciente() {
        val eventos = listOf(
            evento("g2", TiposEvento.GOL, 30),
            evento("g1", TiposEvento.GOL, 10),
            evento("anulado", TiposEvento.FALTA, 15, estado = EstadoEvento.ANULADO),
            evento("viejo", TiposEvento.FALTA, 20, estado = EstadoEvento.REEMPLAZADO),
        )

        assertEquals(listOf("g1", "g2"), ReglasSeguimiento.cronologiaEspectador(eventos).map { it.id })
    }

    // ------------------------------------------------------------------ Estadísticas

    @Test
    fun estadisticas_cuentanFaltasYTarjetasPorEquipoSoloConVigentes() {
        val eventos = listOf(
            evento("f1", TiposEvento.FALTA, 5),
            evento("f2", TiposEvento.FALTA, 6, EquipoEvento.RIVAL, JugadorEspecial.RIVAL),
            evento("f3", TiposEvento.FALTA, 7, EquipoEvento.RIVAL, JugadorEspecial.RIVAL),
            evento("f4", TiposEvento.FALTA, 8, estado = EstadoEvento.ANULADO),
            evento("a1", TiposEvento.TARJETA_AMARILLA, 9, EquipoEvento.RIVAL, JugadorEspecial.RIVAL),
            evento("r1", TiposEvento.TARJETA_ROJA, 40, jugador = "j3"),
        )

        val e = ReglasSeguimiento.estadisticas(eventos)

        assertEquals(1, e.propio.faltas)
        assertEquals(2, e.rival.faltas)
        assertEquals(0, e.propio.amarillas)
        assertEquals(1, e.rival.amarillas)
        assertEquals(1, e.propio.rojas)
        assertEquals(0, e.rival.rojas)
    }

    @Test
    fun estadisticas_tiros_noDuplicanElGolQueNaceDeUnPenal() {
        val eventos = listOf(
            evento("pen", TiposEvento.PENAL, 20, jugador = "j9"),
            evento("golPen", TiposEvento.GOL, 20, jugador = "j9", origen = "pen"),
            evento("gol", TiposEvento.GOL, 33, jugador = "j10"),
            evento("penRival", TiposEvento.PENAL, 50, EquipoEvento.RIVAL, JugadorEspecial.RIVAL),
        )

        val e = ReglasSeguimiento.estadisticas(eventos)

        assertEquals(2, e.propio.tiros)
        assertEquals(1, e.rival.tiros)
    }

    @Test
    fun estadisticas_posesion_repartaEventosDeJuegoSinContarControlesNiCambios() {
        val eventos = listOf(
            evento("ini", TiposEvento.INICIO_PARTIDO, 0, equipo = null),
            evento("c", TiposEvento.CAMBIO, 30, jugador = "j1", secundario = "j12"),
            evento("e1", TiposEvento.TIRO_ESQUINA, 5),
            evento("e2", TiposEvento.TIRO_ESQUINA, 6),
            evento("e3", TiposEvento.TIRO_ESQUINA, 7),
            evento("r1", TiposEvento.SAQUE_LATERAL, 8, EquipoEvento.RIVAL),
        )

        val e = ReglasSeguimiento.estadisticas(eventos)

        assertEquals(3, e.propio.eventosJuego)
        assertEquals(1, e.rival.eventosJuego)
        assertEquals(75, e.propio.posesionPct)
        assertEquals(25, e.rival.posesionPct)
    }

    @Test
    fun estadisticas_sinEventosDeJuego_noInventaPosesion() {
        val e = ReglasSeguimiento.estadisticas(listOf(evento("ini", TiposEvento.INICIO_PARTIDO, 0, equipo = null)))

        assertNull(e.propio.posesionPct)
        assertNull(e.rival.posesionPct)
        assertEquals(0, e.propio.tiros)
    }

    // ------------------------------------------------------------------ Alineación

    private fun situacion(id: String, eventos: List<EventoPartido>) =
        ReglasSeguimiento.alineacion(convocados, eventos).first { it.jugador.id == id }.situacion

    @Test
    fun alineacion_titularesEnCanchaYSuplentesEnBancaAlInicio() {
        assertEquals(SituacionJugador.EN_CANCHA, situacion("j1", emptyList()))
        assertEquals(SituacionJugador.EN_BANCA, situacion("j12", emptyList()))
    }

    @Test
    fun alineacion_cambioMarcaSustituidoEIngreso() {
        val eventos = listOf(evento("c", TiposEvento.CAMBIO, 60, jugador = "j1", secundario = "j12", periodo = 2))

        assertEquals(SituacionJugador.SUSTITUIDO, situacion("j1", eventos))
        assertEquals(SituacionJugador.INGRESO, situacion("j12", eventos))
        assertEquals(SituacionJugador.EN_BANCA, situacion("j13", eventos))
    }

    @Test
    fun alineacion_rojaMarcaExpulsado_yElCambioAnuladoNoCuenta() {
        val eventos = listOf(
            evento("r", TiposEvento.TARJETA_ROJA, 70, jugador = "j3", periodo = 2),
            evento("c", TiposEvento.CAMBIO, 60, jugador = "j2", secundario = "j13", periodo = 2, estado = EstadoEvento.ANULADO),
        )

        assertEquals(SituacionJugador.EXPULSADO, situacion("j3", eventos))
        assertEquals(SituacionJugador.EN_CANCHA, situacion("j2", eventos))
        assertEquals(SituacionJugador.EN_BANCA, situacion("j13", eventos))
    }

    @Test
    fun alineacion_suplenteQueEntroYLuegoSalio_quedaSustituido() {
        val eventos = listOf(
            evento("c1", TiposEvento.CAMBIO, 50, jugador = "j1", secundario = "j12", periodo = 2),
            evento("c2", TiposEvento.CAMBIO, 80, jugador = "j12", secundario = "j13", periodo = 2),
        )

        assertEquals(SituacionJugador.SUSTITUIDO, situacion("j12", eventos))
        assertEquals(SituacionJugador.INGRESO, situacion("j13", eventos))
    }

    @Test
    fun alineacion_ordenaTitularesPrimeroPorDorsal() {
        val orden = ReglasSeguimiento.alineacion(convocados.reversed(), emptyList()).map { it.jugador.id }

        assertEquals("j1", orden.first())
        assertEquals("j14", orden.last())
        assertEquals(11, orden.indexOf("j11") + 1)
    }

    // ------------------------------------------------------------------ Acceso por equipo

    @Test
    fun padreYJugador_soloSiguenPartidosDeSusEquipos() {
        val equipos = setOf("eq1")

        assertTrue(ReglasSeguimiento.puedeSeguir(UserRole.PADRE_DE_FAMILIA, partido("eq1"), equipos))
        assertTrue(ReglasSeguimiento.puedeSeguir(UserRole.JUGADOR, partido("eq1"), equipos))
        assertFalse(ReglasSeguimiento.puedeSeguir(UserRole.PADRE_DE_FAMILIA, partido("eq2"), equipos))
        assertFalse(ReglasSeguimiento.puedeSeguir(UserRole.JUGADOR, partido("eq2"), equipos))
    }

    @Test
    fun sinEquiposPermitidosOSinEquipoEnElPartido_noHayAcceso() {
        assertFalse(ReglasSeguimiento.puedeSeguir(UserRole.PADRE_DE_FAMILIA, partido("eq1"), emptySet()))
        assertFalse(ReglasSeguimiento.puedeSeguir(UserRole.JUGADOR, partido("eq1"), null))
        assertFalse(ReglasSeguimiento.puedeSeguir(UserRole.PADRE_DE_FAMILIA, partido(equipoId = null), setOf("eq1")))
    }

    @Test
    fun administradorYEntrenador_siguenCualquierPartido() {
        assertTrue(ReglasSeguimiento.puedeSeguir(UserRole.ADMINISTRADOR, partido("eq9"), emptySet()))
        assertTrue(ReglasSeguimiento.puedeSeguir(UserRole.ENTRENADOR, partido(null), null))
    }

    @Test
    fun partidosVisibles_filtraPorEquipoDelEspectador() {
        val partidos = listOf(partido("eq1"), partido("eq2").copy(id = "p2"), partido(null).copy(id = "p3"))

        val visibles = ReglasSeguimiento.partidosVisibles(partidos, UserRole.PADRE_DE_FAMILIA, setOf("eq2"))

        assertEquals(listOf("p2"), visibles.map { it.id })
        assertEquals(3, ReglasSeguimiento.partidosVisibles(partidos, UserRole.ADMINISTRADOR, null).size)
    }
}
