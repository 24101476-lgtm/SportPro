package com.esan.sportpro.ui.entrenamientos

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esan.sportpro.R
import com.esan.sportpro.core.theme.EstilosSportPro
import com.esan.sportpro.core.theme.PaletaSportPro
import com.esan.sportpro.core.theme.SportProTema
import com.esan.sportpro.domain.entrenamientos.CategoriaTecnica
import com.esan.sportpro.domain.entrenamientos.Entrenamiento
import com.esan.sportpro.domain.entrenamientos.EstadoSesion
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/*
 * Piezas visuales de Entrenamientos (rediseño): bloque de fecha, tira de la semana, colores y
 * fotos por categoría, chips de información y anillo de asistencia.
 */

private val DIAS = listOf("LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB", "DOM")
private val MESES = listOf("ENE", "FEB", "MAR", "ABR", "MAY", "JUN", "JUL", "AGO", "SET", "OCT", "NOV", "DIC")
internal val MESES_LARGOS = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Setiembre", "Octubre", "Noviembre", "Diciembre",
)

/** Fecha guardada como "yyyy-MM-dd" (o "dd/MM/yyyy" en datos antiguos) a [LocalDate]. */
internal fun fechaDe(texto: String): LocalDate? = runCatching { LocalDate.parse(texto.take(10)) }.getOrNull()
    ?: runCatching { LocalDate.parse(texto.take(10), DateTimeFormatter.ofPattern("dd/MM/yyyy")) }.getOrNull()

internal fun diaCorto(fecha: LocalDate): String = DIAS[fecha.dayOfWeek.value - 1]

internal fun mesCorto(fecha: LocalDate): String = MESES[fecha.monthValue - 1]

/** "16:00" + 90 min → "17:30". `null` si la hora no tiene formato válido. */
internal fun horaMas(hora: String, minutos: Int): String? = runCatching {
    LocalTime.parse(hora.trim().padStart(5, '0')).plusMinutes(minutos.toLong()).toString().take(5)
}.getOrNull()

/** Color de cada categoría técnica (línea de tiempo, barra del plan y biblioteca). */
internal fun colorCategoria(categoria: CategoriaTecnica?): Color = when (categoria) {
    CategoriaTecnica.CALENTAMIENTO -> Color(0xFFE0A800)
    CategoriaTecnica.TECNICA_INDIVIDUAL -> PaletaSportPro.Musgo
    CategoriaTecnica.PASE_Y_CONTROL -> Color(0xFF00897B)
    CategoriaTecnica.DEFINICION -> Color(0xFFD84315)
    CategoriaTecnica.TACTICA_OFENSIVA -> Color(0xFF62802B)
    CategoriaTecnica.TACTICA_DEFENSIVA -> Color(0xFF455A64)
    CategoriaTecnica.FISICO -> Color(0xFF1565C0)
    CategoriaTecnica.ARQUEROS -> Color(0xFF6A1B9A)
    CategoriaTecnica.VUELTA_A_LA_CALMA -> Color(0xFF8D6E63)
    null -> Color(0xFF9A9D90)
}

/** Foto de respaldo por categoría, para ejercicios sin imagen subida. */
@DrawableRes
internal fun fotoCategoria(categoria: CategoriaTecnica?): Int = when (categoria) {
    CategoriaTecnica.CALENTAMIENTO -> R.drawable.foto_juveniles
    CategoriaTecnica.TECNICA_INDIVIDUAL -> R.drawable.foto_conos
    CategoriaTecnica.PASE_Y_CONTROL -> R.drawable.foto_balon
    CategoriaTecnica.DEFINICION -> R.drawable.foto_definicion
    CategoriaTecnica.TACTICA_OFENSIVA, CategoriaTecnica.TACTICA_DEFENSIVA -> R.drawable.foto_cancha
    CategoriaTecnica.FISICO -> R.drawable.foto_botines
    CategoriaTecnica.ARQUEROS -> R.drawable.foto_arqueros
    CategoriaTecnica.VUELTA_A_LA_CALMA -> R.drawable.foto_vestuario
    null -> R.drawable.foto_entrenador
}

/** Bloque con el día de la semana, el número grande y el mes (VIE / 27 / NOV). */
@Composable
internal fun BloqueFecha(
    fechaTexto: String,
    modifier: Modifier = Modifier,
    fondo: Color = PaletaSportPro.Musgo,
    texto: Color = Color.White,
    acento: Color = PaletaSportPro.Lima,
    ancho: Dp = 58.dp,
) {
    val fecha = fechaDe(fechaTexto)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(ancho)
            .clip(RoundedCornerShape(14.dp))
            .background(fondo)
            .padding(vertical = 6.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = fecha?.let { "${diaCorto(it)} ${it.dayOfMonth} ${mesCorto(it)}" } ?: fechaTexto
            },
    ) {
        Text(fecha?.let(::diaCorto) ?: "", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = acento)
        Text(
            fecha?.dayOfMonth?.toString() ?: "—",
            style = EstilosSportPro.NumeroMediano.copy(fontSize = 26.sp, lineHeight = 28.sp),
            color = texto,
        )
        Text(fecha?.let(::mesCorto) ?: "", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = acento)
    }
}

