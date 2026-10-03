package com.esan.sportpro.domain.comunidad

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Aviso de prueba o convocatoria abierta (US-029). Colección Firestore: "avisos".
 *
 * TODO(US-004/005, academia): [academiaVerificada] hoy se calcula al momento de publicar a
 * partir de un valor que el cliente asume `true` (criterio 5: "solo pueden publicar ... usuarios
 * que pertenezcan a una academia registrada y verificada"); cuando exista el módulo de Academia
 * con su propio campo de verificación, [AvisoRepository.publicar] debe leerlo de ahí y rechazar
 * la publicación si la academia no está verificada, en vez de confiar en el cliente.
 */
data class Aviso(
    @DocumentId
    val id: String = "",
    /** 5..80 caracteres. */
    val titulo: String = "",
    val academiaId: String = "",
    val academiaNombre: String = "",
    val academiaEscudoUrl: String? = null,
    val categoria: String = "",
    val posicionesBuscadas: List<String> = emptyList(),
    /** Fecha de la prueba; debe ser posterior a la fecha de publicación (criterio 2). */
    val fechaPrueba: Date? = null,
    val hora: String = "",
    val ubicacionDistrito: String = "",
    val ubicacionReferencia: String = "",
    val requisitos: String = "",
    /** Correo o teléfono institucional del club; nunca un contacto personal (criterio 4). */
    val contactoInstitucional: String = "",
    val autorUid: String = "",
    @ServerTimestamp
    val fechaPublicacion: Date? = null,
    val reportesCount: Int = 0,
    /** Oculto automáticamente al acumular 3 reportes válidos (criterio 9), o por un moderador. */
    val oculto: Boolean = false,
) {
    /** El aviso expira automáticamente al día siguiente de la fecha de la prueba (criterio 8). */
    fun expiradoRespectoDe(ahora: Date): Boolean {
        val fecha = fechaPrueba ?: return false
        val limite = Date(fecha.time + MILISEGUNDOS_UN_DIA)
        return ahora.after(limite)
    }

    private companion object {
        const val MILISEGUNDOS_UN_DIA = 24L * 60 * 60 * 1000
    }
}
