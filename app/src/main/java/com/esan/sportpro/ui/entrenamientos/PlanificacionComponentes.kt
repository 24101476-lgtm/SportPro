package com.esan.sportpro.ui.entrenamientos

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.esan.sportpro.core.fecha.FechasSportPro
import com.esan.sportpro.domain.entrenamientos.Ejercicio
import com.esan.sportpro.domain.entrenamientos.ReglasPlanificacion
import com.esan.sportpro.ui.common.CampoFecha
import com.esan.sportpro.ui.common.CampoFormulario

private val ESPACIO_ENTRE_EJERCICIOS = 8.dp

/**
 * US-010, criterio 3: ejercicios de la sesión con su duración ajustable (solo en esta sesión) y
 * reordenables manteniendo presionado el ícono ⠿ y arrastrando. Las flechas hacen lo mismo para
 * quien prefiera tocar.
 */
@Composable
internal fun ListaEjerciciosReordenable(
    ejercicios: List<Ejercicio>,
    onMover: (desde: Int, hasta: Int) -> Unit,
    onAjustar: (indice: Int, deltaMinutos: Int) -> Unit,
    onQuitar: (indice: Int) -> Unit,
) {
    // Clave estable por ejercicio para que el elemento arrastrado conserve su gesto al moverse.
    val claves = remember(ejercicios) { clavesEstables(ejercicios) }
    val clavesActuales by rememberUpdatedState(claves)
    var arrastrando by remember { mutableStateOf<String?>(null) }
    var desplazamiento by remember { mutableFloatStateOf(0f) }
    val alturas = remember { mutableStateMapOf<String, Int>() }
    val espacioPx = with(LocalDensity.current) { ESPACIO_ENTRE_EJERCICIOS.toPx() }

    Column(verticalArrangement = Arrangement.spacedBy(ESPACIO_ENTRE_EJERCICIOS)) {
        claves.forEachIndexed { indice, clave ->
            key(clave) {
                val ejercicio = ejercicios[indice]
                val esteSeArrastra = arrastrando == clave
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { alturas[clave] = it.size.height }
                        .zIndex(if (esteSeArrastra) 1f else 0f)
                        .graphicsLayer { translationY = if (esteSeArrastra) desplazamiento else 0f },
                    colors = if (esteSeArrastra) {
                        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    } else {
                        CardDefaults.cardColors()
                    },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.DragIndicator,
                            contentDescription = "Mantén presionado y arrastra para reordenar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(8.dp)
                                .pointerInput(clave) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { arrastrando = clave; desplazamiento = 0f },
                                        onDragEnd = { arrastrando = null; desplazamiento = 0f },
                                        onDragCancel = { arrastrando = null; desplazamiento = 0f },
                                        onDrag = { cambio, delta ->
                                            cambio.consume()
                                            desplazamiento += delta.y
                                            val actual = clavesActuales.indexOf(clave)
                                            val salto = (alturas[clave] ?: 0) + espacioPx
                                            if (actual >= 0 && salto > 0f) {
                                                if (desplazamiento > salto / 2 && actual < clavesActuales.lastIndex) {
                                                    onMover(actual, actual + 1)
                                                    desplazamiento -= salto
                                                } else if (desplazamiento < -salto / 2 && actual > 0) {
                                                    onMover(actual, actual - 1)
                                                    desplazamiento += salto
                                                }
                                            }
                                        },
                                    )
                                },
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${indice + 1}. ${ejercicio.nombre}", style = MaterialTheme.typography.titleSmall)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { onAjustar(indice, -5) }) { Text("−5") }
                                Text("${ejercicio.duracionMin} min", style = MaterialTheme.typography.bodyMedium)
                                TextButton(onClick = { onAjustar(indice, 5) }) { Text("+5") }
                            }
                        }
                        Column {
                            IconButton(
                                onClick = { onMover(indice, indice - 1) },
                                enabled = indice > 0,
                                modifier = Modifier.size(36.dp),
                            ) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Subir") }
                            IconButton(
                                onClick = { onMover(indice, indice + 1) },
                                enabled = indice < ejercicios.lastIndex,
                                modifier = Modifier.size(36.dp),
                            ) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Bajar") }
                        }
                        IconButton(onClick = { onQuitar(indice) }) {
                            Icon(Icons.Default.Close, contentDescription = "Quitar ejercicio")
                        }
                    }
                }
            }
        }
    }
}

