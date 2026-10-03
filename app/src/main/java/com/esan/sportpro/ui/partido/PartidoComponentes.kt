package com.esan.sportpro.ui.partido

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsHandball
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.esan.sportpro.domain.partido.EquipoEvento
import com.esan.sportpro.domain.partido.EstadoPartido
import com.esan.sportpro.domain.partido.EventoPartido
import com.esan.sportpro.domain.partido.JugadorEspecial
import com.esan.sportpro.domain.partido.Partido
import com.esan.sportpro.domain.partido.ResultadoPenal
import com.esan.sportpro.domain.partido.TiposEvento
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val localeEs = Locale("es", "PE")

fun formatearFecha(ms: Long): String = SimpleDateFormat("dd/MM/yyyy HH:mm", localeEs).format(Date(ms))

fun formatearFechaHora(ms: Long): String = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", localeEs).format(Date(ms))

fun formatearCronometro(segundos: Long): String = "%02d:%02d".format(segundos / 60, segundos % 60)

fun etiquetaEstado(p: Partido): String = when (p.estado) {
    EstadoPartido.PROGRAMADO -> "Programado"
    EstadoPartido.EN_CURSO -> when {
        p.enDescanso -> "Descanso"
        p.inicioSegundoTiempoMs != null -> "2.º tiempo"
        else -> "1.er tiempo"
    }
    EstadoPartido.FINALIZADO -> "Finalizado"
    EstadoPartido.ACTA_CERRADA -> "Acta cerrada"
}

/** Texto de detalle del evento para la cronología y el historial. */
fun detalleEvento(e: EventoPartido, partido: Partido?): String {
    val equipo = when (e.equipo) {
        EquipoEvento.PROPIO -> partido?.nombreEquipo ?: "Propio"
        EquipoEvento.RIVAL -> partido?.nombreRival ?: "Rival"
        null -> null
    }
    val jugador = e.jugadorNombre ?: e.jugadorId?.let { nombreEspecial(it) }
    val secundario = e.jugadorSecundarioNombre ?: e.jugadorSecundarioId?.let { nombreEspecial(it) }
    val partes = buildList {
        equipo?.let { add(it) }
        when (e.tipoId) {
            TiposEvento.CAMBIO -> {
                add("Sale: ${jugador ?: SIN_INFO}")
                add("Entra: ${secundario ?: SIN_INFO}")
            }
            TiposEvento.GOL -> {
                jugador?.let { add(it) }
                add("Asistencia: ${secundario ?: SIN_INFO}")
            }
            else -> jugador?.let { add(it) }
        }
        e.resultadoPenal?.let { add(if (it == ResultadoPenal.CONVERTIDO) "Convertido" else "Fallado") }
    }
    return partes.joinToString(" · ")
}

const val SIN_INFO = "Sin información"

fun nombreEspecial(id: String): String? = when (id) {
    JugadorEspecial.RIVAL -> JugadorEspecial.NOMBRE_RIVAL
    JugadorEspecial.NO_IDENTIFICADO -> JugadorEspecial.NOMBRE_NO_IDENTIFICADO
    else -> null
}

@Composable
fun Etiqueta(texto: String, color: Color, contenido: Color = Color.White, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(50), color = color, modifier = modifier) {
        Text(
            texto,
            color = contenido,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
fun EstadoPartidoChip(partido: Partido) {
    val color = when (partido.estado) {
        EstadoPartido.PROGRAMADO -> ColoresPartido.Azul
        EstadoPartido.EN_CURSO -> if (partido.enDescanso) ColoresPartido.Naranja else ColoresPartido.Rojo
        EstadoPartido.FINALIZADO -> ColoresPartido.Verde
        EstadoPartido.ACTA_CERRADA -> ColoresPartido.Gris
    }
    Surface(shape = RoundedCornerShape(50), color = color) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            if (partido.estado == EstadoPartido.EN_CURSO && !partido.enDescanso) {
                PuntoEnVivo(Color.White)
                androidx.compose.foundation.layout.Spacer(Modifier.width(6.dp))
            }
            Text(
                if (partido.estado == EstadoPartido.EN_CURSO && !partido.enDescanso) {
                    "EN VIVO · ${etiquetaEstado(partido)}"
                } else {
                    etiquetaEstado(partido)
                },
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            )
        }
    }
}

