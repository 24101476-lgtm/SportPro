package com.esan.sportpro.ui.partido

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.esan.sportpro.core.red.ConectividadObserver
import com.esan.sportpro.core.viewmodel.BaseViewModel
import com.esan.sportpro.data.partido.AccesoEspectador
import com.esan.sportpro.data.partido.describirErrorFirestore
import com.esan.sportpro.domain.cuentas.PerfilActual
import com.esan.sportpro.domain.cuentas.SesionRepository
import com.esan.sportpro.domain.partido.CatalogoBase
import com.esan.sportpro.domain.partido.EstadoPartido
import com.esan.sportpro.domain.partido.EstadisticasPartido
import com.esan.sportpro.domain.partido.EventoPartido
import com.esan.sportpro.domain.partido.JugadorConvocado
import com.esan.sportpro.domain.partido.JugadorEnAlineacion
import com.esan.sportpro.domain.partido.Partido
import com.esan.sportpro.domain.partido.PartidoRepository
import com.esan.sportpro.domain.partido.ReglasPartido
import com.esan.sportpro.domain.ia.ResumenPartido
import com.esan.sportpro.domain.ia.RevisionResumenRepository
import com.esan.sportpro.domain.partido.ReglasSeguimiento
import com.esan.sportpro.domain.partido.TipoEvento
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Resultado de comprobar si el usuario puede seguir este partido. */
enum class AccesoSeguimiento { CARGANDO, PERMITIDO, DENEGADO, NO_ENCONTRADO }

data class SeguimientoUiState(
    val cargando: Boolean = true,
    val partido: Partido? = null,
    val eventos: List<EventoPartido> = emptyList(),
    val convocados: List<JugadorConvocado> = emptyList(),
    val catalogo: List<TipoEvento> = CatalogoBase.tipos,
    val perfil: PerfilActual? = null,
    /** Equipos que el usuario puede seguir; `null` = sin restricción (administrador y entrenador). */
    val equiposPermitidos: Set<String>? = null,
    val equiposResueltos: Boolean = false,
    /** true cuando ya llegó la primera lista de eventos: antes no se animan entradas ni goles. */
    val eventosCargados: Boolean = false,
    val conectado: Boolean = true,
    /** US-024: resumen aprobado del partido, o null si todavía no hay uno publicado. */
    val resumenPublicado: ResumenPartido? = null,
    /** Última vez que llegó información con conexión (criterio 5); null si aún no llegó ninguna. */
    val ultimaActualizacionMs: Long? = null,
    val ahoraMs: Long = System.currentTimeMillis(),
    val error: String? = null,
) {
    val acceso: AccesoSeguimiento
        get() {
            val p = partido
            val usuario = perfil
            return when {
                p == null -> if (cargando) AccesoSeguimiento.CARGANDO else AccesoSeguimiento.NO_ENCONTRADO
                !equiposResueltos -> AccesoSeguimiento.CARGANDO
                usuario == null -> AccesoSeguimiento.DENEGADO
                ReglasSeguimiento.puedeSeguir(usuario.rol, p, equiposPermitidos) -> AccesoSeguimiento.PERMITIDO
                else -> AccesoSeguimiento.DENEGADO
            }
        }

    val reloj: ReglasPartido.Reloj
        get() = partido?.let { ReglasPartido.reloj(it, ahoraMs) } ?: ReglasPartido.Reloj(0, 0, 0, false, 0f)

    val marcador: ReglasPartido.Marcador get() = ReglasPartido.marcador(eventos, catalogo)
    val cronologia: List<EventoPartido> get() = ReglasSeguimiento.cronologiaEspectador(eventos)
    val estadisticas: EstadisticasPartido get() = ReglasSeguimiento.estadisticas(eventos)
    val alineacion: List<JugadorEnAlineacion> get() = ReglasSeguimiento.alineacion(convocados, eventos)
}

/**
 * US-021: seguimiento del partido en tiempo real para jugador y padre. Solo lee: escucha el
 * partido, sus eventos y su alineación con listeners de Firestore y no expone ninguna acción
 * de registro, edición o anulación.
 */
@HiltViewModel
class SeguimientoPartidoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: PartidoRepository,
    private val sesionRepository: SesionRepository,
    private val accesoEspectador: AccesoEspectador,
    private val conectividad: ConectividadObserver,
    private val revisionRepository: RevisionResumenRepository,
) : BaseViewModel<SeguimientoUiState, Unit>(SeguimientoUiState()) {

    val partidoId: String = checkNotNull(savedStateHandle["partidoId"])

    init {
        viewModelScope.launch {
            val perfil = runCatching { sesionRepository.perfilActual() }.getOrNull()
            setState { copy(perfil = perfil) }
            if (perfil == null) {
                setState { copy(equiposResueltos = true) }
                return@launch
            }
            accesoEspectador.equiposPermitidos(perfil)
                .catch { e -> setState { copy(equiposPermitidos = emptySet(), equiposResueltos = true, error = describirErrorFirestore(e)) } }
                .collect { equipos -> setState { copy(equiposPermitidos = equipos, equiposResueltos = true) } }
        }
        viewModelScope.launch {
            repo.observarPartido(partidoId)
                .catch { e -> setState { copy(cargando = false, error = describirErrorFirestore(e)) } }
                .collect { p ->
                    setState { copy(partido = p, cargando = false, error = null) }
                    marcarActualizacion()
                }
        }
        viewModelScope.launch {
            repo.observarEventos(partidoId)
                .catch { e -> setState { copy(error = describirErrorFirestore(e)) } }
                .collect { lista ->
                    setState { copy(eventos = lista, eventosCargados = true) }
                    marcarActualizacion()
                }
        }
        viewModelScope.launch {
            repo.observarConvocados(partidoId)
                .catch { /* sin alineación: la pestaña lo indica */ }
                .collect { setState { copy(convocados = it) } }
        }
        viewModelScope.launch {
            repo.observarCatalogo()
                .catch { /* se mantiene el catálogo base */ }
                .collect { setState { copy(catalogo = it) } }
        }
        viewModelScope.launch {
            revisionRepository.observarPublicado(partidoId)
                .catch { /* sin resumen publicado la pestaña lo indica */ }
                .collect { setState { copy(resumenPublicado = it) } }
        }
        viewModelScope.launch {
            conectividad.conectado.collect { setState { copy(conectado = it) } }
        }
        viewModelScope.launch {
            delay(TIEMPO_ESPERA_MS)
            if (currentState.cargando) {
                setState { copy(cargando = false, error = "No hay respuesta todavía. Revisa tu conexión a internet.") }
            }
        }
        viewModelScope.launch {
            // El reloj solo corre mientras el partido está en juego: no despierta la pantalla en vano.
            while (isActive) {
                delay(1_000)
                if (currentState.partido?.estado == EstadoPartido.EN_CURSO) {
                    setState { copy(ahoraMs = System.currentTimeMillis()) }
                }
            }
        }
    }

    /** Solo cuenta como actualización la información recibida mientras hay conexión. */
    private fun marcarActualizacion() {
        if (currentState.conectado) {
            setState { copy(ultimaActualizacionMs = System.currentTimeMillis()) }
        }
    }

    private companion object {
        const val TIEMPO_ESPERA_MS = 8_000L
    }
}
