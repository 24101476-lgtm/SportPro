package com.esan.sportpro.ui.partido

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartidoProgramarScreen(
    onVolver: () -> Unit,
    onPartidoCreado: (String) -> Unit,
    viewModel: PartidoProgramarViewModel = hiltViewModel(),
) {

    val context =
        LocalContext.current

    var nombreEquipo by rememberSaveable {
        mutableStateOf("")
    }

    var nombreRival by rememberSaveable {
        mutableStateOf("")
    }

    var fechaMs by rememberSaveable {
        mutableStateOf<Long?>(null)
    }

    var hora by rememberSaveable {
        mutableStateOf("")
    }

    var lugar by rememberSaveable {
        mutableStateOf("")
    }

    var tipoPartido by rememberSaveable {
        mutableStateOf("Amistoso")
    }

    var duracion by rememberSaveable {
        mutableStateOf("45")
    }

    var maxConvocados by rememberSaveable {
        mutableStateOf("23")
    }

    var fechaLimiteMs by rememberSaveable {
        mutableStateOf<Long?>(null)
    }

    var errorFormulario by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    val formatoFecha =
        SimpleDateFormat(
            "dd/MM/yyyy",
            Locale.getDefault()
        )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Programar partido"
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onVolver
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding =
                PaddingValues(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(14.dp),
        ) {

            item {
                Text(
                    text = "US-014 · NUEVO PARTIDO",
                    style =
                        MaterialTheme
                            .typography
                            .labelMedium,
                    color =
                        MaterialTheme
                            .colorScheme
                            .primary
                )
            }

            item {
                Text(
                    text = "Datos del partido",
                    style =
                        MaterialTheme
                            .typography
                            .titleLarge,
                    fontWeight =
                        FontWeight.Bold
                )
            }

            item {
                Text(
                    text =
                        "Completa la información antes de seleccionar a los jugadores que serán convocados.",
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }

            item {
                OutlinedTextField(
                    value = nombreEquipo,
                    onValueChange = {
                        nombreEquipo = it
                        errorFormulario = null
                    },
                    label = {
                        Text("Equipo")
                    },
                    placeholder = {
                        Text(
                            "Ej. SportPro"
                        )
                    },
                    singleLine = true,
                    modifier =
                        Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = nombreRival,
                    onValueChange = {
                        nombreRival = it
                        errorFormulario = null
                    },
                    label = {
                        Text("Rival")
                    },
                    placeholder = {
                        Text(
                            "Nombre del equipo rival"
                        )
                    },
                    singleLine = true,
                    modifier =
                        Modifier.fillMaxWidth()
                )
            }

            item {

                OutlinedTextField(
                    value =
                        fechaMs?.let {
                            formatoFecha.format(
                                Date(it)
                            )
                        }.orEmpty(),

                    onValueChange = { },

                    readOnly = true,

                    label = {
                        Text(
                            "Fecha del partido"
                        )
                    },

                    placeholder = {
                        Text(
                            "Seleccionar fecha"
                        )
                    },

                    trailingIcon = {
                        IconButton(
                            onClick = {

                                val calendario =
                                    Calendar
                                        .getInstance()

                                DatePickerDialog(
                                    context,
                                    {
                                            _,
                                            year,
                                            month,
                                            day ->

                                        val seleccionado =
                                            Calendar
                                                .getInstance()
                                                .apply {
                                                    set(
                                                        Calendar.YEAR,
                                                        year
                                                    )

                                                    set(
                                                        Calendar.MONTH,
                                                        month
                                                    )

                                                    set(
                                                        Calendar.DAY_OF_MONTH,
                                                        day
                                                    )

                                                    set(
                                                        Calendar.HOUR_OF_DAY,
                                                        0
                                                    )

                                                    set(
                                                        Calendar.MINUTE,
                                                        0
                                                    )

                                                    set(
                                                        Calendar.SECOND,
                                                        0
                                                    )

                                                    set(
                                                        Calendar.MILLISECOND,
                                                        0
                                                    )
                                                }

                                        fechaMs =
                                            seleccionado
                                                .timeInMillis

                                        errorFormulario =
                                            null
                                    },
                                    calendario.get(
                                        Calendar.YEAR
                                    ),
                                    calendario.get(
                                        Calendar.MONTH
                                    ),
                                    calendario.get(
                                        Calendar.DAY_OF_MONTH
                                    )
                                ).show()
                            }
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription =
                                    "Seleccionar fecha"
                            )
                        }
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                )
            }

            item {

                OutlinedTextField(
                    value = hora,
                    onValueChange = { },
                    readOnly = true,

                    label = {
                        Text(
                            "Hora"
                        )
                    },

                    placeholder = {
                        Text(
                            "Seleccionar hora"
                        )
                    },

                    trailingIcon = {

                        IconButton(
                            onClick = {

                                val calendario =
                                    Calendar
                                        .getInstance()

                                TimePickerDialog(
                                    context,
                                    {
                                            _,
                                            horaSeleccionada,
                                            minutoSeleccionado ->

                                        hora =
                                            String.format(
                                                Locale.getDefault(),
                                                "%02d:%02d",
                                                horaSeleccionada,
                                                minutoSeleccionado
                                            )

                                        errorFormulario =
                                            null
                                    },

                                    calendario.get(
                                        Calendar.HOUR_OF_DAY
                                    ),

                                    calendario.get(
                                        Calendar.MINUTE
                                    ),

                                    true
                                ).show()
                            }
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription =
                                    "Seleccionar hora"
                            )
                        }
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = lugar,
                    onValueChange = {
                        lugar = it
                        errorFormulario = null
                    },

                    label = {
                        Text("Lugar")
                    },

                    placeholder = {
                        Text(
                            "Ej. Estadio Municipal"
                        )
                    },

                    singleLine = true,

                    modifier =
                        Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = tipoPartido,

                    onValueChange = {
                        tipoPartido = it
                        errorFormulario = null
                    },

                    label = {
                        Text(
                            "Tipo de partido"
                        )
                    },

                    placeholder = {
                        Text(
                            "Amistoso, Liga, Torneo..."
                        )
                    },

                    singleLine = true,

                    modifier =
                        Modifier.fillMaxWidth()
                )
            }

            item {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(
                            12.dp
                        )
                ) {

                    OutlinedTextField(
                        value =
                            duracion,

                        onValueChange = {
                                valor ->

                            if (
                                valor.all {
                                    it.isDigit()
                                }
                            ) {
                                duracion =
                                    valor

                                errorFormulario =
                                    null
                            }
                        },

                        label = {
                            Text(
                                "Duración"
                            )
                        },

                        suffix = {
                            Text("min")
                        },

                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType =
                                    KeyboardType.Number
                            ),

                        singleLine =
                            true,

                        modifier =
                            Modifier.weight(
                                1f
                            )
                    )

                    OutlinedTextField(
                        value =
                            maxConvocados,

                        onValueChange = {
                                valor ->

                            if (
                                valor.all {
                                    it.isDigit()
                                }
                            ) {
                                maxConvocados =
                                    valor

                                errorFormulario =
                                    null
                            }
                        },

                        label = {
                            Text(
                                "Máx. convocados"
                            )
                        },

                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType =
                                    KeyboardType.Number
                            ),

                        singleLine =
                            true,

                        modifier =
                            Modifier.weight(
                                1f
                            )
                    )
                }
            }

            item {

                OutlinedTextField(
                    value =
                        fechaLimiteMs?.let {
                            formatoFecha.format(
                                Date(it)
                            )
                        }.orEmpty(),

                    onValueChange = { },

                    readOnly = true,

                    label = {
                        Text(
                            "Fecha límite de respuesta"
                        )
                    },

                    placeholder = {
                        Text(
                            "Seleccionar fecha"
                        )
                    },

                    trailingIcon = {

                        IconButton(
                            onClick = {

                                val calendario =
                                    Calendar
                                        .getInstance()

                                DatePickerDialog(
                                    context,
                                    {
                                            _,
                                            year,
                                            month,
                                            day ->

                                        val seleccionado =
                                            Calendar
                                                .getInstance()
                                                .apply {
                                                    set(
                                                        Calendar.YEAR,
                                                        year
                                                    )

                                                    set(
                                                        Calendar.MONTH,
                                                        month
                                                    )

                                                    set(
                                                        Calendar.DAY_OF_MONTH,
                                                        day
                                                    )

                                                    set(
                                                        Calendar.HOUR_OF_DAY,
                                                        0
                                                    )

                                                    set(
                                                        Calendar.MINUTE,
                                                        0
                                                    )

                                                    set(
                                                        Calendar.SECOND,
                                                        0
                                                    )

                                                    set(
                                                        Calendar.MILLISECOND,
                                                        0
                                                    )
                                                }

                                        fechaLimiteMs =
                                            seleccionado
                                                .timeInMillis

                                        errorFormulario =
                                            null
                                    },

                                    calendario.get(
                                        Calendar.YEAR
                                    ),

                                    calendario.get(
                                        Calendar.MONTH
                                    ),

                                    calendario.get(
                                        Calendar.DAY_OF_MONTH
                                    )
                                ).show()
                            }
                        ) {

                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription =
                                    "Seleccionar fecha límite"
                            )
                        }
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                )
            }

            errorFormulario?.let {
                    mensaje ->

                item {
                    Text(
                        text =
                            mensaje,

                        color =
                            MaterialTheme
                                .colorScheme
                                .error,

                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium
                    )
                }
            }

            item {

                Spacer(
                    modifier =
                        Modifier.height(
                            4.dp
                        )
                )

                Button(
                    onClick = {

                        val duracionNumero =
                            duracion
                                .toIntOrNull()

                        val maxNumero =
                            maxConvocados
                                .toIntOrNull()

                        errorFormulario =
                            validarFormulario(
                                nombreEquipo =
                                    nombreEquipo,

                                nombreRival =
                                    nombreRival,

                                fechaMs =
                                    fechaMs,

                                hora =
                                    hora,

                                lugar =
                                    lugar,

                                tipoPartido =
                                    tipoPartido,

                                duracion =
                                    duracionNumero,

                                maxConvocados =
                                    maxNumero,

                                fechaLimiteMs =
                                    fechaLimiteMs,
                            )

                        if (
                            errorFormulario ==
                            null
                        ) {

                            val partidoId =
                                viewModel
                                    .crearPartido(
                                        nombreEquipo =
                                            nombreEquipo,

                                        nombreRival =
                                            nombreRival,

                                        fechaMs =
                                            fechaMs,

                                        hora =
                                            hora,

                                        lugar =
                                            lugar,

                                        tipoPartido =
                                            tipoPartido,

                                        duracionTiempoMin =
                                            duracionNumero,

                                        maxConvocados =
                                            maxNumero,

                                        fechaLimiteRespuestaMs =
                                            fechaLimiteMs,
                                    )

                            if (
                                partidoId != null
                            ) {
                                onPartidoCreado(
                                    partidoId
                                )
                            } else {

                                errorFormulario =
                                    "Revisa los datos. La fecha límite debe ser anterior o igual a la fecha del partido."
                            }
                        }
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        "Continuar a convocatoria"
                    )
                }
            }

            item {
                Spacer(
                    modifier =
                        Modifier.height(
                            16.dp
                        )
                )
            }
        }
    }
}

