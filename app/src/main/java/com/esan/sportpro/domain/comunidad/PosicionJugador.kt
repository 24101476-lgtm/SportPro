package com.esan.sportpro.domain.comunidad

/**
 * Catálogo de posiciones usado por el formulario de avisos (US-029, criterio 3: "Posiciones
 * buscadas ... una o varias de las posiciones del catálogo de la aplicación").
 *
 * TODO(US-006/007, módulo Jugadores): mover este catálogo a `domain/jugadores` cuando ese módulo
 * exista (aún no tiene código, solo un `.gitkeep`) y sea la fuente única de verdad de posiciones
 * para la ficha de jugador; por ahora se define aquí para no bloquear US-029.
 */
enum class PosicionJugador(val etiqueta: String) {
    PORTERO("Portero"),
    DEFENSA_CENTRAL("Defensa central"),
    LATERAL("Lateral"),
    VOLANTE_DEFENSIVO("Volante defensivo"),
    VOLANTE_CENTRAL("Volante central"),
    VOLANTE_OFENSIVO("Volante ofensivo"),
    EXTREMO("Extremo"),
    DELANTERO("Delantero"),
}
