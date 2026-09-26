package com.esan.sportpro.ui.cuentas

import androidx.lifecycle.viewModelScope
import com.esan.sportpro.core.viewmodel.BaseViewModel
import com.esan.sportpro.navigation.UserRole
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val selectedRole: UserRole = UserRole.JUGADOR,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface AuthEvent {
    data object NavigateToHome : AuthEvent
}

/**
 * ViewModel compartido por LoginScreen y RegisterScreen (US-001/US-002).
 * El registro de datos por rol (perfil extendido en Firestore) queda como TODO para el
 * módulo `cuentas`: aquí solo se resuelve la autenticación con Firebase Auth.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
) : BaseViewModel<AuthUiState, AuthEvent>(AuthUiState()) {

    fun onEmailChanged(value: String) = setState { copy(email = value, errorMessage = null) }

    fun onPasswordChanged(value: String) = setState { copy(password = value, errorMessage = null) }

    fun onRoleSelected(role: UserRole) = setState { copy(selectedRole = role) }

    fun login() {
        val state = currentState
        viewModelScope.launch {
            setState { copy(isLoading = true, errorMessage = null) }
            try {
                withContext(Dispatchers.IO) {
                    firebaseAuth.signInWithEmailAndPassword(state.email, state.password).await()
                }
                setState { copy(isLoading = false) }
                sendEvent(AuthEvent.NavigateToHome)
            } catch (e: Exception) {
                setState { copy(isLoading = false, errorMessage = e.localizedMessage ?: "No se pudo iniciar sesión") }
            }
        }
    }

    fun register() {
        val state = currentState
        viewModelScope.launch {
            setState { copy(isLoading = true, errorMessage = null) }
            try {
                withContext(Dispatchers.IO) {
                    firebaseAuth.createUserWithEmailAndPassword(state.email, state.password).await()
                }
                // TODO(US-001): persistir en Firestore (colección "usuarios") el rol seleccionado
                // y demás datos del perfil una vez que el módulo `cuentas` defina el modelo de dominio.
                setState { copy(isLoading = false) }
                sendEvent(AuthEvent.NavigateToHome)
            } catch (e: Exception) {
                setState { copy(isLoading = false, errorMessage = e.localizedMessage ?: "No se pudo registrar la cuenta") }
            }
        }
    }
}
