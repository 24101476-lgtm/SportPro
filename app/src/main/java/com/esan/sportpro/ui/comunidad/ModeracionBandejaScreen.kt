package com.esan.sportpro.ui.comunidad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.comunidad.Reporte
import com.esan.sportpro.ui.common.EmptyStateMessage
import java.text.SimpleDateFormat
import java.util.Locale

/** US-030, criterios 3 y 7 — Frame 142_Moderacion_Bandeja. */
@Composable
fun ModeracionBandejaScreen(
    onAbrirReporte: (Reporte) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ModeracionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    if (!state.puedeModerar) {
        EmptyStateMessage(mensaje = "Esta sección es solo para administradores", icono = Icons.Default.Shield, modifier = modifier)
        return
    }

    if (!state.isLoadingBandeja && state.bandeja.isEmpty()) {
        EmptyStateMessage(mensaje = "No hay reportes pendientes de revisión", icono = Icons.Default.Shield, modifier = modifier)
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        items(state.bandeja, key = { it.id }) { reporte ->
            ReporteCard(reporte = reporte, onClick = { viewModel.seleccionarReporte(reporte); onAbrirReporte(reporte) })
        }
    }
}

@Composable
private fun ReporteCard(reporte: Reporte, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (reporte.involucraMenor) {
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Icon(
                            Icons.Default.PriorityHigh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            " Involucra a un menor de edad",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            Text("${reporte.tipoContenido.name} — ${reporte.motivo.etiqueta}", style = MaterialTheme.typography.titleSmall)
            if (!reporte.detalle.isNullOrBlank()) {
                Text(reporte.detalle, style = MaterialTheme.typography.bodySmall)
            }
            reporte.fechaReporte?.let {
                Text(
                    SimpleDateFormat("d MMM, HH:mm", Locale("es", "PE")).format(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )
            }
        }
    }
}
