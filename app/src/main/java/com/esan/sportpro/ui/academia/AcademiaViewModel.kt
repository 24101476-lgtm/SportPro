package com.esan.sportpro.ui.academia

import androidx.lifecycle.viewModelScope
import com.esan.sportpro.core.viewmodel.BaseViewModel
import com.esan.sportpro.data.partido.describirErrorFirestore
import com.esan.sportpro.domain.academia.CampoEquipo
import com.esan.sportpro.domain.academia.CategoriasEquipo
import com.esan.sportpro.domain.academia.MensajesEquipo
import com.esan.sportpro.domain.jugadores.JugadorRepository
import com.esan.sportpro.domain.academia.EntrenadorCuenta
import com.esan.sportpro.domain.academia.Equipo
import com.esan.sportpro.domain.academia.EquipoRepository
import com.esan.sportpro.domain.academia.ValidacionEquipo
import com.esan.sportpro.domain.cuentas.PerfilActual
import com.esan.sportpro.domain.cuentas.SesionRepository
import com.esan.sportpro.navigation.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Formulario de alta/edición de equipo. [id] es `null` al crear. */
data class FormularioEquipo(
    val id: String? = null,
    val nombre: String = "",
    val categoria: String = "",
    val entrenador: String = "",
    /** Cuenta del entrenador elegido; vacía = sin asignar (o equipo antiguo solo con nombre). */
    val entrenadorUid: String = "",
    val errores: Map<CampoEquipo, String> = emptyMap(),
    val guardando: Boolean = false,
    /** Escudo ya guardado (al editar). */
    val escudoActual: String = "",
    /** URI local de un escudo nuevo (galería), todavía sin subir. */
    val escudoNuevo: String? = null,
    /** US-005: texto de la categoría cuando se elige "Otra" (máx. 30 caracteres). */
    val categoriaOtra: String = "",
    /** US-005: años de nacimiento como texto, para validar lo que escribe el usuario. */
    val anioMinimoTexto: String = "",
    val anioMaximoTexto: String = "",
    /** US-005: color de uniforme en hexadecimal. */
    val colorUniforme: String = "",
    /** El entrenador edita sus equipos, pero no cambia quién los dirige (solo el administrador). */
    val puedeCambiarEntrenador: Boolean = true,
) {
    val escudoVisible: String? get() = escudoNuevo ?: escudoActual.takeIf { it.isNotBlank() }
}

data class AcademiaUiState(
    val perfil: PerfilActual? = null,
    val equipos: List<Equipo> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null,
    val mostrarInactivos: Boolean = false,
    val formulario: FormularioEquipo? = null,
    /** Cuentas con rol Entrenador de la academia, para asignar a un equipo. */
    val entrenadores: List<EntrenadorCuenta> = emptyList(),
    /** Cuentas de entrenador que esperan aprobación (solo las ve el administrador). */
    val entrenadoresPendientes: List<EntrenadorCuenta> = emptyList(),
    /** US-004: nombre de la academia y día de corte de las mensualidades. */
    val configuracion: com.esan.sportpro.domain.academia.ConfiguracionAcademia =
        com.esan.sportpro.domain.academia.ConfiguracionAcademia(),
    /** US-004: códigos de invitación para cuentas de administrador. */
    val codigos: List<com.esan.sportpro.domain.academia.CodigoInvitacion> = emptyList(),
    val editandoConfiguracion: FormularioConfiguracion? = null,
    /** US-005: jugadores activos por equipo (id del equipo → cantidad). */
    val jugadoresPorEquipo: Map<String, Int> = emptyMap(),
    /** US-005: filtro por categoría de la lista; `null` = todas. */
    val filtroCategoria: String? = null,
    /** US-005: equipo que se quiere eliminar, con la cantidad de jugadores que tiene. */
    val eliminando: Pair<Equipo, Int>? = null,
) {
    /** Solo el administrador crea equipos y gestiona la academia. */
    val puedeEditar: Boolean get() = perfil?.rol == UserRole.ADMINISTRADOR

    /** US-005: el administrador edita cualquier equipo; el entrenador, solo los que dirige. */
    fun puedeEditarEquipo(equipo: Equipo): Boolean {
        val p = perfil ?: return false
        return when (p.rol) {
            UserRole.ADMINISTRADOR -> true
            UserRole.ENTRENADOR -> equipo.dirigidoPor(p.uid, p.nombre)
            else -> false
        }
    }

    /** Categorías que tienen al menos un equipo, para los chips del filtro. */
    val categoriasConEquipos: List<String>
        get() = equipos.filter { mostrarInactivos || it.activo }.map { it.categoria }.distinct().sorted()

    val equiposVisibles: List<Equipo>
        get() = equipos
            .filter { mostrarInactivos || it.activo }
            .filter { filtroCategoria == null || it.categoria == filtroCategoria }

    fun jugadoresDe(equipo: Equipo): Int = jugadoresPorEquipo[equipo.id] ?: 0
}

