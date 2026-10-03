package com.esan.sportpro.data.partido

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Recuerda en el dispositivo qué partido se estaba registrando, para restaurarlo si la app se
 * cierra o el teléfono se apaga durante el partido. Los eventos, el marcador y el cronómetro se
 * recuperan desde la caché local de Firestore.
 */
@Singleton
class PartidoEnCursoStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("sportpro_partido", Context.MODE_PRIVATE)

    var partidoId: String?
        get() = prefs.getString(KEY, null)
        set(value) {
            prefs.edit().apply {
                if (value == null) remove(KEY) else putString(KEY, value)
            }.apply()
        }

    fun limpiarSi(id: String) {
        if (partidoId == id) partidoId = null
    }

    private companion object {
        const val KEY = "partido_en_curso"
    }
}
