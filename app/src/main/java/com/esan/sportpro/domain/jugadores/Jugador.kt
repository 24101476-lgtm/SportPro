package com.esan.sportpro.domain.jugadores

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

/**
 * Perfil de un jugador de la academia (US-006/US-007). Colección Firestore: `jugadores`.
 *
 * Contiene datos físicos y de contacto de personas que pueden ser menores de edad, por eso solo
 * lo leen el administrador y el entrenador (ver `firestore.rules` y [DatosJugador]).
 * [fechaNacimiento] se guarda como texto ISO (`2012-05-30`) para no depender de zonas horarias.
 * Los jugadores no se borran: se desactivan ([activo] = false).
 */
data class Jugador(
    @DocumentId
    val id: String = "",
    val academiaId: String = "",
    val equipoId: String = "",
    val equipoNombre: String = "",
    val nombres: String = "",
    val apellidos: String = "",
    val fechaNacimiento: String = "",
    val posicion: String = "",
    /** US-006: posición secundaria (etiqueta del catálogo) o vacía. */
    val posicionSecundaria: String = "",
    /** US-006: etiqueta de [PieHabil] o vacía. */
    val pieHabil: String = "",
    /** US-006: DNI o carné de extranjería. Dato sensible: misma visibilidad que los físicos. */
    val documentoIdentidad: String = "",
    val dorsal: Int? = null,
    /** Se guarda en centímetros (datos anteriores); la ficha la muestra y pide en metros (US-006). */
    val estaturaCm: Int? = null,
    val pesoKg: Double? = null,
    val telefono: String = "",
    val correo: String = "",
    val contactoEmergenciaNombre: String = "",
    val contactoEmergenciaTelefono: String = "",
    val contactoEmergenciaParentesco: String = "",
    /** URL de la foto en Supabase Storage (US-006); vacía si no tiene. */
    val fotoUrl: String = "",
    /** Código de 6 caracteres que la academia entrega al apoderado para pedir la vinculación (US-007). */
    val codigoVinculacion: String = "",
    /** Correo del apoderado (obligatorio en menores de edad). */
    val apoderadoCorreo: String = "",
    /** Nombre de [EstadoRegistro]; vacío en fichas antiguas (= registrado). Ver [ReglasApoderado]. */
    val estadoRegistro: String = "",
    /**
     * Cuenta de usuario (rol Jugador) enlazada a esta ficha; vacía si el jugador no tiene cuenta.
     * Permite que el propio jugador vea su perfil y edite foto, teléfono y pie hábil (US-006).
     */
    val usuarioUid: String = "",
    val activo: Boolean = true,
    val creadoPor: String = "",
) {
    @get:Exclude
    val nombreCompleto: String get() = "$nombres $apellidos".trim()

    /** Iniciales para el avatar cuando no hay foto: "Luis Pérez" -> "LP". */
    @get:Exclude
    val iniciales: String
        get() = listOf(nombres, apellidos).mapNotNull { it.trim().firstOrNull()?.uppercaseChar() }.joinToString("")

    /** Edad en años cumplidos a la fecha [hoy]; `null` si la fecha de nacimiento no es válida. */
    fun edad(hoy: LocalDate = LocalDate.now()): Int? =
        FechasJugador.parsear(fechaNacimiento)?.let { FechasJugador.edad(it, hoy) }

    fun esMenor(hoy: LocalDate = LocalDate.now()): Boolean = (edad(hoy) ?: 0) < EDAD_MAYORIA

    companion object {
        const val EDAD_MAYORIA = 18
    }
}

/** Utilidades de fecha sin dependencias de Android, probables con pruebas unitarias. */
object FechasJugador {
    fun parsear(texto: String): LocalDate? = try {
        LocalDate.parse(texto.trim())
    } catch (e: DateTimeParseException) {
        null
    }

    fun edad(nacimiento: LocalDate, hoy: LocalDate): Int = ChronoUnit.YEARS.between(nacimiento, hoy).toInt()
}

/** Pie hábil del jugador (US-006). */
enum class PieHabil(val etiqueta: String) {
    DERECHO("Derecho"),
    IZQUIERDO("Izquierdo"),
    AMBIDIESTRO("Ambidiestro"),
}
