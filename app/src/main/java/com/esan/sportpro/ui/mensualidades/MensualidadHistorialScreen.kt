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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.ui.common.RegistroSimuladoLegend
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * US-008 — Frame 39_Mensualidad_Historial. Lista todos los registros de un jugador (vigentes
 * y anulados) para dejar trazable cualquier corrección (criterio de aceptación 6).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MensualidadHistorialScreen(
    jugadorId: String,
    jugadorNombre: String,
    modifier: Modifier = Modifier,
    viewModel: MensualidadHistorialViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val formato = remember { SimpleDateFormat("dd/MM/yyyy", Locale("es")) }

    LaunchedEffect(jugadorId) { viewModel.cargar(jugadorId, jugadorNombre) }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Historial · ${state.jugadorNombre}") }) },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.registros.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Sin historial de mensualidades", style = MaterialTheme.typography.bodyMedium)
            }
            else -> LazyColumn(
                modifier = Modifier.padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { RegistroSimuladoLegend(modifier = Modifier.padding(bottom = 8.dp)) }
                items(state.registros, key = { it.id }) { registro ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "${registro.mes.toString().padStart(2, '0')}/${registro.anio}",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                EstadoMensualidadChip(estado = registro.estadoRegistrado)
                            }
                            Text("S/ %.2f · %s".format(registro.montoReferencial, registro.medioDeclarado.etiqueta))
                            Text(
                                text = "Registrado: ${formato.format(registro.fechaRegistro)}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            if (!registro.observacion.isNullOrBlank()) {
                                Text(
                                    text = "Obs: ${registro.observacion}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            if (!registro.vigente) {
                                Text(
                                    text = "Anulado — corregido por un registro posterior",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFD62828),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
