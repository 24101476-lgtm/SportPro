package com.esan.sportpro.domain

import com.esan.sportpro.domain.comunidad.PosicionJugador
import com.esan.sportpro.domain.jugadores.CampoEditableJugador
import com.esan.sportpro.domain.jugadores.CampoJugador
import com.esan.sportpro.domain.jugadores.DatosJugador
import com.esan.sportpro.domain.jugadores.Jugador
import com.esan.sportpro.domain.jugadores.ValidacionJugador
import com.esan.sportpro.domain.jugadores.VisibilidadJugador
import com.esan.sportpro.navigation.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** US-006: perfil del jugador (criterios 1 a 6). */
class PerfilJugadorValidacionTest {

    private val hoy = LocalDate.of(2026, 11, 1)

    private fun adulto() = DatosJugador(
        nombres = "Juan",
        apellidos = "Pérez",
        fechaNacimiento = "2000-01-27",
        equipoId = "e1",
        posicion = "Extremo",
    )

    private fun menor() = adulto().copy(
        fechaNacimiento = "2013-03-10",
        emergenciaNombre = "Rosa Pérez",
        emergenciaParentesco = "Madre",
        emergenciaTelefono = "987654321",
        apoderadoCorreo = "rosa@correo.com",
    )

    @Test
    fun catalogo_tieneLasNuevePosicionesDeLaHistoria() {
        assertEquals(
            listOf(
                "Arquero", "Defensa central", "Lateral derecho", "Lateral izquierdo", "Volante defensivo",
                "Volante mixto", "Volante ofensivo", "Extremo", "Delantero",
            ),
            PosicionJugador.entries.map { it.etiqueta },
        )
    }

    @Test
    fun posicionesAnteriores_seTraducenParaNoPerderAvisos() {
        assertEquals(PosicionJugador.ARQUERO, PosicionJugador.desde("PORTERO"))
        assertEquals(PosicionJugador.VOLANTE_MIXTO, PosicionJugador.desde("VOLANTE_CENTRAL"))
        assertEquals(PosicionJugador.ARQUERO, PosicionJugador.desdeEtiqueta("Portero"))
        assertNull(PosicionJugador.desde("LIBERO"))
    }

    @Test
    fun posicionSecundaria_esOpcionalYDistintaDeLaPrincipal() {
        assertTrue(ValidacionJugador.validar(adulto().copy(posicionSecundaria = "Delantero"), hoy).isEmpty())
        val errores = ValidacionJugador.validar(adulto().copy(posicionSecundaria = "Extremo"), hoy)
        assertTrue(CampoJugador.POSICION_SECUNDARIA in errores)
    }

    @Test
    fun dorsal_unicoDentroDelEquipo() {
        val companeros = listOf(
            Jugador(id = "j9", equipoId = "e1", dorsal = 10),
            Jugador(id = "j8", equipoId = "e2", dorsal = 7),
            Jugador(id = "j7", equipoId = "e1", dorsal = 5, activo = false),
        )
        val tomado = ValidacionJugador.validar(adulto().copy(dorsal = "10"), hoy, companeros)
        assertEquals("El número ya pertenece a otro jugador del equipo", tomado[CampoJugador.DORSAL])
        // Mismo número en otro equipo, o de un jugador inactivo: permitido.
        assertTrue(ValidacionJugador.validar(adulto().copy(dorsal = "7"), hoy, companeros).isEmpty())
        assertTrue(ValidacionJugador.validar(adulto().copy(dorsal = "5"), hoy, companeros).isEmpty())
        // Al editar al propio j9 no choca consigo mismo.
        assertTrue(ValidacionJugador.validar(adulto().copy(dorsal = "10"), hoy, companeros, idEditando = "j9").isEmpty())
    }

    @Test
    fun estaturaEnMetros_entre1y2_30() {
        assertTrue(ValidacionJugador.validar(adulto().copy(estatura = "1,76"), hoy).isEmpty())
        assertTrue(ValidacionJugador.validar(adulto().copy(estatura = "1.00"), hoy).isEmpty())
        assertTrue(ValidacionJugador.validar(adulto().copy(estatura = "2,30"), hoy).isEmpty())
        listOf("0,99", "2,31", "176", "alto").forEach { valor ->
            assertEquals("estatura '$valor'", "Valor fuera de rango", ValidacionJugador.validar(adulto().copy(estatura = valor), hoy)[CampoJugador.ESTATURA])
        }
    }

    @Test
    fun peso_entre20y150kg() {
        assertTrue(ValidacionJugador.validar(adulto().copy(peso = "20"), hoy).isEmpty())
        assertTrue(ValidacionJugador.validar(adulto().copy(peso = "150"), hoy).isEmpty())
        assertEquals("Valor fuera de rango", ValidacionJugador.validar(adulto().copy(peso = "19,9"), hoy)[CampoJugador.PESO])
        assertEquals("Valor fuera de rango", ValidacionJugador.validar(adulto().copy(peso = "151"), hoy)[CampoJugador.PESO])
    }

