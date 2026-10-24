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

    /** Ejercicios guardados en la biblioteca de la academia, por nombre. */
    fun observarBiblioteca(academiaId: String): Flow<List<EjercicioBiblioteca>>

    suspend fun guardarEnBiblioteca(ejercicio: EjercicioBiblioteca): Boolean
}
