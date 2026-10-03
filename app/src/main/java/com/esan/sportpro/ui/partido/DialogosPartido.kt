package com.esan.sportpro.ui.partido

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.esan.sportpro.domain.partido.EquipoEvento
import com.esan.sportpro.domain.partido.EstadoEvento
import com.esan.sportpro.domain.partido.EventoPartido
import com.esan.sportpro.domain.partido.JugadorConvocado
import com.esan.sportpro.domain.partido.JugadorEspecial
import com.esan.sportpro.domain.partido.ReglasPartido

@Composable
fun DialogosPartido(state: RegistroUiState, viewModel: RegistroPartidoViewModel) {
    when (val d = state.dialogo) {
        null -> Unit

        is DialogoPartido.ExcesoCambios -> AlertDialog(
            onDismissRequest = viewModel::cerrarDialogo,
            title = { Text("Superaste el número de cambios permitidos") },
            text = {
                Text(
                    "Ya se registraron ${ReglasPartido.cambiosRealizados(state.eventos)} de " +
                        "${state.partido?.cambiosPermitidos ?: 0} cambios permitidos. " +
                        "Si confirmas, el cambio quedará marcado como excedido.",
                )
            },
            confirmButton = { TextButton(onClick = viewModel::confirmarExcesoCambios) { Text("Registrar igual") } },
            dismissButton = { TextButton(onClick = viewModel::cerrarDialogo) { Text("Cancelar") } },
        )

        is DialogoPartido.FaltaFinPrimerTiempo -> AlertDialog(
            onDismissRequest = viewModel::cerrarDialogo,
            title = { Text("Falta registrar el fin del primer tiempo") },
            text = {
                Text(
                    "¿Deseas insertarlo con el minuto estimado " +
                        "(${minOf(state.partido?.duracionTiempoMin ?: 45, d.borrador.minuto)}') antes del inicio del segundo tiempo?",
                )
            },
            confirmButton = { TextButton(onClick = viewModel::insertarFinPrimerTiempo) { Text("Insertar y continuar") } },
            dismissButton = { TextButton(onClick = viewModel::continuarSinFinPrimerTiempo) { Text("Continuar sin insertar") } },
        )

        is DialogoPartido.Anular -> AlertDialog(
            onDismissRequest = viewModel::cerrarDialogo,
            title = { Text("Anular evento") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${d.evento.minuto}' · ${d.evento.tipoNombre}", fontWeight = FontWeight.SemiBold)
                    Text(
                        "El evento seguirá visible en la cronología como Anulado y no contará en el marcador, " +
                            "las estadísticas ni el resumen del partido.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedTextField(
                        value = d.motivo,
                        onValueChange = viewModel::onMotivoAnulacion,
                        label = { Text("Motivo (obligatorio)") },
                        isError = d.error != null,
                        supportingText = { Text(d.error ?: "${d.motivo.trim().length}/${ReglasPartido.MOTIVO_MAX}") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = { TextButton(onClick = viewModel::confirmarAnulacion) { Text("Anular") } },
            dismissButton = { TextButton(onClick = viewModel::cerrarDialogo) { Text("Cancelar") } },
        )

        is DialogoPartido.Historial -> {
            val versiones = state.eventos.filter { it.grupoId == d.grupoId }.sortedBy { it.version }
            AlertDialog(
                onDismissRequest = viewModel::cerrarDialogo,
                title = { Text("Historial del evento") },
                text = {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(versiones, key = { it.id }) { v -> VersionItem(v, state) }
                    }
                },
                confirmButton = { TextButton(onClick = viewModel::cerrarDialogo) { Text("Cerrar") } },
            )
        }

        is DialogoPartido.CerrarActa -> AlertDialog(
            onDismissRequest = viewModel::cerrarDialogo,
            title = { Text("Cerrar acta") },
            text = {
                Text(
                    "Se cerrará el acta con ${d.incompletos} eventos incompletos. " +
                        "Esta decisión quedará registrada y ya no se podrán corregir eventos.",
                )
            },
            confirmButton = { TextButton(onClick = viewModel::confirmarCierreActa) { Text("Cerrar acta") } },
            dismissButton = { TextButton(onClick = viewModel::cerrarDialogo) { Text("Volver") } },
        )

        DialogoPartido.Duplicados -> AlertDialog(
            onDismissRequest = viewModel::cerrarDialogo,
            title = { Text("Posibles eventos duplicados") },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Text(
                            "Tienen el mismo tipo, minuto, equipo y jugador. Anula el que no corresponda.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    items(state.duplicados) { grupo ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                grupo.forEach { e ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${e.minuto}' · ${e.tipoNombre}", fontWeight = FontWeight.SemiBold)
                                            Text(
                                                listOfNotNull(
                                                    detalleEvento(e, state.partido),
                                                    e.operadorNombre?.let { "Registró: $it" },
                                                    e.registradoEnMs?.let { formatearFechaHora(it) },
                                                ).joinToString(" · "),
                                                style = MaterialTheme.typography.bodyMedium,
                                            )
                                        }
                                        if (state.puedeCorregir) {
                                            TextButton(onClick = { viewModel.pedirAnulacion(e) }) { Text("Anular") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = viewModel::cerrarDialogo) { Text("Cerrar") } },
        )

        DialogoPartido.Pendientes -> HojaPendientes(state, viewModel)
    }
}

@Composable
private fun VersionItem(v: EventoPartido, state: RegistroUiState) {
    val estado = when (v.estado) {
        EstadoEvento.VIGENTE -> "Vigente"
        EstadoEvento.REEMPLAZADO -> "Reemplazado"
        EstadoEvento.ANULADO -> "Anulado"
    }
    Column {
        Text("Versión ${v.version} · $estado", fontWeight = FontWeight.SemiBold)
        Text(
            "${v.minuto}' · ${detalleEvento(v, state.partido).ifBlank { v.tipoNombre }}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "Registrado por ${v.operadorNombre ?: v.operadorUid ?: SIN_INFO}" +
                (v.registradoEnMs?.let { " · ${formatearFechaHora(it)}" } ?: ""),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (v.camposModificados.isNotEmpty()) {
            Text("Campos modificados: ${v.camposModificados.joinToString()}", style = MaterialTheme.typography.bodyMedium)
        }
        if (v.estado == EstadoEvento.ANULADO) {
            Text(
                "Anulado por ${v.anuladoPorNombre ?: v.anuladoPor ?: SIN_INFO}" +
                    (v.anuladoEnMs?.let { " · ${formatearFechaHora(it)}" } ?: ""),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        v.motivo?.let { Text("Motivo: $it", style = MaterialTheme.typography.bodyMedium) }
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

/** Lista de eventos incompletos para asignar el jugador faltante. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun HojaPendientes(state: RegistroUiState, viewModel: RegistroPartidoViewModel) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var expandido by remember { mutableStateOf<String?>(null) }
    val pendientes = state.incompletos

    ModalBottomSheet(onDismissRequest = viewModel::cerrarDialogo, sheetState = sheetState) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Text("Completar pendientes", style = MaterialTheme.typography.titleMedium) }
            if (pendientes.isEmpty()) {
                item { Text("No hay eventos incompletos.") }
            }
            items(pendientes, key = { it.id }) { e ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${e.minuto}' · ${e.tipoNombre}", fontWeight = FontWeight.SemiBold)
                        Text(detalleEvento(e, state.partido), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Observación: ${e.observacion ?: SIN_INFO}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (expandido == e.id) {
                            val faltaPrincipal = e.jugadorId == null || e.jugadorId == JugadorEspecial.NO_IDENTIFICADO
                            Text(
                                if (faltaPrincipal) "Selecciona el jugador" else "Selecciona el jugador que entra",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            val candidatos = if (e.equipo == EquipoEvento.RIVAL) {
                                listOf(
                                    JugadorConvocado(
                                        id = JugadorEspecial.RIVAL,
                                        nombre = JugadorEspecial.NOMBRE_RIVAL,
                                        dorsal = null,
                                        titular = false,
                                    ),
                                )
                            } else {
                                state.convocados
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                candidatos.forEach { j ->
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            viewModel.completarPendiente(e, j)
                                            expandido = null
                                        },
                                        label = { Text(j.etiqueta) },
                                        modifier = Modifier.heightIn(min = 48.dp),
                                    )
                                }
                            }
                        } else {
                            TextButton(onClick = { expandido = e.id }) { Text("Asignar jugador") }
                        }
                    }
                }
            }
        }
    }
}
