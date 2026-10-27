package com.esan.sportpro.domain

import com.esan.sportpro.core.fecha.FechasSportPro
import com.esan.sportpro.domain.jugadores.CampoJugador
import com.esan.sportpro.domain.jugadores.DatosJugador
import com.esan.sportpro.domain.jugadores.EstadoRegistro
import com.esan.sportpro.domain.jugadores.Jugador
import com.esan.sportpro.domain.jugadores.ReglasApoderado
import com.esan.sportpro.domain.jugadores.ValidacionJugador
import com.esan.sportpro.navigation.HomeModule
import com.esan.sportpro.navigation.MenuPrincipal
import com.esan.sportpro.navigation.TipoPestana
import com.esan.sportpro.navigation.UserRole
import com.esan.sportpro.ui.registro.MensajesRegistro
import com.esan.sportpro.ui.registro.ValidacionRegistro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/** Regla: un jugador menor de 18 no queda registrado hasta tener un apoderado vinculado. */
class ReglasApoderadoTest {

    private val hoy = LocalDate.of(2026, 10, 27)
    private val menor = Jugador(id = "j1", fechaNacimiento = "2013-03-10")
    private val adulto = Jugador(id = "j2", fechaNacimiento = "2000-03-10")

    @Test
    fun menorSinApoderado_quedaPendiente() {
        assertEquals(EstadoRegistro.PENDIENTE_APODERADO, ReglasApoderado.estadoPara(menor, apoderadosAprobados = 0, hoy = hoy))
    }

    @Test
    fun menorConApoderadoAprobado_quedaRegistrado() {
        assertEquals(EstadoRegistro.COMPLETO, ReglasApoderado.estadoPara(menor, apoderadosAprobados = 1, hoy = hoy))
    }

    @Test
    fun mayorDeEdad_noNecesitaApoderado() {
        assertEquals(EstadoRegistro.COMPLETO, ReglasApoderado.estadoPara(adulto, apoderadosAprobados = 0, hoy = hoy))
    }

    @Test
    fun jugadorPendiente_noParticipaEnAsistenciaNiConvocatorias() {
        val pendiente = menor.copy(estadoRegistro = EstadoRegistro.PENDIENTE_APODERADO.name)
        assertTrue(ReglasApoderado.pendiente(pendiente))
        assertFalse(ReglasApoderado.participa(pendiente))
        assertTrue(ReglasApoderado.participa(menor.copy(estadoRegistro = EstadoRegistro.COMPLETO.name)))
    }

    @Test
    fun fichaAntiguaSinEstado_cuentaComoRegistrada() {
        assertFalse(ReglasApoderado.pendiente(adulto))
        assertTrue(ReglasApoderado.participa(adulto))
    }

    @Test
    fun jugadorInactivo_noParticipa() {
        assertFalse(ReglasApoderado.participa(adulto.copy(activo = false)))
    }
}

/** La ficha de un menor exige los datos del apoderado. */
class ApoderadoEnFichaTest {

    private val hoy = LocalDate.of(2026, 10, 27)

    private fun menorValido() = DatosJugador(
        nombres = "Ana",
        apellidos = "Torres",
        fechaNacimiento = "2014-01-15",
        equipoId = "e1",
        posicion = "Portero",
        emergenciaNombre = "Carla Torres",
        emergenciaTelefono = "912345678",
        emergenciaParentesco = "Madre",
        apoderadoCorreo = "carla@correo.com",
    )

    @Test
    fun menorConApoderadoCompleto_esValido() {
        assertTrue(ValidacionJugador.validar(menorValido(), hoy).isEmpty())
    }

    @Test
    fun menorSinCorreoDeApoderado_esRechazado() {
        val errores = ValidacionJugador.validar(menorValido().copy(apoderadoCorreo = ""), hoy)
        assertTrue(CampoJugador.APODERADO_CORREO in errores)
    }

    @Test
    fun menorConParentescoNoValido_esRechazado() {
        val errores = ValidacionJugador.validar(menorValido().copy(emergenciaParentesco = "Vecino"), hoy)
        assertTrue(CampoJugador.EMERGENCIA_PARENTESCO in errores)
    }

    @Test
    fun mayorDeEdad_noNecesitaCorreoDeApoderado() {
        val adulto = menorValido().copy(fechaNacimiento = "1999-06-01", apoderadoCorreo = "", emergenciaParentesco = "Esposa")
        assertTrue(ValidacionJugador.validar(adulto, hoy).isEmpty())
    }

    @Test
    fun correoDeApoderadoMalEscrito_esRechazado() {
        val errores = ValidacionJugador.validar(menorValido().copy(apoderadoCorreo = "carla@"), hoy)
        assertTrue(CampoJugador.APODERADO_CORREO in errores)
    }

    @Test
    fun calendarioDeNacimiento_ofreceSoloEdadesValidas() {
        assertEquals(LocalDate.of(2022, 10, 27), ValidacionJugador.fechaNacimientoMaxima(hoy))
        assertTrue(ValidacionJugador.fechaNacimientoMinima(hoy).isAfter(LocalDate.of(1965, 10, 27)))
    }
}

/** Corrección: el año de nacimiento del registro se valida según el rol. */
class FechaNacimientoRegistroTest {

    private val hoy = LocalDate.of(2026, 10, 27)

    @Test
    fun anioImposible_esRechazado() {
        assertEquals(
            MensajesRegistro.FECHA_NACIMIENTO_INVALIDA,
            ValidacionRegistro.validarFechaNacimiento(LocalDate.of(1800, 1, 1), UserRole.PADRE_DE_FAMILIA, hoy),
        )
    }

