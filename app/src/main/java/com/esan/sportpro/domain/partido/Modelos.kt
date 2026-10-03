package com.esan.sportpro.domain.partido

/** Estado general del partido. */
enum class EstadoPartido { PROGRAMADO, EN_CURSO, FINALIZADO, ACTA_CERRADA }

/** Estado de cada versión de un evento. Ningún evento se elimina físicamente. */
enum class EstadoEvento { VIGENTE, REEMPLAZADO, ANULADO }

/** Ámbito del evento según el catálogo: afecta al partido, a un equipo o a un jugador. */
enum class AmbitoEvento { PARTIDO, EQUIPO, JUGADOR }

/** Equipo al que se atribuye el evento. */
enum class EquipoEvento { PROPIO, RIVAL }

/** Resultado de un penal. */
enum class ResultadoPenal { CONVERTIDO, FALLADO }

/** Campos que el catálogo puede marcar como obligatorios para un tipo de evento. */
object Campos {
    const val MINUTO = "minuto"
    const val EQUIPO = "equipo"
    const val JUGADOR = "jugador"
    const val RESULTADO = "resultado"
    const val JUGADOR_SALE = "jugadorSale"
    const val JUGADOR_ENTRA = "jugadorEntra"
}

/** Identificadores de los tipos de evento con comportamiento especial. */
object TiposEvento {
    const val INICIO_PARTIDO = "inicio_partido"
    const val FIN_PRIMER_TIEMPO = "fin_primer_tiempo"
    const val INICIO_SEGUNDO_TIEMPO = "inicio_segundo_tiempo"
    const val FIN_PARTIDO = "fin_partido"
    const val SAQUE_META = "saque_meta"
    const val SAQUE_LATERAL = "saque_lateral"
    const val TIRO_ESQUINA = "tiro_esquina"
    const val FALTA = "falta"
    const val PENAL = "penal"
    const val GOL = "gol"
    const val TARJETA_AMARILLA = "tarjeta_amarilla"
    const val TARJETA_ROJA = "tarjeta_roja"
    const val FUERA_DE_JUEGO = "fuera_de_juego"
    const val CAMBIO = "cambio"
}

/** Valores especiales del selector de jugador. */
object JugadorEspecial {
    const val RIVAL = "RIVAL"
    const val NO_IDENTIFICADO = "NO_IDENTIFICADO"
    const val NOMBRE_RIVAL = "Jugador del rival"
    const val NOMBRE_NO_IDENTIFICADO = "Jugador no identificado"

    fun esEspecial(id: String?): Boolean = id == RIVAL || id == NO_IDENTIFICADO
}

/** Tipo de evento definido en el catálogo configurable (colección `catalogoEventos`). */
data class TipoEvento(
    val id: String,
    val nombre: String,
    val ambito: AmbitoEvento,
    val camposObligatorios: List<String>,
    val sumaMarcador: Boolean,
    val orden: Int,
    val activo: Boolean = true,
    val version: Int = 1,
) {
    fun requiere(campo: String): Boolean = campo in camposObligatorios
}

/** Jugador convocado al partido (subcolección `convocados`). */
data class JugadorConvocado(
    val id: String,
    val nombre: String,
    val dorsal: Int?,
    val titular: Boolean,
    val posicion: String? = null,
) {
    val etiqueta: String get() = if (dorsal != null) "$dorsal · $nombre" else nombre
}

/** Documento principal del partido (colección `partidos`). */
data class Partido(
    val id: String,
    val nombreEquipo: String,
    val nombreRival: String,
    val categoria: String?,
    val estado: EstadoPartido,
    val alineacionConfirmada: Boolean,
    val cambiosPermitidos: Int,
    val duracionTiempoMin: Int,
    /** Marcas de tiempo del servidor (ms). Si aún no se sincronizan se usa la estimación local. */
    val inicioPrimerTiempoMs: Long?,
    val inicioSegundoTiempoMs: Long?,
    val enDescanso: Boolean,
    val operadorUid: String?,
    val entrenadorUid: String?,
    val creadoPor: String?,
    val fechaMs: Long?,
    val actaCerradaConIncompletos: Int? = null,
    val tienePendientes: Boolean = false,
)

/**
 * Una versión de un evento del partido (subcolección `eventos`).
 * Todas las versiones de un mismo evento comparten [grupoId]; la edición crea una nueva versión
 * y marca la anterior como [EstadoEvento.REEMPLAZADO].
 */
data class EventoPartido(
    val id: String,
    val grupoId: String,
    val version: Int,
    val tipoId: String,
    val tipoNombre: String,
    val minuto: Int,
    val equipo: EquipoEvento?,
    val jugadorId: String?,
    val jugadorNombre: String?,
    val jugadorSecundarioId: String?,
    val jugadorSecundarioNombre: String?,
    val resultadoPenal: ResultadoPenal?,
    val observacion: String?,
    val operadorUid: String?,
    val operadorNombre: String?,
    val registradoEnMs: Long?,
    val estado: EstadoEvento,
    val versionCatalogo: Int,
    val incompleto: Boolean,
    val fueraDeOrden: Boolean,
    val excedeCambios: Boolean,
    val eventoOrigenId: String?,
    val motivo: String?,
    val camposModificados: List<String>,
    val reemplazadoPor: String?,
    val anuladoPor: String?,
    val anuladoPorNombre: String?,
    val anuladoEnMs: Long?,
    /** true mientras la escritura está solo en el dispositivo (sin confirmar por el servidor). */
    val pendienteSincronizar: Boolean,
)

/** Datos capturados en la hoja inferior para registrar o corregir un evento. */
data class BorradorEvento(
    val tipo: TipoEvento,
    val minuto: Int,
    val equipo: EquipoEvento?,
    /** Id del convocado, [JugadorEspecial.RIVAL], [JugadorEspecial.NO_IDENTIFICADO] o null. */
    val jugadorId: String?,
    val jugadorNombre: String?,
    /** Asistente en un gol o jugador que entra en un cambio. */
    val secundarioId: String?,
    val secundarioNombre: String?,
    val resultadoPenal: ResultadoPenal?,
    val observacion: String?,
)
