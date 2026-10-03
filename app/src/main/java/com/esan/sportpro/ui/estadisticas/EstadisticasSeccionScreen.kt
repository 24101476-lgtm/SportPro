package com.esan.sportpro.ui.estadisticas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.estadisticas.EstadisticaJugador

private sealed interface EstadisticasDestino {
    data object Equipo : EstadisticasDestino
    data object Jugadores : EstadisticasDestino
    data object Filtros : EstadisticasDestino
    data class JugadorDetalle(val jugadorId: String) : EstadisticasDestino
}

/**
 * Sección "Estadísticas" del tab Estadísticas y Comunicación (US-026, frames 121 a 125).
 * El [EstadisticasViewModel] se comparte entre las 4 pantallas internas (scoped a esta sección
 * vía [hiltViewModel]) para no repetir la carga de datos al navegar entre ellas.
 */
@Composable
fun EstadisticasSeccionScreen(modifier: Modifier = Modifier) {
    val viewModel: EstadisticasViewModel = hiltViewModel()
    var destino by rememberSaveable(stateSaver = DestinoSaver) { mutableStateOf<EstadisticasDestino>(EstadisticasDestino.Equipo) }
    val state by viewModel.uiState.collectAsState()

    when (val actual = destino) {
        is EstadisticasDestino.Equipo -> EstadisticasEquipoScreen(
            onVerJugadores = { destino = EstadisticasDestino.Jugadores },
            onAbrirFiltros = { destino = EstadisticasDestino.Filtros },
            modifier = modifier,
            viewModel = viewModel,
        )
        is EstadisticasDestino.Jugadores -> EstadisticasJugadoresTablaScreen(
            onVolver = { destino = EstadisticasDestino.Equipo },
            onSeleccionarJugador = { jugador -> destino = EstadisticasDestino.JugadorDetalle(jugador.jugadorId) },
            modifier = modifier,
            viewModel = viewModel,
        )
        is EstadisticasDestino.Filtros -> EstadisticasFiltrosScreen(
            onVolver = { destino = EstadisticasDestino.Equipo },
            modifier = modifier,
            viewModel = viewModel,
        )
        is EstadisticasDestino.JugadorDetalle -> {
            val jugador: EstadisticaJugador? = remember(state.jugadores, actual.jugadorId) {
                state.jugadores.firstOrNull { it.jugadorId == actual.jugadorId }
            }
            if (jugador != null) {
                EstadisticasJugadorDetalleScreen(
                    jugador = jugador,
                    onVolver = { destino = EstadisticasDestino.Jugadores },
                    modifier = modifier,
                )
            } else {
                // Puede ocurrir justo después de recargar datos (p. ej. al cambiar filtros);
                // se redirige a la tabla en vez de mutar el estado directamente en composición.
                LaunchedEffect(actual.jugadorId) { destino = EstadisticasDestino.Jugadores }
            }
        }
    }
}

private val DestinoSaver = androidx.compose.runtime.saveable.Saver<EstadisticasDestino, String>(
    save = { destino ->
        when (destino) {
            is EstadisticasDestino.Equipo -> "equipo"
            is EstadisticasDestino.Jugadores -> "jugadores"
            is EstadisticasDestino.Filtros -> "filtros"
            is EstadisticasDestino.JugadorDetalle -> "detalle|${destino.jugadorId}"
        }
    },
    restore = { guardado ->
        when {
            guardado == "equipo" -> EstadisticasDestino.Equipo
            guardado == "jugadores" -> EstadisticasDestino.Jugadores
            guardado == "filtros" -> EstadisticasDestino.Filtros
            guardado.startsWith("detalle|") -> EstadisticasDestino.JugadorDetalle(guardado.removePrefix("detalle|"))
            else -> EstadisticasDestino.Equipo
        }
    },
)
