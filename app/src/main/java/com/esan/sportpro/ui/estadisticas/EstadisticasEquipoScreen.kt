package com.esan.sportpro.ui.estadisticas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import com.esan.sportpro.core.theme.EstilosSportPro
import com.esan.sportpro.core.theme.PaletaSportPro
import com.esan.sportpro.domain.estadisticas.EstadisticaEquipo
import com.esan.sportpro.domain.estadisticas.EstadisticaJugador
import com.esan.sportpro.domain.estadisticas.RankingsJugadores
import com.esan.sportpro.domain.estadisticas.ResumenPartidos
import com.esan.sportpro.ui.common.CargandoSportPro
import com.esan.sportpro.ui.common.EscudoEquipo
import com.esan.sportpro.ui.common.EstadoVacioModulo
import com.esan.sportpro.ui.common.FondoFotoSportPro
import com.esan.sportpro.ui.common.FotoSportPro

/**
 * US-026 — Estadísticas del equipo. Rediseño estilo infografía de partido: cabecera con escudo y
 * récord, secciones en pastilla, anillos de porcentaje, goles por tramo de minutos y podio de
 * goleadores. Los números son los mismos de antes (no cambia cómo se calculan).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadisticasEquipoScreen(
    onVerJugadores: () -> Unit,
    onAbrirFiltros: () -> Unit,
    onSeleccionarJugador: (EstadisticaJugador) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: EstadisticasViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val margen = Modifier.padding(horizontal = 16.dp)

    Scaffold(modifier = modifier) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Selector de equipo: las estadísticas son siempre de un equipo real de la academia.
            if (state.equipos.size > 1) {
                com.esan.sportpro.ui.common.SelectorEquipo(
                    equipos = state.equipos,
                    seleccionadoId = state.equipoSeleccionadoId,
                    onSeleccionar = { id -> id?.let(viewModel::seleccionarEquipo) },
                    permitirTodos = false,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                )
            }
            when {
                state.isLoading -> CargandoSportPro(modifier = Modifier.weight(1f))

                state.sinEquipos -> EstadoVacioModulo(
                    titulo = "Sin equipos",
                    mensaje = "Las estadísticas aparecen cuando tu academia tiene equipos registrados" +
                        " (para un padre, cuando su hijo está vinculado).",
                    icono = Icons.Default.Groups,
                )

                state.sinPartidosEnPeriodo -> Column(Modifier.padding(16.dp)) {
                    CabeceraEquipo(state, onAbrirFiltros)
                    EstadoVacioModulo(
                        titulo = "Aún no hay partidos",
                        mensaje = "No hay partidos registrados en este periodo",
                        icono = Icons.Default.Groups,
                    )
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item { CabeceraEquipo(state, onAbrirFiltros, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) }
                    if (state.filtro.tieneFiltrosActivos) {
                        item {
                            Text(
                                "Filtros activos: los números corresponden al periodo elegido.",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = margen,
                            )
                        }
                    }

                    item { EtiquetaSeccion("General", modifier = margen.padding(top = 8.dp)) }
                    item { SeccionGeneral(state.resumenEquipo, state.jugadoresReales, margen) }

                    item { EtiquetaSeccion("Rendimiento", modifier = margen.padding(top = 8.dp)) }
                    item { SeccionRendimiento(state.resumenEquipo, state.jugadoresReales, margen) }

                    val tramos = ResumenPartidos.golesPorTramo(state.resumenPartidos.minutosDeGol)
                    if (tramos.sum() > 0) {
                        item { EtiquetaSeccion("¿Cuándo marcamos?", modifier = margen.padding(top = 8.dp)) }
                        item {
                            PanelEstadistica(margen) {
                                Column(Modifier.padding(16.dp)) {
                                    BarrasPorTramo(tramos, ResumenPartidos.TRAMOS.map { it.first })
                                    Text(
                                        "Minuto de cada gol registrado en el acta (${tramos.sum()} goles).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 8.dp),
                                    )
                                }
                            }
                        }
                    }

                    if (state.golesUltimosPartidos.isNotEmpty()) {
                        item { EtiquetaSeccion("Goles por partido", modifier = margen.padding(top = 8.dp)) }
                        item {
                            PanelEstadistica(margen) {
                                GraficoGolesBarChart(puntos = state.golesUltimosPartidos, modifier = Modifier.padding(16.dp))
                            }
                        }
                    }

                    val goleadores = RankingsJugadores.top(state.jugadores) { it.goles.toDouble() }
                    if (goleadores.isNotEmpty()) {
                        item { EtiquetaSeccion("Goleadores", modifier = margen.padding(top = 8.dp)) }
                        item { PodioGoleadores(goleadores, onSeleccionarJugador, margen) }
                    }

                    item { EtiquetaSeccion("Rankings", modifier = margen.padding(top = 8.dp)) }
                    item { Box(margen) { SeccionRankingsEquipo(state.jugadores, onSeleccionarJugador) } }

                    item {
                        PanelEstadistica(margen.padding(bottom = 16.dp).clickable(onClick = onVerJugadores)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.Groups, contentDescription = null)
                                Text(
                                    "Ver tabla de jugadores",
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(start = 10.dp).weight(1f),
                                )
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Cabecera oscura: escudo, nombre, récord G-E-P en números grandes y los últimos 5 resultados. */
@Composable
private fun CabeceraEquipo(state: EstadisticasUiState, onAbrirFiltros: () -> Unit, modifier: Modifier = Modifier) {
    val equipo = state.equipoSeleccionado
    val r = state.resumenEquipo
    val ultimos = state.golesUltimosPartidos.takeLast(5).map {
        when {
            it.golesEquipo > it.golesRival -> 'G'
            it.golesEquipo < it.golesRival -> 'P'
            else -> 'E'
        }
    }
    FondoFotoSportPro(
        foto = FotoSportPro.ESTADIO,
        forma = RoundedCornerShape(26.dp),
        velo = androidx.compose.ui.graphics.Brush.verticalGradient(
            0f to PaletaSportPro.Carbon.copy(alpha = 0.72f),
            1f to PaletaSportPro.Carbon.copy(alpha = 0.95f),
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EscudoEquipo(
                    nombre = equipo?.nombre ?: "Equipo",
                    tamano = 56.dp,
                    url = equipo?.escudoUrl?.ifBlank { null },
                    color = equipo?.colorUniforme,
                )
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        equipo?.nombre ?: "Mi equipo",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        listOfNotNull(equipo?.categoria?.ifBlank { null }, state.filtro.temporada?.let { "Temporada $it" })
                            .joinToString(" · ")
                            .ifBlank { "Todas las temporadas" },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f),
                    )
                }
                IconButton(onClick = onAbrirFiltros) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filtros", tint = Color.White)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                CifraRecord("${r.partidosJugados}", "Jugados")
                CifraRecord("${r.ganados}", "Ganados", PaletaSportPro.Lima)
                CifraRecord("${r.empatados}", "Empates")
                CifraRecord("${r.perdidos}", "Perdidos")
            }
            if (ultimos.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 14.dp),
                ) {
                    Text("Últimos ${ultimos.size}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.75f))
                    Spacer(Modifier.width(2.dp))
                    ultimos.forEach { PuntoResultado(it) }
                }
            }
        }
    }
}

