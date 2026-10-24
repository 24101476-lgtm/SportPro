package com.esan.sportpro.domain.cuentas

import com.esan.sportpro.navigation.UserRole

/**
 * Datos mínimos del usuario autenticado que necesitan los demás módulos: quién es, qué rol tiene
 * y a qué academia pertenece. Se lee de `usuarios/{uid}` (US-001) y alimenta la navegación por
 * rol (US-003) y el alcance de las consultas de equipos, jugadores y entrenamientos.
 */
data class PerfilActual(
    val uid: String,
    val nombre: String,
    val rol: UserRole,
    val academiaId: String,
) {
    /** Administrador y entrenador gestionan el plantel; jugador y padre solo consultan. */
    val esStaff: Boolean get() = rol == UserRole.ADMINISTRADOR || rol == UserRole.ENTRENADOR

    companion object {
        /**
         * Academia que se usa mientras el usuario no esté vinculado a un club (`clubId == null`,
         * US-004). Coincide con el valor temporal que ya usan Mensualidades y Comunidad.
         */
        const val ACADEMIA_POR_DEFECTO = "academia-demo"
    }
}
