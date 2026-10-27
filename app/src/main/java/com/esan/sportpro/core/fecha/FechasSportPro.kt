package com.esan.sportpro.core.fecha

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Criterio único de fechas y horas de SportPro (corrección de la semana 7: "fechas y
 * consistencia de datos"). Toda la app guarda, valida y muestra fechas a través de este objeto.
 *
 * Cómo se GUARDA cada tipo de dato (según lo que representa):
 * - **Fecha de calendario** sin hora (nacimiento, día de entrenamiento): texto ISO `AAAA-MM-DD`
 *   ([aIso]/[desdeIso]). No depende de la zona horaria: un cumpleaños no cambia de día.
 * - **Hora del día** (inicio de entrenamiento o partido): texto `HH:mm` en 24 horas ([horaATexto]).
 * - **Instante** (cuándo ocurrió algo: evento del partido, registro, aprobación): marca de tiempo
 *   del servidor (`Timestamp`/`Date`) o milisegundos desde 1970 en UTC. Nunca texto.
 * - El **minuto de juego** de un evento es un número entero (`Int`), no un texto ni una hora.
 *
 * Cómo se MUESTRA: siempre en la zona de Perú ([ZONA]) con los formatos `dd/MM/yyyy`, `HH:mm`
 * y `dd/MM/yyyy HH:mm`. Ninguna pantalla debe crear su propio `SimpleDateFormat`.
 */
object FechasSportPro {
    val ZONA: ZoneId = ZoneId.of("America/Lima")
    val LOCALE: Locale = Locale.forLanguageTag("es-PE")

    private val FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy", LOCALE)
    private val FORMATO_FECHA_CORTA = DateTimeFormatter.ofPattern("d MMM yyyy", LOCALE)
    private val FORMATO_DIA_MES = DateTimeFormatter.ofPattern("dd/MM", LOCALE)
    private val FORMATO_FECHA_LARGA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", LOCALE)
    private val FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm", LOCALE)
    private val FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", LOCALE)
    private val FORMATO_DIA_MES_HORA = DateTimeFormatter.ofPattern("d MMM, HH:mm", LOCALE)

    /** Fecha de hoy en Perú. */
    fun hoy(): LocalDate = LocalDate.now(ZONA)

    // ---- Almacenamiento ------------------------------------------------------------------

    /** Fecha de calendario a texto ISO para guardar: `2026-10-15`. */
    fun aIso(fecha: LocalDate): String = fecha.toString()

    /** Texto ISO (`2026-10-15`) a fecha; `null` si está vacío o mal formado. */
    fun desdeIso(texto: String?): LocalDate? {
        val limpio = texto?.trim().orEmpty()
        if (limpio.isEmpty()) return null
        return try {
            LocalDate.parse(limpio)
        } catch (e: DateTimeParseException) {
            null
        }
    }

    /** Hora a texto `HH:mm` para guardar. */
    fun horaATexto(hora: LocalTime): String = hora.format(FORMATO_HORA)

    /** Texto `HH:mm` a hora; `null` si está vacío o mal formado. */
    fun horaDesdeTexto(texto: String?): LocalTime? {
        val limpio = texto?.trim().orEmpty()
        if (limpio.isEmpty()) return null
        return try {
            LocalTime.parse(limpio)
        } catch (e: DateTimeParseException) {
            null
        }
    }

    /** Fecha y hora guardadas por separado a un instante en milisegundos (zona de Perú). */
    fun aMillis(fecha: LocalDate, hora: LocalTime = LocalTime.MIDNIGHT): Long =
        LocalDateTime.of(fecha, hora).atZone(ZONA).toInstant().toEpochMilli()

    /** Fecha (y hora opcional) en texto a milisegundos, para ordenar o comparar; `null` si no son válidas. */
    fun aMillis(fechaIso: String?, hora: String? = null): Long? {
        val fecha = desdeIso(fechaIso) ?: return null
        return aMillis(fecha, horaDesdeTexto(hora) ?: LocalTime.MIDNIGHT)
    }

    // ---- Selectores de Material (DatePicker trabaja en milisegundos UTC) -------------------

    fun utcMillisDe(fecha: LocalDate): Long = fecha.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun fechaDesdeUtcMillis(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

    // ---- Presentación --------------------------------------------------------------------

    /** `15/10/2026` */
    fun fecha(fecha: LocalDate): String = fecha.format(FORMATO_FECHA)

    fun fecha(millis: Long): String = fecha(Instant.ofEpochMilli(millis).atZone(ZONA).toLocalDate())

    fun fecha(date: Date): String = fecha(date.time)

    /** Texto ISO guardado a `15/10/2026`; si no es una fecha válida devuelve el texto tal cual. */
    fun fechaIso(texto: String?): String = desdeIso(texto)?.let { fecha(it) } ?: texto.orEmpty()

    /** `15 oct. 2026` */
    fun fechaCorta(fecha: LocalDate): String = fecha.format(FORMATO_FECHA_CORTA)

    fun fechaCorta(date: Date): String = fechaCorta(Instant.ofEpochMilli(date.time).atZone(ZONA).toLocalDate())

    /** `15/10` */
    fun diaMes(fecha: LocalDate): String = fecha.format(FORMATO_DIA_MES)

    /** `jueves 15 de octubre` */
    fun fechaLarga(fecha: LocalDate): String = fecha.format(FORMATO_FECHA_LARGA)

    /** `16:30` */
    fun hora(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZONA).format(FORMATO_HORA)

    fun hora(date: Date): String = hora(date.time)

    /** `15/10/2026 16:30` */
    fun fechaHora(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZONA).format(FORMATO_FECHA_HORA)

    fun fechaHora(date: Date): String = fechaHora(date.time)

    /** `15 oct., 16:30` (listas y publicaciones). */
    fun diaMesHora(date: Date): String = Instant.ofEpochMilli(date.time).atZone(ZONA).format(FORMATO_DIA_MES_HORA)

    // ---- Adaptadores para código con SimpleDateFormat ---------------------------------------
    // Devuelven un formateador nuevo (SimpleDateFormat no es seguro entre hilos) con el patrón
    // estándar y la zona de Perú. Usarlos en lugar de crear SimpleDateFormat a mano.

    /** `dd/MM/yyyy` */
    fun formatoFecha(): SimpleDateFormat = formato("dd/MM/yyyy")

    /** `d MMM yyyy` */
    fun formatoFechaCorta(): SimpleDateFormat = formato("d MMM yyyy")

    /** `dd/MM/yyyy HH:mm` */
    fun formatoFechaHora(): SimpleDateFormat = formato("dd/MM/yyyy HH:mm")

    /** `dd/MM/yyyy HH:mm:ss` (historiales y auditoría). */
    fun formatoFechaHoraSegundos(): SimpleDateFormat = formato("dd/MM/yyyy HH:mm:ss")

    /** `d MMM, HH:mm` */
    fun formatoDiaMesHora(): SimpleDateFormat = formato("d MMM, HH:mm")

    private fun formato(patron: String): SimpleDateFormat = SimpleDateFormat(patron, LOCALE).apply {
        timeZone = TimeZone.getTimeZone(ZONA.id)
        isLenient = false
    }

    // ---- Cálculos ------------------------------------------------------------------------

    /** Años cumplidos a la fecha [hoy]. */
    fun edad(nacimiento: LocalDate, hoy: LocalDate = hoy()): Int = ChronoUnit.YEARS.between(nacimiento, hoy).toInt()

    /** Días desde [hoy] hasta [fecha] (negativo si ya pasó). */
    fun diasHasta(fecha: LocalDate, hoy: LocalDate = hoy()): Long = ChronoUnit.DAYS.between(hoy, fecha)
}
