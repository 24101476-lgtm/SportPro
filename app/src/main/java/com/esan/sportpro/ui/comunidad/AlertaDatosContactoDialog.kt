package com.esan.sportpro.ui.comunidad

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * US-028, criterio 7 — Frame 135_Alerta_Datos_Contacto. Se muestra antes de publicar cuando el
 * texto contiene un patrón de número telefónico de 9 dígitos.
 */
@Composable
fun AlertaDatosContactoDialog(onConfirmar: () -> Unit, onCancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("No compartas números de contacto en la comunidad") },
        text = { Text("Tu publicación parece incluir un número de teléfono. Por seguridad, evita compartir datos de contacto personales en publicaciones públicas. ¿Deseas publicarla de todas formas?") },
        confirmButton = { TextButton(onClick = onConfirmar) { Text("Publicar de todas formas") } },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Editar texto") } },
    )
}
