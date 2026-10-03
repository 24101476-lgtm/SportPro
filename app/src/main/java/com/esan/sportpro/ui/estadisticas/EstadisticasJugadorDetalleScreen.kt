package com.esan.sportpro.ui.estadisticas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.esan.sportpro.domain.estadisticas.EstadisticaJugador

/** US-026 — Frame 123_Estadisticas_Jugador_Detalle. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadisticasJugadorDetalleScreen(
    jugador: EstadisticaJugador,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val filas = listOf(
        "Partidos jugados" to jugador.partidosJugados.toString(),
        "Minutos jugados" to jugador.minutosJugados.toString(),
        "Goles" to jugador.goles.toString(),
        "Asistencias" to jugador.asistencias.toString(),
        "Tarjetas amarillas" to jugador.tarjetasAmarillas.toString(),
        "Tarjetas rojas" to jugador.tarjetasRojas.toString(),
        "Faltas cometidas" to jugador.faltas.toString(),
        "% asistencia a entrenamientos" to "%.0f%%".format(jugador.porcentajeAsistenciaEntrenamientos),
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(jugador.jugadorNombre) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(filas) { (etiqueta, valor) ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(text = etiqueta, style = MaterialTheme.typography.bodyLarge)
                        Text(text = valor, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
