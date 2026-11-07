package com.esan.sportpro.ui.ia

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.esan.sportpro.core.red.ConectividadObserver
import com.esan.sportpro.core.viewmodel.BaseViewModel
import com.esan.sportpro.data.partido.describirErrorFirestore
import com.esan.sportpro.domain.cuentas.PerfilActual
import com.esan.sportpro.domain.cuentas.SesionRepository
import com.esan.sportpro.domain.ia.BloquesResumen
import com.esan.sportpro.domain.ia.CampoBloque
import com.esan.sportpro.domain.ia.CampoEvaluacion
import com.esan.sportpro.domain.ia.ConstructorInsumo
import com.esan.sportpro.domain.ia.DatosEvaluacion
import com.esan.sportpro.domain.ia.DiferenciasResumen
import com.esan.sportpro.domain.ia.ErrorResumen
import com.esan.sportpro.domain.ia.EstadoResumen
import com.esan.sportpro.domain.ia.EvaluacionRepository
import com.esan.sportpro.domain.ia.EvaluacionResumen
import com.esan.sportpro.domain.ia.GeneradorResumen
import com.esan.sportpro.domain.ia.MencionNoVerificada
import com.esan.sportpro.domain.ia.ReglasEvaluacion
import com.esan.sportpro.domain.ia.ReglasResumen
import com.esan.sportpro.domain.ia.ReglasRevision
import com.esan.sportpro.domain.ia.ResumenPartido
import com.esan.sportpro.domain.ia.ResumenRepository
import com.esan.sportpro.domain.ia.RevisionResumenRepository
import com.esan.sportpro.domain.partido.CatalogoBase
import com.esan.sportpro.domain.partido.EventoPartido
import com.esan.sportpro.domain.partido.JugadorConvocado
import com.esan.sportpro.domain.partido.Partido
import com.esan.sportpro.domain.partido.PartidoRepository
import com.esan.sportpro.domain.partido.TipoEvento
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Texto que el entrenador está corrigiendo; [original] es con lo que se abrió, para saber si hay cambios sin guardar. */
data class EdicionResumen(
    val resumenId: String,
    val hechos: String,
    val desarrollo: String,
    val interpretacion: String,
    val original: BloquesResumen,
) {
    val bloques: BloquesResumen get() = BloquesResumen(hechos, desarrollo, interpretacion)
    val modificado: Boolean get() = bloques != original
}

/** Formulario de evaluación (US-025) del resumen [resumenId]. */
data class FormularioEvaluacion(
    val resumenId: String,
    val datos: DatosEvaluacion,
    val errores: Map<CampoEvaluacion, String> = emptyMap(),
    val guardando: Boolean = false,
)

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

    // ---- US-024: revisión, corrección y aprobación
    /** Texto en edición; solo existe mientras el revisor mira una versión en Borrador. */
    val edicion: EdicionResumen? = null,
    val erroresEdicion: Map<CampoBloque, String> = emptyMap(),
    /** Menciones que todavía no se corrigieron en el texto que se está editando. */
    val mencionesEdicion: List<MencionNoVerificada> = emptyList(),
    /** Aviso bajo el botón Aprobar cuando todavía no se puede aprobar (criterio 4). */
    val avisoAprobacion: String? = null,
    val guardando: Boolean = false,
    val dialogoAprobar: Boolean = false,
    val confirmacionMarcada: Boolean = false,
    val publicarEnComunidad: Boolean = false,
    val dialogoEditarAprobado: Boolean = false,

    // ---- US-025: evaluación
    /** Evaluación ya registrada de la versión que se está viendo, o null. */
    val evaluacion: EvaluacionResumen? = null,
    /** Pregunta "¿Evaluar ahora?" que aparece justo después de aprobar. */
    val ofrecerEvaluacion: Boolean = false,
    val formularioEvaluacion: FormularioEvaluacion? = null,

    /** Aviso de una sola vez (guardado, aprobado, error); la pantalla lo muestra y lo limpia. */
    val mensaje: String? = null,
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

    /** El administrador y quien opera el partido revisan, corrigen y aprueban. */
    val puedeRevisar: Boolean get() = ReglasRevision.puedeRevisar(perfil, partido)

    /** La versión mostrada se está revisando: está en Borrador y hay un editor abierto. */
    val enRevision: Boolean get() = edicion != null && resumenMostrado?.estado == EstadoResumen.BORRADOR

    /** US-025, criterio 1: el resumen aprobado todavía no tiene su evaluación. */
    val evaluacionPendiente: Boolean
        get() = resumenMostrado?.let { ReglasEvaluacion.pendiente(perfil, it, evaluacion) } == true

    val puedeEvaluar: Boolean
        get() = resumenMostrado?.let { ReglasEvaluacion.puedeEvaluar(perfil, it) } == true
}

