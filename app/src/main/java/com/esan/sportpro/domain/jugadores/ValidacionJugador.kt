package com.esan.sportpro.domain.jugadores

import com.esan.sportpro.domain.comunidad.PosicionJugador
import java.time.LocalDate

enum class CampoJugador {
    NOMBRES,
    APELLIDOS,
    FECHA_NACIMIENTO,
    EQUIPO,
    POSICION,
    DORSAL,
    ESTATURA,
    PESO,
    TELEFONO,
    CORREO,
    EMERGENCIA_NOMBRE,
    EMERGENCIA_TELEFONO,
    EMERGENCIA_PARENTESCO,
}

/** Valores del formulario del jugador tal como los escribe el usuario (todo texto). */
data class DatosJugador(
    val nombres: String = "",
    val apellidos: String = "",
    val fechaNacimiento: String = "",
    val equipoId: String = "",
    val posicion: String = "",
    val dorsal: String = "",
    val estatura: String = "",
    val peso: String = "",
    val telefono: String = "",
    val correo: String = "",
    val emergenciaNombre: String = "",
    val emergenciaTelefono: String = "",
    val emergenciaParentesco: String = "",
)

/** Reglas de validación del perfil del jugador. Sin dependencias de Android ni Firebase. */
object ValidacionJugador {
    private val REGEX_NOMBRE = Regex("^\\p{L}[\\p{L} .'-]*$")
    private val REGEX_TELEFONO = Regex("^9\\d{8}$")
    private val REGEX_CORREO = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    const val EDAD_MINIMA = 4
    const val EDAD_MAXIMA = 60

    fun validar(datos: DatosJugador, hoy: LocalDate = LocalDate.now()): Map<CampoJugador, String> {
        val errores = mutableMapOf<CampoJugador, String>()

        validarNombre(datos.nombres, 2, 50)?.let { errores[CampoJugador.NOMBRES] = it }
        validarNombre(datos.apellidos, 2, 50)?.let { errores[CampoJugador.APELLIDOS] = it }

        val nacimiento = FechasJugador.parsear(datos.fechaNacimiento)
        when {
            nacimiento == null ->
                errores[CampoJugador.FECHA_NACIMIENTO] = "Usa el formato AAAA-MM-DD (ej. 2012-05-30)."
            nacimiento.isAfter(hoy) ->
                errores[CampoJugador.FECHA_NACIMIENTO] = "La fecha de nacimiento no puede ser futura."
            FechasJugador.edad(nacimiento, hoy) !in EDAD_MINIMA..EDAD_MAXIMA ->
                errores[CampoJugador.FECHA_NACIMIENTO] = "La edad debe estar entre $EDAD_MINIMA y $EDAD_MAXIMA años."
        }

        if (datos.equipoId.isBlank()) errores[CampoJugador.EQUIPO] = "Selecciona un equipo."

        if (PosicionJugador.entries.none { it.etiqueta == datos.posicion }) {
            errores[CampoJugador.POSICION] = "Selecciona una posición."
        }

        if (datos.dorsal.isNotBlank()) {
            val dorsal = datos.dorsal.trim().toIntOrNull()
            if (dorsal == null || dorsal !in 1..99) errores[CampoJugador.DORSAL] = "El dorsal debe estar entre 1 y 99."
        }

        if (datos.estatura.isNotBlank()) {
            val estatura = datos.estatura.trim().toIntOrNull()
            if (estatura == null || estatura !in 80..220) {
                errores[CampoJugador.ESTATURA] = "La estatura debe estar entre 80 y 220 cm."
            }
        }

        if (datos.peso.isNotBlank()) {
            val peso = parsearPeso(datos.peso)
            if (peso == null || peso < 15.0 || peso > 150.0) {
                errores[CampoJugador.PESO] = "El peso debe estar entre 15 y 150 kg."
            }
        }

        if (datos.telefono.isNotBlank() && !REGEX_TELEFONO.matches(datos.telefono.trim())) {
            errores[CampoJugador.TELEFONO] = "El teléfono debe tener 9 dígitos y empezar con 9."
        }

        if (datos.correo.isNotBlank() && !REGEX_CORREO.matches(datos.correo.trim())) {
            errores[CampoJugador.CORREO] = "Ingresa un correo válido."
        }

        // Contacto de emergencia: obligatorio para todos los jugadores, no solo para menores.
        validarNombre(datos.emergenciaNombre, 3, 60)?.let { errores[CampoJugador.EMERGENCIA_NOMBRE] = it }
        if (!REGEX_TELEFONO.matches(datos.emergenciaTelefono.trim())) {
            errores[CampoJugador.EMERGENCIA_TELEFONO] = "El teléfono de emergencia debe tener 9 dígitos y empezar con 9."
        }
        if (datos.emergenciaParentesco.trim().length !in 2..30) {
            errores[CampoJugador.EMERGENCIA_PARENTESCO] = "Indica el parentesco (2 a 30 caracteres)."
        }
        return errores
    }

    /** Acepta coma o punto decimal ("45,5" o "45.5"). */
    fun parsearPeso(texto: String): Double? = texto.trim().replace(',', '.').toDoubleOrNull()

    private fun validarNombre(valor: String, min: Int, max: Int): String? {
        val limpio = valor.trim()
        return when {
            limpio.length < min -> "Debe tener al menos $min caracteres."
            limpio.length > max -> "No puede superar $max caracteres."
            !REGEX_NOMBRE.matches(limpio) -> "Solo se permiten letras y espacios."
            else -> null
        }
    }
}
