package com.esan.sportpro.ui.comunidad

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.esan.sportpro.core.fecha.FechasSportPro
import com.esan.sportpro.domain.comunidad.Comentario

/**
 * Un comentario dentro del detalle de una publicación (US-028, criterio 4 y 9 — acción Reportar).
 * Se ve como burbuja de chat con la inicial y el color de su autor.
 */
@Composable
fun ComentarioItem(comentario: Comentario, onReportar: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        AvatarAutor(comentario.autorNombre, tamano = 34.dp, modifier = Modifier.padding(top = 2.dp))
        Surface(
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 18.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.weight(1f).padding(start = 10.dp),
        ) {
            Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        comentario.autorNombre,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    comentario.fechaCreacion?.let { fecha ->
                        Text(
                            "  ${FechasSportPro.diaMesHora(fecha)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(comentario.texto, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
            }
        }
        IconButton(onClick = onReportar, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Outlined.Flag,
                contentDescription = "Reportar comentario",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
