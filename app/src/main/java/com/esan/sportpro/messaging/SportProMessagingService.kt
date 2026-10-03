package com.esan.sportpro.messaging

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/** Recepción de push notifications (US-027: anuncios internos y notificaciones push). */
class SportProMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Nuevo token FCM: $token")
        // TODO(US-027): persistir el token en Firestore (colección "usuarios") para poder
        // enviar avisos dirigidos por rol/equipo/academia.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Mensaje push recibido: ${message.notification?.title}")
        // TODO(US-027): construir y mostrar la notificación local (NotificationCompat) y
        // resolver la navegación según el payload (data message) cuando el usuario la toque.
    }

    companion object {
        private const val TAG = "SportProMessaging"
    }
}
