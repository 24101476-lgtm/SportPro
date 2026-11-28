package com.esan.sportpro.ui.partido

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import kotlin.math.abs
import kotlin.math.roundToInt

data class JugadorCanchaUi(
    val id: String,
    val nombre: String,
    val dorsal: Int?,
    val posicion: String?,
    val titular: Boolean,
    val x: Float,
    val y: Float,
    /** Foto de la ficha del jugador (Supabase); vacía si no tiene o no se pudo leer. */
    val fotoUrl: String = "",
    /** Partido en vivo: goles, tarjetas y cambios del jugador; se dibujan al costado de la carta. */
    val marcas: MarcasEnVivo? = null,
)

private val ANCHO_FICHA = 64.dp
private val ALTO_FICHA = 88.dp

private val CespedOscuro = Color(0xFF1C5527)
private val CespedClaro = Color(0xFF23652F)
private val LineaCal = Color.White.copy(alpha = 0.70f)
private val Lima = Color(0xFFC6F432)

@Composable
fun CanchaTactica(
    titulares: List<JugadorCanchaUi>,
    suplentes: List<JugadorCanchaUi>,
    editable: Boolean,
    modifier: Modifier = Modifier,
    onJugadorClick: (JugadorCanchaUi) -> Unit = {},
    onPosicionJugadorChange: (jugadorId: String, x: Float, y: Float) -> Unit = { _, _, _ -> },
    /** Se suelta una carta sobre otra: ambas cambian de lugar. Recibe (arrastrado, destino). */
    onIntercambiarJugadores: (arrastrado: JugadorCanchaUi, destino: JugadorCanchaUi) -> Unit = { _, _ -> },
    /** Partido en vivo: jugadores sustituidos o expulsados (se muestran atenuados bajo el banco). */
    fuera: List<JugadorCanchaUi> = emptyList(),
    /** Texto de ayuda bajo la cancha; `null` usa el de la pizarra. */
    ayuda: String? = null,
) {
    val posiciones = remember(titulares) {
        mutableStateMapOf<String, Offset>().apply {
            titulares.forEach { jugador ->
                put(jugador.id, Offset(jugador.x.coerceIn(0f, 1f), jugador.y.coerceIn(0f, 1f)))
            }
        }
    }
    var arrastrandoId by remember { mutableStateOf<String?>(null) }
    var origenArrastre by remember { mutableStateOf<Offset?>(null) }
    var objetivoId by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.66f)
                .shadow(12.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(CespedOscuro),
        ) {
            CespedYLineas(Modifier.fillMaxSize())

            val densidad = LocalDensity.current
            val anchoPx = with(densidad) { maxWidth.toPx() }
            val altoPx = with(densidad) { maxHeight.toPx() }
            val medioAnchoPx = with(densidad) { (ANCHO_FICHA / 2).toPx() }
            val medioAltoPx = with(densidad) { (ALTO_FICHA / 2).toPx() }

            // Límites para que la carta completa quede siempre dentro de la cancha.
            val minX = (medioAnchoPx / anchoPx).coerceAtMost(0.5f)
            val minY = (medioAltoPx / altoPx).coerceAtMost(0.5f)
            fun limitar(o: Offset) = Offset(o.x.coerceIn(minX, 1f - minX), o.y.coerceIn(minY, 1f - minY))

            /** Carta sobre la que cae [id] si se suelta en [p]: la más cercana que se solape bastante. */
            fun buscarObjetivo(id: String, p: Offset): String? =
                titulares
                    .asSequence()
                    .filter { it.id != id }
                    .mapNotNull { otro ->
                        val o = posiciones[otro.id] ?: return@mapNotNull null
                        val dx = abs(o.x - p.x) * anchoPx
                        val dy = abs(o.y - p.y) * altoPx
                        if (dx < medioAnchoPx * 1.1f && dy < medioAltoPx * 1.1f) otro.id to (dx * dx + dy * dy) else null
                    }
                    .minByOrNull { it.second }
                    ?.first

            // Hueco punteado donde estaba la carta que se arrastra.
            origenArrastre?.let { origen ->
                Canvas(
                    Modifier
                        .offset {
                            IntOffset(
                                (origen.x * anchoPx - medioAnchoPx).roundToInt(),
                                (origen.y * altoPx - medioAltoPx).roundToInt(),
                            )
                        }
                        .size(ANCHO_FICHA, ALTO_FICHA)
                ) {
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.10f),
                        cornerRadius = CornerRadius(12.dp.toPx()),
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.65f),
                        cornerRadius = CornerRadius(12.dp.toPx()),
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
                        ),
                    )
                }
            }

            titulares.forEach { jugador ->
                val actual = limitar(posiciones[jugador.id] ?: Offset(jugador.x, jugador.y))
                val arrastrando = arrastrandoId == jugador.id
                val esObjetivo = objetivoId == jugador.id

                // Las cartas que no se arrastran se deslizan a su lugar (formación, intercambio).
                val animada by animateOffsetAsState(
                    targetValue = actual,
                    animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
                    label = "posicionCarta",
                )
                val mostrada = if (arrastrando) actual else animada
                val escala by animateFloatAsState(
                    targetValue = when {
                        arrastrando -> 1.14f
                        esObjetivo -> 1.06f
                        else -> 1f
                    },
                    label = "escalaCarta",
                )

                FichaJugador(
                    jugador = jugador,
                    onClick = { onJugadorClick(jugador) },
                    resaltada = arrastrando,
                    objetivo = esObjetivo,
                    modifier = Modifier
                        .zIndex(if (arrastrando) 100f else mostrada.y)
                        .offset {
                            IntOffset(
                                x = (mostrada.x * anchoPx - medioAnchoPx).roundToInt(),
                                y = (mostrada.y * altoPx - medioAltoPx).roundToInt(),
                            )
                        }
                        .pointerInput(jugador.id, editable, anchoPx, altoPx, titulares) {
                            if (!editable) return@pointerInput
                            detectDragGestures(
                                onDragStart = {
                                    arrastrandoId = jugador.id
                                    origenArrastre = posiciones[jugador.id]
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragCancel = {
                                    origenArrastre?.let { posiciones[jugador.id] = it }
                                    arrastrandoId = null
                                    objetivoId = null
                                    origenArrastre = null
                                },
                                onDragEnd = {
                                    val origen = origenArrastre
                                    val destinoId = objetivoId
                                    val destino = titulares.firstOrNull { it.id == destinoId }
                                    val posDestino = destinoId?.let { posiciones[it] }

                                    if (destino != null && posDestino != null && origen != null) {
                                        // Intercambio: cada uno toma el lugar del otro.
                                        posiciones[jugador.id] = posDestino
                                        posiciones[destino.id] = origen
                                        onIntercambiarJugadores(
                                            jugador.copy(x = posDestino.x, y = posDestino.y),
                                            destino.copy(x = origen.x, y = origen.y),
                                        )
                                    } else {
                                        posiciones[jugador.id]?.let { final ->
                                            val p = limitar(final)
                                            onPosicionJugadorChange(jugador.id, p.x, p.y)
                                        }
                                    }
                                    arrastrandoId = null
                                    objetivoId = null
                                    origenArrastre = null
                                },
                            ) { change, arrastre ->
                                change.consume()
                                val anterior = posiciones[jugador.id] ?: Offset(jugador.x, jugador.y)
                                val nueva = limitar(
                                    Offset(
                                        anterior.x + arrastre.x / anchoPx,
                                        anterior.y + arrastre.y / altoPx,
                                    )
                                )
                                posiciones[jugador.id] = nueva
                                val nuevoObjetivo = buscarObjetivo(jugador.id, nueva)
                                if (nuevoObjetivo != objetivoId) {
                                    objetivoId = nuevoObjetivo
                                    if (nuevoObjetivo != null) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            }
                        }
                        // La escala va después del gesto: así el arrastre se mide en píxeles reales
                        // y la carta sigue al dedo aunque esté agrandada.
                        .graphicsLayer {
                            scaleX = escala
                            scaleY = escala
                            rotationZ = if (arrastrando) -3f else 0f
                        },
                )
            }
        }

        LeyendaLineas()

        if (suplentes.isNotEmpty()) {
            Banco(suplentes = suplentes, onJugadorClick = onJugadorClick)
        }
        if (fuera.isNotEmpty()) {
            Banco(suplentes = fuera, onJugadorClick = onJugadorClick, titulo = "SALIERON", atenuadas = true)
        }

        Text(
            text = ayuda ?: if (editable) {
                "Arrastra una carta para moverla. Suéltala sobre otra para intercambiarlas."
            } else {
                "Toca una carta para ver los datos del jugador."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

/** Qué significa el color de la posición en cada carta. */
@Composable
private fun LeyendaLineas() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        listOf(
            "Arco" to LineaCampo.ARCO,
            "Defensa" to LineaCampo.DEFENSA,
            "Medio" to LineaCampo.MEDIO,
            "Ataque" to LineaCampo.ATAQUE,
        ).forEach { (texto, linea) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(linea.color)
                )
                Text(
                    text = texto,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 5.dp),
                )
            }
        }
    }
}

