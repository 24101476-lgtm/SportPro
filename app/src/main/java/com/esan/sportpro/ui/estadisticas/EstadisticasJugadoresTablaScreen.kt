package com.esan.sportpro.ui.estadisticas

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.estadisticas.ColumnaEstadisticaJugador
import com.esan.sportpro.domain.estadisticas.EstadisticaJugador
import com.esan.sportpro.ui.common.EmptyStateMessage

private val columnas = ColumnaEstadisticaJugador.entries
private val anchoColumna = 84.dp
private val anchoColumnaJugador = 140.dp

/** US-026, criterio 5 — Frame 122_Estadisticas_Jugadores_Tabla, ordenable por encabezado. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadisticasJugadoresTablaScreen(
    onVolver: () -> Unit,
    onSeleccionarJugador: (EstadisticaJugador) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EstadisticasViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val scrollHorizontal = rememberScrollState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
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

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(modifier = Modifier.horizontalScroll(scrollHorizontal)) {
                Column {
                    EncabezadoFila(columnaOrden = state.columnaOrden, ordenAscendente = state.ordenAscendente, onOrdenar = viewModel::cambiarOrden)
                    HorizontalDivider()
                    LazyColumn {
                        items(state.jugadores, key = { it.jugadorId }) { jugador ->
                            FilaJugador(jugador = jugador, onClick = { onSeleccionarJugador(jugador) })
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EncabezadoFila(
    columnaOrden: ColumnaEstadisticaJugador,
    ordenAscendente: Boolean,
    onOrdenar: (ColumnaEstadisticaJugador) -> Unit,
) {
    Row {
        columnas.forEach { columna ->
            val ancho = if (columna == ColumnaEstadisticaJugador.JUGADOR) anchoColumnaJugador else anchoColumna
            Surface(
                modifier = Modifier
                    .width(ancho)
                    .padding(4.dp),
                onClick = { onOrdenar(columna) },
            ) {
                Row(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = columna.etiqueta,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (columnaOrden == columna) {
                        Icon(
                            imageVector = if (ordenAscendente) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaJugador(jugador: EstadisticaJugador, onClick: () -> Unit) {
    val colorTexto = if (jugador.sinAsignar) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Row {
        Surface(modifier = Modifier.width(anchoColumnaJugador), onClick = onClick) {
            Text(
                text = jugador.jugadorNombre,
                modifier = Modifier.padding(8.dp),
                color = colorTexto,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        CeldaValor(jugador.partidosJugados.toString(), colorTexto)
        CeldaValor(jugador.minutosJugados.toString(), colorTexto)
        CeldaValor(jugador.goles.toString(), colorTexto)
        CeldaValor(jugador.asistencias.toString(), colorTexto)
        CeldaValor(jugador.tarjetasAmarillas.toString(), colorTexto)
        CeldaValor(jugador.tarjetasRojas.toString(), colorTexto)
        CeldaValor(jugador.faltas.toString(), colorTexto)
        CeldaValor("%.0f%%".format(jugador.porcentajeAsistenciaEntrenamientos), colorTexto)
    }
}

@Composable
private fun CeldaValor(valor: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text = valor,
        modifier = Modifier
            .width(anchoColumna)
            .padding(8.dp),
        color = color,
        style = MaterialTheme.typography.bodyMedium,
    )
}
