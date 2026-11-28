package com.esan.sportpro.ui.estadisticas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esan.sportpro.core.theme.FuenteNumeros
import com.esan.sportpro.domain.estadisticas.GolesPorPartidoPunto
import java.time.format.DateTimeFormatter

/**
 * US-026, criterio 9: goles por partido de los últimos diez encuentros (nuestros y del rival),
 * con la cantidad encima de cada barra, la fecha y la inicial del rival debajo, y leyenda.
 * Se dibuja con [Canvas] para no añadir una librería solo para este gráfico.
 */
@Composable
fun GraficoGolesBarChart(puntos: List<GolesPorPartidoPunto>, modifier: Modifier = Modifier) {
    val colorEquipo = MaterialTheme.colorScheme.primary
    // Gris medio: se distingue del verde en modo claro y oscuro (antes casi no se veía).
    val colorRival = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
    val colorTexto = MaterialTheme.colorScheme.onSurface
    val colorSecundario = MaterialTheme.colorScheme.onSurfaceVariant
    val colorEje = MaterialTheme.colorScheme.outline
    val medidor = rememberTextMeasurer()
    val formato = remember { DateTimeFormatter.ofPattern("dd/MM") }

    Column(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Leyenda(colorEquipo, "Nosotros")
            Leyenda(colorRival, "Rival")
            Text(
                "Últimos ${puntos.size}",
                style = MaterialTheme.typography.labelSmall,
                color = colorSecundario,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .height(190.dp)
                .semantics {
                    contentDescription = puntos.joinToString("; ") {
                        "${it.fecha.format(formato)} contra ${it.rivalNombre}: ${it.golesEquipo} a ${it.golesRival}"
                    }
                },
        ) {
            if (puntos.isEmpty()) return@Canvas
            val maxGoles = puntos.maxOf { maxOf(it.golesEquipo, it.golesRival) }.coerceAtLeast(1)
            val anchoGrupo = size.width / puntos.size
            val anchoBarra = (anchoGrupo / 3.2f).coerceAtMost(18.dp.toPx())
            val separacion = 3.dp.toPx()
            val zonaEtiquetas = 34.dp.toPx()
            val zonaNumero = 16.dp.toPx()
            val base = size.height - zonaEtiquetas
            val alturaUtil = base - zonaNumero
            val minimo = 3.dp.toPx() // un 0 se ve como una rayita, no desaparece
            val estiloNumero = TextStyle(fontFamily = FuenteNumeros, fontSize = 12.sp, color = colorTexto)
            val estiloFecha = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = colorSecundario)
            val estiloRival = TextStyle(fontSize = 10.sp, color = colorSecundario)

            drawLine(colorEje, Offset(0f, base), Offset(size.width, base), 1.dp.toPx())

            puntos.forEachIndexed { i, p ->
                val centro = i * anchoGrupo + anchoGrupo / 2f
                listOf(
                    Triple(p.golesEquipo, colorEquipo, centro - separacion / 2 - anchoBarra),
                    Triple(p.golesRival, colorRival, centro + separacion / 2),
                ).forEach { (goles, color, x) ->
                    val alto = if (goles == 0) minimo else (goles.toFloat() / maxGoles) * alturaUtil
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, base - alto),
                        size = Size(anchoBarra, alto),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    )
                    val numero = medidor.measure("$goles", estiloNumero)
                    drawText(numero, topLeft = Offset(x + anchoBarra / 2 - numero.size.width / 2f, base - alto - numero.size.height - 1.dp.toPx()))
                }
                val fecha = medidor.measure(p.fecha.format(formato), estiloFecha)
                drawText(fecha, topLeft = Offset(centro - fecha.size.width / 2f, base + 4.dp.toPx()))
                val rival = medidor.measure(p.rivalNombre.take(3).uppercase(), estiloRival)
                drawText(rival, topLeft = Offset(centro - rival.size.width / 2f, base + 4.dp.toPx() + fecha.size.height))
            }
        }
    }
}

@Composable
private fun Leyenda(color: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Text(texto, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 6.dp))
    }
}
