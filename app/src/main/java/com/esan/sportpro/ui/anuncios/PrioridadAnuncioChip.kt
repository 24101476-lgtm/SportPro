package com.esan.sportpro.ui.anuncios

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.esan.sportpro.domain.anuncios.PrioridadAnuncio

/** US-027, criterio 5: los anuncios urgentes se destacan visualmente de los informativos. */
@Composable
fun PrioridadAnuncioChip(prioridad: PrioridadAnuncio, modifier: Modifier = Modifier) {
    val (color, icono) = when (prioridad) {
        PrioridadAnuncio.URGENTE -> Color(0xFFD62828) to Icons.Default.Campaign
        PrioridadAnuncio.INFORMATIVA -> Color(0xFF546E7A) to Icons.Default.Info
    }
    AssistChip(
        onClick = {},
        enabled = false,
        modifier = modifier,
        label = { Text(prioridad.etiqueta) },
        leadingIcon = { Icon(icono, contentDescription = null) },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = color.copy(alpha = 0.15f),
            disabledLabelColor = color,
            disabledLeadingIconContentColor = color,
        ),
        border = AssistChipDefaults.assistChipBorder(enabled = false, disabledBorderColor = color.copy(alpha = 0.4f)),
    )
}
