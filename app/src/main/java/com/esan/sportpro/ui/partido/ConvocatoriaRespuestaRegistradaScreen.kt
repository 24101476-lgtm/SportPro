package com.esan.sportpro.ui.partido

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.EventBusy
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
import com.esan.sportpro.domain.partido.EstadoRespuestaConvocatoria

@Composable
fun ConvocatoriaRespuestaRegistradaScreen(
    onFinalizar: () -> Unit,
    viewModel: ConvocatoriaRespuestaViewModel = hiltViewModel(),
) {

    val state by viewModel.uiState.collectAsState()

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

            val respuesta =
                state.respuesta

            val confirmado =
                respuesta?.estado ==
                        EstadoRespuestaConvocatoria.CONFIRMADO

            val noDisponible =
                respuesta?.estado ==
                        EstadoRespuestaConvocatoria.NO_DISPONIBLE

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
                        Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier =
                        Modifier.padding(
                            bottom = 16.dp
                        ),
                    tint =
                        MaterialTheme
                            .colorScheme
                            .primary
                )

                Text(
                    text =
                        "Respuesta registrada",
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
                            8.dp
                        )
                )

                Text(
                    text =
                        when {

                            confirmado ->
                                "Confirmaste tu asistencia al partido."

                            noDisponible ->
                                "Registraste que no podrás asistir al partido."

                            else ->
                                "Tu respuesta fue registrada."
                        },
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

                            if (confirmado) {

                                EstadoRespuestaFila(
                                    icono =
                                        Icons.Default.EventAvailable,
                                    texto =
                                        "Asistencia confirmada"
                                )
                            }

                            if (noDisponible) {

                                EstadoRespuestaFila(
                                    icono =
                                        Icons.Default.EventBusy,
                                    texto =
                                        "No disponible"
                                )

                                respuesta
                                    ?.motivo
                                    ?.takeIf {
                                        it.isNotBlank()
                                    }
                                    ?.let { motivo ->

                                        Column {

                                            Text(
                                                text =
                                                    "Motivo",
                                                style =
                                                    MaterialTheme
                                                        .typography
                                                        .labelMedium,
                                                color =
                                                    MaterialTheme
                                                        .colorScheme
                                                        .onSurfaceVariant
                                            )

                                            Text(
                                                text =
                                                    motivo,
                                                style =
                                                    MaterialTheme
                                                        .typography
                                                        .bodyMedium
                                            )
                                        }
                                    }
                            }
                        }
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(
                            32.dp
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

@Composable
private fun EstadoRespuestaFila(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    texto: String,
) {

    androidx.compose.foundation.layout.Row(
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Icon(
            imageVector = icono,
            contentDescription = null
        )

        Text(
            text = texto,
            modifier =
                Modifier.padding(
                    start = 10.dp
                ),
            style =
                MaterialTheme
                    .typography
                    .bodyLarge,
            fontWeight =
                FontWeight.SemiBold
        )
    }
}