package com.esan.sportpro.ui.partido

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConvocatoriaNoDisponibleScreen(
    onVolver: () -> Unit,
    onRespuestaGuardada: () -> Unit,
    onConvocatoriaVencida: () -> Unit,
    viewModel: ConvocatoriaRespuestaViewModel = hiltViewModel(),
) {

    val state by viewModel.uiState.collectAsState()

    var motivo by rememberSaveable {
        mutableStateOf("")
    }

    val snackbarHostState =
        remember {
            SnackbarHostState()
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
    // Convocatoria vencida

    LaunchedEffect(state.vencida) {

        if (state.vencida) {
            onConvocatoriaVencida()
        }
    }

    // ---------------------------------------------------------
    // Respuesta registrada

    LaunchedEffect(state.respuestaGuardada) {

        if (state.respuestaGuardada) {

            viewModel.respuestaConsumida()

            onRespuestaGuardada()
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
                        "No podré asistir"
                    )
                },

                navigationIcon = {

                    IconButton(
                        onClick =
                            onVolver
                    ) {

                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription =
                                "Volver"
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
                        16.dp
                    )
            ) {

                Text(
                    text =
                        "US-015 · RESPUESTA",
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
                        "Cuéntanos por qué no podrás asistir",
                    style =
                        MaterialTheme
                            .typography
                            .headlineSmall,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        "Tu respuesta será visible para el entrenador al revisar la convocatoria.",
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )

                state.partido?.let { partido ->

                    Text(
                        text =
                            "${partido.nombreEquipo} vs ${partido.nombreRival}",
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,
                        fontWeight =
                            FontWeight.SemiBold
                    )
                }

                OutlinedTextField(
                    value =
                        motivo,

                    onValueChange = {
                        motivo = it
                    },

                    label = {
                        Text(
                            "Motivo"
                        )
                    },

                    placeholder = {
                        Text(
                            "Ej.: lesión, viaje, compromiso familiar..."
                        )
                    },

                    supportingText = {

                        Text(
                            "${motivo.length} caracteres"
                        )
                    },

                    minLines = 4,
                    maxLines = 7,

                    enabled =
                        !state.guardando &&
                                !state.vencida,

                    modifier =
                        Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {

                        viewModel
                            .responderNoDisponible(
                                motivo
                            )
                    },

                    enabled =
                        motivo.isNotBlank() &&
                                !state.guardando &&
                                !state.vencida,

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    if (state.guardando) {

                        CircularProgressIndicator()

                    } else {

                        Text(
                            "Enviar respuesta"
                        )
                    }
                }

                Text(
                    text =
                        "El motivo es obligatorio para registrar que no estás disponible.",
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }
        }
    }
}