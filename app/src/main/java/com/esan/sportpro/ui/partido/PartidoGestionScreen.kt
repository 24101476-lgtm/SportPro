package com.esan.sportpro.ui.partido

import com.esan.sportpro.core.fecha.FechasSportPro
import com.esan.sportpro.ui.common.BarraSuperiorSportPro
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
            FechasSportPro.formatoFecha()
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

            BarraSuperiorSportPro(

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
                        "Reprogramación",
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

                com.esan.sportpro.ui.common.CampoFecha(
                    valorIso = nuevaFechaMs?.let {
                        java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString()
                    }.orEmpty(),
                    onCambio = { iso ->
                        nuevaFechaMs = java.time.LocalDate.parse(iso)
                            .atStartOfDay(java.time.ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()
                    },
                    etiqueta = "Nueva fecha",
                )

                // ------------------------------------------------
                // Nueva hora

                com.esan.sportpro.ui.common.CampoHora(
                    valor = nuevaHora,
                    onCambio = { nuevaHora = it },
                    etiqueta = "Nueva hora",
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

                com.esan.sportpro.ui.common.CampoFecha(
                    valorIso = nuevaFechaLimiteMs?.let {
                        java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString()
                    }.orEmpty(),
                    onCambio = { iso ->
                        nuevaFechaLimiteMs = java.time.LocalDate.parse(iso)
                            .atStartOfDay(java.time.ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()
                    },
                    etiqueta = "Nueva fecha límite de respuesta",
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