package com.esan.sportpro.ui.mensualidades

import androidx.lifecycle.viewModelScope
import com.esan.sportpro.core.viewmodel.BaseViewModel
import com.esan.sportpro.data.mensualidades.MensualidadRepository
import com.esan.sportpro.data.partido.describirErrorFirestore
import com.esan.sportpro.domain.jugadores.Vinculacion
import com.esan.sportpro.domain.jugadores.VinculacionRepository
import com.esan.sportpro.domain.mensualidades.EstadoMensualidad
import com.esan.sportpro.domain.mensualidades.Mensualidad
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/** Un hijo con vínculo aprobado y sus mensualidades (el mes actual siempre aparece, aunque no tenga pago). */
data class HijoMensualidades(
    val jugadorId: String,
    val nombre: String,
    val equipoNombre: String,
    val meses: List<MensualidadConEstadoEfectivo>,
)

data class PadreMensualidadesUiState(
    /** Se mantiene por compatibilidad: lista plana de todas las mensualidades registradas de los hijos. */
    val mensualidades: List<MensualidadConEstadoEfectivo> = emptyList(),
    val hijos: List<HijoMensualidades> = emptyList(),
    /** Solicitudes de vinculación que el administrador todavía no aprueba. */
    val solicitudesPendientes: Int = 0,
    val isLoading: Boolean = true,
    val sinJugadoresVinculados: Boolean = false,
    val error: String? = null,
)

/**
 * US-008 — Frame 40_Padre_Mensualidades. El padre de familia ve exclusivamente las mensualidades
 * de los jugadores vinculados a su cuenta (criterio de aceptación 8).
 *
 * Usa la misma fuente que la pestaña "Mis hijos" (`observarMisVinculaciones`), en tiempo real:
 * en cuanto el administrador aprueba un vínculo, el hijo aparece aquí sin reabrir la pantalla.
 * Antes se leía una sola vez y cualquier error se tragaba y mostraba "sin jugadores vinculados".
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PadreMensualidadesViewModel @Inject constructor(
    private val repository: MensualidadRepository,
    private val vinculacionRepository: VinculacionRepository,
    private val firebaseAuth: FirebaseAuth,
) : BaseViewModel<PadreMensualidadesUiState, Unit>(PadreMensualidadesUiState()) {

    init {
        cargar()
    }

    private fun cargar() {
        val padreUid = firebaseAuth.currentUser?.uid
        if (padreUid == null) {
            setState { copy(isLoading = false, sinJugadoresVinculados = true) }
            return
        }
        vinculacionRepository.observarMisVinculaciones(padreUid)
            .flatMapLatest { vinculaciones ->
                val aprobadas = vinculaciones.filter { it.aprobada }
                val pendientes = vinculaciones.count { !it.aprobada && it.estado == "PENDIENTE" }
                if (aprobadas.isEmpty()) {
                    flowOf(PadreMensualidadesUiState(isLoading = false, sinJugadoresVinculados = true, solicitudesPendientes = pendientes))
                } else {
                    repository.observarMensualidadesDeJugadores(aprobadas.map { it.jugadorId })
                        .map { pagos -> construir(aprobadas, pagos, pendientes) }
                }
            }
            .onEach { nuevo -> setState { nuevo } }
            .catch { e -> setState { copy(isLoading = false, error = describirErrorFirestore(e)) } }
            .launchIn(viewModelScope)
    }

    private fun construir(aprobadas: List<Vinculacion>, pagos: List<Mensualidad>, pendientes: Int): PadreMensualidadesUiState {
        val hoy = LocalDate.now()
        fun efectivo(m: Mensualidad, sinRegistro: Boolean = false): MensualidadConEstadoEfectivo {
            val diaCorte = 10.coerceAtMost(YearMonth.of(m.anio, m.mes).lengthOfMonth())
            val corte = LocalDate.of(m.anio, m.mes, diaCorte)
            return MensualidadConEstadoEfectivo(m, EstadoMensualidad.calcular(m.estadoRegistrado, corte, hoy), sinRegistro)
        }
        val hijos = aprobadas.sortedBy { it.jugadorNombre.lowercase() }.map { v ->
            val propios = pagos.filter { it.jugadorId == v.jugadorId }
                .sortedWith(compareByDescending<Mensualidad> { it.anio }.thenByDescending { it.mes })
                .map { efectivo(it) }
            val actualRegistrado = propios.any { it.mensualidad.mes == hoy.monthValue && it.mensualidad.anio == hoy.year }
            val meses = if (actualRegistrado) {
                propios
            } else {
                val vacio = Mensualidad(
                    id = "sin-registro-${v.jugadorId}",
                    academiaId = v.academiaId,
                    equipoId = v.equipoId,
                    equipoNombre = v.equipoNombre,
                    jugadorId = v.jugadorId,
                    jugadorNombre = v.jugadorNombre,
                    mes = hoy.monthValue,
                    anio = hoy.year,
                    estadoRegistrado = EstadoMensualidad.PENDIENTE,
                )
                listOf(efectivo(vacio, sinRegistro = true)) + propios
            }
            HijoMensualidades(v.jugadorId, v.jugadorNombre, v.equipoNombre, meses)
        }
        return PadreMensualidadesUiState(
            mensualidades = hijos.flatMap { it.meses },
            hijos = hijos,
            solicitudesPendientes = pendientes,
            isLoading = false,
            sinJugadoresVinculados = false,
        )
    }
}
