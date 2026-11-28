package com.esan.sportpro.ui.jugadores

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.esan.sportpro.core.fecha.FechasSportPro
import com.esan.sportpro.core.theme.FuenteNumeros
import com.esan.sportpro.core.theme.PaletaSportPro
import com.esan.sportpro.core.theme.SportProTema
import com.esan.sportpro.domain.entrenamientos.AsistenciaMensual
import com.esan.sportpro.domain.entrenamientos.Entrenamiento
import com.esan.sportpro.domain.entrenamientos.EstadoAsistencia
import com.esan.sportpro.domain.entrenamientos.FiltroHistorial
import com.esan.sportpro.domain.entrenamientos.RegistroParticipacion
import com.esan.sportpro.domain.entrenamientos.ReglasHistorial
import com.esan.sportpro.domain.entrenamientos.ResumenHistorial
import com.esan.sportpro.domain.entrenamientos.TipoActividad
import com.esan.sportpro.domain.jugadores.Jugador
import com.esan.sportpro.ui.common.CampoFecha
import com.esan.sportpro.ui.common.ChipEstado
import com.esan.sportpro.ui.common.TonoEstado
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * US-012 — Historial de participación del jugador (frames 55_Historial_Jugador,
 * 56_Historial_Filtros y 57_Historial_Vacio). La usan el detalle del jugador (entrenador y
 * administrador) y "Mis hijos" (padre). Quién puede abrirla lo decide [ReglasHistorial.puedeVer].
 *
 * Los filtros viven en la pantalla (sobreviven a la rotación). Los entrenamientos se calculan sobre
 * las sesiones que el módulo ya observa; los partidos terminados llegan en [partidos]
 * (`ParticipacionPartidosRepository`).
 *
 * Diseño: tarjeta oscura con el anillo de asistencia, filtros en píldoras, barras por mes y la
 * lista agrupada por mes, con la fecha en grande y el marcador de cada partido.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialParticipacion(
    jugador: Jugador,
    entrenamientos: List<Entrenamiento>,
    modifier: Modifier = Modifier,
    partidos: List<RegistroParticipacion> = emptyList(),
    cargandoPartidos: Boolean = false,
    hoy: LocalDate = FechasSportPro.hoy(),
) {
    var desdeIso by rememberSaveable(jugador.id) { mutableStateOf("") }
    var hastaIso by rememberSaveable(jugador.id) { mutableStateOf("") }
    var tipoNombre by rememberSaveable(jugador.id) { mutableStateOf("") }
    var rangoPropio by rememberSaveable(jugador.id) { mutableStateOf(false) }

    val tipo = TipoActividad.entries.firstOrNull { it.name == tipoNombre }
    val filtro = FiltroHistorial(
        desde = FechasSportPro.desdeIso(desdeIso),
        hasta = FechasSportPro.desdeIso(hastaIso),
        tipo = tipo,
    )
    val errorFiltro = ReglasHistorial.validarFiltro(filtro)

    val todos = remember(jugador.id, entrenamientos, partidos) {
        ReglasHistorial.ordenar(ReglasHistorial.desdeEntrenamientos(jugador.id, entrenamientos) + partidos)
    }
    val filtrados = if (errorFiltro != null) emptyList() else ReglasHistorial.filtrar(todos, filtro)
    val resumen = ReglasHistorial.resumen(filtrados)
    // El gráfico siempre cubre los últimos seis meses; solo respeta el tipo de actividad.
    val meses = ReglasHistorial.mensual(ReglasHistorial.filtrar(todos, FiltroHistorial(tipo = tipo)), hoy)

    val haceUnMes = FechasSportPro.aIso(hoy.minusMonths(1))
    val haceTresMeses = FechasSportPro.aIso(hoy.minusMonths(3))
    val hoyIso = FechasSportPro.aIso(hoy)
    val periodo = when {
        rangoPropio -> Periodo.PROPIO
        desdeIso.isBlank() && hastaIso.isBlank() -> Periodo.TEMPORADA
        desdeIso == haceUnMes && hastaIso == hoyIso -> Periodo.MES
        desdeIso == haceTresMeses && hastaIso == hoyIso -> Periodo.TRIMESTRE
        else -> Periodo.PROPIO
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "resumen") { TarjetaResumen(resumen, tipo) }

        item(key = "filtros") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
                Segmentado(
                    opciones = listOf("Todo" to null, "Entrenamientos" to TipoActividad.ENTRENAMIENTO, "Partidos" to TipoActividad.PARTIDO),
                    seleccionada = tipo,
                    onElegir = { tipoNombre = it?.name.orEmpty() },
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                ) {
                    Pildora("Temporada", periodo == Periodo.TEMPORADA) {
                        rangoPropio = false; desdeIso = ""; hastaIso = ""
                    }
                    Pildora("Último mes", periodo == Periodo.MES) {
                        rangoPropio = false; desdeIso = haceUnMes; hastaIso = hoyIso
                    }
                    Pildora("3 meses", periodo == Periodo.TRIMESTRE) {
                        rangoPropio = false; desdeIso = haceTresMeses; hastaIso = hoyIso
                    }
                    Pildora("Elegir fechas", periodo == Periodo.PROPIO, icono = Icons.Default.DateRange) { rangoPropio = true }
                }
                if (periodo == Periodo.PROPIO) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CampoFecha(
                            valorIso = desdeIso,
                            onCambio = { desdeIso = it },
                            etiqueta = "Desde",
                            ayuda = "",
                            error = errorFiltro,
                            modifier = Modifier.weight(1f),
                        )
                        CampoFecha(
                            valorIso = hastaIso,
                            onCambio = { hastaIso = it },
                            etiqueta = "Hasta",
                            ayuda = "",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        item(key = "grafico") { GraficoMensual(meses, hoy) }

        item(key = "titulo-sesiones") {
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 10.dp)) {
                Text("Sesiones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "  ${filtrados.size}",
                    style = TextStyle(fontFamily = FuenteNumeros, fontSize = 18.sp),
                    color = SportProTema.colores.textoAcento,
                )
            }
        }

        if (cargandoPartidos && tipo != TipoActividad.ENTRENAMIENTO) {
            item(key = "cargando") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(
                        "Cargando partidos…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (filtrados.isEmpty()) {
            item(key = "vacio") { EstadoVacioHistorial() }
        } else {
            // Agrupado por mes: "Noviembre de 2026", luego sus sesiones.
            filtrados.groupBy { YearMonth.from(it.fecha) }.forEach { (mes, registros) ->
                item(key = "mes-$mes") {
                    Text(
                        mes.format(FORMATO_MES).replaceFirstChar { it.titlecase(FechasSportPro.LOCALE) },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                items(registros, key = { "${it.tipo}-${it.id}" }) { registro -> FilaRegistro(registro) }
            }
        }
    }
}

private enum class Periodo { TEMPORADA, MES, TRIMESTRE, PROPIO }

// ---------------------------------------------------------------------------------------------
// Resumen: anillo con el porcentaje y los cuatro contadores
// ---------------------------------------------------------------------------------------------

@Composable
private fun TarjetaResumen(resumen: ResumenHistorial, tipo: TipoActividad?) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = PaletaSportPro.Carbon,
        contentColor = PaletaSportPro.Tiza,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnilloAsistencia(porcentaje = resumen.porcentaje.toFloat(), sinDatos = resumen.convocadas == 0)
                Column(modifier = Modifier.padding(start = 18.dp).weight(1f)) {
                    Text(
                        when (tipo) {
                            TipoActividad.ENTRENAMIENTO -> "Asistencia a entrenamientos"
                            TipoActividad.PARTIDO -> "Asistencia a partidos"
                            null -> "Asistencia del periodo"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = PaletaSportPro.Niebla,
                    )
                    Text(
                        if (resumen.convocadas == 0) "Sin sesiones convocadas" else "${resumen.participaciones} de ${resumen.convocadas} sesiones",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Text(
                        "Cuentan las asistencias y las tardanzas",
                        style = MaterialTheme.typography.bodySmall,
                        color = PaletaSportPro.Niebla,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
            ) {
                Contador("Asistió", resumen.asistencias, PaletaSportPro.ExitoClaro, Modifier.weight(1f))
                Contador("Tarde", resumen.tardanzas, PaletaSportPro.AdvertenciaClaro, Modifier.weight(1f))
                Contador("Justificó", resumen.justificadas, PaletaSportPro.InformacionClaro, Modifier.weight(1f))
                Contador("Faltó", resumen.injustificadas, PaletaSportPro.ErrorClaro, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AnilloAsistencia(porcentaje: Float, sinDatos: Boolean) {
    val pista = PaletaSportPro.CarbonLinea
    val lima = PaletaSportPro.Lima
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(104.dp).semantics {
            contentDescription = if (sinDatos) "Sin datos de asistencia" else "Asistencia ${porcentaje.roundToInt()} por ciento"
        },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val grosor = 11.dp.toPx()
            val lado = size.minDimension - grosor
            val esquina = Offset((size.width - lado) / 2f, (size.height - lado) / 2f)
            drawArc(pista, -90f, 360f, false, esquina, Size(lado, lado), style = Stroke(grosor))
            if (!sinDatos && porcentaje > 0f) {
                drawArc(
                    lima,
                    -90f,
                    360f * (porcentaje / 100f).coerceIn(0f, 1f),
                    false,
                    esquina,
                    Size(lado, lado),
                    style = Stroke(grosor, cap = StrokeCap.Round),
                )
            }
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                if (sinDatos) "—" else porcentaje.roundToInt().toString(),
                style = TextStyle(fontFamily = FuenteNumeros, fontSize = 34.sp, lineHeight = 34.sp),
                color = PaletaSportPro.Tiza,
            )
            if (!sinDatos) {
                Text(
                    "%",
                    style = TextStyle(fontFamily = FuenteNumeros, fontSize = 16.sp),
                    color = PaletaSportPro.Lima,
                    modifier = Modifier.padding(start = 1.dp, bottom = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun Contador(etiqueta: String, valor: Int, color: Color, modifier: Modifier) {
    Surface(shape = RoundedCornerShape(16.dp), color = PaletaSportPro.CarbonAlto, modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp),
        ) {
            Text(valor.toString(), style = TextStyle(fontFamily = FuenteNumeros, fontSize = 24.sp, lineHeight = 26.sp), color = color)
            Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = PaletaSportPro.Niebla, maxLines = 1)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Filtros
// ---------------------------------------------------------------------------------------------

/** Control segmentado: la opción activa en carbón con texto lima. */
@Composable
private fun Segmentado(
    opciones: List<Pair<String, TipoActividad?>>,
    seleccionada: TipoActividad?,
    onElegir: (TipoActividad?) -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            opciones.forEach { (texto, valor) ->
                val activa = valor == seleccionada
                Surface(
                    onClick = { onElegir(valor) },
                    shape = CircleShape,
                    color = if (activa) PaletaSportPro.Carbon else Color.Transparent,
                    contentColor = if (activa) PaletaSportPro.Lima else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        texto,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (activa) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Pildora(texto: String, activa: Boolean, icono: ImageVector? = null, onClick: () -> Unit) {
    val tema = SportProTema.colores
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (activa) tema.acento else Color.Transparent,
        contentColor = if (activa) tema.sobreAcento else MaterialTheme.colorScheme.onSurface,
        border = if (activa) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            if (icono != null) Icon(icono, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(
                texto,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (activa) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier.padding(start = if (icono != null) 4.dp else 0.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Gráfico por mes
// ---------------------------------------------------------------------------------------------

/** Criterio 7: una barra por mes con el porcentaje de asistencia; "—" si no hubo sesiones. */
@Composable
private fun GraficoMensual(meses: List<AsistenciaMensual>, hoy: LocalDate) {
    val altoMaximo = 96.dp
    val mesActual = YearMonth.from(hoy)
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Asistencia por mes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                "Últimos 6 meses",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth().height(150.dp).padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom,
            ) {
                meses.forEach { mes ->
                    val sinSesiones = mes.convocadas == 0
                    val esActual = mes.mes == mesActual
                    val descripcion = if (sinSesiones) {
                        "${mes.etiqueta}: sin sesiones"
                    } else {
                        "${mes.etiqueta}: ${mes.porcentaje.roundToInt()} %, ${mes.participaciones} de ${mes.convocadas}"
                    }
                    Column(
                        modifier = Modifier.weight(1f).semantics { contentDescription = descripcion },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        Text(
                            if (sinSesiones) "—" else "${mes.porcentaje.roundToInt()}%",
                            style = TextStyle(fontFamily = FuenteNumeros, fontSize = 13.sp),
                            color = if (sinSesiones) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                        val alto = if (sinSesiones) 4.dp else (altoMaximo * (mes.porcentaje / 100.0).toFloat()).coerceAtLeast(8.dp)
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .fillMaxWidth(0.62f)
                                .height(alto)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    when {
                                        sinSesiones -> MaterialTheme.colorScheme.outlineVariant
                                        esActual -> PaletaSportPro.Lima
                                        else -> PaletaSportPro.MusgoMedio
                                    },
                                ),
                        )
                        Text(
                            mes.etiqueta,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (esActual) FontWeight.Bold else FontWeight.Normal,
                            color = if (esActual) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Una sesión
// ---------------------------------------------------------------------------------------------

/** Partes de la descripción de un partido: "vs Mallorca · 1-0 · Jugó 45 min". */
private data class DatosPartido(val rival: String, val marcador: String, val detalle: String)

private fun datosPartido(registro: RegistroParticipacion): DatosPartido? {
    if (registro.tipo != TipoActividad.PARTIDO) return null
    val partes = registro.descripcion.split(" · ")
    if (partes.size < 3 || !partes[0].startsWith("vs ")) return null
    return DatosPartido(rival = partes[0], marcador = partes[1], detalle = partes.drop(2).joinToString(" · "))
}

@Composable
private fun FilaRegistro(registro: RegistroParticipacion) {
    val partido = datosPartido(registro)
    val colorEstado = registro.estado.color()
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Franja con el color del estado: se lee la racha de un vistazo al bajar por la lista.
            Box(Modifier.width(5.dp).fillMaxHeight().background(colorEstado))
            Row(
                modifier = Modifier.padding(start = 12.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(40.dp)) {
                    Text(
                        registro.fecha.dayOfMonth.toString(),
                        style = TextStyle(fontFamily = FuenteNumeros, fontSize = 26.sp, lineHeight = 26.sp),
                    )
                    Text(
                        registro.fecha.format(FORMATO_MES_CORTO).trimEnd('.'),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (registro.tipo == TipoActividad.PARTIDO) Icons.Default.SportsSoccer else Icons.Default.FitnessCenter,
                            contentDescription = registro.tipo.etiqueta,
                            tint = SportProTema.colores.textoAcento,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            partido?.rival ?: registro.descripcion.ifBlank { registro.tipo.etiqueta },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 6.dp).weight(1f, fill = false),
                        )
                        partido?.let { MarcadorPildora(it.marcador) }
                    }
                    Text(
                        listOf(registro.equipoNombre, registro.hora).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    val detalle = partido?.detalle?.takeIf { !it.startsWith("Jugó") }
                    if (detalle != null) {
                        Text(detalle, style = MaterialTheme.typography.bodySmall, maxLines = 2, modifier = Modifier.padding(top = 2.dp))
                    }
                    ChipEstado(registro.estado.etiqueta, registro.estado.tono(), modifier = Modifier.padding(top = 6.dp))
                }
                // A la derecha, lo que más importa del día: minutos jugados o el retraso.
                val minutos = registro.minutosJugados?.takeIf { it > 0 }
                val retraso = registro.minutosRetraso
                if (minutos != null || retraso != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            "${minutos ?: retraso}'",
                            style = TextStyle(fontFamily = FuenteNumeros, fontSize = 24.sp, lineHeight = 24.sp),
                            color = if (minutos != null) SportProTema.colores.textoAcento else SportProTema.colores.advertencia,
                        )
                        Text(
                            if (minutos != null) "jugados" else "de retraso",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** "1-0" en verde si ganó, gris si empató y rojo si perdió. */
@Composable
private fun MarcadorPildora(marcador: String) {
    val goles = marcador.split("-").mapNotNull { it.trim().toIntOrNull() }
    val tema = SportProTema.colores
    val color = when {
        goles.size != 2 -> tema.neutro
        goles[0] > goles[1] -> tema.exito
        goles[0] < goles[1] -> tema.error
        else -> tema.neutro
    }
    Surface(shape = RoundedCornerShape(8.dp), color = color, contentColor = Color.White, modifier = Modifier.padding(start = 8.dp)) {
        Text(
            marcador.replace("-", " - "),
            style = TextStyle(fontFamily = FuenteNumeros, fontSize = 13.sp, lineHeight = 14.sp),
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
        )
    }
}

/** Frame 57_Historial_Vacio. */
@Composable
private fun EstadoVacioHistorial() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Icon(
                Icons.Default.EventBusy,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(30.dp),
            )
        }
        Text(
            ReglasHistorial.MENSAJE_VACIO,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            "Prueba con otro periodo o con \"Todo\".",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private fun EstadoAsistencia.tono(): TonoEstado = when (this) {
    EstadoAsistencia.PRESENTE -> TonoEstado.EXITO
    EstadoAsistencia.TARDE -> TonoEstado.ADVERTENCIA
    EstadoAsistencia.JUSTIFICADO -> TonoEstado.INFORMACION
    EstadoAsistencia.AUSENTE -> TonoEstado.ERROR
}

@Composable
private fun EstadoAsistencia.color(): Color {
    val tema = SportProTema.colores
    return when (this) {
        EstadoAsistencia.PRESENTE -> tema.exito
        EstadoAsistencia.TARDE -> tema.advertencia
        EstadoAsistencia.JUSTIFICADO -> tema.informacion
        EstadoAsistencia.AUSENTE -> tema.error
    }
}

private val FORMATO_MES = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", FechasSportPro.LOCALE)
private val FORMATO_MES_CORTO = DateTimeFormatter.ofPattern("MMM", FechasSportPro.LOCALE)
