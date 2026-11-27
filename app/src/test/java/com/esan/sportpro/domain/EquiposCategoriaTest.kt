package com.esan.sportpro.domain

import com.esan.sportpro.domain.academia.CampoEquipo
import com.esan.sportpro.domain.academia.CategoriasEquipo
import com.esan.sportpro.domain.academia.Equipo
import com.esan.sportpro.domain.academia.MensajesEquipo
import com.esan.sportpro.domain.academia.ValidacionEquipo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** US-005: cada criterio de la gestión de equipos por categorías. */
class EquiposCategoriaTest {

    private fun validar(
        categoria: String = "Sub-15",
        categoriaOtra: String = "",
        entrenador: String = "Carlos Ruiz",
        entrenadorUid: String = "",
        anioMinimo: Int? = 2012,
        anioMaximo: Int? = 2013,
    ) = ValidacionEquipo.validar(
        nombre = "Leones",
        categoria = categoria,
        entrenadorNombre = entrenador,
        existentes = emptyList(),
        categoriaOtra = categoriaOtra,
        entrenadorUid = entrenadorUid,
        anioMinimo = anioMinimo,
        anioMaximo = anioMaximo,
        anioActual = 2026,
    )

    @Test
    fun categorias_sonLasDelCriterio() {
        assertEquals(
            listOf("Sub-8", "Sub-10", "Sub-12", "Sub-15", "Sub-17", "Sub-20", "Primera", "Otra"),
            CategoriasEquipo.todas,
        )
    }

    @Test
    fun categoriaOtra_exigeTextoDeHasta30Caracteres() {
        assertEquals(MensajesEquipo.OTRA_REQUERIDA, validar(categoria = "Otra")[CampoEquipo.CATEGORIA])
        assertEquals(MensajesEquipo.OTRA_MAXIMO, validar(categoria = "Otra", categoriaOtra = "x".repeat(31))[CampoEquipo.CATEGORIA])
        assertTrue(validar(categoria = "Otra", categoriaOtra = "Sub-13 femenino").isEmpty())
    }

    @Test
    fun categoriaAntigua_seMuestraComoOtra() {
        assertEquals("Otra", CategoriasEquipo.opcionDe("Sub-13"))
        assertEquals("Sub-15", CategoriasEquipo.opcionDe("Sub-15"))
        assertNull(CategoriasEquipo.opcionDe(""))
        assertEquals("Sub-13", CategoriasEquipo.categoriaFinal("Otra", " Sub-13 "))
    }

    @Test
    fun sinEntrenador_muestraDebeAsignarUnEntrenador() {
        assertEquals(MensajesEquipo.ENTRENADOR_REQUERIDO, validar(entrenador = "")[CampoEquipo.ENTRENADOR])
        assertTrue(validar(entrenador = "", entrenadorUid = "uid-dt").isEmpty())
    }

    @Test
    fun anioMinimoMayorQueMaximo_esRangoInvalido() {
        assertEquals(MensajesEquipo.RANGO_INVALIDO, validar(anioMinimo = 2014, anioMaximo = 2012)[CampoEquipo.ANIOS])
    }

    @Test
    fun aniosFueraDe1900YElAnioActual_sonInvalidos() {
        assertEquals(MensajesEquipo.RANGO_INVALIDO, validar(anioMinimo = 1899, anioMaximo = 2012)[CampoEquipo.ANIOS])
        assertEquals(MensajesEquipo.RANGO_INVALIDO, validar(anioMinimo = 2012, anioMaximo = 2027)[CampoEquipo.ANIOS])
    }

    @Test
    fun aniosVacios_sonObligatorios() {
        assertEquals(MensajesEquipo.ANIOS_REQUERIDOS, validar(anioMinimo = null)[CampoEquipo.ANIOS])
    }

    @Test
    fun mismoAnio_esUnRangoValido() {
        assertTrue(validar(anioMinimo = 2012, anioMaximo = 2012).isEmpty())
    }

    @Test
    fun rangoSugerido_paraSub15En2026() {
        assertEquals(2012 to 2013, ValidacionEquipo.rangoSugerido("Sub-15", 2026))
        assertNull(ValidacionEquipo.rangoSugerido("Primera", 2026))
    }

    @Test
    fun jugadorFueraDelRango_noEsAdmitido() {
        val equipo = Equipo(nombre = "Leones", categoria = "Sub-15", anioMinimo = 2012, anioMaximo = 2013)
        assertTrue(equipo.admiteAnio(2012))
        assertFalse(equipo.admiteAnio(2010))
        assertTrue(Equipo(nombre = "Antiguo").admiteAnio(1990))
    }

    @Test
    fun equipoConJugadores_noSePuedeEliminar() {
        assertFalse(ValidacionEquipo.puedeEliminar(3))
        assertTrue(ValidacionEquipo.puedeEliminar(0))
    }
}
