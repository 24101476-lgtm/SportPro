package com.esan.sportpro.domain.entrenamientos

import java.time.LocalDate
import java.time.LocalDateTime

/** Reglas de planificación de sesiones (US-010). Sin dependencias de Android ni Firebase. */
object ReglasPlanificacion {
    const val MOTIVO_CANCELACION_MIN = 10
    const val MOTIVO_CANCELACION_MAX = 200

    /**
     * Criterio 4: minutos que los ejercicios superan la duración total de la sesión; 0 si no la
     * superan o si la duración aún no es válida.
     */
    fun excesoMinutos(datos: DatosEntrenamiento): Int {
        val duracion = datos.duracion.trim().toIntOrNull() ?: return 0
        return (datos.ejercicios.sumOf { it.duracionMin } - duracion).coerceAtLeast(0)
    }

    /** Advertencia que no bloquea el guardado, o `null` si no hay exceso. */
    fun avisoExceso(datos: DatosEntrenamiento): String? =
        excesoMinutos(datos).takeIf { it > 0 }?.let { "Los ejercicios superan la duración de la sesión en $it minutos" }

    /**
     * Criterio 6: otra sesión del mismo equipo, no cancelada, cuyo horario se cruza con
     * [fecha] [hora] durante [duracionMin]. Dos sesiones que solo se tocan (una termina 18:00
     * y la otra empieza 18:00) no se solapan.
     */
    fun sesionSolapada(
        equipoId: String,
        fecha: String,
        hora: String,
        duracionMin: Int,
        sesiones: List<Entrenamiento>,
        idEditando: String? = null,
    ): Entrenamiento? {
        val inicio = inicio(fecha, hora) ?: return null
        val fin = inicio.plusMinutes(duracionMin.toLong().coerceAtLeast(1))
        return sesiones
            .filter { it.id != idEditando && it.equipoId == equipoId && it.estadoSesion != EstadoSesion.CANCELADA }
            .firstOrNull { otra ->
                val inicioOtra = inicio(otra.fecha, otra.hora) ?: return@firstOrNull false
                val finOtra = inicioOtra.plusMinutes(otra.duracionSesionMin.toLong().coerceAtLeast(1))
                inicio < finOtra && inicioOtra < fin
            }
    }

    /** Criterio 9: solo se cancela una sesión programada y con un motivo de 10 a 200 caracteres. */
    fun validarCancelacion(entrenamiento: Entrenamiento, motivo: String): String? = when {
        entrenamiento.estadoSesion != EstadoSesion.PROGRAMADA -> "Solo se puede cancelar una sesión programada."
        motivo.trim().length !in MOTIVO_CANCELACION_MIN..MOTIVO_CANCELACION_MAX ->
            "El motivo debe tener entre $MOTIVO_CANCELACION_MIN y $MOTIVO_CANCELACION_MAX caracteres."
        else -> null
    }

    /**
     * Criterio 8: copia de la sesión hacia otra fecha con sus ejercicios, objetivo y duraciones.
     * La copia nace Programada y sin asistencia.
     */
    fun duplicar(original: Entrenamiento, nuevaFecha: String, creadoPor: String): Entrenamiento = Entrenamiento(
        academiaId = original.academiaId,
        equipoId = original.equipoId,
        equipoNombre = original.equipoNombre,
        fecha = nuevaFecha,
        hora = original.hora,
        lugar = original.lugar,
        objetivo = original.objetivo,
        ejercicios = original.ejercicios,
        duracionPlanificadaMin = original.duracionSesionMin,
        creadoPor = creadoPor,
    )

    /** La fecha de una duplicación sigue la regla del criterio 2 (hoy o posterior). */
    fun validarFechaDuplicado(fecha: String, hoy: LocalDate = LocalDate.now()): String? {
        val dia = ValidacionEntrenamiento.parsearFecha(fecha) ?: return "Elige la fecha de la nueva sesión."
        return if (dia.isBefore(hoy)) ValidacionEntrenamiento.MSG_FECHA_PASADA else null
    }

    /** Mueve un ejercicio de la sesión (reordenar por arrastre, criterio 3). */
    fun mover(ejercicios: List<Ejercicio>, desde: Int, hasta: Int): List<Ejercicio> {
        if (desde !in ejercicios.indices || hasta !in ejercicios.indices || desde == hasta) return ejercicios
        return ejercicios.toMutableList().apply { add(hasta, removeAt(desde)) }
    }

    /** Ajusta la duración de un ejercicio solo en esta sesión; la biblioteca no cambia (criterio 3). */
    fun ajustarDuracion(ejercicios: List<Ejercicio>, indice: Int, minutos: Int): List<Ejercicio> =
        ejercicios.mapIndexed { i, ejercicio -> if (i == indice) ejercicio.copy(duracionMin = minutos) else ejercicio }

    private fun inicio(fecha: String, hora: String): LocalDateTime? {
        val dia = ValidacionEntrenamiento.parsearFecha(fecha) ?: return null
        val inicio = ValidacionEntrenamiento.parsearHora(hora) ?: return null
        return dia.atTime(inicio)
    }
}
