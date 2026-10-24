package com.esan.sportpro.domain

import com.esan.sportpro.domain.academia.CampoEquipo
import com.esan.sportpro.domain.academia.Equipo
import com.esan.sportpro.domain.academia.ValidacionEquipo
import com.esan.sportpro.domain.entrenamientos.CalculoAsistencia
import com.esan.sportpro.domain.entrenamientos.CampoEjercicio
import com.esan.sportpro.domain.entrenamientos.CampoEntrenamiento
import com.esan.sportpro.domain.entrenamientos.DatosEntrenamiento
import com.esan.sportpro.domain.entrenamientos.Ejercicio
import com.esan.sportpro.domain.entrenamientos.Entrenamiento
import com.esan.sportpro.domain.entrenamientos.EstadoAsistencia
import com.esan.sportpro.domain.entrenamientos.ValidacionEntrenamiento
import com.esan.sportpro.domain.jugadores.CampoJugador
import com.esan.sportpro.domain.jugadores.DatosJugador
import com.esan.sportpro.domain.jugadores.Jugador
import com.esan.sportpro.domain.jugadores.ValidacionJugador
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ValidacionEquipoTest {

    private val existentes = listOf(Equipo(id = "e1", nombre = "Tigres", categoria = "Sub-15"))

    @Test
    fun equipoValido_noTieneErrores() {
        val errores = ValidacionEquipo.validar("Leones", "Sub-12", "", existentes)
        assertTrue(errores.isEmpty())
    }

    @Test
    fun nombreCorto_esRechazado() {
        val errores = ValidacionEquipo.validar("Ab", "Sub-12", "", existentes)
        assertTrue(CampoEquipo.NOMBRE in errores)
    }

    @Test
    fun categoriaInexistente_esRechazada() {
        val errores = ValidacionEquipo.validar("Leones", "Sub-99", "", existentes)
        assertTrue(CampoEquipo.CATEGORIA in errores)
    }

    @Test
    fun mismoNombreYCategoria_esDuplicado_sinImportarMayusculas() {
        val errores = ValidacionEquipo.validar("  tigres ", "Sub-15", "", existentes)
        assertTrue(CampoEquipo.NOMBRE in errores)
    }

    @Test
    fun mismoNombreEnOtraCategoria_esValido() {
        val errores = ValidacionEquipo.validar("Tigres", "Sub-17", "", existentes)
        assertTrue(errores.isEmpty())
    }

    @Test
    fun editarElMismoEquipo_noCuentaComoDuplicado() {
        val errores = ValidacionEquipo.validar("Tigres", "Sub-15", "", existentes, idEditando = "e1")
        assertTrue(errores.isEmpty())
    }
}

class ValidacionJugadorTest {

    private val hoy = LocalDate.of(2026, 10, 3)

    private fun datosValidos() = DatosJugador(
        nombres = "Luis Miguel",
        apellidos = "Quispe Rojas",
        fechaNacimiento = "2012-05-30",
        equipoId = "e1",
        posicion = "Delantero",
        emergenciaNombre = "Rosa Rojas",
        emergenciaTelefono = "987654321",
        emergenciaParentesco = "Madre",
    )

    @Test
    fun jugadorValido_noTieneErrores() {
        assertTrue(ValidacionJugador.validar(datosValidos(), hoy).isEmpty())
    }

    @Test
    fun contactoDeEmergencia_esObligatorio() {
        val errores = ValidacionJugador.validar(
            datosValidos().copy(emergenciaNombre = "", emergenciaTelefono = "", emergenciaParentesco = ""),
            hoy,
        )
        assertTrue(CampoJugador.EMERGENCIA_NOMBRE in errores)
        assertTrue(CampoJugador.EMERGENCIA_TELEFONO in errores)
        assertTrue(CampoJugador.EMERGENCIA_PARENTESCO in errores)
    }

    @Test
    fun fechaFutura_esRechazada() {
        val errores = ValidacionJugador.validar(datosValidos().copy(fechaNacimiento = "2027-01-01"), hoy)
        assertTrue(CampoJugador.FECHA_NACIMIENTO in errores)
    }

    @Test
    fun fechaConFormatoIncorrecto_esRechazada() {
        val errores = ValidacionJugador.validar(datosValidos().copy(fechaNacimiento = "30/05/2012"), hoy)
        assertTrue(CampoJugador.FECHA_NACIMIENTO in errores)
    }

    @Test
    fun menorDeCuatroAnios_esRechazado() {
        val errores = ValidacionJugador.validar(datosValidos().copy(fechaNacimiento = "2024-01-01"), hoy)
        assertTrue(CampoJugador.FECHA_NACIMIENTO in errores)
    }

    @Test
    fun datosFisicosFueraDeRango_sonRechazados() {
        val errores = ValidacionJugador.validar(
            datosValidos().copy(estatura = "300", peso = "5", dorsal = "100"),
            hoy,
        )
        assertTrue(CampoJugador.ESTATURA in errores)
        assertTrue(CampoJugador.PESO in errores)
        assertTrue(CampoJugador.DORSAL in errores)
    }

    @Test
    fun pesoAceptaComaDecimal() {
        assertEquals(45.5, ValidacionJugador.parsearPeso("45,5")!!, 0.0001)
        assertTrue(ValidacionJugador.validar(datosValidos().copy(peso = "45,5"), hoy).isEmpty())
    }

