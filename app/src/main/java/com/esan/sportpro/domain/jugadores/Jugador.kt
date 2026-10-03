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
    val dorsal: Int? = null,
    val estaturaCm: Int? = null,
    val pesoKg: Double? = null,
    val telefono: String = "",
    val correo: String = "",
    val contactoEmergenciaNombre: String = "",
    val contactoEmergenciaTelefono: String = "",
    val contactoEmergenciaParentesco: String = "",
    val activo: Boolean = true,
    val creadoPor: String = "",
) {
    @get:Exclude
    val nombreCompleto: String get() = "$nombres $apellidos".trim()

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
