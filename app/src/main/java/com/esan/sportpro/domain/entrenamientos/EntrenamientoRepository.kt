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

    // ---- Asistencia (US-011) --------------------------------------------------------------

    /**
     * Una sesión con el indicador de cambios locales aún no confirmados por el servidor, para
     * mostrar "Sin conexión…" / "Sincronizado" (criterios 6 y 7).
     */
    fun observarSesion(entrenamientoId: String): Flow<SesionObservada>

    /** Cambios de asistencia de la sesión, sin ordenar (ver [ReglasAsistencia.ordenarHistorial]). */
    fun observarHistorialAsistencia(entrenamientoId: String): Flow<List<RegistroHistorialAsistencia>>

    /**
     * Guarda al instante una o varias marcas (también sin conexión: quedan en la caché local y se
     * envían solas al volver la red). Actualiza `asistencia` y `asistenciaDetalle` del documento y
     * agrega un registro por marca a `historial_asistencia`.
     * @return `true` si el servidor confirmó, `false` si quedó pendiente.
     */
    suspend fun marcarAsistencia(entrenamientoId: String, marcas: List<MarcaJugador>): Boolean

    /**
     * Cierra la sesión: estado `Realizada`, porcentaje del equipo y un registro por jugador en
     * `asistencias_entrenamiento` (campos `equipoId`, `jugadorId`, `presente`, `fecha`), que es la
     * colección que consume el módulo de Estadísticas.
     */
    suspend fun cerrarSesion(entrenamiento: Entrenamiento, plantelIds: List<String>, porcentaje: Double): Boolean

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

/** Marca de un jugador lista para guardar. */
data class MarcaJugador(val jugadorId: String, val jugadorNombre: String, val marca: MarcaAsistencia)

/**
 * @property entrenamiento `null` si el documento no existe (o se borró).
 * @property cambiosPendientes hay escrituras locales que el servidor todavía no confirmó.
 * @property desdeCache los datos vienen de la caché local (sin confirmación del servidor).
 */
data class SesionObservada(
    val entrenamiento: Entrenamiento?,
    val cambiosPendientes: Boolean,
    val desdeCache: Boolean,
)
