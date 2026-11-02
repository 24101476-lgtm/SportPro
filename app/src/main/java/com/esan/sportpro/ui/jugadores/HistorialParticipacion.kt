package com.esan.sportpro.ui.jugadores

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.esan.sportpro.core.fecha.FechasSportPro
import com.esan.sportpro.domain.entrenamientos.AsistenciaMensual
import com.esan.sportpro.domain.entrenamientos.Entrenamiento
import com.esan.sportpro.domain.entrenamientos.EstadoAsistencia
import com.esan.sportpro.domain.entrenamientos.FiltroHistorial
import com.esan.sportpro.domain.entrenamientos.RegistroParticipacion
import com.esan.sportpro.domain.entrenamientos.ReglasAsistencia
import com.esan.sportpro.domain.entrenamientos.ReglasHistorial
import com.esan.sportpro.domain.entrenamientos.ResumenHistorial
import com.esan.sportpro.domain.entrenamientos.TipoActividad
import com.esan.sportpro.domain.jugadores.Jugador
import com.esan.sportpro.ui.common.CampoFecha
import com.esan.sportpro.ui.common.ChipEstado
import com.esan.sportpro.ui.common.TituloSeccion
import com.esan.sportpro.ui.common.TonoEstado
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * US-012 — Historial de participación del jugador (frames 55_Historial_Jugador,
 * 56_Historial_Filtros y 57_Historial_Vacio). La usan el detalle del jugador (entrenador y
 * administrador) y "Mis hijos" (padre). Quién puede abrirla lo decide [ReglasHistorial.puedeVer].
 *
 * Los filtros viven en la pantalla (sobreviven a la rotación) y todo se calcula sobre las sesiones
 * que el módulo ya observa, así que también funciona sin conexión con la caché de Firestore.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HistorialParticipacion(
    jugador: Jugador,
    entrenamientos: List<Entrenamiento>,
    modifier: Modifier = Modifier,
    hoy: LocalDate = FechasSportPro.hoy(),
) {
    var desdeIso by rememberSaveable(jugador.id) { mutableStateOf("") }
    var hastaIso by rememberSaveable(jugador.id) { mutableStateOf("") }
    var tipoNombre by rememberSaveable(jugador.id) { mutableStateOf("") }

    val tipo = TipoActividad.entries.firstOrNull { it.name == tipoNombre }
    val filtro = FiltroHistorial(
        desde = FechasSportPro.desdeIso(desdeIso),
        hasta = FechasSportPro.desdeIso(hastaIso),
        tipo = tipo,
    )
    val errorFiltro = ReglasHistorial.validarFiltro(filtro)

    val todos = remember(jugador.id, entrenamientos) {
        ReglasHistorial.ordenar(ReglasHistorial.desdeEntrenamientos(jugador.id, entrenamientos))
    }
    val filtrados = if (errorFiltro != null) emptyList() else ReglasHistorial.filtrar(todos, filtro)
    val resumen = ReglasHistorial.resumen(filtrados)
    // El gráfico siempre cubre los últimos seis meses; solo respeta el tipo de actividad.
    val meses = ReglasHistorial.mensual(ReglasHistorial.filtrar(todos, FiltroHistorial(tipo = tipo)), hoy)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { TarjetaResumen(resumen) }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TituloSeccion("Filtros")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = tipo == null, onClick = { tipoNombre = "" }, label = { Text("Todas") })
                    FilterChip(
                        selected = tipo == TipoActividad.ENTRENAMIENTO,
                        onClick = { tipoNombre = TipoActividad.ENTRENAMIENTO.name },
                        label = { Text("Entrenamientos") },
                    )
                    FilterChip(
                        selected = tipo == TipoActividad.PARTIDO,
                        onClick = { tipoNombre = TipoActividad.PARTIDO.name },
                        label = { Text("Partidos") },
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = desdeIso.isBlank() && hastaIso.isBlank(),
                        onClick = { desdeIso = ""; hastaIso = "" },
                        label = { Text("Toda la temporada") },
                    )
                    FilterChip(
                        selected = desdeIso == FechasSportPro.aIso(hoy.minusMonths(1)) && hastaIso == FechasSportPro.aIso(hoy),
                        onClick = { desdeIso = FechasSportPro.aIso(hoy.minusMonths(1)); hastaIso = FechasSportPro.aIso(hoy) },
                        label = { Text("Último mes") },
                    )
                    FilterChip(
                        selected = desdeIso == FechasSportPro.aIso(hoy.minusMonths(3)) && hastaIso == FechasSportPro.aIso(hoy),
                        onClick = { desdeIso = FechasSportPro.aIso(hoy.minusMonths(3)); hastaIso = FechasSportPro.aIso(hoy) },
                        label = { Text("Últimos 3 meses") },
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
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

        item {
            Column {
                TituloSeccion("Asistencia mensual · últimos 6 meses")
                GraficoMensual(meses)
            }
        }

        item { TituloSeccion("Sesiones (${filtrados.size})") }

        if (tipo == TipoActividad.PARTIDO) {
            item {
                Text(
                    "La participación en partidos se mostrará cuando las convocatorias registren qué jugadores jugaron.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (filtrados.isEmpty()) {
            item { EstadoVacioHistorial() }
        } else {
            items(filtrados, key = { "${it.tipo}-${it.id}" }) { registro -> FilaRegistro(registro) }
        }
    }
}

@Composable
private fun TarjetaResumen(resumen: ResumenHistorial) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Porcentaje de asistencia", style = MaterialTheme.typography.labelLarge)
            Text(
                ReglasAsistencia.formatearPorcentaje(resumen.porcentaje),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "${resumen.participaciones} de ${resumen.convocadas} sesiones convocadas (asistencias + tardanzas)",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Contador("Convocadas", resumen.convocadas, Modifier.weight(1f))
                Contador("Asistencias", resumen.asistencias, Modifier.weight(1f))
                Contador("Tardanzas", resumen.tardanzas, Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Contador("Faltas justificadas", resumen.justificadas, Modifier.weight(1f))
                Contador("Faltas injustificadas", resumen.injustificadas, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Contador(etiqueta: String, valor: Int, modifier: Modifier) {
    Column(modifier = modifier.padding(vertical = 4.dp)) {
        Text(valor.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(etiqueta, style = MaterialTheme.typography.labelSmall)
    }
}

/** Criterio 7: una barra por mes con el porcentaje de asistencia; "—" si no hubo sesiones. */
@Composable
private fun GraficoMensual(meses: List<AsistenciaMensual>) {
    val altoMaximo = 110.dp
    Row(
        modifier = Modifier.fillMaxWidth().height(160.dp).padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        meses.forEach { mes ->
            val sinSesiones = mes.convocadas == 0
            val descripcion = if (sinSesiones) {
                "${mes.etiqueta}: sin sesiones"
            } else {
                "${mes.etiqueta}: ${ReglasAsistencia.formatearPorcentaje(mes.porcentaje)}, ${mes.participaciones} de ${mes.convocadas}"
            }
            Column(
                modifier = Modifier.weight(1f).semantics { contentDescription = descripcion },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Text(
                    if (sinSesiones) "—" else "${mes.porcentaje.roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                )
                val alto = if (sinSesiones) 2.dp else (altoMaximo * (mes.porcentaje / 100.0).toFloat()).coerceAtLeast(4.dp)
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .fillMaxWidth(0.55f)
                        .height(alto)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(
                            if (sinSesiones) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary,
                        ),
                )
                Text(
                    mes.etiqueta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun FilaRegistro(registro: RegistroParticipacion) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    listOf(FechasSportPro.fecha(registro.fecha), registro.hora).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    "${registro.tipo.etiqueta} · ${registro.equipoNombre}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (registro.descripcion.isNotBlank()) {
                    Text(registro.descripcion, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                ChipEstado(registro.estado.etiqueta, registro.estado.tono())
                registro.minutosRetraso?.let {
                    Text(
                        "$it min de retraso",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
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
        Icon(
            Icons.Default.EventBusy,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp),
        )
        Text(
            ReglasHistorial.MENSAJE_VACIO,
            style = MaterialTheme.typography.bodyMedium,
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