/** "Rondo", "Rondo", "Pase" -> "Rondo#0", "Rondo#1", "Pase#0". */
private fun clavesEstables(ejercicios: List<Ejercicio>): List<String> {
    val vistos = mutableMapOf<String, Int>()
    return ejercicios.map { ejercicio ->
        val base = ejercicio.ejercicioId.ifBlank { ejercicio.nombre }
        val n = vistos.getOrDefault(base, 0)
        vistos[base] = n + 1
        "$base#$n"
    }
}

/** Diálogos de US-010: sesión solapada (criterio 6), cancelar (9) y duplicar (8). */
@Composable
internal fun DialogosPlanificacion(state: EntrenamientosUiState, viewModel: EntrenamientosViewModel) {
    state.sesionSolapada?.let { otra ->
        AlertDialog(
            onDismissRequest = viewModel::cerrarAvisoSolapamiento,
            title = { Text("Ya existe una sesión programada en ese horario") },
            text = {
                Text(
                    "${otra.equipoNombre} ya tiene una sesión el ${otra.fecha} a las ${otra.hora} " +
                        "(${otra.duracionSesionMin} min): \"${otra.objetivo}\".",
                )
            },
            confirmButton = { TextButton(onClick = viewModel::continuarPeseASolapamiento) { Text("Continuar") } },
            dismissButton = { TextButton(onClick = viewModel::cerrarAvisoSolapamiento) { Text("Cancelar") } },
        )
    }

    state.dialogoCancelacion?.let { dialogo ->
        AlertDialog(
            onDismissRequest = viewModel::cerrarCancelacion,
            title = { Text("Cancelar sesión") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "La sesión del ${dialogo.entrenamiento.fecha} a las ${dialogo.entrenamiento.hora} quedará como " +
                            "Cancelada y seguirá visible en el historial.",
                    )
                    CampoFormulario(
                        valor = dialogo.motivo,
                        onCambio = viewModel::onMotivoCancelacion,
                        etiqueta = "Motivo",
                        error = dialogo.error,
                        ayuda = "${dialogo.motivo.trim().length}/${ReglasPlanificacion.MOTIVO_CANCELACION_MAX} " +
                            "(mínimo ${ReglasPlanificacion.MOTIVO_CANCELACION_MIN})",
                        minLineas = 3,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmarCancelacion, enabled = !state.procesando) {
                    Text("Cancelar sesión", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = viewModel::cerrarCancelacion) { Text("Volver") } },
        )
    }

    state.dialogoDuplicar?.let { dialogo ->
        AlertDialog(
            onDismissRequest = viewModel::cerrarDuplicar,
            title = { Text("Duplicar sesión") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Se copiarán el objetivo, la hora, el lugar y los ${dialogo.entrenamiento.ejercicios.size} " +
                            "ejercicios con sus duraciones.",
                    )
                    CampoFecha(
                        valorIso = dialogo.fecha,
                        onCambio = viewModel::onFechaDuplicado,
                        etiqueta = "Nueva fecha",
                        error = dialogo.error,
                        fechaMinima = FechasSportPro.hoy(),
                        fechaMaxima = FechasSportPro.hoy().plusYears(1),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmarDuplicar, enabled = !state.procesando) { Text("Duplicar") }
            },
            dismissButton = { TextButton(onClick = viewModel::cerrarDuplicar) { Text("Cancelar") } },
        )
    }
}
