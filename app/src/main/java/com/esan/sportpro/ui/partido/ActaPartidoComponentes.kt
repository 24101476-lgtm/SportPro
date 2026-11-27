package com.esan.sportpro.ui.partido

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.esan.sportpro.domain.partido.ActaPartido
import com.esan.sportpro.domain.partido.ReglasActa

/**
 * US-022: contenido del acta (marcador, conteo de eventos, operador, horas reales, versión del
 * catálogo y minutos jugados). Se usa en el diálogo de cierre y al consultar el acta ya cerrada.
 */
@Composable
fun ActaResumenContenido(
    acta: ActaPartido,
    modifier: Modifier = Modifier,
    advertencia: String? = null,
    pie: String? = null,
) {
    // Tope para la barra de minutos: el que más jugó (o 90 si nadie llegó).
    val maxMinutos = (acta.minutosPorJugador.maxOfOrNull { it.minutos } ?: 0).coerceAtLeast(1)
    val jugadores = acta.minutosPorJugador.sortedWith(
        compareByDescending<com.esan.sportpro.domain.partido.MinutosJugador> { it.minutos }.thenBy { it.dorsal ?: 99 },
    )
    LazyColumn(
        modifier = modifier.heightIn(max = 460.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Resultado.
        item {
            androidx.compose.material3.Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                ) {
                    Text(
                        acta.equipo,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.End,
                        maxLines = 2,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${acta.marcadorPropio} – ${acta.marcadorRival}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    Text(
                        acta.rival,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        advertencia?.let {
            item {
                androidx.compose.material3.Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                    )
                }
            }
        }
        // Conteo de eventos.
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ConteoActa("Vigentes", acta.eventosVigentes, ColoresPartido.Verde, Modifier.weight(1f))
                ConteoActa("Anulados", acta.eventosAnulados, ColoresPartido.Gris, Modifier.weight(1f))
                ConteoActa(
                    "Incompletos",
                    acta.eventosIncompletos,
                    if (acta.eventosIncompletos > 0) ColoresPartido.Naranja else ColoresPartido.Gris,
                    Modifier.weight(1f),
                )
            }
        }
        // Datos del registro.
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FilaActa("Operador", acta.operadores.joinToString(", ").ifEmpty { "No identificado" })
                FilaActa("Inicio real", acta.inicioRealMs?.let(::formatearFechaHora) ?: "Sin registro")
                FilaActa("Fin real", acta.finRealMs?.let(::formatearFechaHora) ?: "Sin registro")
                val duracion = if (acta.inicioRealMs != null && acta.finRealMs != null) {
                    ((acta.finRealMs - acta.inicioRealMs) / 60_000L).takeIf { it >= 0 }
                } else {
                    null
                }
                duracion?.let { FilaActa("Duración real", "$it min") }
                FilaActa(
                    "Catálogo de eventos",
                    acta.versionesCatalogo.joinToString(", ") { "v$it" }.ifEmpty { "Sin registro" },
                )
            }
        }
        item {
            Text(
                "Minutos jugados",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        items(jugadores, key = { it.jugadorId }) { jugador ->
            MinutosFila(jugador, maxMinutos)
        }
        pie?.let {
            item {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun ConteoActa(etiqueta: String, valor: Int, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    androidx.compose.material3.Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.10f),
        modifier = modifier,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 8.dp)) {
            Text("$valor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = color)
            Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Jugador con su dorsal, si fue titular o suplente, y una barra con sus minutos. */
@Composable
private fun MinutosFila(jugador: com.esan.sportpro.domain.partido.MinutosJugador, maxMinutos: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.layout.Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(26.dp)
                .background(
                    if (jugador.titular) ColoresPartido.Verde else ColoresPartido.Azul,
                    androidx.compose.foundation.shape.CircleShape,
                ),
        ) {
            Text(
                jugador.dorsal?.toString() ?: "–",
                color = androidx.compose.ui.graphics.Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
            )
        }
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    jugador.nombre,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${jugador.minutos}'",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            androidx.compose.material3.LinearProgressIndicator(
                progress = { jugador.minutos.toFloat() / maxMinutos },
                color = if (jugador.titular) ColoresPartido.Verde else ColoresPartido.Azul,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                drawStopIndicator = {},
                modifier = Modifier.fillMaxWidth().height(5.dp).padding(top = 2.dp),
            )
            Text(
                if (jugador.titular) "Titular" else "Suplente",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FilaActa(titulo: String, valor: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            titulo,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            valor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Abre el selector nativo de Android para compartir el acta como texto (US-022, criterio 7). */
fun compartirActa(context: Context, acta: ActaPartido) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Acta: ${acta.equipo} vs ${acta.rival}")
        putExtra(Intent.EXTRA_TEXT, ReglasActa.textoCompartible(acta, ::formatearFechaHora))
    }
    context.startActivity(Intent.createChooser(intent, "Compartir acta"))
}
