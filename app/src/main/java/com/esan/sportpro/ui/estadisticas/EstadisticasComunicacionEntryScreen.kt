package com.esan.sportpro.ui.estadisticas

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.esan.sportpro.ui.anuncios.AnunciosSeccionScreen

/**
 * Punto de entrada de la pestaña "Estadísticas" del Home (agrupa US-026 y US-027, Página 12 del
 * backlog: "Estadísticas y comunicación"). Internamente alterna entre la sección de Estadísticas
 * (US-026) y la de Anuncios (US-027) con un [TabRow], cada una con su propia navegación interna
 * y su propio ViewModel.
 */
@Composable
fun EstadisticasComunicacionEntryScreen(modifier: Modifier = Modifier) {
    var seccionSeleccionada by rememberSaveable { mutableIntStateOf(0) }
    val titulos = listOf("Estadísticas", "Anuncios")

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = seccionSeleccionada) {
            titulos.forEachIndexed { index, titulo ->
                Tab(
                    selected = seccionSeleccionada == index,
                    onClick = { seccionSeleccionada = index },
                    text = { Text(titulo) },
                )
            }
        }
        when (seccionSeleccionada) {
            0 -> EstadisticasSeccionScreen(modifier = Modifier.weight(1f))
            else -> AnunciosSeccionScreen(modifier = Modifier.weight(1f))
        }
    }
}
