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

/**
 * Una generación guardada en `partidos/{partidoId}/resumenes` (cada intento es una versión).
 *
 * [bloques] es siempre el texto original de la IA y nunca cambia (US-024, criterio 6: se conserva
 * para compararlo). Lo que corrige y aprueba el entrenador va en [bloquesFinal].
 */
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
    /** Texto editado por el entrenador; null mientras nadie lo haya tocado. */
    val bloquesFinal: BloquesResumen? = null,
    val editadoPor: String? = null,
    val aprobadoPor: String? = null,
    val aprobadoEnMs: Long? = null,
    /** US-024, criterio 7: el entrenador decide si el resumen aprobado también se publica en la comunidad. */
    val visibleEnComunidad: Boolean = false,
    /** Publicación del muro creada al aprobar con [visibleEnComunidad]; se retira si el resumen vuelve a Borrador. */
    val publicacionComunidadId: String? = null,
) {
    /** El texto con el que se trabaja: el del entrenador si ya lo editó, y si no el de la IA. */
    val bloquesVigentes: BloquesResumen get() = bloquesFinal ?: bloques

    val publicado: Boolean get() = estado == EstadoResumen.APROBADO
}
