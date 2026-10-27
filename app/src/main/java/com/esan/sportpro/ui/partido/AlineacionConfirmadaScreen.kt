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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.partido.JugadorConvocado


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlineacionConfirmadaScreen(
    onFinalizar: () -> Unit,
    viewModel: AlineacionViewModel = hiltViewModel(),
) {

    val state by
    viewModel.uiState.collectAsState()

    Scaffold(

        topBar = {

            BarraSuperiorSportPro(
                title = {
                    Text(
                        "Alineación confirmada"
                    )
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
                    16.dp
                ),
        ) {

            // -----------------------------------------
            // CONFIRMACIÓN
            // -----------------------------------------

            item {

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth(),

                    horizontalAlignment =
                        Alignment.CenterHorizontally,

                    verticalArrangement =
                        Arrangement.spacedBy(
                            10.dp
                        )
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.CheckCircle,
                        contentDescription =
                            null,
                        tint =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )

                    Text(
                        text =
                            "¡Alineación lista!",
                        style =
                            MaterialTheme
                                .typography
                                .headlineSmall,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        text =
                            "La alineación fue guardada y ya está disponible para el registro del partido.",
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
            }


            // -----------------------------------------
            // PARTIDO
            // -----------------------------------------

            state.partido?.let {
                    partido ->

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
                                        .SportsSoccer,
                                contentDescription =
                                    null
                            )

                            Column {

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

                                Text(
                                    text =
                                        "Alineación confirmada",
                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodySmall
                                )
                            }
                        }
                    }
                }
            }


            // -----------------------------------------
            // RESUMEN
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
                                Icons.Default.Groups,
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

                Text(
                    text =
                        "Once titular",
                    style =
                        MaterialTheme
                            .typography
                            .titleLarge,
                    fontWeight =
                        FontWeight.Bold
                )
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

                JugadorConfirmadoItem(
                    jugador =
                        jugador
                )
            }


            // -----------------------------------------
            // FINALIZAR
            // -----------------------------------------

            item {

                Button(
                    onClick =
                        onFinalizar,

                    modifier =
                        Modifier
                            .fillMaxWidth()
                ) {

                    Text(
                        "Volver a partidos"
                    )
                }
            }
        }
    }
}


@Composable
private fun JugadorConfirmadoItem(
    jugador: JugadorConvocado,
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

                Text(
                    text =
                        listOfNotNull(
                            jugador.posicion
                                ?.let {
                                    "Posición: $it"
                                },

                            jugador.dorsal
                                ?.let {
                                    "Dorsal: $it"
                                }
                        )
                            .joinToString(
                                " · "
                            )
                            .ifBlank {
                                "Titular"
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

            Icon(
                imageVector =
                    Icons.Default.CheckCircle,
                contentDescription =
                    "Confirmado",
                tint =
                    MaterialTheme
                        .colorScheme
                        .primary
            )
        }
    }
}