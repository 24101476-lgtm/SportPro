package com.esan.sportpro.domain.jugadores

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date
import kotlin.random.Random

/** Estado de una solicitud de vinculación padre/madre ↔ jugador (US-007). */
enum class EstadoVinculacion(val etiqueta: String) {
    PENDIENTE("Pendiente"),
    APROBADA("Aprobada"),
    RECHAZADA("Rechazada"),
    REVOCADA("Revocada"),
    ;

    companion object {
        fun desde(nombre: String?): EstadoVinculacion? = entries.firstOrNull { it.name == nombre }
    }
}

/**
 * Vínculo entre un padre de familia y un jugador. Colección `vinculaciones`, con id determinista
 * `{padreUid}_{jugadorId}` ([ReglasVinculacion.idVinculacion]): así las reglas de Firestore pueden
 * comprobar con un solo `get` si el padre tiene acceso al perfil de ese jugador.
 *
 * Mensualidades (US-008) y Estadísticas (US-026) leen `padreUid` + `jugadorId` de los vínculos
 * con estado APROBADA. Cada acción queda en la subcolección `auditoria`.
 */
data class Vinculacion(
    @DocumentId
    val id: String = "",
    val academiaId: String = "",
    val padreUid: String = "",
    val padreNombre: String = "",
    val jugadorId: String = "",
    val jugadorNombre: String = "",
    val equipoId: String = "",
    /** Equipo y categoría del jugador ("Barcelona FC · Sub-13"): lo único visible mientras está pendiente. */
    val equipoNombre: String = "",
    val estado: String = "",
    val motivoRechazo: String = "",
    @ServerTimestamp
    val solicitadaEn: Date? = null,
    val resueltaPor: String = "",
    val resueltaPorNombre: String = "",
    val resueltaEn: Date? = null,
) {
    @get:Exclude
    val estadoVinculacion: EstadoVinculacion get() = EstadoVinculacion.desde(estado) ?: EstadoVinculacion.PENDIENTE

    @get:Exclude
    val aprobada: Boolean get() = estadoVinculacion == EstadoVinculacion.APROBADA
}

/**
 * Datos mínimos de un jugador que un padre de la misma academia puede ver para pedir la
 * vinculación: nombre, equipo y código. Colección `directorio_jugadores/{jugadorId}`, que mantiene
 * el personal al guardar el perfil. Nunca incluye datos físicos, documento ni contactos (criterio 7).
 */
data class DirectorioJugador(
    @DocumentId
    val jugadorId: String = "",
    val academiaId: String = "",
    val equipoId: String = "",
    val equipoNombre: String = "",
    val nombre: String = "",
    val codigo: String = "",
    val activo: Boolean = true,
    /** Apoderados con vínculo aprobado; las reglas impiden que pase de [ReglasVinculacion.MAX_APODERADOS]. */
    val apoderadosAprobados: Int = 0,
)

enum class ProblemaSolicitud(val mensaje: String) {
    MAXIMO_APODERADOS("El jugador ya tiene el máximo de apoderados vinculados"),
    YA_SOLICITADA("Ya tienes una solicitud pendiente para este jugador"),
    YA_VINCULADO("Ya estás vinculado a este jugador"),
    JUGADOR_INACTIVO("Este jugador no está activo en la academia"),
}

/** Reglas de la vinculación padre ↔ jugador (US-007). Sin dependencias de Android ni Firebase. */
object ReglasVinculacion {
    const val MAX_APODERADOS = 2
    const val LONGITUD_CODIGO = 6
    const val MOTIVO_MIN = 10
    const val MOTIVO_MAX = 200

    /** Sin 0/O ni 1/I/L, que se confunden al dictar o copiar el código. */
    const val ALFABETO_CODIGO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"

    fun idVinculacion(padreUid: String, jugadorId: String): String = "${padreUid}_$jugadorId"

    /** Mayúsculas y sin espacios ni guiones: " abc-123 " -> "ABC123". */
    fun normalizarCodigo(texto: String): String = texto.uppercase().filter { it.isLetterOrDigit() }

    /** @return mensaje de error, o `null` si el formato es válido. */
    fun validarCodigo(texto: String): String? {
        val codigo = normalizarCodigo(texto)
        return when {
            codigo.length != LONGITUD_CODIGO -> "El código tiene $LONGITUD_CODIGO caracteres"
            codigo.any { it !in ALFABETO_CODIGO } -> "El código no es válido"
            else -> null
        }
    }

    /** Código de 6 caracteres que no se repite entre los ya usados en la academia. */
    fun generarCodigo(existentes: Set<String>, random: Random = Random.Default): String {
        repeat(1_000) {
            val codigo = (1..LONGITUD_CODIGO).map { ALFABETO_CODIGO[random.nextInt(ALFABETO_CODIGO.length)] }.joinToString("")
            if (codigo !in existentes) return codigo
        }
        error("No se pudo generar un código único")
    }

    /**
     * Comprueba si el padre puede pedir el vínculo (criterio 4). Una solicitud rechazada o revocada
     * se puede volver a enviar.
     */
    fun problemaParaSolicitar(jugador: DirectorioJugador, existente: Vinculacion?): ProblemaSolicitud? = when {
        !jugador.activo -> ProblemaSolicitud.JUGADOR_INACTIVO
        existente?.estadoVinculacion == EstadoVinculacion.APROBADA -> ProblemaSolicitud.YA_VINCULADO
        existente?.estadoVinculacion == EstadoVinculacion.PENDIENTE -> ProblemaSolicitud.YA_SOLICITADA
        jugador.apoderadosAprobados >= MAX_APODERADOS -> ProblemaSolicitud.MAXIMO_APODERADOS
        else -> null
    }

    /** Al aprobar se vuelve a comprobar el máximo, por si otro apoderado fue aprobado mientras tanto. */
    fun puedeAprobar(jugador: DirectorioJugador?): Boolean =
        jugador != null && jugador.apoderadosAprobados < MAX_APODERADOS

    /** El rechazo exige un motivo de 10 a 200 caracteres (criterio 3). */
    fun validarMotivoRechazo(motivo: String): String? =
        if (motivo.trim().length in MOTIVO_MIN..MOTIVO_MAX) {
            null
        } else {
            "El motivo debe tener entre $MOTIVO_MIN y $MOTIVO_MAX caracteres"
        }

    /** Solo las solicitudes pendientes, de la más antigua a la más reciente (orden de atención). */
    fun pendientes(vinculaciones: List<Vinculacion>): List<Vinculacion> =
        vinculaciones.filter { it.estadoVinculacion == EstadoVinculacion.PENDIENTE }
            .sortedBy { it.solicitadaEn?.time ?: Long.MAX_VALUE }

    /** Ids de jugadores con vínculo aprobado: lo único que puede ver el padre (criterios 6 y 7). */
    fun jugadoresAprobados(vinculaciones: List<Vinculacion>): List<String> =
        vinculaciones.filter { it.aprobada }.map { it.jugadorId }.distinct()
}
