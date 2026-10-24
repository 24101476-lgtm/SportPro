package com.esan.sportpro.domain.entrenamientos

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.Date
import java.util.Locale

/** Estado de una sesión de entrenamiento (US-010/US-011). */
enum class EstadoSesion(val etiqueta: String) {
    PROGRAMADA("Programada"),
    REALIZADA("Realizada"),
    CANCELADA("Cancelada"),
    ;

    companion object {
        fun desde(nombre: String?): EstadoSesion? = entries.firstOrNull { it.name == nombre }
    }
}

/**
 * Marca vigente de un jugador en una sesión (`entrenamientos/{id}.asistenciaDetalle.{jugadorId}`).
 * [marcadoEn] lo pone el servidor: si dos usuarios marcan al mismo jugador, la última escritura que
 * llega al servidor es la que queda (y tiene la marca de tiempo más reciente).
 */
data class MarcaAsistencia(
    val estado: String = "",
    val minutosRetraso: Int? = null,
    val motivo: String = "",
    val marcadoPor: String = "",
    val marcadoPorNombre: String = "",
    @ServerTimestamp
    val marcadoEn: Date? = null,
) {
    @get:Exclude
    val estadoAsistencia: EstadoAsistencia? get() = EstadoAsistencia.desde(estado)
}

/**
 * Cada cambio de asistencia queda en `entrenamientos/{id}/historial_asistencia` (criterio 8):
 * nunca se edita ni se borra, así ambos registros se conservan aunque haya conflicto.
 */
data class RegistroHistorialAsistencia(
    @DocumentId
    val id: String = "",
    val jugadorId: String = "",
    val jugadorNombre: String = "",
    val estado: String = "",
    val minutosRetraso: Int? = null,
    val motivo: String = "",
    val marcadoPor: String = "",
    val marcadoPorNombre: String = "",
    @ServerTimestamp
    val marcadoEn: Date? = null,
    /** Hora del dispositivo, para ordenar los cambios que aún no llegan al servidor. */
    val marcadoEnDispositivo: Long = 0,
) {
    @get:Exclude
    val estadoAsistencia: EstadoAsistencia? get() = EstadoAsistencia.desde(estado)
}

enum class CampoMarca { MINUTOS, MOTIVO }

/** Reglas del registro de asistencia (US-011). Sin dependencias de Android ni Firebase. */
object ReglasAsistencia {
    const val MINUTOS_MIN = 1
    const val MINUTOS_MAX = 120
    const val MOTIVO_MIN = 5
    const val MOTIVO_MAX = 200

    /** Los estados Tarde y Falta justificada piden un dato adicional (criterios 3 y 4). */
    fun requiereDetalle(estado: EstadoAsistencia): Boolean =
        estado == EstadoAsistencia.TARDE || estado == EstadoAsistencia.JUSTIFICADO

    fun validarMarca(estado: EstadoAsistencia, minutosTexto: String, motivo: String): Map<CampoMarca, String> {
        val errores = mutableMapOf<CampoMarca, String>()
        when (estado) {
            EstadoAsistencia.TARDE -> {
                val minutos = minutosTexto.trim().toIntOrNull()
                if (minutos == null || minutos !in MINUTOS_MIN..MINUTOS_MAX) {
                    errores[CampoMarca.MINUTOS] = "Ingresa entre $MINUTOS_MIN y $MINUTOS_MAX minutos de retraso."
                }
            }
            EstadoAsistencia.JUSTIFICADO -> {
                if (motivo.trim().length !in MOTIVO_MIN..MOTIVO_MAX) {
                    errores[CampoMarca.MOTIVO] = "El motivo debe tener entre $MOTIVO_MIN y $MOTIVO_MAX caracteres."
                }
            }
            else -> Unit
        }
        return errores
    }

    /** Construye la marca que se guarda; descarta minutos y motivo que no corresponden al estado. */
    fun crearMarca(
        estado: EstadoAsistencia,
        minutosTexto: String = "",
        motivo: String = "",
        uid: String,
        nombre: String,
    ): MarcaAsistencia = MarcaAsistencia(
        estado = estado.name,
        minutosRetraso = if (estado == EstadoAsistencia.TARDE) minutosTexto.trim().toIntOrNull() else null,
        motivo = if (estado == EstadoAsistencia.JUSTIFICADO) motivo.trim() else "",
        marcadoPor = uid,
        marcadoPorNombre = nombre,
    )

    /**
     * La asistencia se toma desde el día de la sesión en adelante y mientras no esté cerrada
     * (Realizada) ni cancelada.
     */
    fun puedeTomarAsistencia(entrenamiento: Entrenamiento, hoy: LocalDate = LocalDate.now()): Boolean {
        if (entrenamiento.estadoSesion != EstadoSesion.PROGRAMADA) return false
        val fecha = ValidacionEntrenamiento.parsearFecha(entrenamiento.fecha) ?: return false
        return !fecha.isAfter(hoy)
    }

    /** Jugadores del plantel que todavía no tienen estado (criterio 2). */
    fun sinMarcar(plantelIds: Collection<String>, asistencia: Map<String, String>): List<String> =
        plantelIds.filter { EstadoAsistencia.desde(asistencia[it]) == null }

    /** "Quedan 3 jugadores sin marcar" / "Queda 1 jugador sin marcar". */
    fun mensajeSinMarcar(cantidad: Int): String =
        if (cantidad == 1) "Queda 1 jugador sin marcar" else "Quedan $cantidad jugadores sin marcar"

    /**
     * Porcentaje de asistencia del equipo en la sesión (criterio 9): presentes más tardanzas sobre
     * el plantel, con un decimal. Mismo criterio que el historial del jugador (US-012).
     */
    fun porcentajeEquipo(plantelIds: Collection<String>, asistencia: Map<String, String>): Double {
        if (plantelIds.isEmpty()) return 0.0
        val participaron = plantelIds.count { EstadoAsistencia.desde(asistencia[it])?.participo == true }
        return BigDecimal(participaron * 100.0 / plantelIds.size).setScale(1, RoundingMode.HALF_UP).toDouble()
    }

    /** "85.7 %": un decimal con punto (formato es-PE), igual en todos los dispositivos. */
    fun formatearPorcentaje(valor: Double): String = String.format(Locale.US, "%.1f %%", valor)

    /** Historial ordenado del cambio más reciente al más antiguo; los pendientes de sincronizar van primero. */
    fun ordenarHistorial(registros: List<RegistroHistorialAsistencia>): List<RegistroHistorialAsistencia> =
        registros.sortedWith(
            compareByDescending<RegistroHistorialAsistencia> { it.marcadoEn == null }
                .thenByDescending { it.marcadoEn?.time ?: it.marcadoEnDispositivo }
                .thenByDescending { it.marcadoEnDispositivo },
        )

    /** Jugadores con más de una marca de distintos usuarios: la vista resalta ese conflicto resuelto. */
    fun jugadoresConConflicto(registros: List<RegistroHistorialAsistencia>): Set<String> =
        registros.groupBy { it.jugadorId }
            .filterValues { marcas -> marcas.map { it.marcadoPor }.distinct().size > 1 }
            .keys
}
