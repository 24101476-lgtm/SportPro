package com.esan.sportpro.ui.home

import androidx.lifecycle.viewModelScope
import com.esan.sportpro.core.fecha.FechasSportPro
import com.esan.sportpro.core.viewmodel.BaseViewModel
import com.esan.sportpro.domain.academia.EquipoRepository
import com.esan.sportpro.domain.cuentas.PerfilActual
import com.esan.sportpro.domain.entrenamientos.Entrenamiento
import com.esan.sportpro.domain.entrenamientos.EntrenamientoRepository
import com.esan.sportpro.domain.entrenamientos.EstadoSesion
import com.esan.sportpro.domain.jugadores.Jugador
import com.esan.sportpro.domain.jugadores.JugadorRepository
import com.esan.sportpro.domain.jugadores.ReglasApoderado
import com.esan.sportpro.domain.partido.EstadoPartido
import com.esan.sportpro.domain.partido.Partido
import com.esan.sportpro.domain.partido.PartidoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InicioUiState(
    val perfil: PerfilActual? = null,
    val proximosEntrenamientos: List<Entrenamiento> = emptyList(),
    val proximoPartido: Partido? = null,
    /** Solo para administrador y entrenador (los demás roles no leen el plantel). */
    val equiposActivos: Int? = null,
    val jugadoresActivos: Int? = null,
    /** Menores registrados que todavía esperan un apoderado vinculado. */
    val pendientesApoderado: Int? = null,
)

sealed interface InicioEvent

/** Resumen de la pantalla de Inicio: próximos entrenamientos, próximo partido y conteos. */
@HiltViewModel
class InicioViewModel @Inject constructor(
    private val entrenamientoRepository: EntrenamientoRepository,
    private val partidoRepository: PartidoRepository,
    private val equipoRepository: EquipoRepository,
    private val jugadorRepository: JugadorRepository,
) : BaseViewModel<InicioUiState, InicioEvent>(InicioUiState()) {

    private var iniciado = false

    /** Se llama una vez con el perfil que ya cargó [HomeViewModel]. */
    fun iniciar(perfil: PerfilActual) {
        if (iniciado) return
        iniciado = true
        setState { copy(perfil = perfil) }

        viewModelScope.launch {
            entrenamientoRepository.observarEntrenamientos(perfil.academiaId)
                .catch { /* el resumen es informativo: si falla, la sección queda vacía */ }
                .collect { lista -> setState { copy(proximosEntrenamientos = proximos(lista)) } }
        }
        viewModelScope.launch {
            partidoRepository.observarPartidos()
                .catch { }
                .collect { lista -> setState { copy(proximoPartido = proximoPartido(lista, perfil.academiaId)) } }
        }
        if (perfil.esStaff) {
            viewModelScope.launch {
                equipoRepository.observarEquipos(perfil.academiaId)
                    .catch { }
                    .collect { lista -> setState { copy(equiposActivos = lista.count { it.activo }) } }
            }
            viewModelScope.launch {
                jugadorRepository.observarJugadores(perfil.academiaId)
                    .catch { }
                    .collect { lista -> setState { copy(jugadoresActivos = activos(lista), pendientesApoderado = pendientes(lista)) } }
            }
        }
    }

    private fun proximos(lista: List<Entrenamiento>): List<Entrenamiento> {
        val hoy = FechasSportPro.hoy()
        return lista
            .filter { it.estadoSesion == EstadoSesion.PROGRAMADA }
            .filter { FechasSportPro.desdeIso(it.fecha)?.let { fecha -> !fecha.isBefore(hoy) } == true }
            .sortedBy { FechasSportPro.aMillis(it.fecha, it.hora) ?: Long.MAX_VALUE }
            .take(3)
    }

    private fun proximoPartido(lista: List<Partido>, academiaId: String): Partido? {
        val inicioDeHoy = FechasSportPro.aMillis(FechasSportPro.hoy())
        return lista
            .filter { it.academiaId == null || it.academiaId == academiaId }
            .filter { it.estado == EstadoPartido.PROGRAMADO || it.estado == EstadoPartido.EN_CURSO }
            .filter { (it.fechaMs ?: Long.MAX_VALUE) >= inicioDeHoy }
            .minByOrNull { it.fechaMs ?: Long.MAX_VALUE }
    }

    private fun activos(lista: List<Jugador>) = lista.count { it.activo && !ReglasApoderado.pendiente(it) }

    private fun pendientes(lista: List<Jugador>) = lista.count { it.activo && ReglasApoderado.pendiente(it) }
}
