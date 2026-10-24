package com.esan.sportpro.ui.partido

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument


// ============================================================
// RUTAS
// ============================================================

private const val RUTA_LISTA =
    "partidos"

private const val RUTA_PARTIDO_NUEVO =
    "partidos/nuevo"

private const val RUTA_CONVOCATORIA =
    "partidos/{partidoId}/convocatoria"

private const val RUTA_SEGUIMIENTO_CONVOCATORIA =
    "partidos/{partidoId}/convocatoria/seguimiento"

private const val RUTA_GESTION_PARTIDO =
    "partidos/{partidoId}/gestion"


// ------------------------------------------------------------
// US-015 - RESPUESTA DEL JUGADOR
// ------------------------------------------------------------

private const val RUTA_CONVOCATORIA_JUGADOR =
    "partidos/{partidoId}/convocatoria/jugador/{jugadorId}"

private const val RUTA_CONVOCATORIA_NO_DISPONIBLE =
    "partidos/{partidoId}/convocatoria/jugador/{jugadorId}/no-disponible"

private const val RUTA_CONVOCATORIA_RESPUESTA =
    "partidos/{partidoId}/convocatoria/jugador/{jugadorId}/respuesta"

private const val RUTA_CONVOCATORIA_VENCIDA =
    "partidos/{partidoId}/convocatoria/jugador/{jugadorId}/vencida"


// ------------------------------------------------------------
// US-013 - CATÁLOGO
// ------------------------------------------------------------

private const val RUTA_CATALOGO =
    "catalogo"

private const val RUTA_CATALOGO_NUEVO =
    "catalogo/nuevo"

private const val RUTA_CATALOGO_EDITAR =
    "catalogo/editar/{eventoId}"


// ------------------------------------------------------------
// REGISTRO EN VIVO
// ------------------------------------------------------------

private const val RUTA_REGISTRO =
    "partido/{partidoId}"


// ============================================================
// RUTAS DINÁMICAS
// ============================================================

private fun rutaRegistro(
    partidoId: String
): String =
    "partido/$partidoId"


private fun rutaCatalogoEditar(
    eventoId: String
): String =
    "catalogo/editar/$eventoId"


private fun rutaConvocatoria(
    partidoId: String
): String =
    "partidos/$partidoId/convocatoria"


private fun rutaSeguimientoConvocatoria(
    partidoId: String
): String =
    "partidos/$partidoId/convocatoria/seguimiento"


private fun rutaGestionPartido(
    partidoId: String
): String =
    "partidos/$partidoId/gestion"


// ------------------------------------------------------------
// US-015
// ------------------------------------------------------------

private fun rutaConvocatoriaJugador(
    partidoId: String,
    jugadorId: String,
): String =
    "partidos/$partidoId/convocatoria/jugador/$jugadorId"


private fun rutaConvocatoriaNoDisponible(
    partidoId: String,
    jugadorId: String,
): String =
    "partidos/$partidoId/convocatoria/jugador/$jugadorId/no-disponible"


private fun rutaConvocatoriaRespuesta(
    partidoId: String,
    jugadorId: String,
): String =
    "partidos/$partidoId/convocatoria/jugador/$jugadorId/respuesta"


private fun rutaConvocatoriaVencida(
    partidoId: String,
    jugadorId: String,
): String =
    "partidos/$partidoId/convocatoria/jugador/$jugadorId/vencida"


// ============================================================
// MÓDULO PARTIDO
// ============================================================

