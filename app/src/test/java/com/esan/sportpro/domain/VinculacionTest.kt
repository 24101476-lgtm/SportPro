package com.esan.sportpro.domain

import com.esan.sportpro.domain.jugadores.DirectorioJugador
import com.esan.sportpro.domain.jugadores.EstadoVinculacion
import com.esan.sportpro.domain.jugadores.ProblemaSolicitud
import com.esan.sportpro.domain.jugadores.ReglasVinculacion
import com.esan.sportpro.domain.jugadores.Vinculacion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date
import kotlin.random.Random

/** US-007: reglas de la vinculación padre ↔ jugador. */
class VinculacionTest {

    private val jugador = DirectorioJugador(jugadorId = "j1", nombre = "Luis Pérez", equipoNombre = "Barcelona FC · Sub-13", codigo = "K7M2QX")

    private fun vinculo(estado: EstadoVinculacion, jugadorId: String = "j1", padre: String = "p1", solicitada: Long = 0) =
        Vinculacion(
            id = ReglasVinculacion.idVinculacion(padre, jugadorId),
            padreUid = padre,
            jugadorId = jugadorId,
            estado = estado.name,
            solicitadaEn = Date(solicitada),
        )

    @Test
    fun idDelVinculo_esPadreGuionBajoJugador() {
        assertEquals("p1_j1", ReglasVinculacion.idVinculacion("p1", "j1"))
    }

    @Test
    fun codigo_tieneSeisCaracteres_yAceptaMinusculasYEspacios() {
        assertEquals("K7M2QX", ReglasVinculacion.normalizarCodigo(" k7m-2qx "))
        assertNull(ReglasVinculacion.validarCodigo("k7m2qx"))
        assertEquals("El código tiene 6 caracteres", ReglasVinculacion.validarCodigo("K7M2"))
        // O, 0, I, 1 y L no se usan para evitar confusiones al dictar el código.
        assertEquals("El código no es válido", ReglasVinculacion.validarCodigo("K7M2Q0"))
    }

    @Test
    fun codigoGenerado_esValido_yNoSeRepite() {
        val random = Random(42)
        val usados = mutableSetOf<String>()
        repeat(200) {
            val codigo = ReglasVinculacion.generarCodigo(usados, random)
            assertNull(ReglasVinculacion.validarCodigo(codigo))
            assertFalse(codigo in usados)
            usados += codigo
        }
    }

    @Test
    fun terceraVinculacion_esRechazada() {
        val conDos = jugador.copy(apoderadosAprobados = 2)
        assertEquals(ProblemaSolicitud.MAXIMO_APODERADOS, ReglasVinculacion.problemaParaSolicitar(conDos, existente = null))
        assertEquals("El jugador ya tiene el máximo de apoderados vinculados", ProblemaSolicitud.MAXIMO_APODERADOS.mensaje)
        assertNull(ReglasVinculacion.problemaParaSolicitar(jugador.copy(apoderadosAprobados = 1), existente = null))
    }

    @Test
    fun noSeDuplicanSolicitudes_peroSePuedeReenviarTrasRechazoORevocacion() {
        assertEquals(ProblemaSolicitud.YA_SOLICITADA, ReglasVinculacion.problemaParaSolicitar(jugador, vinculo(EstadoVinculacion.PENDIENTE)))
        assertEquals(ProblemaSolicitud.YA_VINCULADO, ReglasVinculacion.problemaParaSolicitar(jugador, vinculo(EstadoVinculacion.APROBADA)))
        assertNull(ReglasVinculacion.problemaParaSolicitar(jugador, vinculo(EstadoVinculacion.RECHAZADA)))
        assertNull(ReglasVinculacion.problemaParaSolicitar(jugador, vinculo(EstadoVinculacion.REVOCADA)))
    }

    @Test
    fun jugadorInactivo_noAceptaSolicitudes() {
        assertEquals(ProblemaSolicitud.JUGADOR_INACTIVO, ReglasVinculacion.problemaParaSolicitar(jugador.copy(activo = false), null))
    }

    @Test
    fun aprobar_seVuelveAComprobarElMaximo() {
        assertTrue(ReglasVinculacion.puedeAprobar(jugador.copy(apoderadosAprobados = 1)))
        assertFalse(ReglasVinculacion.puedeAprobar(jugador.copy(apoderadosAprobados = 2)))
        assertFalse(ReglasVinculacion.puedeAprobar(null))
    }

    @Test
    fun motivoDeRechazo_entre10y200Caracteres() {
        assertTrue(ReglasVinculacion.validarMotivoRechazo("No es el apoderado") == null)
        assertTrue(ReglasVinculacion.validarMotivoRechazo("Corto") != null)
        assertTrue(ReglasVinculacion.validarMotivoRechazo("x".repeat(201)) != null)
        assertTrue(ReglasVinculacion.validarMotivoRechazo("x".repeat(200)) == null)
    }

    @Test
    fun bandeja_soloPendientes_delMasAntiguoAlMasReciente() {
        val lista = listOf(
            vinculo(EstadoVinculacion.PENDIENTE, jugadorId = "j2", solicitada = 300),
            vinculo(EstadoVinculacion.APROBADA, jugadorId = "j3", solicitada = 100),
            vinculo(EstadoVinculacion.PENDIENTE, jugadorId = "j1", solicitada = 200),
        )
        assertEquals(listOf("j1", "j2"), ReglasVinculacion.pendientes(lista).map { it.jugadorId })
    }

    @Test
    fun elPadreSoloVeJugadoresConVinculoAprobado() {
        val lista = listOf(
            vinculo(EstadoVinculacion.APROBADA, jugadorId = "j1"),
            vinculo(EstadoVinculacion.PENDIENTE, jugadorId = "j2"),
            vinculo(EstadoVinculacion.RECHAZADA, jugadorId = "j3"),
            vinculo(EstadoVinculacion.REVOCADA, jugadorId = "j4"),
        )
        assertEquals(listOf("j1"), ReglasVinculacion.jugadoresAprobados(lista))
    }

    @Test
    fun estadoDesconocido_seTrataComoPendiente() {
        assertEquals(EstadoVinculacion.PENDIENTE, Vinculacion(estado = "").estadoVinculacion)
        assertFalse(Vinculacion(estado = "").aprobada)
    }
}
