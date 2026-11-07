package com.esan.sportpro.ui.estadisticas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.esan.sportpro.domain.estadisticas.EstadisticaJugador
import com.esan.sportpro.ui.common.BarraSuperiorSportPro
import com.esan.sportpro.ui.common.TarjetaSportPro

/** US-026 — Frame 123_Estadisticas_Jugador_Detalle: foto, nombre y cifras en recuadros. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadisticasJugadorDetalleScreen(
    jugador: EstadisticaJugador,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    esDestacado: Boolean = false,
) {
    val cifras = listOf(
        "Partidos jugados" to jugador.partidosJugados.toString(),
        "Minutos jugados" to jugador.minutosJugados.toString(),
        "Goles" to jugador.goles.toString(),
        "Asistencias" to jugador.asistencias.toString(),
        "Tarjetas amarillas" to jugador.tarjetasAmarillas.toString(),
        "Tarjetas rojas" to jugador.tarjetasRojas.toString(),
        "Faltas cometidas" to jugador.faltas.toString(),
        "Asistencia a entrenamientos" to "%.0f%%".format(jugador.porcentajeAsistenciaEntrenamientos),
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            BarraSuperiorSportPro(
                title = { Text("Detalle del jugador") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AvatarEstadistica(jugador, tamano = 120.dp)
            Text(jugador.jugadorNombre, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (esDestacado) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        "Jugador destacado del periodo",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
            cifras.chunked(2).forEach { par ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    par.forEach { (etiqueta, valor) ->
                        TarjetaSportPro(modifier = Modifier.weight(1f)) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(valor, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    etiqueta,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
