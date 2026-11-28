package com.esan.sportpro.ui.ia

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.core.theme.FuenteNumeros
import com.esan.sportpro.core.theme.PaletaSportPro
import com.esan.sportpro.core.theme.SportProTema
import com.esan.sportpro.domain.ia.EvaluacionResumen
import com.esan.sportpro.domain.ia.ReglasEvaluacion
import com.esan.sportpro.ui.common.EncabezadoModulo
import com.esan.sportpro.ui.common.FotoSportPro
import com.esan.sportpro.ui.partido.formatearFechaHora

/**
 * US-025: pantalla Evaluaciones, con el consolidado de todos los partidos y su exportación como texto.
 *
 * Diseño: el promedio de utilidad en grande con estrellas (lo primero que se pregunta: ¿sirve la
 * IA?), los tres tipos de fallo como fichas y cada evaluación como tarjeta con su nota y su cita.
 */
@Composable
fun EvaluacionesScreen(
    modifier: Modifier = Modifier,
    viewModel: EvaluacionesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxSize()) {
        EncabezadoModulo(
            foto = FotoSportPro.ENTRENADOR,
            titulo = "Evaluaciones",
            subtitulo = "¿Qué tan útil fue el resumen con IA y en qué falló?",
            icono = Icons.Default.AutoAwesome,
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
                } else {
                    item {
                        Text(
                            "Por partido",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp),
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

/** Tarjeta oliva: promedio con estrellas, total de evaluaciones y los tres tipos de fallo. */
@Composable
private fun Consolidado(state: EvaluacionesUiState, onExportar: () -> Unit) {
    val total = state.consolidado
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = PaletaSportPro.Musgo,
        contentColor = Color.White,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                if (state.esAdministrador) "Consolidado de la academia" else "Consolidado de tus partidos",
                style = MaterialTheme.typography.labelLarge,
                color = PaletaSportPro.MusgoClaro,
            )
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 6.dp)) {
                Text(
                    ReglasEvaluacion.formatearPromedio(total.utilidadPromedio),
                    style = TextStyle(fontFamily = FuenteNumeros, fontSize = 56.sp, lineHeight = 56.sp),
                )
                Text(
                    " / ${ReglasEvaluacion.UTILIDAD_MAX}",
                    style = TextStyle(fontFamily = FuenteNumeros, fontSize = 22.sp),
                    color = PaletaSportPro.Lima,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f).padding(bottom = 4.dp)) {
                    Text(
                        total.evaluaciones.toString(),
                        style = TextStyle(fontFamily = FuenteNumeros, fontSize = 28.sp, lineHeight = 28.sp),
                    )
                    Text(
                        if (total.evaluaciones == 1) "evaluación" else "evaluaciones",
                        style = MaterialTheme.typography.labelMedium,
                        color = PaletaSportPro.MusgoClaro,
                    )
                }
            }
            Estrellas(total.utilidadPromedio ?: 0.0, Modifier.padding(top = 4.dp))
            Text(
                "Utilidad promedio",
                style = MaterialTheme.typography.bodySmall,
                color = PaletaSportPro.MusgoClaro,
                modifier = Modifier.padding(top = 2.dp),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                FichaFallo(Icons.Default.RemoveCircleOutline, "Omisiones", total.omisiones, Modifier.weight(1f))
                FichaFallo(Icons.Default.ErrorOutline, "Errores", total.errores, Modifier.weight(1f))
                FichaFallo(Icons.Default.HelpOutline, "Inventados", total.inventados, Modifier.weight(1f))
            }

            Button(
                onClick = onExportar,
                enabled = state.evaluaciones.isNotEmpty(),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PaletaSportPro.Lima,
                    contentColor = PaletaSportPro.Carbon,
                    disabledContainerColor = Color.White.copy(alpha = 0.15f),
                    disabledContentColor = Color.White.copy(alpha = 0.5f),
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).heightIn(min = 48.dp),
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Exportar para el informe", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

/** Cinco estrellas con medias: 4.5 → ★★★★⯪. */
@Composable
private fun Estrellas(valor: Double, modifier: Modifier = Modifier, tamano: Int = 20, color: Color = PaletaSportPro.Lima) {
    Row(modifier = modifier) {
        (1..ReglasEvaluacion.UTILIDAD_MAX).forEach { i ->
            val icono = when {
                valor >= i -> Icons.Default.Star
                valor >= i - 0.5 -> Icons.Default.StarHalf
                else -> Icons.Default.StarBorder
            }
            Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(tamano.dp))
        }
    }
}

@Composable
private fun FichaFallo(icono: ImageVector, etiqueta: String, valor: Int, modifier: Modifier) {
    Surface(shape = RoundedCornerShape(18.dp), color = Color.White.copy(alpha = 0.12f), modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icono, contentDescription = null, tint = PaletaSportPro.MusgoClaro, modifier = Modifier.size(18.dp))
            Text(
                valor.toString(),
                style = TextStyle(fontFamily = FuenteNumeros, fontSize = 26.sp, lineHeight = 28.sp),
                color = Color.White,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = PaletaSportPro.MusgoClaro, maxLines = 1)
        }
    }
}

@Composable
private fun TarjetaEvaluacion(evaluacion: EvaluacionResumen) {
    val tema = SportProTema.colores
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        evaluacion.partidoNombre,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        listOfNotNull(
                            evaluacion.fechaPartidoMs?.let { formatearFechaHora(it) },
                            "versión ${evaluacion.version}",
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // Nota de utilidad como insignia.
                Surface(shape = RoundedCornerShape(14.dp), color = tema.suave, contentColor = tema.sobreSuave) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = PaletaSportPro.Musgo, modifier = Modifier.size(16.dp))
                        Text(
                            "${evaluacion.utilidad}/${ReglasEvaluacion.UTILIDAD_MAX}",
                            style = TextStyle(fontFamily = FuenteNumeros, fontSize = 18.sp),
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ConteoFallo("omisiones", evaluacion.omisiones)
                ConteoFallo("errores", evaluacion.errores)
                ConteoFallo("inventados", evaluacion.inventados)
            }

            if (evaluacion.comentario.isNotBlank()) {
                // El comentario como cita: barra lateral oliva y cursiva.
                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    Box(Modifier.width(3.dp).fillMaxHeight().clip(CircleShape).background(PaletaSportPro.MusgoMedio))
                    Text(
                        evaluacion.comentario,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
            }
            if (evaluacion.correcciones.isNotBlank()) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Default.EditNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        evaluacion.correcciones.lines().filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }
    }
}

/** "0 errores" en gris; con fallos, en el color de alerta. */
@Composable
private fun ConteoFallo(etiqueta: String, valor: Int) {
    val tema = SportProTema.colores
    val color = if (valor > 0) tema.advertencia else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = CircleShape,
        color = if (valor > 0) tema.advertencia.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = color,
    ) {
        Text(
            "$valor $etiqueta",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (valor > 0) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
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