// ============================================================
// CANCHA
// ============================================================

@Composable
private fun CespedYLineas(modifier: Modifier) {
    Canvas(modifier) {
        val franjas = 12
        val altoFranja = size.height / franjas
        repeat(franjas) { i ->
            drawRect(
                color = if (i % 2 == 0) CespedClaro else CespedOscuro,
                topLeft = Offset(0f, i * altoFranja),
                size = Size(size.width, altoFranja),
            )
        }
        // Brillo central y bordes más oscuros: da profundidad sin distraer.
        drawRect(
            Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.10f), Color.Transparent, Color.Black.copy(alpha = 0.28f)),
                center = center,
                radius = size.maxDimension * 0.75f,
            )
        )
        dibujarLineas()
    }
}

private fun DrawScope.dibujarLineas() {
    val trazo = Stroke(width = 1.6.dp.toPx())
    val margen = 10.dp.toPx()
    val ancho = size.width - margen * 2
    val alto = size.height - margen * 2
    val izq = margen
    val arriba = margen
    val abajo = margen + alto
    val cx = size.width / 2

    // Perímetro y línea media.
    drawRoundRect(LineaCal, Offset(izq, arriba), Size(ancho, alto), CornerRadius(4f), style = trazo)
    drawLine(LineaCal, Offset(izq, center.y), Offset(izq + ancho, center.y), trazo.width)

    // Círculo central.
    val radioCentral = ancho * 0.17f
    drawCircle(LineaCal, radioCentral, center, style = trazo)
    drawCircle(LineaCal, 3.dp.toPx(), center)

    // Áreas: grande, chica, punto penal y medialuna (arriba y abajo).
    val anchoArea = ancho * 0.60f
    val altoArea = alto * 0.15f
    val anchoChica = ancho * 0.30f
    val altoChica = alto * 0.055f
    val distPenal = alto * 0.105f
    val radioArco = ancho * 0.14f

    listOf(true, false).forEach { esArriba ->
        val borde = if (esArriba) arriba else abajo
        val dir = if (esArriba) 1f else -1f
        val yArea = if (esArriba) borde else borde - altoArea
        val yChica = if (esArriba) borde else borde - altoChica
        drawRect(LineaCal, Offset(cx - anchoArea / 2, yArea), Size(anchoArea, altoArea), style = trazo)
        drawRect(LineaCal, Offset(cx - anchoChica / 2, yChica), Size(anchoChica, altoChica), style = trazo)

        val penal = Offset(cx, borde + dir * distPenal)
        drawCircle(LineaCal, 2.5.dp.toPx(), penal)
        // Solo la parte de la medialuna que queda fuera del área grande.
        val salida = ((altoArea - distPenal) / radioArco).coerceIn(-1f, 1f)
        val angulo = Math.toDegrees(kotlin.math.asin(salida.toDouble())).toFloat()
        drawArc(
            color = LineaCal,
            startAngle = if (esArriba) angulo else 180f + angulo,
            sweepAngle = 180f - 2 * angulo,
            useCenter = false,
            topLeft = Offset(penal.x - radioArco, penal.y - radioArco),
            size = Size(radioArco * 2, radioArco * 2),
            style = trazo,
        )

        // Arco (portería) por fuera de la línea de fondo.
        val anchoPorteria = ancho * 0.16f
        val fondoPorteria = 5.dp.toPx()
        drawRect(
            color = Color.White.copy(alpha = 0.85f),
            topLeft = Offset(cx - anchoPorteria / 2, if (esArriba) borde - fondoPorteria else borde),
            size = Size(anchoPorteria, fondoPorteria),
            style = Stroke(2.dp.toPx()),
        )
    }

    // Banderines de córner.
    val radioCorner = 8.dp.toPx()
    listOf(
        Offset(izq, arriba) to 0f,
        Offset(izq + ancho, arriba) to 90f,
        Offset(izq + ancho, abajo) to 180f,
        Offset(izq, abajo) to 270f,
    ).forEach { (esquina, inicio) ->
        drawArc(
            color = LineaCal,
            startAngle = inicio,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(esquina.x - radioCorner, esquina.y - radioCorner),
            size = Size(radioCorner * 2, radioCorner * 2),
            style = trazo,
        )
    }
}

