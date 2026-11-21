package com.esan.sportpro.messaging

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.esan.sportpro.MainActivity
import com.esan.sportpro.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Recepción de push notifications (US-027). Los anuncios llegan por temas de FCM (ver
 * `data/notificaciones/TemasPush.kt`), así que el token no se guarda en Firestore: al renovarse,
 * las suscripciones a temas siguen vigentes.
 */
class SportProMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // No se registra el token: puede identificar el dispositivo y no hace falta con temas.
        Log.d(TAG, "Token FCM renovado")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        // Con la app en segundo plano, Android muestra solo las notificaciones con `notification`;
        // en primer plano llegan aquí y se muestran a mano.
        val titulo = message.notification?.title ?: message.data["titulo"] ?: return
        val cuerpo = message.notification?.body ?: message.data["mensaje"].orEmpty()
        val urgente = message.data["prioridad"] == "URGENTE"
        mostrar(this, titulo, cuerpo, urgente, message.messageId?.hashCode() ?: titulo.hashCode())
    }

    companion object {
        private const val TAG = "SportProMessaging"
        const val CANAL_AVISOS = "avisos"
        const val CANAL_URGENTES = "avisos_urgentes"

        /** Crea los canales (Android 8+). Se llama al iniciar la app. */
        fun crearCanales(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val gestor = context.getSystemService(NotificationManager::class.java) ?: return
            gestor.createNotificationChannel(
                NotificationChannel(CANAL_AVISOS, "Anuncios", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Anuncios de la academia"
                },
            )
            gestor.createNotificationChannel(
                NotificationChannel(CANAL_URGENTES, "Anuncios urgentes", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Anuncios marcados como urgentes"
                },
            )
        }

        fun mostrar(context: Context, titulo: String, cuerpo: String, urgente: Boolean, id: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            val abrirApp = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val notificacion = NotificationCompat.Builder(context, if (urgente) CANAL_URGENTES else CANAL_AVISOS)
                .setSmallIcon(R.drawable.ic_notificacion)
                .setContentTitle(titulo)
                .setContentText(cuerpo)
                .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
                .setPriority(if (urgente) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(abrirApp)
                .build()
            runCatching { NotificationManagerCompat.from(context).notify(id, notificacion) }
                .onFailure { Log.w(TAG, "No se pudo mostrar la notificación", it) }
        }
    }
}
