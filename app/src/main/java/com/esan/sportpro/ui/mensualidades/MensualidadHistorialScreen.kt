package com.esan.sportpro.ui.mensualidades

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.esan.sportpro.core.fecha.FechasSportPro
import com.esan.sportpro.ui.common.BarraSuperiorSportPro
import com.esan.sportpro.ui.common.RegistroSimuladoLegend
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

/**
 * US-008 — Frame 39_Mensualidad_Historial. Lista todos los registros de un jugador (vigentes
 * y anulados) para dejar trazable cualquier corrección (criterio de aceptación 6). Muestra además
 * el jugador, su equipo y un resumen por estado; los montos solo los ve el administrador.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MensualidadHistorialScreen(
    jugadorId: String,
    jugadorNombre: String,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MensualidadHistorialViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val formato = remember { FechasSportPro.formatoFecha() }

    LaunchedEffect(jugadorId) { viewModel.cargar(jugadorId, jugadorNombre) }
    // El botón "atrás" del sistema vuelve a la grilla en vez de cerrar la app.
    BackHandler(onBack = onVolver)

    Scaffold(
        modifier = modifier,
        topBar = {
            BarraSuperiorSportPro(
                title = { Text("Historial de mensualidades") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.errorMessage != null -> Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                Text(state.errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
            }
            else -> LazyColumn(
                modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { RegistroSimuladoLegend(modifier = Modifier.padding(bottom = 4.dp)) }
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(state.jugadorNombre.ifBlank { "Jugador" }, style = MaterialTheme.typography.titleMedium)
                            Text(
                                state.equipoNombre.ifBlank { "Sin equipo asignado" },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            val r = state.resumen
                            Text(
                                "Pagadas ${r.pagadas} · Pendientes ${r.pendientes} · Vencidas ${r.vencidas} · Exoneradas ${r.exoneradas}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            if (state.puedeVerMontos) {
                                Text(
                                    "Total pagado (referencial): S/ %.2f".format(r.totalPagado),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
                if (state.registros.isEmpty()) {
                    item {
                        Text(
                            "Este jugador aún no tiene pagos registrados.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 16.dp),
                        )
                    }
                }
                items(state.registros, key = { it.id }) { registro ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val mes = Month.of(registro.mes.coerceIn(1, 12))
                                    .getDisplayName(TextStyle.FULL, Locale("es"))
                                    .replaceFirstChar { it.uppercase() }
                                Text("$mes ${registro.anio}", style = MaterialTheme.typography.titleSmall)
                                EstadoMensualidadChip(estado = registro.estadoRegistrado)
                            }
                            if (state.puedeVerMontos) {
                                Text("S/ %.2f · %s".format(registro.montoReferencial, registro.medioDeclarado.etiqueta))
                            }
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
                            } else if (registro.anulaRegistroId != null) {
                                Text(
                                    text = "Corrige un registro anterior",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
