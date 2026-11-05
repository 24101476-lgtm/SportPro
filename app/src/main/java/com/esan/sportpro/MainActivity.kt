package com.esan.sportpro

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import com.esan.sportpro.core.theme.PreferenciaTema
import com.esan.sportpro.core.theme.SportProTheme
import com.esan.sportpro.navigation.SportProNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferenciaTema.iniciar(this)
        enableEdgeToEdge()
        setContent {
            val oscuro = PreferenciaTema.esOscuro()
            // Íconos de las barras del sistema según la apariencia elegida en la app.
            LaunchedEffect(oscuro) {
                val estilo = if (oscuro) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = estilo, navigationBarStyle = estilo)
            }
            SportProTheme(darkTheme = oscuro) {
                SportProNavHost()
            }
        }
    }
}
