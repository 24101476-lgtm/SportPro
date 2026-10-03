package com.esan.sportpro.ui.partido

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

private const val RUTA_LISTA = "partidos"
private const val RUTA_REGISTRO = "partido/{partidoId}"

private fun rutaRegistro(partidoId: String) = "partido/$partidoId"

/** Módulo Partido: lista de partidos y registro en vivo de cada uno. */
@Composable
fun PartidoScreen(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = RUTA_LISTA, modifier = modifier) {
        composable(RUTA_LISTA) {
            PartidosListaScreen(
                onAbrirPartido = { id ->
                    navController.navigate(rutaRegistro(id)) { launchSingleTop = true }
                },
            )
        }
        composable(
            route = RUTA_REGISTRO,
            arguments = listOf(navArgument("partidoId") { type = NavType.StringType }),
        ) {
            RegistroPartidoScreen(onVolver = { navController.popBackStack() })
        }
    }
}
