package com.esan.sportpro.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.esan.sportpro.navigation.UserRole

/**
 * Login de US-002. No reemplaza a [com.esan.sportpro.ui.cuentas.LoginScreen]: ese sigue siendo el
 * login mínimo original, y este es el definitivo con bloqueo de intentos y recuperación.
 */
@Composable
fun LoginUsuarioScreen(
    onSesionIniciada: (UserRole) -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToRecuperar: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LoginEvent.SesionIniciada -> onSesionIniciada(event.rol)
                is LoginEvent.Snackbar -> snackbarHostState.showSnackbar(event.mensaje)
            }
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
            Text(text = "SportPro", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Inicia sesión para continuar",
                style = MaterialTheme.typography.bodyMedium,
            )

            OutlinedTextField(
                value = state.correo,
                onValueChange = viewModel::onCorreoChanged,
                label = { Text("Correo") },
                singleLine = true,
                isError = state.errores.containsKey(CampoLogin.CORREO),
                supportingText = state.errores[CampoLogin.CORREO]?.let { mensaje ->
                    { Text(mensaje) }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
            )

            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChanged,
                label = { Text("Contraseña") },
                singleLine = true,
                isError = state.errores.containsKey(CampoLogin.PASSWORD),
                supportingText = state.errores[CampoLogin.PASSWORD]?.let { mensaje ->
                    { Text(mensaje) }
                },
                visualTransformation = if (state.mostrarPassword) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = viewModel::onToggleMostrarPassword) {
                        Icon(
                            imageVector = if (state.mostrarPassword) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                            contentDescription = if (state.mostrarPassword) {
                                "Ocultar contraseña"
                            } else {
                                "Mostrar contraseña"
                            },
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            state.errorGeneral?.let { mensaje ->
                Text(
                    text = mensaje,
                    color = if (state.bloqueado) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            // El correo existe pero sin verificar: se ofrece reenviar el mensaje.
            if (state.correoSinVerificar) {
                TextButton(
                    onClick = viewModel::reenviarVerificacion,
                    enabled = !state.isLoading,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text("Reenviar correo de verificación")
                }
            }

            Button(
                onClick = viewModel::login,
                enabled = !state.isLoading && !state.bloqueado,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Ingresar")
                }
            }

            if (state.bloqueado) {
                Text(
                    text = "Puedes volver a intentarlo en ${state.segundosRestantesBloqueo} segundos",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onNavigateToRecuperar) {
                    Text("¿Olvidaste tu contraseña?")
                }
                TextButton(onClick = onNavigateToRegister) {
                    Text("¿No tienes cuenta? Regístrate")
                }
            }
        }
    }
}