/** US-004: formulario con todos los datos de la academia. */
data class FormularioConfiguracion(
    val datos: com.esan.sportpro.domain.academia.ConfiguracionAcademia,
    val anioFundacionTexto: String,
    val diaCorteTexto: String,
    val errores: Map<com.esan.sportpro.domain.academia.CampoAcademia, String> = emptyMap(),
    /** URI local del escudo elegido, todavía sin subir. */
    val escudoNuevo: String? = null,
    val guardando: Boolean = false,
) {
    val escudoVisible: String? get() = escudoNuevo ?: datos.escudoUrl.takeIf { it.isNotBlank() }
    val esRegistro: Boolean get() = datos.nombre.isBlank()
}

sealed interface AcademiaEvent {
    data class Mensaje(val texto: String) : AcademiaEvent
}

/** US-004/US-005: gestión de equipos de la academia por categoría. */
@HiltViewModel
class AcademiaViewModel @Inject constructor(
    private val sesionRepository: SesionRepository,
    private val equipoRepository: EquipoRepository,
    private val usuarioRepository: com.esan.sportpro.domain.cuentas.UsuarioRepository,
    private val academiaRepository: com.esan.sportpro.domain.academia.AcademiaRepository,
    private val jugadorRepository: JugadorRepository,
) : BaseViewModel<AcademiaUiState, AcademiaEvent>(AcademiaUiState()) {

    init {
        viewModelScope.launch {
            val perfil = sesionRepository.perfilActual()
            if (perfil == null) {
                setState { copy(cargando = false, error = "No se pudo leer tu perfil. Vuelve a iniciar sesión.") }
                return@launch
            }
            setState { copy(perfil = perfil) }
            launch {
                academiaRepository.observarConfiguracion(perfil.academiaId)
                    .collect { setState { copy(configuracion = it) } }
            }
            if (perfil.rol == UserRole.ADMINISTRADOR) {
                launch {
                    academiaRepository.observarCodigos(perfil.academiaId)
                        .catch { }
                        .collect { setState { copy(codigos = it) } }
                }
                launch {
                    equipoRepository.observarEntrenadores(perfil.academiaId)
                        .catch { }
                        .collect { setState { copy(entrenadores = it) } }
                }
                launch {
                    usuarioRepository.observarEntrenadoresPendientes(perfil.academiaId)
                        .catch { }
                        .collect { setState { copy(entrenadoresPendientes = it) } }
                }
            }
            if (perfil.esStaff) {
                // US-005: la lista muestra cuántos jugadores activos tiene cada equipo.
                launch {
                    jugadorRepository.observarJugadores(perfil.academiaId)
                        .catch { }
                        .collect { jugadores ->
                            val conteo = jugadores.filter { it.activo && it.equipoId.isNotBlank() }
                                .groupingBy { it.equipoId }.eachCount()
                            setState { copy(jugadoresPorEquipo = conteo) }
                        }
                }
            }
            equipoRepository.observarEquipos(perfil.academiaId)
                .catch { setState { copy(cargando = false, error = describirErrorFirestore(it)) } }
                .collect { lista -> setState { copy(equipos = lista, cargando = false, error = null) } }
        }
    }

    fun onMostrarInactivos(valor: Boolean) = setState { copy(mostrarInactivos = valor) }

    /** US-005: filtra la lista por categoría (`null` = todas). */
    fun onFiltroCategoria(categoria: String?) = setState { copy(filtroCategoria = categoria) }

    fun nuevoEquipo() {
        if (!currentState.puedeEditar) return
        setState { copy(formulario = FormularioEquipo()) }
    }

    fun editarEquipo(equipo: Equipo) {
        if (!currentState.puedeEditarEquipo(equipo)) return
        val opcion = CategoriasEquipo.opcionDe(equipo.categoria).orEmpty()
        setState {
            copy(
                formulario = FormularioEquipo(
                    id = equipo.id,
                    nombre = equipo.nombre,
                    categoria = opcion,
                    categoriaOtra = if (opcion == CategoriasEquipo.OTRA) equipo.categoria else "",
                    anioMinimoTexto = equipo.anioMinimo?.toString().orEmpty(),
                    anioMaximoTexto = equipo.anioMaximo?.toString().orEmpty(),
                    colorUniforme = equipo.colorUniforme,
                    puedeCambiarEntrenador = currentState.puedeEditar,
                    entrenador = equipo.entrenadorNombre,
                    // Equipos antiguos: si el nombre coincide con una cuenta, se propone esa cuenta.
                    entrenadorUid = equipo.entrenadorUid.ifBlank {
                        currentState.entrenadores.firstOrNull { equipo.copy(entrenadorUid = "").dirigidoPor(it.uid, it.nombre) }?.uid.orEmpty()
                    },
                    escudoActual = equipo.escudoUrl,
                ),
            )
        }
    }

    fun onEscudoElegido(uri: String) = editarFormulario { copy(escudoNuevo = uri) }

    fun quitarEscudoNuevo() = editarFormulario { copy(escudoNuevo = null) }

    fun cerrarFormulario() = setState { copy(formulario = null) }

    fun onNombreChanged(valor: String) =
        editarFormulario { copy(nombre = valor, errores = errores - CampoEquipo.NOMBRE) }

    /** Al elegir "Sub-N" se propone el rango de años si todavía está vacío (se puede cambiar). */
    fun onCategoriaChanged(valor: String) = editarFormulario {
        val sugerido = ValidacionEquipo.rangoSugerido(valor)
        val vacio = anioMinimoTexto.isBlank() && anioMaximoTexto.isBlank()
        copy(
            categoria = valor,
            anioMinimoTexto = if (vacio && sugerido != null) sugerido.first.toString() else anioMinimoTexto,
            anioMaximoTexto = if (vacio && sugerido != null) sugerido.second.toString() else anioMaximoTexto,
            errores = errores - CampoEquipo.CATEGORIA,
        )
    }

    fun onCategoriaOtraChanged(valor: String) = editarFormulario {
        copy(categoriaOtra = valor.take(CategoriasEquipo.OTRA_MAX + 5), errores = errores - CampoEquipo.CATEGORIA)
    }

    fun onAnioMinimoChanged(valor: String) = editarFormulario {
        copy(anioMinimoTexto = valor.filter(Char::isDigit).take(4), errores = errores - CampoEquipo.ANIOS)
    }

    fun onAnioMaximoChanged(valor: String) = editarFormulario {
        copy(anioMaximoTexto = valor.filter(Char::isDigit).take(4), errores = errores - CampoEquipo.ANIOS)
    }

    fun onColorUniforme(hex: String) = editarFormulario { copy(colorUniforme = hex) }

    fun onEntrenadorChanged(valor: String) =
        editarFormulario { copy(entrenador = valor, errores = errores - CampoEquipo.ENTRENADOR) }

    /** Asigna una cuenta de entrenador (o ninguna con `null`). */
    fun onEntrenadorElegido(cuenta: EntrenadorCuenta?) = editarFormulario {
        copy(
            entrenadorUid = cuenta?.uid.orEmpty(),
            entrenador = cuenta?.nombre.orEmpty(),
            errores = errores - CampoEquipo.ENTRENADOR,
        )
    }

    fun guardar() {
        val estado = currentState
        val formulario = estado.formulario ?: return
        val perfil = estado.perfil ?: return
        val original = formulario.id?.let { id -> estado.equipos.firstOrNull { it.id == id } }
        val autorizado = if (original == null) estado.puedeEditar else estado.puedeEditarEquipo(original)
        if (!autorizado || formulario.guardando) return

        val anioMinimo = formulario.anioMinimoTexto.toIntOrNull()
        val anioMaximo = formulario.anioMaximoTexto.toIntOrNull()
        val errores = ValidacionEquipo.validar(
            nombre = formulario.nombre,
            categoria = formulario.categoria,
            entrenadorNombre = formulario.entrenador,
            existentes = estado.equipos,
            idEditando = formulario.id,
            categoriaOtra = formulario.categoriaOtra,
            entrenadorUid = formulario.entrenadorUid,
            anioMinimo = anioMinimo,
            anioMaximo = anioMaximo,
        )
        if (errores.isNotEmpty()) {
            editarFormulario { copy(errores = errores) }
            return
        }

        editarFormulario { copy(guardando = true) }
        viewModelScope.launch {
            try {
                val esNuevo = formulario.id == null
                // Para un equipo nuevo con escudo se reserva el id antes, así se sube justo después.
                val id = formulario.id ?: if (formulario.escudoNuevo != null) equipoRepository.nuevoId() else ""
                val confirmado = equipoRepository.guardar(
                    Equipo(
                        id = id,
                        academiaId = perfil.academiaId,
                        nombre = formulario.nombre,
                        categoria = CategoriasEquipo.categoriaFinal(formulario.categoria, formulario.categoriaOtra),
                        entrenadorNombre = formulario.entrenador,
                        entrenadorUid = formulario.entrenadorUid,
                        creadoPor = perfil.uid,
                        anioMinimo = anioMinimo,
                        anioMaximo = anioMaximo,
                        colorUniforme = formulario.colorUniforme,
                    ),
                    esNuevo = esNuevo,
                )
                // El escudo se sube después de guardar el equipo. Si falla, el equipo queda sin escudo.
                val escudoFallido = formulario.escudoNuevo?.let { uri ->
                    id.isBlank() || equipoRepository.subirEscudo(id, uri) == null
                } ?: false
                setState { copy(formulario = null) }
                val texto = buildString {
                    append(mensajeGuardado(confirmado, "Equipo guardado"))
                    if (escudoFallido) append(". No se pudo subir el escudo; inténtalo de nuevo más tarde")
                }
                sendEvent(AcademiaEvent.Mensaje(texto))
            } catch (e: Exception) {
                editarFormulario { copy(guardando = false) }
                sendEvent(AcademiaEvent.Mensaje(describirErrorFirestore(e)))
            }
        }
    }

    // ---- US-004: configuración y códigos de invitación ---------------------------------------

    fun editarConfiguracion() {
        if (!currentState.puedeEditar) return
        val c = currentState.configuracion
        setState {
            copy(
                editandoConfiguracion = FormularioConfiguracion(
                    datos = c,
                    anioFundacionTexto = c.anioFundacion?.toString().orEmpty(),
                    diaCorteTexto = c.diaCorte.toString(),
                ),
            )
        }
    }

    fun cerrarConfiguracion() = setState { copy(editandoConfiguracion = null) }

    /** Cambia un campo de texto del formulario de la academia y limpia su error. */
    fun onCampoAcademia(campo: com.esan.sportpro.domain.academia.CampoAcademia, valor: String) = setState {
        val f = editandoConfiguracion ?: return@setState this
        val d = f.datos
        val actualizado = when (campo) {
            com.esan.sportpro.domain.academia.CampoAcademia.NOMBRE -> f.copy(datos = d.copy(nombre = valor.take(90)))
            com.esan.sportpro.domain.academia.CampoAcademia.CIUDAD -> f.copy(datos = d.copy(ciudad = valor.take(90)))
            com.esan.sportpro.domain.academia.CampoAcademia.DISTRITO -> f.copy(datos = d.copy(distrito = valor.take(90)))
            com.esan.sportpro.domain.academia.CampoAcademia.DIRECCION -> f.copy(datos = d.copy(direccion = valor.take(130)))
            com.esan.sportpro.domain.academia.CampoAcademia.TELEFONO ->
                f.copy(datos = d.copy(telefono = valor.filter { it.isDigit() || it == '+' || it == ' ' }.take(18)))
            com.esan.sportpro.domain.academia.CampoAcademia.CORREO -> f.copy(datos = d.copy(correo = valor.trim().take(110)))
            com.esan.sportpro.domain.academia.CampoAcademia.ANIO_FUNDACION ->
                f.copy(anioFundacionTexto = valor.filter(Char::isDigit).take(4))
            com.esan.sportpro.domain.academia.CampoAcademia.DIA_CORTE ->
                f.copy(diaCorteTexto = valor.filter(Char::isDigit).take(2))
            com.esan.sportpro.domain.academia.CampoAcademia.ESCUDO -> f
        }
        copy(editandoConfiguracion = actualizado.copy(errores = actualizado.errores - campo))
    }

    fun onEscudoAcademia(uri: String?) = setState {
        val f = editandoConfiguracion ?: return@setState this
        copy(
            editandoConfiguracion = f.copy(
                escudoNuevo = uri,
                errores = f.errores - com.esan.sportpro.domain.academia.CampoAcademia.ESCUDO,
            ),
        )
    }

    /**
     * US-004: valida con los mensajes del criterio, sube el escudo (máx. 2 MB, 1:1), comprueba que
     * el nombre sea único y guarda; cada campo que cambió queda en la subcolección `auditoria`.
     */
    fun guardarConfiguracion() {
        val formulario = currentState.editandoConfiguracion ?: return
        val perfil = currentState.perfil ?: return
        if (formulario.guardando) return
        val errores = com.esan.sportpro.domain.academia.ValidacionAcademia.validar(
            datos = formulario.datos,
            anioFundacionTexto = formulario.anioFundacionTexto,
            diaCorteTexto = formulario.diaCorteTexto,
        )
        if (errores.isNotEmpty()) {
            setState { copy(editandoConfiguracion = formulario.copy(errores = errores)) }
            return
        }
        setState { copy(editandoConfiguracion = formulario.copy(guardando = true)) }
        viewModelScope.launch {
            try {
                var escudoUrl = formulario.datos.escudoUrl
                formulario.escudoNuevo?.let { uri ->
                    escudoUrl = academiaRepository.subirEscudo(perfil.academiaId, uri)
                        ?: throw IllegalStateException("No se pudo subir el escudo. Revisa tu conexión e inténtalo de nuevo")
                }
                val anterior = currentState.configuracion.copy(id = perfil.academiaId)
                val nueva = formulario.datos.copy(
                    id = perfil.academiaId,
                    nombre = formulario.datos.nombre.trim(),
                    escudoUrl = escudoUrl,
                    anioFundacion = formulario.anioFundacionTexto.toIntOrNull(),
                    diaCorte = formulario.diaCorteTexto.toInt(),
                )
                val confirmado = academiaRepository.guardarDatos(anterior, nueva, perfil.uid)
                setState { copy(editandoConfiguracion = null) }
                sendEvent(AcademiaEvent.Mensaje(mensajeGuardado(confirmado, "Datos de la academia guardados")))
            } catch (e: com.esan.sportpro.domain.academia.NombreAcademiaDuplicadoException) {
                errorEnCampo(formulario, com.esan.sportpro.domain.academia.CampoAcademia.NOMBRE, e.message.orEmpty())
            } catch (e: com.esan.sportpro.domain.academia.EscudoMuyGrandeException) {
                errorEnCampo(formulario, com.esan.sportpro.domain.academia.CampoAcademia.ESCUDO, e.message.orEmpty())
            } catch (e: IllegalStateException) {
                errorEnCampo(formulario, com.esan.sportpro.domain.academia.CampoAcademia.ESCUDO, e.message.orEmpty())
            } catch (e: Exception) {
                setState { copy(editandoConfiguracion = formulario.copy(guardando = false)) }
                sendEvent(AcademiaEvent.Mensaje(describirErrorFirestore(e)))
            }
        }
    }

    private fun errorEnCampo(
        formulario: FormularioConfiguracion,
        campo: com.esan.sportpro.domain.academia.CampoAcademia,
        mensaje: String,
    ) = setState {
        copy(editandoConfiguracion = formulario.copy(guardando = false, errores = formulario.errores + (campo to mensaje)))
    }

    /** US-004: genera un código de 8 caracteres para [rol], válido por 7 días. */
    fun generarCodigo(rol: UserRole = UserRole.ADMINISTRADOR) {
        val perfil = currentState.perfil ?: return
        if (!currentState.puedeEditar) return
        viewModelScope.launch {
            try {
                val codigo = academiaRepository.generarCodigo(perfil.academiaId, perfil.uid, rol)
                sendEvent(AcademiaEvent.Mensaje("Código $codigo generado: vale 7 días y un solo uso"))
            } catch (e: Exception) {
                sendEvent(AcademiaEvent.Mensaje(describirErrorFirestore(e)))
            }
        }
    }

    fun revocarCodigo(codigo: String) {
        if (!currentState.puedeEditar) return
        viewModelScope.launch {
            try {
                val confirmado = academiaRepository.revocarCodigo(codigo)
                sendEvent(AcademiaEvent.Mensaje(mensajeGuardado(confirmado, "Código revocado")))
            } catch (e: Exception) {
                sendEvent(AcademiaEvent.Mensaje(describirErrorFirestore(e)))
            }
        }
    }

    /** Aprueba (ACTIVO) o rechaza (DESACTIVADO) una cuenta de entrenador pendiente. */
    fun resolverEntrenador(cuenta: EntrenadorCuenta, aprobar: Boolean) {
        if (!currentState.puedeEditar) return
        viewModelScope.launch {
            try {
                val confirmado = usuarioRepository.resolverEntrenador(cuenta.uid, aprobar)
                val texto = if (aprobar) "Entrenador aprobado" else "Cuenta de entrenador rechazada"
                sendEvent(AcademiaEvent.Mensaje(mensajeGuardado(confirmado, texto)))
            } catch (e: Exception) {
                sendEvent(AcademiaEvent.Mensaje(describirErrorFirestore(e)))
            }
        }
    }

    /**
     * US-005: un equipo con jugadores asignados no se elimina; el diálogo ofrece Desactivar equipo,
     * que lo oculta de los listados sin borrar su historial.
     */
    fun pedirEliminar(equipo: Equipo) {
        if (!currentState.puedeEditar) return
        setState { copy(eliminando = equipo to currentState.jugadoresDe(equipo)) }
    }

    fun cerrarEliminar() = setState { copy(eliminando = null) }

    fun confirmarEliminar() {
        val (equipo, jugadores) = currentState.eliminando ?: return
        setState { copy(eliminando = null) }
        if (!ValidacionEquipo.puedeEliminar(jugadores)) {
            sendEvent(AcademiaEvent.Mensaje(MensajesEquipo.NO_SE_PUEDE_ELIMINAR))
            return
        }
        viewModelScope.launch {
            try {
                val confirmado = equipoRepository.eliminar(equipo.id)
                sendEvent(AcademiaEvent.Mensaje(mensajeGuardado(confirmado, "Equipo eliminado")))
            } catch (e: Exception) {
                sendEvent(AcademiaEvent.Mensaje(describirErrorFirestore(e)))
            }
        }
    }

    /** Desde el diálogo de eliminar: el equipo con jugadores se desactiva en su lugar. */
    fun desactivarEnVezDeEliminar() {
        val (equipo, _) = currentState.eliminando ?: return
        setState { copy(eliminando = null) }
        if (equipo.activo) cambiarEstado(equipo)
    }

    fun cambiarEstado(equipo: Equipo) {
        if (!currentState.puedeEditar) return
        viewModelScope.launch {
            try {
                val confirmado = equipoRepository.cambiarEstado(equipo.id, !equipo.activo)
                val texto = if (equipo.activo) "Equipo desactivado" else "Equipo activado"
                sendEvent(AcademiaEvent.Mensaje(mensajeGuardado(confirmado, texto)))
            } catch (e: Exception) {
                sendEvent(AcademiaEvent.Mensaje(describirErrorFirestore(e)))
            }
        }
    }

    private fun editarFormulario(cambio: FormularioEquipo.() -> FormularioEquipo) =
        setState { copy(formulario = formulario?.cambio()) }

    private fun mensajeGuardado(confirmado: Boolean, texto: String) =
        if (confirmado) texto else "$texto. Se sincronizará cuando vuelva la conexión."
}
