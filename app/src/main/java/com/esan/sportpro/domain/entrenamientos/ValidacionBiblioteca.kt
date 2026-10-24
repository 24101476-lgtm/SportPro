package com.esan.sportpro.domain.entrenamientos

enum class CampoBiblioteca { NOMBRE, OBJETIVO, CATEGORIA, DURACION, MIN_JUGADORES, MATERIALES, DESCRIPCION }

/** Valores del formulario de la biblioteca tal como los escribe el usuario (todo texto). */
data class DatosEjercicioBiblioteca(
    val nombre: String = "",
    val objetivo: String = "",
    val categoria: CategoriaTecnica? = null,
    val duracion: String = "",
    val minJugadores: String = "",
    val materiales: String = "",
    val descripcion: String = "",
)

/** Validación del ejercicio de la biblioteca (US-009). Sin dependencias de Android ni Firebase. */
object ValidacionBiblioteca {
    const val NOMBRE_MIN = 3
    const val NOMBRE_MAX = 60
    const val OBJETIVO_MIN = 5
    const val OBJETIVO_MAX = 150
    const val DURACION_MIN = 1
    const val DURACION_MAX = 120
    const val JUGADORES_MIN = 1
    const val JUGADORES_MAX = 40
    const val MATERIALES_MAX = 200
    const val DESCRIPCION_MIN = 20
    const val DESCRIPCION_MAX = 1000

    const val MSG_NOMBRE_DUPLICADO = "Ya existe un ejercicio con ese nombre"
    const val MSG_DURACION_INVALIDA = "Duración inválida"

    /**
     * @param existentes ejercicios del club, activos e inactivos: el nombre es único en todo el club.
     * @param idEditando id del ejercicio que se edita, para no compararlo consigo mismo.
     */
    fun validar(
        datos: DatosEjercicioBiblioteca,
        existentes: List<EjercicioBiblioteca>,
        idEditando: String? = null,
    ): Map<CampoBiblioteca, String> {
        val errores = mutableMapOf<CampoBiblioteca, String>()

        val nombre = datos.nombre.trim()
        val nombreNormalizado = BibliotecaEjercicios.normalizar(nombre)
        when {
            nombre.length !in NOMBRE_MIN..NOMBRE_MAX ->
                errores[CampoBiblioteca.NOMBRE] = "El nombre debe tener entre $NOMBRE_MIN y $NOMBRE_MAX caracteres."
            existentes.any { it.id != idEditando && BibliotecaEjercicios.normalizar(it.nombre) == nombreNormalizado } ->
                errores[CampoBiblioteca.NOMBRE] = MSG_NOMBRE_DUPLICADO
        }

        if (datos.objetivo.trim().length !in OBJETIVO_MIN..OBJETIVO_MAX) {
            errores[CampoBiblioteca.OBJETIVO] = "El objetivo debe tener entre $OBJETIVO_MIN y $OBJETIVO_MAX caracteres."
        }

        if (datos.categoria == null) errores[CampoBiblioteca.CATEGORIA] = "Selecciona una categoría técnica."

        val duracion = datos.duracion.trim().toIntOrNull()
        if (duracion == null || duracion !in DURACION_MIN..DURACION_MAX) {
            errores[CampoBiblioteca.DURACION] = MSG_DURACION_INVALIDA
        }

        val jugadores = datos.minJugadores.trim().toIntOrNull()
        if (jugadores == null || jugadores !in JUGADORES_MIN..JUGADORES_MAX) {
            errores[CampoBiblioteca.MIN_JUGADORES] = "Ingresa un número entre $JUGADORES_MIN y $JUGADORES_MAX."
        }

        if (datos.materiales.trim().length > MATERIALES_MAX) {
            errores[CampoBiblioteca.MATERIALES] = "Los materiales no pueden superar $MATERIALES_MAX caracteres."
        }

        if (datos.descripcion.trim().length !in DESCRIPCION_MIN..DESCRIPCION_MAX) {
            errores[CampoBiblioteca.DESCRIPCION] =
                "La descripción debe tener entre $DESCRIPCION_MIN y $DESCRIPCION_MAX caracteres."
        }
        return errores
    }

    /** Texto del contador de la descripción (criterio 5). */
    fun caracteresRestantes(descripcion: String): String {
        val restantes = DESCRIPCION_MAX - descripcion.length
        return if (restantes >= 0) "$restantes caracteres restantes" else "${-restantes} caracteres de más"
    }
}
