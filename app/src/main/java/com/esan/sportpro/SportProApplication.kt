package com.esan.sportpro

import android.app.Application
import com.esan.sportpro.messaging.SportProMessagingService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SportProApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // US-027: canales de notificación para los anuncios (normales y urgentes).
        SportProMessagingService.crearCanales(this)
    }
}
