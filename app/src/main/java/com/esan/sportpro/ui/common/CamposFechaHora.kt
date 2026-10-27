package com.esan.sportpro.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.esan.sportpro.core.fecha.FechasSportPro
import java.time.LocalDate
import java.time.LocalTime

/**
 * Campo de fecha con calendario. Guarda y devuelve la fecha en ISO (`AAAA-MM-DD`, ver
 * [FechasSportPro]) y la muestra como `dd/MM/yyyy`. El calendario solo deja elegir fechas entre
 * [fechaMinima] y [fechaMaxima], así que el usuario no puede escribir una fecha fuera de rango.
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
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = (seleccionada ?: fechaMaxima?.takeIf { it.isBefore(FechasSportPro.hoy()) })
                ?.let { FechasSportPro.utcMillisDe(it) },
            selectableDates = rango,
        )
        DatePickerDialog(
            onDismissRequest = { mostrar = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        estado.selectedDateMillis?.let { onCambio(FechasSportPro.aIso(FechasSportPro.fechaDesdeUtcMillis(it))) }
                        mostrar = false
                    },
                    enabled = estado.selectedDateMillis != null,
                ) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { mostrar = false }) { Text("Cancelar") } },
        ) {
            DatePicker(state = estado)
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
        val inicial = seleccionada ?: LocalTime.of(16, 0)
        val estado = rememberTimePickerState(initialHour = inicial.hour, initialMinute = inicial.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { mostrar = false },
            title = { Text(etiqueta) },
            text = { TimePicker(state = estado) },
            confirmButton = {
                TextButton(onClick = {
                    onCambio(FechasSportPro.horaATexto(LocalTime.of(estado.hour, estado.minute)))
                    mostrar = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { mostrar = false }) { Text("Cancelar") } },
        )
    }
}

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
