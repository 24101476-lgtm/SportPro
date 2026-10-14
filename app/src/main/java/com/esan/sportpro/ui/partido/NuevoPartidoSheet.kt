package com.esan.sportpro.ui.partido

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.esan.sportpro.domain.jugadores.Jugador
import com.esan.sportpro.domain.partido.TiposPartido

/**
 * Programación rápida de un partido con un equipo real de la academia: equipo, rival, tipo,
 * duración, cambios y convocatoria (titulares / suplentes) a partir de la colección `jugadores`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevoPartidoSheet(state: PartidosUiState, form: FormularioPartido, viewModel: PartidosViewModel) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val jugadores = state.jugadoresDelEquipo

    ModalBottomSheet(onDismissRequest = viewModel::cerrarFormulario, sheetState = sheetState) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            item { Text("Nuevo partido", style = MaterialTheme.typography.titleLarge) }

            item {
                Titulo("Equipo")
                if (state.equiposActivos.isEmpty()) {
                    Text(
                        "Tu academia aún no tiene equipos activos. El administrador puede crearlos " +
                            "en la pestaña Academia.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    FilaChips {
                        state.equiposActivos.forEach { e ->
                            Chip(e.etiqueta, form.equipoId == e.id) { viewModel.onEquipo(e.id) }
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = form.rival,
                    onValueChange = viewModel::onRival,
                    label = { Text("Rival") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                Titulo("Tipo de partido")
                FilaChips {
                    TiposPartido.todos.forEach { (valor, etiqueta) ->
                        Chip(etiqueta, form.tipo == valor) { viewModel.onTipo(valor) }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = form.duracionTexto,
                        onValueChange = viewModel::onDuracion,
                        label = { Text("Min. por tiempo") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = form.cambiosTexto,
                        onValueChange = viewModel::onCambios,
                        label = { Text("Cambios") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Titulo("Convocatoria")
                        Text(
                            "Toca un jugador para cambiar: titular → suplente → no convocado",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Etiqueta("${form.titulares}/11 titulares", if (form.titulares == 11) ColoresPartido.Verde else ColoresPartido.Naranja)
                }
                if (form.titulares in 1..10) {
                    Text(
                        "Puedes jugar con menos de 11 titulares (fútbol reducido o categorías menores).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (form.equipoId != null && jugadores.isEmpty()) {
                item {
                    Text(
                        "Este equipo no tiene jugadores activos. Regístralos en la pestaña Jugadores.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            items(jugadores, key = { it.id }) { j ->
                FilaJugador(j, form.convocatoria[j.id] ?: Convocatoria.NO_CONVOCADO) { viewModel.alternarJugador(j.id) }
            }

            form.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 4.dp)) {
                    OutlinedButton(
                        onClick = viewModel::cerrarFormulario,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    ) { Text("Cancelar") }
                    Button(
                        onClick = viewModel::crearPartido,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ColoresPartido.Verde),
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    ) { Text("Crear partido") }
                }
            }
        }
    }
}

@Composable
private fun FilaJugador(jugador: Jugador, condicion: Convocatoria, onClick: () -> Unit) {
    val (texto, color) = when (condicion) {
        Convocatoria.TITULAR -> "Titular" to ColoresPartido.Verde
        Convocatoria.SUPLENTE -> "Suplente" to ColoresPartido.Azul
        Convocatoria.NO_CONVOCADO -> "No convocado" to ColoresPartido.Gris
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = if (condicion == Convocatoria.NO_CONVOCADO) 0.06f else 0.12f),
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(34.dp).background(color, CircleShape),
            ) {
                Text("${jugador.dorsal ?: "–"}", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(jugador.nombreCompleto, fontWeight = FontWeight.SemiBold)
                if (jugador.posicion.isNotBlank()) {
                    Text(jugador.posicion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Etiqueta(texto, color)
        }
    }
}

@Composable
private fun Titulo(texto: String) {
    Text(texto, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
}

@Composable
private fun FilaChips(contenido: @Composable () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    ) { contenido() }
}

@Composable
private fun Chip(texto: String, seleccionado: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = seleccionado,
        onClick = onClick,
        label = { Text(texto) },
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = ColoresPartido.Verde,
            selectedLabelColor = Color.White,
        ),
        modifier = Modifier.heightIn(min = 48.dp),
    )
}
