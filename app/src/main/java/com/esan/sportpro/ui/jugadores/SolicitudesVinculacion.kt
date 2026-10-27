package com.esan.sportpro.ui.jugadores

import com.esan.sportpro.core.fecha.FechasSportPro
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
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.esan.sportpro.domain.jugadores.EstadoVinculacion
import com.esan.sportpro.domain.jugadores.Jugador
import com.esan.sportpro.domain.jugadores.ReglasVinculacion
import com.esan.sportpro.domain.jugadores.Vinculacion
import com.esan.sportpro.ui.common.CampoFormulario
import com.esan.sportpro.ui.common.EmptyStateMessage
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * US-007 — pantalla "Solicitudes de vinculación" del entrenador y el administrador: aprobar o
 * rechazar (con motivo) las solicitudes pendientes y consultar las ya resueltas.
 */
@Composable
internal fun SolicitudesVinculacionContenido(
    state: JugadoresUiState,
    viewModel: JugadoresViewModel,
    modifier: Modifier = Modifier,
) {
    val historial = state.mostrarHistorialSolicitudes
    val lista = if (historial) state.solicitudesResueltas else state.solicitudesPendientes
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Ver solicitudes resueltas", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Switch(checked = historial, onCheckedChange = viewModel::onMostrarHistorialSolicitudes)
        }
        if (lista.isEmpty()) {
            EmptyStateMessage(
                mensaje = if (historial) "Todavía no hay solicitudes resueltas." else "No hay solicitudes pendientes.",
                icono = Icons.Default.FamilyRestroom,
            )
            return@Column
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(lista, key = { it.id }) { vinculacion ->
                TarjetaSolicitud(
                    vinculacion = vinculacion,
                    apoderadosActuales = state.apoderadosDe(vinculacion.jugadorId).size,
                    procesando = state.procesandoSolicitud == vinculacion.id,
                    puedeRevocar = state.esAdmin,
                    viewModel = viewModel,
                )
            }
        }
    }
}

@Composable
private fun TarjetaSolicitud(
    vinculacion: Vinculacion,
    apoderadosActuales: Int,
    procesando: Boolean,
    puedeRevocar: Boolean,
    viewModel: JugadoresViewModel,
) {
    val formato = FechasSportPro.formatoFechaHora()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(vinculacion.padreNombre.ifBlank { "Apoderado" }, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                EstadoVinculacionChip(vinculacion.estadoVinculacion)
            }
            Text("Solicita vincularse con ${vinculacion.jugadorNombre}", style = MaterialTheme.typography.bodyMedium)
            Text(vinculacion.equipoNombre, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            vinculacion.solicitadaEn?.let {
                Text("Solicitada el ${formato.format(it)}", style = MaterialTheme.typography.labelSmall)
            }

            when (vinculacion.estadoVinculacion) {
                EstadoVinculacion.PENDIENTE -> {
                    Text(
                        "Apoderados aprobados: $apoderadosActuales de ${ReglasVinculacion.MAX_APODERADOS}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (apoderadosActuales >= ReglasVinculacion.MAX_APODERADOS) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.abrirRechazo(vinculacion) },
                            enabled = !procesando,
                            modifier = Modifier.weight(1f),
                        ) { Text("Rechazar") }
                        Button(
                            onClick = { viewModel.aprobar(vinculacion) },
                            enabled = !procesando,
                            modifier = Modifier.weight(1f),
                        ) { Text("Aprobar") }
                    }
                }
                EstadoVinculacion.RECHAZADA -> Text(
                    "Rechazada por ${vinculacion.resueltaPorNombre}: ${vinculacion.motivoRechazo}",
                    style = MaterialTheme.typography.bodySmall,
                )
                EstadoVinculacion.APROBADA -> {
                    Text("Aprobada por ${vinculacion.resueltaPorNombre}", style = MaterialTheme.typography.bodySmall)
                    if (puedeRevocar) {
                        TextButton(onClick = { viewModel.pedirRevocar(vinculacion) }, enabled = !procesando) {
                            Text("Revocar vinculación", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                EstadoVinculacion.REVOCADA -> Text(
                    "Revocada por ${vinculacion.resueltaPorNombre}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

/** Sección del detalle del jugador: código para apoderados y apoderados vinculados. */
@Composable
internal fun SeccionApoderados(
    state: JugadoresUiState,
    jugador: Jugador,
    viewModel: JugadoresViewModel,
) {
    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Apoderados", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        HorizontalDivider()
        if (jugador.codigoVinculacion.isBlank()) {
            Text("Este jugador todavía no tiene código para apoderados.", style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = { viewModel.generarCodigo(jugador) }) { Text("Generar código") }
        } else {
            Text("Código para apoderados", style = MaterialTheme.typography.bodyMedium)
            Text(jugador.codigoVinculacion, style = MaterialTheme.typography.headlineSmall)
            Text(
                "Entrégalo al padre, madre o apoderado para que solicite la vinculación desde su cuenta.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val apoderados = state.apoderadosDe(jugador.id)
        if (apoderados.isEmpty()) {
            Text("Sin apoderados vinculados.", style = MaterialTheme.typography.bodyMedium)
        } else {
            apoderados.forEach { vinculacion ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(vinculacion.padreNombre, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    if (state.esAdmin) {
                        TextButton(onClick = { viewModel.pedirRevocar(vinculacion) }) {
                            Text("Revocar", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        Text(
            "${apoderados.size} de ${ReglasVinculacion.MAX_APODERADOS} apoderados",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Diálogos de rechazo (motivo obligatorio) y de revocación (solo administrador). */
@Composable
internal fun DialogosVinculacion(state: JugadoresUiState, viewModel: JugadoresViewModel) {
    state.dialogoRechazo?.let { dialogo ->
        AlertDialog(
            onDismissRequest = viewModel::cerrarRechazo,
            title = { Text("Rechazar solicitud") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${dialogo.vinculacion.padreNombre} → ${dialogo.vinculacion.jugadorNombre}")
                    CampoFormulario(
                        valor = dialogo.motivo,
                        onCambio = viewModel::onMotivoRechazo,
                        etiqueta = "Motivo del rechazo",
                        error = dialogo.error,
                        ayuda = "${dialogo.motivo.trim().length}/${ReglasVinculacion.MOTIVO_MAX} (mínimo ${ReglasVinculacion.MOTIVO_MIN})",
                        minLineas = 3,
                    )
                }
            },
            confirmButton = { TextButton(onClick = viewModel::confirmarRechazo) { Text("Rechazar") } },
            dismissButton = { TextButton(onClick = viewModel::cerrarRechazo) { Text("Cancelar") } },
        )
    }
    state.revocando?.let { vinculacion ->
        AlertDialog(
            onDismissRequest = viewModel::cancelarRevocar,
            title = { Text("Revocar vinculación") },
            text = {
                Text(
                    "${vinculacion.padreNombre} dejará de ver de inmediato el perfil, la asistencia, las " +
                        "estadísticas y las mensualidades de ${vinculacion.jugadorNombre}. La acción queda registrada.",
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmarRevocar) { Text("Revocar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelarRevocar) { Text("Cancelar") } },
        )
    }
}
