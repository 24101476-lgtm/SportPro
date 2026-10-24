package com.esan.sportpro.domain.jugadores

import kotlinx.coroutines.flow.Flow

/** Acceso a los jugadores de una academia. Implementación: `data/jugadores/FirestoreJugadorRepository`. */
interface JugadorRepository {

    /** Jugadores de la academia (activos e inactivos), ordenados por apellidos y nombres. */
    fun observarJugadores(academiaId: String): Flow<List<Jugador>>

    /**
     * Crea el jugador si [Jugador.id] está vacío; si no, actualiza su perfil. Además mantiene su
     * entrada en `directorio_jugadores` (nombre, equipo y código, sin datos sensibles) que usan
     * los apoderados para pedir la vinculación (US-007).
     * @param equipoEtiqueta equipo y categoría para el directorio ("Barcelona FC · Sub-13").
     * @return `true` si el servidor confirmó, `false` si quedó pendiente por falta de conexión.
     */
    suspend fun guardar(jugador: Jugador, equipoEtiqueta: String = jugador.equipoNombre): Boolean

    /** Activa o desactiva un jugador (nunca se borra); un jugador inactivo no acepta solicitudes. */
    suspend fun cambiarEstado(jugador: Jugador, activo: Boolean): Boolean
}
