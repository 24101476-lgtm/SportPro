package com.esan.sportpro.ui.partido

import com.esan.sportpro.core.fecha.FechasSportPro
import com.esan.sportpro.ui.common.BarraSuperiorSportPro
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.partido.EstadoRespuestaConvocatoria
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConvocatoriaDetalleJugadorScreen(
    onVolver: () -> Unit,
    onNoDisponible: () -> Unit,
    onRespuestaGuardada: () -> Unit,
    onConvocatoriaVencida: () -> Unit,
    viewModel: ConvocatoriaRespuestaViewModel = hiltViewModel(),
) {

    val state by viewModel.uiState.collectAsState()

    val snackbarHostState =
        remember {
            SnackbarHostState()
        }

    val formatoFecha =
        remember {
            FechasSportPro.formatoFecha()
        }

    LaunchedEffect(state.mensaje) {

        state.mensaje?.let { mensaje ->

            snackbarHostState.showSnackbar(
                mensaje
            )

            viewModel.mensajeMostrado()
        }
    }

    LaunchedEffect(state.vencida) {

        if (state.vencida) {
            onConvocatoriaVencida()
        }
    }

    LaunchedEffect(state.respuestaGuardada) {

        if (state.respuestaGuardada) {

            viewModel.respuestaConsumida()

            onRespuestaGuardada()
        }
    }

    Scaffold(

        snackbarHost = {

            SnackbarHost(
                hostState = snackbarHostState
            )
        },

        topBar = {

            BarraSuperiorSportPro(

                title = {
                    Text(
                        "Convocatoria"
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

                Text(
                    text =
                        "US-015 · MI CONVOCATORIA",
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
                            .headlineSmall,
                    fontWeight =
                        FontWeight.Bold
                )

                state.respuesta?.let { respuesta ->

                    Text(
                        text =
                            "Hola, ${respuesta.nombre}",
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,
                        fontWeight =
                            FontWeight.SemiBold
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(
                            4.dp
                        )
                )

                Surface(
                    modifier =
                        Modifier.fillMaxWidth(),
                    tonalElevation = 2.dp,
                    shape =
                        MaterialTheme
                            .shapes
                            .large
                ) {

                    Column(
                        modifier =
                            Modifier.padding(
                                18.dp
                            ),
                        verticalArrangement =
                            Arrangement.spacedBy(
                                14.dp
                            )
                    ) {

                        state.partido
                            ?.fechaMs
                            ?.let { fecha ->

                                DatoConvocatoria(
                                    icono =
                                        Icons.Default.CalendarMonth,
                                    titulo =
                                        "Fecha",
                                    valor =
                                        formatoFecha.format(
                                            Date(fecha)
                                        )
                                )
                            }

                        DatoConvocatoria(
                            icono =
                                Icons.Default.Schedule,
                            titulo =
                                "Hora",
                            valor =
                                state.partido
                                    ?.hora
                                    .orEmpty()
                                    .ifBlank {
                                        "Por confirmar"
                                    }
                        )

                        DatoConvocatoria(
                            icono =
                                Icons.Default.LocationOn,
                            titulo =
                                "Lugar",
                            valor =
                                state.partido
                                    ?.lugar
                                    .orEmpty()
                                    .ifBlank {
                                        "Por confirmar"
                                    }
                        )

                        state.partido
                            ?.fechaLimiteRespuestaMs
                            ?.let { limite ->

                                DatoConvocatoria(
                                    icono =
                                        Icons.Default.CalendarMonth,
                                    titulo =
                                        "Responder hasta",
                                    valor =
                                        formatoFecha.format(
                                            Date(limite)
                                        )
                                )
                            }
                    }
                }

                state.respuesta?.let { respuesta ->

                    if (
                        respuesta.estado !=
                        EstadoRespuestaConvocatoria.SIN_RESPONDER
                    ) {

                        Text(
                            text =
                                when (
                                    respuesta.estado
                                ) {

                                    EstadoRespuestaConvocatoria.CONFIRMADO ->
                                        "Ya confirmaste tu asistencia."

                                    EstadoRespuestaConvocatoria.NO_DISPONIBLE ->
                                        "Registraste que no podrás asistir."

                                    EstadoRespuestaConvocatoria.SIN_RESPONDER ->
                                        ""
                                },
                            style =
                                MaterialTheme
                                    .typography
                                    .bodyMedium,
                            fontWeight =
                                FontWeight.SemiBold
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(
                            8.dp
                        )
                )

                Button(
                    onClick =
                        viewModel::confirmarAsistencia,

                    enabled =
                        !state.guardando &&
                                !state.vencida,

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    if (state.guardando) {

                        CircularProgressIndicator()

                    } else {

                        Text(
                            "Confirmar asistencia"
                        )
                    }
                }

                OutlinedButton(
                    onClick =
                        onNoDisponible,

                    enabled =
                        !state.guardando &&
                                !state.vencida,

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        "No podré asistir"
                    )
                }
            }
        }
    }
}

@Composable
private fun DatoConvocatoria(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    valor: String,
) {

    Column {

        Text(
            text = titulo,
            style =
                MaterialTheme
                    .typography
                    .labelMedium,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )

        androidx.compose.foundation.layout.Row {

            Icon(
                imageVector = icono,
                contentDescription = null
            )

            Text(
                text = valor,
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
