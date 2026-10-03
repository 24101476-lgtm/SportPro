package com.esan.sportpro.ui.partido

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.partido.AmbitoEvento
import com.esan.sportpro.domain.partido.EquipoEvento
import com.esan.sportpro.domain.partido.EstadoEvento
import com.esan.sportpro.domain.partido.EstadoPartido
import com.esan.sportpro.domain.partido.EventoPartido
import com.esan.sportpro.domain.partido.Partido
import com.esan.sportpro.domain.partido.ReglasPartido
import com.esan.sportpro.domain.partido.TipoEvento
import com.esan.sportpro.domain.partido.TiposEvento
import kotlinx.coroutines.launch

/** Registro del partido en vivo: marcador, cronómetro, paleta de eventos y cronología. */
@Composable
fun RegistroPartidoScreen(
    onVolver: () -> Unit,
    viewModel: RegistroPartidoViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // El botón atrás cierra primero la hoja o el diálogo abierto y luego vuelve a la lista.
    BackHandler(enabled = state.hoja != null || state.dialogo != null) {
        if (state.hoja != null) viewModel.cerrarHoja() else viewModel.cerrarDialogo()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { e ->
            if (e is RegistroEvento.Mensaje) {
                scope.launch {
                    snackbar.currentSnackbarData?.dismiss()
                    snackbar.showSnackbar(e.texto)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (!state.conectado) BannerSinConexion(state.pendientesSync)

            val partido = state.partido
            if (partido == null) {
                EstadoSinPartido(state, onVolver)
                return@Column
            }

            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item { Marcador(state, partido, onVolver) }

                state.error?.let { error ->
                    item { Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
                }

                when (partido.estado) {
                    EstadoPartido.PROGRAMADO -> item {
                        PreInicio(state, partido, onIniciar = viewModel::abrirInicioPartido)
                    }
                    else -> item {
                        Indicadores(
                            state = state,
                            partido = partido,
                            onCompletar = viewModel::verPendientes,
                        )
                    }
                }

                if (state.duplicados.isNotEmpty()) {
                    item { AlertaDuplicados(state.duplicados.size, onRevisar = viewModel::verDuplicados) }
                }

                val paleta = state.paleta
                if (paleta.isNotEmpty()) {
                    val controles = paleta.filter { it.ambito == AmbitoEvento.PARTIDO }
                    val eventos = paleta.filter { it.ambito != AmbitoEvento.PARTIDO }
                    if (controles.isNotEmpty()) {
                        item { ControlTiempo(controles, state, partido) { viewModel.abrirHoja(it) } }
                    }
                    item { TituloSeccion("Registrar evento", "Toca un evento: minuto y equipo ya vienen cargados") }
                    items(eventos.chunked(3)) { fila ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            fila.forEach { tipo ->
                                BotonPaleta(tipo, Modifier.weight(1f)) { viewModel.abrirHoja(tipo) }
                            }
                            repeat(3 - fila.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }

                if (partido.estado == EstadoPartido.FINALIZADO || partido.estado == EstadoPartido.ACTA_CERRADA) {
                    item {
                        Finalizado(
                            state = state,
                            partido = partido,
                            onCerrarActa = viewModel::pedirCierreActa,
                        )
                    }
                }

                val cronologia = state.cronologia.asReversed()
                item {
                    TituloSeccion(
                        "Cronología",
                        if (cronologia.isEmpty()) "Aún no hay eventos registrados" else "${cronologia.size} eventos · más reciente arriba",
                    )
                }
                items(cronologia, key = { it.id }) { evento ->
                    EventoItem(
                        evento = evento,
                        partido = partido,
                        puedeCorregir = state.puedeCorregir,
                        esUltimo = evento.id == cronologia.lastOrNull()?.id,
                        onEditar = { viewModel.editarEvento(evento) },
                        onAnular = { viewModel.pedirAnulacion(evento) },
                        onHistorial = { viewModel.verHistorial(evento) },
                    )
                }
            }
        }
    }

    state.hoja?.let { hoja ->
        EventoBottomSheet(hoja = hoja, state = state, viewModel = viewModel)
    }
    DialogosPartido(state = state, viewModel = viewModel)
}

@Composable
private fun EstadoSinPartido(state: RegistroUiState, onVolver: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (state.cargando) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Text("Cargando partido…", modifier = Modifier.padding(top = 12.dp))
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(32.dp),
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text(
                    state.error ?: "No se encontró el partido",
                    textAlign = TextAlign.Center,
                    color = if (state.error != null) MaterialTheme.colorScheme.error else Color.Unspecified,
                )
                Button(onClick = onVolver) { Text("Volver a la lista") }
            }
        }
    }
}

@Composable
fun TituloSeccion(titulo: String, subtitulo: String? = null) {
    Column(modifier = Modifier.padding(top = 4.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleMedium)
        subtitulo?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---------------------------------------------------------------- Marcador

@Composable
private fun Marcador(state: RegistroUiState, partido: Partido, onVolver: () -> Unit) {
    val reloj = state.reloj
    val fondo = Brush.linearGradient(listOf(ColoresPartido.VerdeOscuro, ColoresPartido.Verde, ColoresPartido.VerdeClaro))
    Surface(
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .background(fondo)
                .padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onVolver) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                }
                Text(
                    partido.categoria ?: "Partido",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                EstadoPartidoChip(partido)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
            ) {
                EquipoMarcador(partido.nombreEquipo, ColoresPartido.Azul, Modifier.weight(1f))
                Text(
                    "${state.marcador.propio}  -  ${state.marcador.rival}",
                    color = Color.White,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                EquipoMarcador(partido.nombreRival, ColoresPartido.Rojo, Modifier.weight(1f))
            }

            if (partido.estado != EstadoPartido.PROGRAMADO) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        textoReloj(partido, reloj),
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                    if (partido.estado == EstadoPartido.EN_CURSO && !partido.enDescanso) {
                        Spacer(Modifier.width(10.dp))
                        Surface(shape = RoundedCornerShape(8.dp), color = Color.White.copy(alpha = 0.18f)) {
                            Text(
                                "min ${reloj.minuto}'",
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                LinearProgressIndicator(
                    progress = { reloj.progreso },
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.25f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, top = 10.dp)
                        .height(6.dp),
                )
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("0'", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.weight(1f))
                    Text("${partido.duracionTiempoMin}'", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.weight(1f))
                    Text("${partido.duracionTiempoMin * 2}'", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                }
                if (reloj.adicion > 0) {
                    Text(
                        "Tiempo añadido del 1.er tiempo: registra \"Fin de primer tiempo\" cuando el árbitro pite.",
                        color = Color(0xFFFFE082),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EquipoMarcador(nombre: String, color: Color, modifier: Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Escudo(nombre, color, 52.dp)
        Text(
            nombre,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

// ---------------------------------------------------------------- Antes del inicio

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreInicio(state: RegistroUiState, partido: Partido, onIniciar: () -> Unit) {
    val titulares = state.convocados.filter { it.titular }
    val suplentes = state.convocados.count { !it.titular }
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Listo para iniciar", style = MaterialTheme.typography.titleMedium)
            FilaDato(
                icono = if (partido.alineacionConfirmada) Icons.Default.CheckCircle else Icons.Default.Warning,
                color = if (partido.alineacionConfirmada) ColoresPartido.Verde else MaterialTheme.colorScheme.error,
                texto = if (partido.alineacionConfirmada) "Alineación confirmada" else "Alineación pendiente de confirmar",
            )
            FilaDato(Icons.Default.Groups, ColoresPartido.Azul, "${titulares.size} titulares · $suplentes suplentes")
            FilaDato(Icons.Default.SwapHoriz, ColoresPartido.Azul, "${partido.cambiosPermitidos} cambios permitidos")
            FilaDato(Icons.Default.Timer, ColoresPartido.Azul, "2 tiempos de ${partido.duracionTiempoMin} min · termina al ${partido.duracionTiempoMin * 2}'")
            if (titulares.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    titulares.forEach { j ->
                        Surface(shape = CircleShape, color = ColoresPartido.Verde.copy(alpha = 0.12f), modifier = Modifier.size(34.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("${j.dorsal ?: "?"}", color = ColoresPartido.Verde, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            Button(
                onClick = onIniciar,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ColoresPartido.Verde),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Iniciar partido", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FilaDato(icono: androidx.compose.ui.graphics.vector.ImageVector, color: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(texto, style = MaterialTheme.typography.bodyLarge)
    }
}

// ---------------------------------------------------------------- Indicadores

@Composable
private fun Indicadores(state: RegistroUiState, partido: Partido, onCompletar: () -> Unit) {
    val incompletos = state.incompletos.size
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Indicador(
            titulo = "Cambios",
            valor = "${ReglasPartido.cambiosRealizados(state.eventos)}/${partido.cambiosPermitidos}",
            color = ColoresPartido.Azul,
            modifier = Modifier.weight(1f),
        )
        Indicador(
            titulo = "Incompletos",
            valor = "$incompletos",
            color = if (incompletos > 0) ColoresPartido.Naranja else ColoresPartido.Gris,
            accion = if (incompletos > 0 && state.puedeCorregir) "Completar pendientes" else null,
            onClick = if (incompletos > 0 && state.puedeCorregir) onCompletar else null,
            modifier = Modifier.weight(1.4f),
        )
        Indicador(
            titulo = "Por sincronizar",
            valor = "${state.pendientesSync}",
            color = if (state.pendientesSync > 0) ColoresPartido.Rojo else ColoresPartido.Verde,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun Indicador(
    titulo: String,
    valor: String,
    color: Color,
    modifier: Modifier,
    accion: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.10f),
        modifier = modifier
            .heightIn(min = 72.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(valor, color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(titulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            accion?.let {
                Text(it, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AlertaDuplicados(grupos: Int, onRevisar: () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFFFEBEE), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = ColoresPartido.Rojo)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Posibles eventos duplicados", color = ColoresPartido.Rojo, fontWeight = FontWeight.Bold)
                Text("$grupos grupo(s) con mismo tipo, minuto y jugador", style = MaterialTheme.typography.bodyMedium)
            }
            TextButton(onClick = onRevisar) { Text("Revisar") }
        }
    }
}

// ---------------------------------------------------------------- Paleta

@Composable
private fun ControlTiempo(controles: List<TipoEvento>, state: RegistroUiState, partido: Partido, onClick: (TipoEvento) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TituloSeccion("Control del tiempo")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            controles.forEach { tipo ->
                // Se resalta la acción que corresponde al momento del partido.
                val recomendado = when (tipo.id) {
                    TiposEvento.FIN_PRIMER_TIEMPO -> partido.inicioSegundoTiempoMs == null && !partido.enDescanso &&
                        state.reloj.minuto >= partido.duracionTiempoMin
                    TiposEvento.INICIO_SEGUNDO_TIEMPO -> partido.enDescanso
                    else -> false
                }
                if (recomendado) {
                    Button(
                        onClick = { onClick(tipo) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ColoresPartido.VerdeOscuro),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    ) {
                        Icon(iconoTipo(tipo.id), contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(tipo.nombre, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    OutlinedButton(
                        onClick = { onClick(tipo) },
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    ) {
                        Icon(iconoTipo(tipo.id), contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(tipo.nombre, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun BotonPaleta(tipo: TipoEvento, modifier: Modifier, onClick: () -> Unit) {
    val color = colorTipo(tipo.id)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = modifier.heightIn(min = 92.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 92.dp)
                .background(color.copy(alpha = 0.08f))
                .padding(horizontal = 6.dp, vertical = 12.dp),
        ) {
            if (tipo.id == TiposEvento.TARJETA_AMARILLA || tipo.id == TiposEvento.TARJETA_ROJA) {
                // Tarjeta dibujada con su color real.
                Box(
                    modifier = Modifier
                        .size(width = 26.dp, height = 36.dp)
                        .background(color, RoundedCornerShape(4.dp)),
                )
            } else {
                IconoEvento(tipo.id, 40.dp)
            }
            Text(
                tipo.nombre,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

// ---------------------------------------------------------------- Final

@Composable
private fun Finalizado(state: RegistroUiState, partido: Partido, onCerrarActa: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val cerrado = partido.estado == EstadoPartido.ACTA_CERRADA
            Text(if (cerrado) "Acta cerrada" else "Partido finalizado", style = MaterialTheme.typography.titleMedium)
            Text(
                "Resultado final: ${partido.nombreEquipo} ${state.marcador.propio} - ${state.marcador.rival} ${partido.nombreRival}",
                style = MaterialTheme.typography.bodyLarge,
            )
            if (cerrado) {
                val conIncompletos = partido.actaCerradaConIncompletos ?: 0
                Text(
                    if (conIncompletos > 0) {
                        "Se cerró con $conIncompletos eventos incompletos. Ya no se pueden corregir eventos."
                    } else {
                        "Ya no se pueden corregir eventos."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    "Revisa la cronología, completa los pendientes y cierra el acta.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.puedeCorregir) {
                    Button(
                        onClick = onCerrarActa,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                    ) { Text("Cerrar acta del partido") }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Cronología

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EventoItem(
    evento: EventoPartido,
    partido: Partido?,
    puedeCorregir: Boolean,
    esUltimo: Boolean,
    onEditar: () -> Unit,
    onAnular: () -> Unit,
    onHistorial: () -> Unit,
) {
    val anulado = evento.estado == EstadoEvento.ANULADO
    var menu by remember { mutableStateOf(false) }
    val lineaColor = MaterialTheme.colorScheme.outlineVariant
    val esRival = evento.equipo == EquipoEvento.RIVAL

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                // Línea vertical de la línea de tiempo, a la altura del ícono.
                val x = 52.dp.toPx() + 18.dp.toPx()
                drawLine(
                    color = lineaColor,
                    start = Offset(x, 36.dp.toPx()),
                    end = Offset(x, if (esUltimo) 36.dp.toPx() else size.height + 14.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                )
            },
    ) {
        Text(
            "${evento.minuto}'",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(44.dp)
                .padding(top = 8.dp),
        )
        Spacer(Modifier.width(8.dp))
        Box(modifier = Modifier.padding(top = 2.dp)) { IconoEvento(evento.tipoId, 36.dp, apagado = anulado) }
        Spacer(Modifier.width(10.dp))
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (anulado) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (anulado) 0.dp else 1.dp),
            modifier = Modifier
                .weight(1f)
                .alpha(if (anulado) 0.7f else 1f),
        ) {
            Row(modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            evento.tipoNombre,
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (anulado) TextDecoration.LineThrough else null,
                        )
                        Spacer(Modifier.width(6.dp))
                        if (evento.pendienteSincronizar) {
                            Icon(Icons.Default.Schedule, contentDescription = "Pendiente de sincronizar", tint = ColoresPartido.Gris, modifier = Modifier.size(16.dp))
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Sincronizado", tint = ColoresPartido.Verde, modifier = Modifier.size(16.dp))
                        }
                        if (esRival) {
                            Spacer(Modifier.width(6.dp))
                            Etiqueta("Rival", ColoresPartido.Rojo.copy(alpha = 0.85f))
                        }
                    }
                    val detalle = detalleEvento(evento, partido)
                    if (detalle.isNotBlank()) {
                        Text(
                            detalle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = if (anulado) TextDecoration.LineThrough else null,
                        )
                    }
                    evento.observacion?.let {
                        Text("“$it”", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (anulado || evento.incompleto || evento.fueraDeOrden || evento.excedeCambios || evento.version > 1) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 6.dp),
                        ) {
                            if (anulado) Etiqueta("Anulado", ColoresPartido.Gris)
                            if (evento.incompleto && !anulado) Etiqueta("Incompleto", ColoresPartido.Naranja)
                            if (evento.fueraDeOrden) Etiqueta("Fuera de orden", ColoresPartido.Morado)
                            if (evento.excedeCambios) Etiqueta("Excede cambios", ColoresPartido.Rojo)
                            if (evento.version > 1) Etiqueta("Corregido v${evento.version}", ColoresPartido.Azul)
                        }
                    }
                    if (anulado && evento.motivo != null) {
                        Text(
                            "Motivo: ${evento.motivo}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Acciones del evento")
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        if (puedeCorregir && !anulado) {
                            DropdownMenuItem(text = { Text("Editar") }, onClick = { menu = false; onEditar() })
                            DropdownMenuItem(text = { Text("Anular") }, onClick = { menu = false; onAnular() })
                        }
                        DropdownMenuItem(text = { Text("Ver historial") }, onClick = { menu = false; onHistorial() })
                    }
                }
            }
        }
    }
}

/** Botón secundario reutilizado en hojas y diálogos. */
@Composable
fun BotonSecundario(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onClick, shape = RoundedCornerShape(14.dp), modifier = modifier.heightIn(min = 52.dp)) { Text(texto) }
}
