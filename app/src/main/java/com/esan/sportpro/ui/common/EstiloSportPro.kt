package com.esan.sportpro.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.contentDescription
import com.esan.sportpro.core.theme.FuenteNumeros
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.esan.sportpro.R
import com.esan.sportpro.core.theme.EstilosSportPro
import com.esan.sportpro.core.theme.ModoTema
import com.esan.sportpro.core.theme.PreferenciaTema
import com.esan.sportpro.core.theme.PaletaSportPro
import com.esan.sportpro.core.theme.SportProTema

/**
 * Componentes visuales compartidos por todos los módulos (línea gráfica de SportPro).
 *
 * Reglas para mantener la app uniforme:
 * - Pantallas secundarias (detalle, formulario): [BarraSuperiorSportPro].
 * - Pantalla raíz de cada módulo: [EncabezadoModulo] arriba del contenido.
 * - Listas: cada elemento dentro de [TarjetaSportPro].
 * - Carga, error y vacío: [CargandoSportPro], [ErrorSportPro] y [EstadoVacioModulo].
 * - Títulos de sección: [TituloSeccion]. Estados: [ChipEstado] con un [TonoEstado].
 * - Acción principal de la pantalla: [BotonPrincipalSportPro] (una por pantalla).
 * - Colores: siempre de `MaterialTheme.colorScheme`, `SportProTema.colores` o [PaletaSportPro],
 *   nunca `Color(0x…)`.
 */

/** Degradado de marca (musgo). Se conserva para las pantallas que lo usan directamente. */
val DegradadoSportPro: Brush
    get() = Brush.linearGradient(listOf(PaletaSportPro.VerdeOscuro, PaletaSportPro.Verde))

/** Foto de cancha en blanco y negro, tal como se guardó en `res/drawable-nodpi`. */
enum class FotoSportPro(val recurso: Int) {
    CANCHA(R.drawable.foto_cancha),
    ESTADIO(R.drawable.foto_estadio),
    ENTRENADOR(R.drawable.foto_entrenador),
    JUVENILES(R.drawable.foto_juveniles),
    CONOS(R.drawable.foto_conos),
    BOTINES(R.drawable.foto_botines),
    VESTUARIO(R.drawable.foto_vestuario),
    CELEBRACION(R.drawable.foto_celebracion),
    BALON(R.drawable.foto_balon),
}

private val SinColor = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/** Color suavizado (72 % de saturación) para que las fotos combinen con la paleta. */
private val ColorSuave = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.72f) })

/**
 * Fondo fotográfico con velo oscuro para que el texto blanco se lea siempre, en modo claro y oscuro.
 * Por defecto la foto va en B/N (encabezados); con [aColor] va a color suave (fotos de contenido).
 * El contenido va encima, en [contenido].
 */
@Composable
fun FondoFotoSportPro(
    foto: FotoSportPro,
    modifier: Modifier = Modifier,
    forma: Shape = RoundedCornerShape(28.dp),
    velo: Brush = Brush.verticalGradient(
        0f to PaletaSportPro.Carbon.copy(alpha = 0.25f),
        1f to PaletaSportPro.Carbon.copy(alpha = 0.92f),
    ),
    aColor: Boolean = false,
    contenido: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.clip(forma).background(PaletaSportPro.Carbon)) {
        Image(
            painter = painterResource(foto.recurso),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            colorFilter = if (aColor) ColorSuave else SinColor,
            modifier = Modifier.matchParentSize(),
        )
        Box(modifier = Modifier.matchParentSize().background(velo))
        contenido()
    }
}

/** El gesto de la marca: un trazo de pincel lima. Úsalo una sola vez por pantalla. */
@Composable
fun TrazoSportPro(
    modifier: Modifier = Modifier,
    color: Color = SportProTema.colores.acento,
    grosor: Dp = 6.dp,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val trazo = Path().apply {
            moveTo(w * 0.02f, h * 0.70f)
            cubicTo(w * 0.25f, h * 0.10f, w * 0.45f, h * 1.00f, w * 0.65f, h * 0.45f)
            cubicTo(w * 0.78f, h * 0.15f, w * 0.90f, h * 0.30f, w * 0.98f, h * 0.40f)
        }
        drawPath(trazo, color = color, style = Stroke(width = grosor.toPx(), cap = StrokeCap.Round))
    }
}

