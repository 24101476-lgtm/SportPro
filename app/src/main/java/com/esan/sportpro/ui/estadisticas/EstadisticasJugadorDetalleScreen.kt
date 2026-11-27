package com.esan.sportpro.ui.estadisticas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.esan.sportpro.R
import com.esan.sportpro.core.theme.EstilosSportPro
import com.esan.sportpro.core.theme.PaletaSportPro
import com.esan.sportpro.core.theme.SportProTema
import com.esan.sportpro.domain.estadisticas.EstadisticaJugador
import com.esan.sportpro.domain.estadisticas.PartidoDeJugador
import com.esan.sportpro.ui.common.EscudoEquipo
import com.esan.sportpro.ui.common.FotosDemoJugadores
import java.time.LocalDate
import java.time.Period

/**
 * US-026 — Detalle del jugador. Rediseño estilo ficha de club: cabecera con la foto grande, el
 * dorsal gigante y el nombre en dos líneas; cifras en insignias; radar comparado con el mejor
 * del equipo; zona en la cancha; últimos partidos y carrusel del plantel.
 */
@Composable
fun EstadisticasJugadorDetalleScreen(
    jugador: EstadisticaJugador,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    esDestacado: Boolean = false,
    /** Equipo del jugador (nombre, categoría y escudo de la cabecera). */
    equipo: com.esan.sportpro.domain.academia.Equipo? = null,
    /** Todo el plantel del periodo: base del radar y del carrusel. */
    plantel: List<EstadisticaJugador> = emptyList(),
    partidos: List<PartidoDeJugador> = emptyList(),
    onSeleccionarJugador: (EstadisticaJugador) -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CabeceraJugador(jugador, equipo, onVolver)

        // Cifras: partidos, minutos, goles, asistencias y tarjetas (con el número dentro).
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            InsigniaCifra("${jugador.partidosJugados}", "Partidos")
            InsigniaCifra("${jugador.minutosJugados}", "Minutos")
            InsigniaCifra("${jugador.goles}", "Goles", destacada = true)
            InsigniaCifra("${jugador.asistencias}", "Asist.")
            InsigniaTarjeta(jugador.tarjetasAmarillas, roja = false)
            InsigniaTarjeta(jugador.tarjetasRojas, roja = true)
        }

        if (esDestacado) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(50))
                    .background(PaletaSportPro.Lima.copy(alpha = 0.35f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                    "Jugador destacado del periodo",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }

        val margen = Modifier.padding(horizontal = 16.dp)
        EtiquetaSeccion("Perfil", modifier = margen.padding(top = 6.dp))
        PanelEstadistica(margen) {
            Column(Modifier.padding(12.dp)) {
                RadarJugador(valoresRadar(jugador, plantel), listOf("Goles", "Asistencias", "Minutos", "Entrenos", "Disciplina"))
                Text(
                    "Cada eje se compara con el mejor del equipo (100%). Faltas cometidas: ${jugador.faltas}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        EtiquetaSeccion("Posición y entrenos", modifier = margen.padding(top = 6.dp))
        Row(margen.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PanelEstadistica(Modifier.weight(1f)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Zona habitual", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    CanchaDibujada(Modifier.fillMaxWidth().aspectRatio(0.72f), zonaDePosicion(jugador.posicion))
                    Text(
                        jugador.posicion.ifBlank { "Sin posición registrada" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
            PanelEstadistica(Modifier.weight(1f)) {
                Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Asistencia", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    val pct = jugador.porcentajeAsistenciaEntrenamientos
                    AnilloPorcentaje(
                        fraccion = (pct / 100).toFloat(),
                        valor = "${pct.toInt()}%",
                        etiqueta = "a entrenamientos",
                        color = when {
                            pct >= 80 -> SportProTema.colores.exito
                            pct >= 60 -> SportProTema.colores.advertencia
                            else -> SportProTema.colores.error
                        },
                        tamano = 96.dp,
                    )
                    Text(
                        when {
                            pct >= 80 -> "Constante en los entrenamientos"
                            pct >= 60 -> "Asistencia irregular"
                            pct > 0 -> "Falta seguido a entrenar"
                            else -> "Sin asistencia registrada"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
        }

        if (partidos.isNotEmpty()) {
            EtiquetaSeccion("Últimos partidos", modifier = margen.padding(top = 6.dp))
            PanelEstadistica(margen) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                    partidos.take(5).forEachIndexed { i, partido ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        FilaPartido(partido)
                    }
                }
            }
        }

        val otros = plantel.filterNot { it.sinAsignar }
        if (otros.size > 1) {
            EtiquetaSeccion("Plantel", modifier = margen.padding(top = 6.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(otros, key = { it.jugadorId }) { otro ->
                    val elegido = otro.jugadorId == jugador.jugadorId
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(60.dp).clickable(enabled = !elegido) { onSeleccionarJugador(otro) },
                    ) {
                        Box(
                            Modifier
                                .size(58.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .then(
                                    if (elegido) Modifier.border(3.dp, PaletaSportPro.Lima, RoundedCornerShape(16.dp)) else Modifier,
                                ),
                        ) {
                            FotoRectangular(otro, Modifier.fillMaxSize())
                        }
                        Text(
                            otro.jugadorNombre.split(" ").firstOrNull().orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (elegido) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

/** Cabecera oscura: foto grande a la izquierda, dorsal gigante y nombre en dos líneas a la derecha. */
@Composable
private fun CabeceraJugador(
    jugador: EstadisticaJugador,
    equipo: com.esan.sportpro.domain.academia.Equipo?,
    onVolver: () -> Unit,
) {
    val sinColor = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    val partes = jugador.jugadorNombre.trim().split(Regex("\\s+"))
    val nombre = partes.firstOrNull().orEmpty()
    // Apellido paterno: en "Nombre Segundo Apellido1 Apellido2" es el penúltimo; con 2 palabras, el último.
    val apellido = when {
        partes.size >= 4 -> partes[partes.size - 2]
        partes.size >= 2 -> partes[1]
        else -> ""
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .background(PaletaSportPro.Carbon),
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(R.drawable.foto_estadio),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            colorFilter = sinColor,
            alpha = 0.35f,
            modifier = Modifier.fillMaxSize(),
        )
        // Dorsal gigante, detrás del nombre.
        jugador.dorsal?.let { dorsal ->
            Text(
                "$dorsal",
                style = EstilosSportPro.NumeroGrande.copy(fontSize = 150.sp, lineHeight = 150.sp),
                color = PaletaSportPro.Lima,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 52.dp, end = 18.dp),
            )
        }
        // Foto del jugador a la izquierda, fundida con el fondo.
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxHeight(0.86f)
                .fillMaxWidth(0.58f),
        ) {
            FotoRectangular(jugador, Modifier.fillMaxSize(), escalaDeGrises = true, conIniciales = true)
            Box(
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(0.6f to Color.Transparent, 1f to PaletaSportPro.Carbon.copy(alpha = 0.9f)),
                ),
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0.55f to Color.Transparent, 1f to PaletaSportPro.Carbon),
            ),
        )
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onVolver) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
            }
            Spacer(Modifier.weight(1f))
            equipo?.let { EscudoEquipo(nombre = it.nombre, tamano = 38.dp, url = it.escudoUrl.ifBlank { null }) }
            Spacer(Modifier.width(10.dp))
        }
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 20.dp),
        ) {
            if (jugador.posicion.isNotBlank()) {
                Text(jugador.posicion, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
            }
            Text(nombre.uppercase(), style = MaterialTheme.typography.headlineMedium, color = Color.White, maxLines = 1)
            if (apellido.isNotBlank()) {
                Text(apellido.uppercase(), style = MaterialTheme.typography.headlineMedium, color = PaletaSportPro.Lima, maxLines = 1)
            }
            val chips = listOfNotNull(
                edad(jugador.fechaNacimiento)?.let { "$it años" },
                jugador.pieHabil.ifBlank { null }?.let { pie -> if (pie.startsWith("pie", ignoreCase = true)) pie else "Pie ${pie.lowercase()}" },
                equipo?.categoria?.ifBlank { null },
            )
            if (chips.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                    chips.forEach { texto ->
                        Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.16f), contentColor = Color.White) {
                            Text(texto, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

/** Foto rectangular: la subida o la de demostración; si no hay, las iniciales sobre musgo. */
@Composable
private fun FotoRectangular(
    jugador: EstadisticaJugador,
    modifier: Modifier,
    escalaDeGrises: Boolean = false,
    conIniciales: Boolean = true,
) {
    val modelo = FotosDemoJugadores.modelo(jugador.fotoUrl, jugador.jugadorNombre)
    Box(modifier.background(PaletaSportPro.Musgo.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
        if (modelo != null) {
            AsyncImage(
                model = modelo,
                contentDescription = "Foto de ${jugador.jugadorNombre}",
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                colorFilter = if (escalaDeGrises) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (conIniciales) {
            val iniciales = jugador.jugadorNombre.split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }.take(2).joinToString("")
            Text(iniciales.ifBlank { "?" }, style = EstilosSportPro.NumeroMediano, color = PaletaSportPro.Lima)
        }
    }
}

@Composable
private fun FilaPartido(partido: PartidoDeJugador) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        PuntoResultado(partido.resultado, tamano = 22.dp)
        Text(
            "vs ${partido.rivalNombre}",
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 10.dp).weight(1f),
        )
        Text(
            buildString {
                if (partido.goles > 0) append("⚽ ${partido.goles} · ")
                append("${partido.minutos}'")
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "${partido.golesEquipo}-${partido.golesRival}",
            style = EstilosSportPro.NumeroMediano.copy(fontSize = 17.sp, lineHeight = 17.sp),
            modifier = Modifier.padding(start = 14.dp),
        )
    }
}

/**
 * Radar: goles, asistencias, minutos y asistencia a entrenos comparados con el máximo del plantel;
 * disciplina = 1 menos (amarillas + 3 × rojas) sobre el peor registro del plantel.
 */
private fun valoresRadar(jugador: EstadisticaJugador, plantel: List<EstadisticaJugador>): List<Float> {
    val base = plantel.filterNot { it.sinAsignar }.ifEmpty { listOf(jugador) }
    fun rel(valor: Double, maximo: Double): Float = if (maximo <= 0.0) 0f else (valor / maximo).toFloat()
    fun castigo(j: EstadisticaJugador) = (j.tarjetasAmarillas + 3 * j.tarjetasRojas).toDouble()
    val peor = base.maxOf { castigo(it) }
    return listOf(
        rel(jugador.goles.toDouble(), base.maxOf { it.goles }.toDouble()),
        rel(jugador.asistencias.toDouble(), base.maxOf { it.asistencias }.toDouble()),
        rel(jugador.minutosJugados.toDouble(), base.maxOf { it.minutosJugados }.toDouble()),
        (jugador.porcentajeAsistenciaEntrenamientos / 100.0).toFloat(),
        if (peor <= 0.0) 1f else (1.0 - castigo(jugador) / peor).toFloat().coerceAtLeast(0.1f),
    )
}

private fun edad(fechaIso: String): Int? = runCatching {
    Period.between(LocalDate.parse(fechaIso.take(10)), LocalDate.now()).years
}.getOrNull()?.takeIf { it in 3..80 }
