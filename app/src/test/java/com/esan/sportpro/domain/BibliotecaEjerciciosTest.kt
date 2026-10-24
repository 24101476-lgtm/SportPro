package com.esan.sportpro.domain

import com.esan.sportpro.domain.entrenamientos.BibliotecaEjercicios
import com.esan.sportpro.domain.entrenamientos.CampoBiblioteca
import com.esan.sportpro.domain.entrenamientos.CategoriaTecnica
import com.esan.sportpro.domain.entrenamientos.DatosEjercicioBiblioteca
import com.esan.sportpro.domain.entrenamientos.Ejercicio
import com.esan.sportpro.domain.entrenamientos.EjercicioBiblioteca
import com.esan.sportpro.domain.entrenamientos.Entrenamiento
import com.esan.sportpro.domain.entrenamientos.FiltroBiblioteca
import com.esan.sportpro.domain.entrenamientos.RangoDuracion
import com.esan.sportpro.domain.entrenamientos.ValidacionBiblioteca
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** US-009: validación del formulario de la biblioteca (criterios 1 a 5). */
class ValidacionBibliotecaTest {

    private val existentes = listOf(
        EjercicioBiblioteca(id = "e1", nombre = "Rondo 4v2", duracionMin = 15),
        EjercicioBiblioteca(id = "e2", nombre = "Circuito físico", duracionMin = 20, activo = false),
    )

    private fun datosValidos() = DatosEjercicioBiblioteca(
        nombre = "Pase en triángulo",
        objetivo = "Mejorar el pase corto",
        categoria = CategoriaTecnica.PASE_Y_CONTROL,
        duracion = "20",
        minJugadores = "6",
        materiales = "6 conos, 3 balones",
        descripcion = "Tres grupos en triángulo pasan y siguen al cono siguiente.",
    )

    @Test
    fun ejercicioValido_noTieneErrores() {
        assertTrue(ValidacionBiblioteca.validar(datosValidos(), existentes).isEmpty())
    }

    @Test
    fun nombre_debeTenerEntre3y60Caracteres() {
        assertTrue(CampoBiblioteca.NOMBRE in ValidacionBiblioteca.validar(datosValidos().copy(nombre = "ab"), existentes))
        assertTrue(CampoBiblioteca.NOMBRE in ValidacionBiblioteca.validar(datosValidos().copy(nombre = "x".repeat(61)), existentes))
        assertFalse(CampoBiblioteca.NOMBRE in ValidacionBiblioteca.validar(datosValidos().copy(nombre = "x".repeat(60)), existentes))
    }

    @Test
    fun nombreRepetido_sinImportarMayusculasNiTildes_esRechazado() {
        val errores = ValidacionBiblioteca.validar(datosValidos().copy(nombre = "  RONDÓ 4V2 "), existentes)
        assertEquals(ValidacionBiblioteca.MSG_NOMBRE_DUPLICADO, errores[CampoBiblioteca.NOMBRE])
    }

    @Test
    fun nombreDeUnEjercicioDesactivado_tambienCuentaComoRepetido() {
        val errores = ValidacionBiblioteca.validar(datosValidos().copy(nombre = "Circuito fisico"), existentes)
        assertEquals(ValidacionBiblioteca.MSG_NOMBRE_DUPLICADO, errores[CampoBiblioteca.NOMBRE])
    }

    @Test
    fun editarElMismoEjercicio_noCuentaComoRepetido() {
        val errores = ValidacionBiblioteca.validar(datosValidos().copy(nombre = "Rondo 4v2"), existentes, idEditando = "e1")
        assertFalse(CampoBiblioteca.NOMBRE in errores)
    }

    @Test
    fun categoria_esObligatoria() {
        assertTrue(CampoBiblioteca.CATEGORIA in ValidacionBiblioteca.validar(datosValidos().copy(categoria = null), existentes))
    }

    @Test
    fun duracion_aceptaDe1a120Minutos() {
        listOf("0", "121", "", "diez").forEach { valor ->
            val errores = ValidacionBiblioteca.validar(datosValidos().copy(duracion = valor), existentes)
            assertEquals("duración '$valor'", ValidacionBiblioteca.MSG_DURACION_INVALIDA, errores[CampoBiblioteca.DURACION])
        }
        listOf("1", "120").forEach { valor ->
            assertFalse(CampoBiblioteca.DURACION in ValidacionBiblioteca.validar(datosValidos().copy(duracion = valor), existentes))
        }
    }

    @Test
    fun descripcion_aceptaDe20a1000Caracteres() {
        assertTrue(CampoBiblioteca.DESCRIPCION in ValidacionBiblioteca.validar(datosValidos().copy(descripcion = "x".repeat(19)), existentes))
        assertTrue(CampoBiblioteca.DESCRIPCION in ValidacionBiblioteca.validar(datosValidos().copy(descripcion = "x".repeat(1001)), existentes))
        assertFalse(CampoBiblioteca.DESCRIPCION in ValidacionBiblioteca.validar(datosValidos().copy(descripcion = "x".repeat(1000)), existentes))
    }

    @Test
    fun contadorDeCaracteresRestantes() {
        assertEquals("990 caracteres restantes", ValidacionBiblioteca.caracteresRestantes("x".repeat(10)))
        assertEquals("5 caracteres de más", ValidacionBiblioteca.caracteresRestantes("x".repeat(1005)))
    }

