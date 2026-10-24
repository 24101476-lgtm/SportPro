package com.esan.sportpro.domain.entrenamientos

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude

/**
 * Ejercicio dentro de una sesión de entrenamiento. Es una copia: cambiar o desactivar el ejercicio
 * en la biblioteca no altera las sesiones ya planificadas. [ejercicioId] apunta al ejercicio de la
 * biblioteca del que salió (vacío si se escribió solo para esta sesión).
 */
data class Ejercicio(
    val nombre: String = "",
    val descripcion: String = "",
    val duracionMin: Int = 0,
    val ejercicioId: String = "",
)

/**
 * Sesión de entrenamiento planificada para un equipo (US-009 a US-013). Colección Firestore:
 * `entrenamientos`. [fecha] (`2026-10-15`) y [hora] (`16:30`) se guardan como texto.
 *
 * [asistencia] relaciona `jugadorId` con el nombre de un [EstadoAsistencia]. Se guarda dentro del
 * mismo documento para sincronizar y calcular el historial con un solo listener; solo contiene
 * ids y estados, nunca datos de contacto ni físicos. Un jugador que no figura está "Sin marcar".
 * [asistenciaDetalle] agrega a cada marca los minutos de retraso, el motivo y quién la hizo
 * (US-011); cada cambio queda además en la subcolección `historial_asistencia`.
 */
data class Entrenamiento(
    @DocumentId
    val id: String = "",
    val academiaId: String = "",
    val equipoId: String = "",
    val equipoNombre: String = "",
    val fecha: String = "",
    val hora: String = "",
    val lugar: String = "",
    val objetivo: String = "",
    val ejercicios: List<Ejercicio> = emptyList(),
    val asistencia: Map<String, String> = emptyMap(),
    val asistenciaDetalle: Map<String, MarcaAsistencia> = emptyMap(),
    /** Se mantiene por compatibilidad: `true` una vez cerrada la sesión (lo leen otros módulos). */
    val asistenciaRegistrada: Boolean = false,
    /** Nombre de [EstadoSesion]; vacío en sesiones creadas antes de US-011 (= Programada). */
    val estado: String = "",
    /** Porcentaje de asistencia del equipo calculado al cerrar la sesión (US-011, criterio 9). */
    val porcentajeAsistencia: Double? = null,
    val creadoPor: String = "",
) {
    /** Duración total de la sesión: suma de la duración de sus ejercicios. */
    @get:Exclude
    val duracionTotalMin: Int get() = ejercicios.sumOf { it.duracionMin }

    /** Estado de la sesión; las sesiones antiguas con asistencia registrada cuentan como realizadas. */
    @get:Exclude
    val estadoSesion: EstadoSesion
        get() = EstadoSesion.desde(estado)
            ?: if (asistenciaRegistrada) EstadoSesion.REALIZADA else EstadoSesion.PROGRAMADA

    @get:Exclude
    val realizada: Boolean get() = estadoSesion == EstadoSesion.REALIZADA

    /** Clave para ordenar cronológicamente: `2026-10-15 16:30`. */
    @get:Exclude
    val clave: String get() = "$fecha $hora"
}

/** Estado de asistencia de un jugador a un entrenamiento (US-009 a US-013). */
enum class EstadoAsistencia(val etiqueta: String) {
    PRESENTE("Presente"),
    TARDE("Tarde"),
    JUSTIFICADO("Falta justificada"),
    AUSENTE("Falta injustificada"),
    ;

    /** Presente y tarde cuentan como participación; justificado y ausente no. */
    val participo: Boolean get() = this == PRESENTE || this == TARDE

    companion object {
        fun desde(nombre: String?): EstadoAsistencia? = entries.firstOrNull { it.name == nombre }
    }
}
