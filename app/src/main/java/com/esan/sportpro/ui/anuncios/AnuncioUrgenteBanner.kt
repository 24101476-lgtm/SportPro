package com.esan.sportpro.ui.anuncios

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.esan.sportpro.domain.anuncios.Anuncio

/**
 * US-027, criterio 5 — Frame 129_Anuncio_Urgente_Home: banner destacado para anuncios urgentes no
 * leídos. Pensado para incrustarse en la futura pantalla "Inicio" del equipo (aún no existe un
 * feed propio en el proyecto); por ahora se muestra en la parte superior de la lista de anuncios.
 */
@Composable
fun AnuncioUrgenteBanner(anuncios: List<Anuncio>, onAbrir: (Anuncio) -> Unit, modifier: Modifier = Modifier) {
    if (anuncios.isEmpty()) return

    Column(modifier = modifier) {
        anuncios.forEach { anuncio ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                onClick = { onAbrir(anuncio) },
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            text = anuncio.titulo,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Text(
                            text = anuncio.mensaje,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            maxLines = 2,
                        )
                    }
                }
            }
        }
    }
}
