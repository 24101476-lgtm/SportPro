package com.esan.sportpro.ui.partido

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Group
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.esan.sportpro.domain.partido.JugadorPlantel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConvocatoriaSeleccionScreen(
    onVolver: () -> Unit,
    onConvocatoriaPublicada: () -> Unit,
    viewModel: ConvocatoriaSeleccionViewModel = hiltViewModel(),
) {

    val state by viewModel.uiState.collectAsState()

    val snackbarHostState =
        remember {
            SnackbarHostState()
        }

    /*
     * Mostramos los mensajes generados
     * por el ViewModel.
     */
    LaunchedEffect(state.mensaje) {

        state.mensaje?.let { mensaje ->

            snackbarHostState.showSnackbar(
                mensaje
            )

            viewModel.mensajeMostrado()
        }
    }

    /*
     * Cuando la convocatoria queda publicada,
     * avisamos a PartidoScreen para continuar
     * con la navegación.
     */
    LaunchedEffect(state.publicada) {

        if (state.publicada) {

            viewModel.publicacionConsumida()

            onConvocatoriaPublicada()
        }
    }

    Scaffold(

        snackbarHost = {

            SnackbarHost(
                hostState = snackbarHostState
            )
        },

        topBar = {

            TopAppBar(

                title = {
                    Text(
                        "Seleccionar convocados"
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

            // ------------------------------------------------ Cargando

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
                        text = "Cargando jugadores...",
                        modifier =
                            Modifier.padding(
                                top = 12.dp
                            )
                    )
                }
            }

            // ------------------------------------------------ Sin jugadores

            state.jugadores.isEmpty() -> {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    verticalArrangement =
                        Arrangement.Center,
                    horizontalAlignment =
                        Alignment.CenterHorizontally,
                ) {

                    Icon(
                        Icons.Default.Group,
                        contentDescription = null,
                    )

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    Text(
                        text =
                            "No hay jugadores disponibles.",
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        text =
                            "No se encontraron jugadores vinculados al equipo.",
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,
                        modifier =
                            Modifier.padding(
                                top = 6.dp
                            )
                    )
                }
            }

            // ------------------------------------------------ Lista

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
                                10.dp
                            ),
                    ) {

                        // ---------------------------------------- Cabecera

                        item {

                            Text(
                                text =
                                    "US-014 · CONVOCATORIA",
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
                                    "Selecciona a los jugadores",
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleLarge,
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }

                        item {

                            Text(
                                text =
                                    "Elige a los jugadores que recibirán la convocatoria para este partido.",
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodyMedium,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant,
                            )
                        }

                        // ---------------------------------------- Contador

                        item {

                            Surface(
                                shape =
                                    RoundedCornerShape(
                                        16.dp
                                    ),
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .primaryContainer,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                            ) {

                                Column(
                                    modifier =
                                        Modifier.padding(
                                            16.dp
                                        )
                                ) {

                                    Text(
                                        text =
                                            "Seleccionados",
                                        style =
                                            MaterialTheme
                                                .typography
                                                .labelMedium
                                    )

                                    Text(
                                        text =
                                            "${state.seleccionados.size} / ${state.maxConvocados}",
                                        style =
                                            MaterialTheme
                                                .typography
                                                .headlineSmall,
                                        fontWeight =
                                            FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // ---------------------------------------- Acciones

                        item {

                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement.SpaceBetween,
                                verticalAlignment =
                                    Alignment.CenterVertically,
                            ) {

                                TextButton(
                                    onClick =
                                        viewModel::seleccionarTodos
                                ) {

                                    Text(
                                        "Seleccionar todos"
                                    )
                                }

                                TextButton(
                                    onClick =
                                        viewModel::limpiarSeleccion
                                ) {

                                    Text(
                                        "Limpiar"
                                    )
                                }
                            }
                        }

                        // ---------------------------------------- Jugadores

                        items(
                            items =
                                state.jugadores,

                            key = {
                                it.id
                            }
                        ) { jugador ->

                            JugadorConvocatoriaItem(
                                jugador =
                                    jugador,

                                seleccionado =
                                    jugador.id in
                                            state.seleccionados,

                                onCambiar = {

                                    viewModel
                                        .alternarSeleccion(
                                            jugador.id
                                        )
                                }
                            )
                        }
                    }

                    // ------------------------------------------------ Publicar

                    Surface(
                        shadowElevation = 8.dp,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                    ) {

                        Column(
                            modifier =
                                Modifier.padding(
                                    16.dp
                                )
                        ) {

                            Button(
                                onClick =
                                    viewModel::publicarConvocatoria,

                                enabled =
                                    state
                                        .seleccionados
                                        .isNotEmpty() &&
                                            !state.publicando,

                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                            ) {

                                if (state.publicando) {

                                    CircularProgressIndicator()

                                } else {

                                    Text(
                                        text =
                                            "Publicar convocatoria"
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
private fun JugadorConvocatoriaItem(
    jugador: JugadorPlantel,
    seleccionado: Boolean,
    onCambiar: () -> Unit,
) {

    Surface(
        shape =
            RoundedCornerShape(
                16.dp
            ),

        tonalElevation =
            if (seleccionado) {
                3.dp
            } else {
                1.dp
            },

        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onCambiar()
                }
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 12.dp,
                    ),

            verticalAlignment =
                Alignment.CenterVertically,
        ) {

            Checkbox(
                checked =
                    seleccionado,

                onCheckedChange = {
                    onCambiar()
                }
            )

            Column(
                modifier =
                    Modifier
                        .weight(1f)
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
                            .bodyLarge,

                    fontWeight =
                        FontWeight.SemiBold
                )

                Text(
                    text =
                        "Jugador del plantel",

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