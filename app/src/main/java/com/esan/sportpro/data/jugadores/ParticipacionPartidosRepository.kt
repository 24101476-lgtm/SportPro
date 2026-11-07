package com.esan.sportpro.data.jugadores

import android.util.Log
import com.esan.sportpro.domain.entrenamientos.RegistroParticipacion
import com.esan.sportpro.domain.entrenamientos.ReglasHistorial
import com.esan.sportpro.domain.partido.PartidoRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * US-012, criterio 3: participación del jugador en los partidos terminados de su academia.
 *
 * Solo lee lo que ya guarda el módulo de partidos (sin colecciones nuevas): la alineación final
 * (`convocados`), las respuestas a la convocatoria y los eventos, de los que salen los minutos
 * jugados con la misma regla del acta (US-022). Las reglas de Firestore permiten leerlos a
 * cualquier usuario autenticado, así que sirve igual para el personal, el padre y el jugador.
 */
@Singleton
class ParticipacionPartidosRepository @Inject constructor(
    private val partidoRepository: PartidoRepository,
) {

    /** Registros del jugador; lista vacía si no hay partidos o si falla la lectura. */
    suspend fun registros(jugadorId: String, academiaId: String): List<RegistroParticipacion> = runCatching {
        val terminados = partidoRepository.observarPartidos().first()
            .filter { ReglasHistorial.partidoCuenta(it) }
            .filter { it.academiaId.isNullOrBlank() || academiaId.isBlank() || it.academiaId == academiaId }

        coroutineScope {
            terminados.map { partido ->
                async {
                    runCatching {
                        val convocados = partidoRepository.observarConvocados(partido.id).first()
                        val respuestas = partidoRepository.observarRespuestasConvocatoria(partido.id).first()
                        val participa = convocados.any { it.id == jugadorId } || respuestas.any { it.jugadorId == jugadorId }
                        if (!participa) return@runCatching null
                        // Los eventos solo hacen falta para calcular minutos de quien estuvo en la alineación.
                        val eventos = if (convocados.any { it.id == jugadorId }) {
                            partidoRepository.observarEventos(partido.id).first()
                        } else {
                            emptyList()
                        }
                        ReglasHistorial.desdePartido(jugadorId, partido, convocados, respuestas, eventos)
                    }.onFailure { Log.w(TAG, "No se pudo leer el partido ${partido.id}", it) }.getOrNull()
                }
            }.awaitAll().filterNotNull()
        }
    }.onFailure { Log.w(TAG, "No se pudieron leer los partidos", it) }.getOrDefault(emptyList())

    private companion object {
        const val TAG = "ParticipacionPartidos"
    }
}
