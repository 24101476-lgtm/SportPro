package com.esan.sportpro.domain.academia

import kotlinx.coroutines.flow.Flow

/** Acceso a los equipos de una academia. Implementación: `data/academia/FirestoreEquipoRepository`. */
interface EquipoRepository {

    /** Equipos de la academia (activos e inactivos), ordenados por categoría y nombre. */
    fun observarEquipos(academiaId: String): Flow<List<Equipo>>

    /**
     * Crea el equipo si [esNuevo] (por defecto, cuando [Equipo.id] está vacío); si no, actualiza
     * nombre, categoría y entrenador. Al crear con un id ya reservado ([nuevoId]) se usa ese id.
     * @return `true` si el servidor confirmó, `false` si quedó pendiente por falta de conexión.
     */
    suspend fun guardar(equipo: Equipo, esNuevo: Boolean = equipo.id.isBlank()): Boolean

    /** Reserva un id para un equipo nuevo (sirve para subir su escudo justo después de crearlo). */
    fun nuevoId(): String

    /**
     * Sube el escudo (imagen local) a Supabase y guarda su URL en el equipo; borra el anterior.
     * @return la URL nueva, o `null` si no se pudo (sin conexión o error de Supabase).
     */
    suspend fun subirEscudo(equipoId: String, uriLocal: String): String?

    /** Activa o desactiva un equipo (nunca se borra). */
    suspend fun cambiarEstado(equipoId: String, activo: Boolean): Boolean
}
