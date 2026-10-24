package com.esan.sportpro.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esan.sportpro.ui.partido.ColoresPartido

/**
 * Elementos visuales compartidos por todos los módulos, tomados del estilo de Partido en vivo
 * (encabezado con degradado verde, tarjetas redondeadas con sombra suave y estados vacíos con
 * círculo de color) para que la app se vea como una sola.
 */

/** Encabezado de módulo con degradado verde; sustituye a la barra superior simple de cada pantalla. */
@Composable
fun EncabezadoModulo(
    titulo: String,
    subtitulo: String,
    icono: ImageVector,
    modifier: Modifier = Modifier,
    accion: (@Composable () -> Unit)? = null,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 4.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Brush.linearGradient(listOf(ColoresPartido.VerdeOscuro, ColoresPartido.Verde)))
                .padding(20.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(
                    subtitulo,
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.18f), modifier = Modifier.size(56.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icono, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
            accion?.invoke()
        }
    }
}

/** Tarjeta redondeada con sombra suave; si recibe [onClick] es pulsable. */
@Composable
fun TarjetaSportPro(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contenido: @Composable () -> Unit,
) {
    if (onClick != null) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = modifier.fillMaxWidth(),
            content = contenido,
        )
    } else {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = modifier.fillMaxWidth(),
            content = contenido,
        )
    }
}

/** Estado vacío con círculo de color e ícono, como el de la lista de partidos. */
@Composable
fun EstadoVacioModulo(
    titulo: String,
    mensaje: String,
    icono: ImageVector,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 48.dp, start = 24.dp, end = 24.dp),
    ) {
        Surface(shape = CircleShape, color = ColoresPartido.Verde.copy(alpha = 0.12f), modifier = Modifier.size(96.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icono, contentDescription = null, tint = ColoresPartido.Verde, modifier = Modifier.size(52.dp))
            }
        }
        Text(titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
        Text(
            mensaje,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
