package com.esan.sportpro.domain.cuentas

import com.esan.sportpro.navigation.UserRole
import java.time.LocalDate

/**
 * Estado de una cuenta. Las cuentas de entrenador nacen en [PENDIENTE] hasta que un administrador
 * las aprueba (así nadie se registra como entrenador para leer las fichas de los menores); el resto
 * nace en [ACTIVO] (US-001). El administrador pasa una cuenta a [DESACTIVADO] y el login la rechaza
 * con el mensaje de US-002.
 */
enum class EstadoUsuario {
    ACTIVO,
    PENDIENTE,
    DESACTIVADO,
}

/**
 * Perfil del usuario que se guarda en `usuarios/{uid}` (US-001).
 *
 * El campo [rol] se persiste con [UserRole.name] (en mayúsculas: `ADMINISTRADOR`, `ENTRENADOR`,
 * `JUGADOR`, `PADRE_DE_FAMILIA`) porque es exactamente el formato que lee `firestore.rules`.
 * [clubId] nace en `null`: el usuario se vincula a una academia más adelante (US-004).
 */
data class Usuario(
    val uid: String,
    val nombres: String,
    val apellidos: String,
    val correo: String,
    val telefono: String,
    val fechaNacimiento: LocalDate,
    val rol: UserRole,
    val esMenor: Boolean,
    val estado: EstadoUsuario,
    val clubId: String?,
)