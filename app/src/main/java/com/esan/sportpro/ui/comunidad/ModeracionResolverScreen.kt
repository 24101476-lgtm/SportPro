package com.esan.sportpro.ui.comunidad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.comunidad.AccionModeracion
import com.esan.sportpro.domain.comunidad.DuracionSuspension
import kotlinx.coroutines.flow.collectLatest

/** US-030, criterios 5 y 6 — Frame 143_Moderacion_Resolver. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeracionResolverScreen(
    onResuelto: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ModeracionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val reporte = state.reporteSeleccionado
    var accion by remember { mutableStateOf(AccionModeracion.MANTENER_PUBLICADO) }
    var duracion by remember { mutableStateOf(DuracionSuspension.SIETE_DIAS) }
    var motivo by remember { mutableStateOf("") }
    var errorMensaje by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Resolver reporte") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.cerrarReporte(); onVolver() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        if (reporte == null) {
            Text("No hay un reporte seleccionado", modifier = Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Motivo del reporte: ${reporte.motivo.etiqueta}", style = MaterialTheme.typography.titleSmall)
            if (!reporte.detalle.isNullOrBlank()) Text(reporte.detalle, style = MaterialTheme.typography.bodySmall)

            Text("Acción", style = MaterialTheme.typography.labelLarge)
            AccionModeracion.entries.forEach { opcion ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = accion == opcion, onClick = { accion = opcion }),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = accion == opcion, onClick = { accion = opcion })
                    Text(opcion.etiqueta)
                }
            }

            if (accion == AccionModeracion.OCULTAR_Y_SUSPENDER) {
                Text("Duración de la suspensión", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DuracionSuspension.entries.forEach { opcion ->
                        FilterChip(selected = duracion == opcion, onClick = { duracion = opcion }, label = { Text(opcion.etiqueta) })
                    }
                }
            }

            OutlinedTextField(
                value = motivo,
                onValueChange = { motivo = it.take(300) },
                label = { Text("Motivo de la resolución") },
                supportingText = { Text("${motivo.length}/300 (mínimo 10)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            if (errorMensaje != null) {
                Text(errorMensaje.orEmpty(), color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    viewModel.resolver(accion, motivo, if (accion == AccionModeracion.OCULTAR_Y_SUSPENDER) duracion else null)
                },
                enabled = motivo.length in 10..300,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Resolver reporte") }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                ModeracionEvent.ReporteResuelto -> onResuelto()
                is ModeracionEvent.Error -> errorMensaje = event.mensaje
            }
        }
    }
}