    @Test
    fun fechaFutura_esRechazada() {
        assertEquals(
            MensajesRegistro.FECHA_NACIMIENTO_FUTURA,
            ValidacionRegistro.validarFechaNacimiento(hoy.plusDays(1), UserRole.JUGADOR, hoy),
        )
    }

    @Test
    fun entrenadorMenorDeEdad_esRechazado() {
        assertEquals(
            MensajesRegistro.MAYOR_DE_EDAD_REQUERIDO,
            ValidacionRegistro.validarFechaNacimiento(LocalDate.of(2012, 1, 1), UserRole.ENTRENADOR, hoy),
        )
    }

    @Test
    fun jugadorMenor_noPuedeRegistrarseSolo() {
        assertEquals(
            MensajesRegistro.MENOR_REGISTRA_ACADEMIA,
            ValidacionRegistro.validarFechaNacimiento(LocalDate.of(2012, 1, 1), UserRole.JUGADOR, hoy),
        )
    }

    @Test
    fun jugadorAdulto_esValido() {
        assertNull(ValidacionRegistro.validarFechaNacimiento(LocalDate.of(2004, 5, 20), UserRole.JUGADOR, hoy))
    }

    @Test
    fun padreAdulto_esValido() {
        assertNull(ValidacionRegistro.validarFechaNacimiento(LocalDate.of(1980, 5, 20), UserRole.PADRE_DE_FAMILIA, hoy))
    }
}

/** Criterio único de fechas. */
class FechasSportProTest {

    @Test
    fun isoIdaYVuelta() {
        val fecha = LocalDate.of(2026, 11, 7)
        assertEquals("2026-11-07", FechasSportPro.aIso(fecha))
        assertEquals(fecha, FechasSportPro.desdeIso("2026-11-07"))
        assertNull(FechasSportPro.desdeIso("07/11/2026"))
        assertNull(FechasSportPro.desdeIso(""))
    }

    @Test
    fun presentacionEstandar() {
        assertEquals("07/11/2026", FechasSportPro.fecha(LocalDate.of(2026, 11, 7)))
        assertEquals("07/11/2026", FechasSportPro.fechaIso("2026-11-07"))
        assertEquals("texto raro", FechasSportPro.fechaIso("texto raro"))
    }

    @Test
    fun horaEnVeinticuatroHoras() {
        assertEquals("16:30", FechasSportPro.horaATexto(LocalTime.of(16, 30)))
        assertEquals(LocalTime.of(9, 5), FechasSportPro.horaDesdeTexto("09:05"))
        assertNull(FechasSportPro.horaDesdeTexto("25:00"))
    }

    @Test
    fun instanteSeMuestraEnHoraDePeru() {
        // 2026-11-07 21:30 en UTC son las 16:30 en Lima (UTC-5).
        val millis = java.time.ZonedDateTime.of(2026, 11, 7, 21, 30, 0, 0, java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals("07/11/2026 16:30", FechasSportPro.fechaHora(millis))
    }

    @Test
    fun selectorDeCalendario_noCorreElDia() {
        val fecha = LocalDate.of(2026, 1, 1)
        assertEquals(fecha, FechasSportPro.fechaDesdeUtcMillis(FechasSportPro.utcMillisDe(fecha)))
    }

    @Test
    fun ordenarPorFechaYHora() {
        val antes = FechasSportPro.aMillis("2026-11-07", "09:00")!!
        val despues = FechasSportPro.aMillis("2026-11-07", "16:30")!!
        assertTrue(antes < despues)
        assertNull(FechasSportPro.aMillis("mal", "09:00"))
    }
}

/** Corrección: barra inferior de 4 opciones por rol. */
class MenuPrincipalTest {

    @Test
    fun todosLosRoles_tienenExactamenteCuatroPestanas() {
        UserRole.entries.forEach { rol ->
            assertEquals("Rol $rol", MenuPrincipal.MAX_PESTANAS, MenuPrincipal.para(rol).size)
        }
    }

    @Test
    fun todosLosRoles_empiezanEnInicioYTerminanEnMas() {
        UserRole.entries.forEach { rol ->
            val pestanas = MenuPrincipal.para(rol)
            assertEquals(TipoPestana.INICIO, pestanas.first().tipo)
            assertEquals(TipoPestana.MAS, pestanas.last().tipo)
        }
    }

    @Test
    fun elMenuContieneLosMismosModulosQueElRolPuedeVer() {
        UserRole.entries.forEach { rol ->
            val enMenu = MenuPrincipal.para(rol).flatMap { it.modulos }
            assertEquals("Rol $rol", HomeModule.visiblesPara(rol).toSet(), enMenu.toSet())
            assertEquals("Módulos repetidos en el menú de $rol", enMenu.size, enMenu.toSet().size)
        }
    }

    @Test
    fun pestanaDe_ubicaCadaModulo() {
        assertEquals("plantel", MenuPrincipal.pestanaDe(UserRole.ENTRENADOR, HomeModule.ENTRENAMIENTOS)?.id)
        assertEquals("mas", MenuPrincipal.pestanaDe(UserRole.ADMINISTRADOR, HomeModule.COMUNIDAD)?.id)
        assertNull(MenuPrincipal.pestanaDe(UserRole.JUGADOR, HomeModule.MENSUALIDADES))
    }
}
