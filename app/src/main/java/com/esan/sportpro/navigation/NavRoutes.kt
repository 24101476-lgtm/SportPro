package com.esan.sportpro.navigation

/** Rutas de nivel superior. Cada módulo puede anidar su propio sub-grafo bajo estas rutas. */
sealed class NavRoutes(val route: String) {
    data object Login : NavRoutes("login")
    data object Register : NavRoutes("register")
    data object Home : NavRoutes("home")
}

/** Módulos funcionales disponibles desde [com.esan.sportpro.ui.home.HomeScreen]. */
enum class HomeModule(val label: String) {
    ACADEMIA("Academia"),
    JUGADORES("Jugadores"),
    ENTRENAMIENTOS("Entrenamientos"),
    PARTIDO("Partido"),
    // MENSUALIDADES (US-008) agregado por Flavia Ojeda — módulo propio, no reemplaza Jugadores.
    MENSUALIDADES("Mensualidades"),
    // ESTADISTICAS (US-026, US-027) agregado por Flavia Ojeda — agrupa Estadísticas y Anuncios
    // ("Página 12" del backlog) en un mismo tab con pestañas internas.
    ESTADISTICAS("Estadísticas"),
    COMUNIDAD("Comunidad"),
    IA("Resumen IA"),
}
