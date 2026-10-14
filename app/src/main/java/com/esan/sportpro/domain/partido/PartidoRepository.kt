package com.esan.sportpro.domain.partido

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Acceso a los datos del partido. Las operaciones de escritura no esperan la confirmación del
 * servidor: Firestore las guarda primero en el dispositivo y las sincroniza en cuanto hay red,
 * por lo que funcionan igual con o sin conexión.
 */
interface PartidoRepository {

    val uidActual: String?

    /** Mensajes de error cuando el servidor rechaza una escritura (por ejemplo, por permisos). */
    val erroresEscritura: SharedFlow<String>

    fun observarPartidos(): Flow<List<Partido>>

    fun observarPartido(partidoId: String): Flow<Partido?>

    fun observarEventos(partidoId: String): Flow<List<EventoPartido>>

    fun observarConvocados(partidoId: String): Flow<List<JugadorConvocado>>

    fun observarCatalogo(): Flow<List<TipoEvento>>

    /** Rol guardado en `usuarios/{uid}.rol`, o null si el perfil aún no existe. */
    fun observarRolActual(): Flow<String?>

    /**
     * Crea el partido con sus convocados (titulares y suplentes) y la alineación confirmada.
     * Funciona sin conexión: devuelve el id de inmediato.
     */
    fun crearPartido(nuevo: NuevoPartido): String

    /** Guarda en el documento del partido el marcador calculado a partir de los eventos vigentes. */
    fun actualizarMarcador(partidoId: String, golesEquipo: Int, golesRival: Int)

    /**
     * Registra un evento nuevo. Devuelve el id del documento creado.
     * [eventosRelacionados] se guardan en el mismo lote y apuntan al evento principal
     * (por ejemplo, el gol de un penal convertido o la roja por doble amarilla).
     */
    fun registrarEvento(
        partidoId: String,
        borrador: BorradorEvento,
        incompleto: Boolean,
        fueraDeOrden: Boolean,
        excedeCambios: Boolean,
        eventosRelacionados: List<BorradorEvento> = emptyList(),
    ): String

    /** Crea una nueva versión del evento y marca la anterior como Reemplazado. */
    fun corregirEvento(
        partidoId: String,
        original: EventoPartido,
        borrador: BorradorEvento,
        motivo: String,
        camposModificados: List<String>,
        incompleto: Boolean,
    )

    /** Marca el evento (y los generados a partir de él) como Anulado. Nunca se borra. */
    fun anularEvento(partidoId: String, evento: EventoPartido, relacionados: List<EventoPartido>, motivo: String)

    fun cerrarActa(partidoId: String, eventosIncompletos: Int, golesEquipo: Int, golesRival: Int)
}
