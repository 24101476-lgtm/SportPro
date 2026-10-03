package com.esan.sportpro.ui.estadisticas

import androidx.lifecycle.viewModelScope
import com.esan.sportpro.core.viewmodel.BaseViewModel
import com.esan.sportpro.data.estadisticas.StatsRepository
import com.esan.sportpro.domain.estadisticas.ColumnaEstadisticaJugador
import com.esan.sportpro.domain.estadisticas.EstadisticaEquipo
import com.esan.sportpro.domain.estadisticas.EstadisticaJugador
import com.esan.sportpro.domain.estadisticas.FiltroEstadisticas
import com.esan.sportpro.domain.estadisticas.GolesPorPartidoPunto
import com.esan.sportpro.navigation.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class EstadisticasUiState(
    val filtro: FiltroEstadisticas = FiltroEstadisticas(),
    val temporadasDisponibles: List<String> = emptyList(),
    val resumenEquipo: EstadisticaEquipo = EstadisticaEquipo(),
    val golesUltimosPartidos: List<GolesPorPartidoPunto> = emptyList(),
    val jugadores: List<EstadisticaJugador> = emptyList(),
    val columnaOrden: ColumnaEstadisticaJugador = ColumnaEstadisticaJugador.JUGADOR,
    val ordenAscendente: Boolean = true,
    /**
     * US-026, criterio 8: un padre de familia solo ve las estadísticas de su(s) jugador(es)
     * vinculado(s) y las del equipo. `null` = sin restricción (Entrenador/Administrador).
     * TODO(US-003): reemplazar [rolActual] por el rol real del usuario autenticado.
     */
    val rolActual: UserRole = UserRole.ENTRENADOR,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
) {
    val sinPartidosEnPeriodo: Boolean
        get() = !isLoading && resumenEquipo.partidosJugados == 0
}

/** US-026 — Frames 121 a 125 (Estadísticas de equipo y jugadores). */
@HiltViewModel
class EstadisticasViewModel @Inject constructor(
    private val repository: StatsRepository,
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
) : BaseViewModel<EstadisticasUiState, Unit>(EstadisticasUiState()) {

    // TODO(academia/equipo): reemplazar por el equipoId real del usuario autenticado.
    private val equipoIdActual = "equipo-demo"

    init {
        cargarTemporadas()
        recargar()
    }

    private fun cargarTemporadas() {
        viewModelScope.launch {
            val temporadas = repository.obtenerTemporadasDisponibles(equipoIdActual)
            setState { copy(temporadasDisponibles = temporadas) }
        }
    }

    fun aplicarFiltro(nuevo: FiltroEstadisticas) {
        setState { copy(filtro = nuevo, isLoading = true) }
        recargar()
    }

    fun limpiarFiltros() = aplicarFiltro(FiltroEstadisticas())

    fun cambiarOrden(columna: ColumnaEstadisticaJugador) {
        val estado = currentState
        val ascendente = if (estado.columnaOrden == columna) !estado.ordenAscendente else true
        setState {
            copy(
                columnaOrden = columna,
                ordenAscendente = ascendente,
                jugadores = repository.ordenarJugadores(jugadores, columna, ascendente),
            )
        }
    }

    private fun recargar() {
        viewModelScope.launch {
            val jugadorIdsPermitidos = jugadorIdsPermitidosParaRolActual()
            val filtro = currentState.filtro
            val equipo = repository.obtenerEstadisticaEquipo(equipoIdActual, filtro)
            val goles = repository.obtenerGolesUltimosPartidos(equipoIdActual)
            val jugadores = repository.obtenerEstadisticasJugadores(equipoIdActual, filtro, jugadorIdsPermitidos)
            val ordenados = repository.ordenarJugadores(jugadores, currentState.columnaOrden, currentState.ordenAscendente)
            setState {
                copy(
                    resumenEquipo = equipo,
                    golesUltimosPartidos = goles,
                    jugadores = ordenados,
                    isLoading = false,
                    errorMessage = null,
                )
            }
        }
    }

    /**
     * Devuelve la lista de jugadorId visibles para el usuario actual, o `null` si no hay
     * restricción. Reutiliza el mismo criterio de vinculación padre-jugador que US-008
     * (colección `vinculaciones`) — ver TODO(US-007) en `PadreMensualidadesViewModel`.
     */
    private suspend fun jugadorIdsPermitidosParaRolActual(): List<String>? {
        if (currentState.rolActual != UserRole.PADRE_DE_FAMILIA) return null
        val padreUid = firebaseAuth.currentUser?.uid ?: return emptyList()
        return runCatching {
            firestore.collection("vinculaciones")
                .whereEqualTo("padreUid", padreUid)
                .get()
                .await()
                .documents
                .mapNotNull { it.getString("jugadorId") }
        }.getOrDefault(emptyList())
    }
}
