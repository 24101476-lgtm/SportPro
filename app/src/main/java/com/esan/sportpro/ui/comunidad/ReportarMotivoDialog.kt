package com.esan.sportpro.ui.comunidad

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.esan.sportpro.domain.comunidad.MotivoReporte

/** US-030, criterio 1 — Frame 141_Reportar_Motivo. */
@Composable
fun ReportarMotivoDialog(onConfirmar: (MotivoReporte, String?) -> Unit, onCancelar: () -> Unit) {
    var motivo by remember { mutableStateOf(MotivoReporte.CONTENIDO_OFENSIVO) }
    var detalle by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Reportar contenido") },
        text = {
            Column {
                MotivoReporte.entries.forEach { opcion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = motivo == opcion, onClick = { motivo = opcion }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = motivo == opcion, onClick = { motivo = opcion })
                        Text(opcion.etiqueta, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                OutlinedTextField(
                    value = detalle,
                    onValueChange = { detalle = it.take(300) },
                    label = { Text("Detalle (opcional)") },
                    supportingText = { Text("${detalle.length}/300") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirmar(motivo, detalle.ifBlank { null }) }) { Text("Enviar reporte") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    )
}
