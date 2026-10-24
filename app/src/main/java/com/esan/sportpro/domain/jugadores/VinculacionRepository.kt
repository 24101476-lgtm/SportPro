package com.esan.sportpro.domain.jugadores

import kotlinx.coroutines.flow.Flow

/**
 * Vinculación padre/madre ↔ jugador (US-007). Implementación:
 * `data/jugadores/FirestoreVinculacionRepository`.
 */
interface VinculacionRepository {

    // ---- Padre de familia ------------------------------------------------------------------

    /** Busca un jugador activo de la academia por su código de 6 caracteres; `null` si no existe. */
    suspend fun buscarPorCodigo(academiaId: String, codigo: String): DirectorioJugador?

    /** Jugadores de la academia (solo nombre, equipo y código) para elegirlos de la lista de su equipo. */
    fun observarDirectorio(academiaId: String): Flow<List<DirectorioJugador>>

    /** Vínculos del padre en cualquier estado. */
    fun observarMisVinculaciones(padreUid: String): Flow<List<Vinculacion>>

    /** Crea (o reenvía, si fue rechazada o revocada) la solicitud en estado Pendiente. */
    suspend fun solicitar(solicitud: Vinculacion): Boolean

    /** Perfil de un jugador vinculado; las reglas solo lo permiten con el vínculo aprobado. */
    fun observarJugador(jugadorId: String): Flow<Jugador?>

    // ---- Administrador y entrenador ----------------------------------------------------------

    /** Todas las solicitudes de la academia (pendientes y resueltas). */
    fun observarSolicitudes(academiaId: String): Flow<List<Vinculacion>>

    suspend fun aprobar(vinculacion: Vinculacion, uid: String, nombre: String): Boolean

    suspend fun rechazar(vinculacion: Vinculacion, motivo: String, uid: String, nombre: String): Boolean

    /** Solo el administrador; el padre pierde el acceso en cuanto el cambio llega al servidor. */
    suspend fun revocar(vinculacion: Vinculacion, uid: String, nombre: String): Boolean

    /**
     * Asigna el código de vinculación a un jugador que todavía no lo tiene.
     * @param equipoEtiqueta equipo y categoría ("Barcelona FC · Sub-13") que verá el apoderado.
     */
    suspend fun asignarCodigo(jugador: Jugador, codigo: String, equipoEtiqueta: String): Boolean
}
