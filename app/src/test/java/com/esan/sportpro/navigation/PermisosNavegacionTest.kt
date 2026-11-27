package com.esan.sportpro.navigation

import com.esan.sportpro.ui.common.MENSAJE_SIN_PERMISO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** US-003: cada rol solo llega a sus secciones; lo demás muestra "No tienes permiso…". */
class PermisosNavegacionTest {

    @Test
    fun jugador_noTieneMensualidadesNiAcademia() {
        assertNull(MenuPrincipal.pestanaDe(UserRole.JUGADOR, HomeModule.MENSUALIDADES))
        assertNull(MenuPrincipal.pestanaDe(UserRole.JUGADOR, HomeModule.ACADEMIA))
        assertNotNull(MenuPrincipal.pestanaDe(UserRole.JUGADOR, HomeModule.ENTRENAMIENTOS))
    }

    @Test
    fun padre_noTieneEntrenamientosNiAcademia() {
        assertNull(MenuPrincipal.pestanaDe(UserRole.PADRE_DE_FAMILIA, HomeModule.ACADEMIA))
        assertNotNull(MenuPrincipal.pestanaDe(UserRole.PADRE_DE_FAMILIA, HomeModule.MENSUALIDADES))
    }

    @Test
    fun administrador_llegaATodosLosModulos() {
        HomeModule.entries.forEach { modulo ->
            assertNotNull(modulo.name, MenuPrincipal.pestanaDe(UserRole.ADMINISTRADOR, modulo))
        }
    }

    @Test
    fun cadaRol_empiezaEnInicioYTieneComoMaximoCuatroPestanas() {
        UserRole.entries.forEach { rol ->
            val pestanas = MenuPrincipal.para(rol)
            assertEquals(TipoPestana.INICIO, pestanas.first().tipo)
            assertTrue(pestanas.size <= MenuPrincipal.MAX_PESTANAS)
        }
    }

    @Test
    fun mensajeSinPermiso_esElDelCriterio() {
        assertEquals("No tienes permiso para ver esta sección", MENSAJE_SIN_PERMISO)
    }
}
