package com.esan.sportpro.ui.partido

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class DetalleJugadorUi(
    val id: String,
    val nombreCompleto: String,
    val dorsal: Int?,
    val edad: Int?,
    val posicion: String?,
    val categoria: String?,
    val pieDominante: String?,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlineacionTableroScreen(
    titulares: List<JugadorCanchaUi>,
    suplentes: List<JugadorCanchaUi>,
    esEntrenador: Boolean,
    onVolver: () -> Unit,
    onGuardar: () -> Unit,
    textoAccion: String = "Guardar alineación",
    onPosicionJugadorChange: (jugadorId: String, x: Float, y: Float) -> Unit,
    obtenerDetalleJugador: (String) -> DetalleJugadorUi?,
    modifier: Modifier = Modifier,
) {
    var formacion by remember { mutableStateOf("4-3-3") }
    var jugadorSeleccionado by remember { mutableStateOf<JugadorCanchaUi?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (esEntrenador) "Armar equipo"
                        else "Alineación"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (esEntrenador) {
                Surface(
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp,
                ) {
                    Button(
                        onClick = onGuardar,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 16.dp,
                                vertical = 10.dp,
                            ),
                        contentPadding = PaddingValues(vertical = 14.dp),
                    ) {
                        Text(
                            text = textoAccion,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        },
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (esEntrenador) {
                        "Pizarra táctica"
                    } else {
                        "Formación del equipo"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (esEntrenador) {
                        "Ubica a los 11 titulares en el campo. Puedes arrastrar cada ficha."
                    } else {
                        "Esta es la alineación definida por el entrenador. Toca un jugador para ver sus datos."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (esEntrenador) {
                Formaciones(
                    seleccionada = formacion,
                    onSeleccionar = { nuevaFormacion ->

                        formacion = nuevaFormacion

                        if (nuevaFormacion != "Personalizada") {
                            posicionesFormacion(
                                formacion = nuevaFormacion,
                                jugadores = titulares,
                            ).forEach { jugador ->
                                onPosicionJugadorChange(
                                    jugador.id,
                                    jugador.x,
                                    jugador.y,
                                )
                            }
                        }
                    }
                )
            }

            CanchaTactica(
                titulares = titulares,
                suplentes = suplentes,
                editable = esEntrenador,
                modifier = Modifier.padding(horizontal = 12.dp),
                onJugadorClick = {
                    jugadorSeleccionado = it
                },
                onPosicionJugadorChange = { jugadorId, x, y ->
                    if (esEntrenador) {
                        formacion = "Personalizada"
                    }

                    onPosicionJugadorChange(
                        jugadorId,
                        x,
                        y,
                    )
                }
            )

        }
    }

    jugadorSeleccionado?.let { jugador ->
        val detalle = obtenerDetalleJugador(jugador.id)

        AlertDialog(
            onDismissRequest = {
                jugadorSeleccionado = null
            },
            title = {
                Text(
                    text = detalle?.nombreCompleto ?: jugador.nombre,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DatoJugador(
                        etiqueta = "Dorsal",
                        valor = (detalle?.dorsal ?: jugador.dorsal)?.toString()
                    )

                    DatoJugador(
                        etiqueta = "Edad",
                        valor = detalle?.edad?.let { "$it años" }
                    )

                    DatoJugador(
                        etiqueta = "Posición",
                        valor = detalle?.posicion ?: jugador.posicion
                    )

                    DatoJugador(
                        etiqueta = "Categoría",
                        valor = detalle?.categoria
                    )

                    DatoJugador(
                        etiqueta = "Pie dominante",
                        valor = detalle?.pieDominante
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        jugadorSeleccionado = null
                    }
                ) {
                    Text("Cerrar")
                }
            }
        )
    }
}

@Composable
private fun Formaciones(
    seleccionada: String,
    onSeleccionar: (String) -> Unit,
) {
    val opciones = listOf(
        "4-3-3",
        "4-4-2",
        "4-2-3-1",
        "3-5-2",
        "3-4-3",
        "Personalizada"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        opciones.forEach { opcion ->
            FilterChip(
                selected = seleccionada == opcion,
                onClick = {
                    onSeleccionar(opcion)
                },
                label = {
                    Text(opcion)
                }
            )
        }
    }
}


private fun posicionesFormacion(
    formacion: String,
    jugadores: List<JugadorCanchaUi>,
): List<JugadorCanchaUi> {

    val posiciones =
        when (formacion) {

            "4-3-3" -> listOf(
                0.50f to 0.90f, // POR

                0.14f to 0.72f,
                0.38f to 0.76f,
                0.62f to 0.76f,
                0.86f to 0.72f,

                0.30f to 0.52f,
                0.50f to 0.57f,
                0.70f to 0.52f,

                0.18f to 0.27f,
                0.50f to 0.19f,
                0.82f to 0.27f,
            )

            "4-4-2" -> listOf(
                0.50f to 0.90f, // POR

                0.14f to 0.73f,
                0.38f to 0.76f,
                0.62f to 0.76f,
                0.86f to 0.73f,

                0.16f to 0.50f,
                0.39f to 0.55f,
                0.61f to 0.55f,
                0.84f to 0.50f,

                0.36f to 0.24f,
                0.64f to 0.24f,
            )

            "4-2-3-1" -> listOf(
                0.50f to 0.90f, // POR

                0.14f to 0.74f,
                0.38f to 0.77f,
                0.62f to 0.77f,
                0.86f to 0.74f,

                0.38f to 0.59f,
                0.62f to 0.59f,

                0.18f to 0.39f,
                0.50f to 0.42f,
                0.82f to 0.39f,

                0.50f to 0.19f,
            )

            "3-5-2" -> listOf(
                0.50f to 0.90f, // POR

                0.25f to 0.74f,
                0.50f to 0.78f,
                0.75f to 0.74f,

                0.10f to 0.50f,
                0.30f to 0.55f,
                0.50f to 0.52f,
                0.70f to 0.55f,
                0.90f to 0.50f,

                0.36f to 0.24f,
                0.64f to 0.24f,
            )

            "3-4-3" -> listOf(
                0.50f to 0.90f, // POR

                0.25f to 0.74f,
                0.50f to 0.78f,
                0.75f to 0.74f,

                0.15f to 0.51f,
                0.39f to 0.55f,
                0.61f to 0.55f,
                0.85f to 0.51f,

                0.18f to 0.26f,
                0.50f to 0.19f,
                0.82f to 0.26f,
            )

            else -> return jugadores
        }

    return jugadores
        .take(11)
        .mapIndexed { index, jugador ->

            val (x, y) =
                posiciones[index]

            jugador.copy(
                x = x,
                y = y,
            )
        }
}


@Composable
private fun DatoJugador(
    etiqueta: String,
    valor: String?,
) {
    if (!valor.isNullOrBlank()) {
        Column {
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = valor,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

