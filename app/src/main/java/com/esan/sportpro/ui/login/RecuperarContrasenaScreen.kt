package com.esan.sportpro.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.esan.sportpro.core.viewmodel.BaseViewModel
import com.esan.sportpro.domain.cuentas.SesionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecuperarUiState(
    val correo: String = "",
    val error: String? = null,
    val isLoading: Boolean = false,
    val enviado: Boolean = false,
)

sealed interface RecuperarEvent {
    data object Enviado : RecuperarEvent
}

/** US-002: envío del correo de restablecimiento. */
@HiltViewModel
class RecuperarContrasenaViewModel @Inject constructor(
    private val sesionRepository: SesionRepository,
) : BaseViewModel<RecuperarUiState, RecuperarEvent>(RecuperarUiState()) {

    fun onCorreoChanged(valor: String) = setState { copy(correo = valor, error = null) }

    fun enviar() {
        val estado = currentState
        if (estado.isLoading) return

        val error = ValidacionLogin.validarCorreo(estado.correo)
        if (error != null) {
            setState { copy(error = error) }
            return
        }

        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            sesionRepository.restablecerContrasena(estado.correo)
            setState { copy(isLoading = false, enviado = true) }
            sendEvent(RecuperarEvent.Enviado)
        }
    }
}

/**
 * Recuperación de contraseña (US-002). La respuesta es siempre la misma exista o no la cuenta,
 * para no revelar qué correos están registrados.
 */
@Composable
fun RecuperarContrasenaScreen(
    onNavigateBack: () -> Unit,
    viewModel: RecuperarContrasenaViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect {
            snackbarHostState.showSnackbar(MensajesLogin.RECUPERACION_ENVIADA)
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = "Recuperar contraseña", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Escribe tu correo y te enviaremos un enlace para crear una nueva contraseña.",
                style = MaterialTheme.typography.bodyMedium,
            )

            OutlinedTextField(
                value = state.correo,
                onValueChange = viewModel::onCorreoChanged,
                label = { Text("Correo") },
                singleLine = true,
                isError = state.error != null,
                supportingText = state.error?.let { mensaje -> { Text(mensaje) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
            )

            Button(
                onClick = viewModel::enviar,
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Enviar enlace")
                }
            }

            TextButton(
                onClick = onNavigateBack,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text("Volver al inicio de sesión")
            }
        }
    }
}