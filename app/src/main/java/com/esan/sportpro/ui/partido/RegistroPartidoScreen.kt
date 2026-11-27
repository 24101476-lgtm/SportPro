package com.esan.sportpro.ui.partido

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SportsSoccer
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
import androidx.compose.ui.platform.LocalContext
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
import com.esan.sportpro.domain.partido.ReglasActa
import com.esan.sportpro.domain.partido.ReglasPartido
import com.esan.sportpro.domain.partido.TipoEvento
import com.esan.sportpro.domain.partido.TiposEvento
import kotlinx.coroutines.launch

/** Registro del partido en vivo: marcador, cronómetro, paleta de eventos y cronología. */
@Composable
fun RegistroPartidoScreen(
    onVolver: () -> Unit,
    onResumenIa: () -> Unit = {},
    onConfigurarAlineacion: () -> Unit = {},
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
                item { Marcador(state, partido, onVolver, onVerAlineacion = viewModel::abrirAlineacion) }

                state.error?.let { error ->
                    item { Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
                }

                when (partido.estado) {
                    EstadoPartido.PROGRAMADO -> item {
                        PreInicio(
                            state = state,
                            partido = partido,
                            onIniciar = viewModel::abrirInicioPartido,
                            onConfigurarAlineacion = onConfigurarAlineacion,
                        )
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

                if (partido.estado == EstadoPartido.EN_CURSO && !state.puedeCorregir) {
                    item { AvisoSoloLectura() }
                }

                val paleta = state.paleta
                if (paleta.isNotEmpty()) {
                    val controles = paleta.filter { it.ambito == AmbitoEvento.PARTIDO }
                    val eventos = paleta.filter { it.ambito != AmbitoEvento.PARTIDO }
                    if (controles.isNotEmpty()) {
                        item { ControlTiempo(controles, state, partido) { viewModel.abrirHoja(it) } }
                    }
                    item { TituloSeccion("Registrar evento", "Toca un evento: minuto y equipo ya vienen cargados") }
                    val conteo = state.eventos
                        .filter { it.estado == com.esan.sportpro.domain.partido.EstadoEvento.VIGENTE }
                        .groupingBy { it.tipoId }
                        .eachCount()
                    items(eventos.chunked(3)) { fila ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            fila.forEach { tipo ->
                                BotonPaleta(tipo, conteo[tipo.id] ?: 0, Modifier.weight(1f)) { viewModel.abrirHoja(tipo) }
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
                            onVerActa = viewModel::verActa,
                            onReabrirActa = viewModel::pedirReapertura,
                            onResumenIa = onResumenIa,
                        )
                    }
                }

                val cronologia = state.cronologia.asReversed()
                item {
                    TituloSeccion(
                        "Cronología",
                        if (cronologia.isEmpty()) "Aún no hay eventos registrados" else "${cantidad(cronologia.size, "evento", "eventos")} · el más reciente arriba",
                    )
                }
                items(cronologia, key = { it.id }) { evento ->
                    EventoItem(
                        evento = evento,
                        partido = partido,
                        textoMinuto = minutoEvento(evento, state.eventos, partido),
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

    // Once en vivo (se abre tocando el escudo propio del marcador).
    if (state.viendoAlineacion) {
        state.partido?.let { partido -> AlineacionEnVivoDialog(state = state, partido = partido, viewModel = viewModel) }
    }
    state.hoja?.let { hoja ->
        EventoBottomSheet(hoja = hoja, state = state, viewModel = viewModel)
    }
    DialogosPartido(state = state, viewModel = viewModel)
}

/** Explica por qué no hay controles: solo los responsables del partido pueden registrar eventos. */
@Composable
private fun AvisoSoloLectura() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Modo solo lectura", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                "Este partido lo registra otra cuenta. Para continuarlo, entra con la cuenta que lo inició o con un administrador.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
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
private fun Marcador(
    state: RegistroUiState,
    partido: Partido,
    onVolver: () -> Unit,
    onVerAlineacion: () -> Unit = {},
) {
    val reloj = state.reloj
    // Rediseño: foto del estadio en B/N con velo; el texto sigue siendo blanco.
    com.esan.sportpro.ui.common.FondoFotoSportPro(
        foto = com.esan.sportpro.ui.common.FotoSportPro.ESTADIO,
        forma = RoundedCornerShape(28.dp),
        velo = Brush.verticalGradient(
            0f to com.esan.sportpro.core.theme.PaletaSportPro.MusgoProfundo.copy(alpha = 0.55f),
            1f to com.esan.sportpro.core.theme.PaletaSportPro.Carbon.copy(alpha = 0.92f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
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
                // Tocar el escudo propio abre el once en vivo (solo hay plantel del equipo propio).
                EquipoMarcador(
                    partido.nombreEquipo,
                    ColoresPartido.Azul,
                    Modifier.weight(1f),
                    onClick = if (state.convocados.isNotEmpty()) onVerAlineacion else null,
                )
                Text(
                    "${state.marcador.propio}  –  ${state.marcador.rival}",
                    color = Color.White,
                    style = com.esan.sportpro.core.theme.EstilosSportPro.NumeroGrande.copy(fontSize = 60.sp, lineHeight = 60.sp),
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
                        style = com.esan.sportpro.core.theme.EstilosSportPro.NumeroMediano.copy(fontFeatureSettings = "tnum"),
                    )
                    if (partido.estado == EstadoPartido.EN_CURSO && !partido.enDescanso) {
                        Spacer(Modifier.width(10.dp))
                        Surface(shape = RoundedCornerShape(50), color = com.esan.sportpro.core.theme.PaletaSportPro.Lima) {
                            Text(
                                "min " + ReglasPartido.textoMinuto(
                                    reloj.minuto,
                                    if (partido.inicioSegundoTiempoMs != null) 2 else 1,
                                    partido.duracionTiempoMin,
                                ),
                                color = com.esan.sportpro.core.theme.PaletaSportPro.Carbon,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
                LinearProgressIndicator(
                    progress = { reloj.progreso },
                    color = com.esan.sportpro.core.theme.PaletaSportPro.Lima,
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
                        if (partido.inicioSegundoTiempoMs != null) {
                            "Tiempo añadido: registra \"Fin de partido\" cuando el árbitro pite el final."
                        } else {
                            "Tiempo añadido: registra \"Fin de primer tiempo\" cuando el árbitro pite."
                        },
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
private fun EquipoMarcador(nombre: String, color: Color, modifier: Modifier, onClick: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 4.dp),
    ) {
        Escudo(nombre, color, 64.dp)
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
        if (onClick != null) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.White.copy(alpha = 0.16f),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Text(
                    "Ver once",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Antes del inicio

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreInicio(
    state: RegistroUiState,
    partido: Partido,
    onIniciar: () -> Unit,
    onConfigurarAlineacion: () -> Unit,
) {
    val titulares = state.convocados.filter { it.titular }
    val suplentes = state.convocados.count { !it.titular }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Listo para iniciar", style = MaterialTheme.typography.titleMedium)

            FilaDato(
                icono = if (partido.alineacionConfirmada) {
                    Icons.Default.CheckCircle
                } else {
                    Icons.Default.Warning
                },
                color = if (partido.alineacionConfirmada) {
                    ColoresPartido.Verde
                } else {
                    MaterialTheme.colorScheme.error
                },
                texto = if (partido.alineacionConfirmada) {
                    "Alineación confirmada"
                } else {
                    "Alineación pendiente de confirmar"
                },
            )

            FilaDato(
                Icons.Default.Groups,
                ColoresPartido.Azul,
                "${titulares.size} titulares · $suplentes suplentes",
            )
            FilaDato(
                Icons.Default.SwapHoriz,
                ColoresPartido.Azul,
                "${partido.cambiosPermitidos} cambios permitidos",
            )
            FilaDato(
                Icons.Default.Timer,
                ColoresPartido.Azul,
                "2 tiempos de ${partido.duracionTiempoMin} min · termina al ${partido.duracionTiempoMin * 2}'",
            )

            if (titulares.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    titulares.forEach { j ->
                        Surface(
                            shape = CircleShape,
                            color = ColoresPartido.Verde.copy(alpha = 0.12f),
                            modifier = Modifier.size(34.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "${j.dorsal ?: "?"}",
                                    color = ColoresPartido.Verde,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }

            if (state.puedeCorregir) {

                if (!partido.alineacionConfirmada) {
                    Button(
                        onClick = onConfigurarAlineacion,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColoresPartido.Verde,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                    ) {
                        Icon(
                            Icons.Default.SportsSoccer,
                            contentDescription = null,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Configurar alineación",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Button(
                    onClick = onIniciar,
                    enabled = partido.alineacionConfirmada,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColoresPartido.Verde,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Iniciar partido",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                if (!partido.alineacionConfirmada) {
                    Text(
                        "Confirma la alineación antes de iniciar el partido.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

            } else {
                Text(
                    "El entrenador u operador del partido lo iniciará. Aquí verás el marcador en vivo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                Text("${cantidad(grupos, "grupo", "grupos")} con el mismo tipo, minuto y jugador", style = MaterialTheme.typography.bodyMedium)
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
                    TiposEvento.FIN_PARTIDO -> partido.inicioSegundoTiempoMs != null &&
                            state.reloj.minuto >= partido.duracionTiempoMin * 2
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

/**
 * Acción de la paleta: símbolo del evento en un recuadro de su color, nombre y, si ya se registró
 * en este partido, cuántas veces (p. ej. "×2" en tarjetas amarillas).
 */
@Composable
private fun BotonPaleta(tipo: TipoEvento, cantidad: Int, modifier: Modifier, onClick: () -> Unit) {
    val color = colorTipo(tipo.id)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.22f)),
        shadowElevation = 1.dp,
        modifier = modifier.heightIn(min = 104.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 104.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(color.copy(alpha = 0.04f), color.copy(alpha = 0.14f))
                    )
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(color.copy(alpha = 0.18f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    SimboloEvento(tipoId = tipo.id, tamano = 24.dp, tinta = color)
                }
                Text(
                    tipo.nombre,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
            if (cantidad > 0) {
                Text(
                    "×$cantidad",
                    color = if (tipo.id == TiposEvento.TARJETA_AMARILLA) Color(0xFF1A1A1A) else Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(color, RoundedCornerShape(50))
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                )
            }
            // Franja inferior del color del evento.
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(color)
            )
        }
    }
}

// ---------------------------------------------------------------- Final

@Composable
private fun Finalizado(
    state: RegistroUiState,
    partido: Partido,
    onCerrarActa: () -> Unit,
    onVerActa: () -> Unit,
    onReabrirActa: () -> Unit,
    onResumenIa: () -> Unit,
) {
    val context = LocalContext.current
    val cerrado = partido.estado == EstadoPartido.ACTA_CERRADA
    val acento = if (cerrado) ColoresPartido.Verde else ColoresPartido.Naranja
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, acento.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Encabezado: estado del acta.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = acento.copy(alpha = 0.14f), modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (cerrado) Icons.Default.Lock else Icons.Default.SportsScore,
                            contentDescription = null,
                            tint = acento,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        if (cerrado) "Acta cerrada" else "Partido finalizado",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (cerrado) "Ya no se pueden corregir eventos" else "Revisa y cierra el acta",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Etiqueta(if (cerrado) "Cerrada" else "Por cerrar", acento)
            }

            // Resultado final con los escudos.
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                ) {
                    EquipoResultado(partido.nombreEquipo, ColoresPartido.Azul, Modifier.weight(1f))
                    Text(
                        "${state.marcador.propio} – ${state.marcador.rival}",
                        style = com.esan.sportpro.core.theme.EstilosSportPro.NumeroGrande.copy(fontSize = 36.sp, lineHeight = 36.sp),
                        modifier = Modifier.padding(horizontal = 10.dp),
                    )
                    EquipoResultado(partido.nombreRival, ColoresPartido.Rojo, Modifier.weight(1f))
                }
            }

            state.acta?.let { acta ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DatoActa("Eventos", acta.eventosVigentes.toString(), ColoresPartido.Verde, Modifier.weight(1f))
                    DatoActa("Anulados", acta.eventosAnulados.toString(), ColoresPartido.Gris, Modifier.weight(1f))
                    DatoActa(
                        "Incompletos",
                        acta.eventosIncompletos.toString(),
                        if (acta.eventosIncompletos > 0) ColoresPartido.Naranja else ColoresPartido.Gris,
                        Modifier.weight(1f),
                    )
                }
            }

            if (cerrado) {
                val conIncompletos = partido.actaCerradaConIncompletos ?: 0
                if (conIncompletos > 0) {
                    Aviso(
                        "Se cerró con ${cantidad(conIncompletos, "evento incompleto", "eventos incompletos")}.",
                        ColoresPartido.Naranja,
                    )
                }
                state.acta?.let { acta ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = onVerActa,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Ver acta")
                        }
                        OutlinedButton(
                            onClick = { compartirActa(context, acta) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Compartir")
                        }
                    }
                }
                if (state.puedeResumir) {
                    AccesoResumenIa(publicado = state.resumenPublicado != null, onClick = onResumenIa)
                }
                if (state.evaluacionPendiente) {
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.errorContainer) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Evaluación del resumen pendiente",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Text(
                                "Ya aprobaste el resumen. Registra su utilidad, omisiones, errores y datos inventados.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            OutlinedButton(onClick = onResumenIa, modifier = Modifier.fillMaxWidth()) { Text("Evaluar el resumen") }
                        }
                    }
                }
                if (state.puedeReabrirActa) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            ReglasActa.limiteReaperturaMs(partido)?.let { limite ->
                                Text(
                                    "Administrador: puedes reabrirla hasta el ${formatearFechaHora(limite)}.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        TextButton(onClick = onReabrirActa) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reabrir")
                        }
                    }
                }
            } else {
                // Lo que conviene revisar antes de cerrar.
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PasoCierre(
                        hecho = state.incompletos.isEmpty(),
                        texto = if (state.incompletos.isEmpty()) "Sin eventos incompletos"
                        else "${cantidad(state.incompletos.size, "evento incompleto", "eventos incompletos")} por completar",
                    )
                    PasoCierre(
                        hecho = state.pendientesSync == 0,
                        texto = if (state.pendientesSync == 0) "Todo sincronizado"
                        else "${cantidad(state.pendientesSync, "evento", "eventos")} por sincronizar",
                    )
                    PasoCierre(hecho = false, texto = "Revisa la cronología antes de cerrar", neutro = true)
                }
                if (state.puedeCorregir) {
                    Button(
                        onClick = onCerrarActa,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ColoresPartido.VerdeOscuro),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Cerrar acta del partido", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EquipoResultado(nombre: String, color: Color, modifier: Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Escudo(nombre, color, 40.dp)
        Text(
            nombre,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun DatoActa(etiqueta: String, valor: String, color: Color, modifier: Modifier) {
    Surface(shape = RoundedCornerShape(14.dp), color = color.copy(alpha = 0.10f), modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 10.dp)) {
            Text(valor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = color)
            Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Aviso(texto: String, color: Color) {
    Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.12f)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(texto, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PasoCierre(hecho: Boolean, texto: String, neutro: Boolean = false) {
    val color = when {
        neutro -> MaterialTheme.colorScheme.onSurfaceVariant
        hecho -> ColoresPartido.Verde
        else -> ColoresPartido.Naranja
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            when {
                neutro -> Icons.Default.Checklist
                hecho -> Icons.Default.CheckCircle
                else -> Icons.Default.Warning
            },
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(texto, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Acceso destacado al resumen con IA, con su estado (publicado o por generar/revisar). */
@Composable
private fun AccesoResumenIa(publicado: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = ColoresPartido.VerdeOscuro,
        contentColor = Color.White,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Surface(shape = CircleShape, color = com.esan.sportpro.core.theme.PaletaSportPro.Lima, modifier = Modifier.size(38.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = com.esan.sportpro.core.theme.PaletaSportPro.Carbon,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text("Resumen con IA", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    if (publicado) "Publicado · lo ven los jugadores y apoderados del equipo"
                    else "Genera un borrador, revísalo y publícalo",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
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
    /** Minuto ya formateado (p. ej. "45+3'"). */
    textoMinuto: String = "${evento.minuto}'",
) {
    val anulado = evento.estado == EstadoEvento.ANULADO
    var menu by remember { mutableStateOf(false) }
    val lineaColor = MaterialTheme.colorScheme.outlineVariant
    val esRival = evento.equipo == EquipoEvento.RIVAL
    // El 2.º tiempo no puede quedar "fuera de orden": empieza siempre en su minuto reglamentario.
    // Partidos antiguos lo marcaban así cuando el 1.er tiempo tuvo tiempo añadido.
    val fueraDeOrden = evento.fueraDeOrden && evento.tipoId != TiposEvento.INICIO_SEGUNDO_TIEMPO

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                // Línea vertical de la línea de tiempo, a la altura del ícono.
                val x = 64.dp.toPx() + 18.dp.toPx()
                drawLine(
                    color = lineaColor,
                    start = Offset(x, 36.dp.toPx()),
                    end = Offset(x, if (esUltimo) 36.dp.toPx() else size.height + 14.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                )
            },
    ) {
        Text(
            textoMinuto,
            style = if (textoMinuto.length > 4) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier
                .width(56.dp)
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
                    if (anulado || evento.incompleto || fueraDeOrden || evento.excedeCambios || evento.version > 1) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 6.dp),
                        ) {
                            if (anulado) Etiqueta("Anulado", ColoresPartido.Gris)
                            if (evento.incompleto && !anulado) Etiqueta("Incompleto", ColoresPartido.Naranja)
                            if (fueraDeOrden) Etiqueta("Fuera de orden", ColoresPartido.Morado)
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

