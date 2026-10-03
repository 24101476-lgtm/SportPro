package com.esan.sportpro.ui.comunidad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.esan.sportpro.domain.comunidad.Aviso
import com.esan.sportpro.domain.comunidad.PosicionJugador
import java.text.SimpleDateFormat
import java.util.Locale

/** US-029, criterios 7 y 9 — Frame 139_Aviso_Detalle. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvisoDetalleScreen(
    aviso: Aviso,
    onVolver: () -> Unit,
    onReportar: (avisoId: String, autorUid: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(aviso.titulo) },
                navigationIcon = {
                    IconButton(onClick = onVolver) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver") }
                },
                actions = {
                    IconButton(onClick = { onReportar(aviso.id, aviso.autorUid) }) {
                        Icon(Icons.Default.Flag, contentDescription = "Reportar convocatoria engañosa")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(aviso.academiaNombre, style = MaterialTheme.typography.titleSmall)
            aviso.fechaPublicacion?.let {
                Text(
                    "Publicado el ${SimpleDateFormat("d MMM yyyy", Locale("es", "PE")).format(it)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Detalle("Categoría", aviso.categoria)
            Detalle(
                "Posiciones buscadas",
                aviso.posicionesBuscadas.mapNotNull { nombre -> runCatching { PosicionJugador.valueOf(nombre) }.getOrNull()?.etiqueta }
                    .joinToString(", "),
            )
            aviso.fechaPrueba?.let { Detalle("Fecha de la prueba", SimpleDateFormat("d MMM yyyy", Locale("es", "PE")).format(it)) }
            Detalle("Hora", aviso.hora)
            Detalle("Ubicación", "${aviso.ubicacionDistrito} — ${aviso.ubicacionReferencia}")
            Detalle("Requisitos", aviso.requisitos)
            Detalle("Contacto institucional", aviso.contactoInstitucional)
        }
    }
}

@Composable
private fun Detalle(etiqueta: String, valor: String) {
    Column {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor.ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium)
    }
}
