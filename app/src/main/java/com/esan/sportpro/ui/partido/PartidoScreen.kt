package com.esan.sportpro.ui.partido

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

private const val RUTA_LISTA = "partidos"

private const val RUTA_CATALOGO = "catalogo"
private const val RUTA_CATALOGO_NUEVO = "catalogo/nuevo"
private const val RUTA_CATALOGO_EDITAR = "catalogo/editar/{eventoId}"

private const val RUTA_REGISTRO = "partido/{partidoId}"

private fun rutaRegistro(
    partidoId: String
) = "partido/$partidoId"

private fun rutaCatalogoEditar(
    eventoId: String
) = "catalogo/editar/$eventoId"

/**
 * Módulo Partido:
 * lista, catálogo de eventos y registro en vivo.
 */
@Composable
fun PartidoScreen(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = RUTA_LISTA,
        modifier = modifier,
    ) {

        // ------------------------------------------------ Lista de partidos

        composable(RUTA_LISTA) {

            PartidosListaScreen(
                onAbrirPartido = { id ->

                    navController.navigate(
                        rutaRegistro(id)
                    ) {
                        launchSingleTop = true
                    }
                },

                onAbrirCatalogo = {

                    navController.navigate(
                        RUTA_CATALOGO
                    ) {
                        launchSingleTop = true
                    }
                },
            )
        }

        // ------------------------------------------------ Catálogo

        composable(RUTA_CATALOGO) {

            CatalogoEventosScreen(
                onVolver = {
                    navController.popBackStack()
                },

                onNuevoEvento = {

                    navController.navigate(
                        RUTA_CATALOGO_NUEVO
                    ) {
                        launchSingleTop = true
                    }
                },

                onAbrirEvento = { eventoId ->

                    navController.navigate(
                        rutaCatalogoEditar(eventoId)
                    ) {
                        launchSingleTop = true
                    }
                },
            )
        }

        // ------------------------------------------------ Nuevo evento

        composable(RUTA_CATALOGO_NUEVO) {

            EventoCatalogoFormScreen(
                eventoId = null,

                onVolver = {
                    navController.popBackStack()
                },

                onGuardado = {
                    navController.popBackStack()
                },
            )
        }

        // ------------------------------------------------ Editar evento

        composable(
            route = RUTA_CATALOGO_EDITAR,
            arguments = listOf(
                navArgument("eventoId") {
                    type = NavType.StringType
                }
            ),
        ) { backStackEntry ->

            val eventoId =
                backStackEntry.arguments
                    ?.getString("eventoId")
                    .orEmpty()

            EventoCatalogoFormScreen(
                eventoId = eventoId,

                onVolver = {
                    navController.popBackStack()
                },

                onGuardado = {
                    navController.popBackStack()
                },
            )
        }

        // ------------------------------------------------ Registro en vivo

        composable(
            route = RUTA_REGISTRO,
            arguments = listOf(
                navArgument("partidoId") {
                    type = NavType.StringType
                }
            ),
        ) {

            RegistroPartidoScreen(
                onVolver = {
                    navController.popBackStack()
                }
            )
        }
    }
}