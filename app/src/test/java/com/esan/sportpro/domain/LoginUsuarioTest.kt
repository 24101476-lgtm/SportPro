package com.esan.sportpro.domain

import com.esan.sportpro.ui.login.CampoLogin
import com.esan.sportpro.ui.login.MensajesLogin
import com.esan.sportpro.ui.login.ValidacionLogin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** US-002: campos obligatorios, mensajes exactos y bloqueo por intentos fallidos. */
class LoginUsuarioTest {

    @Test
    fun camposVacios_muestranCampoObligatorio() {
        val errores = ValidacionLogin.validarCampos(listOf("", " "))
        assertEquals(MensajesLogin.CAMPO_OBLIGATORIO, errores[CampoLogin.CORREO])
        assertEquals(MensajesLogin.CAMPO_OBLIGATORIO, errores[CampoLogin.PASSWORD])
    }

    @Test
    fun camposLlenos_noTienenErrores() {
        assertTrue(ValidacionLogin.validarCampos(listOf("gabriela@esan.edu.pe", "SportPro2026!")).isEmpty())
    }

    @Test
    fun credencialesIncorrectas_noRevelanQueCampoFallo() {
        assertEquals("Correo o contraseña incorrectos", MensajesLogin.CREDENCIALES_INCORRECTAS)
    }

    @Test
    fun recuperacion_usaElTextoExactoDelCriterio() {
        assertEquals(
            "Si el correo está registrado recibirás un enlace de recuperación",
            MensajesLogin.RECUPERACION_ENVIADA,
        )
        assertEquals(MensajesLogin.CAMPO_OBLIGATORIO, ValidacionLogin.validarCorreo(""))
        assertNull(ValidacionLogin.validarCorreo("gabriela@esan.edu.pe"))
    }

    @Test
    fun bloqueo_esDeTresIntentosYTreintaSegundos() {
        assertEquals(3, MensajesLogin.MAX_INTENTOS_FALLIDOS)
        assertEquals(30L, MensajesLogin.SEGUNDOS_BLOQUEO)
        assertTrue(MensajesLogin.mensajeBloqueo(12).contains("12 segundos"))
    }

    @Test
    fun cuentaDesactivada_usaElMensajeDelCriterio() {
        assertEquals("Cuenta desactivada. Contacta al administrador de tu academia", MensajesLogin.CUENTA_DESACTIVADA)
    }
}
