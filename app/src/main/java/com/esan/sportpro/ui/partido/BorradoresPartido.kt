package com.esan.sportpro.ui.partido

import com.esan.sportpro.domain.partido.NuevoPartido
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Partidos que el entrenador está armando en "Nuevo partido" → pizarra táctica.
 *
 * Viven solo en memoria: nada se escribe en Firestore hasta que se pulsa "Crear partido".
 * Así, si el entrenador vuelve atrás o cierra la app a mitad del flujo, no quedan partidos
 * a medio crear en la base de datos ni en la lista de otros dispositivos.
 */
@Singleton
class BorradoresPartido @Inject constructor() {

    private val borradores = ConcurrentHashMap<String, NuevoPartido>()

    fun guardar(id: String, partido: NuevoPartido) {
        borradores[id] = partido
    }

    fun obtener(id: String): NuevoPartido? = borradores[id]

    fun quitar(id: String) {
        borradores.remove(id)
    }
}
