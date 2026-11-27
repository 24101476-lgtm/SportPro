package com.esan.sportpro.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Mensaje exacto del criterio de US-003. */
const val MENSAJE_SIN_PERMISO = "No tienes permiso para ver esta sección"

/**
 * US-003: se muestra cuando se intenta abrir (por ejemplo, con un deep link) una sección que el
 * rol del usuario no tiene habilitada.
 */
@Composable
fun SinPermisoScreen(onVolverAlInicio: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        IconoCircular(Icons.Default.Lock)
        Text(
            MENSAJE_SIN_PERMISO,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            "Esta sección no está disponible para tu rol. Si crees que deberías verla, consulta con el administrador de tu academia.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        BotonPrincipalSportPro(texto = "Volver al inicio", onClick = onVolverAlInicio, icono = Icons.Default.Home)
    }
}