    @Test
    fun estatura_seGuardaEnCentimetrosYSeMuestraEnMetros() {
        assertEquals(176, ValidacionJugador.estaturaACentimetros("1,76"))
        assertEquals("1,76", ValidacionJugador.centimetrosAMetros(176))
        assertEquals("1,05", ValidacionJugador.centimetrosAMetros(105))
        assertEquals("", ValidacionJugador.centimetrosAMetros(null))
    }

    @Test
    fun contactoDeEmergencia_obligatorioSoloParaMenores() {
        val errores = ValidacionJugador.validar(
            menor().copy(emergenciaNombre = "", emergenciaTelefono = "", emergenciaParentesco = ""),
            hoy,
        )
        assertEquals("Obligatorio para menores de edad", errores[CampoJugador.EMERGENCIA_NOMBRE])
        assertEquals("Obligatorio para menores de edad", errores[CampoJugador.EMERGENCIA_TELEFONO])
        assertEquals("Obligatorio para menores de edad", errores[CampoJugador.EMERGENCIA_PARENTESCO])
        // Un mayor de edad puede no registrar contacto de emergencia.
        assertTrue(ValidacionJugador.validar(adulto(), hoy).isEmpty())
    }

    @Test
    fun telefonoDeEmergencia_exactamenteNueveDigitos() {
        listOf("98765432", "9876543210", "98765432a").forEach { telefono ->
            assertEquals(
                "teléfono '$telefono'",
                "Ingrese un número de 9 dígitos",
                ValidacionJugador.validar(menor().copy(emergenciaTelefono = telefono), hoy)[CampoJugador.EMERGENCIA_TELEFONO],
            )
        }
        assertTrue(ValidacionJugador.validar(menor().copy(emergenciaTelefono = "012345678"), hoy).isEmpty())
    }

    @Test
    fun documentoYPieHabil_sonOpcionalesPeroValidados() {
        assertTrue(ValidacionJugador.validar(adulto().copy(documento = "71234567", pieHabil = "Izquierdo"), hoy).isEmpty())
        assertTrue(CampoJugador.DOCUMENTO in ValidacionJugador.validar(adulto().copy(documento = "123"), hoy))
        assertTrue(CampoJugador.PIE_HABIL in ValidacionJugador.validar(adulto().copy(pieHabil = "Zurdo"), hoy))
    }
}

/** US-006: privacidad y edición del perfil (criterios 8 y 9). */
class VisibilidadJugadorTest {

    private val jugador = Jugador(id = "j1", usuarioUid = "u-jugador")

    @Test
    fun personalDeLaAcademia_veLosDatosSensibles() {
        assertTrue(VisibilidadJugador.puedeVerDatosSensibles(UserRole.ADMINISTRADOR, "a1", jugador, vinculoAprobado = false))
        assertTrue(VisibilidadJugador.puedeVerDatosSensibles(UserRole.ENTRENADOR, "dt1", jugador, vinculoAprobado = false))
    }

    @Test
    fun apoderado_soloConVinculoAprobado() {
        assertTrue(VisibilidadJugador.puedeVerDatosSensibles(UserRole.PADRE_DE_FAMILIA, "p1", jugador, vinculoAprobado = true))
        assertFalse(VisibilidadJugador.puedeVerDatosSensibles(UserRole.PADRE_DE_FAMILIA, "p1", jugador, vinculoAprobado = false))
    }

    @Test
    fun jugador_soloSusPropiosDatos() {
        assertTrue(VisibilidadJugador.puedeVerDatosSensibles(UserRole.JUGADOR, "u-jugador", jugador, vinculoAprobado = false))
        assertFalse(VisibilidadJugador.puedeVerDatosSensibles(UserRole.JUGADOR, "otro", jugador, vinculoAprobado = false))
        // Ficha sin cuenta enlazada: ningún jugador la ve como propia.
        assertFalse(VisibilidadJugador.puedeVerDatosSensibles(UserRole.JUGADOR, "", Jugador(id = "j2"), vinculoAprobado = false))
    }

    @Test
    fun paraOtrosUsuarios_seMuestraInformacionRestringida() {
        assertEquals("Información restringida", VisibilidadJugador.mostrar("1,76 m", puedeVer = false))
        assertEquals("1,76 m", VisibilidadJugador.mostrar("1,76 m", puedeVer = true))
    }

    @Test
    fun elJugadorEditaSoloFotoTelefonoYPieHabil() {
        assertEquals(
            setOf(CampoEditableJugador.FOTO, CampoEditableJugador.TELEFONO, CampoEditableJugador.PIE_HABIL),
            VisibilidadJugador.camposEditables(UserRole.JUGADOR, "u-jugador", jugador),
        )
        assertTrue(VisibilidadJugador.camposEditables(UserRole.JUGADOR, "otro", jugador).isEmpty())
        assertTrue(VisibilidadJugador.camposEditables(UserRole.PADRE_DE_FAMILIA, "p1", jugador).isEmpty())
        assertTrue(VisibilidadJugador.puedeEditarFichaCompleta(UserRole.ENTRENADOR))
        assertFalse(VisibilidadJugador.puedeEditarFichaCompleta(UserRole.JUGADOR))
    }
}
