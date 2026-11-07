package com.esan.sportpro.domain.ia

/** Ciclo de vida de un resumen (US-023 genera BORRADOR; US-024 lo revisa y aprueba). */
enum class EstadoResumen {
    BORRADOR,
    EN_REVISION,
    APROBADO,
    DESCARTADO,
    DESACTUALIZADO,
}

/** Una afirmación del resumen y los eventos del partido que la respaldan. */
data class Afirmacion(
    val texto: String,
    val eventoIds: List<String>,
)

/** Respuesta ya validada de la función intermedia, con los jugadores todavía como alias (`J09`). */
data class SalidaIa(
    val modelo: String,
    val hechos: List<Afirmacion>,
    val desarrollo: List<Afirmacion>,
    val advertencias: List<String>,
)

/** Texto del resumen que no se pudo contrastar con los eventos registrados ("Dato no verificado"). */
data class MencionNoVerificada(
    val texto: String,
    val motivo: String,
)

/** Los tres bloques del resumen. [interpretacion] nace vacío: la completa el entrenador. */
data class BloquesResumen(
    val hechos: String,
    val desarrollo: String,
    val interpretacion: String = "",
)

/** Una generación guardada en `partidos/{partidoId}/resumenes` (cada intento es una versión). */
data class ResumenPartido(
    val id: String,
    val partidoId: String,
    val version: Int,
    val estado: EstadoResumen,
    val bloques: BloquesResumen,
    val modelo: String,
    val versionPrompt: String,
    val solicitadoPor: String,
    val creadoEnMs: Long?,
    val mencionesNoVerificadas: List<MencionNoVerificada>,
    val advertencias: List<String>,
    val eventosUsados: List<String>,
    val marcador: String,
)
