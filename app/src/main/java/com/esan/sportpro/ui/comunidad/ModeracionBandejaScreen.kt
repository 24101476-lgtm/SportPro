package com.esan.sportpro.ui.comunidad

import com.esan.sportpro.core.fecha.FechasSportPro
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
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.text.font.FontWeight
import com.esan.sportpro.core.theme.PaletaSportPro
import com.esan.sportpro.ui.common.ChipEstado
import com.esan.sportpro.ui.common.DatoEstadistica
import com.esan.sportpro.ui.common.EmptyStateMessage
import com.esan.sportpro.ui.common.FilaEstadisticas
import com.esan.sportpro.ui.common.TarjetaSportPro
import com.esan.sportpro.ui.common.TituloSeccion
import com.esan.sportpro.ui.common.TonoEstado
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

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier,
    ) {
        item(key = "resumen") {
            FilaEstadisticas(
                datos = listOf(
                    DatoEstadistica(state.bandeja.size.toString(), "Pendientes"),
                    DatoEstadistica(state.enRevision.toString(), "En revisión"),
                    DatoEstadistica(state.suspendidosActivos.toString(), "Suspendidos"),
                    DatoEstadistica(state.resueltos.size.toString(), "Resueltos"),
                ),
            )
        }
        item(key = "t-pendientes") { TituloSeccion("Reportes pendientes") }
        if (!state.isLoadingBandeja && state.bandeja.isEmpty()) {
            item(key = "vacio") {
                TarjetaSportPro {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PaletaSportPro.Exito)
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text("Todo en orden", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                "No hay reportes pendientes. Cuando alguien reporte una publicación, comentario o aviso, aparecerá aquí.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        } else {
            items(state.bandeja, key = { it.id }) { reporte ->
                ReporteCard(reporte = reporte, onClick = { viewModel.seleccionarReporte(reporte); onAbrirReporte(reporte) })
            }
        }

        item(key = "t-reglas") { TituloSeccion("Reglas de la comunidad") }
        item(key = "reglas") { ReglasComunidadCard() }

        if (state.resueltos.isNotEmpty()) {
            item(key = "t-resueltos") { TituloSeccion("Últimos reportes resueltos") }
            items(state.resueltos, key = { "r-" + it.id }) { reporte -> ReporteResueltoRow(reporte) }
        }
    }
}

@Composable
private fun ReglasComunidadCard() {
    TarjetaSportPro {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                "🛡️ Un contenido con 3 reportes se oculta solo mientras se revisa.",
                "🧒 Lo que involucra a un menor de edad siempre aparece primero.",
                "📵 No se permiten datos personales (teléfonos, direcciones) en el muro.",
                "⏱️ Las suspensiones son de 7, 15 o 30 días y el usuario ve cuándo terminan.",
            ).forEach { regla -> Text(regla, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun ReporteResueltoRow(reporte: Reporte) {
    TarjetaSportPro {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PaletaSportPro.Exito)
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    "${reporte.tipoContenido.name} — ${reporte.motivo.etiqueta}",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                )
                Text(
                    reporte.motivoResolucion.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            reporte.accionTomada?.let { ChipEstado(texto = it.etiqueta.substringBefore(" y "), tono = TonoEstado.INFORMACION) }
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
                    FechasSportPro.formatoDiaMesHora().format(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )
            }
        }
    }
}
