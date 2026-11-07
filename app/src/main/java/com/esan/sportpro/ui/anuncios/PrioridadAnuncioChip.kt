package com.esan.sportpro.ui.anuncios

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.esan.sportpro.core.theme.PaletaSportPro
import com.esan.sportpro.domain.anuncios.PrioridadAnuncio

/** Color de acento de cada prioridad (compartido por la lista, el banner y el detalle). */
fun PrioridadAnuncio.colorAcento() = when (this) {
    PrioridadAnuncio.URGENTE -> PaletaSportPro.Error
    PrioridadAnuncio.INFORMATIVA -> PaletaSportPro.Informacion
}

/**
 * US-027, criterio 5: los anuncios urgentes se destacan visualmente de los informativos.
 *
 * Es una píldora propia (no un AssistChip deshabilitado) con `maxLines = 1`: así nunca se
 * aplasta ni parte "Informativa" en varias líneas cuando el título ocupa el ancho de la fila.
 */
@Composable
fun PrioridadAnuncioChip(prioridad: PrioridadAnuncio, modifier: Modifier = Modifier) {
    val color = prioridad.colorAcento()
    val icono = if (prioridad == PrioridadAnuncio.URGENTE) Icons.Default.Campaign else Icons.Default.Info
    Surface(shape = CircleShape, color = color.copy(alpha = 0.14f), modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Text(
                text = prioridad.etiqueta,
                color = color,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}