@Composable
fun PartidoScreen(
    modifier: Modifier = Modifier
) {

    val navController =
        rememberNavController()

    NavHost(
        navController = navController,
        startDestination = RUTA_LISTA,
        modifier = modifier,
    ) {


        // ====================================================
        // 63 - LISTA DE PARTIDOS
        // ====================================================

        composable(
            route = RUTA_LISTA
        ) {

            PartidosListaScreen(

                onAbrirPartido = { partidoId ->

                    navController.navigate(
                        rutaRegistro(
                            partidoId
                        )
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

                onNuevoPartido = {

                    navController.navigate(
                        RUTA_PARTIDO_NUEVO
                    ) {
                        launchSingleTop = true
                    }
                },
            )
        }


        // ====================================================
        // 64 - PROGRAMAR PARTIDO
        // ====================================================

        composable(
            route = RUTA_PARTIDO_NUEVO
        ) {

            PartidoProgramarScreen(

                onVolver = {
                    navController.popBackStack()
                },

                onPartidoCreado = { partidoId ->

                    navController.navigate(
                        rutaConvocatoria(
                            partidoId
                        )
                    ) {
                        launchSingleTop = true
                    }
                },
            )
        }


        // ====================================================
        // 65 - SELECCIONAR CONVOCADOS
        // ====================================================

        composable(
            route = RUTA_CONVOCATORIA,

            arguments = listOf(

                navArgument(
                    "partidoId"
                ) {
                    type =
                        NavType.StringType
                }
            )
        ) { backStackEntry ->

            val partidoId =
                backStackEntry
                    .arguments
                    ?.getString(
                        "partidoId"
                    )
                    .orEmpty()

            ConvocatoriaSeleccionScreen(

                onVolver = {
                    navController.popBackStack()
                },

                onConvocatoriaPublicada = {

                    navController.navigate(
                        rutaSeguimientoConvocatoria(
                            partidoId
                        )
                    ) {
                        launchSingleTop = true
                    }
                },
            )
        }


        // ====================================================
        // 66 - SEGUIMIENTO DE CONVOCATORIA
        // ====================================================

        composable(
            route =
                RUTA_SEGUIMIENTO_CONVOCATORIA,

            arguments = listOf(

                navArgument(
                    "partidoId"
                ) {
                    type =
                        NavType.StringType
                }
            )
        ) { backStackEntry ->

            val partidoId =
                backStackEntry
                    .arguments
                    ?.getString(
                        "partidoId"
                    )
                    .orEmpty()

            ConvocatoriaSeguimientoScreen(

                onVolver = {
                    navController.popBackStack()
                },

                onGestionarPartido = {

                    navController.navigate(
                        rutaGestionPartido(
                            partidoId
                        )
                    ) {
                        launchSingleTop = true
                    }
                },
            )
        }


        // ====================================================
        // 67 - REPROGRAMAR / CANCELAR PARTIDO
        // ====================================================

        composable(
            route =
                RUTA_GESTION_PARTIDO,

            arguments = listOf(

                navArgument(
                    "partidoId"
                ) {
                    type =
                        NavType.StringType
                }
            )
        ) {

            PartidoGestionScreen(

                onVolver = {
                    navController.popBackStack()
                },

                onFinalizado = {
                    navController.popBackStack()
                },
            )
        }


        // ====================================================
        // 68 - DETALLE DE CONVOCATORIA DEL JUGADOR
        // ====================================================

        composable(
            route =
                RUTA_CONVOCATORIA_JUGADOR,

            arguments = listOf(

                navArgument(
                    "partidoId"
                ) {
                    type =
                        NavType.StringType
                },

                navArgument(
                    "jugadorId"
                ) {
                    type =
                        NavType.StringType
                },
            )
        ) { backStackEntry ->

            val partidoId =
                backStackEntry
                    .arguments
                    ?.getString(
                        "partidoId"
                    )
                    .orEmpty()

            val jugadorId =
                backStackEntry
                    .arguments
                    ?.getString(
                        "jugadorId"
                    )
                    .orEmpty()

            ConvocatoriaDetalleJugadorScreen(

                onVolver = {
                    navController.popBackStack()
                },

                onNoDisponible = {

                    navController.navigate(
                        rutaConvocatoriaNoDisponible(
                            partidoId = partidoId,
                            jugadorId = jugadorId,
                        )
                    ) {
                        launchSingleTop = true
                    }
                },

                onRespuestaGuardada = {

                    navController.navigate(
                        rutaConvocatoriaRespuesta(
                            partidoId = partidoId,
                            jugadorId = jugadorId,
                        )
                    ) {
                        launchSingleTop = true
                    }
                },

                onConvocatoriaVencida = {

                    navController.navigate(
                        rutaConvocatoriaVencida(
                            partidoId = partidoId,
                            jugadorId = jugadorId,
                        )
                    ) {
                        launchSingleTop = true
                    }
                },
            )
        }


        // ====================================================
        // 69 - MOTIVO DE NO DISPONIBILIDAD
        // ====================================================

        composable(
            route =
                RUTA_CONVOCATORIA_NO_DISPONIBLE,

            arguments = listOf(

                navArgument(
                    "partidoId"
                ) {
                    type =
                        NavType.StringType
                },

                navArgument(
                    "jugadorId"
                ) {
                    type =
                        NavType.StringType
                },
            )
        ) { backStackEntry ->

            val partidoId =
                backStackEntry
                    .arguments
                    ?.getString(
                        "partidoId"
                    )
                    .orEmpty()

            val jugadorId =
                backStackEntry
                    .arguments
                    ?.getString(
                        "jugadorId"
                    )
                    .orEmpty()

            ConvocatoriaNoDisponibleScreen(

                onVolver = {
                    navController.popBackStack()
                },

                onRespuestaGuardada = {

                    navController.navigate(
                        rutaConvocatoriaRespuesta(
                            partidoId = partidoId,
                            jugadorId = jugadorId,
                        )
                    ) {
                        launchSingleTop = true
                    }
                },

                onConvocatoriaVencida = {

                    navController.navigate(
                        rutaConvocatoriaVencida(
                            partidoId = partidoId,
                            jugadorId = jugadorId,
                        )
                    ) {
                        launchSingleTop = true
                    }
                },
            )
        }


        // ====================================================
        // 70 - RESPUESTA REGISTRADA
        // ====================================================

        composable(
            route =
                RUTA_CONVOCATORIA_RESPUESTA,

            arguments = listOf(

                navArgument(
                    "partidoId"
                ) {
                    type =
                        NavType.StringType
                },

                navArgument(
                    "jugadorId"
                ) {
                    type =
                        NavType.StringType
                },
            )
        ) {

            ConvocatoriaRespuestaRegistradaScreen(

                onFinalizar = {

                    navController.navigate(
                        RUTA_LISTA
                    ) {

                        popUpTo(
                            RUTA_LISTA
                        ) {
                            inclusive = false
                        }

                        launchSingleTop = true
                    }
                },
            )
        }


        // ====================================================
        // 71 - CONVOCATORIA VENCIDA
        // ====================================================

        composable(
            route =
                RUTA_CONVOCATORIA_VENCIDA,

            arguments = listOf(

                navArgument(
                    "partidoId"
                ) {
                    type =
                        NavType.StringType
                },

                navArgument(
                    "jugadorId"
                ) {
                    type =
                        NavType.StringType
                },
            )
        ) {

            ConvocatoriaVencidaScreen(

                onFinalizar = {

                    navController.navigate(
                        RUTA_LISTA
                    ) {

                        popUpTo(
                            RUTA_LISTA
                        ) {
                            inclusive = false
                        }

                        launchSingleTop = true
                    }
                },
            )
        }


        // ====================================================
        // US-013 - CATÁLOGO DE EVENTOS
        // ====================================================

        composable(
            route =
                RUTA_CATALOGO
        ) {

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
                        rutaCatalogoEditar(
                            eventoId
                        )
                    ) {
                        launchSingleTop = true
                    }
                },
            )
        }


        // ====================================================
        // US-013 - NUEVO EVENTO
        // ====================================================

        composable(
            route =
                RUTA_CATALOGO_NUEVO
        ) {

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


        // ====================================================
        // US-013 - EDITAR EVENTO
        // ====================================================

        composable(
            route =
                RUTA_CATALOGO_EDITAR,

            arguments = listOf(

                navArgument(
                    "eventoId"
                ) {
                    type =
                        NavType.StringType
                }
            )
        ) { backStackEntry ->

            val eventoId =
                backStackEntry
                    .arguments
                    ?.getString(
                        "eventoId"
                    )

            EventoCatalogoFormScreen(

                eventoId =
                    eventoId,

                onVolver = {
                    navController.popBackStack()
                },

                onGuardado = {
                    navController.popBackStack()
                },
            )
        }


        // ====================================================
        // REGISTRO DEL PARTIDO EN VIVO
        // ====================================================

        composable(
            route =
                RUTA_REGISTRO,

            arguments = listOf(

                navArgument(
                    "partidoId"
                ) {
                    type =
                        NavType.StringType
                }
            )
        ) {

            RegistroPartidoScreen(

                onVolver = {
                    navController.popBackStack()
                },
            )
        }
    }
}