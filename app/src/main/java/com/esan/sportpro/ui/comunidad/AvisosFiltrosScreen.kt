package com.esan.sportpro.ui.comunidad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.esan.sportpro.domain.comunidad.FiltroAvisos
import com.esan.sportpro.domain.comunidad.PosicionJugador

/** US-029, criterio 6 — Frame 137_Avisos_Filtros, mostrado como hoja modal sobre el buscador. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AvisosFiltrosScreen(filtroActual: FiltroAvisos, onAplicar: (FiltroAvisos) -> Unit, onCerrar: () -> Unit) {
    var categoria by remember { mutableStateOf(filtroActual.categoria.orEmpty()) }
    var posicion by remember { mutableStateOf(filtroActual.posicion) }
    var distrito by remember { mutableStateOf(filtroActual.distrito.orEmpty()) }

    ModalBottomSheet(onDismissRequest = onCerrar) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Filtrar avisos", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = categoria,
                onValueChange = { categoria = it },
                label = { Text("Categoría") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = distrito,
                onValueChange = { distrito = it },
                label = { Text("Distrito") },
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Posición", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PosicionJugador.entries.forEach { opcion ->
                    FilterChip(
                        selected = posicion == opcion,
                        onClick = { posicion = if (posicion == opcion) null else opcion },
                        label = { Text(opcion.etiqueta) },
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { categoria = ""; posicion = null; distrito = "" }) { Text("Limpiar") }
                Button(
                    onClick = {
                        onAplicar(
                            filtroActual.copy(
                                categoria = categoria.ifBlank { null },
                                posicion = posicion,
                                distrito = distrito.ifBlank { null },
                            ),
                        )
                    },
                ) { Text("Aplicar filtros") }
            }
        }
    }
}
