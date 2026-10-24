package com.esan.sportpro.domain.partido

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Acceso a los datos del partido.
 *
 * Las operaciones de escritura no esperan la confirmación del servidor:
 * Firestore las guarda primero en el dispositivo y las sincroniza
 * en cuanto hay red, por lo que funcionan igual con o sin conexión.
 */
interface PartidoRepository {

    val uidActual: String?

    /**
     * Mensajes de error cuando el servidor rechaza una escritura
     * (por ejemplo, por permisos).
     */
    val erroresEscritura: SharedFlow<String>

    // ------------------------------------------------ Lecturas generales

    fun observarPartidos(): Flow<List<Partido>>

    fun observarPartido(
        partidoId: String
    ): Flow<Partido?>

    fun observarEventos(
        partidoId: String
    ): Flow<List<EventoPartido>>

    fun observarConvocados(
        partidoId: String
    ): Flow<List<JugadorConvocado>>

    // ------------------------------------------------ US-013: catálogo

    fun observarCatalogo(): Flow<List<TipoEvento>>

    fun observarCatalogoCompleto(): Flow<List<TipoEvento>>

    fun crearTipoEvento(
        tipo: TipoEvento
    ): String

    fun actualizarTipoEvento(
        tipo: TipoEvento
    )

    fun cambiarEstadoTipoEvento(
        tipoId: String,
        activo: Boolean,
        versionActual: Int,
    )

    /**
     * Rol guardado en usuarios/{uid}.rol,
     * o null si el perfil aún no existe.
     */
    fun observarRolActual(): Flow<String?>

    // ------------------------------------------------ US-014: programación y convocatoria

    /**
     * Crea un partido programado pendiente de iniciar.
     */
    fun crearPartidoProgramado(
        nombreEquipo: String,
        nombreRival: String,
        fechaMs: Long,
        hora: String,
        lugar: String,
        tipoPartido: String,
        duracionTiempoMin: Int,
        maxConvocados: Int,
        fechaLimiteRespuestaMs: Long,
    ): String

    /**
     * Observa en tiempo real las respuestas de la convocatoria.
     */
    fun observarRespuestasConvocatoria(
        partidoId: String
    ): Flow<List<RespuestaConvocatoria>>

    /**
     * Publica los jugadores seleccionados para la convocatoria.
     *
     * Se almacenarán en respuestasConvocatoria y no en convocados,
     * porque convocados queda reservado para la alineación final.
     */
    fun publicarConvocatoria(
        partidoId: String,
        jugadores: List<RespuestaConvocatoria>,
    )

    /**
     * Reprograma fecha, hora, lugar y fecha límite de respuesta.
     */
    fun reprogramarPartido(
        partidoId: String,
        nuevaFechaMs: Long,
        nuevaHora: String,
        nuevoLugar: String,
        nuevaFechaLimiteRespuestaMs: Long,
    )

    /**
     * Cancela el partido conservando el registro.
     */
    fun cancelarPartido(
        partidoId: String,
        motivo: String,
    )

    /**
     * Registra el envío de un recordatorio a jugadores
     * que todavía no han respondido.
     */
    fun enviarRecordatorioConvocatoria(
        partidoId: String,
        jugadoresPendientesIds: List<String>,
    )

    // ------------------------------------------------ Partido de práctica existente

    /**
     * Crea un partido de práctica con plantel de ejemplo
     * y alineación confirmada.
     */
    fun crearPartidoPractica(
        nombreEquipo: String,
        nombreRival: String,
        cambiosPermitidos: Int,
        duracionTiempoMin: Int,
    ): String

    // ------------------------------------------------ Registro de eventos en vivo

    /**
     * Registra un evento nuevo. Devuelve el id del documento creado.
     *
     * [eventosRelacionados] se guardan en el mismo lote y apuntan
     * al evento principal.
     *
     * Ejemplos:
     * - gol generado por un penal convertido;
     * - roja por doble amarilla.
     */
    fun registrarEvento(
        partidoId: String,
        borrador: BorradorEvento,
        incompleto: Boolean,
        fueraDeOrden: Boolean,
        excedeCambios: Boolean,
        eventosRelacionados: List<BorradorEvento> = emptyList(),
    ): String

    /**
     * Crea una nueva versión del evento y marca
     * la anterior como REEMPLAZADO.
     */
    fun corregirEvento(
        partidoId: String,
        original: EventoPartido,
        borrador: BorradorEvento,
        motivo: String,
        camposModificados: List<String>,
        incompleto: Boolean,
    )

    fun observarJugadoresEquipo(
        equipoId: String
    ): Flow<List<JugadorPlantel>>

    /**
     * Marca el evento y los generados a partir de él
     * como ANULADO. Nunca se borra.
     */
    fun anularEvento(
        partidoId: String,
        evento: EventoPartido,
        relacionados: List<EventoPartido>,
        motivo: String,
    )

    fun cerrarActa(
        partidoId: String,
        eventosIncompletos: Int,
    )

    fun observarRespuestaConvocatoria(
        partidoId: String,
        jugadorId: String,
    ): Flow<RespuestaConvocatoria?>

    fun responderConvocatoria(
        partidoId: String,
        jugadorId: String,
        estado: EstadoRespuestaConvocatoria,
        motivo: String? = null,
    )
}