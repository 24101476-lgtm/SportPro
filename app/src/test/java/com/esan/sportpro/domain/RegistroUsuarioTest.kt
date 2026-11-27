package com.esan.sportpro.domain

import com.esan.sportpro.ui.registro.MensajesRegistro
import com.esan.sportpro.ui.registro.ValidacionRegistro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** US-001: cada criterio de validación del registro, con el mensaje exacto del criterio. */
class RegistroUsuarioTest {

    @Test
    fun nombreDeUnaLetra_muestraMinimoDosCaracteres() {
        assertEquals(MensajesRegistro.MINIMO_CARACTERES, ValidacionRegistro.validarNombre("A"))
        assertEquals(MensajesRegistro.MINIMO_CARACTERES, ValidacionRegistro.validarNombre(" "))
    }

    @Test
    fun nombreDeDosLetras_esValido() {
        assertNull(ValidacionRegistro.validarNombre("Al"))
        assertNull(ValidacionRegistro.validarNombre("Ana María"))
    }

    @Test
    fun nombreDeMasDeCincuentaLetras_muestraMaximo() {
        assertEquals(MensajesRegistro.MAXIMO_CARACTERES, ValidacionRegistro.validarNombre("a".repeat(51)))
    }

    @Test
    fun nombreConDigitos_muestraSoloLetras() {
        assertEquals(MensajesRegistro.SOLO_LETRAS, ValidacionRegistro.validarNombre("Gabriela2"))
    }

    @Test
    fun correoSinFormato_esInvalido() {
        assertEquals(MensajesRegistro.CORREO_INVALIDO, ValidacionRegistro.validarCorreo("gabriela.com"))
        assertEquals(MensajesRegistro.CORREO_INVALIDO, ValidacionRegistro.validarCorreo("a@b"))
        assertNull(ValidacionRegistro.validarCorreo("gabriela@esan.edu.pe"))
    }

    @Test
    fun passwordSinMayusculaNumeroOEspecial_esRechazada() {
        assertEquals(MensajesRegistro.PASSWORD_DEBE, ValidacionRegistro.validarPassword("corto1!"))
        assertEquals(MensajesRegistro.PASSWORD_DEBE, ValidacionRegistro.validarPassword("sinmayuscula1!"))
        assertEquals(MensajesRegistro.PASSWORD_DEBE, ValidacionRegistro.validarPassword("SinNumero!!"))
        assertEquals(MensajesRegistro.PASSWORD_DEBE, ValidacionRegistro.validarPassword("SinEspecial12"))
        assertNull(ValidacionRegistro.validarPassword("SportPro2026!"))
    }

    @Test
    fun confirmacionDistinta_muestraNoCoinciden() {
        assertEquals(
            MensajesRegistro.PASSWORDS_NO_COINCIDEN,
            ValidacionRegistro.validarConfirmacion("SportPro2026!", "SportPro2026?"),
        )
        assertNull(ValidacionRegistro.validarConfirmacion("SportPro2026!", "SportPro2026!"))
    }

    @Test
    fun codigoDeInvitacion_debeTenerOchoCaracteres() {
        assertEquals(MensajesRegistro.CODIGO_INVALIDO, ValidacionRegistro.validarCodigoInvitacion("ABC123"))
        assertNull(ValidacionRegistro.validarCodigoInvitacion("ABCD2345"))
    }

    @Test
    fun menorDeEdad_seDetectaPorLaFecha() {
        val hoy = LocalDate.now()
        assertTrue(ValidacionRegistro.esMenor(hoy.minusYears(15)))
        assertFalse(ValidacionRegistro.esMenor(hoy.minusYears(20)))
    }
}
