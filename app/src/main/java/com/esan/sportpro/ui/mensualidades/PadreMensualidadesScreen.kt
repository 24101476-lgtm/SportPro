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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.ui.common.RegistroSimuladoLegend

/**
 * US-008 — Frame 40_Padre_Mensualidades. Solo muestra mensualidades de los jugadores vinculados
 * a la cuenta del padre de familia (criterio de aceptación 8).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PadreMensualidadesScreen(
    modifier: Modifier = Modifier,
    viewModel: PadreMensualidadesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Mensualidades de mis hijos") }) },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.sinJugadoresVinculados -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "No tienes jugadores vinculados a tu cuenta",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "Pide al administrador que complete la vinculación (US-007)",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            else -> LazyColumn(
                modifier = Modifier.padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { RegistroSimuladoLegend(modifier = Modifier.padding(bottom = 8.dp)) }
                items(state.mensualidades, key = { it.mensualidad.id }) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(item.mensualidad.jugadorNombre, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${item.mensualidad.mes.toString().padStart(2, '0')}/${item.mensualidad.anio} · S/ %.2f".format(item.mensualidad.montoReferencial),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            EstadoMensualidadChip(estado = item.estadoEfectivo)
                        }
                    }
                }
            }
        }
    }
}
