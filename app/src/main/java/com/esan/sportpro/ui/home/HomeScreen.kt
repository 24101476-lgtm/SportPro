package com.esan.sportpro.ui.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups2
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.esan.sportpro.navigation.HomeModule
import com.esan.sportpro.ui.academia.AcademiaScreen
import com.esan.sportpro.ui.comunidad.ComunidadScreen
import com.esan.sportpro.ui.entrenamientos.EntrenamientosScreen
import com.esan.sportpro.ui.estadisticas.EstadisticasComunicacionEntryScreen
import com.esan.sportpro.ui.ia.IaScreen
import com.esan.sportpro.ui.jugadores.JugadoresScreen
import com.esan.sportpro.ui.partido.PartidoScreen

/**
 * Punto de entrada tras el login (US-003: navegación principal diferenciada por rol).
 * Por ahora los módulos son visibles para todos los roles; cada equipo puede restringir
 * los items de [HomeModule] según [com.esan.sportpro.navigation.UserRole] cuando el perfil
 * del usuario autenticado esté disponible (módulo `cuentas`).
 *
 * `ESTADISTICAS` (US-026, US-027) se agregó como pestaña propia que agrupa Estadísticas y
 * Anuncios — ver [EstadisticasComunicacionEntryScreen].
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onLogout: () -> Unit) {
    var selectedModule by remember { mutableStateOf(HomeModule.ACADEMIA) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SportPro") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Cerrar sesión")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedModule == HomeModule.ACADEMIA,
                    onClick = { selectedModule = HomeModule.ACADEMIA },
                    icon = { Icon(Icons.Default.School, contentDescription = null) },
                    label = { Text(HomeModule.ACADEMIA.label) },
                )
                NavigationBarItem(
                    selected = selectedModule == HomeModule.JUGADORES,
                    onClick = { selectedModule = HomeModule.JUGADORES },
                    icon = { Icon(Icons.Default.Group, contentDescription = null) },
                    label = { Text(HomeModule.JUGADORES.label) },
                )
                NavigationBarItem(
                    selected = selectedModule == HomeModule.ENTRENAMIENTOS,
                    onClick = { selectedModule = HomeModule.ENTRENAMIENTOS },
                    icon = { Icon(Icons.Default.Groups2, contentDescription = null) },
                    label = { Text(HomeModule.ENTRENAMIENTOS.label) },
                )
                NavigationBarItem(
                    selected = selectedModule == HomeModule.PARTIDO,
                    onClick = { selectedModule = HomeModule.PARTIDO },
                    icon = { Icon(Icons.Default.SportsSoccer, contentDescription = null) },
                    label = { Text(HomeModule.PARTIDO.label) },
                )
                NavigationBarItem(
                    selected = selectedModule == HomeModule.ESTADISTICAS,
                    onClick = { selectedModule = HomeModule.ESTADISTICAS },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                    label = { Text(HomeModule.ESTADISTICAS.label) },
                )
                NavigationBarItem(
                    selected = selectedModule == HomeModule.COMUNIDAD,
                    onClick = { selectedModule = HomeModule.COMUNIDAD },
                    icon = { Icon(Icons.Default.Newspaper, contentDescription = null) },
                    label = { Text(HomeModule.COMUNIDAD.label) },
                )
                NavigationBarItem(
                    selected = selectedModule == HomeModule.IA,
                    onClick = { selectedModule = HomeModule.IA },
                    icon = { Icon(Icons.Default.Psychology, contentDescription = null) },
                    label = { Text(HomeModule.IA.label) },
                )
            }
        },
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        when (selectedModule) {
            HomeModule.ACADEMIA -> AcademiaScreen(contentModifier)
            HomeModule.JUGADORES -> JugadoresScreen(contentModifier)
            HomeModule.ENTRENAMIENTOS -> EntrenamientosScreen(contentModifier)
            HomeModule.PARTIDO -> PartidoScreen(contentModifier)
            HomeModule.ESTADISTICAS -> EstadisticasComunicacionEntryScreen(modifier = contentModifier)
            HomeModule.COMUNIDAD -> ComunidadScreen(contentModifier)
            HomeModule.IA -> IaScreen(contentModifier)
        }
    }
}
