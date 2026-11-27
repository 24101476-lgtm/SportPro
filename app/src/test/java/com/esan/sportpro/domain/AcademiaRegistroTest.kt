package com.esan.sportpro.domain

import com.esan.sportpro.domain.academia.CampoAcademia
import com.esan.sportpro.domain.academia.CodigoInvitacion
import com.esan.sportpro.domain.academia.ConfiguracionAcademia
import com.esan.sportpro.domain.academia.EstadoCodigo
import com.esan.sportpro.domain.academia.MensajesAcademia
import com.esan.sportpro.domain.academia.ValidacionAcademia
import com.esan.sportpro.navigation.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/** US-004: cada criterio del registro y configuración de la academia. */
class AcademiaRegistroTest {

    private val valida = ConfiguracionAcademia(
        id = "a1",
        nombre = "Academia Lima Norte",
        ciudad = "Lima",
        distrito = "Los Olivos",
        direccion = "Av. Universitaria 1234",
        telefono = "987654321",
        correo = "contacto@limanorte.pe",
        anioFundacion = 2015,
    )

    private fun validar(datos: ConfiguracionAcademia = valida, anio: String = "2015", dia: String = "10") =
        ValidacionAcademia.validar(datos, anio, dia, anioActual = 2026)

    @Test
    fun academiaCompleta_noTieneErrores() {
        assertTrue(validar().isEmpty())
    }

    @Test
    fun nombre_debeTenerEntre3y80Caracteres() {
        assertEquals(MensajesAcademia.NOMBRE_LARGO, validar(valida.copy(nombre = "AB"))[CampoAcademia.NOMBRE])
        assertEquals(MensajesAcademia.NOMBRE_LARGO, validar(valida.copy(nombre = "x".repeat(81)))[CampoAcademia.NOMBRE])
        assertTrue(validar(valida.copy(nombre = "x".repeat(80))).isEmpty())
    }

    @Test
    fun anioDeFundacion_fueraDe1900YElAnioActual_esAnioInvalido() {
        assertEquals(MensajesAcademia.ANIO_INVALIDO, validar(anio = "1899")[CampoAcademia.ANIO_FUNDACION])
        assertEquals(MensajesAcademia.ANIO_INVALIDO, validar(anio = "2027")[CampoAcademia.ANIO_FUNDACION])
        assertEquals(MensajesAcademia.ANIO_INVALIDO, validar(anio = "")[CampoAcademia.ANIO_FUNDACION])
        assertNull(ValidacionAcademia.validarAnioFundacion("1900", 2026))
        assertNull(ValidacionAcademia.validarAnioFundacion("2026", 2026))
    }

    @Test
    fun escudoDeMasDe2MB_esRechazado() {
        assertEquals(MensajesAcademia.IMAGEN_GRANDE, ValidacionAcademia.validarEscudo(2L * 1024 * 1024 + 1))
        assertNull(ValidacionAcademia.validarEscudo(2L * 1024 * 1024))
    }

    @Test
    fun camposObligatoriosYFormatos() {
        val errores = validar(valida.copy(ciudad = "", telefono = "12", correo = "contacto"))
        assertEquals(MensajesAcademia.CAMPO_OBLIGATORIO, errores[CampoAcademia.CIUDAD])
        assertEquals(MensajesAcademia.TELEFONO_INVALIDO, errores[CampoAcademia.TELEFONO])
        assertEquals(MensajesAcademia.CORREO_INVALIDO, errores[CampoAcademia.CORREO])
    }

    @Test
    fun nombreUnico_ignoraMayusculasTildesYEspacios() {
        assertEquals(
            ValidacionAcademia.claveNombre("Academia  Lima Norte"),
            ValidacionAcademia.claveNombre("academia lima norté"),
        )
    }

    @Test
    fun auditoria_registraSoloLosCamposQueCambiaron() {
        val cambios = ValidacionAcademia.cambios(valida, valida.copy(ciudad = "Callao", anioFundacion = 2016))
        assertEquals(listOf("ciudad", "anioFundacion"), cambios.map { it.campo })
        assertEquals("Lima", cambios.first().valorAnterior)
        assertEquals("Callao", cambios.first().valorNuevo)
    }

    @Test
    fun codigo_muestraVigenteUsadoExpiradoORevocado() {
        val manana = Date(System.currentTimeMillis() + 86_400_000)
        val ayer = Date(System.currentTimeMillis() - 86_400_000)
        assertEquals(EstadoCodigo.VIGENTE, CodigoInvitacion("ABCDEFGH", null, manana, false, false, UserRole.ENTRENADOR).estado())
        assertEquals(EstadoCodigo.USADO, CodigoInvitacion("ABCDEFGH", null, manana, true, false).estado())
        assertEquals(EstadoCodigo.EXPIRADO, CodigoInvitacion("ABCDEFGH", null, ayer, false, false).estado())
        assertEquals(EstadoCodigo.REVOCADO, CodigoInvitacion("ABCDEFGH", null, manana, false, true).estado())
    }
}
