package com.esan.sportpro.ui.estadisticas

import kotlinx.coroutines.flow.first
import com.esan.sportpro.domain.cuentas.SesionRepository
import com.esan.sportpro.domain.cuentas.PerfilActual
import com.esan.sportpro.domain.academia.EquipoRepository
import com.esan.sportpro.domain.academia.Equipo
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
     * Se toma del perfil autenticado; hasta cargarlo se asume el rol de menor privilegio.
     */
    val rolActual: UserRole = UserRole.JUGADOR,
    /** Equipos que el usuario puede consultar (el padre, solo los de sus hijos vinculados). */
    val equipos: List<Equipo> = emptyList(),
    val equipoSeleccionadoId: String? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
) {
    val sinPartidosEnPeriodo: Boolean
        get() = !isLoading && resumenEquipo.partidosJugados == 0

    val sinEquipos: Boolean get() = !isLoading && equipos.isEmpty()
}

/** US-026 — Frames 121 a 125 (Estadísticas de equipo y jugadores). */
@HiltViewModel
class EstadisticasViewModel @Inject constructor(
    private val repository: StatsRepository,
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val sesionRepository: SesionRepository,
    private val equipoRepository: EquipoRepository,
) : BaseViewModel<EstadisticasUiState, Unit>(EstadisticasUiState()) {

    /** Equipo real elegido (antes era un valor fijo "equipo-demo" que no existía en Firestore). */
    private val equipoIdActual: String get() = currentState.equipoSeleccionadoId.orEmpty()

    init {
        viewModelScope.launch {
            val perfil = sesionRepository.perfilActual()
            if (perfil != null) setState { copy(rolActual = perfil.rol) }
            val equipos = perfil?.let { equiposVisibles(it) }.orEmpty()
            setState { copy(equipos = equipos, equipoSeleccionadoId = equipos.firstOrNull()?.id) }
            if (equipos.isEmpty()) {
                setState { copy(isLoading = false) }
                return@launch
            }
            cargarTemporadas()
            recargar()
        }
    }

    fun seleccionarEquipo(equipoId: String) {
        if (equipoId == currentState.equipoSeleccionadoId) return
        setState { copy(equipoSeleccionadoId = equipoId, isLoading = true) }
        cargarTemporadas()
        recargar()
    }

    /** Equipos activos de la academia; para el padre, solo los equipos de sus hijos vinculados. */
    private suspend fun equiposVisibles(perfil: PerfilActual): List<Equipo> {
        val activos = runCatching { equipoRepository.observarEquipos(perfil.academiaId).first() }
            .getOrDefault(emptyList())
            .filter { it.activo }
        if (perfil.rol != UserRole.PADRE_DE_FAMILIA) return activos
        val equiposDeHijos = runCatching {
            firestore.collection("vinculaciones")
                .whereEqualTo("padreUid", perfil.uid)
                .whereEqualTo("estado", "APROBADA")
                .get()
                .await()
                .documents
                .mapNotNull { it.getString("equipoId") }
                .toSet()
        }.getOrDefault(emptySet())
        return activos.filter { it.id in equiposDeHijos }
    }

    private fun cargarTemporadas() {
        if (equipoIdActual.isEmpty()) return
        viewModelScope.launch {
            val temporadas = runCatching { repository.obtenerTemporadasDisponibles(equipoIdActual) }.getOrDefault(emptyList())
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
        if (equipoIdActual.isEmpty()) return
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
     * (colección `vinculaciones`, solo vínculos con estado APROBADA — US-007).
     */
    private suspend fun jugadorIdsPermitidosParaRolActual(): List<String>? {
        if (currentState.rolActual != UserRole.PADRE_DE_FAMILIA) return null
        val padreUid = firebaseAuth.currentUser?.uid ?: return emptyList()
        return runCatching {
            firestore.collection("vinculaciones")
                .whereEqualTo("padreUid", padreUid)
                // US-007: solo los vínculos aprobados (no los pendientes, rechazados ni revocados).
                .whereEqualTo("estado", "APROBADA")
                .get()
                .await()
                .documents
                .mapNotNull { it.getString("jugadorId") }
        }.getOrDefault(emptyList())
    }
}
