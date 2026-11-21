package com.esan.sportpro.data.notificaciones

import android.util.Log
import com.esan.sportpro.domain.academia.EquipoRepository
import com.esan.sportpro.domain.cuentas.PerfilActual
import com.esan.sportpro.domain.jugadores.JugadorRepository
import com.esan.sportpro.domain.jugadores.VinculacionRepository
import com.esan.sportpro.navigation.UserRole
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * US-027: temas de FCM a los que se suscribe cada dispositivo. La función `notificar` de Supabase
 * envía cada anuncio a un tema, así el servidor no necesita guardar ni leer tokens.
 *
 * - `academia_{id}`: todo el club.
 * - `academia_{id}_rol_{ROL}`: solo entrenadores, solo jugadores o solo padres.
 * - `equipo_{id}`: un equipo (sus jugadores, su entrenador y los padres de sus jugadores).
 * - `usuario_{uid}`: avisos para una sola persona, como la respuesta a una vinculación (US-007).
 */
object TemasPush {
    fun academia(academiaId: String) = "academia_${limpio(academiaId)}"
    fun rol(academiaId: String, rol: UserRole) = "${academia(academiaId)}_rol_${rol.name}"
    fun equipo(equipoId: String) = "equipo_${limpio(equipoId)}"
    fun usuario(uid: String) = "usuario_${limpio(uid)}"

    /** FCM solo acepta letras, números y `-_.~%` en el nombre del tema. */
    private fun limpio(texto: String) = texto.replace(Regex("[^A-Za-z0-9_.~%-]"), "-")
}

/** Suscribe el dispositivo a los temas que le corresponden según el perfil y sus equipos. */
@Singleton
class SuscripcionPush @Inject constructor(
    private val jugadorRepository: JugadorRepository,
    private val equipoRepository: EquipoRepository,
    private val vinculacionRepository: VinculacionRepository,
) {
    private var suscritos: Set<String> = emptySet()

    suspend fun sincronizar(perfil: PerfilActual) {
        val temas = buildSet {
            add(TemasPush.academia(perfil.academiaId))
            add(TemasPush.rol(perfil.academiaId, perfil.rol))
            add(TemasPush.usuario(perfil.uid))
            equiposDe(perfil).forEach { add(TemasPush.equipo(it)) }
        }
        val mensajeria = FirebaseMessaging.getInstance()
        (suscritos - temas).forEach { tema -> runCatching { mensajeria.unsubscribeFromTopic(tema).await() } }
        (temas - suscritos).forEach { tema ->
            runCatching { mensajeria.subscribeToTopic(tema).await() }
                .onFailure { Log.w(TAG, "No se pudo suscribir a $tema", it) }
        }
        suscritos = temas
    }

    /** Al cerrar sesión el dispositivo deja de recibir los avisos de esa cuenta. */
    suspend fun limpiar() {
        val mensajeria = FirebaseMessaging.getInstance()
        suscritos.forEach { tema -> runCatching { mensajeria.unsubscribeFromTopic(tema).await() } }
        suscritos = emptySet()
        runCatching { mensajeria.deleteToken().await() }
    }

    private suspend fun equiposDe(perfil: PerfilActual): Set<String> = runCatching {
        withTimeoutOrNull(ESPERA_MS) {
            when (perfil.rol) {
                UserRole.JUGADOR -> setOfNotNull(
                    jugadorRepository.observarFichaPropia(perfil.uid).first()?.equipoId?.takeIf { it.isNotBlank() },
                )
                UserRole.PADRE_DE_FAMILIA -> vinculacionRepository.observarMisVinculaciones(perfil.uid).first()
                    .filter { it.aprobada }
                    .mapNotNull { it.equipoId.takeIf { id -> id.isNotBlank() } }
                    .toSet()
                UserRole.ENTRENADOR -> equipoRepository.observarEquipos(perfil.academiaId).first()
                    .filter { it.activo && it.dirigidoPor(perfil.uid, perfil.nombre) }
                    .map { it.id }
                    .toSet()
                UserRole.ADMINISTRADOR -> emptySet()
            }
        }.orEmpty()
    }.getOrDefault(emptySet())

    private companion object {
        const val TAG = "SuscripcionPush"
        const val ESPERA_MS = 8_000L
    }
}
