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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.ui.common.EncabezadoModulo
import com.esan.sportpro.ui.common.EstadoVacioModulo
import com.esan.sportpro.ui.common.RegistroSimuladoLegend
import com.esan.sportpro.ui.common.TarjetaSportPro
import com.esan.sportpro.ui.common.TituloSeccion
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

/**
 * US-008 — Frame 40_Padre_Mensualidades. Solo muestra mensualidades de los jugadores vinculados
 * a la cuenta del padre de familia (criterio de aceptación 8). Cada hijo tiene su sección con el
 * mes actual (aunque no tenga pago registrado) y su historial de meses.
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
            state.error != null -> Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Encabezado()
                EstadoVacioModulo(
                    titulo = "No pudimos cargar las mensualidades",
                    mensaje = state.error.orEmpty(),
                    icono = Icons.Default.Payments,
                )
            }
            state.sinJugadoresVinculados -> Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Encabezado()
                EstadoVacioModulo(
                    titulo = "No tienes jugadores vinculados a tu cuenta",
                    mensaje = if (state.solicitudesPendientes > 0) {
                        "Tienes ${state.solicitudesPendientes} solicitud(es) de vinculación esperando la aprobación del administrador. Cuando la apruebe, las mensualidades aparecerán aquí."
                    } else {
                        "Solicita la vinculación de tu hijo en la pestaña \"Mis hijos\"."
                    },
                    icono = Icons.Default.Payments,
                )
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Encabezado() }
                item { RegistroSimuladoLegend() }
                state.hijos.forEach { hijo ->
                    item(key = "titulo-${hijo.jugadorId}") {
                        TituloSeccion(
                            if (hijo.equipoNombre.isBlank()) hijo.nombre else "${hijo.nombre} · ${hijo.equipoNombre}",
                        )
                    }
                    items(hijo.meses, key = { "${hijo.jugadorId}-${it.mensualidad.id}" }) { item ->
                        TarjetaSportPro {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val m = item.mensualidad
                                    val mes = Month.of(m.mes.coerceIn(1, 12))
                                        .getDisplayName(TextStyle.FULL, Locale("es"))
                                        .replaceFirstChar { it.uppercase() }
                                    Text("$mes ${m.anio}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = if (item.sinRegistro) "Sin pago registrado" else "S/ %.2f".format(m.montoReferencial),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
}

@Composable
private fun Encabezado() {
    EncabezadoModulo(
        foto = com.esan.sportpro.ui.common.FotoSportPro.BOTINES,
        titulo = "Mensualidades de mis hijos",
        subtitulo = "Estado de pago de cada jugador vinculado",
        icono = Icons.Default.Payments,
    )
}
