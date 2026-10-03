package com.esan.sportpro.domain.anuncios

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Anuncio interno (US-027). La colección en Firestore es "anuncios" (criterio de aceptación 4).
 *
 * El envío real de la notificación push por Firebase Cloud Messaging requiere una Cloud Function
 * con credenciales de servidor, que está fuera del alcance de este módulo cliente; por eso
 * [AnuncioRepository.publicar] simula el envío (ver [com.esan.sportpro.ui.anuncios.NotificacionSimuladaLegend])
 * y solo persiste el documento, igual que el registro simulado de pagos del módulo de
 * Mensualidades (US-008).
 */
data class Anuncio(
    @DocumentId
    val id: String = "",
    val academiaId: String = "",
    /** Solo relevante cuando [destinatarios] == EQUIPO_ESPECIFICO. */
    val equipoId: String? = null,
    /** 5..80 caracteres (criterio de aceptación 2). */
    val titulo: String = "",
    /** 10..500 caracteres (criterio de aceptación 2). */
    val mensaje: String = "",
    val destinatarios: TipoDestinatario = TipoDestinatario.TODO_EL_CLUB,
    val prioridad: PrioridadAnuncio = PrioridadAnuncio.INFORMATIVA,
    val autorUid: String = "",
    val autorNombre: String = "",
    @ServerTimestamp
    val fechaPublicacion: Date? = null,
    /** true si se editó dentro de los primeros 15 minutos (criterio de aceptación 8). */
    val editado: Boolean = false,
    /** Un anuncio archivado ya no puede editarse ni se muestra en la lista activa. */
    val archivado: Boolean = false,
    /** true si fue generado automáticamente por un cambio de horario o reprogramación (criterio 7). */
    val origenAutomatico: Boolean = false,
    /** UIDs de quienes ya marcaron el anuncio como leído (criterio de aceptación 6). */
    val lecturas: List<String> = emptyList(),
    /**
     * Número estimado de destinatarios al momento de publicar, usado para mostrar "X de Y
     * leyeron" en el detalle. TODO(cuentas/academia): calcular el valor real una vez que exista
     * un repositorio de membresías; por ahora puede quedar en 0 si no se puede estimar.
     */
    val destinatariosTotalEstimado: Int = 0,
    /** true si el envío de FCM falló al publicar (criterio de aceptación 10). */
    val envioNotificacionFallido: Boolean = false,
)
