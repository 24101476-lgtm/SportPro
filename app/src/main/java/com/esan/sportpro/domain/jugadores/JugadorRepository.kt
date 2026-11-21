package com.esan.sportpro.domain.jugadores

import kotlinx.coroutines.flow.Flow

/** Acceso a los jugadores de una academia. Implementación: `data/jugadores/FirestoreJugadorRepository`. */
interface JugadorRepository {

    /** Jugadores de la academia (activos e inactivos), ordenados por apellidos y nombres. */
    fun observarJugadores(academiaId: String): Flow<List<Jugador>>

    /**
     * Ficha enlazada a la cuenta del jugador (`usuarioUid`), o `null` si todavía no hay enlace.
     * Las reglas de Firestore solo dejan leer la propia (US-021: el jugador sigue los partidos de su equipo).
     */
    fun observarFichaPropia(usuarioUid: String): Flow<Jugador?>

    /**
     * US-006: cuentas con rol Jugador de la academia, para enlazar una a su ficha. Solo el personal
     * lo usa (las reglas dejan leer `usuarios` a cualquier autenticado).
     */
    fun observarCuentasJugador(academiaId: String): Flow<List<CuentaJugador>>

    /**
     * US-006, criterio 9: el propio jugador cambia solo su teléfono y su pie hábil.
     * @return `true` si el servidor confirmó, `false` si quedó pendiente por falta de conexión.
     */
    suspend fun actualizarDatosPropios(jugadorId: String, telefono: String, pieHabil: String): Boolean

    /**
     * Crea el jugador si [Jugador.id] está vacío; si no, actualiza su perfil. Además mantiene su
     * entrada en `directorio_jugadores` (nombre, equipo y código, sin datos sensibles) que usan
     * los apoderados para pedir la vinculación (US-007).
     * @param equipoEtiqueta equipo y categoría para el directorio ("Barcelona FC · Sub-13").
     * @return `true` si el servidor confirmó, `false` si quedó pendiente por falta de conexión.
     */
    suspend fun guardar(
        jugador: Jugador,
        equipoEtiqueta: String = jugador.equipoNombre,
        nuevo: Boolean = jugador.id.isBlank(),
    ): Boolean

    /** Id para una ficha nueva, generado en el dispositivo (funciona sin conexión). */
    fun nuevoId(): String

    /**
     * US-006, criterio 7: comprime la foto a 1024 px por lado, la sube a Supabase Storage
     * y guarda su URL en la ficha, eliminando la foto anterior.
     * @param uriLocal URI local (`content://…`) de la cámara o la galería.
     * @return la URL, o `null` si no se pudo subir (sin conexión o error de Supabase).
     */
    suspend fun subirFoto(jugadorId: String, uriLocal: String): String?

    /** Activa o desactiva un jugador (nunca se borra); un jugador inactivo no acepta solicitudes. */
    suspend fun cambiarEstado(jugador: Jugador, activo: Boolean): Boolean
}

/** Cuenta de la app (rol Jugador) que se puede enlazar a una ficha. */
data class CuentaJugador(val uid: String, val nombre: String, val correo: String)
