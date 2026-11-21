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
    /**
     * Cuenta (uid en `usuarios`) del entrenador que dirige el equipo. Es lo que usan las reglas de
     * Firestore para dejarle ver las mensualidades de su equipo. Vacío en equipos antiguos, que
     * solo guardaban [entrenadorNombre].
     */
    val entrenadorUid: String = "",
    /** Escudo subido por el administrador (URL pública de Supabase). Vacío = se muestra uno generado. */
    val escudoUrl: String = "",
    val activo: Boolean = true,
    val creadoPor: String = "",
) {
    /** Texto que se muestra en listas y selectores, p. ej. "Tigres · Sub-15". */
    val etiqueta: String get() = "$nombre · $categoria"

    /**
     * true si el equipo lo dirige esa cuenta. Los equipos antiguos sin [entrenadorUid] se comparan
     * por nombre, como se hacía antes.
     */
    fun dirigidoPor(uid: String, nombre: String): Boolean =
        if (entrenadorUid.isNotBlank()) entrenadorUid == uid
        else entrenadorNombre.isNotBlank() && normalizarNombre(entrenadorNombre) == normalizarNombre(nombre)

    /** Sin entrenador asignado (ni cuenta ni nombre). */
    fun sinEntrenador(): Boolean = entrenadorUid.isBlank() && entrenadorNombre.isBlank()
}

/** Cuenta de entrenador que se puede asignar a un equipo. */
data class EntrenadorCuenta(val uid: String, val nombre: String)

private fun normalizarNombre(texto: String): String =
    java.text.Normalizer.normalize(texto.trim().lowercase(), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("\\s+"), " ")

/** Categorías disponibles al crear un equipo. */
object CategoriasEquipo {
    val todas: List<String> = listOf(
        "Sub-8", "Sub-10", "Sub-12", "Sub-13", "Sub-15", "Sub-17", "Sub-20", "Primera",
    )
}
