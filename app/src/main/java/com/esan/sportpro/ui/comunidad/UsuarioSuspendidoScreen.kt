package com.esan.sportpro.ui.comunidad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.esan.sportpro.domain.comunidad.Suspension
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * US-030, criterio 9 — Frame 144_Usuario_Suspendido. Banner mostrado en la parte superior de la
 * sección Comunidad mientras el usuario tiene una suspensión activa; sigue pudiendo navegar y
 * consultar su información deportiva, pero no publicar, comentar ni reaccionar (las acciones
 * correspondientes deben ocultarse o deshabilitarse donde se muestren este banner).
 */
@Composable
fun UsuarioSuspendidoBanner(suspension: Suspension, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(Icons.Default.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Text(
                "Tu cuenta está suspendida de la comunidad",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(suspension.motivo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                suspension.fechaFin?.let {
                    Text(
                        "Podrás volver a publicar, comentar y reaccionar a partir del ${SimpleDateFormat("d MMM yyyy", Locale("es", "PE")).format(it)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
    }
}
