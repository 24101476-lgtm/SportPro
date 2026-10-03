package com.esan.sportpro.domain.mensualidades

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Registro de mensualidad simulada de un jugador (US-008). La colección en Firestore es
 * "mensualidades". Un registro **nunca se elimina ni se edita**: para corregir un error se crea
 * un nuevo documento con [anulaRegistroId] apuntando al registro incorrecto, y este pasa a
 * [vigente] = false — ambos quedan conservados en el historial (criterio de aceptación 6).
 *
 * El campo no-arg y los `var` son requeridos por el mapeo automático de Firestore
 * (`DocumentSnapshot.toObject<Mensualidad>()`).
 */
data class Mensualidad(
    @DocumentId
    val id: String = "",
    val academiaId: String = "",
    val equipoId: String = "",
    val equipoNombre: String = "",
    val jugadorId: String = "",
    val jugadorNombre: String = "",
    /** 1..12 */
    val mes: Int = 1,
    val anio: Int = 2026,
    /** Monto referencial: 0.00..5000.00, dos decimales (criterio de aceptación 4). */
    val montoReferencial: Double = 0.0,
    /** Fecha en que se recibió el pago, ingresada por el administrador al registrar. */
    val fechaRegistro: Date = Date(),
    val medioDeclarado: MedioPago = MedioPago.EFECTIVO,
    /** Hasta 200 caracteres, opcional. */
    val observacion: String? = null,
    /**
     * Estado tal como fue registrado por el administrador. Nunca se persiste [EstadoMensualidad.VENCIDO]
     * directamente: ese estado se calcula en pantalla con [EstadoMensualidad.calcular] a partir de
     * [PENDIENTE][EstadoMensualidad.PENDIENTE] + el día de corte de la academia.
     */
    val estadoRegistrado: EstadoMensualidad = EstadoMensualidad.PENDIENTE,
    val registradoPorUid: String = "",
    /** Marca de tiempo del servidor (auditoría, criterio de aceptación 6) — no editable desde el formulario. */
    @ServerTimestamp
    val marcaDeTiempoAuditoria: Date? = null,
    /** Id del registro anterior que este corrige/anula, o null si es el primer registro del mes. */
    val anulaRegistroId: String? = null,
    /** false cuando este registro fue anulado por uno posterior; se mantiene en el historial. */
    @get:PropertyName("vigente")
    @set:PropertyName("vigente")
    var vigente: Boolean = true,
)
