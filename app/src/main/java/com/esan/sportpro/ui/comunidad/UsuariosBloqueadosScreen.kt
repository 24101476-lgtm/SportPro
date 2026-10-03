package com.esan.sportpro.ui.comunidad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.comunidad.Bloqueo
import com.esan.sportpro.ui.common.EmptyStateMessage

/** US-030, criterio 8 — Frame 145_Usuarios_Bloqueados. */
@Composable
fun UsuariosBloqueadosScreen(modifier: Modifier = Modifier, viewModel: ModeracionViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    if (state.bloqueados.isEmpty()) {
        EmptyStateMessage(mensaje = "No has bloqueado a ningún usuario", icono = Icons.Default.PersonOff, modifier = modifier)
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        items(state.bloqueados, key = { it.id }) { bloqueo ->
            BloqueoRow(bloqueo = bloqueo, onDesbloquear = { viewModel.desbloquear(bloqueo.uidBloqueado) })
        }
    }
}

// TODO(US-003/cuentas): mostrar el nombre del usuario bloqueado en vez de su uid, una vez que
// exista un repositorio de perfiles públicos para resolverlo.
@Composable
private fun BloqueoRow(bloqueo: Bloqueo, onDesbloquear: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(bloqueo.uidBloqueado, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onDesbloquear) { Text("Desbloquear") }
        }
    }
}
