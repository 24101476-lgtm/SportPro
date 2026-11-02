package com.esan.sportpro.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.esan.sportpro.core.theme.PaletaSportPro

/**
 * Componentes visuales compartidos por todos los módulos (línea gráfica de SportPro).
 *
 * Reglas para mantener la app uniforme:
 * - Pantallas secundarias (detalle, formulario): [BarraSuperiorSportPro].
 * - Pantalla raíz de cada módulo: [EncabezadoModulo] arriba del contenido.
 * - Listas: cada elemento dentro de [TarjetaSportPro].
 * - Carga, error y vacío: [CargandoSportPro], [ErrorSportPro] y [EstadoVacioModulo].
 * - Títulos de sección: [TituloSeccion]. Estados: [ChipEstado] con un [TonoEstado].
 * - Colores: siempre de `MaterialTheme.colorScheme` o de [PaletaSportPro], nunca `Color(0x…)`.
 */

/** Degradado de marca usado en barras y encabezados. */
val DegradadoSportPro: Brush
    get() = Brush.linearGradient(listOf(PaletaSportPro.VerdeOscuro, PaletaSportPro.Verde))

/**
 * Barra superior estándar con el degradado de marca. Tiene los mismos parámetros que
 * `TopAppBar` de Material 3, así que reemplazarla no cambia el resto de la pantalla.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraSuperiorSportPro(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    Box(modifier = modifier.background(DegradadoSportPro)) {
        TopAppBar(
            title = title,
            navigationIcon = navigationIcon,
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White,
                actionIconContentColor = Color.White,
            ),
        )
    }
}

/** Encabezado de la pantalla raíz de un módulo: tarjeta con degradado, título, subtítulo e ícono. */
@Composable
fun EncabezadoModulo(
    titulo: String,
    subtitulo: String,
    icono: ImageVector,
    modifier: Modifier = Modifier,
    accion: (@Composable () -> Unit)? = null,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        shadowElevation = 4.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(DegradadoSportPro)
                .padding(20.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    titulo,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
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
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = modifier.fillMaxWidth(),
            content = contenido,
        )
    } else {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = modifier.fillMaxWidth(),
            content = contenido,
        )
    }
}

/** Estado vacío con círculo de color e ícono. */
@Composable
fun EstadoVacioModulo(
    titulo: String,
    mensaje: String,
    icono: ImageVector,
    modifier: Modifier = Modifier,
    accion: (@Composable () -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 48.dp, start = 24.dp, end = 24.dp, bottom = 24.dp),
    ) {
        IconoCircular(icono = icono, tamano = 96, color = MaterialTheme.colorScheme.primary)
        Text(
            titulo,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        if (mensaje.isNotBlank()) {
            Text(
                mensaje,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (accion != null) {
            Box(modifier = Modifier.padding(top = 16.dp)) { accion() }
        }
    }
}

/** Indicador de carga centrado, con texto opcional. */
@Composable
fun CargandoSportPro(modifier: Modifier = Modifier, mensaje: String? = null) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        if (mensaje != null) {
            Text(
                mensaje,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

/** Error con mensaje claro y, si se puede, botón para reintentar. */
@Composable
fun ErrorSportPro(
    mensaje: String,
    modifier: Modifier = Modifier,
    titulo: String = "Algo salió mal",
    onReintentar: (() -> Unit)? = null,
) {
    val accion: (@Composable () -> Unit)? = onReintentar?.let { reintentar ->
        @Composable { Button(onClick = reintentar) { Text("Reintentar") } }
    }
    EstadoVacioModulo(
        titulo = titulo,
        mensaje = mensaje,
        icono = Icons.Default.CloudOff,
        modifier = modifier,
        accion = accion,
    )
}

/** Título de sección dentro de una pantalla (detalle, formulario). */
@Composable
fun TituloSeccion(texto: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp)) {
        Text(
            texto,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

/** Fila "etiqueta — valor" para pantallas de detalle. Un valor vacío se muestra como "Sin información". */
@Composable
fun FilaDatoSportPro(etiqueta: String, valor: String?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            valor?.ifBlank { null } ?: "Sin información",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Significado de un estado; define el color del [ChipEstado]. */
enum class TonoEstado { EXITO, ADVERTENCIA, ERROR, INFORMACION, NEUTRO }

private fun TonoEstado.color(): Color = when (this) {
    TonoEstado.EXITO -> PaletaSportPro.Exito
    TonoEstado.ADVERTENCIA -> PaletaSportPro.Advertencia
    TonoEstado.ERROR -> PaletaSportPro.Error
    TonoEstado.INFORMACION -> PaletaSportPro.Informacion
    TonoEstado.NEUTRO -> PaletaSportPro.Neutro
}

/** Etiqueta pequeña de estado ("Activo", "Pendiente", "Menor"...). */
@Composable
fun ChipEstado(texto: String, tono: TonoEstado, modifier: Modifier = Modifier) {
    val color = tono.color()
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f),
        modifier = modifier,
    ) {
        Text(
            texto,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/** Ícono dentro de un círculo de color suave. */
@Composable
fun IconoCircular(icono: ImageVector, modifier: Modifier = Modifier, tamano: Int = 44, color: Color = MaterialTheme.colorScheme.primary) {
    Surface(shape = CircleShape, color = color.copy(alpha = 0.12f), modifier = modifier.size(tamano.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size((tamano * 0.55f).dp))
        }
    }
}

/** Avatar con las iniciales de una persona cuando no hay foto. */
@Composable
fun AvatarIniciales(iniciales: String, modifier: Modifier = Modifier, tamano: Int = 44) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = modifier.size(tamano.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                iniciales.take(2).ifBlank { "?" },
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

/** Marca de la app para login y registro: degradado, ícono de balón, nombre y subtítulo. */
@Composable
fun MarcaSportPro(subtitulo: String, modifier: Modifier = Modifier) {
    Surface(shape = MaterialTheme.shapes.extraLarge, shadowElevation = 6.dp, modifier = modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(DegradadoSportPro)
                .padding(vertical = 28.dp, horizontal = 20.dp),
        ) {
            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.18f), modifier = Modifier.size(72.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.SportsSoccer, contentDescription = null, tint = Color.White, modifier = Modifier.size(42.dp))
                }
            }
            Text(
                "SportPro",
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                subtitulo,
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}
