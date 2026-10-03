package com.esan.sportpro.ui.estadisticas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.estadisticas.FiltroEstadisticas
import com.esan.sportpro.domain.estadisticas.TipoPartido
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** US-026, criterio 4 — Frame 124_Estadisticas_Filtros. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadisticasFiltrosScreen(
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EstadisticasViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var temporada by remember(state.filtro) { mutableStateOf(state.filtro.temporada) }
    var tipoPartido by remember(state.filtro) { mutableStateOf(state.filtro.tipoPartido) }
    var fechaInicioTexto by remember(state.filtro) { mutableStateOf(state.filtro.fechaInicio?.format(formatoFecha) ?: "") }
    var fechaFinTexto by remember(state.filtro) { mutableStateOf(state.filtro.fechaFin?.format(formatoFecha) ?: "") }
    var menuTemporadaAbierto by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Filtros") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Temporada", style = MaterialTheme.typography.labelLarge)
            Column {
                OutlinedButton(onClick = { menuTemporadaAbierto = true }, modifier = Modifier.wrapContentWidth()) {
                    Text(temporada ?: "Todas las temporadas")
                }
                DropdownMenu(expanded = menuTemporadaAbierto, onDismissRequest = { menuTemporadaAbierto = false }) {
                    DropdownMenuItem(text = { Text("Todas las temporadas") }, onClick = { temporada = null; menuTemporadaAbierto = false })
                    state.temporadasDisponibles.forEach { t ->
                        DropdownMenuItem(text = { Text(t) }, onClick = { temporada = t; menuTemporadaAbierto = false })
                    }
                }
            }

            Text("Tipo de partido", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TipoPartido.entries.forEach { tipo ->
                    FilterChip(
                        selected = tipoPartido == tipo,
                        onClick = { tipoPartido = if (tipoPartido == tipo) null else tipo },
                        label = { Text(tipo.etiqueta) },
                    )
                }
            }

            Text("Rango de fechas", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = fechaInicioTexto,
                onValueChange = { fechaInicioTexto = it },
                label = { Text("Desde (aaaa-mm-dd)") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = fechaFinTexto,
                onValueChange = { fechaFinTexto = it },
                label = { Text("Hasta (aaaa-mm-dd)") },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = {
                    temporada = null
                    tipoPartido = null
                    fechaInicioTexto = ""
                    fechaFinTexto = ""
                }) { Text("Limpiar") }

                Button(onClick = {
                    viewModel.aplicarFiltro(
                        FiltroEstadisticas(
                            temporada = temporada,
                            tipoPartido = tipoPartido,
                            fechaInicio = fechaInicioTexto.toLocalDateOrNull(),
                            fechaFin = fechaFinTexto.toLocalDateOrNull(),
                        ),
                    )
                    onVolver()
                }) { Text("Aplicar filtros") }
            }
        }
    }
}

private val formatoFecha: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this, formatoFecha) }.getOrNull()
