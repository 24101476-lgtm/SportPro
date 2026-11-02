package com.esan.sportpro.ui.partido

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esan.sportpro.domain.academia.Equipo
import com.esan.sportpro.domain.academia.EquipoRepository
import com.esan.sportpro.domain.cuentas.PerfilActual
import com.esan.sportpro.domain.cuentas.SesionRepository
import com.esan.sportpro.domain.partido.PartidoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class PartidoProgramarUiState(
    val cargando: Boolean = true,
    val perfil: PerfilActual? = null,
    /** Equipos activos de la academia: el partido se programa siempre para un equipo real. */
    val equipos: List<Equipo> = emptyList(),
)

/** Resultado de programar el partido (US-014). */
sealed interface ResultadoProgramar {
    data class Creado(val partidoId: String) : ResultadoProgramar
    data class Error(val mensaje: String) : ResultadoProgramar
}

@HiltViewModel
class PartidoProgramarViewModel @Inject constructor(
    private val repo: PartidoRepository,
    private val sesionRepository: SesionRepository,
    private val equipoRepository: EquipoRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PartidoProgramarUiState())
    val uiState: StateFlow<PartidoProgramarUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val perfil = runCatching { sesionRepository.perfilActual() }.getOrNull()
            _uiState.update { it.copy(perfil = perfil, cargando = perfil != null) }
            if (perfil == null) return@launch
            equipoRepository.observarEquipos(perfil.academiaId)
                .catch { _uiState.update { it.copy(cargando = false) } }
                .collect { lista -> _uiState.update { it.copy(equipos = lista.filter { e -> e.activo }, cargando = false) } }
        }
    }

    fun crearPartido(
        equipoId: String?,
        nombreRival: String,
        fechaMs: Long?,
        hora: String,
        lugar: String,
        tipoPartido: String,
        duracionTiempoMin: Int?,
        maxConvocados: Int?,
        fechaLimiteRespuestaMs: Long?,
    ): ResultadoProgramar {
        val st = _uiState.value
        val perfil = st.perfil ?: return ResultadoProgramar.Error("No se pudo leer tu perfil. Vuelve a iniciar sesión.")
        val equipo = st.equipos.firstOrNull { it.id == equipoId }
            ?: return ResultadoProgramar.Error("Selecciona el equipo")
        val rival = nombreRival.trim()
        if (rival.length < 2) return ResultadoProgramar.Error("Ingresa el equipo rival")
        if (rival.equals(equipo.nombre.trim(), ignoreCase = true)) {
            return ResultadoProgramar.Error("El rival no puede ser tu propio equipo")
        }
        if (fechaMs == null) return ResultadoProgramar.Error("Selecciona la fecha del partido")
        val fechaPartido = combinarFechaHora(fechaMs, hora)
            ?: return ResultadoProgramar.Error("Selecciona la hora del partido")
        if (fechaPartido <= System.currentTimeMillis()) {
            return ResultadoProgramar.Error("La fecha y hora del partido deben ser futuras")
        }
        if (lugar.isBlank()) return ResultadoProgramar.Error("Ingresa el lugar del partido")
        if (duracionTiempoMin == null || duracionTiempoMin !in DURACION_MIN..DURACION_MAX) {
            return ResultadoProgramar.Error("La duración de cada tiempo debe estar entre $DURACION_MIN y $DURACION_MAX minutos")
        }
        if (maxConvocados == null || maxConvocados !in CONVOCADOS_MIN..CONVOCADOS_MAX) {
            return ResultadoProgramar.Error("Valor entre $CONVOCADOS_MIN y $CONVOCADOS_MAX")
        }
        if (fechaLimiteRespuestaMs == null) return ResultadoProgramar.Error("Selecciona la fecha límite de respuesta")

        // La fecha límite cierra al final del día elegido; si es el mismo día del partido, cierra
        // una hora antes del inicio para que el entrenador alcance a armar la alineación.
        val limite = minOf(finDelDia(fechaLimiteRespuestaMs), fechaPartido - UNA_HORA_MS)
        if (limite <= System.currentTimeMillis() || fechaLimiteRespuestaMs > fechaPartido) {
            return ResultadoProgramar.Error("La fecha límite debe ser anterior al partido")
        }

        val id = repo.crearPartidoProgramado(
            nombreEquipo = equipo.nombre.trim(),
            nombreRival = rival,
            fechaMs = fechaPartido,
            hora = hora.trim(),
            lugar = lugar.trim(),
            tipoPartido = tipoPartido.trim(),
            duracionTiempoMin = duracionTiempoMin,
            maxConvocados = maxConvocados,
            fechaLimiteRespuestaMs = limite,
            equipoId = equipo.id,
            academiaId = perfil.academiaId,
            categoria = equipo.categoria,
        )
        return ResultadoProgramar.Creado(id)
    }

    private fun combinarFechaHora(fechaMs: Long, hora: String): Long? {
        val partes = hora.split(":")
        if (partes.size != 2) return null
        val horas = partes[0].toIntOrNull()?.takeIf { it in 0..23 } ?: return null
        val minutos = partes[1].toIntOrNull()?.takeIf { it in 0..59 } ?: return null
        return Calendar.getInstance().apply {
            timeInMillis = fechaMs
            set(Calendar.HOUR_OF_DAY, horas)
            set(Calendar.MINUTE, minutos)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun finDelDia(fechaMs: Long): Long = Calendar.getInstance().apply {
        timeInMillis = fechaMs
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis

    private companion object {
        const val DURACION_MIN = 10
        const val DURACION_MAX = 45
        const val CONVOCADOS_MIN = 7
        const val CONVOCADOS_MAX = 30
        const val UNA_HORA_MS = 60 * 60 * 1000L
    }
}
