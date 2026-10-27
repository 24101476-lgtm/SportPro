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
         * - Padre de familia: sus hijos vinculados (US-007: el módulo Jugadores le muestra solo
         *   "Mis hijos", nunca a otros jugadores), mensualidades, partidos, estadísticas y comunidad.
         */
        fun visiblesPara(rol: UserRole): List<HomeModule> = when (rol) {
            UserRole.ADMINISTRADOR -> entries.toList()
            UserRole.ENTRENADOR -> listOf(
                ACADEMIA, JUGADORES, ENTRENAMIENTOS, PARTIDO, ESTADISTICAS, COMUNIDAD, IA,
            )
            UserRole.JUGADOR -> listOf(ENTRENAMIENTOS, PARTIDO, ESTADISTICAS, COMUNIDAD)
            UserRole.PADRE_DE_FAMILIA -> listOf(JUGADORES, MENSUALIDADES, PARTIDO, ESTADISTICAS, COMUNIDAD)
        }
    }
}

/** Tipo de pestaña de la barra inferior. */
enum class TipoPestana { INICIO, SECCION, MAS }

/**
 * Una pestaña de la barra inferior. Una [TipoPestana.SECCION] con varios [modulos] los muestra
 * con pestañas internas; [TipoPestana.MAS] los lista como accesos.
 */
data class PestanaNavegacion(
    val id: String,
    val label: String,
    val tipo: TipoPestana,
    val modulos: List<HomeModule> = emptyList(),
)

/**
 * Barra inferior de 4 pestañas por rol (corrección de la entrega de la semana 7: con 7 u 8
 * pestañas los botones quedaban pequeños y apretados). Lo que no cabe va agrupado en una sección
 * o en "Más". Función pura para poder probarla sin Android.
 */
object MenuPrincipal {
    const val MAX_PESTANAS = 4

    private val inicio = PestanaNavegacion("inicio", "Inicio", TipoPestana.INICIO)
    private val partidos = PestanaNavegacion("partidos", "Partidos", TipoPestana.SECCION, listOf(HomeModule.PARTIDO))

    fun para(rol: UserRole): List<PestanaNavegacion> = when (rol) {
        UserRole.ADMINISTRADOR -> listOf(
            inicio,
            PestanaNavegacion(
                "plantel", "Plantel", TipoPestana.SECCION,
                listOf(HomeModule.ACADEMIA, HomeModule.JUGADORES, HomeModule.ENTRENAMIENTOS, HomeModule.MENSUALIDADES),
            ),
            partidos,
            PestanaNavegacion("mas", "Más", TipoPestana.MAS, listOf(HomeModule.ESTADISTICAS, HomeModule.COMUNIDAD, HomeModule.IA)),
        )
        UserRole.ENTRENADOR -> listOf(
            inicio,
            PestanaNavegacion(
                "plantel", "Plantel", TipoPestana.SECCION,
                listOf(HomeModule.ACADEMIA, HomeModule.JUGADORES, HomeModule.ENTRENAMIENTOS),
            ),
            partidos,
            PestanaNavegacion("mas", "Más", TipoPestana.MAS, listOf(HomeModule.ESTADISTICAS, HomeModule.COMUNIDAD, HomeModule.IA)),
        )
        UserRole.JUGADOR -> listOf(
            inicio,
            PestanaNavegacion("entrenos", "Entrenos", TipoPestana.SECCION, listOf(HomeModule.ENTRENAMIENTOS)),
            partidos,
            PestanaNavegacion("mas", "Más", TipoPestana.MAS, listOf(HomeModule.ESTADISTICAS, HomeModule.COMUNIDAD)),
        )
        UserRole.PADRE_DE_FAMILIA -> listOf(
            inicio,
            PestanaNavegacion(
                "hijos", "Mis hijos", TipoPestana.SECCION,
                listOf(HomeModule.JUGADORES, HomeModule.MENSUALIDADES),
            ),
            partidos,
            PestanaNavegacion("mas", "Más", TipoPestana.MAS, listOf(HomeModule.ESTADISTICAS, HomeModule.COMUNIDAD)),
        )
    }

    /** Pestaña que contiene a [modulo] para ese rol, o `null` si el rol no lo ve. */
    fun pestanaDe(rol: UserRole, modulo: HomeModule): PestanaNavegacion? =
        para(rol).firstOrNull { modulo in it.modulos }
}
