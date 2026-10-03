package com.esan.sportpro.ui.partido

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.esan.sportpro.domain.partido.Campos
import com.esan.sportpro.domain.partido.EquipoEvento
import com.esan.sportpro.domain.partido.JugadorConvocado
import com.esan.sportpro.domain.partido.JugadorEspecial
import com.esan.sportpro.domain.partido.ReglasPartido
import com.esan.sportpro.domain.partido.ResultadoPenal
import com.esan.sportpro.domain.partido.TiposEvento

/**
 * Hoja inferior que solicita solo los campos obligatorios del tipo de evento.
 * El minuto y el equipo propio vienen precargados para registrar en máximo tres toques.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventoBottomSheet(
    hoja: HojaEvento,
    state: RegistroUiState,
    viewModel: RegistroPartidoViewModel,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val partido = state.partido
    val tipo = hoja.tipo
    val esCambio = tipo.id == TiposEvento.CAMBIO
    val esGol = tipo.id == TiposEvento.GOL

    // Para una corrección, las listas se calculan sin el propio evento.
    val eventosBase = hoja.editando?.let { ed -> state.eventos.filter { it.grupoId != ed.grupoId } } ?: state.eventos
    val enCanchaIds = ReglasPartido.enCancha(state.convocados, eventosBase)
    val enCancha = state.convocados.filter { it.id in enCanchaIds }
    val suplentes = ReglasPartido.suplentesDisponibles(state.convocados, eventosBase)
    val propio = hoja.equipo != EquipoEvento.RIVAL

    var mostrarObservacion by remember(hoja.tipo.id, hoja.editando?.id) {
        mutableStateOf(hoja.observacion.isNotBlank())
    }

    ModalBottomSheet(onDismissRequest = viewModel::cerrarHoja, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                IconoEvento(tipo.id, 44.dp)
                androidx.compose.foundation.layout.Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (hoja.editando != null) "Corregir evento" else "Registrar evento",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(tipo.nombre, style = MaterialTheme.typography.titleLarge)
                }
            }

            OutlinedTextField(
                value = hoja.minutoTexto,
                onValueChange = viewModel::onMinuto,
                label = { Text("Minuto") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = { Text("Entre ${ReglasPartido.MINUTO_MIN} y ${ReglasPartido.MINUTO_MAX}") },
                modifier = Modifier.fillMaxWidth(),
            )

            if (tipo.requiere(Campos.EQUIPO)) {
                Seccion("Equipo") {
                    Chip(partido?.nombreEquipo ?: "Propio", hoja.equipo == EquipoEvento.PROPIO) {
                        viewModel.onEquipo(EquipoEvento.PROPIO)
                    }
                    Chip(partido?.nombreRival ?: "Rival", hoja.equipo == EquipoEvento.RIVAL) {
                        viewModel.onEquipo(EquipoEvento.RIVAL)
                    }
                }
            }

            if (tipo.requiere(Campos.JUGADOR) || tipo.requiere(Campos.JUGADOR_SALE)) {
                SelectorJugador(
                    titulo = if (esCambio) "Jugador que sale" else if (esGol) "Jugador anotador" else "Jugador",
                    // En un cambio solo puede salir alguien que está en cancha.
                    enCancha = if (propio) enCancha else emptyList(),
                    suplentes = if (propio && !esCambio) suplentes else emptyList(),
                    incluirRival = !esCambio || !propio,
                    seleccionado = hoja.jugadorId,
                    onSeleccionar = viewModel::onJugador,
                )
            }

            if (esCambio && tipo.requiere(Campos.JUGADOR_ENTRA)) {
                SelectorJugador(
                    titulo = "Jugador que entra",
                    enCancha = emptyList(),
                    suplentes = if (propio) suplentes else emptyList(),
                    incluirRival = !propio,
                    seleccionado = hoja.secundarioId,
                    onSeleccionar = { id, nombre -> viewModel.onSecundario(id, nombre) },
                )
            }

            if (esGol) {
                Seccion("Asistencia (opcional)") {
                    Chip("Sin asistencia", hoja.secundarioId == null) { viewModel.onSecundario(null, null) }
                    if (propio) {
                        enCancha.filter { it.id != hoja.jugadorId }.forEach { j ->
                            Chip(j.etiqueta, hoja.secundarioId == j.id) { viewModel.onSecundario(j.id, j.nombre) }
                        }
                    }
                }
            }

            if (tipo.requiere(Campos.RESULTADO)) {
                Seccion("Resultado") {
                    Chip("Convertido", hoja.resultado == ResultadoPenal.CONVERTIDO) {
                        viewModel.onResultado(ResultadoPenal.CONVERTIDO)
                    }
                    Chip("Fallado", hoja.resultado == ResultadoPenal.FALLADO) {
                        viewModel.onResultado(ResultadoPenal.FALLADO)
                    }
                }
            }

            if (mostrarObservacion) {
                OutlinedTextField(
                    value = hoja.observacion,
                    onValueChange = viewModel::onObservacion,
                    label = { Text("Observación (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                TextButton(onClick = { mostrarObservacion = true }) { Text("Agregar observación") }
            }

            if (hoja.editando != null) {
                OutlinedTextField(
                    value = hoja.motivo,
                    onValueChange = viewModel::onMotivo,
                    label = { Text("Motivo de la corrección") },
                    supportingText = {
                        Text("${hoja.motivo.trim().length}/${ReglasPartido.MOTIVO_MAX} · mínimo ${ReglasPartido.MOTIVO_MIN}")
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            hoja.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BotonSecundario("Cancelar", viewModel::cerrarHoja, Modifier.weight(1f))
                Button(
                    onClick = viewModel::guardarHoja,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = if (tipo.id == TiposEvento.TARJETA_AMARILLA) ColoresPartido.Verde else colorTipo(tipo.id),
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp),
                ) { Text(if (hoja.editando != null) "Guardar corrección" else "Guardar") }
            }
        }
    }
}

@Composable
private fun SelectorJugador(
    titulo: String,
    enCancha: List<JugadorConvocado>,
    suplentes: List<JugadorConvocado>,
    incluirRival: Boolean,
    seleccionado: String?,
    onSeleccionar: (String, String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(titulo, style = MaterialTheme.typography.bodyLarge)
        if (enCancha.isNotEmpty()) {
            Seccion("En cancha") {
                enCancha.forEach { j -> Chip(j.etiqueta, seleccionado == j.id) { onSeleccionar(j.id, j.nombre) } }
            }
        }
        if (suplentes.isNotEmpty()) {
            Seccion("Suplentes") {
                suplentes.forEach { j -> Chip(j.etiqueta, seleccionado == j.id) { onSeleccionar(j.id, j.nombre) } }
            }
        }
        Seccion("Otros") {
            if (incluirRival) {
                Chip(JugadorEspecial.NOMBRE_RIVAL, seleccionado == JugadorEspecial.RIVAL) {
                    onSeleccionar(JugadorEspecial.RIVAL, JugadorEspecial.NOMBRE_RIVAL)
                }
            }
            Chip(JugadorEspecial.NOMBRE_NO_IDENTIFICADO, seleccionado == JugadorEspecial.NO_IDENTIFICADO) {
                onSeleccionar(JugadorEspecial.NO_IDENTIFICADO, JugadorEspecial.NOMBRE_NO_IDENTIFICADO)
            }
        }
    }
}

/** Fila desplazable horizontalmente: mantiene la hoja corta para registrar sin hacer scroll. */
@Composable
private fun Seccion(titulo: String, contenido: @Composable () -> Unit) {
    Column {
        Text(titulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        ) { contenido() }
    }
}

@Composable
private fun Chip(texto: String, seleccionado: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = seleccionado,
        onClick = onClick,
        label = { Text(texto, fontWeight = if (seleccionado) androidx.compose.ui.text.font.FontWeight.Bold else null) },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
            selectedContainerColor = ColoresPartido.Verde,
            selectedLabelColor = androidx.compose.ui.graphics.Color.White,
        ),
        modifier = Modifier.heightIn(min = 48.dp),
    )
}
