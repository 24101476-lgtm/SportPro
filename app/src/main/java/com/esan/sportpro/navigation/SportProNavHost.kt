package com.esan.sportpro.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.esan.sportpro.ui.cuentas.RegisterScreen
import com.esan.sportpro.ui.home.HomeScreen
import com.esan.sportpro.ui.login.LoginUsuarioScreen
import com.esan.sportpro.ui.login.RecuperarContrasenaScreen
import com.esan.sportpro.ui.login.SesionGateViewModel
import com.esan.sportpro.ui.registro.RegistroUsuarioScreen

/**
 * Grafo de navegación raíz de la app. Autenticación (US-001/US-002) vive fuera de [NavRoutes.Home];
 * una vez autenticado, [HomeScreen] resuelve internamente qué módulos mostrar según el rol.
 */
@Composable
fun SportProNavHost(navController: NavHostController = rememberNavController()) {
    val sesionGate: SesionGateViewModel = hiltViewModel()
    // US-002: si la app se abre con sesión activa se salta el login.
    val destinoInicial = remember {
        if (sesionGate.haySesionActiva()) NavRoutes.Home.route else NavRoutes.Login.route
    }
    NavHost(navController = navController, startDestination = destinoInicial) {
        composable(NavRoutes.Login.route) {
            LoginUsuarioScreen(
                onSesionIniciada = {
                    // US-003: hoy todos los roles entran por Home; el rol ya viene en el evento
                    // y ahí se decide la redirección definitiva.
                    navController.navigate(NavRoutes.Home.route) {
                        popUpTo(NavRoutes.Login.route) { inclusive = true }
                    }
                },
                // US-001: el botón de registro lleva al formulario con rol.
                onNavigateToRegister = { navController.navigate(NavRoutes.RegistroUsuario.route) },
                onNavigateToRecuperar = { navController.navigate(NavRoutes.RecuperarContrasena.route) },
            )
        }
        composable(NavRoutes.RecuperarContrasena.route) {
            RecuperarContrasenaScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(NavRoutes.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(NavRoutes.Home.route) {
                        popUpTo(NavRoutes.Login.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(NavRoutes.RegistroUsuario.route) {
            RegistroUsuarioScreen(
                // US-001: la cuenta se crea y se pide verificar el correo, así que se vuelve al login.
                onCuentaCreada = {
                    navController.navigate(NavRoutes.Login.route) {
                        popUpTo(NavRoutes.RegistroUsuario.route) { inclusive = true }
                    }
                },
            )
        }
        composable(NavRoutes.Home.route) {
            HomeScreen(
                onLogout = {
                    // US-002: cerrar la sesión de Firebase Auth, no solo navegar.
                    sesionGate.cerrarSesion {
                        navController.navigate(NavRoutes.Login.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                },
            )
        }
    }
}
