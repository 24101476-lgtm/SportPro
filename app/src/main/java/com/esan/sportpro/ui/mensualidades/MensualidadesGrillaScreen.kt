package com.esan.sportpro.ui.mensualidades

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.core.fecha.FechasSportPro
import com.esan.sportpro.domain.mensualidades.EstadoMensualidad
import com.esan.sportpro.ui.common.EncabezadoModulo
import com.esan.sportpro.ui.common.EstadoVacioModulo
import com.esan.sportpro.ui.common.RegistroSimuladoLegend
import com.esan.sportpro.ui.partido.AvatarJugador
import com.esan.sportpro.ui.partido.ColoresPartido

/**
 * US-008 — Frame 37_Mensualidades_Grilla.
 * Administrador: ve montos completos y registra. Entrenador: solo el indicador de estado, sin montos
 * (criterio de aceptación 7, resuelto con [MensualidadesGrillaUiState.rolActual]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MensualidadesGrillaScreen(
    onNuevoRegistro: (jugadorId: String?, mes: Int, anio: Int) -> Unit,
    onVerHistorial: (jugadorId: String, jugadorNombre: String, fotoUrl: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MensualidadesGrillaViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MensualidadesGrillaEvent.AbrirRegistrar -> onNuevoRegistro(event.jugadorId, event.mes, event.anio)
                is MensualidadesGrillaEvent.AbrirHistorial -> onVerHistorial(event.jugadorId, event.jugadorNombre, event.fotoUrl)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            if (state.esAdministrador) {
                ExtendedFloatingActionButton(
                    onClick = viewModel::onNuevoRegistro,
                    containerColor = ColoresPartido.Verde,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Registrar", fontWeight = FontWeight.Bold) },
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                EncabezadoModulo(
                    foto = com.esan.sportpro.ui.common.FotoSportPro.BOTINES,
                    titulo = "Mensualidades",
                    subtitulo = "Control de pagos del mes por jugador",
                    icono = Icons.Default.Payments,
                )
            }
            item { RegistroSimuladoLegend() }

            // Entrenador: solo ve los equipos asignados a su cuenta (las reglas de Firestore igual).
            if (state.rolActual == com.esan.sportpro.navigation.UserRole.ENTRENADOR &&
                (state.sinEquiposAsignados || state.equiposSoloPorNombre.isNotEmpty())
            ) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            buildString {
                                if (state.sinEquiposAsignados) {
                                    append("Aún no tienes equipos asignados a tu cuenta, así que no ves mensualidades. ")
                                }
                                if (state.equiposSoloPorNombre.isNotEmpty()) {
                                    append("Figuras por nombre en ${state.equiposSoloPorNombre.joinToString()}. ")
                                }
                                append("Pide al administrador que te elija como entrenador al editar el equipo.")
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                }
            }

            item {
                ResumenDelMes(
                    state = state,
                    onAnterior = viewModel::onMesAnterior,
                    onSiguiente = viewModel::onMesSiguiente,
                    onFiltroEstado = viewModel::onFiltroEstadoChanged,
                )
            }

            if (state.equiposDisponibles.size > 1) {
                item {
                    com.esan.sportpro.ui.common.SelectorEquipo(
                        equipos = state.equiposDisponibles.map { com.esan.sportpro.domain.academia.Equipo(id = it.id, nombre = it.nombre) },
                        seleccionadoId = state.equipoFiltro,
                        onSeleccionar = viewModel::onFiltroEquipoChanged,
                    )
                }
            }

            state.estadoFiltro?.let { filtro ->
                item {
                    Surface(
                        onClick = { viewModel.onFiltroEstadoChanged(null) },
                        shape = RoundedCornerShape(50),
                        color = colorEstado(filtro).copy(alpha = 0.12f),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(
                                "Solo ${filtro.etiqueta.lowercase()}s",
                                color = colorEstado(filtro),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Quitar filtro",
                                tint = colorEstado(filtro),
                                modifier = Modifier.padding(start = 6.dp).size(16.dp),
                            )
                        }
                    }
                }
            }

            when {
                state.isLoading -> item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.errorMessage != null -> item {
                    Text(state.errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
                state.mensualidades.isEmpty() -> item {
                    EstadoVacioModulo(
                        titulo = "Sin jugadores en esta vista",
                        mensaje = "No hay jugadores que coincidan con este mes o filtro",
                        icono = Icons.Default.Payments,
                    )
                }
                else -> items(state.mensualidades, key = { it.mensualidad.id }) { fila ->
                    FilaMensualidad(
                        fila = fila,
                        verMontos = state.esAdministrador,
                        diaCorte = state.diaCorte,
                        onClick = { viewModel.onJugadorSeleccionado(fila) },
                        onRegistrar = if (state.esAdministrador) ({ viewModel.onRegistrarDe(fila) }) else null,
                    )
                }
            }
        }
    }
}

// ============================================================
// RESUMEN
// ============================================================

@Composable
private fun ResumenDelMes(
    state: MensualidadesGrillaUiState,
    onAnterior: () -> Unit,
    onSiguiente: () -> Unit,
    onFiltroEstado: (EstadoMensualidad) -> Unit,
) {
    val r = state.resumen
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onAnterior) { Icon(Icons.Default.ChevronLeft, contentDescription = "Mes anterior") }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(nombreMes(state.mes, state.anio), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (state.esMesActual) "Mes actual · vence el día ${state.diaCorte}"
                        else "Vence el día ${state.diaCorte}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onSiguiente) { Icon(Icons.Default.ChevronRight, contentDescription = "Mes siguiente") }
            }

            // Avance: cuántos jugadores ya no deben nada este mes.
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "${r.alDia}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        " de ${r.total} al día",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp).weight(1f),
                    )
                    if (state.esAdministrador) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Recaudado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "S/ %.2f".format(r.recaudado),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = colorEstado(EstadoMensualidad.PAGADO),
                            )
                        }
                    }
                }
                LinearProgressIndicator(
                    progress = { r.progreso },
                    color = colorEstado(EstadoMensualidad.PAGADO),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                )
            }

            // Contadores por estado: tocarlos filtra la lista.
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    EstadoMensualidad.VENCIDO,
                    EstadoMensualidad.PENDIENTE,
                    EstadoMensualidad.PAGADO,
                    EstadoMensualidad.EXONERADO,
                ).forEach { estado ->
                    ContadorEstado(
                        estado = estado,
                        cantidad = r.cantidad(estado),
                        seleccionado = state.estadoFiltro == estado,
                        modifier = Modifier.weight(1f),
                    ) { onFiltroEstado(estado) }
                }
            }
        }
    }
}

@Composable
private fun ContadorEstado(
    estado: EstadoMensualidad,
    cantidad: Int,
    seleccionado: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val color = colorEstado(estado)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (seleccionado) color else color.copy(alpha = 0.10f),
        contentColor = if (seleccionado) Color.White else color,
        border = if (seleccionado) null else BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
        ) {
            Text("$cantidad", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(
                estado.etiqueta,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ============================================================
// FILAS
// ============================================================

@Composable
private fun FilaMensualidad(
    fila: MensualidadConEstadoEfectivo,
    verMontos: Boolean,
    diaCorte: Int,
    onClick: () -> Unit,
    onRegistrar: (() -> Unit)?,
) {
    val m = fila.mensualidad
    val estado = fila.estadoEfectivo
    val color = colorEstado(estado)
    val formato = remember { FechasSportPro.formatoFechaCorta() }
    val debe = estado == EstadoMensualidad.VENCIDO || estado == EstadoMensualidad.PENDIENTE

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Franja lateral del color del estado, dibujada detrás para que siga la altura de la fila.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .heightIn(min = 76.dp)
                .drawBehind { drawRect(color, size = androidx.compose.ui.geometry.Size(5.dp.toPx(), size.height)) },
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 17.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            ) {
                AvatarJugador(nombre = m.jugadorNombre, fotoUrl = fila.fotoUrl, tamano = 46.dp)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        m.jugadorNombre.ifBlank { "Jugador" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        m.equipoNombre.ifBlank { "Sin equipo" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    Text(
                        detalle(fila, verMontos, formato, diaCorte),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (debe) color else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (debe) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    EstadoMensualidadChip(estado = estado)
                    // Registro rápido: el formulario abre con este jugador y este mes.
                    if (onRegistrar != null && debe) {
                        Surface(
                            onClick = onRegistrar,
                            shape = RoundedCornerShape(50),
                            color = ColoresPartido.Verde,
                            contentColor = Color.White,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Text(
                                    "Registrar",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 3.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun detalle(
    fila: MensualidadConEstadoEfectivo,
    verMontos: Boolean,
    formato: java.text.SimpleDateFormat,
    diaCorte: Int,
): String {
    val m = fila.mensualidad
    if (fila.sinRegistro) {
        return if (fila.estadoEfectivo == EstadoMensualidad.VENCIDO) "Sin pago · venció el día $diaCorte"
        else "Sin pago registrado"
    }
    val fecha = formato.format(m.fechaRegistro)
    return when (m.estadoRegistrado) {
        EstadoMensualidad.PAGADO ->
            if (verMontos) "S/ %.2f · %s · %s".format(m.montoReferencial, m.medioDeclarado.etiqueta, fecha)
            else "Pagó el $fecha"
        EstadoMensualidad.EXONERADO -> "Exonerado el $fecha"
        else -> if (verMontos && m.montoReferencial > 0) "Debe S/ %.2f".format(m.montoReferencial) else "Registrado el $fecha"
    }
}
