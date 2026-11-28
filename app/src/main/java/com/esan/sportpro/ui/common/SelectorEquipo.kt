package com.esan.sportpro.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Groups
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
import com.esan.sportpro.domain.academia.Equipo

/**
 * Combo para elegir un equipo. Ocupa una sola fila, a diferencia de una fila de chips que crece
 * con cada equipo de la academia.
 *
 * - Como filtro ([permitirTodos] = true) ofrece "Todos los equipos" y devuelve `null` al elegirlo.
 * - En un formulario ([permitirTodos] = false) el equipo es obligatorio: muestra [placeholder]
 *   mientras no se elija y marca el borde en rojo si [error] es true.
 */
@Composable
fun SelectorEquipo(
    equipos: List<Equipo>,
    seleccionadoId: String?,
    onSeleccionar: (String?) -> Unit,
    modifier: Modifier = Modifier,
    textoTodos: String = "Todos los equipos",
    permitirTodos: Boolean = true,
    placeholder: String = "Elige un equipo",
    error: Boolean = false,
    habilitado: Boolean = true,
) {
    var abierto by remember { mutableStateOf(false) }
    val actual = equipos.firstOrNull { it.id == seleccionadoId }

    Box(modifier = modifier) {
        Surface(
            onClick = { abierto = true },
            enabled = habilitado && equipos.isNotEmpty(),
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
                modifier = Modifier.padding(start = 10.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            ) {
                if (actual != null) {
                    EscudoEquipo(nombre = actual.nombre, tamano = 24.dp, url = actual.escudoUrl.ifBlank { null }, color = actual.colorUniforme)
                } else {
                    Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    actual?.texto() ?: if (permitirTodos) textoTodos else placeholder,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (actual != null || permitirTodos) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (actual == null && !permitirTodos) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Elegir equipo")
            }
        }

        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            if (permitirTodos) {
                OpcionEquipo(
                    texto = textoTodos,
                    elegida = seleccionadoId == null,
                    icono = { Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(22.dp)) },
                ) {
                    abierto = false
                    onSeleccionar(null)
                }
            }
            equipos.forEach { equipo ->
                OpcionEquipo(
                    texto = equipo.texto(),
                    elegida = seleccionadoId == equipo.id,
                    icono = { EscudoEquipo(nombre = equipo.nombre, tamano = 24.dp, url = equipo.escudoUrl.ifBlank { null }, color = equipo.colorUniforme) },
                ) {
                    abierto = false
                    onSeleccionar(equipo.id)
                }
            }
        }
    }
}

@Composable
private fun OpcionEquipo(
    texto: String,
    elegida: Boolean,
    icono: @Composable () -> Unit,
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
            { Icon(Icons.Default.Check, contentDescription = "Elegido", tint = MaterialTheme.colorScheme.primary) }
        } else {
            null
        },
        onClick = onClick,
    )
}

/** "Nombre · Categoría", o solo el nombre si el equipo no trae categoría. */
private fun Equipo.texto(): String = if (categoria.isBlank()) nombre else etiqueta
