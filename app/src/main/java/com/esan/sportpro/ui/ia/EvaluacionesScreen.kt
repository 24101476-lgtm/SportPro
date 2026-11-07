package com.esan.sportpro.ui.ia

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.ia.EvaluacionResumen
import com.esan.sportpro.domain.ia.ReglasEvaluacion
import com.esan.sportpro.ui.common.EncabezadoModulo
import com.esan.sportpro.ui.partido.formatearFechaHora

/** US-025: pantalla Evaluaciones, con el consolidado de todos los partidos y su exportación como texto. */
@Composable
fun EvaluacionesScreen(
    modifier: Modifier = Modifier,
    viewModel: EvaluacionesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxSize()) {
        EncabezadoModulo(
            titulo = "Evaluaciones",
            subtitulo = "Utilidad del resumen con IA y lo que omitió, erró o inventó",
            icono = Icons.Default.Star,
            modifier = Modifier.padding(16.dp),
        )
        when {
            state.cargando -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            !state.tieneAcceso -> MensajeEvaluaciones("Las evaluaciones solo están disponibles para el entrenador y el administrador.")

            state.error != null -> MensajeEvaluaciones(state.error.orEmpty())

            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item { Consolidado(state, onExportar = { exportar(context, state.evaluaciones) }) }
                if (state.evaluaciones.isEmpty()) {
                    item {
                        Text(
                            "Aún no hay evaluaciones. Aprueba el resumen de un partido y regístrala desde la pantalla del partido.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(state.evaluaciones, key = { it.id }) { TarjetaEvaluacion(it) }
            }
        }
    }
}

@Composable
private fun MensajeEvaluaciones(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(16.dp),
    )
}

@Composable
private fun Consolidado(state: EvaluacionesUiState, onExportar: () -> Unit) {
    val total = state.consolidado
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                if (state.esAdministrador) "Consolidado de la academia" else "Consolidado de tus partidos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Dato("Evaluaciones", total.evaluaciones.toString(), Modifier.weight(1f))
                Dato("Utilidad promedio", ReglasEvaluacion.formatearPromedio(total.utilidadPromedio) + " / 5", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Dato("Omisiones", total.omisiones.toString(), Modifier.weight(1f))
                Dato("Errores", total.errores.toString(), Modifier.weight(1f))
                Dato("Inventados", total.inventados.toString(), Modifier.weight(1f))
            }
            Button(
                onClick = onExportar,
                enabled = state.evaluaciones.isNotEmpty(),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text("Exportar como texto", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun Dato(titulo: String, valor: String, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(valor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(titulo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TarjetaEvaluacion(evaluacion: EvaluacionResumen) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "${evaluacion.partidoNombre} · versión ${evaluacion.version}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            evaluacion.fechaPartidoMs?.let {
                Text(formatearFechaHora(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "Utilidad ${evaluacion.utilidad}/${ReglasEvaluacion.UTILIDAD_MAX} · Omisiones ${evaluacion.omisiones} · " +
                    "Errores ${evaluacion.errores} · Inventados ${evaluacion.inventados}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(evaluacion.comentario, style = MaterialTheme.typography.bodyMedium)
            if (evaluacion.correcciones.isNotBlank()) {
                Text(
                    "Correcciones: " + evaluacion.correcciones.lines().filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Abre el selector nativo de Android con el consolidado como texto, para llevarlo al informe final (criterio 6). */
private fun exportar(context: Context, evaluaciones: List<EvaluacionResumen>) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Evaluación del resumen con IA - SportPro")
        putExtra(Intent.EXTRA_TEXT, ReglasEvaluacion.textoExportable(evaluaciones, ::formatearFechaHora))
    }
    context.startActivity(Intent.createChooser(intent, "Exportar evaluaciones"))
}
