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
    /** US-005: rango de años de nacimiento de la categoría. `null` en equipos antiguos. */
    val anioMinimo: Int? = null,
    val anioMaximo: Int? = null,
    /** US-005: color de uniforme en hexadecimal ("#1565C0"). Vacío = sin definir. */
    val colorUniforme: String = "",
) {
    /** true si el año de nacimiento está dentro del rango del equipo (o el equipo no tiene rango). */
    fun admiteAnio(anioNacimiento: Int): Boolean {
        val minimo = anioMinimo ?: return true
        val maximo = anioMaximo ?: return true
        return anioNacimiento in minimo..maximo
    }

    /** Texto del rango para la lista: "2011 – 2012". Vacío si el equipo no lo tiene. */
    val rangoAnios: String
        get() = if (anioMinimo != null && anioMaximo != null) "$anioMinimo – $anioMaximo" else ""

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

/** US-005: categorías del criterio de aceptación. "Otra" habilita un texto libre de hasta 30 caracteres. */
object CategoriasEquipo {
    const val OTRA = "Otra"
    const val OTRA_MAX = 30

    val todas: List<String> = listOf(
        "Sub-8", "Sub-10", "Sub-12", "Sub-15", "Sub-17", "Sub-20", "Primera", OTRA,
    )

    /** Categorías fijas (sin "Otra"). Un equipo con otra categoría guarda su propio texto. */
    val fijas: List<String> = todas - OTRA

    /** Para el selector: "Sub-15" se elige tal cual; "Sub-13" o cualquier texto propio cae en "Otra". */
    fun opcionDe(categoria: String): String? = when {
        categoria.isBlank() -> null
        categoria in fijas -> categoria
        else -> OTRA
    }

    /** Lo que se guarda en Firestore: la categoría fija o el texto de "Otra". */
    fun categoriaFinal(opcion: String, textoOtra: String): String =
        if (opcion == OTRA) textoOtra.trim() else opcion
}

/** US-005: colores de uniforme que se ofrecen al crear un equipo. */
object ColoresUniforme {
    val opciones: List<Pair<String, String>> = listOf(
        "Blanco" to "#FFFFFF",
        "Negro" to "#212121",
        "Rojo" to "#C62828",
        "Azul" to "#1565C0",
        "Celeste" to "#4FC3F7",
        "Verde" to "#2E7D32",
        "Amarillo" to "#FBC02D",
        "Naranja" to "#EF6C00",
        "Morado" to "#6A1B9A",
        "Granate" to "#7B1F2E",
    )

    fun nombreDe(hex: String): String = opciones.firstOrNull { it.second.equals(hex, ignoreCase = true) }?.first ?: hex
}
