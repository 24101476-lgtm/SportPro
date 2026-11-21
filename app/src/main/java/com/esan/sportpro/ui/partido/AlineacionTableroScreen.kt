package com.esan.sportpro.ui.partido

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
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
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val sinArquero = titulares.isNotEmpty() && titulares.none { abreviaturaPosicion(it.posicion) == "POR" }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (esEntrenador) "Armar equipo" else "Alineación",
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Pizarra táctica",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (esEntrenador) {
                Surface(tonalElevation = 4.dp, shadowElevation = 8.dp) {
                    Button(
                        onClick = onGuardar,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentPadding = PaddingValues(vertical = 14.dp),
                    ) {
                        Text(text = textoAccion, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ResumenEquipo(
                formacion = formacion,
                titulares = titulares.size,
                suplentes = suplentes.size,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            if (esEntrenador) {
                Formaciones(
                    seleccionada = formacion,
                    onSeleccionar = { nueva ->
                        formacion = nueva
                        if (nueva != FORMACION_PERSONALIZADA) {
                            distribuirEnFormacion(nueva, titulares).forEach { jugador ->
                                onPosicionJugadorChange(jugador.id, jugador.x, jugador.y)
                            }
                        }
                    },
                )
            } else {
                Text(
                    text = "Alineación definida por el entrenador. Toca un jugador para ver sus datos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            if (esEntrenador && sinArquero) {
                AvisoAlineacion(
                    texto = "Ningún titular es arquero. Pon a un jugador en el arco antes de crear el partido.",
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            CanchaTactica(
                titulares = titulares,
                suplentes = suplentes,
                editable = esEntrenador,
                modifier = Modifier.padding(horizontal = 12.dp),
                onJugadorClick = { jugadorSeleccionado = it },
                onPosicionJugadorChange = { jugadorId, x, y ->
                    if (esEntrenador) formacion = FORMACION_PERSONALIZADA
                    onPosicionJugadorChange(jugadorId, x, y)
                },
                // El intercambio respeta la formación: no la marca como personalizada.
                onIntercambiarJugadores = { arrastrado, destino ->
                    onPosicionJugadorChange(arrastrado.id, arrastrado.x, arrastrado.y)
                    onPosicionJugadorChange(destino.id, destino.x, destino.y)
                    scope.launch {
                        snackbar.currentSnackbarData?.dismiss()
                        snackbar.showSnackbar(
                            message = "${nombrePila(arrastrado.nombre)} ⇄ ${nombrePila(destino.nombre)}",
                            duration = SnackbarDuration.Short,
                        )
                    }
                },
            )
        }
    }

    jugadorSeleccionado?.let { jugador ->
        val detalle = obtenerDetalleJugador(jugador.id)

        AlertDialog(
            onDismissRequest = { jugadorSeleccionado = null },
            icon = { FotoJugadorGrande(jugador) },
            title = {
                Text(
                    text = detalle?.nombreCompleto ?: jugador.nombre,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DatoJugador("Dorsal", (detalle?.dorsal ?: jugador.dorsal)?.toString())
                    DatoJugador("Edad", detalle?.edad?.let { "$it años" })
                    DatoJugador("Posición", detalle?.posicion ?: jugador.posicion)
                    DatoJugador("Categoría", detalle?.categoria)
                    DatoJugador("Pie dominante", detalle?.pieDominante)
                    DatoJugador("Rol", if (jugador.titular) "Titular" else "Suplente")
                }
            },
            confirmButton = {
                Button(onClick = { jugadorSeleccionado = null }) { Text("Cerrar") }
            },
        )
    }
}

@Composable
private fun AvisoAlineacion(texto: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

private fun nombrePila(nombre: String): String =
    nombre.trim().substringBefore(" ").replaceFirstChar { it.uppercase() }

/** Franja con la formación activa y cuántos jugadores hay en cancha y en el banco. */
@Composable
private fun ResumenEquipo(
    formacion: String,
    titulares: Int,
    suplentes: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DatoResumen("Formación", formacion, Modifier.weight(1.3f))
        DatoResumen("Titulares", "$titulares/11", Modifier.weight(1f), alerta = titulares != 11)
        DatoResumen("Banco", suplentes.toString(), Modifier.weight(1f))
    }
}

@Composable
private fun DatoResumen(
    etiqueta: String,
    valor: String,
    modifier: Modifier = Modifier,
    alerta: Boolean = false,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = etiqueta.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (alerta) {
                Box(
                    Modifier
                        .padding(end = 6.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                )
            }
            Text(
                text = valor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun Formaciones(
    seleccionada: String,
    onSeleccionar: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        (FORMACIONES_DISPONIBLES + FORMACION_PERSONALIZADA).forEach { opcion ->
            FilterChip(
                selected = seleccionada == opcion,
                onClick = { onSeleccionar(opcion) },
                label = {
                    Text(
                        text = opcion,
                        fontWeight = if (seleccionada == opcion) FontWeight.Bold else FontWeight.Normal,
                    )
                },
            )
        }
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = valor,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
