package com.esan.sportpro.domain.comunidad

/**
 * Catálogo único de posiciones de la aplicación. Lo usan la ficha del jugador (US-006, criterio 2)
 * y los avisos de pruebas (US-029, criterio 3: "una o varias de las posiciones del catálogo").
 *
 * Los avisos guardan el `name` de la posición y la ficha guarda la [etiqueta]. Las versiones
 * anteriores usaban PORTERO, LATERAL y VOLANTE_CENTRAL: [desde] y [desdeEtiqueta] las traducen
 * para que los datos ya guardados se sigan mostrando y filtrando.
 */
enum class PosicionJugador(val etiqueta: String) {
    ARQUERO("Arquero"),
    DEFENSA_CENTRAL("Defensa central"),
    LATERAL_DERECHO("Lateral derecho"),
    LATERAL_IZQUIERDO("Lateral izquierdo"),
    VOLANTE_DEFENSIVO("Volante defensivo"),
    VOLANTE_MIXTO("Volante mixto"),
    VOLANTE_OFENSIVO("Volante ofensivo"),
    EXTREMO("Extremo"),
    DELANTERO("Delantero"),
    ;

    companion object {
        /** Nombres y etiquetas de versiones anteriores del catálogo. "Lateral" no distinguía lado. */
        private val ANTERIORES = mapOf(
            "PORTERO" to ARQUERO,
            "Portero" to ARQUERO,
            "VOLANTE_CENTRAL" to VOLANTE_MIXTO,
            "Volante central" to VOLANTE_MIXTO,
        )

        /** Desde el `name` guardado (avisos). `null` si no corresponde a ninguna posición. */
        fun desde(nombre: String?): PosicionJugador? =
            entries.firstOrNull { it.name == nombre } ?: ANTERIORES[nombre]

        /** Desde la etiqueta guardada (ficha del jugador). */
        fun desdeEtiqueta(etiqueta: String?): PosicionJugador? =
            entries.firstOrNull { it.etiqueta == etiqueta } ?: ANTERIORES[etiqueta]
    }
}
