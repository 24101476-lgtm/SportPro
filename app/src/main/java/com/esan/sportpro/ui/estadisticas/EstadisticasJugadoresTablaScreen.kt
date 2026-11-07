package com.esan.sportpro.ui.estadisticas

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.estadisticas.ColumnaEstadisticaJugador
import com.esan.sportpro.domain.estadisticas.EstadisticaJugador
import com.esan.sportpro.ui.common.BarraSuperiorSportPro
import com.esan.sportpro.ui.common.EmptyStateMessage
import com.esan.sportpro.ui.common.TarjetaSportPro

/**
 * US-026, criterio 5 — Frame 122_Estadisticas_Jugadores_Tabla. Cada jugador es una tarjeta con
 * foto y sus cifras clave; el orden se cambia con los chips (volver a tocar invierte el sentido).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadisticasJugadoresTablaScreen(
    onVolver: () -> Unit,
    onSeleccionarJugador: (EstadisticaJugador) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EstadisticasViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            BarraSuperiorSportPro(
                title = { Text("Jugadores") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        if (state.jugadores.isEmpty()) {
            EmptyStateMessage(
                mensaje = "Aún no hay partidos registrados en este periodo",
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ColumnaEstadisticaJugador.entries.forEach { columna ->
                    val activa = state.columnaOrden == columna
                    FilterChip(
                        selected = activa,
                        onClick = { viewModel.cambiarOrden(columna) },
                        label = { Text(columna.etiqueta) },
                        trailingIcon = if (activa) {
                            {
                                Icon(
                                    imageVector = if (state.ordenAscendente) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                )
                            }
                        } else null,
                    )
                }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(state.jugadores, key = { _, j -> j.jugadorId }) { _, jugador ->
                    TarjetaJugadorLista(jugador = jugador, onClick = { onSeleccionarJugador(jugador) })
                }
            }
        }
    }
}

@Composable
private fun TarjetaJugadorLista(jugador: EstadisticaJugador, onClick: () -> Unit) {
    TarjetaSportPro(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AvatarEstadistica(jugador, tamano = 48.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    jugador.jugadorNombre,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (jugador.sinAsignar) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "${jugador.partidosJugados} PJ · ${jugador.minutosJugados} min · " +
                        "TA ${jugador.tarjetasAmarillas} · TR ${jugador.tarjetasRojas}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!jugador.sinAsignar) {
                    Text(
                        "Entrenamientos ${"%.0f".format(jugador.porcentajeAsistenciaEntrenamientos)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            CifraDestacada(valor = jugador.goles, etiqueta = "Goles")
            CifraDestacada(valor = jugador.asistencias, etiqueta = "Asist.")
        }
    }
}

@Composable
private fun CifraDestacada(valor: Int, etiqueta: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valor.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
