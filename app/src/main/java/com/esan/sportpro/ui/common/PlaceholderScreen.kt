package com.esan.sportpro.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Construction
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Pantalla de módulo aún no implementado, con la línea gráfica común. */
@Composable
fun PlaceholderScreen(
    title: String,
    relatedUserStories: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        EncabezadoModulo(titulo = title, subtitulo = relatedUserStories, icono = Icons.Default.Construction)
        EstadoVacioModulo(
            titulo = "Próximamente",
            mensaje = "Este módulo está en desarrollo.",
            icono = Icons.Default.Construction,
        )
    }
}
