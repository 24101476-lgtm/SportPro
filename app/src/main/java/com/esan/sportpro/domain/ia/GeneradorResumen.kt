package com.esan.sportpro.domain.ia

import kotlinx.coroutines.flow.Flow

/** Fallo al generar el resumen, con el mensaje que ve el entrenador (US-023, criterio 10). */
class ErrorResumen(val mensaje: String) : Exception(mensaje) {
    companion object {
        const val GENERICO = "No se pudo generar el resumen. Inténtalo nuevamente"
    }
}

/**
 * Pide el resumen a la función intermedia que guarda la clave del proveedor de IA. La app nunca
 * habla directamente con el proveedor. Implementación: `data/ia/SupabaseGeneradorResumen`.
 */
interface GeneradorResumen {

    /** @throws ErrorResumen si falla la llamada, se agota el tiempo (30 s) o la respuesta no es válida. */
    suspend fun generar(partidoId: String, insumo: InsumoResumen): SalidaIa
}

/** Versiones del resumen de un partido (`partidos/{id}/resumenes`). Implementación: `data/ia/FirestoreResumenRepository`. */
interface ResumenRepository {

    /** Todas las versiones del partido, de la más reciente a la más antigua. */
    fun observarResumenes(partidoId: String): Flow<List<ResumenPartido>>

    /**
     * Guarda una generación como una versión nueva en estado BORRADOR, con el modelo, la marca de
     * tiempo y el uid de quien la pidió (criterio 12).
     * @return `true` si el servidor confirmó, `false` si quedó pendiente por falta de conexión.
     */
    suspend fun guardarBorrador(resumen: ResumenPartido): Boolean
}

/**
 * Revisión y aprobación del resumen (US-024). Cada operación escribe sobre una versión existente:
 * el texto original de la IA nunca se modifica, solo se agrega el texto del entrenador.
 */
interface RevisionResumenRepository {

    /**
     * Resumen aprobado más reciente del partido, o null. Es lo único que leen jugadores y padres,
     * por eso la consulta filtra por estado APROBADO (las reglas rechazan leer los borradores).
     */
    fun observarPublicado(partidoId: String): Flow<ResumenPartido?>

    /**
     * Guarda el texto del entrenador y deja la versión en BORRADOR. Si estaba aprobada, deja de ser
     * visible hasta su nueva aprobación (US-024, criterio 9).
     * @return `true` si el servidor confirmó, `false` si quedó pendiente por falta de conexión.
     */
    suspend fun guardarEdicion(partidoId: String, resumenId: String, bloques: BloquesResumen, uid: String): Boolean

    /**
     * Aprueba la versión con el texto final, el uid de quien aprueba y la fecha del servidor
     * (criterio 6). [visibleEnComunidad] es la decisión del entrenador del criterio 7.
     */
    suspend fun aprobar(
        partidoId: String,
        resumenId: String,
        bloques: BloquesResumen,
        uid: String,
        visibleEnComunidad: Boolean,
    ): Boolean

    /**
     * Publica el resumen aprobado en el muro de la comunidad y guarda el id de la publicación en la
     * versión. @return el id de la publicación.
     */
    suspend fun publicarEnComunidad(
        partidoId: String,
        resumenId: String,
        academiaId: String,
        autorUid: String,
        autorNombre: String,
        texto: String,
    ): String

    /** Retira de la comunidad la publicación de un resumen que vuelve a Borrador. */
    suspend fun retirarDeComunidad(partidoId: String, resumenId: String, publicacionId: String)
}

/** Evaluaciones documentadas de los resúmenes aprobados (US-025). Implementación: `data/ia/FirestoreEvaluacionRepository`. */
interface EvaluacionRepository {

    /** La evaluación de una versión del resumen, o null si todavía no se registró. */
    fun observarEvaluacion(partidoId: String, resumenId: String): Flow<EvaluacionResumen?>

    /**
     * Todas las evaluaciones que el usuario puede ver: el administrador, las de su academia; el
     * entrenador, las de los resúmenes que aprobó. De la más reciente a la más antigua.
     */
    fun observarEvaluaciones(uid: String, esAdministrador: Boolean, academiaId: String): Flow<List<EvaluacionResumen>>

    /** Crea la evaluación de la versión; las reglas no permiten modificarla después. @return `true` si el servidor confirmó. */
    suspend fun guardar(evaluacion: EvaluacionResumen): Boolean
}
