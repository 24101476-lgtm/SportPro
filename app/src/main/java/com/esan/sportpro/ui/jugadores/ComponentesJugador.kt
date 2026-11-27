package com.esan.sportpro.ui.jugadores

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.esan.sportpro.core.theme.SportProTema
import com.esan.sportpro.domain.jugadores.PieHabil
import com.esan.sportpro.ui.partido.abreviaturaPosicion
import com.esan.sportpro.ui.partido.colorPosicion

/** Píldora con la abreviatura de la posición ("MC", "DFC"...) en el color de su línea. */
@Composable
fun InsigniaPosicion(etiqueta: String?, modifier: Modifier = Modifier) {
    val color = if (etiqueta.isNullOrBlank()) MaterialTheme.colorScheme.outline else colorPosicion(etiqueta)
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.14f),
        contentColor = color,
        modifier = modifier,
    ) {
        Text(
            if (etiqueta.isNullOrBlank()) "—" else abreviaturaPosicion(etiqueta),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(40.dp).padding(vertical = 3.dp),
        )
    }
}

/** Foto del jugador con un anillo del color de su posición. */
@Composable
internal fun FotoConAnillo(url: String?, iniciales: String, posicion: String, tamano: Dp, nombre: String? = null) {
    val anillo = if (posicion.isBlank()) MaterialTheme.colorScheme.outlineVariant else colorPosicion(posicion)
    Box(
        modifier = Modifier
            .size(tamano + 8.dp)
            .border(BorderStroke(2.5.dp, anillo), CircleShape)
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        FotoJugador(url, iniciales, tamano = tamano, nombre = nombre)
    }
}

/** Dorsal en un círculo lima suave. */
@Composable
internal fun DorsalJugador(dorsal: Int, tamano: Dp = 44.dp) {
    val acento = SportProTema.colores.acento
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(tamano)
            .clip(CircleShape)
            .background(acento.copy(alpha = 0.35f)),
    ) {
        Text(
            "$dorsal",
            style = if (tamano >= 52.dp) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Bloque blanco con encabezado de ícono en color suave. Agrupa los datos de la ficha y los campos
 * del formulario para que no queden como una lista larga y plana.
 */
@Composable
internal fun BloqueJugador(
    titulo: String,
    icono: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.14f)),
                ) {
                    Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(19.dp))
                }
                Column(Modifier.padding(start = 10.dp)) {
                    Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (subtitulo != null) {
                        Text(
                            subtitulo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            contenido()
        }
    }
}

/** Fila "etiqueta · valor" dentro de un [BloqueJugador]. */
@Composable
internal fun DatoJugador(etiqueta: String, valor: String?) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            valor?.ifBlank { null } ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.2f),
        )
    }
}

/** Cuadro de estadística: número grande sobre un fondo de color suave. */
@Composable
internal fun CuadroEstadistica(valor: String, etiqueta: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(vertical = 12.dp, horizontal = 6.dp),
    ) {
        Text(valor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = color)
        Text(
            etiqueta,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Pie hábil: tres botones en fila (con tan pocas opciones es más rápido que un combo). */
@Composable
internal fun SelectorPieHabil(seleccionado: String, onElegir: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier.fillMaxWidth()) {
        PieHabil.entries.forEach { pie ->
            val elegido = seleccionado == pie.etiqueta
            Surface(
                onClick = { onElegir(pie.etiqueta) },
                shape = RoundedCornerShape(14.dp),
                color = if (elegido) SportProTema.colores.acento.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    if (elegido) SportProTema.colores.textoAcento else MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
                ) {
                    if (elegido) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        pie.etiqueta,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (elegido) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** Fondo claro del encabezado de la ficha: tinte del color de la posición que se desvanece. */
@Composable
internal fun fondoEncabezado(posicion: String): Brush {
    val base = if (posicion.isBlank()) SportProTema.colores.acento else colorPosicion(posicion)
    return Brush.verticalGradient(
        0f to base.copy(alpha = 0.20f),
        1f to SportProTema.colores.acento.copy(alpha = 0.10f),
    )
}
