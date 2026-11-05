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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.navigationBarsPadding
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
                com.esan.sportpro.ui.common.BotonPrincipalSportPro(
                    texto = "Cerrar sesión",
                    onClick = viewModel::solicitarCierre,
                    habilitado = !state.cerrando,
                    cargando = state.cerrando,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                        .heightIn(min = 56.dp),
                )
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
    val fecha = FechasSportPro.desdeIso(entrenamiento.fecha)
    val estados = state.plantel.map { state.estadoDe(it.id) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Rediseño: tarjeta con la foto de los conos, la fecha grande en lima y el objetivo de la sesión.
        com.esan.sportpro.ui.common.FondoFotoSportPro(
            foto = com.esan.sportpro.ui.common.FotoSportPro.CONOS,
            velo = androidx.compose.ui.graphics.Brush.verticalGradient(
                0f to com.esan.sportpro.core.theme.PaletaSportPro.MusgoProfundo.copy(alpha = 0.45f),
                1f to com.esan.sportpro.core.theme.PaletaSportPro.Carbon.copy(alpha = 0.92f),
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(18.dp),
            ) {
                androidx.compose.material3.Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    color = com.esan.sportpro.core.theme.PaletaSportPro.Lima,
                    contentColor = com.esan.sportpro.core.theme.PaletaSportPro.Carbon,
                    modifier = Modifier.size(width = 64.dp, height = 74.dp),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(fecha?.dayOfMonth?.toString()?.padStart(2, '0') ?: "–", style = com.esan.sportpro.core.theme.EstilosSportPro.NumeroMediano)
                        Text(fecha?.let { diaSemana(it) } ?: "", style = MaterialTheme.typography.labelMedium)
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        entrenamiento.objetivo.ifBlank { "Entrenamiento" },
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                    Text(
                        listOf(entrenamiento.equipoNombre, entrenamiento.hora).filter { it.isNotBlank() }.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }
        }

        com.esan.sportpro.ui.common.FilaEstadisticas(
            datos = listOf(
                com.esan.sportpro.ui.common.DatoEstadistica("${estados.count { it == EstadoAsistencia.PRESENTE }}", "Presentes"),
                com.esan.sportpro.ui.common.DatoEstadistica("${estados.count { it == EstadoAsistencia.TARDE }}", "Tarde"),
                com.esan.sportpro.ui.common.DatoEstadistica("${estados.count { it == EstadoAsistencia.JUSTIFICADO || it == EstadoAsistencia.AUSENTE }}", "Faltas"),
                com.esan.sportpro.ui.common.DatoEstadistica("${state.sinMarcar.size}", "Sin marcar"),
            ),
        )

        when (entrenamiento.estadoSesion) {
            EstadoSesion.REALIZADA -> Text(
                "Sesión realizada. Asistencia del equipo: ${ReglasAsistencia.formatearPorcentaje(state.porcentaje)}",
                style = MaterialTheme.typography.bodyMedium,
                color = com.esan.sportpro.core.theme.SportProTema.colores.textoAcento,
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
                    color = if (pendientes > 0 && state.resaltarSinMarcar) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (state.editable) {
                    TextButton(onClick = viewModel::marcarTodosPresentes) {
                        Text("Marcar todos presentes", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private val FORMATO_DIA_SEMANA: java.time.format.DateTimeFormatter =
    java.time.format.DateTimeFormatter.ofPattern("EEE", java.util.Locale.forLanguageTag("es-PE"))

/** "Sáb", "Jue"… sin el punto que agrega el formato abreviado. */
private fun diaSemana(fecha: java.time.LocalDate): String =
    fecha.format(FORMATO_DIA_SEMANA).removeSuffix(".").replaceFirstChar { it.uppercase() }

@Composable
private fun FilaJugador(state: AsistenciaUiState, jugador: Jugador, viewModel: AsistenciaViewModel) {
    val estado = state.estadoDe(jugador.id)
    val detalle = state.detalleDe(jugador.id)
    val resaltar = estado == null && state.resaltarSinMarcar
    androidx.compose.material3.Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        color = if (resaltar) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(jugador)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        jugador.nombreCompleto,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    )
                    Text(
                        descripcionEstado(estado, detalle),
                        style = MaterialTheme.typography.bodySmall,
                        color = estado?.let { colorEstado(it) } ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (jugador.id in state.conConflicto) {
                        Text(
                            "Marcado por más de una persona: vale el último cambio (ver historial).",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
                jugador.dorsal?.let { dorsal ->
                    Text("$dorsal", style = com.esan.sportpro.core.theme.EstilosSportPro.NumeroMediano, color = com.esan.sportpro.core.theme.SportProTema.colores.textoAcento)
                }
            }
            if (state.editable) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    EstadoAsistencia.entries.forEach { opcion ->
                        BotonEstado(
                            opcion = opcion,
                            elegido = estado == opcion,
                            onClick = { viewModel.onEstado(jugador, opcion) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/** Texto corto del botón: los nombres completos ("Falta injustificada") no entran en 4 columnas. */
private fun etiquetaCorta(estado: EstadoAsistencia): String = when (estado) {
    EstadoAsistencia.PRESENTE -> "Presente"
    EstadoAsistencia.TARDE -> "Tarde"
    EstadoAsistencia.JUSTIFICADO -> "Justificada"
    EstadoAsistencia.AUSENTE -> "Falta"
}

/** Presente usa el acento de la marca; los demás, los colores de estado del tema. */
@Composable
private fun colorEstado(estado: EstadoAsistencia): Color {
    val c = com.esan.sportpro.core.theme.SportProTema.colores
    return when (estado) {
        EstadoAsistencia.PRESENTE -> c.textoAcento
        EstadoAsistencia.TARDE -> c.advertencia
        EstadoAsistencia.JUSTIFICADO -> c.informacion
        EstadoAsistencia.AUSENTE -> c.error
    }
}

@Composable
private fun BotonEstado(opcion: EstadoAsistencia, elegido: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = com.esan.sportpro.core.theme.SportProTema.colores
    val relleno = if (opcion == EstadoAsistencia.PRESENTE) c.acento else colorEstado(opcion)
    val luz = 0.2126f * relleno.red + 0.7152f * relleno.green + 0.0722f * relleno.blue
    val textoElegido = if (luz > 0.55f) com.esan.sportpro.core.theme.PaletaSportPro.Carbon else Color.White
    androidx.compose.material3.Surface(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.CircleShape,
        color = if (elegido) relleno else Color.Transparent,
        contentColor = if (elegido) textoElegido else MaterialTheme.colorScheme.onSurface,
        border = if (elegido) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
            .heightIn(min = 44.dp)
            // Para lectores de pantalla: nombre completo del estado y si está elegido.
            .semantics {
                contentDescription = opcion.etiqueta
                selected = elegido
            },
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp)) {
            Text(
                etiquetaCorta(opcion),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (elegido) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.SemiBold,
                maxLines = 1,
            )
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