/**
 * Barra superior estándar para detalle y formularios. Tiene los mismos parámetros que
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
    TopAppBar(
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground,
        ),
    )
}

/**
 * Encabezado de la pantalla raíz de un módulo: foto de cancha en B/N con velo musgo, título
 * grande y subtítulo. El texto es blanco en ambos modos (la foto siempre es oscura), por eso la
 * [accion] puede seguir usando íconos blancos.
 */
@Composable
fun EncabezadoModulo(
    titulo: String,
    subtitulo: String,
    icono: ImageVector,
    modifier: Modifier = Modifier,
    accion: (@Composable () -> Unit)? = null,
    foto: FotoSportPro = FotoSportPro.CANCHA,
) {
    FondoFotoSportPro(
        foto = foto,
        modifier = modifier.fillMaxWidth().heightIn(min = 132.dp),
        velo = Brush.horizontalGradient(
            0f to PaletaSportPro.MusgoProfundo.copy(alpha = 0.95f),
            1f to PaletaSportPro.MusgoProfundo.copy(alpha = 0.55f),
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 22.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    titulo,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitulo,
                    color = Color.White.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.16f), modifier = Modifier.size(52.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icono, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
            accion?.invoke()
        }
    }
}

/**
 * Tarjeta sin sombra: la profundidad viene del contraste entre fondo y superficie.
 * Si recibe [onClick] es pulsable y se encoge levemente al presionarla.
 */
@Composable
fun TarjetaSportPro(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contenido: @Composable () -> Unit,
) {
    if (onClick != null) {
        val interaccion = remember { MutableInteractionSource() }
        val presionada by interaccion.collectIsPressedAsState()
        val escala by animateFloatAsState(if (presionada) 0.97f else 1f, tween(150), label = "escalaTarjeta")
        Surface(
            onClick = onClick,
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            interactionSource = interaccion,
            modifier = modifier.fillMaxWidth().scale(escala),
            content = contenido,
        )
    } else {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = modifier.fillMaxWidth(),
            content = contenido,
        )
    }
}

