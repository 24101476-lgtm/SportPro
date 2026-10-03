package com.esan.sportpro.ui.comunidad

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.esan.sportpro.domain.comunidad.Comentario

/** Un comentario dentro del detalle de una publicación (US-028, criterio 4 y 9 — acción Reportar). */
@Composable
fun ComentarioItem(comentario: Comentario, onReportar: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(comentario.autorNombre, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(comentario.texto, style = MaterialTheme.typography.bodyMedium)
        }
        IconButton(onClick = onReportar) {
            Icon(Icons.Default.Flag, contentDescription = "Reportar comentario", modifier = Modifier)
        }
    }
}