    @Test
    fun telefonoPropio_esOpcionalPeroDebeSerValido() {
        assertTrue(ValidacionJugador.validar(datosValidos().copy(telefono = ""), hoy).isEmpty())
        val errores = ValidacionJugador.validar(datosValidos().copy(telefono = "12345"), hoy)
        assertTrue(CampoJugador.TELEFONO in errores)
    }

    @Test
    fun posicionFueraDelCatalogo_esRechazada() {
        val errores = ValidacionJugador.validar(datosValidos().copy(posicion = "Arquero"), hoy)
        assertTrue(CampoJugador.POSICION in errores)
    }

    @Test
    fun esMenor_dependeDeLaEdad() {
        val menor = Jugador(fechaNacimiento = "2012-05-30")
        val adulto = Jugador(fechaNacimiento = "2000-01-01")
        assertTrue(menor.esMenor(hoy))
        assertFalse(adulto.esMenor(hoy))
        assertEquals(14, menor.edad(hoy))
        assertNull(Jugador(fechaNacimiento = "basura").edad(hoy))
    }
}

class ValidacionEntrenamientoTest {

    private val hoy = LocalDate.of(2026, 10, 3)
    private val ejercicio = Ejercicio("Rondos", "Posesión 4v2", 20)

    private fun datosValidos() = DatosEntrenamiento(
        equipoId = "e1",
        fecha = "2026-10-10",
        hora = "16:30",
        objetivo = "Mejorar la salida con balón",
        ejercicios = listOf(ejercicio),
    )

    @Test
    fun entrenamientoValido_noTieneErrores() {
        assertTrue(ValidacionEntrenamiento.validar(datosValidos(), hoy).isEmpty())
    }

    @Test
    fun sinEjercicios_esRechazado() {
        val errores = ValidacionEntrenamiento.validar(datosValidos().copy(ejercicios = emptyList()), hoy)
        assertTrue(CampoEntrenamiento.EJERCICIOS in errores)
    }

    @Test
    fun sesionMuyLarga_esRechazada() {
        val largos = listOf(Ejercicio("A", "", 150), Ejercicio("B", "", 150))
        val errores = ValidacionEntrenamiento.validar(datosValidos().copy(ejercicios = largos), hoy)
        assertTrue(CampoEntrenamiento.EJERCICIOS in errores)
    }

    @Test
    fun fechaPasada_soloSePermiteAlEditar() {
        val pasada = datosValidos().copy(fecha = "2026-09-01")
        assertTrue(CampoEntrenamiento.FECHA in ValidacionEntrenamiento.validar(pasada, hoy))
        assertTrue(ValidacionEntrenamiento.validar(pasada, hoy, permitirPasado = true).isEmpty())
    }

    @Test
    fun horaInvalida_esRechazada() {
        val errores = ValidacionEntrenamiento.validar(datosValidos().copy(hora = "25:99"), hoy)
        assertTrue(CampoEntrenamiento.HORA in errores)
    }

    @Test
    fun objetivoVacio_esRechazado() {
        val errores = ValidacionEntrenamiento.validar(datosValidos().copy(objetivo = " "), hoy)
        assertTrue(CampoEntrenamiento.OBJETIVO in errores)
    }

    @Test
    fun ejercicio_validaNombreYDuracion() {
        assertTrue(ValidacionEntrenamiento.validarEjercicio("Rondos", "", "20").isEmpty())
        val errores = ValidacionEntrenamiento.validarEjercicio("ab", "", "0")
        assertTrue(CampoEjercicio.NOMBRE in errores)
        assertTrue(CampoEjercicio.DURACION in errores)
    }

    @Test
    fun duracionTotal_sumaLosEjercicios() {
        val sesion = Entrenamiento(ejercicios = listOf(Ejercicio("A", "", 15), Ejercicio("B", "", 25)))
        assertEquals(40, sesion.duracionTotalMin)
    }
}

class CalculoAsistenciaTest {

    private fun sesion(registrada: Boolean, vararg estados: Pair<String, EstadoAsistencia>) = Entrenamiento(
        asistenciaRegistrada = registrada,
        asistencia = estados.associate { it.first to it.second.name },
    )

    @Test
    fun cuentaSoloSesionesConAsistenciaRegistrada() {
        val sesiones = listOf(
            sesion(true, "j1" to EstadoAsistencia.PRESENTE),
            sesion(true, "j1" to EstadoAsistencia.TARDE),
            sesion(true, "j1" to EstadoAsistencia.AUSENTE),
            sesion(true, "j1" to EstadoAsistencia.JUSTIFICADO),
            sesion(false, "j1" to EstadoAsistencia.PRESENTE),
        )
        val resumen = CalculoAsistencia.resumen("j1", sesiones)
        assertEquals(4, resumen.registrados)
        assertEquals(2, resumen.participaciones)
        assertEquals(50, resumen.porcentaje)
        assertEquals(1, resumen.ausentes)
        assertEquals(1, resumen.justificados)
    }

    @Test
    fun sesionEnLaQueNoFiguraElJugador_noLoPenaliza() {
        val sesiones = listOf(
            sesion(true, "otro" to EstadoAsistencia.PRESENTE),
            sesion(true, "j1" to EstadoAsistencia.PRESENTE),
        )
        val resumen = CalculoAsistencia.resumen("j1", sesiones)
        assertEquals(1, resumen.registrados)
        assertEquals(100, resumen.porcentaje)
    }

    @Test
    fun sinSesiones_elPorcentajeEsCero() {
        assertEquals(0, CalculoAsistencia.resumen("j1", emptyList()).porcentaje)
    }
}
