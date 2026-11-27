package com.esan.sportpro.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Combo genérico para listas largas (posiciones, categorías, parentescos...). Se ve igual que
 * [SelectorEquipo]: una píldora de una sola fila que abre un menú.
 *
 * - [opcionVacia] agrega una primera opción que devuelve `null` ("Ninguna", "Todas"...). Sin ella,
 *   la elección es obligatoria y se muestra [placeholder] mientras no haya una.
 * - [icono] dibuja algo a la izquierda de cada opción (un punto de color, una abreviatura...);
 *   recibe `null` para la opción vacía.
 */
@Composable
fun <T> SelectorOpciones(
    opciones: List<T>,
    seleccionada: T?,
    texto: (T) -> String,
    onSeleccionar: (T?) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Elige una opción",
    opcionVacia: String? = null,
    error: Boolean = false,
    habilitado: Boolean = true,
    icono: (@Composable (T?) -> Unit)? = null,
) {
    var abierto by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            onClick = { abierto = true },
            enabled = habilitado && opciones.isNotEmpty(),
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                if (error) 2.dp else 1.dp,
                if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            ) {
                val vacia = seleccionada == null
                if (icono != null && !(vacia && opcionVacia == null)) {
                    icono(seleccionada)
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    seleccionada?.let(texto) ?: opcionVacia ?: placeholder,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (!vacia || opcionVacia != null) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (vacia && opcionVacia == null) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Abrir opciones")
            }
        }

        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            if (opcionVacia != null) {
                Opcion(opcionVacia, seleccionada == null, icono?.let { { it(null) } }) {
                    abierto = false
                    onSeleccionar(null)
                }
            }
            opciones.forEach { opcion ->
                Opcion(texto(opcion), seleccionada == opcion, icono?.let { { it(opcion) } }) {
                    abierto = false
                    onSeleccionar(opcion)
                }
            }
        }
    }
}

@Composable
private fun Opcion(
    texto: String,
    elegida: Boolean,
    icono: (@Composable () -> Unit)?,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(
                texto,
                fontWeight = if (elegida) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingIcon = icono,
        trailingIcon = if (elegida) {
            { Icon(Icons.Default.Check, contentDescription = "Elegida", tint = MaterialTheme.colorScheme.primary) }
        } else {
            null
        },
        onClick = onClick,
    )
}
