package com.esan.sportpro.ui.estadisticas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.esan.sportpro.domain.estadisticas.EstadisticaJugador
import com.esan.sportpro.domain.estadisticas.RankingsJugadores
import com.esan.sportpro.ui.common.TarjetaSportPro

/** Tarjeta del jugador destacado del periodo (ver [RankingsJugadores.puntaje]). */
@Composable
fun TarjetaJugadorDestacado(jugador: EstadisticaJugador, onClick: () -> Unit) {
    TarjetaSportPro(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AvatarEstadistica(jugador, tamano = 72.dp)
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        "Jugador destacado",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
                Text(jugador.jugadorNombre, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "${jugador.goles} goles · ${jugador.asistencias} asistencias · ${jugador.partidosJugados} PJ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Ranking corto (hasta 3) con posición, foto, nombre y valor. No se dibuja si nadie suma. */
@Composable
fun TarjetaRanking(
    titulo: String,
    jugadores: List<EstadisticaJugador>,
    valorTexto: (EstadisticaJugador) -> String,
    onSeleccionar: (EstadisticaJugador) -> Unit,
) {
    if (jugadores.isEmpty()) return
    TarjetaSportPro {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            jugadores.forEachIndexed { indice, jugador ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onSeleccionar(jugador) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "${indice + 1}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (indice == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(20.dp),
                    )
                    AvatarEstadistica(jugador, tamano = 36.dp)
                    Text(
                        jugador.jugadorNombre,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Text(valorTexto(jugador), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** Destacado y rankings del equipo, calculados con [RankingsJugadores]. */
@Composable
fun SeccionRankingsEquipo(
    jugadores: List<EstadisticaJugador>,
    onSeleccionar: (EstadisticaJugador) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        RankingsJugadores.destacado(jugadores)?.let { TarjetaJugadorDestacado(it) { onSeleccionar(it) } }
        TarjetaRanking(
            titulo = "Goleadores",
            jugadores = RankingsJugadores.top(jugadores) { it.goles.toDouble() },
            valorTexto = { "${it.goles}" },
            onSeleccionar = onSeleccionar,
        )
        TarjetaRanking(
            titulo = "Asistencias",
            jugadores = RankingsJugadores.top(jugadores) { it.asistencias.toDouble() },
            valorTexto = { "${it.asistencias}" },
            onSeleccionar = onSeleccionar,
        )
        TarjetaRanking(
            titulo = "Más minutos jugados",
            jugadores = RankingsJugadores.top(jugadores) { it.minutosJugados.toDouble() },
            valorTexto = { "${it.minutosJugados} min" },
            onSeleccionar = onSeleccionar,
        )
        TarjetaRanking(
            titulo = "Asistencia a entrenamientos",
            jugadores = RankingsJugadores.top(jugadores) { it.porcentajeAsistenciaEntrenamientos },
            valorTexto = { "%.0f%%".format(it.porcentajeAsistenciaEntrenamientos) },
            onSeleccionar = onSeleccionar,
        )
    }
}
