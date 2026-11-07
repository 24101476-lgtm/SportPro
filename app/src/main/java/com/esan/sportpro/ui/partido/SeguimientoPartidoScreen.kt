package com.esan.sportpro.ui.partido

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.partido.EstadoPartido
import com.esan.sportpro.ui.ia.ResumenPublicado
import com.esan.sportpro.domain.partido.EventoPartido
import com.esan.sportpro.domain.partido.JugadorEnAlineacion
import com.esan.sportpro.domain.partido.Partido
import com.esan.sportpro.domain.partido.ReglasPartido
import com.esan.sportpro.domain.partido.SituacionJugador
import com.esan.sportpro.navigation.UserRole
import kotlinx.coroutines.delay

private val TITULOS_PESTANAS = listOf("Cronología", "Alineación", "Estadísticas", "Resumen")

/**
 * US-021: seguimiento del partido en tiempo real para jugador y padre de familia. Es de solo
 * lectura: no hay ningún control para registrar, editar ni anular eventos.
 */
@Composable
fun SeguimientoPartidoScreen(
    onVolver: () -> Unit,
    viewModel: SeguimientoPartidoViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (!state.conectado) BannerUltimaInformacion(state.ultimaActualizacionMs)

        val partido = state.partido
        when (state.acceso) {
            AccesoSeguimiento.CARGANDO -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            AccesoSeguimiento.NO_ENCONTRADO -> MensajeSeguimiento(
                titulo = "No se encontró el partido",
                detalle = state.error ?: "Puede que se haya eliminado o que no tengas conexión.",
                onVolver = onVolver,
            )

            AccesoSeguimiento.DENEGADO -> MensajeSeguimiento(
                titulo = "No puedes seguir este partido",
                detalle = if (state.perfil?.rol == UserRole.PADRE_DE_FAMILIA) {
                    "Solo ves los partidos de los equipos de tus jugadores con vínculo aprobado."
                } else {
                    "Solo ves los partidos de tu equipo."
                },
                onVolver = onVolver,
            )

            AccesoSeguimiento.PERMITIDO -> partido?.let { Contenido(state, it, onVolver) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Contenido(state: SeguimientoUiState, partido: Partido, onVolver: () -> Unit) {
    var pestana by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        Cabecera(state, partido, onVolver, modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp))

        TabRow(
            selectedTabIndex = pestana,
            containerColor = Color.Transparent,
            contentColor = ColoresPartido.Verde,
        ) {
            TITULOS_PESTANAS.forEachIndexed { indice, titulo ->
                Tab(
                    selected = pestana == indice,
                    onClick = { pestana = indice },
                    selectedContentColor = ColoresPartido.Verde,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = {
                        Text(titulo, fontWeight = if (pestana == indice) FontWeight.Bold else FontWeight.Normal)
                    },
                )
            }
        }

        val modifier = Modifier.weight(1f)
        when (pestana) {
            0 -> PestanaCronologia(state, partido, modifier)
            1 -> PestanaAlineacion(state, partido, modifier)
            2 -> PestanaEstadisticas(state, partido, modifier)
            else -> PestanaResumen(state, partido, modifier)
        }
    }
}

// ---------------------------------------------------------------- Cabecera

@Composable
private fun Cabecera(state: SeguimientoUiState, partido: Partido, onVolver: () -> Unit, modifier: Modifier = Modifier) {
    val reloj = state.reloj
    val marcador = state.marcador
    val goles = marcador.propio + marcador.rival

    // Resaltado temporal del marcador cuando entra un gol (criterio 3). No se dispara con la
    // carga inicial ni cuando un gol anulado baja el marcador.
    var golesPrevios by remember { mutableStateOf<Int?>(null) }
    var golNuevo by remember { mutableIntStateOf(0) }
    var resaltado by remember { mutableStateOf(false) }
    LaunchedEffect(goles, state.eventosCargados) {
        if (!state.eventosCargados) return@LaunchedEffect
        val previos = golesPrevios
        golesPrevios = goles
        if (previos != null && goles > previos) golNuevo++
    }
    LaunchedEffect(golNuevo) {
        if (golNuevo == 0) return@LaunchedEffect
        resaltado = true
        delay(3_000)
        resaltado = false
    }
    val escala by animateFloatAsState(if (resaltado) 1.18f else 1f, animationSpec = tween(300), label = "escalaMarcador")
    val colorMarcador by animateColorAsState(
        if (resaltado) com.esan.sportpro.core.theme.PaletaSportPro.Lima else Color.White,
        animationSpec = tween(300),
        label = "colorMarcador",
    )

    com.esan.sportpro.ui.common.FondoFotoSportPro(
        foto = com.esan.sportpro.ui.common.FotoSportPro.ESTADIO,
        forma = RoundedCornerShape(28.dp),
        velo = Brush.verticalGradient(
            0f to com.esan.sportpro.core.theme.PaletaSportPro.MusgoProfundo.copy(alpha = 0.55f),
            1f to com.esan.sportpro.core.theme.PaletaSportPro.Carbon.copy(alpha = 0.92f),
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
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
                    .padding(top = 4.dp, bottom = 2.dp),
            ) {
                EquipoEncabezado(partido.nombreEquipo, ColoresPartido.Azul, Modifier.weight(1f))
                Text(
                    "${marcador.propio}  –  ${marcador.rival}",
                    color = colorMarcador,
                    style = com.esan.sportpro.core.theme.EstilosSportPro.NumeroGrande.copy(fontSize = 52.sp, lineHeight = 52.sp),
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .scale(escala),
                )
                EquipoEncabezado(partido.nombreRival, ColoresPartido.Rojo, Modifier.weight(1f))
            }

            AnimatedVisibility(
                visible = resaltado,
                enter = fadeIn() + scaleIn(),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Surface(shape = RoundedCornerShape(50), color = com.esan.sportpro.core.theme.PaletaSportPro.Lima) {
                    Text(
                        "¡GOL!",
                        color = com.esan.sportpro.core.theme.PaletaSportPro.Carbon,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    )
                }
            }

            if (partido.estado == EstadoPartido.PROGRAMADO) {
                val cuando = listOfNotNull(partido.fechaMs?.let { formatearFecha(it) }, partido.lugar?.takeIf { it.isNotBlank() })
                Text(
                    cuando.joinToString(" · ").ifEmpty { "Aún no comienza" },
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
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
                if (partido.estado == EstadoPartido.EN_CURSO) {
                    LinearProgressIndicator(
                        progress = { reloj.progreso },
                        color = com.esan.sportpro.core.theme.PaletaSportPro.Lima,
                        trackColor = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 4.dp)
                            .height(6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EquipoEncabezado(nombre: String, color: Color, modifier: Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Escudo(nombre, color, 56.dp)
        Text(
            nombre,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

// ---------------------------------------------------------------- Cronología

@Composable
private fun PestanaCronologia(state: SeguimientoUiState, partido: Partido, modifier: Modifier) {
    val cronologia = state.cronologia
    val ids = cronologia.map { it.id }.toSet()

    // Los eventos que ya estaban al abrir la pantalla no se animan; solo los que llegan después.
    var conocidos by remember { mutableStateOf<Set<String>?>(null) }
    LaunchedEffect(state.eventosCargados, ids) {
        if (!state.eventosCargados) return@LaunchedEffect
        if (conocidos == null) {
            conocidos = ids
        } else {
            delay(1_500)
            conocidos = ids
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (cronologia.isEmpty()) {
            item {
                TextoSecundario(
                    if (partido.estado == EstadoPartido.PROGRAMADO) {
                        "El partido aún no comienza. Verás los eventos aquí apenas empiece."
                    } else {
                        "Aún no hay eventos registrados."
                    },
                )
            }
        }
        items(cronologia, key = { it.id }) { evento ->
            val nuevo = conocidos?.let { evento.id !in it } == true
            val visible = remember(evento.id) { MutableTransitionState(!nuevo).apply { targetState = true } }
            AnimatedVisibility(
                visibleState = visible,
                enter = fadeIn(animationSpec = tween(400)) + slideInVertically(animationSpec = tween(400)) { it / 2 },
            ) {
                FilaEvento(evento, state.eventos, partido)
            }
        }
    }
}

@Composable
private fun FilaEvento(evento: EventoPartido, todos: List<EventoPartido>, partido: Partido) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            minutoEvento(evento, todos, partido),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.width(56.dp),
        )
        Spacer(Modifier.width(8.dp))
        IconoEvento(evento.tipoId, 36.dp)
        Spacer(Modifier.width(10.dp))
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.weight(1f),
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(evento.tipoNombre, fontWeight = FontWeight.Bold)
                val detalle = detalleEvento(evento, partido)
                if (detalle.isNotBlank()) {
                    Text(
                        detalle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Resumen (US-024)

/** Resumen narrativo aprobado por el entrenador; mientras no exista uno, la pestaña lo indica. */
@Composable
private fun PestanaResumen(state: SeguimientoUiState, partido: Partido, modifier: Modifier) {
    val resumen = state.resumenPublicado
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 32.dp),
    ) {
        if (resumen == null) {
            TextoSecundario(
                if (partido.estado == EstadoPartido.ACTA_CERRADA) {
                    "El entrenador todavía no publicó el resumen de este partido."
                } else {
                    "El resumen aparecerá aquí cuando el partido termine y el entrenador lo publique."
                },
            )
        } else {
            ResumenPublicado(resumen, partido)
        }
    }
}

// ---------------------------------------------------------------- Alineación

@Composable
private fun PestanaAlineacion(state: SeguimientoUiState, partido: Partido, modifier: Modifier) {
    val alineacion = state.alineacion
    val titulares = alineacion.filter { it.jugador.titular }
    val suplentes = alineacion.filterNot { it.jugador.titular }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (alineacion.isEmpty()) {
            item { TextoSecundario("La alineación de ${partido.nombreEquipo} aún no está disponible.") }
        } else {
            item {
                TextoSecundario(
                    if (partido.alineacionConfirmada) "Alineación confirmada" else "Alineación pendiente de confirmar",
                )
            }
            item { Subtitulo("Titulares") }
            items(titulares, key = { it.jugador.id }) { FilaJugador(it) }
            if (suplentes.isNotEmpty()) {
                item { Subtitulo("Suplentes") }
                items(suplentes, key = { it.jugador.id }) { FilaJugador(it) }
            }
        }
    }
}

@Composable
private fun FilaJugador(item: JugadorEnAlineacion) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Surface(shape = CircleShape, color = ColoresPartido.Verde.copy(alpha = 0.12f), modifier = Modifier.size(36.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text("${item.jugador.dorsal ?: "?"}", color = ColoresPartido.Verde, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            item.jugador.nombre,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Etiqueta(
            texto = item.situacion.etiqueta,
            color = when (item.situacion) {
                SituacionJugador.EN_CANCHA -> ColoresPartido.Verde
                SituacionJugador.SUSTITUIDO -> ColoresPartido.Naranja
                SituacionJugador.EXPULSADO -> ColoresPartido.Rojo
                SituacionJugador.INGRESO -> ColoresPartido.Azul
                SituacionJugador.EN_BANCA -> ColoresPartido.Gris
            },
        )
    }
}

// ---------------------------------------------------------------- Estadísticas

@Composable
private fun PestanaEstadisticas(state: SeguimientoUiState, partido: Partido, modifier: Modifier) {
    val e = state.estadisticas

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            partido.nombreEquipo,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            partido.nombreRival,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    FilaComparacion("Posesión de eventos", e.propio.posesionPct?.let { "$it %" } ?: "—", e.rival.posesionPct?.let { "$it %" } ?: "—")
                    e.propio.posesionPct?.let { pct ->
                        LinearProgressIndicator(
                            progress = { pct / 100f },
                            color = ColoresPartido.Azul,
                            trackColor = ColoresPartido.Rojo,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                        )
                    }
                    FilaComparacion("Tiros", e.propio.tiros.toString(), e.rival.tiros.toString())
                    FilaComparacion("Faltas", e.propio.faltas.toString(), e.rival.faltas.toString())
                    FilaComparacion("Tarjetas amarillas", e.propio.amarillas.toString(), e.rival.amarillas.toString())
                    FilaComparacion("Tarjetas rojas", e.propio.rojas.toString(), e.rival.rojas.toString())
                }
            }
        }
        item {
            TextoSecundario(
                "La posesión reparte los eventos de juego registrados (sin cambios ni controles de tiempo). " +
                    "Los tiros incluyen goles y penales.",
            )
        }
    }
}

@Composable
private fun FilaComparacion(titulo: String, propio: String, rival: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            titulo,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(propio, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(
                rival,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ---------------------------------------------------------------- Comunes

@Composable
private fun Subtitulo(texto: String) {
    Text(texto, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun TextoSecundario(texto: String) {
    Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun MensajeSeguimiento(titulo: String, detalle: String, onVolver: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(titulo, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            detalle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 12.dp),
        )
        Button(onClick = onVolver) { Text("Volver") }
    }
}

/** Criterio 5: sin red se sigue mostrando lo último recibido, con la hora de esa actualización. */
@Composable
private fun BannerUltimaInformacion(ultimaActualizacionMs: Long?) {
    Surface(color = Color(0xFFB71C1C), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.WifiOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    "Mostrando la última información recibida",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Última actualización: " + (ultimaActualizacionMs?.let { formatearFechaHora(it) } ?: "sin hora disponible"),
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
