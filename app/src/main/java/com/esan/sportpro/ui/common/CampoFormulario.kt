package com.esan.sportpro.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

/** Campo de texto de formulario con etiqueta, mensaje de error y teclado configurable. */
@Composable
fun CampoFormulario(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    ayuda: String? = null,
    teclado: KeyboardType = KeyboardType.Text,
    minLineas: Int = 1,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        isError = error != null,
        supportingText = {
            val texto = error ?: ayuda
            if (texto != null) Text(texto)
        },
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        singleLine = minLineas == 1,
        minLines = minLineas,
        modifier = modifier.fillMaxWidth(),
    )
}
