package com.esan.sportpro.navigation

/** Rutas de nivel superior. Cada módulo puede anidar su propio sub-grafo bajo estas rutas. */
sealed class NavRoutes(val route: String) {
    data object Login : NavRoutes("login")
    data object Home : NavRoutes("home")
    // US-001: registro con rol (pantalla en `ui/registro`).
    data object RegistroUsuario : NavRoutes("registroUsuario")
    // US-002: recuperación de contraseña.
    data object RecuperarContrasena : NavRoutes("recuperarContrasena")
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
    ;

    companion object {
        /**
         * US-003: módulos que ve cada rol en la barra inferior. El orden de la lista es el orden
         * de la barra. Es una función pura para poder probarla sin Android.
         *
         * - Administrador: todo el producto.
         * - Entrenador: gestiona plantel, entrenamientos y partido; no ve la configuración de la
         *   academia más allá de la lectura de equipos (la escritura se restringe en pantalla).
         * - Jugador: sus entrenamientos, partidos, estadísticas y la comunidad.
         * - Padre de familia: mensualidades de sus hijos, partidos, estadísticas y comunidad.
         *   No accede a datos físicos ni de contacto de otros jugadores (módulo Jugadores oculto).
         */
        fun visiblesPara(rol: UserRole): List<HomeModule> = when (rol) {
            UserRole.ADMINISTRADOR -> entries.toList()
            UserRole.ENTRENADOR -> listOf(
                ACADEMIA, JUGADORES, ENTRENAMIENTOS, PARTIDO, ESTADISTICAS, COMUNIDAD, IA,
            )
            UserRole.JUGADOR -> listOf(ENTRENAMIENTOS, PARTIDO, ESTADISTICAS, COMUNIDAD)
            UserRole.PADRE_DE_FAMILIA -> listOf(MENSUALIDADES, PARTIDO, ESTADISTICAS, COMUNIDAD)
        }
    }
}
