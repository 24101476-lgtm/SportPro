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