@Composable
private fun CifraRecord(valor: String, etiqueta: String, color: Color = Color.White) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valor, style = EstilosSportPro.NumeroGrande, color = color)
        Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.75f))
    }
}

/** Goles, diferencia, promedio y tarjetas del equipo en dos columnas. */
@Composable
private fun SeccionGeneral(r: EstadisticaEquipo, jugadores: List<EstadisticaJugador>, modifier: Modifier) {
    val amarillas = jugadores.sumOf { it.tarjetasAmarillas }
    val rojas = jugadores.sumOf { it.tarjetasRojas }
    PanelEstadistica(modifier) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            FilaGeneral("Goles a favor", "${r.golesFavor}", "Goles en contra", "${r.golesContra}")
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            FilaGeneral(
                "Diferencia", if (r.diferenciaGol > 0) "+${r.diferenciaGol}" else "${r.diferenciaGol}",
                "Prom. goles", "%.1f".format(r.promedioGolesPorPartido),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Amarillas", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                TarjetaArbitro(amarillas, roja = false, alto = 30.dp)
                Spacer(Modifier.width(24.dp))
                Text("Rojas", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                TarjetaArbitro(rojas, roja = true, alto = 30.dp)
            }
        }
    }
}

@Composable
private fun FilaGeneral(etiqueta1: String, valor1: String, etiqueta2: String, valor2: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(etiqueta1, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(valor1, style = EstilosSportPro.NumeroMediano.copy(fontSize = 20.sp, lineHeight = 20.sp))
        Spacer(Modifier.width(24.dp))
        Text(etiqueta2, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(valor2, style = EstilosSportPro.NumeroMediano.copy(fontSize = 20.sp, lineHeight = 20.sp))
    }
}

/** Tres anillos: % de victorias, asistencia media a entrenos y goles por partido (sobre 3). */
@Composable
private fun SeccionRendimiento(r: EstadisticaEquipo, jugadores: List<EstadisticaJugador>, modifier: Modifier) {
    val victorias = if (r.partidosJugados > 0) r.ganados.toFloat() / r.partidosJugados else 0f
    val conAsistencia = jugadores.filter { it.porcentajeAsistenciaEntrenamientos > 0.0 }
    val asistencia = if (conAsistencia.isEmpty()) 0.0 else conAsistencia.sumOf { it.porcentajeAsistenciaEntrenamientos } / conAsistencia.size
    PanelEstadistica(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            AnilloPorcentaje(victorias, "${(victorias * 100).toInt()}%", "Victorias", Modifier.weight(1f))
            AnilloPorcentaje(
                (asistencia / 100).toFloat(), if (conAsistencia.isEmpty()) "—" else "${asistencia.toInt()}%",
                "Asist. a entrenos", Modifier.weight(1f), color = com.esan.sportpro.core.theme.SportProTema.colores.exito,
            )
            AnilloPorcentaje(
                (r.promedioGolesPorPartido / 3.0).toFloat(), "%.1f".format(r.promedioGolesPorPartido),
                "Goles por partido", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** Podio de los 3 goleadores: el primero al centro y más alto, con foto. */
@Composable
private fun PodioGoleadores(
    goleadores: List<EstadisticaJugador>,
    onSeleccionar: (EstadisticaJugador) -> Unit,
    modifier: Modifier,
) {
    // Orden visual del podio: 2.º, 1.º, 3.º.
    val orden = listOfNotNull(goleadores.getOrNull(1), goleadores.getOrNull(0), goleadores.getOrNull(2))
    PanelEstadistica(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, start = 12.dp, end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.Bottom,
        ) {
            orden.forEach { jugador ->
                val puesto = goleadores.indexOf(jugador)
                val alto = when (puesto) { 0 -> 72.dp; 1 -> 52.dp; else -> 38.dp }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f).clickable { onSeleccionar(jugador) },
                ) {
                    FotoCirculo(jugador, if (puesto == 0) 70.dp else 56.dp)
                    Text(
                        jugador.jugadorNombre.split(" ").take(2).joinToString(" "),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Box(
                        contentAlignment = Alignment.TopCenter,
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .fillMaxWidth()
                            .height(alto)
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                            .background(PaletaSportPro.Musgo),
                    ) {
                        Text(
                            "${jugador.goles}",
                            style = EstilosSportPro.NumeroMediano,
                            color = PaletaSportPro.Lima,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Foto circular con anillo lima: la subida, la de demostración o las iniciales. */
@Composable
internal fun FotoCirculo(jugador: EstadisticaJugador, tamano: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(tamano)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(PaletaSportPro.Lima)
            .padding(3.dp)
            .clip(androidx.compose.foundation.shape.CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        AvatarEstadistica(jugador, tamano = tamano - 6.dp)
    }
}
