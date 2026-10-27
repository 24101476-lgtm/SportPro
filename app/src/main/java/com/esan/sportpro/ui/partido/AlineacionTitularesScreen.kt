package com.esan.sportpro.ui.partido

import com.esan.sportpro.ui.common.BarraSuperiorSportPro
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import com.esan.sportpro.domain.partido.JugadorConvocado


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlineacionTitularesScreen(
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

            viewModel.mensajeMostrado()
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
                        "Seleccionar titulares"
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
                            "Cargando jugadores...",
                        modifier =
                            Modifier.padding(
                                top = 12.dp
                            )
                    )
                }
            }


            else -> {

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
                            12.dp
                        ),
                ) {

                    // -----------------------------------------
                    // ENCABEZADO
                    // -----------------------------------------

                    item {

                        Text(
                            text =
                                "US-016 · TITULARES",
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
                                "Selecciona los 11 titulares",
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
                                "Elige quiénes iniciarán el partido. Los demás jugadores confirmados quedarán como suplentes.",
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
                    // CONTADOR
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
                                if (
                                    state
                                        .cantidadTitulares ==
                                    11
                                ) {

                                    MaterialTheme
                                        .colorScheme
                                        .primaryContainer

                                } else {

                                    MaterialTheme
                                        .colorScheme
                                        .surfaceVariant
                                },
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

                                horizontalArrangement =
                                    Arrangement
                                        .spacedBy(
                                            12.dp
                                        )
                            ) {

                                Icon(
                                    imageVector =
                                        if (
                                            state
                                                .cantidadTitulares ==
                                            11
                                        ) {

                                            Icons.Default
                                                .CheckCircle

                                        } else {

                                            Icons.Default
                                                .Person
                                        },

                                    contentDescription =
                                        null
                                )

                                Column {

                                    Text(
                                        text =
                                            "${state.cantidadTitulares} de 11 titulares",
                                        style =
                                            MaterialTheme
                                                .typography
                                                .titleMedium,
                                        fontWeight =
                                            FontWeight.Bold
                                    )

                                    Text(
                                        text =
                                            when {

                                                state
                                                    .cantidadTitulares <
                                                        11 ->

                                                    "Selecciona ${
                                                        11 -
                                                                state
                                                                    .cantidadTitulares
                                                    } jugador(es) más."

                                                else ->

                                                    "Los 11 titulares están completos."
                                            },

                                        style =
                                            MaterialTheme
                                                .typography
                                                .bodySmall
                                    )
                                }
                            }
                        }
                    }


                    // -----------------------------------------
                    // JUGADORES
                    // -----------------------------------------

                    if (
                        state
                            .jugadores
                            .isEmpty()
                    ) {

                        item {

                            Text(
                                text =
                                    "No hay jugadores confirmados para este partido.",
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodyMedium
                            )
                        }

                    } else {

                        items(
                            items =
                                state.jugadores,

                            key = {
                                    jugador ->
                                jugador.id
                            }
                        ) {
                                jugador ->

                            JugadorTitularItem(
                                jugador =
                                    jugador,

                                onCambiar = {

                                    viewModel
                                        .cambiarTitularidad(
                                            jugador.id
                                        )
                                }
                            )
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
                                    .cantidadTitulares ==
                                        11,

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                        ) {

                            Text(
                                "Continuar con posiciones"
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun JugadorTitularItem(
    jugador: JugadorConvocado,
    onCambiar: () -> Unit,
) {

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    onClick =
                        onCambiar
                ),

        shape =
            MaterialTheme
                .shapes
                .large,

        tonalElevation =
            if (
                jugador.titular
            ) {
                4.dp
            } else {
                1.dp
            },

        color =
            if (
                jugador.titular
            ) {

                MaterialTheme
                    .colorScheme
                    .primaryContainer

            } else {

                MaterialTheme
                    .colorScheme
                    .surface
            },
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        14.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically,
        ) {

            Checkbox(
                checked =
                    jugador.titular,

                onCheckedChange = {
                    onCambiar()
                }
            )

            Column(
                modifier =
                    Modifier
                        .weight(
                            1f
                        )
                        .padding(
                            start = 8.dp
                        )
            ) {

                Text(
                    text =
                        jugador.nombre,
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,
                    fontWeight =
                        FontWeight.SemiBold
                )

                Text(
                    text =
                        jugador.dorsal
                            ?.let {
                                    dorsal ->
                                "Dorsal $dorsal"
                            }
                            ?: "Dorsal por definir",
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

            Text(
                text =
                    if (
                        jugador.titular
                    ) {
                        "Titular"
                    } else {
                        "Suplente"
                    },

                style =
                    MaterialTheme
                        .typography
                        .labelMedium,

                color =
                    if (
                        jugador.titular
                    ) {

                        MaterialTheme
                            .colorScheme
                            .primary

                    } else {

                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                    }
            )
        }
    }
}