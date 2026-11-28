package com.esan.sportpro.ui.estadisticas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esan.sportpro.core.theme.EstilosSportPro
import com.esan.sportpro.core.theme.PaletaSportPro
import com.esan.sportpro.core.theme.SportProTema
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * Piezas gráficas de Estadísticas (rediseño inspirado en las infografías de partido): etiquetas
 * de sección en pastilla, anillos de porcentaje, tarjetas de árbitro con el número dentro,
 * radar del jugador, cancha con su zona y barras por tramos de minutos.
 */

/** Colores de tarjeta de árbitro. */
internal val AmarillaTarjeta = Color(0xFFF2C230)
internal val RojaTarjeta = Color(0xFFD32F2F)

/** Etiqueta de sección en pastilla lima (equivale a la cinta roja de las infografías). */
@Composable
internal fun EtiquetaSeccion(texto: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(50),
        color = PaletaSportPro.Lima,
        contentColor = PaletaSportPro.Carbon,
        modifier = modifier,
    ) {
        Text(
            texto,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
        )
    }
}

/** Tarjeta blanca con borde suave: el contenedor de cada sección. */
@Composable
internal fun PanelEstadistica(modifier: Modifier = Modifier, contenido: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = modifier.fillMaxWidth(),
    ) { contenido() }
}

