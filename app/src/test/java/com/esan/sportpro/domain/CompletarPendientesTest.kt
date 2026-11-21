package com.esan.sportpro.domain

import com.esan.sportpro.data.notificaciones.TemasPush
import com.esan.sportpro.domain.academia.CodigoInvitacion
import com.esan.sportpro.domain.academia.ConfiguracionAcademia
import com.esan.sportpro.domain.academia.Equipo
import com.esan.sportpro.domain.cuentas.PerfilActual
import com.esan.sportpro.domain.mensualidades.EstadoMensualidad
import com.esan.sportpro.navigation.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Date

/** Lógica agregada al completar las HU pendientes (US-004, US-008, US-027 y enlace de entrenadores). */
class CompletarPendientesTest {

    // ---- US-004 / US-008: día de corte configurable ----------------------------------------

    @Test
    fun diaDeCorte_configurado_cambiaCuandoVence() {
        val hoy = LocalDate.of(2026, 11, 12)
        // Con el corte por defecto (10) ya venció; con corte el 15 todavía está pendiente.
        assertEquals(EstadoMensualidad.VENCIDO, EstadoMensualidad.efectivo(EstadoMensualidad.PENDIENTE, 11, 2026, hoy))
        assertEquals(
            EstadoMensualidad.PENDIENTE,
            EstadoMensualidad.efectivo(EstadoMensualidad.PENDIENTE, 11, 2026, hoy, diaCorte = 15),
        )
    }

    @Test
    fun diaDeCorte_seAjustaAMesesCortos() {
        assertEquals(LocalDate.of(2026, 2, 28), EstadoMensualidad.fechaCorte(2, 2026, diaCorte = 31))
    }

    @Test
    fun configuracionAcademia_valida() {
        assertNull(ConfiguracionAcademia.validar("Academia Lima", 10))
        assertNotNull(ConfiguracionAcademia.validar("AB", 10))
        assertNotNull(ConfiguracionAcademia.validar("Academia Lima", 0))
        assertNotNull(ConfiguracionAcademia.validar("Academia Lima", 29))
        assertNotNull(ConfiguracionAcademia.validar("Academia Lima", null))
    }

    @Test
    fun codigoInvitacion_vigenteSoloSinUsarNiRevocarNiVencer() {
        val manana = Date(System.currentTimeMillis() + 86_400_000)
        val ayer = Date(System.currentTimeMillis() - 86_400_000)
        assertTrue(CodigoInvitacion("ABCDEFGH", null, manana, usado = false, revocado = false).vigente())
        assertFalse(CodigoInvitacion("ABCDEFGH", null, manana, usado = true, revocado = false).vigente())
        assertFalse(CodigoInvitacion("ABCDEFGH", null, manana, usado = false, revocado = true).vigente())
        assertFalse(CodigoInvitacion("ABCDEFGH", null, ayer, usado = false, revocado = false).vigente())
    }

    @Test
    fun clubId_nuloParaLaAcademiaPorDefecto() {
        assertNull(PerfilActual.clubIdDe(PerfilActual.ACADEMIA_POR_DEFECTO))
        assertEquals("club-1", PerfilActual.clubIdDe("club-1"))
    }

    // ---- US-008: entrenador asignado al equipo por cuenta -----------------------------------

    @Test
    fun equipo_dirigidoPorCuenta() {
        val equipo = Equipo(id = "e1", entrenadorUid = "uidHugo", entrenadorNombre = "Hugo Flores")
        assertTrue(equipo.dirigidoPor("uidHugo", "Otro nombre"))
        // Con cuenta asignada, el nombre ya no basta.
        assertFalse(equipo.dirigidoPor("uidOtro", "Hugo Flores"))
    }

    @Test
    fun equipoAntiguo_sinCuenta_secomparaPorNombre() {
        val equipo = Equipo(id = "e1", entrenadorNombre = "Hugo Flóres")
        assertTrue(equipo.dirigidoPor("cualquiera", "hugo  flores"))
        assertFalse(Equipo(id = "e2").dirigidoPor("cualquiera", "Hugo Flores"))
        assertTrue(Equipo(id = "e2").sinEntrenador())
    }

    // ---- US-027: temas de FCM (deben coincidir con supabase/functions/notificar) ------------

    @Test
    fun temasPush_formato() {
        assertEquals("academia_academia-demo", TemasPush.academia("academia-demo"))
        assertEquals("academia_academia-demo_rol_JUGADOR", TemasPush.rol("academia-demo", UserRole.JUGADOR))
        assertEquals("equipo_abc123", TemasPush.equipo("abc123"))
        // Caracteres no permitidos por FCM se reemplazan.
        assertEquals("academia_mi-club", TemasPush.academia("mi club"))
    }
}
