package com.esan.sportpro.ui.estadisticas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.estadisticas.EstadisticaEquipo
import com.esan.sportpro.ui.common.EmptyStateMessage

/** US-026 — Frame 121_Estadisticas_Equipo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadisticasEquipoScreen(
    onVerJugadores: () -> Unit,
    onAbrirFiltros: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EstadisticasViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Estadísticas del equipo") },
                actions = {
                    IconButton(onClick = onAbrirFiltros) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filtros")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator() }

            state.sinPartidosEnPeriodo -> EmptyStateMessage(
                mensaje = "Aún no hay partidos registrados en este periodo",
                modifier = Modifier.padding(padding),
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    if (state.filtro.tieneFiltrosActivos) {
                        Text(
                            text = "Filtros activos",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                item { ResumenEquipoGrid(state.resumenEquipo) }
                item {
                    if (state.golesUltimosPartidos.isNotEmpty()) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            GraficoGolesBarChart(
                                puntos = state.golesUltimosPartidos,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onVerJugadores,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Groups, contentDescription = null)
                                Text(
                                    text = "Ver tabla de jugadores",
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResumenEquipoGrid(resumen: EstadisticaEquipo) {
    val filas = listOf(
        "Partidos jugados" to resumen.partidosJugados.toString(),
        "Ganados" to resumen.ganados.toString(),
        "Empatados" to resumen.empatados.toString(),
        "Perdidos" to resumen.perdidos.toString(),
        "Goles a favor" to resumen.golesFavor.toString(),
        "Goles en contra" to resumen.golesContra.toString(),
        "Diferencia de gol" to resumen.diferenciaGol.toString(),
        "Promedio goles/partido" to "%.2f".format(resumen.promedioGolesPorPartido),
    )
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            filas.chunked(2).forEach { par ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    par.forEach { (etiqueta, valor) ->
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Text(text = valor, style = MaterialTheme.typography.headlineSmall)
                            Text(
                                text = etiqueta,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
