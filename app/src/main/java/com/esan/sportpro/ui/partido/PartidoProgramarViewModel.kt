package com.esan.sportpro.ui.partido

import androidx.lifecycle.ViewModel
import com.esan.sportpro.domain.partido.PartidoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject

data class PartidoProgramarUiState(
    val guardando: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class PartidoProgramarViewModel @Inject constructor(
    private val repo: PartidoRepository,
) : ViewModel() {

    fun crearPartido(
        nombreEquipo: String,
        nombreRival: String,
        fechaMs: Long?,
        hora: String,
        lugar: String,
        tipoPartido: String,
        duracionTiempoMin: Int?,
        maxConvocados: Int?,
        fechaLimiteRespuestaMs: Long?,
    ): String? {

        if (nombreEquipo.isBlank()) {
            return null
        }

        if (nombreRival.isBlank()) {
            return null
        }

        if (fechaMs == null) {
            return null
        }

        if (hora.isBlank()) {
            return null
        }

        if (lugar.isBlank()) {
            return null
        }

        if (tipoPartido.isBlank()) {
            return null
        }

        if (duracionTiempoMin == null || duracionTiempoMin <= 0) {
            return null
        }

        if (maxConvocados == null || maxConvocados <= 0) {
            return null
        }

        if (fechaLimiteRespuestaMs == null) {
            return null
        }

        val fechaPartidoCompleta =
            combinarFechaHora(
                fechaMs = fechaMs,
                hora = hora
            ) ?: return null

        val fechaLimiteCompleta =
            finDelDia(
                fechaLimiteRespuestaMs
            )

        if (fechaLimiteCompleta > fechaPartidoCompleta) {
            return null
        }

        return repo.crearPartidoProgramado(
            nombreEquipo = nombreEquipo.trim(),
            nombreRival = nombreRival.trim(),
            fechaMs = fechaPartidoCompleta,
            hora = hora.trim(),
            lugar = lugar.trim(),
            tipoPartido = tipoPartido.trim(),
            duracionTiempoMin = duracionTiempoMin,
            maxConvocados = maxConvocados,
            fechaLimiteRespuestaMs = fechaLimiteCompleta,
        )
    }

    private fun combinarFechaHora(
        fechaMs: Long,
        hora: String,
    ): Long? {

        val partes =
            hora.split(":")

        if (partes.size != 2) {
            return null
        }

        val horas =
            partes[0].toIntOrNull()
                ?: return null

        val minutos =
            partes[1].toIntOrNull()
                ?: return null

        if (horas !in 0..23) {
            return null
        }

        if (minutos !in 0..59) {
            return null
        }

        return Calendar
            .getInstance()
            .apply {
                timeInMillis = fechaMs

                set(
                    Calendar.HOUR_OF_DAY,
                    horas
                )

                set(
                    Calendar.MINUTE,
                    minutos
                )

                set(
                    Calendar.SECOND,
                    0
                )

                set(
                    Calendar.MILLISECOND,
                    0
                )
            }
            .timeInMillis
    }

    private fun finDelDia(
        fechaMs: Long
    ): Long {

        return Calendar
            .getInstance()
            .apply {
                timeInMillis = fechaMs

                set(
                    Calendar.HOUR_OF_DAY,
                    23
                )

                set(
                    Calendar.MINUTE,
                    59
                )

                set(
                    Calendar.SECOND,
                    59
                )

                set(
                    Calendar.MILLISECOND,
                    999
                )
            }
            .timeInMillis
    }
}

