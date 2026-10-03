package com.esan.sportpro.domain.partido

/**
 * Configuración inicial del catálogo de eventos. Se usa para poblar la colección
 * `catalogoEventos` la primera vez y como respaldo si el catálogo aún no se ha descargado.
 * El administrador puede modificarlo luego desde la configuración del catálogo.
 */
object CatalogoBase {

    const val VERSION = 1

    private val M = listOf(Campos.MINUTO)
    private val ME = listOf(Campos.MINUTO, Campos.EQUIPO)
    private val MEJ = listOf(Campos.MINUTO, Campos.EQUIPO, Campos.JUGADOR)

    val tipos: List<TipoEvento> = listOf(
        TipoEvento(TiposEvento.INICIO_PARTIDO, "Inicio de partido", AmbitoEvento.PARTIDO, M, false, 1),
        TipoEvento(TiposEvento.FIN_PRIMER_TIEMPO, "Fin de primer tiempo", AmbitoEvento.PARTIDO, M, false, 2),
        TipoEvento(TiposEvento.INICIO_SEGUNDO_TIEMPO, "Inicio de segundo tiempo", AmbitoEvento.PARTIDO, M, false, 3),
        TipoEvento(TiposEvento.FIN_PARTIDO, "Fin de partido", AmbitoEvento.PARTIDO, M, false, 4),
        TipoEvento(TiposEvento.GOL, "Gol", AmbitoEvento.JUGADOR, MEJ, true, 5),
        TipoEvento(TiposEvento.FALTA, "Falta", AmbitoEvento.JUGADOR, MEJ, false, 6),
        TipoEvento(TiposEvento.TARJETA_AMARILLA, "Tarjeta amarilla", AmbitoEvento.JUGADOR, MEJ, false, 7),
        TipoEvento(TiposEvento.TARJETA_ROJA, "Tarjeta roja", AmbitoEvento.JUGADOR, MEJ, false, 8),
        TipoEvento(TiposEvento.CAMBIO, "Cambio", AmbitoEvento.JUGADOR,
            listOf(Campos.MINUTO, Campos.EQUIPO, Campos.JUGADOR_SALE, Campos.JUGADOR_ENTRA), false, 9),
        TipoEvento(TiposEvento.PENAL, "Penal", AmbitoEvento.JUGADOR, MEJ + Campos.RESULTADO, false, 10),
        TipoEvento(TiposEvento.TIRO_ESQUINA, "Tiro de esquina", AmbitoEvento.EQUIPO, ME, false, 11),
        TipoEvento(TiposEvento.FUERA_DE_JUEGO, "Fuera de juego", AmbitoEvento.EQUIPO, ME, false, 12),
        TipoEvento(TiposEvento.SAQUE_LATERAL, "Saque lateral", AmbitoEvento.EQUIPO, ME, false, 13),
        TipoEvento(TiposEvento.SAQUE_META, "Saque de meta", AmbitoEvento.EQUIPO, ME, false, 14),
    )

    fun porId(id: String): TipoEvento? = tipos.firstOrNull { it.id == id }
}
