package com.esan.sportpro.ui.partido

import com.esan.sportpro.core.fecha.FechasSportPro
import com.esan.sportpro.ui.common.BarraSuperiorSportPro
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.SportsSoccer
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlineacionPrepararScreen(
    onVolver: () -> Unit,
    onContinuar: () -> Unit,
    viewModel: AlineacionViewModel = hiltViewModel(),
) {

    val state by
    viewModel.uiState.collectAsState()

    val snackbar =
        remember {
            SnackbarHostState()
        }

    LaunchedEffect(
        state.mensaje
    ) {

        state.mensaje?.let {
                mensaje ->

            snackbar.showSnackbar(
                mensaje
            )

            viewModel
                .mensajeMostrado()
        }
    }

    Scaffold(

        snackbarHost = {
            SnackbarHost(
                hostState = snackbar
            )
        },

        topBar = {

            BarraSuperiorSportPro(

                title = {
                    Text(
                        "Preparar alineación"
                    )
                },

                navigationIcon = {

                    IconButton(
                        onClick = onVolver
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.ArrowBack,
                            contentDescription =
                                "Volver"
                        )
                    }
                }
            )
        }

    ) {
            padding ->

        when {

            state.cargando -> {

                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding),

                    verticalArrangement =
                        Arrangement.Center,

                    horizontalAlignment =
                        Alignment.CenterHorizontally,
                ) {

                    CircularProgressIndicator()

                    Text(
                        text =
                            "Cargando jugadores confirmados...",
                        modifier =
                            Modifier.padding(
                                top = 12.dp
                            )
                    )
                }
            }


            state.partido == null -> {

                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(24.dp),

                    verticalArrangement =
                        Arrangement.Center,

                    horizontalAlignment =
                        Alignment.CenterHorizontally,
                ) {

                    Text(
                        text =
                            "No se pudo cargar el partido.",
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium
                    )
                }
            }


            else -> {

                val partido =
                    state.partido!!

                val formatoFecha =
                    FechasSportPro.formatoFecha()

                LazyColumn(

                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding),

                    contentPadding =
                        PaddingValues(
                            16.dp
                        ),

                    verticalArrangement =
                        Arrangement.spacedBy(
                            16.dp
                        ),
                ) {

                    // -----------------------------------------
                    // ENCABEZADO
                    // -----------------------------------------

                    item {

                        Text(
                            text =
                                "US-016 · ALINEACIÓN",
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
                                "Prepara el equipo para el partido",
                            style =
                                MaterialTheme
                                    .typography
                                    .headlineSmall,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }


                    item {

                        Text(
                            text =
                                "Selecciona los 11 titulares y organiza a los jugadores suplentes antes de iniciar el partido.",
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


                    // -----------------------------------------
                    // DATOS DEL PARTIDO
                    // -----------------------------------------

                    item {

                        Surface(
                            modifier =
                                Modifier
                                    .fillMaxWidth(),

                            shape =
                                MaterialTheme
                                    .shapes
                                    .large,

                            tonalElevation =
                                2.dp,
                        ) {

                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            18.dp
                                        ),

                                verticalArrangement =
                                    Arrangement
                                        .spacedBy(
                                            8.dp
                                        )
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default
                                            .SportsSoccer,

                                    contentDescription =
                                        null,

                                    tint =
                                        MaterialTheme
                                            .colorScheme
                                            .primary
                                )

                                Text(
                                    text =
                                        "${partido.nombreEquipo} vs ${partido.nombreRival}",
                                    style =
                                        MaterialTheme
                                            .typography
                                            .titleLarge,
                                    fontWeight =
                                        FontWeight.Bold
                                )

                                partido.fechaMs
                                    ?.let {
                                            fecha ->

                                        Text(
                                            text =
                                                "Fecha: ${
                                                    formatoFecha
                                                        .format(
                                                            Date(
                                                                fecha
                                                            )
                                                        )
                                                }"
                                        )
                                    }

                                partido.hora
                                    ?.takeIf {
                                        it.isNotBlank()
                                    }
                                    ?.let {
                                            hora ->

                                        Text(
                                            text =
                                                "Hora: $hora"
                                        )
                                    }

                                partido.lugar
                                    ?.takeIf {
                                        it.isNotBlank()
                                    }
                                    ?.let {
                                            lugar ->

                                        Text(
                                            text =
                                                "Lugar: $lugar"
                                        )
                                    }
                            }
                        }
                    }


                    // -----------------------------------------
                    // JUGADORES CONFIRMADOS
                    // -----------------------------------------

                    item {

                        Surface(
                            modifier =
                                Modifier
                                    .fillMaxWidth(),

                            shape =
                                MaterialTheme
                                    .shapes
                                    .large,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .secondaryContainer,
                        ) {

                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            18.dp
                                        ),

                                verticalArrangement =
                                    Arrangement
                                        .spacedBy(
                                            8.dp
                                        )
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default
                                            .Groups,

                                    contentDescription =
                                        null,

                                    tint =
                                        MaterialTheme
                                            .colorScheme
                                            .onSecondaryContainer
                                )

                                Text(
                                    text =
                                        "${state.jugadores.size} jugadores confirmados",
                                    style =
                                        MaterialTheme
                                            .typography
                                            .titleMedium,
                                    fontWeight =
                                        FontWeight.Bold,
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onSecondaryContainer
                                )

                                Text(
                                    text =
                                        if (
                                            state.jugadores.size >=
                                            11
                                        ) {

                                            "Tienes suficientes jugadores para preparar la alineación."

                                        } else {

                                            "Se necesitan al menos 11 jugadores confirmados."
                                        },

                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodyMedium,

                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onSecondaryContainer
                                )
                            }
                        }
                    }


                    // -----------------------------------------
                    // ESTADO SI YA ESTÁ CONFIRMADA
                    // -----------------------------------------

                    if (
                        partido
                            .alineacionConfirmada
                    ) {

                        item {

                            Surface(
                                modifier =
                                    Modifier
                                        .fillMaxWidth(),

                                shape =
                                    MaterialTheme
                                        .shapes
                                        .medium,

                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .tertiaryContainer,
                            ) {

                                Text(
                                    text =
                                        "Este partido ya tiene una alineación confirmada. Puedes revisarla o modificarla antes de iniciar el encuentro.",

                                    modifier =
                                        Modifier
                                            .padding(
                                                16.dp
                                            ),

                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onTertiaryContainer,

                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodyMedium
                                )
                            }
                        }
                    }


                    // -----------------------------------------
                    // CONTINUAR
                    // -----------------------------------------

                    item {

                        Button(
                            onClick =
                                onContinuar,

                            enabled =
                                state
                                    .jugadores
                                    .size >= 11,

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                        ) {

                            Text(
                                text =
                                    if (
                                        partido
                                            .alineacionConfirmada
                                    ) {
                                        "Revisar alineación"
                                    } else {
                                        "Seleccionar titulares"
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}