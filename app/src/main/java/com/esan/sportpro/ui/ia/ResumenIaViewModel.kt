package com.esan.sportpro.ui.ia

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.esan.sportpro.core.red.ConectividadObserver
import com.esan.sportpro.core.viewmodel.BaseViewModel
import com.esan.sportpro.domain.cuentas.PerfilActual
import com.esan.sportpro.domain.cuentas.SesionRepository
import com.esan.sportpro.domain.ia.BloquesResumen
import com.esan.sportpro.domain.ia.ConstructorInsumo
import com.esan.sportpro.domain.ia.ErrorResumen
import com.esan.sportpro.domain.ia.EstadoResumen
import com.esan.sportpro.domain.ia.GeneradorResumen
import com.esan.sportpro.domain.ia.ReglasResumen
import com.esan.sportpro.domain.ia.ResumenPartido
import com.esan.sportpro.domain.ia.ResumenRepository
import com.esan.sportpro.domain.partido.CatalogoBase
import com.esan.sportpro.domain.partido.EventoPartido
import com.esan.sportpro.domain.partido.JugadorConvocado
import com.esan.sportpro.domain.partido.Partido
import com.esan.sportpro.domain.partido.PartidoRepository
import com.esan.sportpro.domain.partido.TipoEvento
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResumenIaUiState(
    val cargando: Boolean = true,
    val perfil: PerfilActual? = null,
    val partido: Partido? = null,
    val eventos: List<EventoPartido> = emptyList(),
    val convocados: List<JugadorConvocado> = emptyList(),
    val catalogo: List<TipoEvento> = CatalogoBase.tipos,
    val resumenes: List<ResumenPartido> = emptyList(),
    /** Versión que se está viendo; null = la más reciente. */
    val versionElegidaId: String? = null,
    val conectado: Boolean = true,
    val generando: Boolean = false,
    /** Mensaje del último intento fallido; se muestra con la acción Reintentar (criterio 10). */
    val error: String? = null,
) {
    val resumenMostrado: ResumenPartido?
        get() = resumenes.firstOrNull { it.id == versionElegidaId } ?: resumenes.firstOrNull()

    /** Por qué no se puede generar ahora, o null si se puede (criterios 1, 8 y conexión). */
    val motivoNoGenerar: String?
        get() {
            val p = partido ?: return "Cargando el partido…"
            if (perfil?.esStaff != true) return "Solo el entrenador o el administrador pueden generar el resumen"
            return ReglasResumen.motivoNoGenerar(p, eventos, conectado)
        }
}

/**
 * US-023: genera el resumen narrativo del partido. Arma el insumo con alias en lugar de nombres,
 * lo envía a la función intermedia, verifica la respuesta contra los eventos, repone los nombres
 * de pila y guarda la generación como una versión nueva en estado Borrador.
 */
@HiltViewModel
class ResumenIaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val partidoRepository: PartidoRepository,
    private val resumenRepository: ResumenRepository,
    private val generador: GeneradorResumen,
    private val sesionRepository: SesionRepository,
    private val conectividad: ConectividadObserver,
) : BaseViewModel<ResumenIaUiState, Unit>(ResumenIaUiState()) {

    val partidoId: String = checkNotNull(savedStateHandle["partidoId"])

    init {
        viewModelScope.launch {
            val perfil = runCatching { sesionRepository.perfilActual() }.getOrNull()
            setState { copy(perfil = perfil) }
        }
        viewModelScope.launch {
            partidoRepository.observarPartido(partidoId)
                .catch { setState { copy(cargando = false) } }
                .collect { setState { copy(partido = it, cargando = false) } }
        }
        viewModelScope.launch {
            partidoRepository.observarEventos(partidoId)
                .catch { /* sin eventos no se puede generar: el motivo lo indica */ }
                .collect { setState { copy(eventos = it) } }
        }
        viewModelScope.launch {
            partidoRepository.observarConvocados(partidoId)
                .catch { }
                .collect { setState { copy(convocados = it) } }
        }
        viewModelScope.launch {
            partidoRepository.observarCatalogo()
                .catch { }
                .collect { setState { copy(catalogo = it) } }
        }
        viewModelScope.launch {
            resumenRepository.observarResumenes(partidoId)
                .catch { /* sin permiso o sin red: se muestra lo que haya */ }
                .collect { setState { copy(resumenes = it) } }
        }
        viewModelScope.launch {
            conectividad.conectado.collect { setState { copy(conectado = it) } }
        }
    }

    fun elegirVersion(id: String) = setState { copy(versionElegidaId = id) }

    fun generar() {
        val st = currentState
        val partido = st.partido ?: return
        val perfil = st.perfil ?: return
        if (st.generando) return
        st.motivoNoGenerar?.let { motivo ->
            setState { copy(error = motivo) }
            return
        }

        setState { copy(generando = true, error = null) }
        viewModelScope.launch {
            try {
                val insumo = ConstructorInsumo.construir(partido, st.eventos, st.convocados, st.catalogo)
                val salida = generador.generar(partidoId, insumo)

                val bloquesIa = ReglasResumen.armarBloques(salida)
                if (!ReglasResumen.dentroDelLimite(bloquesIa)) throw ErrorResumen(ErrorResumen.GENERICO)
                // Se verifica con alias (así se comparan jugadores y minutos contra los eventos) y
                // recién después se reponen los nombres de pila para mostrar el texto.
                val menciones = ReglasResumen.verificar(salida, insumo)
                val nombres = { texto: String -> ReglasResumen.reidentificar(texto, insumo.aliases) }

                resumenRepository.guardarBorrador(
                    ResumenPartido(
                        id = "",
                        partidoId = partidoId,
                        version = (st.resumenes.maxOfOrNull { it.version } ?: 0) + 1,
                        estado = EstadoResumen.BORRADOR,
                        bloques = BloquesResumen(
                            hechos = nombres(bloquesIa.hechos),
                            desarrollo = nombres(bloquesIa.desarrollo),
                            interpretacion = "",
                        ),
                        modelo = salida.modelo,
                        versionPrompt = ReglasResumen.VERSION_PROMPT,
                        solicitadoPor = perfil.uid,
                        creadoEnMs = null,
                        mencionesNoVerificadas = menciones.map { it.copy(texto = nombres(it.texto)) },
                        advertencias = salida.advertencias.map(nombres),
                        eventosUsados = insumo.eventos.map { it.id },
                        marcador = "${insumo.partido.marcadorPropio}-${insumo.partido.marcadorRival}",
                    ),
                )
                setState { copy(generando = false, versionElegidaId = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ErrorResumen) {
                setState { copy(generando = false, error = e.mensaje) }
            } catch (e: Exception) {
                // Los eventos del partido no se tocan: el entrenador puede volver a intentarlo.
                setState { copy(generando = false, error = ErrorResumen.GENERICO) }
            }
        }
    }
}
