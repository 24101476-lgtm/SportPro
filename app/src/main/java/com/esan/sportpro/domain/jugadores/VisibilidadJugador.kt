package com.esan.sportpro.domain.jugadores

import com.esan.sportpro.navigation.UserRole

/** Campos de la ficha que el propio jugador puede editar (US-006, criterio 9). */
enum class CampoEditableJugador { FOTO, TELEFONO, PIE_HABIL }

/**
 * Privacidad del perfil del jugador (US-006, criterios 8 y 9; matriz 4.3 y criterios 4.4 del
 * backlog). Sin dependencias de Android ni Firebase.
 *
 * Datos sensibles: fecha de nacimiento, documento, estatura, peso, teléfono, correo y contacto de
 * emergencia. Los ven el personal de la academia, el propio jugador y su apoderado con vínculo
 * aprobado; para cualquier otro usuario se muestran como [TEXTO_RESTRINGIDO].
 */
object VisibilidadJugador {
    const val TEXTO_RESTRINGIDO = "Información restringida"

    fun esPropio(jugador: Jugador, uid: String): Boolean = jugador.usuarioUid.isNotBlank() && jugador.usuarioUid == uid

    fun puedeVerDatosSensibles(rol: UserRole, uid: String, jugador: Jugador, vinculoAprobado: Boolean): Boolean =
        when (rol) {
            UserRole.ADMINISTRADOR, UserRole.ENTRENADOR -> true
            UserRole.PADRE_DE_FAMILIA -> vinculoAprobado
            UserRole.JUGADOR -> esPropio(jugador, uid)
        }

    /** El valor si el usuario puede verlo; si no, el texto "Información restringida". */
    fun mostrar(valor: String?, puedeVer: Boolean): String? = if (puedeVer) valor else TEXTO_RESTRINGIDO

    /**
     * Criterio 9: el personal edita toda la ficha; el propio jugador solo su foto, teléfono y pie
     * hábil; nadie más edita.
     */
    fun camposEditables(rol: UserRole, uid: String, jugador: Jugador): Set<CampoEditableJugador> = when {
        rol == UserRole.ADMINISTRADOR || rol == UserRole.ENTRENADOR -> CampoEditableJugador.entries.toSet()
        rol == UserRole.JUGADOR && esPropio(jugador, uid) -> CampoEditableJugador.entries.toSet()
        else -> emptySet()
    }

    fun puedeEditarFichaCompleta(rol: UserRole): Boolean = rol == UserRole.ADMINISTRADOR || rol == UserRole.ENTRENADOR
}
