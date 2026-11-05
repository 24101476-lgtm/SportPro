package com.esan.sportpro.core.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Apariencia elegida por el usuario. Por defecto la app es clara; "Sistema" sigue al teléfono. */
enum class ModoTema(val etiqueta: String) {
    SISTEMA("Sistema"),
    CLARO("Claro"),
    OSCURO("Oscuro"),
}

/**
 * Guarda la apariencia en el teléfono (SharedPreferences) y la expone como estado de Compose,
 * así toda la app cambia al instante. Se inicia una vez en `MainActivity`.
 */
object PreferenciaTema {
    private const val ARCHIVO = "sportpro_apariencia"
    private const val CLAVE = "modo_tema"
    private var prefs: SharedPreferences? = null

    var modo by mutableStateOf(ModoTema.CLARO)
        private set

    fun iniciar(context: Context) {
        val guardadas = context.applicationContext.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
        prefs = guardadas
        modo = guardadas.getString(CLAVE, null)
            ?.let { nombre -> ModoTema.entries.firstOrNull { it.name == nombre } }
            ?: ModoTema.CLARO
    }

    fun cambiar(nuevo: ModoTema) {
        modo = nuevo
        prefs?.edit()?.putString(CLAVE, nuevo.name)?.apply()
    }

    /** Pasa a la siguiente opción: Sistema → Claro → Oscuro → Sistema. */
    fun siguiente() = cambiar(ModoTema.entries[(modo.ordinal + 1) % ModoTema.entries.size])

    @Composable
    fun esOscuro(): Boolean = when (modo) {
        ModoTema.SISTEMA -> isSystemInDarkTheme()
        ModoTema.CLARO -> false
        ModoTema.OSCURO -> true
    }
}
