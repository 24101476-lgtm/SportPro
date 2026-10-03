package com.esan.sportpro.ui.comunidad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.esan.sportpro.domain.comunidad.Aviso
import com.esan.sportpro.ui.common.EmptyStateMessage
import java.text.SimpleDateFormat
import java.util.Locale

/** US-029, criterios 6 y 10 — Frame 136_Avisos_Buscador (incluye el estado vacío del frame 140). */
@Composable
fun AvisosBuscadorScreen(
    onNuevoAviso: () -> Unit,
    onAbrirAviso: (Aviso) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AvisosViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var mostrarFiltros by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = onNuevoAviso) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo aviso")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Pruebas y convocatorias", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { mostrarFiltros = true }) {
                    Icon(Icons.Default.Tune, contentDescription = "Filtros")
                }
            }

            if (!state.isLoading && state.avisos.isEmpty()) {
                EmptyStateMessage(
                    mensaje = "No encontramos convocatorias con esos filtros",
                    icono = Icons.Default.SearchOff,
                    modifier = Modifier.weight(1f, fill = true),
                )
                if (state.filtro.tieneFiltrosActivos) {
                    TextButton(onClick = viewModel::limpiarFiltros, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("Limpiar filtros")
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.avisos, key = { it.id }) { aviso ->
                        AvisoCard(aviso = aviso, onClick = { onAbrirAviso(aviso) })
                    }
                }
            }
        }
    }

    if (mostrarFiltros) {
        AvisosFiltrosScreen(
            filtroActual = state.filtro,
            onAplicar = { nuevo -> viewModel.aplicarFiltro(nuevo); mostrarFiltros = false },
            onCerrar = { mostrarFiltros = false },
        )
    }
}

@Composable
private fun AvisoCard(aviso: Aviso, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (aviso.academiaEscudoUrl != null) {
                    AsyncImage(model = aviso.academiaEscudoUrl, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(aviso.academiaNombre, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(aviso.titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                "${aviso.categoria} · ${aviso.ubicacionDistrito}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
            aviso.fechaPrueba?.let { fecha ->
                Text(
                    "Prueba: ${SimpleDateFormat("d MMM yyyy", Locale("es", "PE")).format(fecha)} · ${aviso.hora}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
