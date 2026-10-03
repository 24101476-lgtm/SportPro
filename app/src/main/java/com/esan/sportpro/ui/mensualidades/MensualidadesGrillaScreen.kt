package com.esan.sportpro.ui.mensualidades

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.mensualidades.EstadoMensualidad
import com.esan.sportpro.navigation.UserRole
import com.esan.sportpro.ui.common.RegistroSimuladoLegend
import java.time.format.TextStyle
import java.util.Locale

/**
 * US-008 — Frame 37_Mensualidades_Grilla.
 * Administrador: ve montos completos. Entrenador: solo el indicador de estado, sin montos
 * (criterio de aceptación 7, resuelto aquí con [MensualidadesGrillaUiState.rolActual]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MensualidadesGrillaScreen(
    onNuevoRegistro: () -> Unit,
    onVerHistorial: (jugadorId: String, jugadorNombre: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MensualidadesGrillaViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MensualidadesGrillaEvent.AbrirRegistrar -> onNuevoRegistro()
                is MensualidadesGrillaEvent.AbrirHistorial -> {
                    val encontrada = state.mensualidades.firstOrNull { it.mensualidad.jugadorId == event.jugadorId }
                    onVerHistorial(event.jugadorId, encontrada?.mensualidad?.jugadorNombre.orEmpty())
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Mensualidades") }) },
        floatingActionButton = {
            if (state.rolActual == UserRole.ADMINISTRADOR) {
                FloatingActionButton(onClick = viewModel::onNuevoRegistro) {
                    Icon(Icons.Default.Add, contentDescription = "Registrar pago")
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            RegistroSimuladoLegend()

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = {
                    val nuevoMes = if (state.mes == 1) 12 else state.mes - 1
                    val nuevoAnio = if (state.mes == 1) state.anio - 1 else state.anio
                    viewModel.onMesAnioChanged(nuevoMes, nuevoAnio)
                }) { Icon(Icons.Default.ChevronLeft, contentDescription = "Mes anterior") }

                val nombreMes = java.time.Month.of(state.mes).getDisplayName(TextStyle.FULL, Locale("es"))
                Text(
                    text = "${nombreMes.replaceFirstChar { it.uppercase() }} ${state.anio}",
                    style = MaterialTheme.typography.titleMedium,
                )

                IconButton(onClick = {
                    val nuevoMes = if (state.mes == 12) 1 else state.mes + 1
                    val nuevoAnio = if (state.mes == 12) state.anio + 1 else state.anio
                    viewModel.onMesAnioChanged(nuevoMes, nuevoAnio)
                }) { Icon(Icons.Default.ChevronRight, contentDescription = "Mes siguiente") }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EstadoMensualidad.entries.forEach { estado ->
                    FilterChip(
                        selected = state.estadoFiltro == estado,
                        onClick = {
                            viewModel.onFiltroEstadoChanged(if (state.estadoFiltro == estado) null else estado)
                        },
                        label = { Text(estado.etiqueta) },
                    )
                }
            }

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.mensualidades.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aún no hay mensualidades registradas en este periodo", style = MaterialTheme.typography.bodyMedium)
                }
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.mensualidades, key = { it.mensualidad.id }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { viewModel.onJugadorSeleccionado(item.mensualidad.jugadorId) },
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(item.mensualidad.jugadorNombre, style = MaterialTheme.typography.titleSmall)
                                    Text(item.mensualidad.equipoNombre, style = MaterialTheme.typography.bodySmall)
                                    if (state.rolActual == UserRole.ADMINISTRADOR) {
                                        Text(
                                            text = "S/ %.2f".format(item.mensualidad.montoReferencial),
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    }
                                }
                                EstadoMensualidadChip(estado = item.estadoEfectivo)
                            }
                        }
                    }
                }
            }
        }
    }
}
