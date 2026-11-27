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
import com.esan.sportpro.data.anuncios.AnuncioRepository
import com.esan.sportpro.domain.anuncios.Anuncio
import com.esan.sportpro.domain.partido.EstadoRespuestaConvocatoria
import com.esan.sportpro.navigation.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
    /** US-003: los últimos tres anuncios del club. */
    val ultimosAnuncios: List<Anuncio> = emptyList(),
    /** US-003: convocatorias que esperan respuesta (del propio jugador, o de su plantel si es personal). */
    val convocatoriasPendientes: List<ConvocatoriaPendiente> = emptyList(),
    /** US-003: mientras llegan los primeros datos se muestra un esqueleto en lugar de "vacío". */
    val cargando: Boolean = true,
)

/** Un partido con convocatoria abierta y cuántas respuestas faltan. */
data class ConvocatoriaPendiente(val partido: Partido, val sinResponder: Int)

sealed interface InicioEvent

/** Resumen de la pantalla de Inicio: próximos entrenamientos, próximo partido y conteos. */
@HiltViewModel
class InicioViewModel @Inject constructor(
    private val entrenamientoRepository: EntrenamientoRepository,
    private val partidoRepository: PartidoRepository,
    private val equipoRepository: EquipoRepository,
    private val jugadorRepository: JugadorRepository,
    private val anuncioRepository: AnuncioRepository,
) : BaseViewModel<InicioUiState, InicioEvent>(InicioUiState()) {

    private var iniciado = false

    /** Se llama una vez con el perfil que ya cargó [HomeViewModel]. */
    fun iniciar(perfil: PerfilActual) {
        if (iniciado) return
        iniciado = true
        setState { copy(perfil = perfil) }

        viewModelScope.launch {
            entrenamientoRepository.observarEntrenamientos(perfil.academiaId)
                .catch { setState { copy(cargando = false) } /* el resumen es informativo: si falla, la sección queda vacía */ }
                .collect { lista -> setState { copy(proximosEntrenamientos = proximos(lista), cargando = false) } }
        }
        // US-003: los últimos tres anuncios del club (sin archivados).
        viewModelScope.launch {
            anuncioRepository.observarAnuncios(perfil.academiaId)
                .catch { }
                .collect { lista ->
                    val ultimos = lista.filterNot { it.archivado }
                        .sortedByDescending { it.fechaPublicacion?.time ?: 0L }
                        .take(3)
                    setState { copy(ultimosAnuncios = ultimos) }
                }
        }
        // US-003: convocatorias pendientes de respuesta.
        viewModelScope.launch {
            convocatoriasPendientes(perfil)
                .catch { }
                .collect { lista -> setState { copy(convocatoriasPendientes = lista) } }
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

    /**
     * Jugador: partidos próximos donde su respuesta sigue "sin responder". Personal: partidos
     * próximos con jugadores convocados que aún no responden. Padre: se ve en Mis hijos.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun convocatoriasPendientes(perfil: PerfilActual): Flow<List<ConvocatoriaPendiente>> {
        val partidos = partidoRepository.observarPartidos().map { lista -> conConvocatoria(lista, perfil.academiaId) }
        return when (perfil.rol) {
            UserRole.JUGADOR -> combine(partidos, jugadorRepository.observarFichaPropia(perfil.uid)) { p, ficha -> p to ficha }
                .flatMapLatest { (lista, ficha) ->
                    val suyos = if (ficha == null) emptyList() else lista.filter { it.equipoId == null || it.equipoId == ficha.equipoId }
                    if (ficha == null || suyos.isEmpty()) {
                        flowOf(emptyList<ConvocatoriaPendiente>())
                    } else {
                        combine(
                            suyos.map { partido ->
                                partidoRepository.observarRespuestaConvocatoria(partido.id, ficha.id)
                                    .map { respuesta ->
                                        if (respuesta?.estado == EstadoRespuestaConvocatoria.SIN_RESPONDER) {
                                            ConvocatoriaPendiente(partido, 1)
                                        } else {
                                            null
                                        }
                                    }
                                    .catch { emit(null) }
                            },
                        ) { respuestas -> respuestas.filterNotNull() }
                    }
                }

            UserRole.ADMINISTRADOR, UserRole.ENTRENADOR -> partidos.flatMapLatest { lista ->
                if (lista.isEmpty()) {
                    flowOf(emptyList<ConvocatoriaPendiente>())
                } else {
                    combine(
                        lista.map { partido ->
                            partidoRepository.observarRespuestasConvocatoria(partido.id)
                                .map { respuestas ->
                                    val faltan = respuestas.count { it.estado == EstadoRespuestaConvocatoria.SIN_RESPONDER }
                                    if (faltan > 0) ConvocatoriaPendiente(partido, faltan) else null
                                }
                                .catch { emit(null) }
                        },
                    ) { pendientes -> pendientes.filterNotNull() }
                }
            }

            UserRole.PADRE_DE_FAMILIA -> flowOf(emptyList<ConvocatoriaPendiente>())
        }
    }

    /** Partidos programados desde hoy de la academia, los 5 más cercanos. */
    private fun conConvocatoria(lista: List<Partido>, academiaId: String): List<Partido> {
        val inicioDeHoy = FechasSportPro.aMillis(FechasSportPro.hoy())
        return lista
            .filter { it.academiaId == null || it.academiaId == academiaId }
            .filter { it.estado == EstadoPartido.PROGRAMADO }
            .filter { (it.fechaMs ?: Long.MAX_VALUE) >= inicioDeHoy }
            .sortedBy { it.fechaMs ?: Long.MAX_VALUE }
            .take(5)
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
