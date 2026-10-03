package com.esan.sportpro.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups2
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.esan.sportpro.domain.cuentas.PerfilActual
import com.esan.sportpro.navigation.HomeModule
import com.esan.sportpro.navigation.UserRole
import com.esan.sportpro.ui.academia.AcademiaScreen
import com.esan.sportpro.ui.comunidad.ComunidadScreen
import com.esan.sportpro.ui.entrenamientos.EntrenamientosScreen
import com.esan.sportpro.ui.estadisticas.EstadisticasComunicacionEntryScreen
import com.esan.sportpro.ui.ia.IaScreen
import com.esan.sportpro.ui.jugadores.JugadoresScreen
import com.esan.sportpro.ui.mensualidades.MensualidadesEntryScreen
import com.esan.sportpro.ui.partido.PartidoScreen

/**
 * Punto de entrada tras el login (US-003: navegación principal diferenciada por rol).
 *
 * Lee el perfil del usuario (rol y academia) y muestra solo los módulos de
 * [HomeModule.visiblesPara] su rol. Cada módulo vuelve a validar el rol por su cuenta y
 * `firestore.rules` lo exige en el servidor: ocultar una pestaña no es la única barrera.
 *
 * `MENSUALIDADES` (US-008) es una pestaña propia — ver [MensualidadesEntryScreen]. `ESTADISTICAS`
 * (US-026, US-027) agrupa Estadísticas y Anuncios — ver [EstadisticasComunicacionEntryScreen].
 */
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val estado by viewModel.uiState.collectAsState()

    when (val actual = estado) {
        is HomeUiState.Cargando -> Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) { CircularProgressIndicator() }

        is HomeUiState.SinPerfil -> PerfilNoDisponible(
            onReintentar = viewModel::cargarPerfil,
            onLogout = onLogout,
        )

        is HomeUiState.Listo -> HomeConPerfil(perfil = actual.perfil, onLogout = onLogout)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeConPerfil(perfil: PerfilActual, onLogout: () -> Unit) {
    val modulos = HomeModule.visiblesPara(perfil.rol)
    var seleccionadoGuardado by rememberSaveable { mutableStateOf(modulos.first()) }
    // Si el rol cambió (otra cuenta en el mismo dispositivo) y el módulo guardado ya no es visible.
    val seleccionado = if (seleccionadoGuardado in modulos) seleccionadoGuardado else modulos.first()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SportPro · ${etiquetaRol(perfil.rol)}") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Cerrar sesión")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                modulos.forEach { modulo ->
                    NavigationBarItem(
                        selected = seleccionado == modulo,
                        onClick = { seleccionadoGuardado = modulo },
                        icon = { Icon(iconoDe(modulo), contentDescription = null) },
                        label = { Text(modulo.label) },
                        // Con más de 5 pestañas solo se rotula la seleccionada para que quepan.
                        alwaysShowLabel = modulos.size <= 5,
                    )
                }
            }
        },
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        when (seleccionado) {
            HomeModule.ACADEMIA -> AcademiaScreen(contentModifier)
            HomeModule.JUGADORES -> JugadoresScreen(contentModifier)
            HomeModule.ENTRENAMIENTOS -> EntrenamientosScreen(contentModifier)
            HomeModule.PARTIDO -> PartidoScreen(contentModifier)
            HomeModule.MENSUALIDADES -> MensualidadesEntryScreen(modifier = contentModifier, rolActual = perfil.rol)
            HomeModule.ESTADISTICAS -> EstadisticasComunicacionEntryScreen(modifier = contentModifier)
            HomeModule.COMUNIDAD -> ComunidadScreen(contentModifier)
            HomeModule.IA -> IaScreen(contentModifier)
        }
    }
}

/** El usuario autenticado no tiene perfil legible: se le deja reintentar o salir, nunca bloqueado. */
@Composable
private fun PerfilNoDisponible(onReintentar: () -> Unit, onLogout: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Icon(Icons.Default.Warning, contentDescription = null)
        Text("No pudimos cargar tu perfil. Revisa tu conexión e inténtalo de nuevo.")
        Button(onClick = onReintentar) { Text("Reintentar") }
        OutlinedButton(onClick = onLogout) { Text("Cerrar sesión") }
    }
}

private fun etiquetaRol(rol: UserRole): String = when (rol) {
    UserRole.ADMINISTRADOR -> "Administrador"
    UserRole.ENTRENADOR -> "Entrenador"
    UserRole.JUGADOR -> "Jugador"
    UserRole.PADRE_DE_FAMILIA -> "Padre de familia"
}

private fun iconoDe(modulo: HomeModule): ImageVector = when (modulo) {
    HomeModule.ACADEMIA -> Icons.Default.School
    HomeModule.JUGADORES -> Icons.Default.Group
    HomeModule.ENTRENAMIENTOS -> Icons.Default.Groups2
    HomeModule.PARTIDO -> Icons.Default.SportsSoccer
    HomeModule.MENSUALIDADES -> Icons.Default.Payments
    HomeModule.ESTADISTICAS -> Icons.Default.BarChart
    HomeModule.COMUNIDAD -> Icons.Default.Newspaper
    HomeModule.IA -> Icons.Default.Psychology
}
