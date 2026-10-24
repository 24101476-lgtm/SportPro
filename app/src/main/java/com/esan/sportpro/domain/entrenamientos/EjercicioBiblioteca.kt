package com.esan.sportpro.domain.entrenamientos

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import java.text.Normalizer

/**
 * Ejercicio guardado en la biblioteca reutilizable del club (US-009). Colección Firestore:
 * `ejercicios`. Lo ven todos los entrenadores de la academia; solo su autor ([creadoPor]) o el
 * administrador lo editan, desactivan o eliminan.
 *
 * [categoria] guarda el `name` de [CategoriaTecnica]. Los ejercicios usados en sesiones ya
 * realizadas no se eliminan: se desactivan ([activo] = false) y dejan de aparecer en el buscador,
 * pero las sesiones históricas los conservan porque guardan su propia copia ([Ejercicio]).
 */
data class EjercicioBiblioteca(
    @DocumentId
    val id: String = "",
    val academiaId: String = "",
    val nombre: String = "",
    val objetivo: String = "",
    val categoria: String = "",
    val duracionMin: Int = 0,
    val minJugadores: Int = 0,
    val materiales: String = "",
    val descripcion: String = "",
    val imagenUrl: String = "",
    val activo: Boolean = true,
    val creadoPor: String = "",
) {
    @get:Exclude
    val categoriaTecnica: CategoriaTecnica? get() = CategoriaTecnica.desde(categoria)

    /** Copia para agregar a una sesión: la duración puede ajustarse sin tocar la biblioteca. */
    fun aEjercicioDeSesion(): Ejercicio = Ejercicio(
        nombre = nombre,
        descripcion = descripcion,
        duracionMin = duracionMin,
        ejercicioId = id,
    )
}

/** Categoría técnica del ejercicio (US-009, criterio 3). */
enum class CategoriaTecnica(val etiqueta: String) {
    CALENTAMIENTO("Calentamiento"),
    TECNICA_INDIVIDUAL("Técnica individual"),
    PASE_Y_CONTROL("Pase y control"),
    DEFINICION("Definición"),
    TACTICA_OFENSIVA("Táctica ofensiva"),
    TACTICA_DEFENSIVA("Táctica defensiva"),
    FISICO("Físico"),
    ARQUEROS("Arqueros"),
    VUELTA_A_LA_CALMA("Vuelta a la calma"),
    ;

    companion object {
        fun desde(nombre: String?): CategoriaTecnica? = entries.firstOrNull { it.name == nombre }
    }
}

/** Rangos del filtro por duración del buscador (US-009, criterio 6). */
enum class RangoDuracion(val etiqueta: String, val rango: IntRange) {
    HASTA_10("Hasta 10 min", 1..10),
    DE_11_A_20("11 a 20 min", 11..20),
    DE_21_A_30("21 a 30 min", 21..30),
    MAS_DE_30("Más de 30 min", 31..ValidacionBiblioteca.DURACION_MAX),
}

/** Criterios del buscador de la biblioteca. `null` = sin filtrar por ese criterio. */
data class FiltroBiblioteca(
    val texto: String = "",
    val categoria: CategoriaTecnica? = null,
    val duracion: RangoDuracion? = null,
) {
    val activo: Boolean get() = texto.isNotBlank() || categoria != null || duracion != null
}

/** Reglas de negocio de la biblioteca. Sin dependencias de Android ni Firebase. */
object BibliotecaEjercicios {

    /**
     * Ejercicios que muestra el buscador: solo los activos (criterio 8), filtrados por nombre
     * (sin distinguir mayúsculas ni tildes), categoría y duración, ordenados por nombre.
     * Con [desactivados] = `true` busca entre los desactivados (para poder reactivarlos).
     */
    fun buscar(
        ejercicios: List<EjercicioBiblioteca>,
        filtro: FiltroBiblioteca,
        desactivados: Boolean = false,
    ): List<EjercicioBiblioteca> {
        val texto = normalizar(filtro.texto)
        return ejercicios
            .filter { it.activo != desactivados }
            .filter { texto.isEmpty() || normalizar(it.nombre).contains(texto) }
            .filter { filtro.categoria == null || it.categoria == filtro.categoria.name }
            .filter { filtro.duracion == null || it.duracionMin in filtro.duracion.rango }
            .sortedBy { normalizar(it.nombre) }
    }

    /** Solo el autor del ejercicio o el administrador del club lo editan o desactivan (criterio 7). */
    fun puedeEditar(ejercicio: EjercicioBiblioteca, uid: String, esAdmin: Boolean): Boolean =
        esAdmin || (ejercicio.creadoPor.isNotBlank() && ejercicio.creadoPor == uid)

    /**
     * `true` si el ejercicio figura en alguna sesión ya realizada (criterio 8). Las sesiones
     * anteriores a esta versión no guardaban `ejercicioId`, por eso también se compara el nombre.
     */
    fun usadoEnSesionRealizada(ejercicio: EjercicioBiblioteca, entrenamientos: List<Entrenamiento>): Boolean {
        val nombre = normalizar(ejercicio.nombre)
        return entrenamientos
            .filter { it.realizada }
            .any { sesion ->
                sesion.ejercicios.any { usado ->
                    if (usado.ejercicioId.isNotBlank()) {
                        usado.ejercicioId == ejercicio.id
                    } else {
                        normalizar(usado.nombre) == nombre
                    }
                }
            }
    }

    /** Minúsculas, sin tildes y sin espacios repetidos: "  Rondó  4v2" -> "rondo 4v2". */
    fun normalizar(texto: String): String =
        Normalizer.normalize(texto.trim().lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("\\s+"), " ")
}