// ============================================================
// CARTAS
// ============================================================

private val OroClaro = Color(0xFFFFEDB0)
private val Oro = Color(0xFFF0BE4A)
private val OroOscuro = Color(0xFFB9781A)
private val PlataClara = Color(0xFFF1F4F8)
private val Plata = Color(0xFFC3CCD8)
private val PlataOscura = Color(0xFF8D99AA)
private val TintaFicha = Color(0xFF101F16)

/**
 * Carta del jugador: marco dorado (plateado en el banco) con la foto enmarcada, el dorsal en la
 * esquina, el nombre en una franja oscura y la posición abreviada con el color de su línea.
 */
@Composable
private fun FichaJugador(
    jugador: JugadorCanchaUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    resaltada: Boolean = false,
    objetivo: Boolean = false,
    ancho: Dp = ANCHO_FICHA,
    alto: Dp = ALTO_FICHA,
    suplente: Boolean = false,
    /** Jugador que ya salió: la carta se atenúa, pero sus marcas se ven completas. */
    atenuada: Boolean = false,
) {
    val forma = RoundedCornerShape(12.dp)
    val marco = if (suplente) {
        Brush.linearGradient(listOf(PlataClara, Plata, PlataOscura))
    } else {
        Brush.linearGradient(listOf(OroClaro, Oro, OroOscuro))
    }
    val colorLinea = colorPosicion(jugador.posicion)

    Box(modifier = modifier.width(ancho).height(alto)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = if (atenuada) 0.55f else 1f }
                .shadow(if (resaltada) 18.dp else 7.dp, forma)
                .clip(forma)
                .background(marco)
                .border(
                    width = if (resaltada || objetivo) 2.dp else 1.dp,
                    color = when {
                        objetivo -> Lima
                        resaltada -> Color.White
                        else -> Color.White.copy(alpha = 0.6f)
                    },
                    shape = forma,
                )
                .clickable(onClick = onClick)
                .padding(3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Foto enmarcada con el dorsal.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 9.dp, topEnd = 9.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                    .background(Color.White.copy(alpha = 0.35f)),
            ) {
                FotoFicha(jugador = jugador, modifier = Modifier.fillMaxSize())

                // Degradado inferior para separar la foto de la franja del nombre.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, TintaFicha.copy(alpha = 0.45f))))
                )

                Text(
                    text = jugador.dorsal?.toString() ?: "–",
                    color = Color.White,
                    fontSize = 11.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(2.dp)
                        .background(TintaFicha.copy(alpha = 0.9f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                )
            }

            // Nombre + posición.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp, bottomStart = 9.dp, bottomEnd = 9.dp))
                    .background(TintaFicha),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = nombreCorto(jugador.nombre),
                    color = Color.White,
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.4.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 3.dp, end = 3.dp, top = 2.dp),
                )
                Text(
                    text = abreviaturaPosicion(jugador.posicion),
                    color = colorLinea,
                    fontSize = 8.5.sp,
                    lineHeight = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.6.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
                // Barra del color de la línea, como el borde inferior de la carta.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(colorLinea)
                )
            }
        }

        // Partido en vivo: goles, tarjetas y cambios, pegados al borde derecho de la carta.
        jugador.marcas?.let { marcas ->
            MarcasCarta(
                marcas = marcas,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 9.dp, y = 4.dp),
            )
        }

        // Aviso de intercambio sobre la carta destino.
        if (objetivo) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(30.dp)
                    .shadow(6.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Lima),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.SwapVert,
                    contentDescription = "Intercambiar",
                    tint = TintaFicha,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** Marcas pequeñas al costado de la carta: lo justo para leerlas sin tapar la cancha. */
