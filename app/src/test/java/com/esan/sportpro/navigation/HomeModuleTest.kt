package com.esan.sportpro.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** US-003: cada rol ve únicamente los módulos que le corresponden. */
class HomeModuleTest {

    @Test
    fun administrador_veTodoElProducto() {
        assertEquals(HomeModule.entries.toList(), HomeModule.visiblesPara(UserRole.ADMINISTRADOR))
    }

    @Test
    fun entrenador_gestionaPlantelPeroNoMensualidades() {
        val modulos = HomeModule.visiblesPara(UserRole.ENTRENADOR)
        assertTrue(HomeModule.JUGADORES in modulos)
        assertTrue(HomeModule.ENTRENAMIENTOS in modulos)
        assertTrue(HomeModule.PARTIDO in modulos)
        assertFalse(HomeModule.MENSUALIDADES in modulos)
    }

    @Test
    fun jugador_noAccedeAJugadoresNiMensualidadesNiAcademia() {
        val modulos = HomeModule.visiblesPara(UserRole.JUGADOR)
        assertFalse(HomeModule.JUGADORES in modulos)
        assertFalse(HomeModule.MENSUALIDADES in modulos)
        assertFalse(HomeModule.ACADEMIA in modulos)
        assertTrue(HomeModule.ENTRENAMIENTOS in modulos)
    }

    @Test
    fun padre_soloVeMensualidadesPartidoEstadisticasYComunidad() {
        assertEquals(
            listOf(HomeModule.MENSUALIDADES, HomeModule.PARTIDO, HomeModule.ESTADISTICAS, HomeModule.COMUNIDAD),
            HomeModule.visiblesPara(UserRole.PADRE_DE_FAMILIA),
        )
    }

    @Test
    fun todosLosRoles_tienenAlMenosUnModulo() {
        UserRole.entries.forEach { rol ->
            assertTrue("El rol $rol no tiene módulos", HomeModule.visiblesPara(rol).isNotEmpty())
        }
    }

    @Test
    fun soloAdministradorYEntrenador_veLaIa() {
        assertTrue(HomeModule.IA in HomeModule.visiblesPara(UserRole.ADMINISTRADOR))
        assertTrue(HomeModule.IA in HomeModule.visiblesPara(UserRole.ENTRENADOR))
        assertFalse(HomeModule.IA in HomeModule.visiblesPara(UserRole.JUGADOR))
        assertFalse(HomeModule.IA in HomeModule.visiblesPara(UserRole.PADRE_DE_FAMILIA))
    }
}
