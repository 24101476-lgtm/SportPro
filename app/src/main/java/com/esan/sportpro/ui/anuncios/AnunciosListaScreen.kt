package com.esan.sportpro.ui.anuncios

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Surface
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.esan.sportpro.core.fecha.FechasSportPro
import com.esan.sportpro.domain.anuncios.PrioridadAnuncio
import com.esan.sportpro.ui.common.ChipEstado
import com.esan.sportpro.ui.common.TonoEstado
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
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.anuncios.Anuncio
import com.esan.sportpro.ui.common.EncabezadoModulo
import com.esan.sportpro.ui.common.EstadoVacioModulo
import com.esan.sportpro.ui.common.TarjetaSportPro
import com.esan.sportpro.ui.partido.ColoresPartido

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
        floatingActionButton = {
            if (state.puedePublicar) {
                FloatingActionButton(
                    onClick = onNuevoAnuncio,
                    containerColor = ColoresPartido.Verde,
                    contentColor = Color.White,
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nuevo anuncio")
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            EncabezadoModulo(
                foto = com.esan.sportpro.ui.common.FotoSportPro.ESTADIO,
                titulo = "Anuncios",
                subtitulo = "Comunicados de tu academia",
                icono = Icons.Default.Campaign,
                accion = {
                    IconButton(onClick = onAbrirPreferencias) {
                        Icon(
                            Icons.Default.NotificationsOff,
                            contentDescription = "Preferencias de notificaciones",
                            tint = Color.White,
                        )
                    }
                },
            )
            when {
                state.isLoading -> Column(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) { CircularProgressIndicator() }

                state.anuncios.isEmpty() && state.anunciosUrgentesNoLeidos.isEmpty() -> EstadoVacioModulo(
                    titulo = "Todavía no hay anuncios",
                    mensaje = "Cuando se publique un comunicado, aparecerá aquí",
                    icono = Icons.Default.Campaign,
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
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
}

@Composable
private fun AnuncioListaItem(anuncio: Anuncio, onClick: () -> Unit) {
    val acento = anuncio.prioridad.colorAcento()
    val urgente = anuncio.prioridad == PrioridadAnuncio.URGENTE
    TarjetaSportPro(onClick = onClick) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Franja de color a la izquierda: de un vistazo se distingue urgente de informativo.
            Box(modifier = Modifier.width(6.dp).fillMaxHeight().background(acento))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(if (urgente) acento.copy(alpha = 0.06f) else Color.Transparent)
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = acento.copy(alpha = 0.14f), modifier = Modifier.size(40.dp)) {
                        Icon(
                            if (urgente) Icons.Default.Campaign else Icons.Default.Notifications,
                            contentDescription = null,
                            tint = acento,
                            modifier = Modifier.padding(9.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(
                            text = anuncio.titulo,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        anuncio.fechaPublicacion?.let {
                            Text(
                                text = FechasSportPro.formatoDiaMesHora().format(it),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    PrioridadAnuncioChip(prioridad = anuncio.prioridad)
                }
                Text(
                    text = anuncio.mensaje,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ChipEstado(texto = anuncio.destinatarios.etiqueta, tono = TonoEstado.NEUTRO)
                    if (anuncio.editado) ChipEstado(texto = "Editado", tono = TonoEstado.ADVERTENCIA)
                    if (anuncio.archivado) ChipEstado(texto = "Archivado", tono = TonoEstado.NEUTRO)
                }
            }
        }
    }
}
