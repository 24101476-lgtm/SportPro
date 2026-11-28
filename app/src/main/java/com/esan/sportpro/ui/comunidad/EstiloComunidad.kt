package com.esan.sportpro.ui.comunidad

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esan.sportpro.core.theme.FuenteNumeros
import com.esan.sportpro.core.theme.PaletaSportPro
import com.esan.sportpro.core.theme.SportProTema
import com.esan.sportpro.domain.ia.ReglasRevision
import com.esan.sportpro.ui.common.FondoFotoSportPro
import com.esan.sportpro.ui.common.FotoSportPro
import kotlin.math.abs

// ---------------------------------------------------------------------------------------------
// Avatar del autor: inicial sobre un color propio de cada persona
// ---------------------------------------------------------------------------------------------

private val ColoresAvatar = listOf(
    PaletaSportPro.Musgo,
    PaletaSportPro.Verde,
    PaletaSportPro.Informacion,
    Color(0xFF8D5B2E),
    PaletaSportPro.Morado,
    Color(0xFF00796B),
    PaletaSportPro.Grafito,
)

/** Mismo nombre, mismo color: en el muro se reconoce a cada persona de un vistazo. */
@Composable
fun AvatarAutor(nombre: String, modifier: Modifier = Modifier, tamano: Dp = 44.dp) {
    val limpio = nombre.trim()
    val iniciales = limpio.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }.uppercase()
    val color = ColoresAvatar[abs(limpio.lowercase().hashCode()) % ColoresAvatar.size]
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(tamano).clip(CircleShape).background(color),
    ) {
        Text(
            iniciales.ifBlank { "?" },
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = if (tamano >= 40.dp) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelMedium,
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Selector de secciones (Muro / Avisos / Moderación)
// ---------------------------------------------------------------------------------------------

/** Píldoras en una sola línea: el texto nunca se parte ("Moderaci-ón"). */
@Composable
fun SelectorSeccionComunidad(
    titulos: List<String>,
    seleccion: Int,
    onSeleccion: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = modifier) {
        Row(modifier = Modifier.padding(4.dp)) {
            titulos.forEachIndexed { indice, titulo ->
                val activa = indice == seleccion
                Surface(
                    onClick = { onSeleccion(indice) },
                    shape = CircleShape,
                    color = if (activa) PaletaSportPro.Carbon else Color.Transparent,
                    contentColor = if (activa) PaletaSportPro.Lima else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        titulo,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (activa) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Publicación automática "Resumen del partido" → marcador y cronología
// ---------------------------------------------------------------------------------------------

internal enum class TipoEventoMuro { GOL, AMARILLA, ROJA, CAMBIO, SILBATO, OTRO }

internal data class EventoMuro(val texto: String, val minuto: Int?, val tipo: TipoEventoMuro)

internal data class ResumenPartidoMuro(
    val equipo: String,
    val rival: String,
    val golesEquipo: Int?,
    val golesRival: Int?,
    val eventos: List<EventoMuro>,
    val generadoConIa: Boolean,
)

private const val PREFIJO_RESUMEN = "Resumen del partido:"
private val REGEX_MARCADOR = Regex("^(.+) (\\d+) - (\\d+) (.+)$")
private val REGEX_MINUTO = Regex("\\s*en el minuto (\\d+)")

/**
 * Lee el texto que arma `ReglasRevision.textoParaComunidad` ("Resumen del partido: A 2 - 0 B", los
 * hechos con viñetas y la leyenda de IA). Si el texto no tiene ese formato devuelve `null` y la
 * publicación se muestra como texto normal.
 */
internal fun resumenPartidoDesde(texto: String): ResumenPartidoMuro? {
    val lineas = texto.lines().map { it.trim() }
    val cabecera = lineas.firstOrNull() ?: return null
    if (!cabecera.startsWith(PREFIJO_RESUMEN)) return null
    val marcador = cabecera.removePrefix(PREFIJO_RESUMEN).trim()

    val coincidencia = REGEX_MARCADOR.find(marcador)
    val (equipo, golesEquipo, golesRival, rival) = if (coincidencia != null) {
        val (a, ga, gb, b) = coincidencia.destructured
        listOf(a.trim(), ga, gb, b.trim())
    } else {
        val partes = marcador.split(" vs ", limit = 2)
        listOf(partes.first().trim(), "", "", partes.getOrNull(1)?.trim().orEmpty())
    }

    val leyenda = leyendaIa()
    val eventos = lineas.drop(1)
        .filter { it.isNotEmpty() && it != leyenda && it != "…" }
        .map { linea ->
            val limpia = linea.removePrefix("•").removePrefix("-").trim()
            val minuto = REGEX_MINUTO.find(limpia)?.groupValues?.get(1)?.toIntOrNull()
            EventoMuro(
                texto = limpia.replace(REGEX_MINUTO, "").trim().removeSuffix("…").trim(),
                minuto = minuto,
                tipo = tipoDeEvento(limpia),
            )
        }

    return ResumenPartidoMuro(
        equipo = equipo,
        rival = rival,
        golesEquipo = golesEquipo.toIntOrNull(),
        golesRival = golesRival.toIntOrNull(),
        eventos = eventos,
        generadoConIa = lineas.any { it == leyenda },
    )
}

private fun leyendaIa(): String = ReglasRevision.LEYENDA_IA

private fun tipoDeEvento(texto: String): TipoEventoMuro {
    val t = texto.lowercase()
    return when {
        "amarilla" in t -> TipoEventoMuro.AMARILLA
        "roja" in t -> TipoEventoMuro.ROJA
        "fallad" in t -> TipoEventoMuro.OTRO
        "gol" in t || "penal convertido" in t -> TipoEventoMuro.GOL
        "cambio" in t || "sustituc" in t || "ingresa" in t -> TipoEventoMuro.CAMBIO
        "inicio" in t || "fin de" in t || "tiempo" in t -> TipoEventoMuro.SILBATO
        else -> TipoEventoMuro.OTRO
    }
}

/**
 * Marcador sobre la foto del estadio y, debajo, la cronología con el minuto de cada hecho. En el
 * muro ([completo] = false) se ven los primeros hechos importantes; en el detalle, todos.
 */
@Composable
internal fun TarjetaResumenPartido(
    resumen: ResumenPartidoMuro,
    completo: Boolean,
    onVerTodo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        FondoFotoSportPro(
            foto = FotoSportPro.ESTADIO,
            forma = RoundedCornerShape(22.dp),
            velo = Brush.verticalGradient(
                0f to PaletaSportPro.Carbon.copy(alpha = 0.72f),
                1f to PaletaSportPro.Carbon.copy(alpha = 0.94f),
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
            ) {
                Text("Resumen del partido", style = MaterialTheme.typography.labelLarge, color = PaletaSportPro.Lima)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                ) {
                    NombreEquipo(resumen.equipo, TextAlign.End, Modifier.weight(1f))
                    Text(
                        if (resumen.golesEquipo != null && resumen.golesRival != null) {
                            "${resumen.golesEquipo} – ${resumen.golesRival}"
                        } else {
                            "vs"
                        },
                        style = TextStyle(fontFamily = FuenteNumeros, fontSize = 44.sp, lineHeight = 46.sp),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp),
                    )
                    NombreEquipo(resumen.rival, TextAlign.Start, Modifier.weight(1f))
                }
                ResultadoPildora(resumen)
            }
        }

        val importantes = resumen.eventos.filter { it.tipo != TipoEventoMuro.SILBATO }
        val visibles = if (completo) resumen.eventos else importantes.take(MAX_EVENTOS_MURO)
        if (visibles.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 14.dp),
            ) {
                visibles.forEach { FilaEvento(it) }
            }
        }
        val ocultos = resumen.eventos.size - visibles.size
        if (!completo && ocultos > 0) {
            Text(
                "Ver la cronología completa (+$ocultos)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = SportProTema.colores.textoAcento,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onVerTodo)
                    .padding(vertical = 6.dp, horizontal = 2.dp),
            )
        }
        if (resumen.generadoConIa) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    leyendaIa(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

private const val MAX_EVENTOS_MURO = 4

@Composable
private fun NombreEquipo(nombre: String, alineacion: TextAlign, modifier: Modifier) {
    Text(
        nombre,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        textAlign = alineacion,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
private fun ResultadoPildora(resumen: ResumenPartidoMuro) {
    val a = resumen.golesEquipo ?: return
    val b = resumen.golesRival ?: return
    val (texto, fondo, tinta) = when {
        a > b -> Triple("Victoria", PaletaSportPro.Lima, PaletaSportPro.Carbon)
        a < b -> Triple("Derrota", PaletaSportPro.ErrorClaro, PaletaSportPro.Carbon)
        else -> Triple("Empate", PaletaSportPro.Tiza, PaletaSportPro.Carbon)
    }
    Surface(shape = CircleShape, color = fondo, contentColor = tinta, modifier = Modifier.padding(top = 10.dp)) {
        Text(
            texto,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun FilaEvento(evento: EventoMuro) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            evento.minuto?.let { "$it'" } ?: "",
            style = TextStyle(fontFamily = FuenteNumeros, fontSize = 16.sp),
            color = SportProTema.colores.textoAcento,
            textAlign = TextAlign.End,
            modifier = Modifier.width(34.dp),
        )
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp).size(26.dp)) {
            MarcaEvento(evento.tipo)
        }
        Text(
            evento.texto,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (evento.tipo == TipoEventoMuro.GOL) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Ícono de cada hecho: balón, tarjeta dibujada, cambio o silbato. */
@Composable
private fun MarcaEvento(tipo: TipoEventoMuro) {
    when (tipo) {
        TipoEventoMuro.GOL -> Text("⚽", style = MaterialTheme.typography.titleMedium)
        TipoEventoMuro.AMARILLA -> Tarjeta(PaletaSportPro.Amarillo)
        TipoEventoMuro.ROJA -> Tarjeta(PaletaSportPro.Error)
        TipoEventoMuro.CAMBIO -> Icon(Icons.Default.SwapHoriz, contentDescription = "Cambio", tint = SportProTema.colores.informacion)
        TipoEventoMuro.SILBATO -> Icon(
            Icons.Default.Sports,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        TipoEventoMuro.OTRO -> Box(
            Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant),
        )
    }
}

@Composable
private fun Tarjeta(color: Color) {
    Box(Modifier.size(width = 13.dp, height = 18.dp).clip(RoundedCornerShape(3.dp)).background(color))
}
