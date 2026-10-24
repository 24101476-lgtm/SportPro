package com.esan.sportpro.ui.partido

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartidoGestionScreen(
    onVolver: () -> Unit,
    onFinalizado: () -> Unit,
    viewModel: PartidoGestionViewModel = hiltViewModel(),
) {

    val state by viewModel.uiState.collectAsState()

    val context =
        LocalContext.current

    val snackbarHostState =
        remember {
            SnackbarHostState()
        }

    val formatoFecha =
        remember {
            SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            )
        }

    var nuevaFechaMs by rememberSaveable {
        mutableStateOf<Long?>(null)
    }

    var nuevaHora by rememberSaveable {
        mutableStateOf("")
    }

    var nuevoLugar by rememberSaveable {
        mutableStateOf("")
    }

    var nuevaFechaLimiteMs by rememberSaveable {
        mutableStateOf<Long?>(null)
    }

    var formularioInicializado by rememberSaveable {
        mutableStateOf(false)
    }

    var mostrarCancelar by rememberSaveable {
        mutableStateOf(false)
    }

    var motivoCancelacion by rememberSaveable {
        mutableStateOf("")
    }

    // ---------------------------------------------------------
    // Precargar datos actuales del partido

    LaunchedEffect(state.partido) {

        val partido =
            state.partido

        if (
            partido != null &&
            !formularioInicializado
        ) {

            nuevaFechaMs =
                partido.fechaMs

            nuevaHora =
                partido.hora.orEmpty()

            nuevoLugar =
                partido.lugar.orEmpty()

            nuevaFechaLimiteMs =
                partido.fechaLimiteRespuestaMs

            formularioInicializado =
                true
        }
    }

    // ---------------------------------------------------------
    // Mensajes

    LaunchedEffect(state.mensaje) {

        state.mensaje?.let { mensaje ->

            snackbarHostState.showSnackbar(
                mensaje
            )

            viewModel.mensajeMostrado()
        }
    }

    // ---------------------------------------------------------
    // Reprogramado o cancelado

    LaunchedEffect(
        state.reprogramado,
        state.cancelado
    ) {

        if (
            state.reprogramado ||
            state.cancelado
        ) {

            viewModel.resultadoConsumido()

            onFinalizado()
        }
    }

    Scaffold(

        snackbarHost = {

            SnackbarHost(
                hostState =
                    snackbarHostState
            )
        },

        topBar = {

            TopAppBar(

                title = {
                    Text(
                        "Gestionar partido"
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

        if (state.cargando) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement =
                    Arrangement.Center
            ) {

                CircularProgressIndicator(
                    modifier =
                        Modifier.padding(
                            24.dp
                        )
                )
            }

        } else {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(16.dp),

                verticalArrangement =
                    Arrangement.spacedBy(
                        14.dp
                    )
            ) {

                // ------------------------------------------------
                // Encabezado

                Text(
                    text =
                        "US-014 · REPROGRAMACIÓN",
                    style =
                        MaterialTheme
                            .typography
                            .labelMedium,
                    color =
                        MaterialTheme
                            .colorScheme
                            .primary
                )

                Text(
                    text =
                        state.partido
                            ?.let {
                                "${it.nombreEquipo} vs ${it.nombreRival}"
                            }
                            ?: "Partido",
                    style =
                        MaterialTheme
                            .typography
                            .titleLarge,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        "Actualiza los datos del partido o cancélalo si ya no se realizará.",
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            4.dp
                        )
                )

                // ------------------------------------------------
                // Nueva fecha

                OutlinedTextField(
                    value =
                        nuevaFechaMs?.let {
                            formatoFecha.format(
                                Date(it)
                            )
                        }.orEmpty(),

                    onValueChange = {},

                    readOnly = true,

                    label = {
                        Text(
                            "Nueva fecha"
                        )
                    },

                    leadingIcon = {

                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null
                        )
                    },

                    trailingIcon = {

                        IconButton(
                            onClick = {

                                val calendario =
                                    Calendar.getInstance()

                                nuevaFechaMs?.let {
                                    calendario.timeInMillis =
                                        it
                                }

                                DatePickerDialog(
                                    context,

                                    { _, year, month, day ->

                                        val seleccionado =
                                            Calendar.getInstance()

                                        seleccionado.set(
                                            year,
                                            month,
                                            day,
                                            0,
                                            0,
                                            0
                                        )

                                        seleccionado.set(
                                            Calendar.MILLISECOND,
                                            0
                                        )

                                        nuevaFechaMs =
                                            seleccionado.timeInMillis
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

                // ------------------------------------------------
                // Nueva hora

                OutlinedTextField(
                    value =
                        nuevaHora,

                    onValueChange = {},

                    readOnly = true,

                    label = {
                        Text(
                            "Nueva hora"
                        )
                    },

                    leadingIcon = {

                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null
                        )
                    },

                    trailingIcon = {

                        IconButton(
                            onClick = {

                                val calendario =
                                    Calendar.getInstance()

                                TimePickerDialog(
                                    context,

                                    { _, hour, minute ->

                                        nuevaHora =
                                            String.format(
                                                Locale.getDefault(),
                                                "%02d:%02d",
                                                hour,
                                                minute
                                            )
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

                // ------------------------------------------------
                // Lugar

                OutlinedTextField(
                    value =
                        nuevoLugar,

                    onValueChange = {
                        nuevoLugar = it
                    },

                    label = {
                        Text(
                            "Nuevo lugar"
                        )
                    },

                    leadingIcon = {

                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null
                        )
                    },

                    singleLine = true,

                    modifier =
                        Modifier.fillMaxWidth()
                )

                // ------------------------------------------------
                // Fecha límite

                OutlinedTextField(
                    value =
                        nuevaFechaLimiteMs?.let {
                            formatoFecha.format(
                                Date(it)
                            )
                        }.orEmpty(),

                    onValueChange = {},

                    readOnly = true,

                    label = {
                        Text(
                            "Nueva fecha límite de respuesta"
                        )
                    },

                    leadingIcon = {

                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null
                        )
                    },

                    trailingIcon = {

                        IconButton(
                            onClick = {

                                val calendario =
                                    Calendar.getInstance()

                                nuevaFechaLimiteMs?.let {
                                    calendario.timeInMillis =
                                        it
                                }

                                DatePickerDialog(
                                    context,

                                    { _, year, month, day ->

                                        val seleccionado =
                                            Calendar.getInstance()

                                        seleccionado.set(
                                            year,
                                            month,
                                            day,
                                            0,
                                            0,
                                            0
                                        )

                                        seleccionado.set(
                                            Calendar.MILLISECOND,
                                            0
                                        )

                                        nuevaFechaLimiteMs =
                                            seleccionado.timeInMillis
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

                // ------------------------------------------------
                // Guardar reprogramación

                Button(
                    onClick = {

                        viewModel.reprogramar(
                            nuevaFechaMs =
                                nuevaFechaMs,

                            nuevaHora =
                                nuevaHora,

                            nuevoLugar =
                                nuevoLugar,

                            nuevaFechaLimiteRespuestaMs =
                                nuevaFechaLimiteMs,
                        )
                    },

                    enabled =
                        !state.guardando,

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    if (state.guardando) {

                        CircularProgressIndicator()

                    } else {

                        Text(
                            "Guardar reprogramación"
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(
                            12.dp
                        )
                )

                // ------------------------------------------------
                // Cancelar

                Text(
                    text =
                        "Cancelar partido",
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        "Esta opción cambia el estado del partido a cancelado y registra el motivo.",
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )

                OutlinedButton(
                    onClick = {
                        mostrarCancelar =
                            true
                    },

                    enabled =
                        !state.guardando,

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        "Cancelar partido"
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(
                            24.dp
                        )
                )
            }
        }
    }

    // =========================================================
    // Diálogo de cancelación

    if (mostrarCancelar) {

        AlertDialog(

            onDismissRequest = {
                mostrarCancelar =
                    false
            },

            title = {
                Text(
                    "Cancelar partido"
                )
            },

            text = {

                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(
                            12.dp
                        )
                ) {

                    Text(
                        "Indica el motivo de la cancelación."
                    )

                    OutlinedTextField(
                        value =
                            motivoCancelacion,

                        onValueChange = {
                            motivoCancelacion = it
                        },

                        label = {
                            Text(
                                "Motivo"
                            )
                        },

                        minLines = 3,

                        modifier =
                            Modifier.fillMaxWidth()
                    )
                }
            },

            confirmButton = {

                Button(
                    onClick = {

                        viewModel.cancelar(
                            motivoCancelacion
                        )

                        if (
                            motivoCancelacion
                                .isNotBlank()
                        ) {

                            mostrarCancelar =
                                false
                        }
                    },

                    enabled =
                        !state.guardando
                ) {

                    Text(
                        "Confirmar cancelación"
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {

                        mostrarCancelar =
                            false
                    }
                ) {

                    Text(
                        "Volver"
                    )
                }
            }
        )
    }
}