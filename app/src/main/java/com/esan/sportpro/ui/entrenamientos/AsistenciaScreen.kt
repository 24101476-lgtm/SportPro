package com.esan.sportpro.ui.entrenamientos

import com.esan.sportpro.core.fecha.FechasSportPro
import com.esan.sportpro.ui.common.BarraSuperiorSportPro
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Groups2
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.esan.sportpro.domain.entrenamientos.CampoMarca
import com.esan.sportpro.domain.entrenamientos.EstadoAsistencia
import com.esan.sportpro.domain.entrenamientos.EstadoSesion
import com.esan.sportpro.domain.entrenamientos.MarcaAsistencia
import com.esan.sportpro.domain.entrenamientos.RegistroHistorialAsistencia
import com.esan.sportpro.domain.entrenamientos.ReglasAsistencia
import com.esan.sportpro.domain.jugadores.Jugador
import com.esan.sportpro.ui.common.CampoFormulario
import com.esan.sportpro.ui.common.EmptyStateMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * US-011 — Registro de asistencia (Figma node 11-628). Cada marca se guarda al instante, también
 * sin conexión; el indicador superior muestra si hay cambios por sincronizar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsistenciaScreen(
    entrenamientoId: String,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AsistenciaViewModel = hiltViewModel(),
) {
    LaunchedEffect(entrenamientoId) { viewModel.iniciar(entrenamientoId) }
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { evento ->
            if (evento is AsistenciaEvent.Mensaje) snackbarHostState.showSnackbar(evento.texto)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BarraSuperiorSportPro(
                title = { Text("Asistencia") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (state.entrenamiento != null) {
                        IconButton(onClick = viewModel::abrirHistorial) {
                            Icon(Icons.Default.History, contentDescription = "Historial de cambios")
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (state.editable && state.plantel.isNotEmpty()) {
                Button(
                    onClick = viewModel::solicitarCierre,
                    enabled = !state.cerrando,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) {
                    if (state.cerrando) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Cerrar sesión")
                    }
                }
            }
        },
    ) { padding ->
        val contenido = Modifier.padding(padding)
        val entrenamiento = state.entrenamiento
        when {
            state.cargando -> Box(contenido.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            state.error != null -> EmptyStateMessage(mensaje = state.error.orEmpty(), modifier = contenido)

            entrenamiento == null -> EmptyStateMessage("No se encontró el entrenamiento.", contenido)

            else -> Column(modifier = contenido.fillMaxSize()) {
                state.indicador?.let { BannerSync(it) }
                Encabezado(state, viewModel)
                if (state.plantel.isEmpty()) {
                    EmptyStateMessage(
                        mensaje = "El equipo ${entrenamiento.equipoNombre} todavía no tiene jugadores activos.",
                        icono = Icons.Default.Groups2,
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.plantel, key = { it.id }) { jugador ->
                            FilaJugador(state, jugador, viewModel)
                        }
                    }
                }
            }
        }
    }

    state.dialogo?.let { DialogoDetalle(it, viewModel) }
    state.confirmacion?.let { DialogoConfirmacion(it, state.cerrando, viewModel) }
    if (state.mostrarHistorial) DialogoHistorial(state, viewModel)
}

@Composable
private fun BannerSync(indicador: IndicadorSync) {
    val (icono, fondo) = when (indicador) {
        IndicadorSync.SinConexion -> Icons.Default.CloudOff to MaterialTheme.colorScheme.errorContainer
        IndicadorSync.Sincronizando -> Icons.Default.CloudSync to MaterialTheme.colorScheme.secondaryContainer
        is IndicadorSync.Sincronizado -> Icons.Default.CloudDone to MaterialTheme.colorScheme.primaryContainer
    }
    Row(
        modifier = Modifier.fillMaxWidth().background(fondo).padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icono, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(indicador.texto, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Encabezado(state: AsistenciaUiState, viewModel: AsistenciaViewModel) {
    val entrenamiento = state.entrenamiento ?: return
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("${entrenamiento.equipoNombre} · ${entrenamiento.fecha} ${entrenamiento.hora}", style = MaterialTheme.typography.titleMedium)
        when (entrenamiento.estadoSesion) {
            EstadoSesion.REALIZADA -> Text(
                "Sesión realizada · Asistencia del equipo: ${ReglasAsistencia.formatearPorcentaje(state.porcentaje)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            EstadoSesion.CANCELADA -> Text(
                "Sesión cancelada",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            EstadoSesion.PROGRAMADA -> Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val pendientes = state.sinMarcar.size
                Text(
                    if (pendientes == 0) "Todos marcados (${state.marcados})" else ReglasAsistencia.mensajeSinMarcar(pendientes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (pendientes > 0 && state.resaltarSinMarcar) MaterialTheme.colorScheme.error else Color.Unspecified,
                )
                if (state.editable) {
                    TextButton(onClick = viewModel::marcarTodosPresentes) { Text("Marcar todos como presentes") }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilaJugador(state: AsistenciaUiState, jugador: Jugador, viewModel: AsistenciaViewModel) {
    val estado = state.estadoDe(jugador.id)
    val detalle = state.detalleDe(jugador.id)
    val resaltar = estado == null && state.resaltarSinMarcar
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (resaltar) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(jugador)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        listOfNotNull(jugador.dorsal?.let { "#$it" }, jugador.nombreCompleto).joinToString("  "),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        descripcionEstado(estado, detalle),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (estado == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                    )
                    if (jugador.id in state.conConflicto) {
                        Text(
                            "Marcado por más de una persona: vale el último cambio (ver historial).",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }
            if (state.editable) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EstadoAsistencia.entries.forEach { opcion ->
                        FilterChip(
                            selected = estado == opcion,
                            onClick = { viewModel.onEstado(jugador, opcion) },
                            label = { Text(opcion.etiqueta) },
                        )
                    }
                }
            }
        }
    }
}

private fun descripcionEstado(estado: EstadoAsistencia?, detalle: MarcaAsistencia?): String = when (estado) {
    null -> "Sin marcar"
    EstadoAsistencia.TARDE -> detalle?.minutosRetraso?.let { "Tarde · $it min de retraso" } ?: "Tarde"
    EstadoAsistencia.JUSTIFICADO -> detalle?.motivo?.takeIf { it.isNotBlank() }?.let { "Falta justificada · $it" } ?: EstadoAsistencia.JUSTIFICADO.etiqueta
    else -> estado.etiqueta
}

@Composable
private fun Avatar(jugador: Jugador) {
    Box(
        modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (jugador.fotoUrl.isNotBlank()) {
            AsyncImage(
                model = jugador.fotoUrl,
                contentDescription = "Foto de ${jugador.nombreCompleto}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(jugador.iniciales, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun DialogoDetalle(dialogo: DialogoMarca, viewModel: AsistenciaViewModel) {
    AlertDialog(
        onDismissRequest = viewModel::cerrarDialogo,
        title = { Text("${dialogo.estado.etiqueta}: ${dialogo.jugadorNombre}") },
        text = {
            if (dialogo.estado == EstadoAsistencia.TARDE) {
                CampoFormulario(
                    valor = dialogo.minutos,
                    onCambio = { v -> viewModel.onDialogoChanged { copy(minutos = v, errores = errores - CampoMarca.MINUTOS) } },
                    etiqueta = "Minutos de retraso",
                    error = dialogo.errores[CampoMarca.MINUTOS],
                    ayuda = "Entre ${ReglasAsistencia.MINUTOS_MIN} y ${ReglasAsistencia.MINUTOS_MAX}",
                    teclado = KeyboardType.Number,
                )
            } else {
                CampoFormulario(
                    valor = dialogo.motivo,
                    onCambio = { v -> viewModel.onDialogoChanged { copy(motivo = v, errores = errores - CampoMarca.MOTIVO) } },
                    etiqueta = "Motivo",
                    error = dialogo.errores[CampoMarca.MOTIVO],
                    ayuda = "${dialogo.motivo.length}/${ReglasAsistencia.MOTIVO_MAX}",
                    minLineas = 2,
                )
            }
        },
        confirmButton = { TextButton(onClick = viewModel::confirmarDialogo) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = viewModel::cerrarDialogo) { Text("Cancelar") } },
    )
}

@Composable
private fun DialogoConfirmacion(confirmacion: ConfirmacionAsistencia, cerrando: Boolean, viewModel: AsistenciaViewModel) {
    val (titulo, texto, accion) = when (confirmacion) {
        is ConfirmacionAsistencia.TodosPresentes -> Triple(
            "Marcar todos como presentes",
            if (confirmacion.reemplazadas == 1) {
                "1 jugador ya tiene otra marca (tarde o falta) y se cambiará a Presente."
            } else {
                "${confirmacion.reemplazadas} jugadores ya tienen otra marca (tarde o falta) y se cambiarán a Presente."
            },
            "Marcar todos",
        )
        is ConfirmacionAsistencia.CerrarSesion -> Triple(
            "Cerrar sesión",
            "La sesión pasará a Realizada con una asistencia del equipo de " +
                "${ReglasAsistencia.formatearPorcentaje(confirmacion.porcentaje)}. Después ya no podrás cambiar las marcas.",
            "Cerrar sesión",
        )
    }
    AlertDialog(
        onDismissRequest = viewModel::cancelarConfirmacion,
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = { TextButton(onClick = viewModel::confirmar, enabled = !cerrando) { Text(accion) } },
        dismissButton = { TextButton(onClick = viewModel::cancelarConfirmacion) { Text("Cancelar") } },
    )
}

@Composable
private fun DialogoHistorial(state: AsistenciaUiState, viewModel: AsistenciaViewModel) {
    val formato = remember { FechasSportPro.formatoFechaHoraSegundos() }
    AlertDialog(
        onDismissRequest = viewModel::cerrarHistorial,
        title = { Text("Historial de cambios") },
        text = {
            val registros = state.historialOrdenado
            if (registros.isEmpty()) {
                Text("Todavía no hay marcas en esta sesión.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(registros, key = { it.id }) { registro -> FilaHistorial(registro, formato) }
                }
            }
        },
        confirmButton = { TextButton(onClick = viewModel::cerrarHistorial) { Text("Cerrar") } },
    )
}

@Composable
private fun FilaHistorial(registro: RegistroHistorialAsistencia, formato: SimpleDateFormat) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            "${registro.jugadorNombre}: ${descripcionEstado(registro.estadoAsistencia, MarcaAsistencia(registro.estado, registro.minutosRetraso, registro.motivo))}",
            style = MaterialTheme.typography.bodyMedium,
        )
        val cuando = registro.marcadoEn?.let { formato.format(it) }
            ?: "${formato.format(Date(registro.marcadoEnDispositivo))} (pendiente de sincronizar)"
        Text(
            "${registro.marcadoPorNombre.ifBlank { "Usuario" }} · $cuando",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(modifier = Modifier.padding(top = 6.dp))
    }
}