private fun validarFormulario(
    nombreEquipo: String,
    nombreRival: String,
    fechaMs: Long?,
    hora: String,
    lugar: String,
    tipoPartido: String,
    duracion: Int?,
    maxConvocados: Int?,
    fechaLimiteMs: Long?,
): String? {

    if (
        nombreEquipo.isBlank()
    ) {
        return "Ingresa el nombre del equipo."
    }

    if (
        nombreRival.isBlank()
    ) {
        return "Ingresa el equipo rival."
    }

    if (
        fechaMs == null
    ) {
        return "Selecciona la fecha del partido."
    }

    if (
        hora.isBlank()
    ) {
        return "Selecciona la hora del partido."
    }

    if (
        lugar.isBlank()
    ) {
        return "Ingresa el lugar del partido."
    }

    if (
        tipoPartido.isBlank()
    ) {
        return "Ingresa el tipo de partido."
    }

    if (
        duracion == null ||
        duracion <= 0
    ) {
        return "Ingresa una duración válida."
    }

    if (
        maxConvocados == null ||
        maxConvocados <= 0
    ) {
        return "Ingresa un máximo de convocados válido."
    }

    if (
        fechaLimiteMs == null
    ) {
        return "Selecciona la fecha límite de respuesta."
    }

    return null
}