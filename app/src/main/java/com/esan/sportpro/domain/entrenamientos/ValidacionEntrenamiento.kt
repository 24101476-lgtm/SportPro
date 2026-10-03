package com.esan.sportpro.domain.entrenamientos

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeParseException

enum class CampoEntrenamiento { EQUIPO, FECHA, HORA, LUGAR, OBJETIVO, EJERCICIOS }

enum class CampoEjercicio { NOMBRE, DURACION, DESCRIPCION }

/** Valores del formulario de entrenamiento tal como los escribe el usuario. */
data class DatosEntrenamiento(
    val equipoId: String = "",
    val fecha: String = "",
    val hora: String = "",
    val lugar: String = "",
    val objetivo: String = "",
    val ejercicios: List<Ejercicio> = emptyList(),
)

/** Reglas de validación de sesiones y ejercicios. Sin dependencias de Android ni Firebase. */
object ValidacionEntrenamiento {
    const val OBJETIVO_MIN = 5
    const val OBJETIVO_MAX = 200
    const val DURACION_EJERCICIO_MAX = 180
    const val DURACION_SESION_MAX = 240

    /**
     * @param permitirPasado `true` al editar una sesión existente (puede ser de una fecha ya vencida);
     * al crear una sesión nueva la fecha no puede ser anterior a [hoy].
     */
    fun validar(
        datos: DatosEntrenamiento,
        hoy: LocalDate = LocalDate.now(),
        permitirPasado: Boolean = false,
    ): Map<CampoEntrenamiento, String> {
        val errores = mutableMapOf<CampoEntrenamiento, String>()

        if (datos.equipoId.isBlank()) errores[CampoEntrenamiento.EQUIPO] = "Selecciona un equipo."

        val fecha = parsearFecha(datos.fecha)
        when {
            fecha == null -> errores[CampoEntrenamiento.FECHA] = "Usa el formato AAAA-MM-DD (ej. 2026-10-15)."
            !permitirPasado && fecha.isBefore(hoy) ->
                errores[CampoEntrenamiento.FECHA] = "La fecha no puede ser anterior a hoy."
        }

        if (parsearHora(datos.hora) == null) {
            errores[CampoEntrenamiento.HORA] = "Usa el formato HH:mm de 24 horas (ej. 16:30)."
        }

        if (datos.lugar.trim().length > 80) errores[CampoEntrenamiento.LUGAR] = "El lugar no puede superar 80 caracteres."

        val objetivo = datos.objetivo.trim()
        when {
            objetivo.length < OBJETIVO_MIN ->
                errores[CampoEntrenamiento.OBJETIVO] = "El objetivo debe tener al menos $OBJETIVO_MIN caracteres."
            objetivo.length > OBJETIVO_MAX ->
                errores[CampoEntrenamiento.OBJETIVO] = "El objetivo no puede superar $OBJETIVO_MAX caracteres."
        }

        val total = datos.ejercicios.sumOf { it.duracionMin }
        when {
            datos.ejercicios.isEmpty() ->
                errores[CampoEntrenamiento.EJERCICIOS] = "Agrega al menos un ejercicio."
            total > DURACION_SESION_MAX ->
                errores[CampoEntrenamiento.EJERCICIOS] =
                    "La sesión dura $total min; el máximo es $DURACION_SESION_MAX min."
        }
        return errores
    }

    /** Valida un ejercicio escrito en el diálogo; la duración llega como texto. */
    fun validarEjercicio(
        nombre: String,
        descripcion: String,
        duracionTexto: String,
    ): Map<CampoEjercicio, String> {
        val errores = mutableMapOf<CampoEjercicio, String>()
        if (nombre.trim().length !in 3..60) errores[CampoEjercicio.NOMBRE] = "El nombre debe tener entre 3 y 60 caracteres."
        val duracion = duracionTexto.trim().toIntOrNull()
        if (duracion == null || duracion !in 1..DURACION_EJERCICIO_MAX) {
            errores[CampoEjercicio.DURACION] = "La duración debe estar entre 1 y $DURACION_EJERCICIO_MAX minutos."
        }
        if (descripcion.trim().length > 300) {
            errores[CampoEjercicio.DESCRIPCION] = "La descripción no puede superar 300 caracteres."
        }
        return errores
    }

    fun parsearFecha(texto: String): LocalDate? = try {
        LocalDate.parse(texto.trim())
    } catch (e: DateTimeParseException) {
        null
    }

    fun parsearHora(texto: String): LocalTime? = try {
        LocalTime.parse(texto.trim())
    } catch (e: DateTimeParseException) {
        null
    }
}
