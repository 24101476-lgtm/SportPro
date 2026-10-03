package com.esan.sportpro.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.esan.sportpro.ui.cuentas.LoginScreen
import com.esan.sportpro.ui.cuentas.RegisterScreen
import com.esan.sportpro.ui.home.HomeScreen
import com.esan.sportpro.ui.registro.RegistroUsuarioScreen

/**
 * Grafo de navegación raíz de la app. Autenticación (US-001/US-002) vive fuera de [NavRoutes.Home];
 * una vez autenticado, [HomeScreen] resuelve internamente qué módulos mostrar según el rol.
 */
@Composable
fun SportProNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = NavRoutes.Login.route) {
        composable(NavRoutes.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(NavRoutes.Home.route) {
                        popUpTo(NavRoutes.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(NavRoutes.Register.route) },
            )
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
                    navController.navigate(NavRoutes.Login.route) {
                        popUpTo(NavRoutes.Home.route) { inclusive = true }
                    }
                },
            )
        }
    }
}
