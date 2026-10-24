package com.esan.sportpro.ui.partido

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConvocatoriaVencidaScreen(
    onFinalizar: () -> Unit,
    viewModel: ConvocatoriaRespuestaViewModel = hiltViewModel(),
) {

    val state by viewModel.uiState.collectAsState()

    val formatoFecha =
        SimpleDateFormat(
            "dd/MM/yyyy",
            Locale.getDefault()
        )

    Scaffold { padding ->

        if (state.cargando) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment =
                    Alignment.CenterHorizontally,
                verticalArrangement =
                    Arrangement.Center
            ) {

                CircularProgressIndicator()
            }

        } else {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),

                horizontalAlignment =
                    Alignment.CenterHorizontally,

                verticalArrangement =
                    Arrangement.Center
            ) {

                Icon(
                    imageVector =
                        Icons.Default.EventBusy,
                    contentDescription = null,
                    tint =
                        MaterialTheme
                            .colorScheme
                            .error
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            18.dp
                        )
                )

                Text(
                    text =
                        "Convocatoria vencida",
                    style =
                        MaterialTheme
                            .typography
                            .headlineSmall,
                    fontWeight =
                        FontWeight.Bold,
                    textAlign =
                        TextAlign.Center
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            10.dp
                        )
                )

                Text(
                    text =
                        "El plazo para responder esta convocatoria ya finalizó.",
                    style =
                        MaterialTheme
                            .typography
                            .bodyLarge,
                    textAlign =
                        TextAlign.Center,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            24.dp
                        )
                )

                state.partido?.let { partido ->

                    Surface(
                        modifier =
                            Modifier.fillMaxWidth(),
                        shape =
                            MaterialTheme
                                .shapes
                                .large,
                        tonalElevation = 2.dp
                    ) {

                        Column(
                            modifier =
                                Modifier.padding(
                                    18.dp
                                ),
                            verticalArrangement =
                                Arrangement.spacedBy(
                                    12.dp
                                )
                        ) {

                            Text(
                                text =
                                    "${partido.nombreEquipo} vs ${partido.nombreRival}",
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleMedium,
                                fontWeight =
                                    FontWeight.Bold
                            )

                            partido
                                .fechaLimiteRespuestaMs
                                ?.let { limite ->

                                    Column {

                                        Text(
                                            text =
                                                "Fecha límite de respuesta",
                                            style =
                                                MaterialTheme
                                                    .typography
                                                    .labelMedium,
                                            color =
                                                MaterialTheme
                                                    .colorScheme
                                                    .onSurfaceVariant
                                        )

                                        androidx.compose.foundation.layout.Row(
                                            verticalAlignment =
                                                Alignment.CenterVertically
                                        ) {

                                            Icon(
                                                imageVector =
                                                    Icons.Default.Schedule,
                                                contentDescription = null
                                            )

                                            Text(
                                                text =
                                                    formatoFecha.format(
                                                        Date(limite)
                                                    ),
                                                modifier =
                                                    Modifier.padding(
                                                        start = 8.dp
                                                    ),
                                                style =
                                                    MaterialTheme
                                                        .typography
                                                        .bodyLarge,
                                                fontWeight =
                                                    FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                            state.respuesta
                                ?.let { respuesta ->

                                    Text(
                                        text =
                                            "Jugador: ${respuesta.nombre}",
                                        style =
                                            MaterialTheme
                                                .typography
                                                .bodyMedium
                                    )
                                }
                        }
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(
                            20.dp
                        )
                )

                Text(
                    text =
                        "Si necesitas comunicar tu disponibilidad, coordina directamente con el entrenador.",
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,
                    textAlign =
                        TextAlign.Center,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            28.dp
                        )
                )

                Button(
                    onClick =
                        onFinalizar,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        "Entendido"
                    )
                }
            }
        }
    }
}