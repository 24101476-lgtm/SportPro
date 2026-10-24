package com.esan.sportpro.domain.entrenamientos

import kotlinx.coroutines.flow.Flow

/**
 * Acceso a entrenamientos, asistencia y biblioteca de ejercicios. Implementación:
 * `data/entrenamientos/FirestoreEntrenamientoRepository`. Todas las escrituras devuelven `true`
 * si el servidor confirmó y `false` si quedaron pendientes por falta de conexión.
 */
interface EntrenamientoRepository {

    /** Sesiones de la academia, de la más próxima/reciente a la más antigua. */
    fun observarEntrenamientos(academiaId: String): Flow<List<Entrenamiento>>

    /** Crea la sesión si [Entrenamiento.id] está vacío; si no, actualiza su planificación. */
    suspend fun guardar(entrenamiento: Entrenamiento): Boolean

    /**
     * Guarda la asistencia de la sesión: `jugadorId -> nombre de EstadoAsistencia`. Reemplaza la
     * asistencia anterior de esa sesión y marca `asistenciaRegistrada = true`. Además escribe un
     * registro por jugador en `asistencias_entrenamiento`, que es la colección que consume el
     * módulo de Estadísticas (campos `equipoId`, `jugadorId`, `presente` y `fecha`).
     */
    suspend fun registrarAsistencia(entrenamiento: Entrenamiento, asistencia: Map<String, String>): Boolean

    // ---- Biblioteca de ejercicios (US-009) ------------------------------------------------

    /** Ejercicios de la biblioteca de la academia (activos e inactivos), por nombre. */
    fun observarBiblioteca(academiaId: String): Flow<List<EjercicioBiblioteca>>

    /**
     * Crea el ejercicio si [EjercicioBiblioteca.id] está vacío; si no, lo actualiza.
     * @param imagenLocal URI local (`content://…`) de una imagen nueva elegida por el usuario, o
     * `null` para conservar la actual. Se comprime y se sube a `ejercicios/{id}/imagen.jpg`.
     * @param quitarImagen `true` para eliminar la imagen actual del ejercicio.
     */
    suspend fun guardarEnBiblioteca(
        ejercicio: EjercicioBiblioteca,
        imagenLocal: String? = null,
        quitarImagen: Boolean = false,
    ): ResultadoGuardarEjercicio

    /** Activa o desactiva un ejercicio; desactivado deja de aparecer en el buscador. */
    suspend fun cambiarEstadoEjercicio(ejercicioId: String, activo: Boolean): Boolean

    /**
     * Elimina un ejercicio que nunca se usó en una sesión realizada (la app lo verifica antes con
     * [BibliotecaEjercicios.usadoEnSesionRealizada]; si se usó, solo se desactiva).
     */
    suspend fun eliminarEjercicio(ejercicio: EjercicioBiblioteca): Boolean
}

/**
 * @property confirmado `true` si el servidor confirmó; `false` si quedó pendiente por falta de conexión.
 * @property imagenFallida `true` si el ejercicio se guardó pero la imagen no se pudo subir.
 */
data class ResultadoGuardarEjercicio(val confirmado: Boolean, val imagenFallida: Boolean = false)