/** Anillo de porcentaje con el valor en Anton al centro (los "donuts" de las infografías). */
@Composable
internal fun AnilloPorcentaje(
    fraccion: Float,
    valor: String,
    etiqueta: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    tamano: Dp = 84.dp,
) {
    val pista = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = "$etiqueta: $valor" },
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(tamano)) {
            Canvas(Modifier.fillMaxSize()) {
                val grosor = size.minDimension * 0.11f
                val lado = size.minDimension - grosor
                val origen = Offset(grosor / 2, grosor / 2)
                drawArc(pista, 0f, 360f, false, origen, Size(lado, lado), style = Stroke(grosor))
                drawArc(
                    color, -90f, 360f * fraccion.coerceIn(0f, 1f), false, origen, Size(lado, lado),
                    style = Stroke(grosor, cap = StrokeCap.Round),
                )
            }
            Text(valor, style = EstilosSportPro.NumeroMediano.copy(fontSize = 22.sp, lineHeight = 22.sp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            etiqueta,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Tarjeta de árbitro (amarilla o roja) inclinada, con la cantidad dentro. */
@Composable
internal fun TarjetaArbitro(cantidad: Int, roja: Boolean, modifier: Modifier = Modifier, alto: Dp = 46.dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .rotate(-6f)
            .size(width = alto * 0.72f, height = alto)
            .clip(RoundedCornerShape(alto * 0.13f))
            .background(if (roja) RojaTarjeta else AmarillaTarjeta)
            .semantics { contentDescription = "$cantidad tarjetas " + if (roja) "rojas" else "amarillas" },
    ) {
        Text(
            "$cantidad",
            style = EstilosSportPro.NumeroMediano.copy(fontSize = (alto.value * 0.42f).sp, lineHeight = (alto.value * 0.42f).sp),
            color = if (roja) Color.White else PaletaSportPro.Carbon,
        )
    }
}

/** Número en un círculo con su etiqueta debajo (fila de cifras del jugador). */
@Composable
internal fun InsigniaCifra(valor: String, etiqueta: String, modifier: Modifier = Modifier, destacada: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (destacada) PaletaSportPro.Lima else MaterialTheme.colorScheme.surface)
                .border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
        ) {
            Text(
                valor,
                style = EstilosSportPro.NumeroMediano.copy(fontSize = if (valor.length > 3) 15.sp else 18.sp, lineHeight = 18.sp),
                color = if (destacada) PaletaSportPro.Carbon else MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Celda de la tarjeta de árbitro con su etiqueta, alineada con las insignias. */
@Composable
internal fun InsigniaTarjeta(cantidad: Int, roja: Boolean, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { TarjetaArbitro(cantidad, roja, alto = 44.dp) }
        Spacer(Modifier.height(4.dp))
        Text(
            if (roja) "Rojas" else "Amarillas",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Radar de 5 ejes. Cada valor va de 0 a 1 (el jugador comparado con el mejor del equipo en ese
 * aspecto), así que el gráfico muestra su perfil, no números absolutos.
 */
@Composable
internal fun RadarJugador(valores: List<Float>, etiquetas: List<String>, modifier: Modifier = Modifier) {
    val medidor = rememberTextMeasurer()
    val rejilla = MaterialTheme.colorScheme.outlineVariant
    val linea = MaterialTheme.colorScheme.primary
    val relleno = PaletaSportPro.Lima.copy(alpha = 0.55f)
    val estiloEtiqueta = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Canvas(
        modifier
            .fillMaxWidth()
            .aspectRatio(1.15f)
            .semantics {
                contentDescription = "Perfil: " + etiquetas.zip(valores)
                    .joinToString { (e, v) -> "$e ${(v * 100).toInt()}%" }
            },
    ) {
        val n = valores.size
        val centro = Offset(size.width / 2, size.height / 2 + 6f)
        val radio = size.minDimension * 0.34f
        fun punto(i: Int, r: Float): Offset {
            val angulo = -PI / 2 + 2 * PI * i / n
            return Offset(centro.x + (r * cos(angulo)).toFloat(), centro.y + (r * sin(angulo)).toFloat())
        }
        listOf(0.25f, 0.5f, 0.75f, 1f).forEach { k ->
            val p = Path()
            for (i in 0 until n) {
                val q = punto(i, radio * k)
                if (i == 0) p.moveTo(q.x, q.y) else p.lineTo(q.x, q.y)
            }
            p.close()
            drawPath(p, rejilla, style = Stroke(1.dp.toPx()))
        }
        for (i in 0 until n) drawLine(rejilla, centro, punto(i, radio), 1.dp.toPx())
        val forma = Path()
        valores.forEachIndexed { i, v ->
            val q = punto(i, radio * v.coerceIn(0.04f, 1f))
            if (i == 0) forma.moveTo(q.x, q.y) else forma.lineTo(q.x, q.y)
        }
        forma.close()
        drawPath(forma, relleno)
        drawPath(forma, linea, style = Stroke(2.dp.toPx()))
        valores.forEachIndexed { i, v -> drawCircle(linea, 4.dp.toPx(), punto(i, radio * v.coerceIn(0.04f, 1f))) }
        etiquetas.forEachIndexed { i, texto ->
            val medida = medidor.measure(texto, estiloEtiqueta)
            val q = punto(i, radio + 18.dp.toPx())
            drawText(
                medida,
                topLeft = Offset(
                    (q.x - medida.size.width / 2).coerceIn(0f, size.width - medida.size.width),
                    q.y - medida.size.height / 2,
                ),
            )
        }
    }
}

/**
 * Cancha vertical dibujada (nuestro arco abajo). [zona] marca dónde juega el jugador, con
 * coordenadas de 0 a 1.
 */
@Composable
internal fun CanchaDibujada(modifier: Modifier = Modifier, zona: Offset? = null) {
    val cesped = PaletaSportPro.Musgo
    val lineas = Color.White.copy(alpha = 0.7f)
    val marca = PaletaSportPro.Lima
    Canvas(modifier.clip(RoundedCornerShape(14.dp))) {
        drawRect(cesped)
        val franjas = 8
        for (i in 0 until franjas step 2) {
            drawRect(Color.White.copy(alpha = 0.04f), Offset(0f, size.height * i / franjas), Size(size.width, size.height / franjas))
        }
        val m = 8.dp.toPx()
        val trazo = Stroke(1.5.dp.toPx())
        drawRoundRect(lineas, Offset(m, m), Size(size.width - 2 * m, size.height - 2 * m), CornerRadius(3f), style = trazo)
        drawLine(lineas, Offset(m, size.height / 2), Offset(size.width - m, size.height / 2), 1.5.dp.toPx())
        drawCircle(lineas, size.width * 0.13f, Offset(size.width / 2, size.height / 2), style = trazo)
        val areaAncho = size.width * 0.5f
        val areaAlto = size.height * 0.16f
        drawRect(lineas, Offset((size.width - areaAncho) / 2, m), Size(areaAncho, areaAlto), style = trazo)
        drawRect(lineas, Offset((size.width - areaAncho) / 2, size.height - m - areaAlto), Size(areaAncho, areaAlto), style = trazo)
        zona?.let { z ->
            val c = Offset(z.x * size.width, z.y * size.height)
            drawOval(marca.copy(alpha = 0.5f), Offset(c.x - size.width * 0.2f, c.y - size.height * 0.12f), Size(size.width * 0.4f, size.height * 0.24f))
            drawCircle(marca, 6.dp.toPx(), c)
            drawCircle(PaletaSportPro.Carbon, 6.dp.toPx(), c, style = Stroke(2.dp.toPx()))
        }
    }
}

/** Zona habitual en la cancha según la posición de la ficha (nuestro arco abajo). */
internal fun zonaDePosicion(posicion: String): Offset? {
    val p = posicion.lowercase()
    return when {
        p.isBlank() -> null
        "arquero" in p || "portero" in p -> Offset(0.5f, 0.9f)
        "lateral" in p && "izq" in p -> Offset(0.18f, 0.7f)
        "lateral" in p -> Offset(0.82f, 0.7f)
        "defensa" in p || "central" in p -> Offset(0.5f, 0.74f)
        "volante defensivo" in p || "contención" in p -> Offset(0.5f, 0.58f)
        "volante ofensivo" in p || "enganche" in p -> Offset(0.5f, 0.36f)
        "volante" in p || "medio" in p -> Offset(0.5f, 0.47f)
        "extremo" in p -> Offset(0.8f, 0.27f)
        "delantero" in p -> Offset(0.5f, 0.17f)
        else -> null
    }
}

/** Barras verticales por tramo de minutos; la más alta en lima. */
@Composable
internal fun BarrasPorTramo(valores: List<Int>, etiquetas: List<String>, modifier: Modifier = Modifier) {
    val maximo = (valores.maxOrNull() ?: 0).coerceAtLeast(1)
    Row(
        modifier = modifier.fillMaxWidth().height(140.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        valores.forEachIndexed { i, v ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f).semantics(mergeDescendants = true) {
                    contentDescription = "${etiquetas[i]} minutos: $v goles"
                },
            ) {
                Text("$v", style = EstilosSportPro.NumeroMediano.copy(fontSize = 15.sp, lineHeight = 15.sp))
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .width(26.dp)
                        .height(if (v == 0) 4.dp else (14 + 76 * v / maximo).dp)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                        .background(if (v == maximo && v > 0) PaletaSportPro.Lima else MaterialTheme.colorScheme.primary),
                )
                Spacer(Modifier.height(6.dp))
                Text(etiquetas[i], style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Puntito de resultado: G (verde), E (gris) o P (rojo). */
@Composable
internal fun PuntoResultado(resultado: Char, tamano: Dp = 24.dp) {
    val fondo = when (resultado) {
        'G' -> SportProTema.colores.exito
        'P' -> SportProTema.colores.error
        else -> Color(0xFFC9CCBF)
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(tamano).clip(CircleShape).background(fondo).semantics {
            contentDescription = when (resultado) {
                'G' -> "Ganado"
                'P' -> "Perdido"
                else -> "Empate"
            }
        },
    ) {
        Text(
            resultado.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = if (resultado == 'E') PaletaSportPro.Carbon else Color.White,
        )
    }
}