@Composable
private fun MarcasCarta(marcas: MarcasEnVivo, modifier: Modifier = Modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp), horizontalAlignment = Alignment.End, modifier = modifier) {
        if (marcas.goles > 0) {
            Insignia(fondo = Color.White, borde = TintaFicha.copy(alpha = 0.2f)) {
                Icon(Icons.Filled.SportsSoccer, contentDescription = "Goles", tint = TintaFicha, modifier = Modifier.size(11.dp))
                if (marcas.goles > 1) TextoInsignia("${marcas.goles}", TintaFicha)
            }
        }
        if (marcas.asistencias > 0) {
            Insignia(fondo = Color(0xFF2F7DE1)) {
                TextoInsignia(if (marcas.asistencias > 1) "A${marcas.asistencias}" else "A", Color.White)
            }
        }
        if (marcas.roja) {
            TarjetaInsignia(Color(0xFFE5484D))
        } else if (marcas.amarillas > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
                repeat(marcas.amarillas.coerceAtMost(2)) { TarjetaInsignia(Color(0xFFF5C518)) }
            }
        }
        marcas.entroMinuto?.let { minuto ->
            Insignia(fondo = Color(0xFF22A060)) { TextoInsignia("▲$minuto'", Color.White) }
        }
        marcas.salioMinuto?.let { minuto ->
            Insignia(fondo = Color(0xFFE5484D)) { TextoInsignia("▼$minuto'", Color.White) }
        }
    }
}