    @Test
    fun minimoDeJugadores_esObligatorioYEnRango() {
        assertTrue(CampoBiblioteca.MIN_JUGADORES in ValidacionBiblioteca.validar(datosValidos().copy(minJugadores = "0"), existentes))
        assertTrue(CampoBiblioteca.MIN_JUGADORES in ValidacionBiblioteca.validar(datosValidos().copy(minJugadores = ""), existentes))
    }

    @Test
    fun materiales_sonOpcionales() {
        assertTrue(ValidacionBiblioteca.validar(datosValidos().copy(materiales = ""), existentes).isEmpty())
    }
}

/** US-009: buscador, permisos y desactivación (criterios 6 a 8). */
class BibliotecaEjerciciosTest {

    private val rondo = EjercicioBiblioteca(
        id = "e1", nombre = "Rondo 4v2", categoria = CategoriaTecnica.PASE_Y_CONTROL.name, duracionMin = 15, creadoPor = "dt1",
    )
    private val definicion = EjercicioBiblioteca(
        id = "e2", nombre = "Definición tras pase", categoria = CategoriaTecnica.DEFINICION.name, duracionMin = 25, creadoPor = "dt2",
    )
    private val trote = EjercicioBiblioteca(
        id = "e3", nombre = "Trote suave", categoria = CategoriaTecnica.VUELTA_A_LA_CALMA.name, duracionMin = 8, creadoPor = "dt1",
    )
    private val desactivado = EjercicioBiblioteca(
        id = "e4", nombre = "Rondo antiguo", categoria = CategoriaTecnica.PASE_Y_CONTROL.name, duracionMin = 15, activo = false,
    )
    private val todos = listOf(trote, definicion, rondo, desactivado)

    @Test
    fun buscador_muestraSoloActivos_ordenadosPorNombre() {
        val resultado = BibliotecaEjercicios.buscar(todos, FiltroBiblioteca())
        assertEquals(listOf("e2", "e1", "e3"), resultado.map { it.id })
    }

    @Test
    fun buscador_porNombre_ignoraMayusculasYTildes() {
        val resultado = BibliotecaEjercicios.buscar(todos, FiltroBiblioteca(texto = "DEFINICION"))
        assertEquals(listOf("e2"), resultado.map { it.id })
    }

    @Test
    fun buscador_combinaCategoriaYDuracion() {
        val porCategoria = BibliotecaEjercicios.buscar(todos, FiltroBiblioteca(categoria = CategoriaTecnica.PASE_Y_CONTROL))
        assertEquals(listOf("e1"), porCategoria.map { it.id })

        val porDuracion = BibliotecaEjercicios.buscar(todos, FiltroBiblioteca(duracion = RangoDuracion.HASTA_10))
        assertEquals(listOf("e3"), porDuracion.map { it.id })

        val combinado = BibliotecaEjercicios.buscar(
            todos,
            FiltroBiblioteca(texto = "rondo", categoria = CategoriaTecnica.PASE_Y_CONTROL, duracion = RangoDuracion.DE_21_A_30),
        )
        assertTrue(combinado.isEmpty())
    }

    @Test
    fun buscadorDeDesactivados_soloDevuelveDesactivados() {
        val resultado = BibliotecaEjercicios.buscar(todos, FiltroBiblioteca(), desactivados = true)
        assertEquals(listOf("e4"), resultado.map { it.id })
    }

    @Test
    fun soloElAutorOElAdministrador_puedenEditar() {
        assertTrue(BibliotecaEjercicios.puedeEditar(rondo, uid = "dt1", esAdmin = false))
        assertFalse(BibliotecaEjercicios.puedeEditar(rondo, uid = "dt2", esAdmin = false))
        assertTrue(BibliotecaEjercicios.puedeEditar(rondo, uid = "adm", esAdmin = true))
        // Ejercicios antiguos sin autor: solo el administrador.
        assertFalse(BibliotecaEjercicios.puedeEditar(desactivado, uid = "", esAdmin = false))
    }

    @Test
    fun usadoEnSesionRealizada_porIdOPorNombreEnSesionesAntiguas() {
        val realizadaConId = Entrenamiento(
            asistenciaRegistrada = true,
            ejercicios = listOf(Ejercicio("Rondo 4v2", "", 15, ejercicioId = "e1")),
        )
        val realizadaAntigua = Entrenamiento(
            asistenciaRegistrada = true,
            ejercicios = listOf(Ejercicio("Definicion tras pase", "", 25)),
        )
        val programada = Entrenamiento(
            asistenciaRegistrada = false,
            ejercicios = listOf(Ejercicio("Trote suave", "", 8, ejercicioId = "e3")),
        )
        val sesiones = listOf(realizadaConId, realizadaAntigua, programada)

        assertTrue(BibliotecaEjercicios.usadoEnSesionRealizada(rondo, sesiones))
        assertTrue(BibliotecaEjercicios.usadoEnSesionRealizada(definicion, sesiones))
        // Solo está en una sesión que todavía no se realizó: se puede eliminar.
        assertFalse(BibliotecaEjercicios.usadoEnSesionRealizada(trote, sesiones))
    }

    @Test
    fun alAgregarloAUnaSesion_seCopiaConSuId() {
        val copia = rondo.aEjercicioDeSesion()
        assertEquals(Ejercicio("Rondo 4v2", "", 15, ejercicioId = "e1"), copia)
    }
}
