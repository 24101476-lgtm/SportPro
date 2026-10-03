package com.esan.sportpro.domain.academia

import com.google.firebase.firestore.DocumentId

/**
 * Equipo de una academia, organizado por categoría (sub-10, sub-15, primera...).
 * Colección Firestore: `equipos`. Los equipos no se borran: se desactivan ([activo] = false)
 * para conservar el historial de jugadores, entrenamientos y partidos que los referencian.
 */
data class Equipo(
    @DocumentId
    val id: String = "",
    val academiaId: String = "",
    val nombre: String = "",
    val categoria: String = "",
    val entrenadorNombre: String = "",
    val activo: Boolean = true,
    val creadoPor: String = "",
) {
    /** Texto que se muestra en listas y selectores, p. ej. "Tigres · Sub-15". */
    val etiqueta: String get() = "$nombre · $categoria"
}

/** Categorías disponibles al crear un equipo. */
object CategoriasEquipo {
    val todas: List<String> = listOf(
        "Sub-8", "Sub-10", "Sub-12", "Sub-13", "Sub-15", "Sub-17", "Sub-20", "Primera",
    )
}
