package com.esan.sportpro.ui.estadisticas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esan.sportpro.domain.estadisticas.GolesPorPartidoPunto
import java.time.format.DateTimeFormatter

/**
 * US-026, criterio 9: gráfico de barras con los goles por partido de los últimos diez encuentros.
 * Se dibuja con [Canvas] en vez de una librería externa para no añadir una nueva dependencia solo
 * para este gráfico.
 */
@Composable
fun GraficoGolesBarChart(puntos: List<GolesPorPartidoPunto>, modifier: Modifier = Modifier) {
    val colorEquipo = MaterialTheme.colorScheme.primary
    val colorRival = MaterialTheme.colorScheme.tertiary
    val colorEje = MaterialTheme.colorScheme.outlineVariant
    val textMeasurer = rememberTextMeasurer()
    val formato = remember(puntos) { DateTimeFormatter.ofPattern("dd/MM") }

    Column(modifier = modifier) {
        Text(
            text = "Goles por partido (últimos ${puntos.size})",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
        ) {
            if (puntos.isEmpty()) return@Canvas
            val maxGoles = (puntos.maxOf { maxOf(it.golesEquipo, it.golesRival) }).coerceAtLeast(1)
            val anchoGrupo = size.width / puntos.size
            val anchoBarra = anchoGrupo / 3.2f
            val alturaDisponible = size.height - 24.dp.toPx()

            drawLine(colorEje, start = androidx.compose.ui.geometry.Offset(0f, alturaDisponible), end = androidx.compose.ui.geometry.Offset(size.width, alturaDisponible))

            puntos.forEachIndexed { index, punto ->
                val xBase = index * anchoGrupo + anchoGrupo / 2f
                val alturaEquipo = (punto.golesEquipo.toFloat() / maxGoles) * alturaDisponible
                val alturaRival = (punto.golesRival.toFloat() / maxGoles) * alturaDisponible

                drawRect(
                    color = colorEquipo,
                    topLeft = androidx.compose.ui.geometry.Offset(xBase - anchoBarra - 2.dp.toPx(), alturaDisponible - alturaEquipo),
                    size = androidx.compose.ui.geometry.Size(anchoBarra, alturaEquipo),
                )
                drawRect(
                    color = colorRival,
                    topLeft = androidx.compose.ui.geometry.Offset(xBase + 2.dp.toPx(), alturaDisponible - alturaRival),
                    size = androidx.compose.ui.geometry.Size(anchoBarra, alturaRival),
                )
                val etiquetaStyle = androidx.compose.ui.text.TextStyle(fontSize = 9.sp, color = colorEje)
                val layout = textMeasurer.measure(text = punto.fecha.format(formato), style = etiquetaStyle)
                drawText(
                    textMeasurer = textMeasurer,
                    text = punto.fecha.format(formato),
                    topLeft = androidx.compose.ui.geometry.Offset(xBase - layout.size.width / 2f, alturaDisponible + 4.dp.toPx()),
                    style = etiquetaStyle,
                )
            }
        }
    }
}
