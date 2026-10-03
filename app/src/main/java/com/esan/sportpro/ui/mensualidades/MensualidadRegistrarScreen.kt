package com.esan.sportpro.ui.mensualidades

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.mensualidades.EstadoMensualidad
import com.esan.sportpro.domain.mensualidades.MedioPago
import com.esan.sportpro.ui.common.RegistroSimuladoLegend

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
    viewModel: MensualidadRegistrarViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is MensualidadRegistrarEvent.RegistroGuardado) onGuardado()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Registrar pago simulado") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RegistroSimuladoLegend()

            OutlinedTextField(
                value = state.jugadorNombre,
                onValueChange = { viewModel.onJugadorSeleccionado(state.jugadorId, it, state.equipoId, state.equipoNombre) },
                label = { Text("Jugador") },
                supportingText = { Text("TODO: selector real una vez integrado con el módulo Jugadores") },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.equipoNombre,
                onValueChange = { viewModel.onJugadorSeleccionado(state.jugadorId, state.jugadorNombre, state.equipoId, it) },
                label = { Text("Equipo") },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.montoTexto,
                onValueChange = viewModel::onMontoChanged,
                label = { Text("Monto referencial (S/)") },
                isError = state.montoError != null,
                supportingText = { state.montoError?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Medio declarado", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MedioPago.entries.forEach { medio ->
                    FilterChip(
                        selected = state.medioDeclarado == medio,
                        onClick = { viewModel.onMedioChanged(medio) },
                        label = { Text(medio.etiqueta) },
                    )
                }
            }

            Text("Estado", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(EstadoMensualidad.PAGADO, EstadoMensualidad.PENDIENTE, EstadoMensualidad.EXONERADO).forEach { estado ->
                    FilterChip(
                        selected = state.estadoSeleccionado == estado,
                        onClick = { viewModel.onEstadoChanged(estado) },
                        label = { Text(estado.etiqueta) },
                    )
                }
            }

            OutlinedTextField(
                value = state.observacion,
                onValueChange = viewModel::onObservacionChanged,
                label = { Text("Observación (opcional)") },
                isError = state.observacionError != null,
                supportingText = {
                    Text(state.observacionError ?: "${state.observacion.length}/200")
                },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                androidx.compose.material3.OutlinedButton(onClick = onCancelar, modifier = Modifier.weight(1f)) {
                    Text("Cancelar")
                }
                Button(
                    onClick = viewModel::guardar,
                    enabled = !state.isSaving,
                    modifier = Modifier.weight(1f),
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}
