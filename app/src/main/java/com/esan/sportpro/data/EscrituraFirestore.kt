package com.esan.sportpro.data

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Espera la confirmación del servidor para una escritura de Firestore, pero sin colgar la
 * interfaz cuando no hay conexión: con la persistencia offline activa la escritura ya quedó en
 * la caché local y se enviará sola al volver la red.
 *
 * @return `true` si el servidor confirmó, `false` si quedó pendiente de sincronizar. Los errores
 * reales (por ejemplo reglas de seguridad que rechazan la escritura) se lanzan como excepción.
 */
suspend fun <T> Task<T>.esperarConfirmacion(timeoutMs: Long = 4_000): Boolean =
    withTimeoutOrNull(timeoutMs) {
        await()
        true
    } ?: false
