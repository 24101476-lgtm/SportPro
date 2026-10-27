package com.esan.sportpro.ui.partido

import com.esan.sportpro.ui.common.BarraSuperiorSportPro
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
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
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
import com.esan.sportpro.domain.partido.JugadorConvocado


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlineacionResumenScreen(
    onVolver: () -> Unit,
    onConfirmada: () -> Unit,
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

    LaunchedEffect(
        state.alineacionGuardada
    ) {

        if (
            state.alineacionGuardada
        ) {

            onConfirmada()
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
                        "Revisar alineación"
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
                    14.dp
                ),
        ) {

            // -----------------------------------------
            // ENCABEZADO
            // -----------------------------------------

            item {

                Text(
                    text =
                        "US-016 · RESUMEN",
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
                        "Revisa antes de confirmar",
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
                        "Comprueba los titulares, posiciones, dorsales y suplentes antes de guardar la alineación definitiva.",
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
            // RESUMEN GENERAL
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
                            .primaryContainer,
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
                            Arrangement.spacedBy(
                                12.dp
                            )
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default
                                    .CheckCircle,
                            contentDescription =
                                null
                        )

                        Column {

                            Text(
                                text =
                                    "${state.titulares.size} titulares",
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleMedium,
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(
                                text =
                                    "${state.suplentes.size} suplentes",
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodyMedium
                            )
                        }
                    }
                }
            }


            // -----------------------------------------
            // TITULARES
            // -----------------------------------------

            item {

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically,

                    horizontalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        )
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Person,
                        contentDescription =
                            null,
                        tint =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )

                    Text(
                        text =
                            "Titulares",
                        style =
                            MaterialTheme
                                .typography
                                .titleLarge,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }


            items(
                items =
                    state.titulares,

                key = {
                        jugador ->
                    jugador.id
                }
            ) {
                    jugador ->

                JugadorResumenItem(
                    jugador =
                        jugador,
                    tipo =
                        "Titular"
                )
            }


            // -----------------------------------------
            // SUPLENTES
            // -----------------------------------------

            item {

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically,

                    horizontalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        )
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Groups,
                        contentDescription =
                            null,
                        tint =
                            MaterialTheme
                                .colorScheme
                                .secondary
                    )

                    Text(
                        text =
                            "Suplentes",
                        style =
                            MaterialTheme
                                .typography
                                .titleLarge,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }


            if (
                state.suplentes.isEmpty()
            ) {

                item {

                    Text(
                        text =
                            "No hay suplentes seleccionados.",
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

            } else {

                items(
                    items =
                        state.suplentes,

                    key = {
                            jugador ->
                        jugador.id
                    }
                ) {
                        jugador ->

                    JugadorResumenItem(
                        jugador =
                            jugador,
                        tipo =
                            "Suplente"
                    )
                }
            }


            // -----------------------------------------
            // CONFIRMAR
            // -----------------------------------------

            item {

                Button(
                    onClick = {
                        viewModel
                            .confirmarAlineacion()
                    },

                    enabled =
                        state.puedeConfirmar &&
                                !state.confirmando,

                    modifier =
                        Modifier
                            .fillMaxWidth()
                ) {

                    if (
                        state.confirmando
                    ) {

                        CircularProgressIndicator()

                    } else {

                        Text(
                            "Confirmar alineación"
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun JugadorResumenItem(
    jugador: JugadorConvocado,
    tipo: String,
) {

    Surface(
        modifier =
            Modifier
                .fillMaxWidth(),

        shape =
            MaterialTheme
                .shapes
                .medium,

        tonalElevation =
            1.dp,
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

            Column(
                modifier =
                    Modifier
                        .weight(
                            1f
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

                val detalle =
                    buildList {

                        jugador
                            .posicion
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?.let {
                                add(
                                    "Posición: $it"
                                )
                            }

                        jugador
                            .dorsal
                            ?.let {
                                add(
                                    "Dorsal: $it"
                                )
                            }
                    }

                Text(
                    text =
                        if (
                            detalle.isEmpty()
                        ) {

                            "Sin datos adicionales"

                        } else {

                            detalle
                                .joinToString(
                                    " · "
                                )
                        },

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
                    tipo,
                style =
                    MaterialTheme
                        .typography
                        .labelMedium,
                color =
                    if (
                        tipo ==
                        "Titular"
                    ) {

                        MaterialTheme
                            .colorScheme
                            .primary

                    } else {

                        MaterialTheme
                            .colorScheme
                            .secondary
                    }
            )
        }
    }
}