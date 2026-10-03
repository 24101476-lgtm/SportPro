package com.esan.sportpro.domain.comunidad

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Suspensión activa de una cuenta (US-030, criterio 5 y 9). Colección Firestore: "suspensiones",
 * con el uid del usuario suspendido como id de documento (una suspensión activa a la vez).
 *
 * TODO(US-003/cuentas): cuando exista el módulo de autenticación y perfil de cuentas, el login
 * debe consultar esta colección para impedir que un usuario suspendido publique, comente o
 * reaccione también desde el lado del flujo de sesión, no solo aquí; por ahora la comprobación
 * vive únicamente en [com.esan.sportpro.data.comunidad.ModeracionRepository.observarSuspensionActiva]
 * y en las reglas de seguridad de Firestore (ver comentario en ese repositorio).
 */
data class Suspension(
    val uid: String = "",
    val fechaFin: Date? = null,
    /** 10..300 caracteres, el mismo motivo de resolución que originó la suspensión. */
    val motivo: String = "",
    val moderadorUid: String = "",
    @ServerTimestamp
    val fechaCreacion: Date? = null,
) {
    fun estaActivaRespectoDe(ahora: Date): Boolean {
        val fin = fechaFin ?: return false
        return ahora.before(fin)
    }
}