@Composable
private fun Insignia(
    fondo: Color,
    borde: Color = Color.White.copy(alpha = 0.8f),
    contenido: @Composable () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(16.dp)
            .shadow(2.dp, RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .background(fondo)
            .border(1.dp, borde, RoundedCornerShape(50))
            .padding(horizontal = 4.dp),
    ) { contenido() }
}

@Composable
private fun TextoInsignia(texto: String, color: Color) {
    Text(texto, color = color, fontSize = 9.sp, lineHeight = 10.sp, fontWeight = FontWeight.Black, maxLines = 1)
}

@Composable
private fun TarjetaInsignia(color: Color) {
    Box(
        modifier = Modifier
            .size(width = 10.dp, height = 14.dp)
            .shadow(2.dp, RoundedCornerShape(2.dp))
            .clip(RoundedCornerShape(2.dp))
            .background(color)
            .border(1.dp, Color.White, RoundedCornerShape(2.dp)),
    )
}

@Composable
private fun FotoFicha(jugador: JugadorCanchaUi, modifier: Modifier) {
    // Foto subida o, si no hay, la de demostración del plantel de prueba.
    val modelo = com.esan.sportpro.ui.common.FotosDemoJugadores.modelo(jugador.fotoUrl, jugador.nombre)
    Box(modifier = modifier, contentAlignment = Alignment.BottomCenter) {
        if (modelo != null) {
            AsyncImage(
                model = modelo,
                contentDescription = "Foto de ${jugador.nombre}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            // Sin foto: silueta con las iniciales encima, sobre el fondo de la carta.
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = TintaFicha.copy(alpha = 0.20f),
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = 6.dp),
            )
            Text(
                text = iniciales(jugador.nombre),
                color = TintaFicha.copy(alpha = 0.85f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

// ============================================================
// BANCO DE SUPLENTES
// ============================================================

@Composable
private fun Banco(
    suplentes: List<JugadorCanchaUi>,
    onJugadorClick: (JugadorCanchaUi) -> Unit,
    titulo: String = "BANCO",
    atenuadas: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            )
            Text(
                text = if (atenuadas) "${suplentes.size}" else "${suplentes.size} suplente${if (suplentes.size == 1) "" else "s"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
            // Espacio extra entre cartas para las marcas que van al costado.
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            suplentes.forEach { jugador ->
                FichaJugador(
                    jugador = jugador,
                    onClick = { onJugadorClick(jugador) },
                    atenuada = atenuadas,
                    suplente = true,
                    ancho = 56.dp,
                    alto = 76.dp,
                )
            }
        }
    }
}

/** Foto grande para el diálogo de detalle (o iniciales si no hay). */
@Composable
fun FotoJugadorGrande(jugador: JugadorCanchaUi, tamano: Dp = 88.dp) {
    Box(
        modifier = Modifier
            .size(tamano)
            .shadow(6.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(OroClaro, Oro, OroOscuro))),
    ) {
        FotoFicha(jugador = jugador, modifier = Modifier.fillMaxSize())
    }
}

private fun nombreCorto(nombre: String): String {
    val partes = nombre.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return (partes.firstOrNull() ?: "Jugador").uppercase()
}

private fun iniciales(nombre: String): String =
    nombre.trim().split(Regex("\\s+"))
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .take(2)
        .joinToString("")
        .ifBlank { "?" }

/** Distribución inicial 4-3-3 según la posición de cada titular. */
fun posiciones433(jugadores: List<JugadorCanchaUi>): List<JugadorCanchaUi> =
    distribuirEnFormacion("4-3-3", jugadores)
