package com.esan.sportpro.ui.partido

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.partido.JugadorConvocado


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlineacionPosicionesScreen(
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

    val titulares =
        state.titulares

    val todosConPosicion =
        titulares.isNotEmpty() &&
                titulares.all {
                    !it.posicion.isNullOrBlank()
                }

    Scaffold(

        snackbarHost = {
            SnackbarHost(
                hostState = snackbar
            )
        },

        topBar = {

            TopAppBar(

                title = {
                    Text(
                        "Posiciones y dorsales"
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
                        "US-016 · POSICIONES",
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
                        "Completa la formación titular",
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
                        "Asigna una posición a cada titular. También puedes definir su dorsal antes de confirmar la alineación.",
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

                    color =
                        MaterialTheme
                            .colorScheme
                            .secondaryContainer,
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
                                    "${titulares.size} titulares",
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleMedium,
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(
                                text =
                                    "${titulares.count { !it.posicion.isNullOrBlank() }} con posición asignada",
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
            // TITULARES
            // -----------------------------------------

            items(
                items =
                    titulares,

                key = {
                        jugador ->
                    jugador.id
                }
            ) {
                    jugador ->

                JugadorPosicionItem(
                    jugador =
                        jugador,

                    onPosicionCambiada = {
                            posicion ->

                        viewModel
                            .cambiarPosicion(
                                jugadorId =
                                    jugador.id,
                                posicion =
                                    posicion,
                            )
                    },

                    onDorsalCambiado = {
                            dorsal ->

                        viewModel
                            .cambiarDorsal(
                                jugadorId =
                                    jugador.id,
                                dorsal =
                                    dorsal,
                            )
                    }
                )
            }


            // -----------------------------------------
            // CONTINUAR
            // -----------------------------------------

            item {

                Button(
                    onClick =
                        onContinuar,

                    enabled =
                        titulares.size == 11 &&
                                todosConPosicion,

                    modifier =
                        Modifier
                            .fillMaxWidth()
                ) {

                    Text(
                        "Revisar alineación"
                    )
                }
            }
        }
    }
}


@Composable
private fun JugadorPosicionItem(
    jugador: JugadorConvocado,
    onPosicionCambiada: (String) -> Unit,
    onDorsalCambiado: (Int?) -> Unit,
) {

    var menuAbierto by
    remember {
        mutableStateOf(false)
    }

    var dorsalTexto by
    remember(
        jugador.id,
        jugador.dorsal
    ) {
        mutableStateOf(
            jugador.dorsal
                ?.toString()
                .orEmpty()
        )
    }

    val posiciones =
        listOf(
            "POR",
            "DEF",
            "MED",
            "DEL",
        )

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
                        16.dp
                    ),

            verticalArrangement =
                Arrangement.spacedBy(
                    12.dp
                )
        ) {

            // -------------------------------------
            // JUGADOR
            // -------------------------------------

            Row(
                verticalAlignment =
                    Alignment.CenterVertically,

                horizontalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Badge,
                    contentDescription =
                        null,
                    tint =
                        MaterialTheme
                            .colorScheme
                            .primary
                )

                Column {

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
                            "Titular",
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
            }


            // -------------------------------------
            // POSICIÓN
            // -------------------------------------

            Column {

                Text(
                    text =
                        "Posición",
                    style =
                        MaterialTheme
                            .typography
                            .labelMedium
                )

                OutlinedButton(
                    onClick = {
                        menuAbierto = true
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                ) {

                    Text(
                        text =
                            jugador.posicion
                                ?: "Seleccionar posición"
                    )
                }

                DropdownMenu(
                    expanded =
                        menuAbierto,

                    onDismissRequest = {
                        menuAbierto = false
                    }
                ) {

                    posiciones.forEach {
                            posicion ->

                        DropdownMenuItem(

                            text = {
                                Text(
                                    when (
                                        posicion
                                    ) {

                                        "POR" ->
                                            "POR · Portero"

                                        "DEF" ->
                                            "DEF · Defensa"

                                        "MED" ->
                                            "MED · Mediocampista"

                                        "DEL" ->
                                            "DEL · Delantero"

                                        else ->
                                            posicion
                                    }
                                )
                            },

                            onClick = {

                                onPosicionCambiada(
                                    posicion
                                )

                                menuAbierto =
                                    false
                            }
                        )
                    }
                }
            }


            // -------------------------------------
            // DORSAL
            // -------------------------------------

            OutlinedTextField(
                value =
                    dorsalTexto,

                onValueChange = {
                        nuevoValor ->

                    val soloNumeros =
                        nuevoValor
                            .filter {
                                it.isDigit()
                            }
                            .take(2)

                    dorsalTexto =
                        soloNumeros

                    onDorsalCambiado(
                        soloNumeros
                            .toIntOrNull()
                    )
                },

                label = {
                    Text(
                        "Dorsal"
                    )
                },

                placeholder = {
                    Text(
                        "Ej. 10"
                    )
                },

                singleLine = true,

                keyboardOptions =
                    KeyboardOptions(
                        keyboardType =
                            KeyboardType.Number
                    ),

                modifier =
                    Modifier
                        .fillMaxWidth()
            )
        }
    }
}