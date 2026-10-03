package com.esan.sportpro.ui.comunidad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.esan.sportpro.domain.comunidad.EstadoPublicacion
import com.esan.sportpro.domain.comunidad.Publicacion
import com.esan.sportpro.domain.comunidad.TipoReaccion
import java.text.SimpleDateFormat
import java.util.Locale

/** Tarjeta de una publicación del muro (US-028, frames 131 y 134: estado "En revisión" incluido). */
@Composable
fun PublicacionCard(
    publicacion: Publicacion,
    uidActual: String?,
    reaccionActual: TipoReaccion?,
    onAbrir: () -> Unit,
    onReaccionar: (TipoReaccion?) -> Unit,
    onReportar: () -> Unit,
    onEliminar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuAbierto by remember { mutableStateOf(false) }
    val esAutor = publicacion.autorUid == uidActual

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(publicacion.autorNombre, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        formatearFecha(publicacion) + if (publicacion.editado) " · Editado" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { menuAbierto = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                }
                DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                    DropdownMenuItem(
                        text = { Text("Reportar") },
                        leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) },
                        onClick = { menuAbierto = false; onReportar() },
                    )
                    if (esAutor) {
                        DropdownMenuItem(
                            text = { Text("Eliminar") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = { menuAbierto = false; onEliminar() },
                        )
                    }
                }
            }

            if (publicacion.estado == EstadoPublicacion.EN_REVISION) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text(
                        "En revisión: pendiente de aprobación del entrenador por tratarse de una cuenta de menor de edad",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }

            Text(publicacion.texto, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))

            if (publicacion.imagenesUrls.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    items(publicacion.imagenesUrls) { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TipoReaccion.entries.forEach { tipo ->
                    val cantidad = publicacion.reaccionesContadas[tipo] ?: 0
                    val activa = reaccionActual == tipo
                    Surface(
                        onClick = { onReaccionar(if (activa) null else tipo) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (activa) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(end = 4.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(tipo.emoji)
                            if (cantidad > 0) {
                                Text(" $cantidad", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
                Row(modifier = Modifier.weight(1f)) {}
                IconButton(onClick = onAbrir) {
                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Comentarios")
                }
                Text("${publicacion.comentariosCount}", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

private fun formatearFecha(publicacion: Publicacion): String {
    val fecha = publicacion.fechaPublicacion ?: return "Publicando…"
    return SimpleDateFormat("d MMM, HH:mm", Locale("es", "PE")).format(fecha)
}
