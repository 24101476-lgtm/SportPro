package com.esan.sportpro.domain.mensualidades

/** Medio declarado al registrar un pago simulado (US-008, criterio de aceptación 3). */
enum class MedioPago(val etiqueta: String) {
    EFECTIVO("Efectivo"),
    TRANSFERENCIA("Transferencia"),
    OTRO("Otro"),
}