/** Botón de la acción principal de la pantalla: píldora lima con texto oscuro. Uno por pantalla. */
@Composable
fun BotonPrincipalSportPro(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    icono: ImageVector? = null,
    cargando: Boolean = false,
) {
    val colores = SportProTema.colores
    Button(
        onClick = onClick,
        enabled = habilitado,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = colores.acento,
            contentColor = colores.sobreAcento,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        modifier = modifier.heightIn(min = 52.dp),
    ) {
        if (cargando) {
            CircularProgressIndicator(
                color = LocalContentColor.current,
                strokeWidth = 2.dp,
                strokeCap = StrokeCap.Round,
                modifier = Modifier.size(20.dp),
            )
        } else {
            if (icono != null) {
                Icon(icono, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(texto, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

/** Un número grande con su etiqueta, para [FilaEstadisticas]. */
data class DatoEstadistica(val valor: String, val etiqueta: String)

/** Fila de 2 a 4 números grandes separados por divisores verticales. */
@Composable
fun FilaEstadisticas(datos: List<DatoEstadistica>, modifier: Modifier = Modifier) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.height(IntrinsicSize.Min).padding(vertical = 16.dp)) {
            datos.forEachIndexed { indice, dato ->
                if (indice > 0) {
                    VerticalDivider(modifier = Modifier.fillMaxHeight(), color = MaterialTheme.colorScheme.outline)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
                ) {
                    Text(dato.valor, style = EstilosSportPro.NumeroMediano, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        dato.etiqueta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}

/** Estado vacío: una invitación a actuar, con ícono, título y mensaje. */
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
        IconoCircular(icono = icono, tamano = 88)
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
        CircularProgressIndicator(color = SportProTema.colores.textoAcento, strokeCap = StrokeCap.Round)
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
        @Composable { BotonPrincipalSportPro(texto = "Reintentar", onClick = reintentar) }
    }
    EstadoVacioModulo(
        titulo = titulo,
        mensaje = mensaje,
        icono = Icons.Default.CloudOff,
        modifier = modifier,
        accion = accion,
    )
}

/** Título de sección dentro de una pantalla: texto fuerte, sin decoración. */
@Composable
fun TituloSeccion(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.fillMaxWidth().padding(top = 16.dp, bottom = 2.dp),
    )
}

/** Fila "etiqueta / valor" para pantallas de detalle. Un valor vacío se muestra como "Sin información". */
@Composable
fun FilaDatoSportPro(etiqueta: String, valor: String?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
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
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Significado de un estado; define el color del [ChipEstado]. */
enum class TonoEstado { EXITO, ADVERTENCIA, ERROR, INFORMACION, NEUTRO }

@Composable
private fun TonoEstado.color(): Color {
    val c = SportProTema.colores
    return when (this) {
        TonoEstado.EXITO -> c.exito
        TonoEstado.ADVERTENCIA -> c.advertencia
        TonoEstado.ERROR -> c.error
        TonoEstado.INFORMACION -> c.informacion
        TonoEstado.NEUTRO -> c.neutro
    }
}

/** Etiqueta pequeña de estado ("Activo", "Pendiente", "Menor"...): píldora con borde. */
@Composable
fun ChipEstado(texto: String, tono: TonoEstado, modifier: Modifier = Modifier) {
    val color = tono.color()
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.12f),
        modifier = modifier,
    ) {
        Text(
            texto,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

/** Ícono dentro de un cuadro redondeado de color suave. */
@Composable
fun IconoCircular(
    icono: ImageVector,
    modifier: Modifier = Modifier,
    tamano: Int = 44,
    color: Color = SportProTema.colores.textoAcento,
) {
    Surface(
        shape = RoundedCornerShape((tamano * 0.32f).dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.size(tamano.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size((tamano * 0.52f).dp))
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
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** Marca de la app para login y registro: foto del estadio, nombre grande, trazo lima y subtítulo. */
@Composable
fun MarcaSportPro(subtitulo: String, modifier: Modifier = Modifier) {
    FondoFotoSportPro(
        foto = FotoSportPro.ESTADIO,
        forma = RoundedCornerShape(32.dp),
        modifier = modifier.fillMaxWidth().heightIn(min = 220.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.Bottom,
            modifier = Modifier.matchParentSize().padding(24.dp),
        ) {
            Text("SportPro", color = Color.White, style = MaterialTheme.typography.displayMedium)
            TrazoSportPro(modifier = Modifier.width(170.dp).height(18.dp).padding(top = 2.dp))
            Text(
                subtitulo,
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

private fun iconoModo(modo: ModoTema): ImageVector = when (modo) {
    ModoTema.SISTEMA -> Icons.Default.BrightnessAuto
    ModoTema.CLARO -> Icons.Default.LightMode
    ModoTema.OSCURO -> Icons.Default.DarkMode
}

/** Selector de apariencia (Sistema, Claro, Oscuro) para la pestaña "Más". Se guarda en el teléfono. */
@Composable
fun SelectorApariencia(modifier: Modifier = Modifier) {
    val actual = PreferenciaTema.modo
    val colores = SportProTema.colores
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ModoTema.entries.forEach { modo ->
                val elegido = modo == actual
                Surface(
                    onClick = { PreferenciaTema.cambiar(modo) },
                    shape = CircleShape,
                    color = if (elegido) colores.acento else Color.Transparent,
                    contentColor = if (elegido) colores.sobreAcento else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics { selected = elegido },
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 12.dp),
                    ) {
                        Icon(iconoModo(modo), contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(modo.etiqueta, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

/**
 * Botón redondo que cambia la apariencia (Sistema → Claro → Oscuro).
 * Con [sobreFoto] es blanco translúcido; si no, usa los colores del fondo de la pantalla.
 */
@Composable
fun BotonApariencia(modifier: Modifier = Modifier, sobreFoto: Boolean = true) {
    val modo = PreferenciaTema.modo
    Surface(
        onClick = { PreferenciaTema.siguiente() },
        shape = CircleShape,
        color = if (sobreFoto) Color.White.copy(alpha = 0.16f) else Color.Transparent,
        contentColor = if (sobreFoto) Color.White else MaterialTheme.colorScheme.onBackground,
        border = if (sobreFoto) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.size(48.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                iconoModo(modo),
                contentDescription = "Apariencia: ${modo.etiqueta}. Toca para cambiarla",
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** Colores de los campos de texto: superficie, borde y etiqueta de acento al enfocar. */
@Composable
fun coloresCampoSportPro(): TextFieldColors {
    val acento = SportProTema.colores.textoAcento
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = acento,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedLabelColor = acento,
        cursorColor = acento,
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        errorContainerColor = MaterialTheme.colorScheme.surface,
    )
}

/* ---------- Escudos de equipo ---------- */

/** Colores de camiseta para los escudos generados. Cada par: fondo y color de la franja. */
private val KitsEscudo = listOf(
    Color(0xFF1F4E8C) to Color(0xFFF2F3EC), // azul y blanco
    Color(0xFFB3261E) to Color(0xFF121310), // rojo y negro
    Color(0xFF2E6B2F) to Color(0xFFD6F55A), // verde y lima
    Color(0xFF5B2A86) to Color(0xFFF2B544), // morado y oro
    Color(0xFF0F766E) to Color(0xFFF2F3EC), // turquesa y blanco
    Color(0xFFC2410C) to Color(0xFF121310), // naranja y negro
    Color(0xFF121310) to Color(0xFFD6F55A), // negro y lima
    Color(0xFF8A1538) to Color(0xFF6FA8DC), // granate y celeste
)

private val PalabrasVacias = setOf("de", "del", "la", "las", "el", "los", "y", "club", "fc", "cd", "sc", "academia")

/** Iniciales del escudo: hasta 3 letras de las palabras importantes ("Halcones Dorados FC" → "HD"). */
fun inicialesEscudo(nombre: String): String {
    val palabras = nombre.split(" ", "-", "_").map { it.trim() }.filter { it.isNotEmpty() }
    val importantes = palabras.filter { it.lowercase() !in PalabrasVacias }.ifEmpty { palabras }
    val iniciales = if (importantes.size == 1) {
        importantes.first().take(3)
    } else {
        importantes.take(3).joinToString("") { it.take(1) }
    }
    return iniciales.uppercase().ifBlank { "?" }
}

/**
 * Escudos de prueba para la demo: si el nombre del equipo coincide, se usa la imagen incluida en la app.
 * Cuando cada academia suba su escudo (fase 4), la imagen subida tendrá prioridad.
 */
/** Nombre en minúsculas, sin tildes ni espacios de más: "Atlètic  Catalunya" → "atletic catalunya". */
fun normalizarNombreEquipo(nombre: String): String =
    java.text.Normalizer.normalize(nombre.trim().lowercase(), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("\\s+"), " ")

private fun escudoDemo(nombre: String): Int? {
    val n = normalizarNombreEquipo(nombre)
    return when {
        listOf("atletic", "catalunya", "barcelona", "barca").any { it in n } -> R.drawable.escudo_demo_atletic
        listOf("cantomar", "cantolao", "delfin").any { it in n } -> R.drawable.escudo_demo_cantomar
        else -> null
    }
}

/** Contorno de escudo clásico: borde superior con hombros y punta abajo. */
private fun contornoEscudo(ancho: Float, alto: Float): Path = Path().apply {
    moveTo(ancho * 0.50f, 0f)
    cubicTo(ancho * 0.68f, alto * 0.07f, ancho * 0.86f, alto * 0.07f, ancho, alto * 0.04f)
    lineTo(ancho, alto * 0.46f)
    cubicTo(ancho, alto * 0.74f, ancho * 0.78f, alto * 0.90f, ancho * 0.50f, alto)
    cubicTo(ancho * 0.22f, alto * 0.90f, 0f, alto * 0.74f, 0f, alto * 0.46f)
    lineTo(0f, alto * 0.04f)
    cubicTo(ancho * 0.14f, alto * 0.07f, ancho * 0.32f, alto * 0.07f, ancho * 0.50f, 0f)
    close()
}

/**
 * Escudo generado a partir del nombre del equipo: los colores y el diseño (liso, mitades, banda
 * diagonal o franja) salen siempre igual para el mismo nombre, así cada equipo se reconoce.
 * Original y sin marcas de terceros. En la fase de "escudo propio" se mostrará la imagen subida.
 */
@Composable
fun EscudoEquipo(
    nombre: String,
    modifier: Modifier = Modifier,
    tamano: Dp = 48.dp,
    url: String? = null,
) {
    // 1) El escudo subido (el que se pasa o el de la academia), 2) el de demo, 3) el generado.
    val subido = url?.takeIf { it.isNotBlank() } ?: LocalEscudosEquipos.current[normalizarNombreEquipo(nombre)]
    if (subido != null) {
        // Supabase guarda JPEG (sin transparencia): se muestra dentro de un círculo blanco.
        Surface(
            shape = CircleShape,
            color = Color.White,
            border = BorderStroke(1.dp, PaletaSportPro.Carbon.copy(alpha = 0.10f)),
            modifier = modifier.size(tamano),
        ) {
            coil.compose.AsyncImage(
                model = subido,
                contentDescription = "Escudo de $nombre",
                contentScale = ContentScale.Fit,
                modifier = Modifier.padding(tamano * 0.10f),
            )
        }
        return
    }
    val demo = escudoDemo(nombre)
    if (demo != null) {
        Image(
            painter = painterResource(demo),
            contentDescription = "Escudo de $nombre",
            contentScale = ContentScale.Fit,
            modifier = modifier.size(tamano),
        )
        return
    }
    val semilla = nombre.trim().lowercase().hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }
    val (fondo, franja) = KitsEscudo[semilla % KitsEscudo.size]
    val diseno = (semilla / KitsEscudo.size) % 4
    val iniciales = inicialesEscudo(nombre)
    val textoClaro = fondo.luminancia() < 0.45f
    val colorTexto = if (textoClaro) Color.White else PaletaSportPro.Carbon
    val tamanoTexto = (tamano.value * if (iniciales.length >= 3) 0.30f else 0.38f).sp

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(width = tamano * 0.86f, height = tamano)
            .semantics { contentDescription = "Escudo de $nombre" },
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val borde = w * 0.06f
            val exterior = contornoEscudo(w, h)
            drawPath(exterior, color = Color.White)
            val interior = Path().apply {
                addPath(contornoEscudo(w - borde * 2, h - borde * 2.2f), Offset(borde, borde * 1.1f))
            }
            clipPath(interior) {
                drawRect(fondo)
                when (diseno) {
                    1 -> drawRect(franja, topLeft = Offset(w / 2f, 0f), size = Size(w / 2f, h))
                    2 -> {
                        val banda = Path().apply {
                            moveTo(0f, h * 0.98f); lineTo(w, h * 0.68f); lineTo(w, h * 0.84f); lineTo(0f, h * 1.14f); close()
                        }
                        drawPath(banda, franja)
                    }
                    3 -> drawRect(franja, topLeft = Offset(0f, h * 0.30f), size = Size(w, h * 0.16f))
                }
            }
            drawPath(exterior, color = PaletaSportPro.Carbon.copy(alpha = 0.18f), style = Stroke(width = w * 0.025f))
        }
        Text(
            iniciales,
            style = TextStyle(
                fontFamily = FuenteNumeros,
                fontSize = tamanoTexto,
                lineHeight = tamanoTexto,
                shadow = if (textoClaro) Shadow(PaletaSportPro.Carbon.copy(alpha = 0.45f), Offset(0f, 2f), 6f) else null,
            ),
            color = colorTexto,
            modifier = Modifier
                .padding(top = tamano * 0.04f)
                .then(
                    if (diseno == 1) Modifier.background(fondo.copy(alpha = 0.92f), RoundedCornerShape(4.dp)).padding(horizontal = 3.dp) else Modifier,
                ),
        )
    }
}

private fun Color.luminancia(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue

/** Colores de los chips de filtro: el elegido se rellena con el color del texto; el resto, solo borde. */
@Composable
fun coloresChipSportPro(): androidx.compose.material3.SelectableChipColors =
    androidx.compose.material3.FilterChipDefaults.filterChipColors(
        containerColor = Color.Transparent,
        labelColor = MaterialTheme.colorScheme.onBackground,
        selectedContainerColor = MaterialTheme.colorScheme.onBackground,
        selectedLabelColor = MaterialTheme.colorScheme.background,
    )
