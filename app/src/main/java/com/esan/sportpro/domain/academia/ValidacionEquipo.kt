package com.esan.sportpro.domain.academia

enum class CampoEquipo { NOMBRE, CATEGORIA, ENTRENADOR }

/** Reglas de validación del formulario de equipo, sin dependencias de Android ni Firebase. */
object ValidacionEquipo {
    const val NOMBRE_MIN = 3
    const val NOMBRE_MAX = 40
    const val ENTRENADOR_MAX = 60

    /**
     * @param existentes equipos ya registrados en la academia, para evitar duplicados.
     * @param idEditando id del equipo que se está editando (se excluye de la comparación).
     */
    fun validar(
        nombre: String,
        categoria: String,
        entrenadorNombre: String,
        existentes: List<Equipo>,
        idEditando: String? = null,
    ): Map<CampoEquipo, String> {
        val errores = mutableMapOf<CampoEquipo, String>()
        val nombreLimpio = nombre.trim()

        when {
            nombreLimpio.length < NOMBRE_MIN ->
                errores[CampoEquipo.NOMBRE] = "El nombre debe tener al menos $NOMBRE_MIN caracteres."
            nombreLimpio.length > NOMBRE_MAX ->
                errores[CampoEquipo.NOMBRE] = "El nombre no puede superar $NOMBRE_MAX caracteres."
        }

        if (categoria !in CategoriasEquipo.todas) {
            errores[CampoEquipo.CATEGORIA] = "Selecciona una categoría."
        }

        if (entrenadorNombre.trim().length > ENTRENADOR_MAX) {
            errores[CampoEquipo.ENTRENADOR] = "El nombre del entrenador no puede superar $ENTRENADOR_MAX caracteres."
        }

        val duplicado = existentes.any {
            it.id != idEditando &&
                it.nombre.trim().equals(nombreLimpio, ignoreCase = true) &&
                it.categoria == categoria
        }
        if (duplicado && CampoEquipo.NOMBRE !in errores) {
            errores[CampoEquipo.NOMBRE] = "Ya existe un equipo con ese nombre en la categoría $categoria."
        }
        return errores
    }
}
