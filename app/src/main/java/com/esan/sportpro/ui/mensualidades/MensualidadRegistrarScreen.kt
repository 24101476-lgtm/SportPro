package com.esan.sportpro.ui.mensualidades

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.jugadores.Jugador
import com.esan.sportpro.domain.mensualidades.EstadoMensualidad
import com.esan.sportpro.domain.mensualidades.MedioPago
import com.esan.sportpro.ui.common.AvisoErrorSportPro
import com.esan.sportpro.ui.common.BarraSuperiorSportPro
import com.esan.sportpro.ui.common.IndicadorCargaBoton
import com.esan.sportpro.ui.common.RegistroSimuladoLegend
import com.esan.sportpro.ui.partido.AvatarJugador

/**
 * US-008 — Frame 38_Mensualidad_Registrar. Solo Administrador (verificado por la navegación).
 * El registro es siempre nuevo: si ya existía un registro vigente para el mismo jugador/mes/año,
 * el repositorio lo anula automáticamente (criterio de aceptación 6) — esta pantalla no permite
 * editar ni eliminar un registro existente directamente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MensualidadRegistrarScreen(
    onGuardado: () -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier,
    jugadorInicialId: String? = null,
    mesInicial: Int = java.time.LocalDate.now().monthValue,
    anioInicial: Int = java.time.LocalDate.now().year,
    viewModel: MensualidadRegistrarViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val contexto = LocalContext.current

    LaunchedEffect(Unit) { viewModel.inicializar(jugadorInicialId, mesInicial, anioInicial) }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is MensualidadRegistrarEvent.RegistroGuardado) {
                if (!event.sincronizado) {
                    Toast.makeText(
                        contexto,
                        "Guardado sin conexión. Se sincronizará al volver la señal.",
                        Toast.LENGTH_LONG,
                    ).show()
                }
                onGuardado()
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            BarraSuperiorSportPro(
                title = { Text("Registrar mensualidad") },
                navigationIcon = {
                    IconButton(onClick = onCancelar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            RegistroSimuladoLegend()

            Seccion("Jugador") {
                SelectorJugador(
                    state = state,
                    onBusqueda = viewModel::onBusquedaJugadorChanged,
                    onElegir = viewModel::onJugadorElegido,
                    onQuitar = viewModel::onJugadorQuitado,
                )
            }

            Seccion("Mes") {
                SelectorMes(
                    mes = state.mes,
                    anio = state.anio,
                    onAnterior = viewModel::onMesAnterior,
                    onSiguiente = viewModel::onMesSiguiente,
                )
                state.registroExistente?.let { previo ->
                    AvisoReemplazo(
                        estado = EstadoMensualidad.efectivo(previo.estadoRegistrado, previo.mes, previo.anio),
                        monto = previo.montoReferencial,
                    )
                }
            }

            Seccion("Estado") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(EstadoMensualidad.PAGADO, EstadoMensualidad.PENDIENTE, EstadoMensualidad.EXONERADO).forEach { estado ->
                        OpcionEstado(
                            estado = estado,
                            seleccionado = state.estadoSeleccionado == estado,
                            modifier = Modifier.weight(1f),
                        ) { viewModel.onEstadoChanged(estado) }
                    }
                }
            }

            if (state.requiereMonto) {
                Seccion("Monto referencial") {
                    OutlinedTextField(
                        value = state.montoTexto,
                        onValueChange = viewModel::onMontoChanged,
                        prefix = { Text("S/ ") },
                        placeholder = { Text("0.00") },
                        isError = state.montoError != null,
                        supportingText = {
                            Text(state.montoError ?: "Entre S/ 0.00 y S/ 5000.00")
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // El medio solo tiene sentido cuando se recibió dinero.
            if (state.estadoSeleccionado == EstadoMensualidad.PAGADO) {
                Seccion("Medio de pago") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MedioPago.entries.forEach { medio ->
                            OpcionTexto(
                                texto = medio.etiqueta,
                                seleccionado = state.medioDeclarado == medio,
                                modifier = Modifier.weight(1f),
                            ) { viewModel.onMedioChanged(medio) }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = state.observacion,
                onValueChange = viewModel::onObservacionChanged,
                label = { Text("Observación (opcional)") },
                isError = state.observacionError != null,
                supportingText = { Text(state.observacionError ?: "${state.observacion.length}/200") },
                minLines = 2,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            )

            state.errorMessage?.let { AvisoErrorSportPro(mensaje = it) }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onCancelar,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                ) { Text("Cancelar") }
                Button(
                    onClick = viewModel::guardar,
                    enabled = !state.isSaving,
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(containerColor = colorEstado(state.estadoSeleccionado)),
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                ) {
                    if (state.isSaving) {
                        IndicadorCargaBoton()
                    } else {
                        Text(
                            if (state.registroExistente != null) "Reemplazar" else "Guardar",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Seccion(titulo: String, contenido: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            titulo.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        contenido()
    }
}

@Composable
private fun SelectorMes(mes: Int, anio: Int, onAnterior: () -> Unit, onSiguiente: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
            IconButton(onClick = onAnterior) { Icon(Icons.Default.ChevronLeft, contentDescription = "Mes anterior") }
            Text(
                nombreMes(mes, anio),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            IconButton(onClick = onSiguiente) { Icon(Icons.Default.ChevronRight, contentDescription = "Mes siguiente") }
        }
    }
}

/** El mes ya tiene un registro vigente: el nuevo lo reemplaza y el anterior queda en el historial. */
@Composable
private fun AvisoReemplazo(estado: EstadoMensualidad, monto: Double) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = colorEstado(estado).copy(alpha = 0.12f),
        border = BorderStroke(1.dp, colorEstado(estado).copy(alpha = 0.4f)),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Info, contentDescription = null, tint = colorEstado(estado), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                buildString {
                    append("Este mes ya figura como ${estado.etiqueta.lowercase()}")
                    if (monto > 0) append(" (S/ %.2f)".format(monto))
                    append(". Al guardar se reemplaza y el registro anterior queda en el historial.")
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun OpcionEstado(
    estado: EstadoMensualidad,
    seleccionado: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val color = colorEstado(estado)
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = if (seleccionado) color else color.copy(alpha = 0.08f),
        contentColor = if (seleccionado) Color.White else color,
        border = BorderStroke(1.dp, color.copy(alpha = if (seleccionado) 1f else 0.35f)),
        modifier = modifier.heightIn(min = 72.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp),
        ) {
            Icon(iconoEstado(estado), contentDescription = null, modifier = Modifier.size(24.dp))
            Text(
                estado.etiqueta,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun OpcionTexto(texto: String, seleccionado: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val color = MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = if (seleccionado) color.copy(alpha = 0.14f) else Color.Transparent,
        border = BorderStroke(if (seleccionado) 2.dp else 1.dp, if (seleccionado) color else MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.heightIn(min = 48.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(8.dp)) {
            Text(texto, fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
        }
    }
}

/** Busca entre los jugadores activos de la academia; no admite nombres que no estén inscritos. */
@Composable
private fun SelectorJugador(
    state: MensualidadRegistrarUiState,
    onBusqueda: (String) -> Unit,
    onElegir: (Jugador) -> Unit,
    onQuitar: () -> Unit,
) {
    if (state.jugadorId.isNotBlank()) {
        val foto = state.jugadoresDisponibles.firstOrNull { it.id == state.jugadorId }?.fotoUrl.orEmpty()
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                AvatarJugador(nombre = state.jugadorNombre, fotoUrl = foto, tamano = 52.dp)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(state.jugadorNombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        state.equipoNombre.ifBlank { "Sin equipo" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onQuitar) { Text("Cambiar") }
            }
        }
        return
    }

    OutlinedTextField(
        value = state.busquedaJugador,
        onValueChange = onBusqueda,
        placeholder = { Text("Buscar jugador inscrito") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        supportingText = {
            Text(
                when {
                    state.cargandoJugadores -> "Cargando jugadores…"
                    state.jugadoresDisponibles.isEmpty() -> "No hay jugadores activos registrados en la academia"
                    else -> "Elige un jugador de la lista"
                },
            )
        },
        singleLine = true,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    )
    val consulta = state.busquedaJugador.trim()
    val coincidencias = state.jugadoresDisponibles
        .filter { consulta.isBlank() || it.nombreCompleto.contains(consulta, ignoreCase = true) }
        .take(6)
    if (!state.cargandoJugadores && coincidencias.isEmpty() && consulta.isNotBlank()) {
        Text(
            "No hay ningún jugador inscrito con ese nombre",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        coincidencias.forEach { jugador ->
            Surface(
                onClick = { onElegir(jugador) },
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)) {
                    AvatarJugador(nombre = jugador.nombreCompleto, fotoUrl = jugador.fotoUrl, tamano = 40.dp)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(
                            jugador.nombreCompleto,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (jugador.equipoNombre.isNotBlank()) {
                            Text(
                                jugador.equipoNombre,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** "Noviembre 2026". */
fun nombreMes(mes: Int, anio: Int): String =
    java.time.Month.of(mes)
        .getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale("es"))
        .replaceFirstChar { it.uppercase() } + " $anio"