/**
 * US-023: genera el resumen narrativo del partido. Arma el insumo con alias en lugar de nombres,
 * lo envía a la función intermedia, verifica la respuesta contra los eventos, repone los nombres
 * de pila y guarda la generación como una versión nueva en estado Borrador.
 *
 * US-024: el entrenador corrige el texto con la verificación a la vista, lo aprueba con una
 * confirmación explícita y, si lo decide, lo publica también en la comunidad. US-025: tras aprobar
 * registra la evaluación del resumen.
 */
@HiltViewModel
class ResumenIaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val partidoRepository: PartidoRepository,
    private val resumenRepository: ResumenRepository,
    private val revisionRepository: RevisionResumenRepository,
    private val evaluacionRepository: EvaluacionRepository,
    private val generador: GeneradorResumen,
    private val sesionRepository: SesionRepository,
    private val conectividad: ConectividadObserver,
) : BaseViewModel<ResumenIaUiState, Unit>(ResumenIaUiState()) {

    val partidoId: String = checkNotNull(savedStateHandle["partidoId"])

    private var trabajoEvaluacion: Job? = null
    private var evaluacionObservadaId: String? = null

    init {
        viewModelScope.launch {
            val perfil = runCatching { sesionRepository.perfilActual() }.getOrNull()
            setState { copy(perfil = perfil) }
            sincronizar()
        }
        viewModelScope.launch {
            partidoRepository.observarPartido(partidoId)
                .catch { setState { copy(cargando = false) } }
                .collect {
                    setState { copy(partido = it, cargando = false) }
                    sincronizar()
                }
        }
        viewModelScope.launch {
            partidoRepository.observarEventos(partidoId)
                .catch { /* sin eventos no se puede generar: el motivo lo indica */ }
                .collect {
                    setState { copy(eventos = it) }
                    recalcularMenciones()
                }
        }
        viewModelScope.launch {
            partidoRepository.observarConvocados(partidoId)
                .catch { }
                .collect {
                    setState { copy(convocados = it) }
                    recalcularMenciones()
                }
        }
        viewModelScope.launch {
            partidoRepository.observarCatalogo()
                .catch { }
                .collect {
                    setState { copy(catalogo = it) }
                    recalcularMenciones()
                }
        }
        viewModelScope.launch {
            resumenRepository.observarResumenes(partidoId)
                .catch { /* sin permiso o sin red: se muestra lo que haya */ }
                .collect {
                    setState { copy(resumenes = it) }
                    sincronizar()
                }
        }
        viewModelScope.launch {
            conectividad.conectado.collect { setState { copy(conectado = it) } }
        }
    }

    fun elegirVersion(id: String) {
        setState { copy(versionElegidaId = id, avisoAprobacion = null, erroresEdicion = emptyMap()) }
        sincronizar()
    }

    fun limpiarMensaje() = setState { copy(mensaje = null) }

    // ------------------------------------------------------------------ Generación (US-023)

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
                sincronizar()
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

    // ------------------------------------------------------------------ Edición (US-024)

    fun onHechosChanged(texto: String) = editar { copy(hechos = texto) }

    fun onDesarrolloChanged(texto: String) = editar { copy(desarrollo = texto) }

    fun onInterpretacionChanged(texto: String) = editar { copy(interpretacion = texto) }

    private fun editar(cambio: EdicionResumen.() -> EdicionResumen) {
        if (currentState.edicion == null) return
        setState { copy(edicion = edicion?.cambio(), erroresEdicion = emptyMap(), avisoAprobacion = null) }
        recalcularMenciones()
    }

    fun guardarBorrador() {
        val st = currentState
        val edicion = st.edicion ?: return
        val perfil = st.perfil ?: return
        if (st.guardando) return
        val errores = ReglasRevision.validar(edicion.bloques)
        if (errores.isNotEmpty()) {
            setState { copy(erroresEdicion = errores) }
            return
        }
        val guardado = edicion.bloques
        setState { copy(guardando = true) }
        viewModelScope.launch {
            try {
                val confirmado = revisionRepository.guardarEdicion(partidoId, edicion.resumenId, guardado, perfil.uid)
                setState {
                    copy(
                        guardando = false,
                        edicion = this.edicion?.copy(original = guardado),
                        mensaje = if (confirmado) "Borrador guardado" else "Borrador guardado. Se sincronizará cuando vuelva la conexión.",
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                setState { copy(guardando = false, mensaje = describirErrorFirestore(e)) }
            }
        }
    }

    /** Aprobar y publicar (criterios 4 y 5): si falta algo se avisa; si no, se pide la confirmación. */
    fun pedirAprobacion() {
        val st = currentState
        val edicion = st.edicion ?: return
        val motivo = ReglasRevision.motivoNoAprobar(edicion.bloques, st.mencionesEdicion)
        if (motivo != null) {
            setState { copy(avisoAprobacion = motivo, erroresEdicion = ReglasRevision.validar(edicion.bloques)) }
            return
        }
        setState { copy(avisoAprobacion = null, dialogoAprobar = true, confirmacionMarcada = false, publicarEnComunidad = false) }
    }

    fun onConfirmacionMarcada(marcada: Boolean) = setState { copy(confirmacionMarcada = marcada) }

    fun onPublicarEnComunidad(publicar: Boolean) = setState { copy(publicarEnComunidad = publicar) }

    fun cerrarDialogoAprobar() = setState { copy(dialogoAprobar = false) }

    fun confirmarAprobacion() {
        val st = currentState
        val edicion = st.edicion ?: return
        val perfil = st.perfil ?: return
        val partido = st.partido ?: return
        val resumen = st.resumenes.firstOrNull { it.id == edicion.resumenId } ?: return
        if (!st.confirmacionMarcada || st.guardando) return
        // Se vuelve a comprobar por si el texto o los eventos cambiaron con el diálogo abierto.
        ReglasRevision.motivoNoAprobar(edicion.bloques, st.mencionesEdicion)?.let { motivo ->
            setState { copy(dialogoAprobar = false, avisoAprobacion = motivo) }
            return
        }
        setState { copy(guardando = true) }
        viewModelScope.launch {
            try {
                val confirmado = revisionRepository.aprobar(
                    partidoId, edicion.resumenId, edicion.bloques, perfil.uid, st.publicarEnComunidad,
                )
                var aviso = if (confirmado) {
                    "Resumen aprobado. Ya es visible en el detalle del partido"
                } else {
                    "Resumen aprobado. Se sincronizará cuando vuelva la conexión."
                }
                if (st.publicarEnComunidad) {
                    val aprobado = resumen.copy(bloquesFinal = edicion.bloques, aprobadoPor = perfil.uid)
                    try {
                        revisionRepository.publicarEnComunidad(
                            partidoId, resumen.id, perfil.academiaId, perfil.uid, perfil.nombre,
                            ReglasRevision.textoParaComunidad(partido, aprobado),
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        aviso += " No se pudo publicar en la comunidad."
                    }
                }
                setState {
                    copy(
                        guardando = false,
                        dialogoAprobar = false,
                        edicion = null,
                        versionElegidaId = resumen.id,
                        ofrecerEvaluacion = true,
                        mensaje = aviso,
                    )
                }
                sincronizar()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                setState { copy(guardando = false, dialogoAprobar = false, mensaje = describirErrorFirestore(e)) }
            }
        }
    }

    /** Criterio 9: editar un resumen aprobado lo devuelve a Borrador y lo retira de donde se publicó. */
    fun pedirEditarAprobado() = setState { copy(dialogoEditarAprobado = true) }

    fun cerrarDialogoEditarAprobado() = setState { copy(dialogoEditarAprobado = false) }

    fun confirmarEditarAprobado() {
        val st = currentState
        val perfil = st.perfil ?: return
        val resumen = st.resumenMostrado?.takeIf { it.estado == EstadoResumen.APROBADO } ?: return
        if (!st.puedeRevisar || st.guardando) return
        setState { copy(guardando = true) }
        viewModelScope.launch {
            try {
                revisionRepository.guardarEdicion(partidoId, resumen.id, resumen.bloquesVigentes, perfil.uid)
                resumen.publicacionComunidadId?.let { publicacion ->
                    runCatching { revisionRepository.retirarDeComunidad(partidoId, resumen.id, publicacion) }
                }
                setState {
                    copy(
                        guardando = false,
                        dialogoEditarAprobado = false,
                        mensaje = "El resumen volvió a Borrador y dejó de ser visible hasta que lo apruebes de nuevo",
                    )
                }
                sincronizar()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                setState { copy(guardando = false, dialogoEditarAprobado = false, mensaje = describirErrorFirestore(e)) }
            }
        }
    }

    // ------------------------------------------------------------------ Evaluación (US-025)

    fun posponerEvaluacion() = setState { copy(ofrecerEvaluacion = false, formularioEvaluacion = null) }

    /** Abre el formulario con las correcciones precargadas: las diferencias entre el texto de la IA y el aprobado. */
    fun abrirEvaluacion() {
        val st = currentState
        val resumen = st.resumenMostrado ?: return
        if (!ReglasEvaluacion.puedeEvaluar(st.perfil, resumen)) return
        setState {
            copy(
                ofrecerEvaluacion = false,
                formularioEvaluacion = FormularioEvaluacion(
                    resumenId = resumen.id,
                    datos = DatosEvaluacion(correcciones = DiferenciasResumen.correcciones(resumen.bloques, resumen.bloquesVigentes)),
                ),
            )
        }
    }

    fun onEvaluacionChanged(campo: CampoEvaluacion, cambio: DatosEvaluacion.() -> DatosEvaluacion) = setState {
        copy(formularioEvaluacion = formularioEvaluacion?.let { it.copy(datos = it.datos.cambio(), errores = it.errores - campo) })
    }

    fun guardarEvaluacion() {
        val st = currentState
        val formulario = st.formularioEvaluacion ?: return
        val perfil = st.perfil ?: return
        val partido = st.partido ?: return
        val resumen = st.resumenes.firstOrNull { it.id == formulario.resumenId } ?: return
        val aprobadoPor = resumen.aprobadoPor ?: return
        if (formulario.guardando || !ReglasEvaluacion.puedeEvaluar(perfil, resumen)) return
        val errores = ReglasEvaluacion.validar(formulario.datos)
        if (errores.isNotEmpty()) {
            setState { copy(formularioEvaluacion = formulario.copy(errores = errores)) }
            return
        }
        val datos = formulario.datos
        setState { copy(formularioEvaluacion = formulario.copy(guardando = true)) }
        viewModelScope.launch {
            try {
                val confirmado = evaluacionRepository.guardar(
                    EvaluacionResumen(
                        id = "${partidoId}_${resumen.id}",
                        partidoId = partidoId,
                        resumenId = resumen.id,
                        version = resumen.version,
                        partidoNombre = "${partido.nombreEquipo} vs ${partido.nombreRival}",
                        fechaPartidoMs = partido.fechaMs,
                        academiaId = perfil.academiaId,
                        aprobadoPor = aprobadoPor,
                        evaluadorUid = perfil.uid,
                        utilidad = datos.utilidad.trim().toInt(),
                        omisiones = datos.omisiones.trim().toInt(),
                        errores = datos.errores.trim().toInt(),
                        inventados = datos.inventados.trim().toInt(),
                        comentario = datos.comentario,
                        correcciones = datos.correcciones,
                        creadoEnMs = null,
                    ),
                )
                setState {
                    copy(
                        formularioEvaluacion = null,
                        mensaje = if (confirmado) "Evaluación guardada" else "Evaluación guardada. Se sincronizará cuando vuelva la conexión.",
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                setState { copy(formularioEvaluacion = formulario.copy(guardando = false), mensaje = describirErrorFirestore(e)) }
            }
        }
    }

    // ------------------------------------------------------------------ Sincronización interna

    /**
     * Mantiene el editor y la observación de la evaluación al día con la versión que se está viendo:
     * el editor existe solo para el revisor y una versión en Borrador, y el texto abierto no se pisa
     * mientras sigue siendo la misma versión.
     */
    private fun sincronizar() {
        val st = currentState
        val resumen = st.resumenMostrado
        if (resumen == null || !st.puedeRevisar || resumen.estado != EstadoResumen.BORRADOR) {
            if (st.edicion != null) setState { copy(edicion = null, mencionesEdicion = emptyList()) }
        } else if (st.edicion?.resumenId != resumen.id) {
            val texto = resumen.bloquesVigentes
            setState {
                copy(
                    edicion = EdicionResumen(resumen.id, texto.hechos, texto.desarrollo, texto.interpretacion, texto),
                    erroresEdicion = emptyMap(),
                    avisoAprobacion = null,
                )
            }
            recalcularMenciones()
        }
        observarEvaluacion(resumen)
    }

    private fun observarEvaluacion(resumen: ResumenPartido?) {
        val id = resumen?.takeIf { ReglasEvaluacion.puedeEvaluar(currentState.perfil, it) }?.id
        if (id == evaluacionObservadaId) return
        evaluacionObservadaId = id
        trabajoEvaluacion?.cancel()
        setState { copy(evaluacion = null) }
        if (id == null) return
        trabajoEvaluacion = viewModelScope.launch {
            evaluacionRepository.observarEvaluacion(partidoId, id)
                .catch { /* sin permiso o sin red: se queda como pendiente */ }
                .collect { setState { copy(evaluacion = it) } }
        }
    }

    private fun recalcularMenciones() {
        val st = currentState
        val edicion = st.edicion ?: return
        val partido = st.partido ?: return
        val resumen = st.resumenes.firstOrNull { it.id == edicion.resumenId } ?: return
        val insumo = ConstructorInsumo.construir(partido, st.eventos, st.convocados, st.catalogo)
        val menciones = ReglasRevision.mencionesVigentes(resumen.mencionesNoVerificadas, edicion.bloques, insumo)
        setState { copy(mencionesEdicion = menciones) }
    }
}
