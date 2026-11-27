package com.esan.sportpro.domain.academia

import java.time.LocalDate

enum class CampoEquipo { NOMBRE, CATEGORIA, ENTRENADOR, ANIOS }

/** Mensajes exactos de los criterios de US-005. */
object MensajesEquipo {
    const val ENTRENADOR_REQUERIDO = "Debe asignar un entrenador"
    const val RANGO_INVALIDO = "El rango de años es inválido"
    const val ANIOS_REQUERIDOS = "Ingresa el año mínimo y el año máximo de nacimiento"
    const val CATEGORIA_REQUERIDA = "Selecciona una categoría."
    const val OTRA_REQUERIDA = "Escribe el nombre de la categoría"
    const val OTRA_MAXIMO = "Máximo ${CategoriasEquipo.OTRA_MAX} caracteres"
    const val FUERA_DE_RANGO = "El jugador no cumple el rango de la categoría"
    const val NO_SE_PUEDE_ELIMINAR = "No puedes eliminar un equipo con jugadores asignados"
}

/** Reglas de validación del formulario de equipo, sin dependencias de Android ni Firebase. */
object ValidacionEquipo {
    const val NOMBRE_MIN = 3
    const val NOMBRE_MAX = 40
    const val ENTRENADOR_MAX = 60
    const val ANIO_MINIMO_PERMITIDO = 1900

    /**
     * @param categoria opción elegida en el selector ("Sub-15", "Otra"...).
     * @param categoriaOtra texto libre cuando [categoria] es "Otra".
     * @param entrenadorUid cuenta del entrenador asignado; si está vacío vale el nombre escrito.
     * @param existentes equipos ya registrados en la academia, para evitar duplicados.
     * @param idEditando id del equipo que se está editando (se excluye de la comparación).
     */
    fun validar(
        nombre: String,
        categoria: String,
        entrenadorNombre: String,
        existentes: List<Equipo>,
        idEditando: String? = null,
        categoriaOtra: String = "",
        entrenadorUid: String = "",
        anioMinimo: Int? = null,
        anioMaximo: Int? = null,
        anioActual: Int = LocalDate.now().year,
    ): Map<CampoEquipo, String> {
        val errores = mutableMapOf<CampoEquipo, String>()
        val nombreLimpio = nombre.trim()

        when {
            nombreLimpio.length < NOMBRE_MIN ->
                errores[CampoEquipo.NOMBRE] = "El nombre debe tener al menos $NOMBRE_MIN caracteres."
            nombreLimpio.length > NOMBRE_MAX ->
                errores[CampoEquipo.NOMBRE] = "El nombre no puede superar $NOMBRE_MAX caracteres."
        }

        when {
            categoria !in CategoriasEquipo.todas -> errores[CampoEquipo.CATEGORIA] = MensajesEquipo.CATEGORIA_REQUERIDA
            categoria == CategoriasEquipo.OTRA && categoriaOtra.isBlank() ->
                errores[CampoEquipo.CATEGORIA] = MensajesEquipo.OTRA_REQUERIDA
            categoria == CategoriasEquipo.OTRA && categoriaOtra.trim().length > CategoriasEquipo.OTRA_MAX ->
                errores[CampoEquipo.CATEGORIA] = MensajesEquipo.OTRA_MAXIMO
        }

        when {
            entrenadorUid.isBlank() && entrenadorNombre.isBlank() ->
                errores[CampoEquipo.ENTRENADOR] = MensajesEquipo.ENTRENADOR_REQUERIDO
            entrenadorNombre.trim().length > ENTRENADOR_MAX ->
                errores[CampoEquipo.ENTRENADOR] = "El nombre del entrenador no puede superar $ENTRENADOR_MAX caracteres."
        }

        validarRango(anioMinimo, anioMaximo, anioActual)?.let { errores[CampoEquipo.ANIOS] = it }

        val categoriaFinal = CategoriasEquipo.categoriaFinal(categoria, categoriaOtra)
        val duplicado = existentes.any {
            it.id != idEditando &&
                it.nombre.trim().equals(nombreLimpio, ignoreCase = true) &&
                it.categoria.equals(categoriaFinal, ignoreCase = true)
        }
        if (duplicado && CampoEquipo.NOMBRE !in errores) {
            errores[CampoEquipo.NOMBRE] = "Ya existe un equipo con ese nombre en la categoría $categoriaFinal."
        }
        return errores
    }

    /** Ambos años obligatorios, entre 1900 y el año actual, y el mínimo no mayor que el máximo. */
    fun validarRango(anioMinimo: Int?, anioMaximo: Int?, anioActual: Int = LocalDate.now().year): String? = when {
        anioMinimo == null || anioMaximo == null -> MensajesEquipo.ANIOS_REQUERIDOS
        anioMinimo !in ANIO_MINIMO_PERMITIDO..anioActual || anioMaximo !in ANIO_MINIMO_PERMITIDO..anioActual ->
            MensajesEquipo.RANGO_INVALIDO
        anioMinimo > anioMaximo -> MensajesEquipo.RANGO_INVALIDO
        else -> null
    }

    /**
     * Rango sugerido al elegir una categoría "Sub-N": jugadores que cumplen como máximo N-1 años en
     * el año actual (p. ej. Sub-15 en 2026 → nacidos entre 2012 y 2013).
     */
    fun rangoSugerido(categoria: String, anioActual: Int = LocalDate.now().year): Pair<Int, Int>? {
        val edad = categoria.removePrefix("Sub-").toIntOrNull() ?: return null
        val masJoven = anioActual - (edad - 2)
        val masGrande = anioActual - (edad - 1)
        return masGrande to masJoven
    }

    /** Un equipo con jugadores activos no se elimina: se desactiva para conservar su historial. */
    fun puedeEliminar(jugadoresAsignados: Int): Boolean = jugadoresAsignados == 0
}
