package com.esan.sportpro.ui.mensualidades

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.esan.sportpro.domain.mensualidades.EstadoMensualidad

/** Pagado = verde, Pendiente = ámbar, Vencido = rojo, Exonerado = gris azulado (US-008, criterio 1). */
fun colorEstado(estado: EstadoMensualidad): Color = when (estado) {
    EstadoMensualidad.PAGADO -> Color(0xFF2E7D32)
    EstadoMensualidad.PENDIENTE -> Color(0xFFD9891A)
    EstadoMensualidad.VENCIDO -> Color(0xFFD62828)
    EstadoMensualidad.EXONERADO -> Color(0xFF546E7A)
}

fun iconoEstado(estado: EstadoMensualidad): ImageVector = when (estado) {
    EstadoMensualidad.PAGADO -> Icons.Default.CheckCircle
    EstadoMensualidad.PENDIENTE -> Icons.Default.Schedule
    EstadoMensualidad.VENCIDO -> Icons.Default.Error
    EstadoMensualidad.EXONERADO -> Icons.Default.RemoveCircle
}

/** Etiqueta de estado con color + ícono diferenciados (US-008, criterio de aceptación 1). */
@Composable
fun EstadoMensualidadChip(estado: EstadoMensualidad, modifier: Modifier = Modifier) {
    val color = colorEstado(estado)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(color.copy(alpha = 0.13f), RoundedCornerShape(50))
            .padding(start = 8.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),
    ) {
        Icon(iconoEstado(estado), contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(
            estado.etiqueta,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}
