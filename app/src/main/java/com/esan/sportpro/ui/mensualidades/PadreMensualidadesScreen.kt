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
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.ui.common.EncabezadoModulo
import com.esan.sportpro.ui.common.EstadoVacioModulo
import com.esan.sportpro.ui.common.RegistroSimuladoLegend
import com.esan.sportpro.ui.common.TarjetaSportPro

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
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.sinJugadoresVinculados -> Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                EncabezadoModulo(
                    titulo = "Mensualidades de mis hijos",
                    subtitulo = "Estado de pago de cada jugador vinculado",
                    icono = Icons.Default.Payments,
                )
                EstadoVacioModulo(
                    titulo = "No tienes jugadores vinculados a tu cuenta",
                    mensaje = "Pide al administrador que complete la vinculación (US-007)",
                    icono = Icons.Default.Payments,
                )
            }
            else -> LazyColumn(
                modifier = Modifier.padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    EncabezadoModulo(
                        titulo = "Mensualidades de mis hijos",
                        subtitulo = "Estado de pago de cada jugador vinculado",
                        icono = Icons.Default.Payments,
                    )
                }
                item { RegistroSimuladoLegend() }
                items(state.mensualidades, key = { it.mensualidad.id }) { item ->
                    TarjetaSportPro {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
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
