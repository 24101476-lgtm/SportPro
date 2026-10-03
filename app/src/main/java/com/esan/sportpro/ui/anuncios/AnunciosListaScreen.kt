package com.esan.sportpro.ui.anuncios

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.anuncios.Anuncio
import com.esan.sportpro.ui.common.EmptyStateMessage

/** US-027 — Frame 126_Anuncios_Lista (incluye el banner de urgentes del Frame 129). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnunciosListaScreen(
    onNuevoAnuncio: () -> Unit,
    onAbrirAnuncio: (Anuncio) -> Unit,
    onAbrirPreferencias: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnunciosViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Anuncios") },
                actions = {
                    IconButton(onClick = onAbrirPreferencias) {
                        Icon(Icons.Default.NotificationsOff, contentDescription = "Preferencias de notificaciones")
                    }
                },
            )
        },
        floatingActionButton = {
            if (state.puedePublicar) {
                FloatingActionButton(onClick = onNuevoAnuncio) {
                    Icon(Icons.Default.Add, contentDescription = "Nuevo anuncio")
                }
            }
        },
    ) { padding ->
        when {
            state.isLoading -> Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator() }

            state.anuncios.isEmpty() && state.anunciosUrgentesNoLeidos.isEmpty() -> EmptyStateMessage(
                mensaje = "Todavía no hay anuncios publicados",
                modifier = Modifier.padding(padding),
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.anunciosUrgentesNoLeidos.isNotEmpty()) {
                    item {
                        AnuncioUrgenteBanner(
                            anuncios = state.anunciosUrgentesNoLeidos,
                            onAbrir = onAbrirAnuncio,
                        )
                    }
                }
                items(state.anuncios, key = { it.id }) { anuncio ->
                    AnuncioListaItem(anuncio = anuncio, onClick = { onAbrirAnuncio(anuncio) })
                }
            }
        }
    }
}

@Composable
private fun AnuncioListaItem(anuncio: Anuncio, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = anuncio.titulo, style = MaterialTheme.typography.titleMedium)
                PrioridadAnuncioChip(prioridad = anuncio.prioridad)
            }
            Text(
                text = anuncio.mensaje,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = anuncio.destinatarios.etiqueta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (anuncio.editado) {
                    Text(
                        text = "· Editado",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (anuncio.archivado) {
                    Text(
                        text = "· Archivado",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
