package com.esan.sportpro.ui.login

import androidx.lifecycle.viewModelScope
import com.esan.sportpro.core.viewmodel.BaseViewModel
import com.esan.sportpro.domain.cuentas.ResultadoEnvioVerificacion
import com.esan.sportpro.domain.cuentas.ResultadoSesion
import com.esan.sportpro.domain.cuentas.SesionRepository
import com.esan.sportpro.navigation.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val correo: String = "",
    val password: String = "",
    val mostrarPassword: Boolean = false,
    val isLoading: Boolean = false,
    val errores: Map<CampoLogin, String> = emptyMap(),
    val errorGeneral: String? = null,
    /** El correo existe pero sin verificar: la pantalla ofrece reenviar la verificación. */
    val correoSinVerificar: Boolean = false,
    val intentosFallidos: Int = 0,
    val segundosRestantesBloqueo: Int = 0,
) {
    /** El botón "Ingresar" se apaga mientras carga o durante el bloqueo por intentos fallidos. */
    val bloqueado: Boolean get() = segundosRestantesBloqueo > 0
}

sealed interface LoginEvent {
    /** La app navega al inicio; [rol] define la redirección según el perfil (US-003). */
    data class SesionIniciada(val uid: String, val rol: UserRole) : LoginEvent
    data class Snackbar(val mensaje: String) : LoginEvent
}

/** US-002: login con bloqueo por intentos fallidos. */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val sesionRepository: SesionRepository,
) : BaseViewModel<LoginUiState, LoginEvent>(LoginUiState()) {

    private var cuentaAtrasiva: Job? = null

    fun onCorreoChanged(valor: String) = limpiar(CampoLogin.CORREO) { copy(correo = valor) }

    fun onPasswordChanged(valor: String) = limpiar(CampoLogin.PASSWORD) { copy(password = valor) }

    fun onToggleMostrarPassword() = setState { copy(mostrarPassword = !mostrarPassword) }

    /** `true` si ya hay sesión: el NavHost usa esto para arrancar en el inicio. */
    fun haySesionActiva(): Boolean = sesionRepository.haySesionActiva()

    fun login() {
        val formulario = currentState
        if (formulario.isLoading || formulario.bloqueado) return

        val errores = ValidacionLogin.validarCampos(listOf(formulario.correo, formulario.password))
        if (errores.isNotEmpty()) {
            setState { copy(errores = errores, errorGeneral = null) }
            return
        }

        viewModelScope.launch {
            setState { copy(isLoading = true, errores = emptyMap(), errorGeneral = null) }

            when (val resultado = sesionRepository.iniciarSesion(formulario.correo, formulario.password)) {
                is ResultadoSesion.Exito -> {
                    setState {
                        copy(
                            isLoading = false,
                            intentosFallidos = 0,
                            errorGeneral = null,
                            correoSinVerificar = false,
                        )
                    }
                    // El rol viaja en el evento; la redirección por rol se resuelve en US-003.
                    sendEvent(LoginEvent.SesionIniciada(resultado.uid, resultado.rol))
                }

                is ResultadoSesion.CredencialesIncorrectas -> {
                    val intentos = formulario.intentosFallidos + 1
                    if (intentos >= MensajesLogin.MAX_INTENTOS_FALLIDOS) {
                        iniciarBloqueo(intentos)
                    } else {
                        setState {
                            copy(
                                isLoading = false,
                                intentosFallidos = intentos,
                                errorGeneral = MensajesLogin.CREDENCIALES_INCORRECTAS,
                            )
                        }
                    }
                }

                is ResultadoSesion.CorreoSinVerificar -> setState {
                    copy(
                        isLoading = false,
                        correoSinVerificar = true,
                        errorGeneral = MensajesLogin.CORREO_SIN_VERIFICAR,
                    )
                }

                is ResultadoSesion.CuentaDesactivada -> setState {
                    copy(
                        isLoading = false,
                        correoSinVerificar = false,
                        errorGeneral = MensajesLogin.CUENTA_DESACTIVADA,
                    )
                }

                is ResultadoSesion.SinConexion -> {
                    setState { copy(isLoading = false) }
                    sendEvent(LoginEvent.Snackbar(MensajesLogin.SIN_CONEXION))
                }

                is ResultadoSesion.Error -> {
                    setState { copy(isLoading = false) }
                    sendEvent(LoginEvent.Snackbar(resultado.mensaje))
                }
            }
        }
    }

    private fun iniciarBloqueo(intentos: Int) {
        val total = MensajesLogin.SEGUNDOS_BLOQUEO.toInt()
        setState {
            copy(
                isLoading = false,
                intentosFallidos = intentos,
                segundosRestantesBloqueo = total,
                errorGeneral = MensajesLogin.mensajeBloqueo(total),
            )
        }
        cuentaAtrasiva?.cancel()
        cuentaAtrasiva = viewModelScope.launch {
            var restantes = total
            while (restantes > 0) {
                delay(1_000)
                restantes -= 1
                // El mensaje muestra el tiempo restante que falta.
                setState {
                    copy(
                        segundosRestantesBloqueo = restantes,
                        errorGeneral = MensajesLogin.mensajeBloqueo(restantes),
                    )
                }
            }
            setState {
                copy(segundosRestantesBloqueo = 0, intentosFallidos = 0, errorGeneral = null)
            }
        }
    }

    private fun limpiar(campo: CampoLogin, reducer: LoginUiState.() -> LoginUiState) =
        setState {
            reducer().copy(
                errores = errores - campo,
                errorGeneral = null,
                // Al editar el correo deja de tener sentido el aviso de verificación.
                correoSinVerificar = if (campo == CampoLogin.CORREO) false else correoSinVerificar,
            )
        }

    /** Pide otro correo de verificación manteniendo el mensaje en pantalla. */
    fun reenviarVerificacion() {
        val formulario = currentState
        if (formulario.isLoading) return

        if (formulario.password.isBlank()) {
            setState {
                copy(
                    correoSinVerificar = true,
                    errorGeneral = MensajesLogin.SIN_CONTRASENA,
                    errores = errores + (CampoLogin.PASSWORD to MensajesLogin.CAMPO_OBLIGATORIO),
                )
            }
            return
        }

        viewModelScope.launch {
            setState { copy(isLoading = true, errorGeneral = null) }
            when (sesionRepository.reenviarCorreoVerificacion(formulario.correo, formulario.password)) {
                is ResultadoEnvioVerificacion.Enviado -> {
                    setState {
                        copy(
                            isLoading = false,
                            correoSinVerificar = true,
                            errorGeneral = MensajesLogin.CORREO_SIN_VERIFICAR,
                        )
                    }
                    sendEvent(LoginEvent.Snackbar(MensajesLogin.VERIFICACION_REENVIADA))
                }

                is ResultadoEnvioVerificacion.CredencialesIncorrectas -> setState {
                    copy(
                        isLoading = false,
                        correoSinVerificar = true,
                        errorGeneral = MensajesLogin.CREDENCIALES_INCORRECTAS,
                    )
                }

                is ResultadoEnvioVerificacion.SinConexion -> {
                    setState { copy(isLoading = false) }
                    sendEvent(LoginEvent.Snackbar(MensajesLogin.SIN_CONEXION))
                }

                is ResultadoEnvioVerificacion.Error -> {
                    setState { copy(isLoading = false) }
                    sendEvent(LoginEvent.Snackbar(MensajesLogin.SIN_CONEXION))
                }
            }
        }
    }
}