/** Franja fija que se muestra mientras no hay conexión. */
@Composable
fun BannerSinConexion(pendientes: Int) {
    Surface(color = Color(0xFFB71C1C), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.WifiOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
            Text(
                "Sin conexión. $pendientes eventos pendientes de sincronizar",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

// ---------------------------------------------------------------- Identidad visual del módulo

/** Colores del módulo Partido (pensados para leerse rápido en la cancha). */
object ColoresPartido {
    val VerdeOscuro = Color(0xFF0B3D2E)
    val Verde = Color(0xFF1B7A3E)
    val VerdeClaro = Color(0xFF2E9E5B)
    val Rojo = Color(0xFFD32F2F)
    val Amarillo = Color(0xFFFBC02D)
    val Azul = Color(0xFF1565C0)
    val Naranja = Color(0xFFEF6C00)
    val Morado = Color(0xFF6A1B9A)
    val Gris = Color(0xFF607D8B)
}

/** Ícono representativo de cada tipo de evento del catálogo. */
fun iconoTipo(tipoId: String): androidx.compose.ui.graphics.vector.ImageVector = when (tipoId) {
    TiposEvento.INICIO_PARTIDO -> Icons.Default.PlayArrow
    TiposEvento.FIN_PRIMER_TIEMPO -> Icons.Default.Pause
    TiposEvento.INICIO_SEGUNDO_TIEMPO -> Icons.Default.PlayCircle
    TiposEvento.FIN_PARTIDO -> Icons.Default.SportsScore
    TiposEvento.GOL -> Icons.Default.SportsSoccer
    TiposEvento.FALTA -> Icons.Default.Gavel
    TiposEvento.TARJETA_AMARILLA, TiposEvento.TARJETA_ROJA -> Icons.Default.Style
    TiposEvento.CAMBIO -> Icons.Default.SwapHoriz
    TiposEvento.PENAL -> Icons.Default.GpsFixed
    TiposEvento.TIRO_ESQUINA -> Icons.Default.Flag
    TiposEvento.FUERA_DE_JUEGO -> Icons.Default.Block
    TiposEvento.SAQUE_LATERAL -> Icons.Default.SportsHandball
    TiposEvento.SAQUE_META -> Icons.Default.Shield
    else -> Icons.Default.Event
}

/** Color de acento de cada tipo de evento. */
fun colorTipo(tipoId: String): Color = when (tipoId) {
    TiposEvento.GOL -> ColoresPartido.Verde
    TiposEvento.TARJETA_AMARILLA -> ColoresPartido.Amarillo
    TiposEvento.TARJETA_ROJA -> ColoresPartido.Rojo
    TiposEvento.CAMBIO -> ColoresPartido.Azul
    TiposEvento.PENAL -> ColoresPartido.Morado
    TiposEvento.FALTA -> ColoresPartido.Naranja
    TiposEvento.INICIO_PARTIDO, TiposEvento.INICIO_SEGUNDO_TIEMPO,
    TiposEvento.FIN_PRIMER_TIEMPO, TiposEvento.FIN_PARTIDO -> ColoresPartido.VerdeOscuro
    else -> ColoresPartido.Gris
}

/** Círculo con el ícono del tipo de evento. */
@Composable
fun IconoEvento(tipoId: String, tamano: androidx.compose.ui.unit.Dp = 36.dp, apagado: Boolean = false) {
    val color = if (apagado) ColoresPartido.Gris else colorTipo(tipoId)
    Surface(shape = androidx.compose.foundation.shape.CircleShape, color = color, modifier = Modifier.size(tamano)) {
        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
            Icon(
                iconoTipo(tipoId),
                contentDescription = null,
                tint = if (tipoId == TiposEvento.TARJETA_AMARILLA && !apagado) Color.Black else Color.White,
                modifier = Modifier.size(tamano * 0.55f),
            )
        }
    }
}

/** Escudo genérico con las iniciales del equipo. */
@Composable
fun Escudo(nombre: String, fondo: Color, tamano: androidx.compose.ui.unit.Dp = 48.dp) {
    val iniciales = nombre.trim().split(" ").filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().uppercase() }.ifBlank { "?" }
    Surface(
        shape = androidx.compose.foundation.shape.CircleShape,
        color = fondo,
        border = androidx.compose.foundation.BorderStroke(2.dp, Color.White.copy(alpha = 0.7f)),
        modifier = Modifier.size(tamano),
    ) {
        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
            Text(
                iniciales,
                color = Color.White,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                fontSize = (tamano.value * 0.36f).sp,
            )
        }
    }
}

/** Punto rojo que late mientras el partido está en vivo. */
@Composable
fun PuntoEnVivo(color: Color = Color(0xFFFF5252)) {
    val transicion = rememberInfiniteTransition(label = "vivo")
    val alpha: Float by transicion.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(8.dp)
            .alpha(alpha)
            .background(color, androidx.compose.foundation.shape.CircleShape),
    )
}

/** Texto del reloj: mm:ss, con tiempo añadido en el 1.er tiempo. */
fun textoReloj(partido: Partido, reloj: com.esan.sportpro.domain.partido.ReglasPartido.Reloj): String = when {
    partido.estado == EstadoPartido.PROGRAMADO -> "00:00"
    partido.estado != EstadoPartido.EN_CURSO -> "Final"
    partido.enDescanso -> "Descanso"
    reloj.adicion > 0 -> "${partido.duracionTiempoMin}:00 +${reloj.adicion}'"
    else -> formatearCronometro(reloj.segundosJuego)
}
