package com.esan.sportpro.ui.partido

import com.esan.sportpro.ui.common.BarraSuperiorSportPro
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.partido.EstadoRespuestaConvocatoria
import com.esan.sportpro.domain.partido.RespuestaConvocatoria
import androidx.compose.material3.OutlinedButton

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun ConvocatoriaSeguimientoScreen(
    onVolver: () -> Unit,
    onGestionarPartido: () -> Unit,
    onPrepararAlineacion: () -> Unit,
    viewModel: ConvocatoriaSeguimientoViewModel = hiltViewModel(),
) {

    val state by viewModel.uiState.collectAsState()

    val snackbarHostState =
        remember {
            SnackbarHostState()
        }

    LaunchedEffect(state.mensaje) {

        state.mensaje?.let { mensaje ->

            snackbarHostState.showSnackbar(
                mensaje
            )

            viewModel.mensajeMostrado()
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
                        "Seguimiento de convocatoria"
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

        when {

            state.cargando -> {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement =
                        Arrangement.Center,
                    horizontalAlignment =
                        Alignment.CenterHorizontally,
                ) {

                    CircularProgressIndicator()

                    Text(
                        text = "Cargando convocatoria...",
                        modifier =
                            Modifier.padding(
                                top = 12.dp
                            )
                    )
                }
            }

            else -> {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {

                    LazyColumn(
                        modifier =
                            Modifier.weight(1f),

                        contentPadding =
                            PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 16.dp,
                                bottom = 16.dp,
                            ),

                        verticalArrangement =
                            Arrangement.spacedBy(
                                12.dp
                            ),
                    ) {

                        item {

                            Text(
                                text =
                                    "US-014 · SEGUIMIENTO",
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
                                text =
                                    state.partido
                                        ?.let {
                                            "${it.nombreEquipo} vs ${it.nombreRival}"
                                        }
                                        ?: "Convocatoria",
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleLarge,
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }

                        item {

                            Row(
                                modifier =
                                    Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement.spacedBy(
                                        8.dp
                                    )
                            ) {

                                ResumenConvocatoria(
                                    titulo = "Confirmados",
                                    cantidad =
                                        state.confirmados,
                                    modifier =
                                        Modifier.weight(1f)
                                )

                                ResumenConvocatoria(
                                    titulo = "No disponibles",
                                    cantidad =
                                        state.noDisponibles,
                                    modifier =
                                        Modifier.weight(1f)
                                )

                                ResumenConvocatoria(
                                    titulo = "Sin responder",
                                    cantidad =
                                        state.sinResponder,
                                    modifier =
                                        Modifier.weight(1f)
                                )
                            }
                        }

                        if (
                            state.respuestas.isEmpty()
                        ) {

                            item {

                                Text(
                                    text =
                                        "Aún no hay jugadores en esta convocatoria.",
                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodyMedium
                                )
                            }

                        } else {

                            items(
                                items =
                                    state.respuestas,

                                key = {
                                    it.jugadorId
                                }
                            ) { respuesta ->

                                RespuestaConvocatoriaItem(
                                    respuesta =
                                        respuesta
                                )
                            }
                        }
                    }

                    Surface(
                        shadowElevation = 8.dp,
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Column(
                            modifier =
                                Modifier.padding(
                                    16.dp
                                ),
                            verticalArrangement =
                                Arrangement.spacedBy(
                                    10.dp
                                )
                        ) {

                            OutlinedButton(
                                onClick =
                                    onGestionarPartido,

                                modifier =
                                    Modifier.fillMaxWidth()
                            ) {

                                Text(
                                    "Gestionar partido"
                                )
                            }

                            Button(
                                onClick = onPrepararAlineacion,
                                enabled = state.confirmados >= 11,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Preparar alineación")
                            }


                            Button(
                                onClick =
                                    viewModel::enviarRecordatorioPendientes,

                                enabled =
                                    state.sinResponder > 0 &&
                                            !state.enviandoRecordatorio,

                                modifier =
                                    Modifier.fillMaxWidth()
                            ) {

                                if (
                                    state.enviandoRecordatorio
                                ) {

                                    CircularProgressIndicator()

                                } else {

                                    Text(
                                        text =
                                            if (
                                                state.sinResponder > 0
                                            ) {
                                                "Recordar pendientes (${state.sinResponder})"
                                            } else {
                                                "Sin pendientes"
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResumenConvocatoria(
    titulo: String,
    cantidad: Int,
    modifier: Modifier = Modifier,
) {

    Surface(
        modifier = modifier,
        shape =
            RoundedCornerShape(
                14.dp
            ),
        tonalElevation = 2.dp,
    ) {

        Column(
            modifier =
                Modifier.padding(
                    12.dp
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally,
        ) {

            Text(
                text =
                    cantidad.toString(),
                style =
                    MaterialTheme
                        .typography
                        .headlineSmall,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(
                        4.dp
                    )
            )

            Text(
                text = titulo,
                style =
                    MaterialTheme
                        .typography
                        .labelSmall
            )
        }
    }
}

@Composable
private fun RespuestaConvocatoriaItem(
    respuesta: RespuestaConvocatoria,
) {

    val icono =
        when (respuesta.estado) {

            EstadoRespuestaConvocatoria.CONFIRMADO ->
                Icons.Default.CheckCircle

            EstadoRespuestaConvocatoria.NO_DISPONIBLE ->
                Icons.Default.Close

            EstadoRespuestaConvocatoria.SIN_RESPONDER ->
                Icons.Default.Schedule
        }

    val textoEstado =
        when (respuesta.estado) {

            EstadoRespuestaConvocatoria.CONFIRMADO ->
                "Confirmado"

            EstadoRespuestaConvocatoria.NO_DISPONIBLE ->
                "No disponible"

            EstadoRespuestaConvocatoria.SIN_RESPONDER ->
                "Sin responder"
        }

    Surface(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                16.dp
            ),

        tonalElevation =
            1.dp
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        16.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically,
        ) {

            Icon(
                imageVector =
                    icono,
                contentDescription =
                    null
            )

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(
                            start = 12.dp
                        )
            ) {

                Text(
                    text =
                        respuesta.nombre,
                    fontWeight =
                        FontWeight.SemiBold,
                    style =
                        MaterialTheme
                            .typography
                            .bodyLarge
                )

                Text(
                    text =
                        textoEstado,
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )

                if (
                    !respuesta.motivo
                        .isNullOrBlank()
                ) {

                    Text(
                        text =
                            "Motivo: ${respuesta.motivo}",
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                        modifier =
                            Modifier.padding(
                                top = 4.dp
                            )
                    )
                }
            }
        }
    }
}