/**
 * Tira de lunes a domingo de la semana actual: hoy en negro y un punto en los días con sesión
 * (rojo si todas las de ese día se cancelaron).
 */
@Composable
internal fun TiraSemana(entrenamientos: List<Entrenamiento>, modifier: Modifier = Modifier, hoy: LocalDate = LocalDate.now()) {
    val lunes = hoy.with(DayOfWeek.MONDAY)
    val porDia = entrenamientos.groupBy { fechaDe(it.fecha) }
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (0L..6L).map { lunes.plusDays(it) }.forEach { dia ->
            val esHoy = dia == hoy
            val delDia = porDia[dia].orEmpty()
            val punto = when {
                delDia.isEmpty() -> Color.Transparent
                delDia.all { it.estadoSesion == EstadoSesion.CANCELADA } -> SportProTema.colores.error
                esHoy -> PaletaSportPro.Lima
                else -> MaterialTheme.colorScheme.primary
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (esHoy) PaletaSportPro.Musgo else MaterialTheme.colorScheme.surface)
                    .border(
                        1.dp,
                        if (esHoy) PaletaSportPro.Musgo else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                        RoundedCornerShape(14.dp),
                    )
                    .padding(vertical = 8.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "${diaCorto(dia)} ${dia.dayOfMonth}" +
                            (if (esHoy) ", hoy" else "") +
                            (if (delDia.isNotEmpty()) ", ${delDia.size} sesión" else "")
                    },
            ) {
                Text(
                    if (esHoy) "HOY" else diaCorto(dia),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (esHoy) PaletaSportPro.Lima else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${dia.dayOfMonth}",
                    style = EstilosSportPro.NumeroMediano.copy(fontSize = 19.sp, lineHeight = 21.sp),
                    color = if (esHoy) Color.White else MaterialTheme.colorScheme.onSurface,
                )
                Box(Modifier.padding(top = 3.dp).size(6.dp).clip(CircleShape).background(punto))
            }
        }
    }
}

/** Chip pequeño de información (duración, ejercicios, estado). */
@Composable
internal fun ChipInfo(texto: String, fondo: Color = MaterialTheme.colorScheme.surfaceVariant, color: Color = MaterialTheme.colorScheme.onSurface) {
    Surface(shape = RoundedCornerShape(50), color = fondo, contentColor = color) {
        Text(
            texto,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
        )
    }
}

/** Chip del estado de la sesión con su color: Hoy, Programada, Realizada (con %) o Cancelada. */
@Composable
internal fun ChipEstadoSesion(entrenamiento: Entrenamiento, hoy: LocalDate = LocalDate.now()) {
    val esHoy = fechaDe(entrenamiento.fecha) == hoy
    when (entrenamiento.estadoSesion) {
        EstadoSesion.CANCELADA -> ChipInfo(
            "Cancelada" + entrenamiento.motivoCancelacion.takeIf { it.isNotBlank() }?.let { ": $it" }.orEmpty(),
            fondo = SportProTema.colores.error.copy(alpha = 0.12f),
            color = SportProTema.colores.error,
        )
        EstadoSesion.REALIZADA -> ChipInfo(
            "Realizada" + entrenamiento.porcentajeAsistencia?.let { " · ${it.toInt()}%" }.orEmpty(),
            fondo = SportProTema.colores.exito.copy(alpha = 0.14f),
            color = SportProTema.colores.exito,
        )
        EstadoSesion.PROGRAMADA -> if (esHoy) {
            ChipInfo("Hoy", fondo = PaletaSportPro.Lima, color = PaletaSportPro.Carbon)
        } else {
            ChipInfo("Programada")
        }
    }
}

/** Anillo de porcentaje simple (asistencia de la sesión). */
@Composable
internal fun AnilloAsistencia(fraccion: Float?, modifier: Modifier = Modifier, tamano: Dp = 68.dp) {
    val pista = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val color = when {
        fraccion == null -> Color.Transparent
        fraccion >= 0.8f -> SportProTema.colores.exito
        fraccion >= 0.6f -> SportProTema.colores.advertencia
        else -> SportProTema.colores.error
    }
    Box(contentAlignment = Alignment.Center, modifier = modifier.size(tamano)) {
        Canvas(Modifier.fillMaxSize()) {
            val grosor = size.minDimension * 0.12f
            val lado = size.minDimension - grosor
            val origen = Offset(grosor / 2, grosor / 2)
            drawArc(pista, 0f, 360f, false, origen, Size(lado, lado), style = Stroke(grosor))
            if (fraccion != null) {
                drawArc(color, -90f, 360f * fraccion.coerceIn(0f, 1f), false, origen, Size(lado, lado), style = Stroke(grosor, cap = StrokeCap.Round))
            }
        }
        Text(
            fraccion?.let { "${(it * 100).toInt()}%" } ?: "—",
            style = EstilosSportPro.NumeroMediano.copy(fontSize = 17.sp, lineHeight = 17.sp),
            textAlign = TextAlign.Center,
        )
    }
}

/** Separación vertical estándar entre bloques de la sección. */
@Composable
internal fun Separacion(alto: Dp = 8.dp) = Spacer(Modifier.height(alto))
