package com.esan.sportpro.ui.common

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerColors
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.esan.sportpro.core.fecha.FechasSportPro
import com.esan.sportpro.core.theme.FuenteNumeros
import com.esan.sportpro.core.theme.PaletaSportPro
import com.esan.sportpro.core.theme.SportProTema
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Campo de fecha con calendario. Guarda y devuelve la fecha en ISO (`AAAA-MM-DD`, ver
 * [FechasSportPro]) y la muestra como `dd/MM/yyyy`. El calendario solo deja elegir fechas entre
 * [fechaMinima] y [fechaMaxima], así que el usuario no puede escribir una fecha fuera de rango.
 *
 * El calendario va en español (meses y días), con la fecha elegida en grande y atajos
 * ("Hoy", "Mañana", "En 1 semana") cuando caen dentro del rango permitido.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoFecha(
    valorIso: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    ayuda: String? = null,
    fechaMinima: LocalDate? = null,
    fechaMaxima: LocalDate? = null,
    habilitado: Boolean = true,
) {
    var mostrar by rememberSaveable { mutableStateOf(false) }
    val seleccionada = FechasSportPro.desdeIso(valorIso)

    CampoSoloLectura(
        texto = seleccionada?.let { FechasSportPro.fecha(it) }.orEmpty(),
        etiqueta = etiqueta,
        error = error,
        ayuda = ayuda ?: "Toca para elegir en el calendario",
        icono = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
        habilitado = habilitado,
        onClick = { mostrar = true },
        modifier = modifier,
    )

    if (mostrar) {
        EnEspanol {
            val minimo = fechaMinima?.let { FechasSportPro.utcMillisDe(it) }
            val maximo = fechaMaxima?.let { FechasSportPro.utcMillisDe(it) }
            val rango = remember(fechaMinima, fechaMaxima) {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                        (minimo == null || utcTimeMillis >= minimo) && (maximo == null || utcTimeMillis <= maximo)

                    override fun isSelectableYear(year: Int): Boolean =
                        (fechaMinima == null || year >= fechaMinima.year) && (fechaMaxima == null || year <= fechaMaxima.year)
                }
            }
            val hoy = FechasSportPro.hoy()
            fun enRango(fecha: LocalDate) =
                (fechaMinima == null || !fecha.isBefore(fechaMinima)) && (fechaMaxima == null || !fecha.isAfter(fechaMaxima))

            // Al tocar un atajo se vuelve a crear el estado con esa fecha ya elegida.
            var inicial by remember { mutableStateOf(seleccionada ?: fechaMaxima?.takeIf { it.isBefore(hoy) }) }
            val atajos = listOf("Hoy" to hoy, "Mañana" to hoy.plusDays(1), "En 1 semana" to hoy.plusWeeks(1))
                .filter { (_, fecha) -> enRango(fecha) }
            val colores = coloresCalendario()

            key(inicial) {
                val estado = rememberDatePickerState(
                    initialSelectedDateMillis = inicial?.let { FechasSportPro.utcMillisDe(it) },
                    selectableDates = rango,
                )
                val elegida = estado.selectedDateMillis?.let { FechasSportPro.fechaDesdeUtcMillis(it) }
                DatePickerDialog(
                    onDismissRequest = { mostrar = false },
                    shape = RoundedCornerShape(32.dp),
                    colors = colores,
                    confirmButton = {
                        BotonAceptar(
                            habilitado = elegida != null,
                            onClick = {
                                elegida?.let { onCambio(FechasSportPro.aIso(it)) }
                                mostrar = false
                            },
                        )
                    },
                    dismissButton = { TextButton(onClick = { mostrar = false }) { Text("Cancelar") } },
                ) {
                    DatePicker(
                        state = estado,
                        colors = colores,
                        showModeToggle = false,
                        title = {
                            Text(
                                etiqueta,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 20.dp),
                            )
                        },
                        headline = {
                            Column(modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp)) {
                                FechaEnGrande(elegida)
                                if (atajos.isNotEmpty()) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(top = 12.dp),
                                    ) {
                                        atajos.forEach { (texto, fecha) ->
                                            Atajo(texto = texto, activo = elegida == fecha, onClick = { inicial = fecha })
                                        }
                                    }
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

/** "28" en grande y, al lado, "Sábado" / "noviembre 2026". */
@Composable
private fun FechaEnGrande(fecha: LocalDate?) {
    if (fecha == null) {
        Text("Elige un día", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            fecha.dayOfMonth.toString(),
            style = TextStyle(fontFamily = FuenteNumeros, fontSize = 52.sp, lineHeight = 52.sp),
            color = SportProTema.colores.textoAcento,
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                fecha.format(FORMATO_DIA_SEMANA).replaceFirstChar { it.titlecase(FechasSportPro.LOCALE) },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                fecha.format(FORMATO_MES_ANIO),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Campo de hora con reloj de 24 horas. Guarda y devuelve `HH:mm`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoHora(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    habilitado: Boolean = true,
) {
    var mostrar by rememberSaveable { mutableStateOf(false) }
    val seleccionada = FechasSportPro.horaDesdeTexto(valor)

    CampoSoloLectura(
        texto = seleccionada?.let { FechasSportPro.horaATexto(it) }.orEmpty(),
        etiqueta = etiqueta,
        error = error,
        ayuda = "Formato de 24 horas",
        icono = { Icon(Icons.Default.Schedule, contentDescription = null) },
        habilitado = habilitado,
        onClick = { mostrar = true },
        modifier = modifier,
    )

    if (mostrar) {
        var inicial by remember { mutableStateOf(seleccionada ?: LocalTime.of(16, 0)) }
        var conTeclado by rememberSaveable { mutableStateOf(false) }
        val colores = coloresReloj()

        Dialog(
            onDismissRequest = { mostrar = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                modifier = Modifier.padding(horizontal = 20.dp).widthIn(max = 400.dp),
            ) {
                key(inicial) {
                    val estado = rememberTimePickerState(initialHour = inicial.hour, initialMinute = inicial.minute, is24Hour = true)
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Cabecera: etiqueta del campo y cambio entre reloj y teclado.
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    etiqueta,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    if (conTeclado) "Escribe la hora" else "Toca la hora y luego los minutos",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { conTeclado = !conTeclado }) {
                                Icon(
                                    if (conTeclado) Icons.Default.Schedule else Icons.Default.Keyboard,
                                    contentDescription = if (conTeclado) "Usar el reloj" else "Escribir la hora",
                                )
                            }
                        }

                        // Horas frecuentes de entrenamiento: un toque y listo.
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 12.dp, bottom = 16.dp),
                        ) {
                            HORAS_FRECUENTES.forEach { hora ->
                                Atajo(
                                    texto = FechasSportPro.horaATexto(hora),
                                    activo = estado.hour == hora.hour && estado.minute == hora.minute,
                                    onClick = { inicial = hora },
                                )
                            }
                        }

                        if (conTeclado) {
                            TimeInput(state = estado, colors = colores)
                        } else {
                            TimePicker(state = estado, colors = colores)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(onClick = { mostrar = false }) { Text("Cancelar") }
                            Spacer(Modifier.padding(start = 8.dp))
                            BotonAceptar(
                                habilitado = true,
                                onClick = {
                                    onCambio(FechasSportPro.horaATexto(LocalTime.of(estado.hour, estado.minute)))
                                    mostrar = false
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Píldora pequeña para los atajos ("Hoy", "18:00"): lima cuando es la opción elegida. */
@Composable
private fun Atajo(texto: String, activo: Boolean, onClick: () -> Unit) {
    val colores = SportProTema.colores
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (activo) colores.acento else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (activo) colores.sobreAcento else MaterialTheme.colorScheme.onSurface,
    ) {
        Text(
            texto,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (activo) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun BotonAceptar(habilitado: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = habilitado,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = PaletaSportPro.Musgo,
            contentColor = Color.White,
        ),
    ) { Text("Aceptar", fontWeight = FontWeight.Bold) }
}

/** Día elegido en lima con número oscuro; hoy con borde musgo; los días fuera de rango bien apagados. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun coloresCalendario(): DatePickerColors {
    val tema = SportProTema.colores
    val superficie = MaterialTheme.colorScheme.surface
    val texto = MaterialTheme.colorScheme.onSurface
    return DatePickerDefaults.colors(
        containerColor = superficie,
        titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        headlineContentColor = texto,
        weekdayContentColor = tema.textoAcento,
        subheadContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        navigationContentColor = texto,
        yearContentColor = texto,
        currentYearContentColor = tema.textoAcento,
        selectedYearContentColor = tema.sobreAcento,
        selectedYearContainerColor = tema.acento,
        dayContentColor = texto,
        disabledDayContentColor = texto.copy(alpha = 0.28f),
        selectedDayContentColor = tema.sobreAcento,
        selectedDayContainerColor = tema.acento,
        todayContentColor = tema.textoAcento,
        todayDateBorderColor = tema.textoAcento,
        dividerColor = Color.Transparent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun coloresReloj(): TimePickerColors {
    val tema = SportProTema.colores
    return TimePickerDefaults.colors(
        clockDialColor = MaterialTheme.colorScheme.surfaceVariant,
        clockDialSelectedContentColor = PaletaSportPro.Carbon,
        clockDialUnselectedContentColor = MaterialTheme.colorScheme.onSurface,
        selectorColor = tema.acento,
        containerColor = MaterialTheme.colorScheme.surface,
        timeSelectorSelectedContainerColor = tema.acento,
        timeSelectorSelectedContentColor = tema.sobreAcento,
        timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurface,
    )
}

/**
 * Los nombres de meses y días del calendario salen del idioma de la configuración. Así el
 * calendario se ve en español aunque el teléfono o el emulador esté en inglés.
 */
@Composable
private fun EnEspanol(contenido: @Composable () -> Unit) {
    val actual = LocalConfiguration.current
    val enEspanol = remember(actual) { Configuration(actual).apply { setLocale(FechasSportPro.LOCALE) } }
    CompositionLocalProvider(LocalConfiguration provides enEspanol, content = contenido)
}

private val FORMATO_DIA_SEMANA = DateTimeFormatter.ofPattern("EEEE", FechasSportPro.LOCALE)
private val FORMATO_MES_ANIO = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", FechasSportPro.LOCALE)
private val HORAS_FRECUENTES = listOf(LocalTime.of(8, 0), LocalTime.of(16, 0), LocalTime.of(17, 0), LocalTime.of(18, 0))

/** Campo de texto de solo lectura que abre un selector al tocarlo. */
@Composable
private fun CampoSoloLectura(
    texto: String,
    etiqueta: String,
    error: String?,
    ayuda: String?,
    icono: @Composable () -> Unit,
    habilitado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = texto,
            onValueChange = {},
            readOnly = true,
            enabled = habilitado,
            label = { Text(etiqueta) },
            trailingIcon = icono,
            isError = error != null,
            supportingText = {
                val mensaje = error ?: ayuda
                if (mensaje != null) Text(mensaje)
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        // Capa transparente encima: el campo de solo lectura no recibe toques por sí mismo.
        if (habilitado) {
            Spacer(modifier = Modifier.matchParentSize().clickable(onClick = onClick))
        }
    }
}
