package com.esan.sportpro.ui.partido

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.esan.sportpro.domain.partido.EquipoEvento
import com.esan.sportpro.domain.partido.EstadoEvento
import com.esan.sportpro.domain.partido.EventoPartido
import com.esan.sportpro.domain.partido.JugadorConvocado
import com.esan.sportpro.domain.partido.JugadorEspecial
import com.esan.sportpro.domain.partido.ReglasPartido
import com.esan.sportpro.domain.partido.ResultadoPenal
import com.esan.sportpro.domain.partido.TiposEvento

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
                    Text("${minutoEvento(d.evento, state.eventos, state.partido)} · ${d.evento.tipoNombre}", fontWeight = FontWeight.SemiBold)
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

        is DialogoPartido.Historial -> HistorialEvento(
            versiones = state.eventos.filter { it.grupoId == d.grupoId }.sortedByDescending { it.version },
            state = state,
            onCerrar = viewModel::cerrarDialogo,
        )

        is DialogoPartido.CerrarActa -> AlertDialog(
            onDismissRequest = viewModel::cerrarDialogo,
            title = { Text("Cerrar acta") },
            text = {
                Text(
                    "Se cerrará el acta con ${cantidad(d.incompletos, "evento incompleto", "eventos incompletos")}. " +
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
                                            Text("${minutoEvento(e, state.eventos, state.partido)} · ${e.tipoNombre}", fontWeight = FontWeight.SemiBold)
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

/**
 * US-018 · Ver historial: todas las versiones del evento, de la más reciente a la original, con
 * quién y cuándo las registró, qué campos cambiaron (valor anterior → nuevo) y el motivo.
 */
@Composable
private fun HistorialEvento(versiones: List<EventoPartido>, state: RegistroUiState, onCerrar: () -> Unit) {
    val actual = versiones.firstOrNull() ?: return
    val porVersion = versiones.associateBy { it.version }
    Dialog(onDismissRequest = onCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 620.dp),
        ) {
            Column {
                // Cabecera con el evento tal como está hoy.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorTipo(actual.tipoId).copy(alpha = 0.10f))
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                ) {
                    IconoEvento(actual.tipoId, 44.dp, apagado = actual.estado == EstadoEvento.ANULADO)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Historial del evento", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "${minutoEvento(actual, state.eventos, state.partido)} · ${actual.tipoNombre}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            cantidad(versiones.size, "versión", "versiones"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
                ) {
                    items(versiones, key = { it.id }) { v ->
                        VersionItem(
                            v = v,
                            anterior = porVersion[v.version - 1],
                            state = state,
                            esUltima = v.version == versiones.minOf { it.version },
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    TextButton(onClick = onCerrar) { Text("Cerrar") }
                }
            }
        }
    }
}

/** Una versión del evento dentro de la línea de tiempo del historial. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VersionItem(v: EventoPartido, anterior: EventoPartido?, state: RegistroUiState, esUltima: Boolean) {
    val (estado, color) = when (v.estado) {
        EstadoEvento.VIGENTE -> "Vigente" to ColoresPartido.Verde
        EstadoEvento.REEMPLAZADO -> "Reemplazado" to ColoresPartido.Gris
        EstadoEvento.ANULADO -> "Anulado" to ColoresPartido.Rojo
    }
    val lineaColor = MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                if (!esUltima) {
                    val x = 14.dp.toPx()
                    drawLine(lineaColor, Offset(x, 30.dp.toPx()), Offset(x, size.height), strokeWidth = 2.dp.toPx())
                }
            },
    ) {
        // Marca de la versión en la línea de tiempo.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .background(color, CircleShape),
        ) {
            Text("v${v.version}", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (v.version == 1) "Registro original" else "Corrección",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Etiqueta(estado, color)
            }
            Text(
                "${minutoEvento(v, state.eventos, state.partido)} · ${detalleEvento(v, state.partido).ifBlank { v.tipoNombre }}",
                style = MaterialTheme.typography.bodyMedium,
            )
            DatoHistorial(
                Icons.Default.Person,
                (if (v.version == 1) "Registró " else "Corrigió ") + (v.operadorNombre ?: v.operadorUid ?: SIN_INFO),
            )
            DatoHistorial(Icons.Default.Schedule, v.registradoEnMs?.let { formatearFechaHora(it) } ?: "Pendiente de sincronizar")

            // Qué cambió respecto de la versión anterior.
            if (v.camposModificados.isNotEmpty()) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Cambios", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        v.camposModificados.forEach { campo ->
                            val antes = anterior?.let { valorCampo(campo, it, state) }
                            val despues = valorCampo(campo, v, state)
                            Text(
                                if (antes != null) "${nombreCampo(campo)}: $antes → $despues" else "${nombreCampo(campo)}: $despues",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
            if (v.motivo != null) {
                Text(
                    "“${v.motivo}”",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (v.estado == EstadoEvento.REEMPLAZADO) {
                DatoHistorial(
                    Icons.Default.History,
                    "Reemplazada" + (v.reemplazadoEnMs?.let { " el ${formatearFechaHora(it)}" } ?: ""),
                )
            }
            if (v.estado == EstadoEvento.ANULADO) {
                DatoHistorial(
                    Icons.Default.Block,
                    "Anuló ${v.anuladoPorNombre ?: v.anuladoPor ?: SIN_INFO}" +
                        (v.anuladoEnMs?.let { " · ${formatearFechaHora(it)}" } ?: ""),
                    color = ColoresPartido.Rojo,
                )
            }
            if (v.fueraDeOrden && v.tipoId != TiposEvento.INICIO_SEGUNDO_TIEMPO) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) { Etiqueta("Fuera de orden", ColoresPartido.Morado) }
            }
        }
    }
}

@Composable
private fun DatoHistorial(icono: ImageVector, texto: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(texto, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

private fun nombreCampo(campo: String): String = when (campo) {
    "minuto" -> "Minuto"
    "equipo" -> "Equipo"
    "jugador" -> "Jugador"
    "jugadorSecundario" -> "Jugador secundario"
    "resultado" -> "Resultado"
    "observacion" -> "Observación"
    else -> campo
}

private fun valorCampo(campo: String, e: EventoPartido, state: RegistroUiState): String = when (campo) {
    "minuto" -> minutoEvento(e, state.eventos, state.partido)
    "equipo" -> when (e.equipo) {
        EquipoEvento.PROPIO -> state.partido?.nombreEquipo ?: "Propio"
        EquipoEvento.RIVAL -> state.partido?.nombreRival ?: "Rival"
        null -> SIN_INFO
    }
    "jugador" -> e.jugadorNombre ?: e.jugadorId?.let { nombreEspecial(it) } ?: SIN_INFO
    "jugadorSecundario" -> e.jugadorSecundarioNombre ?: e.jugadorSecundarioId?.let { nombreEspecial(it) } ?: SIN_INFO
    "resultado" -> when (e.resultadoPenal) {
        ResultadoPenal.CONVERTIDO -> "Convertido"
        ResultadoPenal.FALLADO -> "Fallado"
        null -> SIN_INFO
    }
    "observacion" -> e.observacion ?: SIN_INFO
    else -> SIN_INFO
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
                        Text("${minutoEvento(e, state.eventos, state.partido)} · ${e.tipoNombre}", fontWeight = FontWeight.SemiBold)
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
