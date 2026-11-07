package com.esan.sportpro.ui.estadisticas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.esan.sportpro.domain.estadisticas.EstadisticaJugador

/** Foto de perfil redonda; si no hay foto, las iniciales del nombre. Coil carga y cachea la imagen. */
@Composable
fun AvatarEstadistica(jugador: EstadisticaJugador, tamano: Dp = 44.dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(tamano)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (jugador.fotoUrl.isNotBlank()) {
            AsyncImage(
                model = jugador.fotoUrl,
                contentDescription = "Foto de ${jugador.jugadorNombre}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            val iniciales = jugador.jugadorNombre.split(" ")
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .take(2)
                .joinToString("")
            Text(
                text = iniciales.ifBlank { "?" },
                style = if (tamano >= 72.dp) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleSmall,
            )
        }
    }
}
