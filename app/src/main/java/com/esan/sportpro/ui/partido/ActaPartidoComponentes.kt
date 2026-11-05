package com.esan.sportpro.ui.partido

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    LazyColumn(
        modifier = modifier.heightIn(max = 420.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            Text(
                "${acta.equipo} ${acta.marcadorPropio} - ${acta.marcadorRival} ${acta.rival}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        advertencia?.let {
            item { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        }
        item { FilaActa("Eventos vigentes", acta.eventosVigentes.toString()) }
        item { FilaActa("Eventos anulados", acta.eventosAnulados.toString()) }
        item { FilaActa("Eventos incompletos", acta.eventosIncompletos.toString()) }
        item { FilaActa("Operador", acta.operadores.joinToString(", ").ifEmpty { "No identificado" }) }
        item { FilaActa("Inicio real", acta.inicioRealMs?.let(::formatearFechaHora) ?: "Sin registro") }
        item { FilaActa("Fin real", acta.finRealMs?.let(::formatearFechaHora) ?: "Sin registro") }
        item {
            FilaActa(
                "Catálogo de eventos",
                acta.versionesCatalogo.joinToString(", ") { "v$it" }.ifEmpty { "Sin registro" },
            )
        }
        item {
            Text(
                "Minutos jugados",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        items(acta.minutosPorJugador, key = { it.jugadorId }) { jugador ->
            FilaActa(
                titulo = jugador.etiqueta + if (jugador.titular) " (titular)" else "",
                valor = "${jugador.minutos}'",
            )
        }
        pie?.let {
            item {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
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
