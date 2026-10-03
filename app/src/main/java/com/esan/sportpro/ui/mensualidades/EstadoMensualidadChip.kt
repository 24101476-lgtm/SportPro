package com.esan.sportpro.ui.mensualidades

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.esan.sportpro.domain.mensualidades.EstadoMensualidad

/**
 * Chip de estado con color + ícono diferenciados (US-008, criterio de aceptación 1):
 * Pagado = verde, Pendiente = ámbar, Vencido = rojo, Exonerado = gris/azul.
 */
@Composable
fun EstadoMensualidadChip(estado: EstadoMensualidad, modifier: Modifier = Modifier) {
    val (color, icono) = when (estado) {
        EstadoMensualidad.PAGADO -> Color(0xFF2E7D32) to Icons.Default.CheckCircle
        EstadoMensualidad.PENDIENTE -> Color(0xFFE8A33D) to Icons.Default.Schedule
        EstadoMensualidad.VENCIDO -> Color(0xFFD62828) to Icons.Default.Error
        EstadoMensualidad.EXONERADO -> Color(0xFF546E7A) to Icons.Default.RemoveCircle
    }
    AssistChip(
        onClick = {},
        enabled = false,
        modifier = modifier,
        label = { androidx.compose.material3.Text(estado.etiqueta) },
        leadingIcon = { Icon(icono, contentDescription = null, modifier = Modifier.padding(0.dp)) },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = color.copy(alpha = 0.15f),
            disabledLabelColor = color,
            disabledLeadingIconContentColor = color,
        ),
        border = AssistChipDefaults.assistChipBorder(enabled = false, disabledBorderColor = color.copy(alpha = 0.4f)),
    )
}
