package com.esan.sportpro.domain.academia

import kotlinx.coroutines.flow.Flow

/** Acceso a los equipos de una academia. Implementación: `data/academia/FirestoreEquipoRepository`. */
interface EquipoRepository {

    /** Equipos de la academia (activos e inactivos), ordenados por categoría y nombre. */
    fun observarEquipos(academiaId: String): Flow<List<Equipo>>

    /**
     * Crea el equipo si [Equipo.id] está vacío; si no, actualiza nombre, categoría y entrenador.
     * @return `true` si el servidor confirmó, `false` si quedó pendiente por falta de conexión.
     */
    suspend fun guardar(equipo: Equipo): Boolean

    /** Activa o desactiva un equipo (nunca se borra). */
    suspend fun cambiarEstado(equipoId: String, activo: Boolean): Boolean
}
