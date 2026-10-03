package com.esan.sportpro.domain.jugadores

import kotlinx.coroutines.flow.Flow

/** Acceso a los jugadores de una academia. Implementación: `data/jugadores/FirestoreJugadorRepository`. */
interface JugadorRepository {

    /** Jugadores de la academia (activos e inactivos), ordenados por apellidos y nombres. */
    fun observarJugadores(academiaId: String): Flow<List<Jugador>>

    /**
     * Crea el jugador si [Jugador.id] está vacío; si no, actualiza su perfil.
     * @return `true` si el servidor confirmó, `false` si quedó pendiente por falta de conexión.
     */
    suspend fun guardar(jugador: Jugador): Boolean

    /** Activa o desactiva un jugador (nunca se borra). */
    suspend fun cambiarEstado(jugadorId: String, activo: Boolean): Boolean
}
