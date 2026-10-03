package com.esan.sportpro.ui.partido

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.partido.EstadoPartido
import com.esan.sportpro.domain.partido.Partido

@Composable
fun PartidosListaScreen(
    onAbrirPartido: (String) -> Unit,
    viewModel: PartidosViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.revisarPartidoEnCurso()
    }
    LaunchedEffect(state.abrirPartidoId) {
        state.abrirPartidoId?.let { id ->
            viewModel.navegacionRealizada()
            onAbrirPartido(id)
        }
    }
    LaunchedEffect(state.mensaje) {
        state.mensaje?.let {
            viewModel.mensajeMostrado()
            snackbar.showSnackbar(it)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when {
            state.cargando -> Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(color = ColoresPartido.Verde)
                Text("Conectando con Firestore…", modifier = Modifier.padding(top = 12.dp))
            }

            state.error != null -> Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp),
                )
                Text("No se pudieron cargar los partidos", style = MaterialTheme.typography.titleMedium)
                Text(state.error.orEmpty(), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                Button(onClick = viewModel::escuchar) { Text("Reintentar") }
            }

            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Encabezado(state.partidos) }
                state.aviso?.let { aviso ->
                    item {
                        Text(aviso, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (state.partidos.isEmpty()) {
                    item { EstadoVacio() }
                }
                items(state.partidos, key = { it.id }) { partido ->
                    PartidoCard(partido = partido, onClick = { onAbrirPartido(partido.id) })
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { viewModel.mostrarCrear(true) },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Partido de práctica", fontWeight = FontWeight.Bold) },
            containerColor = ColoresPartido.Verde,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        )
        SnackbarHost(
            snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp),
        )
    }

    if (state.mostrarCrear) {
        CrearPartidoDialog(
            onCrear = viewModel::crearPartidoPractica,
            onCancelar = { viewModel.mostrarCrear(false) },
        )
    }
}

@Composable
private fun Encabezado(partidos: List<Partido>) {
    val enVivo = partidos.count { it.estado == EstadoPartido.EN_CURSO }
    Surface(
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Brush.linearGradient(listOf(ColoresPartido.VerdeOscuro, ColoresPartido.Verde)))
                .padding(20.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Partidos", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(
                    when {
                        enVivo > 0 -> "$enVivo partido(s) en vivo ahora"
                        partidos.isEmpty() -> "Registra eventos en vivo desde la cancha"
                        else -> "${partidos.size} partido(s) registrados"
                    },
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.18f), modifier = Modifier.size(56.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.SportsSoccer, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

@Composable
private fun EstadoVacio() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, start = 24.dp, end = 24.dp),
    ) {
        Surface(shape = CircleShape, color = ColoresPartido.Verde.copy(alpha = 0.12f), modifier = Modifier.size(96.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.SportsSoccer, contentDescription = null, tint = ColoresPartido.Verde, modifier = Modifier.size(52.dp))
            }
        }
        Text("Aún no hay partidos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
        Text(
            "Crea un partido de práctica para probar el registro en vivo.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PartidoCard(partido: Partido, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    listOfNotNull(partido.categoria, partido.fechaMs?.let { formatearFecha(it) }).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                EstadoPartidoChip(partido)
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                Escudo(partido.nombreEquipo, ColoresPartido.Azul, 40.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    partido.nombreEquipo,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text("vs", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp))
                Text(
                    partido.nombreRival,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(10.dp))
                Escudo(partido.nombreRival, ColoresPartido.Rojo, 40.dp)
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CrearPartidoDialog(
    onCrear: (String, String, Int, Int) -> Unit,
    onCancelar: () -> Unit,
) {
    var equipo by remember { mutableStateOf("") }
    var rival by remember { mutableStateOf("") }
    var cambios by remember { mutableStateOf("5") }
    var duracion by remember { mutableStateOf("45") }

    AlertDialog(
        onDismissRequest = onCancelar,
        icon = { Icon(Icons.Default.SportsSoccer, contentDescription = null, tint = ColoresPartido.Verde) },
        title = { Text("Nuevo partido de práctica") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Se crea con un plantel de ejemplo (11 titulares y 5 suplentes) y la alineación confirmada.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = equipo,
                    onValueChange = { equipo = it.take(40) },
                    label = { Text("Tu equipo") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = rival,
                    onValueChange = { rival = it.take(40) },
                    label = { Text("Rival") },
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = cambios,
                        onValueChange = { cambios = it.filter(Char::isDigit).take(2) },
                        label = { Text("Cambios") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = duracion,
                        onValueChange = { duracion = it.filter(Char::isDigit).take(2) },
                        label = { Text("Min. por tiempo") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    "El partido termina solo al cumplirse 2 × ${duracion.toIntOrNull() ?: 45} minutos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = { onCrear(equipo, rival, cambios.toIntOrNull() ?: 5, duracion.toIntOrNull() ?: 45) }) {
                Text("Crear")
            }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    )
}
