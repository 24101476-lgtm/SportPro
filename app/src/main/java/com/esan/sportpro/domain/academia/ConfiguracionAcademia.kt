package com.esan.sportpro.domain.academia

import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * US-004: configuración de la academia (colección `academias`, id = academiaId del perfil).
 * Si el documento todavía no existe se usan estos valores por defecto.
 */
data class ConfiguracionAcademia(
    val id: String = "",
    val nombre: String = "",
    /** Día del mes desde el que una mensualidad pendiente pasa a Vencido (US-008, criterio 5). */
    val diaCorte: Int = DIA_CORTE_POR_DEFECTO,
) {
    companion object {
        const val DIA_CORTE_POR_DEFECTO = 10
        const val DIA_CORTE_MAX = 28
        const val NOMBRE_MIN = 3
        const val NOMBRE_MAX = 60

        /** Mensaje de error o `null` si los datos son válidos (las reglas de Firestore exigen lo mismo). */
        fun validar(nombre: String, diaCorte: Int?): String? = when {
            nombre.trim().length !in NOMBRE_MIN..NOMBRE_MAX -> "El nombre debe tener entre $NOMBRE_MIN y $NOMBRE_MAX caracteres"
            diaCorte == null || diaCorte !in 1..DIA_CORTE_MAX -> "El día de corte debe estar entre 1 y $DIA_CORTE_MAX"
            else -> null
        }
    }
}

/** US-004: código de 8 caracteres para registrar una cuenta de Administrador (vigencia de 7 días). */
data class CodigoInvitacion(
    val codigo: String,
    val creadoEn: Date?,
    val expiraEn: Date?,
    val usado: Boolean,
    val revocado: Boolean,
) {
    fun vigente(ahora: Date = Date()): Boolean = !usado && !revocado && (expiraEn == null || expiraEn.after(ahora))
}

/** Configuración de la academia y códigos de invitación. Implementación: `data/academia/FirestoreAcademiaRepository`. */
interface AcademiaRepository {
    fun observarConfiguracion(academiaId: String): Flow<ConfiguracionAcademia>

    /** @return `true` si el servidor confirmó, `false` si quedó pendiente por falta de conexión. */
    suspend fun guardarConfiguracion(configuracion: ConfiguracionAcademia): Boolean

    fun observarCodigos(academiaId: String): Flow<List<CodigoInvitacion>>

    /** Genera un código nuevo (sin caracteres ambiguos) y devuelve su texto. */
    suspend fun generarCodigo(academiaId: String, adminUid: String): String

    suspend fun revocarCodigo(codigo: String): Boolean
}
