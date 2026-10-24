package com.esan.sportpro.domain.entrenamientos

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude

/** Ejercicio dentro de una sesión de entrenamiento. */
data class Ejercicio(
    val nombre: String = "",
    val descripcion: String = "",
    val duracionMin: Int = 0,
)

/**
 * Sesión de entrenamiento planificada para un equipo (US-009 a US-013). Colección Firestore:
 * `entrenamientos`. [fecha] (`2026-10-15`) y [hora] (`16:30`) se guardan como texto.
 *
 * [asistencia] relaciona `jugadorId` con el nombre de un [EstadoAsistencia]. Se guarda dentro del
 * mismo documento para sincronizar y calcular el historial con un solo listener; solo contiene
 * ids y estados, nunca datos de contacto ni físicos.
 */
data class Entrenamiento(
    @DocumentId
    val id: String = "",
    val academiaId: String = "",
    val equipoId: String = "",
    val equipoNombre: String = "",
    val fecha: String = "",
    val hora: String = "",
    val lugar: String = "",
    val objetivo: String = "",
    val ejercicios: List<Ejercicio> = emptyList(),
    val asistencia: Map<String, String> = emptyMap(),
    val asistenciaRegistrada: Boolean = false,
    val creadoPor: String = "",
) {
    /** Duración total de la sesión: suma de la duración de sus ejercicios. */
    @get:Exclude
    val duracionTotalMin: Int get() = ejercicios.sumOf { it.duracionMin }

    /** Clave para ordenar cronológicamente: `2026-10-15 16:30`. */
    @get:Exclude
    val clave: String get() = "$fecha $hora"
}

/** Ejercicio guardado en la biblioteca reutilizable de la academia (`ejercicios`). */
data class EjercicioBiblioteca(
    @DocumentId
    val id: String = "",
    val academiaId: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val duracionMin: Int = 0,
)

/** Estado de asistencia de un jugador a un entrenamiento (US-009 a US-013). */
enum class EstadoAsistencia(val etiqueta: String) {
    PRESENTE("Presente"),
    TARDE("Tarde"),
    JUSTIFICADO("Justificado"),
    AUSENTE("Ausente"),
    ;

    /** Presente y tarde cuentan como participación; justificado y ausente no. */
    val participo: Boolean get() = this == PRESENTE || this == TARDE

    companion object {
        fun desde(nombre: String?): EstadoAsistencia? = entries.firstOrNull { it.name == nombre }
    }
}
