package com.esan.sportpro.ui.mensualidades

import androidx.lifecycle.viewModelScope
import com.esan.sportpro.core.viewmodel.BaseViewModel
import com.esan.sportpro.data.mensualidades.MensualidadRepository
import com.esan.sportpro.data.partido.describirErrorFirestore
import com.esan.sportpro.domain.cuentas.SesionRepository
import com.esan.sportpro.domain.mensualidades.EstadoMensualidad
import com.esan.sportpro.domain.mensualidades.Mensualidad
import com.esan.sportpro.navigation.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/** Conteo de los registros vigentes de un jugador por estado efectivo (Vencido calculado). */
data class ResumenHistorial(
    val pagadas: Int = 0,
    val pendientes: Int = 0,
    val vencidas: Int = 0,
    val exoneradas: Int = 0,
    /** Suma de los pagos vigentes en estado Pagado (referencial, simulado). */
    val totalPagado: Double = 0.0,
)

data class MensualidadHistorialUiState(
    val jugadorId: String = "",
    val jugadorNombre: String = "",
    val equipoNombre: String = "",
    val registros: List<Mensualidad> = emptyList(),
    val resumen: ResumenHistorial = ResumenHistorial(),
    /** Solo el administrador ve montos (criterio de aceptación 7). */
    val puedeVerMontos: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

/**
 * US-008 — Frame 39_Mensualidad_Historial. Muestra **todos** los registros (vigentes y anulados)
 * de un jugador, más reciente primero, de forma que la corrección de un pago quede trazable
 * (criterio de aceptación 6: ningún registro se elimina).
 *
 * El módulo navega internamente por estado (ver [MensualidadesEntryScreen]) en vez de usar
 * argumentos de Navigation-Compose, así que [cargar] se invoca explícitamente desde la pantalla.
 */
@HiltViewModel
class MensualidadHistorialViewModel @Inject constructor(
    private val repository: MensualidadRepository,
    private val sesionRepository: SesionRepository,
    private val academiaRepository: com.esan.sportpro.domain.academia.AcademiaRepository,
) : BaseViewModel<MensualidadHistorialUiState, Unit>(MensualidadHistorialUiState()) {

    /** Día de corte de la academia (US-004). */
    private var diaCorte = EstadoMensualidad.DIA_CORTE

    private var observacionJob: Job? = null

    fun cargar(jugadorId: String, jugadorNombre: String) {
        if (currentState.jugadorId == jugadorId && !currentState.isLoading) return
        setState { copy(jugadorId = jugadorId, jugadorNombre = jugadorNombre, isLoading = true, errorMessage = null) }
        observacionJob?.cancel()
        observacionJob = viewModelScope.launch {
            val perfil = sesionRepository.perfilActual()
            perfil?.let {
                diaCorte = kotlinx.coroutines.withTimeoutOrNull(5_000) {
                    academiaRepository.observarConfiguracion(it.academiaId).first().diaCorte
                } ?: diaCorte
            }
            val rol = perfil?.rol
            setState { copy(puedeVerMontos = rol == UserRole.ADMINISTRADOR) }
            // Administrador y entrenador: sus reglas comparan la academia, así que la consulta la filtra.
            val academia = perfil?.academiaId?.takeIf { perfil.esStaff }
            repository.observarHistorialDeJugador(jugadorId, academia)
                .catch { e -> setState { copy(isLoading = false, errorMessage = describirErrorFirestore(e)) } }
                .collect { registros ->
                    val vigentes = registros.filter { it.vigente }
                    setState {
                        copy(
                            registros = registros,
                            resumen = resumir(vigentes),
                            equipoNombre = registros.firstOrNull { it.equipoNombre.isNotBlank() }?.equipoNombre.orEmpty(),
                            isLoading = false,
                            errorMessage = null,
                        )
                    }
                }
        }
    }

    private fun resumir(vigentes: List<Mensualidad>): ResumenHistorial {
        var pagadas = 0
        var pendientes = 0
        var vencidas = 0
        var exoneradas = 0
        var total = 0.0
        vigentes.forEach { m ->
            when (EstadoMensualidad.efectivo(m.estadoRegistrado, m.mes, m.anio, diaCorte = diaCorte)) {
                EstadoMensualidad.PAGADO -> { pagadas++; total += m.montoReferencial }
                EstadoMensualidad.PENDIENTE -> pendientes++
                EstadoMensualidad.VENCIDO -> vencidas++
                EstadoMensualidad.EXONERADO -> exoneradas++
            }
        }
        return ResumenHistorial(pagadas, pendientes, vencidas, exoneradas, total)
    }
}
