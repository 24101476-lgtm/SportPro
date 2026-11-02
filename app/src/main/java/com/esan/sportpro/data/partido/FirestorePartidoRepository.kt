package com.esan.sportpro.data.partido

import android.util.Log
import com.esan.sportpro.domain.partido.AmbitoEvento
import com.esan.sportpro.domain.partido.BorradorEvento
import com.esan.sportpro.domain.partido.CatalogoBase
import com.esan.sportpro.domain.partido.EquipoEvento
import com.esan.sportpro.domain.partido.EstadoEvento
import com.esan.sportpro.domain.partido.EstadoPartido
import com.esan.sportpro.domain.partido.EstadoRespuestaConvocatoria
import com.esan.sportpro.domain.partido.EventoPartido
import com.esan.sportpro.domain.partido.JugadorConvocado
import com.esan.sportpro.domain.partido.NuevoPartido
import com.esan.sportpro.domain.partido.Partido
import com.esan.sportpro.domain.partido.PartidoRepository
import com.esan.sportpro.domain.partido.RespuestaConvocatoria
import com.esan.sportpro.domain.partido.ResultadoPenal
import com.esan.sportpro.domain.partido.TipoEvento
import com.esan.sportpro.domain.partido.TiposEvento
import com.esan.sportpro.domain.partido.TiposPartido
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import com.esan.sportpro.domain.partido.JugadorPlantel

/**
 * Implementación con Cloud Firestore.
 *
 * Estructura:
 *
 * - partidos/{partidoId}
 *      Datos generales y programación del partido.
 *
 * - partidos/{partidoId}/respuestasConvocatoria/{jugadorId}
 *      Invitaciones y respuestas de US-014 / US-015.
 *
 * - partidos/{partidoId}/convocados/{jugadorId}
 *      Plantel final, titulares y suplentes de US-016 / partido en vivo.
 *
 * - partidos/{partidoId}/eventos/{eventoId}
 *      Eventos ocurridos durante el partido.
 *
 * - partidos/{partidoId}/historial/{registroId}
 *      Historial de programación, reprogramación y cancelación.
 *
 * - catalogoEventos/{tipoId}
 *      Catálogo configurable de tipos de evento.
 *
 * Los listeners usan MetadataChanges.INCLUDE cuando es útil para detectar
 * escrituras locales pendientes de sincronización.
 *
 * Las escrituras no usan await(): Firestore permite conservarlas localmente
 * y sincronizarlas posteriormente cuando vuelve la conexión.
 */
@Singleton
class FirestorePartidoRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : PartidoRepository {

    // ---------------------------------------------------------------- Referencias Firestore

    private val partidos
        get() = firestore.collection(COL_PARTIDOS)

    private fun eventosRef(
        partidoId: String
    ) = partidos
        .document(partidoId)
        .collection(COL_EVENTOS)

    private fun convocadosRef(
        partidoId: String
    ) = partidos
        .document(partidoId)
        .collection(COL_CONVOCADOS)

    private fun respuestasConvocatoriaRef(
        partidoId: String
    ) = partidos
        .document(partidoId)
        .collection(COL_RESPUESTAS_CONVOCATORIA)

    private fun historialPartidoRef(
        partidoId: String
    ) = partidos
        .document(partidoId)
        .collection(COL_HISTORIAL)

    // ---------------------------------------------------------------- Usuario actual

    override val uidActual: String?
        get() = auth.currentUser?.uid

    private val nombreActual: String?
        get() = auth.currentUser?.let {
            it.displayName ?: it.email
        }

    // ---------------------------------------------------------------- Errores de escritura

    private val _erroresEscritura =
        MutableSharedFlow<String>(
            extraBufferCapacity = 8
        )

    override val erroresEscritura: SharedFlow<String> =
        _erroresEscritura.asSharedFlow()

    override fun observarJugadoresEquipo(
        equipoId: String,
        academiaId: String?,
    ): Flow<List<JugadorPlantel>> = callbackFlow {

        var consulta = firestore
            .collection("jugadores")
            .whereEqualTo("equipoId", equipoId)
        if (academiaId != null) {
            consulta = consulta.whereEqualTo("academiaId", academiaId)
        }
        val reg = consulta
            .addSnapshotListener { snap, error ->

                if (error != null) {
                    Log.w(
                        TAG,
                        "observarJugadoresEquipo",
                        error
                    )

                    close(error)
                    return@addSnapshotListener
                }

                val jugadores =
                    snap
                        ?.documents
                        .orEmpty()
                        // Solo jugadores activos y con registro completo: un menor sin apoderado
                        // vinculado no puede ser convocado (ReglasApoderado).
                        .filter { doc ->
                            doc.getBoolean("activo") != false &&
                                doc.getString("estadoRegistro") != "PENDIENTE_APODERADO"
                        }
                        .map { doc ->

                            JugadorPlantel(
                                id = doc.id,
                                nombre =
                                    doc.getString("nombre")
                                        ?: listOfNotNull(doc.getString("nombres"), doc.getString("apellidos"))
                                            .joinToString(" ")
                                            .ifBlank { "Jugador sin nombre" },
                                equipoId =
                                    doc.getString("equipoId")
                                        ?: equipoId,
                            )
                        }
                        .sortedBy {
                            it.nombre.lowercase()
                        }

                trySend(
                    jugadores
                )
            }

        awaitClose {
            reg.remove()
        }
    }

    // ---------------------------------------------------------------- Lecturas en tiempo real

    override fun observarRespuestaConvocatoria(
        partidoId: String,
        jugadorId: String,
    ): Flow<RespuestaConvocatoria?> = callbackFlow {

        val reg =
            respuestasConvocatoriaRef(partidoId)
                .document(jugadorId)
                .addSnapshotListener { doc, error ->

                    if (error != null) {
                        Log.w(
                            TAG,
                            "observarRespuestaConvocatoria",
                            error
                        )

                        close(error)
                        return@addSnapshotListener
                    }

                    if (
                        doc == null ||
                        !doc.exists()
                    ) {
                        trySend(null)
                        return@addSnapshotListener
                    }

                    val estado =
                        runCatching {
                            EstadoRespuestaConvocatoria.valueOf(
                                doc.getString("estado")
                                    ?: EstadoRespuestaConvocatoria
                                        .SIN_RESPONDER
                                        .name
                            )
                        }.getOrDefault(
                            EstadoRespuestaConvocatoria.SIN_RESPONDER
                        )

                    trySend(
                        RespuestaConvocatoria(
                            jugadorId = doc.id,
                            nombre =
                                doc.getString("nombre")
                                    ?: "Jugador",
                            dorsal =
                                doc.getLong("dorsal")
                                    ?.toInt(),
                            estado = estado,
                            motivo =
                                doc.getString("motivo"),
                            respondidoPorUid =
                                doc.getString(
                                    "respondidoPorUid"
                                ),
                            respondidoPorNombre =
                                doc.getString(
                                    "respondidoPorNombre"
                                ),
                            fechaRespuestaMs =
                                doc.getTimestamp(
                                    "fechaRespuesta"
                                )
                                    ?.toDate()
                                    ?.time,
                            convocadoPorUid =
                                doc.getString(
                                    "convocadoPorUid"
                                ),
                            fechaConvocatoriaMs =
                                doc.getTimestamp(
                                    "fechaConvocatoria"
                                )
                                    ?.toDate()
                                    ?.time,
                        )
                    )
                }

        awaitClose {
            reg.remove()
        }
    }

    override fun observarPartidos(): Flow<List<Partido>> =
        callbackFlow {

            /*
             * Sin orderBy:
             * se ordena en el dispositivo para incluir también
             * partidos recién creados sin conexión.
             */
            val reg = partidos
                .addSnapshotListener(
                    MetadataChanges.INCLUDE
                ) { snap, error ->

                    if (error != null) {
                        Log.w(
                            TAG,
                            "observarPartidos",
                            error
                        )

                        close(error)
                        return@addSnapshotListener
                    }

                    val lista = snap?.documents
                        .orEmpty()
                        .mapNotNull {
                            it.toPartido()
                        }
                        .sortedByDescending {
                            it.fechaMs ?: Long.MAX_VALUE
                        }

                    trySend(lista)
                }

            awaitClose {
                reg.remove()
            }
        }

    override fun observarPartido(
        partidoId: String
    ): Flow<Partido?> =
        callbackFlow {

            val reg = partidos
                .document(partidoId)
                .addSnapshotListener(
                    MetadataChanges.INCLUDE
                ) { snap, error ->

                    if (error != null) {
                        Log.w(
                            TAG,
                            "observarPartido",
                            error
                        )

                        close(error)
                        return@addSnapshotListener
                    }

                    trySend(
                        snap?.toPartido()
                    )
                }

            awaitClose {
                reg.remove()
            }
        }

    override fun observarEventos(
        partidoId: String
    ): Flow<List<EventoPartido>> =
        callbackFlow {

            val reg = eventosRef(partidoId)
                .addSnapshotListener(
                    MetadataChanges.INCLUDE
                ) { snap, error ->

                    if (error != null) {
                        Log.w(
                            TAG,
                            "observarEventos",
                            error
                        )

                        close(error)
                        return@addSnapshotListener
                    }

                    val eventos = snap?.documents
                        .orEmpty()
                        .mapNotNull {
                            it.toEvento()
                        }

                    trySend(eventos)
                }

            awaitClose {
                reg.remove()
            }
        }

    override fun observarConvocados(
        partidoId: String
    ): Flow<List<JugadorConvocado>> =
        callbackFlow {

            val reg = convocadosRef(partidoId)
                .addSnapshotListener { snap, error ->

                    if (error != null) {
                        Log.w(
                            TAG,
                            "observarConvocados",
                            error
                        )

                        close(error)
                        return@addSnapshotListener
                    }

                    val lista = snap?.documents
                        .orEmpty()
                        .map { documento ->

                            JugadorConvocado(
                                id = documento.id,

                                nombre =
                                    documento.getString(
                                        "nombre"
                                    ) ?: "Sin nombre",

                                dorsal =
                                    documento.getLong(
                                        "dorsal"
                                    )?.toInt(),

                                titular =
                                    documento.getBoolean(
                                        "titular"
                                    ) ?: false,

                                posicion =
                                    documento.getString(
                                        "posicion"
                                    ),
                            )
                        }
                        .sortedWith(
                            compareBy(
                                { !it.titular },
                                {
                                    it.dorsal
                                        ?: Int.MAX_VALUE
                                }
                            )
                        )

                    trySend(lista)
                }

            awaitClose {
                reg.remove()
            }
        }

    // ---------------------------------------------------------------- US-014: respuestas convocatoria

    override fun observarRespuestasConvocatoria(
        partidoId: String
    ): Flow<List<RespuestaConvocatoria>> =
        callbackFlow {

            val reg = respuestasConvocatoriaRef(
                partidoId
            ).addSnapshotListener(
                MetadataChanges.INCLUDE
            ) { snap, error ->

                if (error != null) {
                    Log.w(
                        TAG,
                        "observarRespuestasConvocatoria",
                        error
                    )

                    close(error)
                    return@addSnapshotListener
                }

                val respuestas = snap?.documents
                    .orEmpty()
                    .mapNotNull { documento ->

                        val nombre =
                            documento.getString(
                                "nombre"
                            )
                                ?: return@mapNotNull null

                        RespuestaConvocatoria(
                            jugadorId =
                                documento.id,

                            nombre =
                                nombre,

                            dorsal =
                                documento.getLong(
                                    "dorsal"
                                )?.toInt(),

                            estado =
                                enumOr(
                                    documento.getString(
                                        "estado"
                                    ),
                                    EstadoRespuestaConvocatoria
                                        .SIN_RESPONDER
                                ),

                            motivo =
                                documento.getString(
                                    "motivo"
                                ),

                            respondidoPorUid =
                                documento.getString(
                                    "respondidoPorUid"
                                ),

                            respondidoPorNombre =
                                documento.getString(
                                    "respondidoPorNombre"
                                ),

                            fechaRespuestaMs =
                                documento.millis(
                                    "fechaRespuesta"
                                ),

                            convocadoPorUid =
                                documento.getString(
                                    "convocadoPorUid"
                                ),

                            fechaConvocatoriaMs =
                                documento.millis(
                                    "fechaConvocatoria"
                                ),
                        )
                    }
                    .sortedWith(
                        compareBy<RespuestaConvocatoria>(
                            {
                                it.estado !=
                                        EstadoRespuestaConvocatoria
                                            .SIN_RESPONDER
                            },
                            {
                                it.nombre.lowercase()
                            }
                        )
                    )

                trySend(respuestas)
            }

            awaitClose {
                reg.remove()
            }
        }

    // ---------------------------------------------------------------- US-013: catálogo de eventos

    override fun observarCatalogo():
            Flow<List<TipoEvento>> =
        callbackFlow {

            val reg = firestore
                .collection(COL_CATALOGO)
                .addSnapshotListener { snap, error ->

                    if (error != null) {
                        Log.w(
                            TAG,
                            "observarCatalogo",
                            error
                        )

                        trySend(
                            CatalogoBase.tipos
                        )

                        return@addSnapshotListener
                    }

                    if (
                        snap == null ||
                        snap.isEmpty
                    ) {
                        /*
                         * Primera vez:
                         * publica catálogo base.
                         */
                        if (
                            snap != null &&
                            !snap.metadata.isFromCache
                        ) {
                            sembrarCatalogo()
                        }

                        trySend(
                            CatalogoBase.tipos
                        )

                        return@addSnapshotListener
                    }

                    val tipos = snap.documents
                        .mapNotNull {
                            it.toTipoEvento()
                        }
                        .filter {
                            it.activo
                        }
                        .sortedBy {
                            it.orden
                        }

                    trySend(tipos)
                }

            awaitClose {
                reg.remove()
            }
        }

    override fun observarCatalogoCompleto():
            Flow<List<TipoEvento>> =
        callbackFlow {

            val reg = firestore
                .collection(COL_CATALOGO)
                .addSnapshotListener { snap, error ->

                    if (error != null) {
                        Log.w(
                            TAG,
                            "observarCatalogoCompleto",
                            error
                        )

                        close(error)
                        return@addSnapshotListener
                    }

                    if (
                        snap == null ||
                        snap.isEmpty
                    ) {
                        if (
                            snap != null &&
                            !snap.metadata.isFromCache
                        ) {
                            sembrarCatalogo()
                        }

                        trySend(
                            CatalogoBase.tipos
                        )

                        return@addSnapshotListener
                    }

                    val tipos = snap.documents
                        .mapNotNull {
                            it.toTipoEvento()
                        }
                        .sortedBy {
                            it.orden
                        }

                    trySend(tipos)
                }

            awaitClose {
                reg.remove()
            }
        }

    override fun observarRolActual():
            Flow<String?> =
        callbackFlow {

            val uid = uidActual

            if (uid == null) {
                trySend(null)

                awaitClose { }

                return@callbackFlow
            }

            val reg = firestore
                .collection(COL_USUARIOS)
                .document(uid)
                .addSnapshotListener { snap, _ ->

                    trySend(
                        snap?.getString("rol")
                    )
                }

            awaitClose {
                reg.remove()
            }
        }

    // ---------------------------------------------------------------- US-013: escrituras catálogo

    override fun responderConvocatoria(
        partidoId: String,
        jugadorId: String,
        estado: EstadoRespuestaConvocatoria,
        motivo: String?,
    ) {

        val cambios =
            mapOf(
                "estado" to estado.name,

                "motivo" to
                        if (
                            estado ==
                            EstadoRespuestaConvocatoria.NO_DISPONIBLE
                        ) {
                            motivo
                                ?.trim()
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                        } else {
                            null
                        },

                "respondidoPorUid" to
                        uidActual,

                "respondidoPorNombre" to
                        nombreActual,

                "fechaRespuesta" to
                        FieldValue.serverTimestamp(),
            )

        respuestasConvocatoriaRef(
            partidoId
        )
            .document(
                jugadorId
            )
            .update(
                cambios
            )
            .addOnFailureListener {
                reportarError(
                    "responderConvocatoria",
                    it
                )
            }
    }

    override fun guardarAlineacionFinal(
        partidoId: String,
        jugadores: List<JugadorConvocado>,
        convocadosPreviosIds: List<String>,
    ) {
        val partidoRef =
            partidos.document(partidoId)

        val convocados =
            convocadosRef(partidoId)

        val batch =
            firestore.batch()

        /*
         * Si anteriormente existía una alineación y algún jugador
         * ya no forma parte de ella, eliminamos únicamente ese
         * documento para no dejar jugadores antiguos.
         */
        val nuevosIds =
            jugadores
                .map { it.id }
                .toSet()

        convocadosPreviosIds
            .filterNot { it in nuevosIds }
            .forEach { jugadorId ->

                batch.delete(
                    convocados.document(jugadorId)
                )
            }

        /*
         * Guardamos titulares y suplentes.
         *
         * El ID del documento es el jugadorId real.
         */
        jugadores.forEach { jugador ->

            batch.set(
                convocados.document(jugador.id),
                mapOf(
                    "nombre" to jugador.nombre,
                    "dorsal" to jugador.dorsal,
                    "titular" to jugador.titular,
                    "posicion" to jugador.posicion,
                )
            )
        }

        /*
         * La alineación pasa a estar oficialmente confirmada.
         */
        batch.update(
            partidoRef,
            mapOf(
                "alineacionConfirmada" to true,
                "alineacionConfirmadaPor" to uidActual,
                "alineacionConfirmadaEn" to
                        FieldValue.serverTimestamp(),
            )
        )

        batch.commitLogged(
            "guardarAlineacionFinal"
        )
    }

    override fun crearTipoEvento(
        tipo: TipoEvento
    ): String {

        val ref = firestore
            .collection(COL_CATALOGO)
            .document()

        ref.set(
            tipo.toMapCatalogo(
                version = 1,
                incluirCreacion = true,
            )
        ).addOnFailureListener {
            reportarError(
                "crearTipoEvento",
                it
            )
        }

        return ref.id
    }

    override fun actualizarTipoEvento(
        tipo: TipoEvento
    ) {
        firestore
            .collection(COL_CATALOGO)
            .document(tipo.id)
            .update(
                tipo.toMapCatalogo(
                    version =
                        tipo.version + 1,

                    incluirCreacion =
                        false,
                )
            )
            .addOnFailureListener {
                reportarError(
                    "actualizarTipoEvento",
                    it
                )
            }
    }

    override fun cambiarEstadoTipoEvento(
        tipoId: String,
        activo: Boolean,
        versionActual: Int,
    ) {
        firestore
            .collection(COL_CATALOGO)
            .document(tipoId)
            .update(
                mapOf(
                    "activo" to activo,

                    "version" to
                            versionActual + 1,

                    "actualizadoPor" to
                            uidActual,

                    "actualizadoEn" to
                            FieldValue
                                .serverTimestamp(),
                )
            )
            .addOnFailureListener {
                reportarError(
                    "cambiarEstadoTipoEvento",
                    it
                )
            }
    }

    // ---------------------------------------------------------------- US-014: crear partido programado

    override fun crearPartidoProgramado(
        nombreEquipo: String,
        nombreRival: String,
        fechaMs: Long,
        hora: String,
        lugar: String,
        tipoPartido: String,
        duracionTiempoMin: Int,
        maxConvocados: Int,
        fechaLimiteRespuestaMs: Long,
        equipoId: String?,
        academiaId: String?,
        categoria: String?,
    ): String {

        val uid = uidActual
        // El formulario usa "Amistoso" / "Liga" / "Torneo"; Estadísticas filtra por AMISTOSO, LIGA, TORNEO.
        val tipo = TiposPartido.todos.firstOrNull { (valor, etiqueta) ->
            tipoPartido.trim().equals(valor, ignoreCase = true) || tipoPartido.trim().equals(etiqueta, ignoreCase = true)
        }?.first ?: TiposPartido.AMISTOSO

        val partidoRef =
            partidos.document()

        val historialRef =
            historialPartidoRef(
                partidoRef.id
            ).document()

        val batch =
            firestore.batch()

        batch.set(
            partidoRef,
            mapOf(
                "nombreEquipo" to
                        nombreEquipo.trim(),

                "nombreRival" to
                        nombreRival.trim(),

                // Antes `categoria` guardaba el tipo de partido y no había equipo ni academia:
                // el partido no aparecía en la lista (se filtra por academia) y la convocatoria
                // quedaba sin jugadores.
                "categoria" to
                        categoria,

                "equipoId" to
                        equipoId,

                "academiaId" to
                        academiaId,

                "rivalNombre" to
                        nombreRival.trim(),

                "tipo" to
                        tipo,

                "temporada" to
                        Calendar.getInstance().apply { timeInMillis = fechaMs }.get(Calendar.YEAR).toString(),

                "golesEquipo" to
                        0,

                "golesRival" to
                        0,

                "actaCerrada" to
                        false,

                "estado" to
                        EstadoPartido
                            .PROGRAMADO.name,

                "fecha" to
                        Date(fechaMs),

                "hora" to
                        hora.trim(),

                "lugar" to
                        lugar.trim(),

                "tipoPartido" to
                        tipoPartido.trim(),

                "duracionTiempoMin" to
                        duracionTiempoMin,

                "maxConvocados" to
                        maxConvocados,

                "fechaLimiteRespuesta" to
                        Date(
                            fechaLimiteRespuestaMs
                        ),

                "alineacionConfirmada" to
                        false,

                "cambiosPermitidos" to
                        5,

                "enDescanso" to
                        false,

                "operadorUid" to
                        uid,

                "entrenadorUid" to
                        uid,

                "creadoPor" to
                        uid,

                "convocatoriaPublicada" to
                        false,

                "creadoEn" to
                        FieldValue
                            .serverTimestamp(),
            )
        )

        /*
         * Primer registro del historial.
         */
        batch.set(
            historialRef,
            mapOf(
                "tipo" to
                        "PROGRAMACION_INICIAL",

                "fecha" to
                        Date(fechaMs),

                "hora" to
                        hora.trim(),

                "lugar" to
                        lugar.trim(),

                "fechaLimiteRespuesta" to
                        Date(
                            fechaLimiteRespuestaMs
                        ),

                "realizadoPor" to
                        uid,

                "registradoEn" to
                        FieldValue
                            .serverTimestamp(),
            )
        )

        batch.commitLogged(
            "crearPartidoProgramado"
        )

        return partidoRef.id
    }

    // ---------------------------------------------------------------- US-014: publicar convocatoria

    override fun publicarConvocatoria(
        partidoId: String,
        jugadores:
        List<RespuestaConvocatoria>,
    ) {
        val batch =
            firestore.batch()

        jugadores.forEach { jugador ->

            val ref =
                respuestasConvocatoriaRef(
                    partidoId
                ).document(
                    jugador.jugadorId
                )

            batch.set(
                ref,
                mapOf(
                    "nombre" to
                            jugador.nombre,

                    "dorsal" to
                            jugador.dorsal,

                    "estado" to
                            EstadoRespuestaConvocatoria
                                .SIN_RESPONDER
                                .name,

                    "motivo" to
                            null,

                    "respondidoPorUid" to
                            null,

                    "respondidoPorNombre" to
                            null,

                    "fechaRespuesta" to
                            null,

                    "convocadoPorUid" to
                            uidActual,

                    "fechaConvocatoria" to
                            FieldValue
                                .serverTimestamp(),

                    "ultimoRecordatorioEn" to
                            null,

                    "cantidadRecordatorios" to
                            0,
                )
            )
        }

        batch.update(
            partidos.document(
                partidoId
            ),
            mapOf(
                "convocatoriaPublicada" to
                        true,

                "convocatoriaPublicadaPor" to
                        uidActual,

                "convocatoriaPublicadaEn" to
                        FieldValue
                            .serverTimestamp(),
            )
        )

        batch.commitLogged(
            "publicarConvocatoria"
        )
    }

    // ---------------------------------------------------------------- US-014: reprogramar

    override fun reprogramarPartido(
        partidoId: String,
        nuevaFechaMs: Long,
        nuevaHora: String,
        nuevoLugar: String,
        nuevaFechaLimiteRespuestaMs: Long,
    ) {
        val partidoRef =
            partidos.document(
                partidoId
            )

        val historialRef =
            historialPartidoRef(
                partidoId
            ).document()

        val batch =
            firestore.batch()

        batch.update(
            partidoRef,
            mapOf(
                "fecha" to
                        Date(nuevaFechaMs),

                "hora" to
                        nuevaHora.trim(),

                "lugar" to
                        nuevoLugar.trim(),

                "fechaLimiteRespuesta" to
                        Date(
                            nuevaFechaLimiteRespuestaMs
                        ),

                "reprogramadoPor" to
                        uidActual,

                "reprogramadoEn" to
                        FieldValue
                            .serverTimestamp(),
            )
        )

        /*
         * No eliminamos programación anterior.
         * Agregamos una nueva entrada al historial.
         */
        batch.set(
            historialRef,
            mapOf(
                "tipo" to
                        "REPROGRAMACION",

                "fecha" to
                        Date(nuevaFechaMs),

                "hora" to
                        nuevaHora.trim(),

                "lugar" to
                        nuevoLugar.trim(),

                "fechaLimiteRespuesta" to
                        Date(
                            nuevaFechaLimiteRespuestaMs
                        ),

                "realizadoPor" to
                        uidActual,

                "registradoEn" to
                        FieldValue
                            .serverTimestamp(),
            )
        )

        batch.commitLogged(
            "reprogramarPartido"
        )
    }

    // ---------------------------------------------------------------- US-014: cancelar

    override fun cancelarPartido(
        partidoId: String,
        motivo: String,
    ) {
        val partidoRef =
            partidos.document(
                partidoId
            )

        val historialRef =
            historialPartidoRef(
                partidoId
            ).document()

        val batch =
            firestore.batch()

        /*
         * Cancelar NO elimina el partido.
         */
        batch.update(
            partidoRef,
            mapOf(
                "estado" to
                        EstadoPartido
                            .CANCELADO.name,

                "motivoCancelacion" to
                        motivo.trim(),

                "canceladoPor" to
                        uidActual,

                "canceladoEn" to
                        FieldValue
                            .serverTimestamp(),
            )
        )

        batch.set(
            historialRef,
            mapOf(
                "tipo" to
                        "CANCELACION",

                "motivo" to
                        motivo.trim(),

                "realizadoPor" to
                        uidActual,

                "registradoEn" to
                        FieldValue
                            .serverTimestamp(),
            )
        )

        batch.commitLogged(
            "cancelarPartido"
        )
    }

    // ---------------------------------------------------------------- US-014: recordatorio

    override fun enviarRecordatorioConvocatoria(
        partidoId: String,
        jugadoresPendientesIds:
        List<String>,
    ) {
        if (
            jugadoresPendientesIds
                .isEmpty()
        ) {
            return
        }

        val batch =
            firestore.batch()

        jugadoresPendientesIds
            .forEach { jugadorId ->

                val ref =
                    respuestasConvocatoriaRef(
                        partidoId
                    ).document(
                        jugadorId
                    )

                batch.update(
                    ref,
                    mapOf(
                        "ultimoRecordatorioEn" to
                                FieldValue
                                    .serverTimestamp(),

                        "cantidadRecordatorios" to
                                FieldValue.increment(
                                    1
                                ),
                    )
                )
            }

        batch.update(
            partidos.document(
                partidoId
            ),
            mapOf(
                "ultimoRecordatorioConvocatoria" to
                        FieldValue
                            .serverTimestamp(),

                "recordatorioEnviadoPor" to
                        uidActual,
            )
        )

        batch.commitLogged(
            "enviarRecordatorioConvocatoria"
        )
    }

    // ---------------------------------------------------------------- Partido de práctica existente

    override fun crearPartidoPractica(
        nombreEquipo: String,
        nombreRival: String,
        cambiosPermitidos: Int,
        duracionTiempoMin: Int,
    ): String {

        val uid = uidActual
        val ref = partidos.document()
        val batch = firestore.batch()

        batch.set(
            ref,
            mapOf(
                "nombreEquipo" to nombreEquipo,
                "nombreRival" to nombreRival,
                "categoria" to "Práctica",
                "tipo" to TiposPartido.AMISTOSO,
                "estado" to EstadoPartido.PROGRAMADO.name,
                "actaCerrada" to false,
                "alineacionConfirmada" to true,
                "cambiosPermitidos" to cambiosPermitidos,
                "duracionTiempoMin" to duracionTiempoMin,
                "enDescanso" to false,
                "golesEquipo" to 0,
                "golesRival" to 0,
                "operadorUid" to uid,
                "entrenadorUid" to uid,
                "creadoPor" to uid,
                "fecha" to FieldValue.serverTimestamp(),
            ),
        )

        val posiciones =
            listOf(
                "POR",
                "DEF",
                "DEF",
                "DEF",
                "DEF",
                "MED",
                "MED",
                "MED",
                "DEL",
                "DEL",
                "DEL",
            )

        (1..16).forEach { dorsal ->
            batch.set(
                ref.collection(COL_CONVOCADOS)
                    .document("j$dorsal"),
                mapOf(
                    "nombre" to "Jugador $dorsal",
                    "dorsal" to dorsal,
                    "titular" to (dorsal <= 11),
                    "posicion" to posiciones.getOrElse(dorsal - 1) { "SUP" },
                ),
            )
        }

        batch.commitLogged("crearPartidoPractica")
        return ref.id
    }

    override fun crearPartido(
        nuevo: NuevoPartido
    ): String {
        val uid = uidActual
        val ref = partidos.document()
        val batch = firestore.batch()

        batch.set(
            ref,
            mapOf(
                "equipoId" to nuevo.equipoId,
                "academiaId" to nuevo.academiaId,
                "nombreEquipo" to nuevo.nombreEquipo,
                "nombreRival" to nuevo.nombreRival,
                // Nombre de campo que usa el módulo de Estadísticas.
                "rivalNombre" to nuevo.nombreRival,
                "categoria" to nuevo.categoria,
                "tipo" to nuevo.tipo,
                "temporada" to nuevo.temporada,
                "estado" to EstadoPartido.PROGRAMADO.name,
                "actaCerrada" to false,
                "alineacionConfirmada" to nuevo.convocados.any { it.titular },
                "cambiosPermitidos" to nuevo.cambiosPermitidos,
                "duracionTiempoMin" to nuevo.duracionTiempoMin,
                "enDescanso" to false,
                "golesEquipo" to 0,
                "golesRival" to 0,
                "operadorUid" to uid,
                "entrenadorUid" to uid,
                "creadoPor" to uid,
                "fecha" to FieldValue.serverTimestamp(),
            ),
        )

        // Solo se copian datos deportivos: los convocados los ve cualquier
        // usuario autenticado, mientras que `jugadores` solo lo lee el personal de la academia.
        nuevo.convocados.forEach { jugador ->
            batch.set(
                ref.collection(COL_CONVOCADOS).document(jugador.id),
                mapOf(
                    "nombre" to jugador.nombre,
                    "dorsal" to jugador.dorsal,
                    "titular" to jugador.titular,
                    "posicion" to jugador.posicion,
                ),
            )
        }

        batch.commitLogged("crearPartido")
        return ref.id
    }

    override fun actualizarMarcador(
        partidoId: String,
        golesEquipo: Int,
        golesRival: Int,
    ) {
        partidos.document(partidoId)
            .update(
                mapOf(
                    "golesEquipo" to golesEquipo,
                    "golesRival" to golesRival,
                )
            )
            .addOnFailureListener {
                reportarError("actualizarMarcador", it)
            }
    }

    // ---------------------------------------------------------------- Registro eventos

    override fun registrarEvento(
        partidoId: String,
        borrador: BorradorEvento,
        incompleto: Boolean,
        fueraDeOrden: Boolean,
        excedeCambios: Boolean,
        eventosRelacionados:
        List<BorradorEvento>,
    ): String {

        val batch =
            firestore.batch()

        val ref =
            eventosRef(
                partidoId
            ).document()

        batch.set(
            ref,
            borrador.toMap(
                partidoId = partidoId,
                grupoId = ref.id,
                version = 1,
                incompleto = incompleto,
                fueraDeOrden = fueraDeOrden,
                excedeCambios = excedeCambios,
                eventoOrigenId = null,
            ),
        )

        eventosRelacionados.forEach { relacionado ->
            val relRef = eventosRef(partidoId).document()

            batch.set(
                relRef,
                relacionado.toMap(
                    partidoId = partidoId,
                    grupoId = relRef.id,
                    version = 1,
                    incompleto = incompleto,
                    fueraDeOrden = fueraDeOrden,
                    excedeCambios = false,
                    eventoOrigenId = ref.id,
                ),
            )
        }

        actualizarEstadoPartido(
            batch = batch,
            partidoId = partidoId,
            tipoId = borrador.tipo.id,
            minuto = borrador.minuto,
        )

        batch.commitLogged("registrarEvento")
        return ref.id
    }

    /** Los eventos de ámbito partido mueven el estado y el cronómetro. */
    private fun actualizarEstadoPartido(
        batch: WriteBatch,
        partidoId: String,
        tipoId: String,
        minuto: Int,
    ) {
        val cambios: Map<String, Any?> =
            when (tipoId) {
                TiposEvento.INICIO_PARTIDO ->
                    mapOf(
                        "estado" to EstadoPartido.EN_CURSO.name,
                        "inicioPrimerTiempo" to FieldValue.serverTimestamp(),
                        "enDescanso" to false,
                        "operadorUid" to uidActual,
                    )

                TiposEvento.FIN_PRIMER_TIEMPO ->
                    mapOf(
                        "enDescanso" to true,
                        "finPrimerTiempoMinuto" to minuto,
                        "finPrimerTiempo" to FieldValue.serverTimestamp(),
                    )

                TiposEvento.INICIO_SEGUNDO_TIEMPO ->
                    mapOf(
                        "enDescanso" to false,
                        "inicioSegundoTiempo" to FieldValue.serverTimestamp(),
                    )

                TiposEvento.FIN_PARTIDO ->
                    mapOf(
                        "estado" to EstadoPartido.FINALIZADO.name,
                        "finPartidoMinuto" to minuto,
                    )

                else -> return
            }

        batch.update(
            partidos.document(partidoId),
            cambios,
        )
    }

    override fun corregirEvento(
        partidoId: String,
        original: EventoPartido,
        borrador: BorradorEvento,
        motivo: String,
        camposModificados:
        List<String>,
        incompleto: Boolean,
    ) {
        val batch = firestore.batch()
        val nuevaRef = eventosRef(partidoId).document()

        val datos =
            borrador.toMap(
                partidoId = partidoId,
                grupoId = original.grupoId,
                version = original.version + 1,
                incompleto = incompleto,
                fueraDeOrden = original.fueraDeOrden,
                excedeCambios = original.excedeCambios,
                eventoOrigenId = original.eventoOrigenId,
            ).toMutableMap()

        datos["motivo"] = motivo.trim()
        datos["camposModificados"] = camposModificados
        datos["versionAnteriorId"] = original.id

        batch.set(
            nuevaRef,
            datos,
        )
        batch.update(
            eventosRef(
                partidoId
            ).document(
                original.id
            ),
            mapOf(
                "estado" to
                        EstadoEvento
                            .REEMPLAZADO.name,

                "reemplazadoPor" to
                        nuevaRef.id,

                "reemplazadoPorUid" to
                        uidActual,

                "reemplazadoEn" to
                        FieldValue
                            .serverTimestamp(),
            ),
        )

        batch.commitLogged(
            "corregirEvento"
        )
    }

    override fun anularEvento(
        partidoId: String,
        evento: EventoPartido,
        relacionados:
        List<EventoPartido>,
        motivo: String,
    ) {
        val batch =
            firestore.batch()

        (
                listOf(evento) +
                        relacionados
                ).forEach { item ->

                batch.update(
                    eventosRef(
                        partidoId
                    ).document(
                        item.id
                    ),
                    mapOf(
                        "estado" to
                                EstadoEvento
                                    .ANULADO.name,

                        "motivoAnulacion" to
                                motivo.trim(),

                        "anuladoPor" to
                                uidActual,

                        "anuladoPorNombre" to
                                nombreActual,

                        "anuladoEn" to
                                FieldValue
                                    .serverTimestamp(),
                    ),
                )
            }

        batch.commitLogged(
            "anularEvento"
        )
    }

    override fun cerrarActa(
        partidoId: String,
        eventosIncompletos: Int,
    ) {
        partidos.document(partidoId)
            .update(
                mapOf(
                    // Estadísticas consulta FINALIZADO; la UI interpreta actaCerrada=true
                    // como EstadoPartido.ACTA_CERRADA.
                    "estado" to EstadoPartido.FINALIZADO.name,
                    "actaCerrada" to true,
                    "actaCerradaPor" to uidActual,
                    "actaCerradaEn" to FieldValue.serverTimestamp(),
                    "actaCerradaConIncompletos" to eventosIncompletos,
                ),
            )
            .addOnFailureListener {
                reportarError("cerrarActa", it)
            }
    }

    override fun cerrarActa(
        partidoId: String,
        eventosIncompletos: Int,
        golesEquipo: Int,
        golesRival: Int,
    ) {
        partidos.document(partidoId)
            .update(
                mapOf(
                    "estado" to EstadoPartido.FINALIZADO.name,
                    "actaCerrada" to true,
                    "golesEquipo" to golesEquipo,
                    "golesRival" to golesRival,
                    "actaCerradaPor" to uidActual,
                    "actaCerradaEn" to FieldValue.serverTimestamp(),
                    "actaCerradaConIncompletos" to eventosIncompletos,
                ),
            )
            .addOnFailureListener {
                reportarError("cerrarActa", it)
            }
    }

    // ---------------------------------------------------------------- Catálogo inicial

    private fun sembrarCatalogo() {

        val batch =
            firestore.batch()

        CatalogoBase.tipos
            .forEach { tipo ->

                batch.set(
                    firestore
                        .collection(
                            COL_CATALOGO
                        )
                        .document(
                            tipo.id
                        ),
                    mapOf(
                        "nombre" to
                                tipo.nombre,

                        "ambito" to
                                tipo.ambito.name,

                        "camposObligatorios" to
                                tipo.camposObligatorios,

                        "camposOpcionales" to
                                tipo.camposOpcionales,

                        "sumaMarcador" to
                                tipo.sumaMarcador,

                        "orden" to
                                tipo.orden,

                        "activo" to
                                tipo.activo,

                        "version" to
                                CatalogoBase.VERSION,

                        "icono" to
                                tipo.icono,

                        "color" to
                                tipo.color,
                    ),
                )
            }

        batch.commitLogged(
            "sembrarCatalogo"
        )
    }

    // ---------------------------------------------------------------- Escrituras / errores

    /**
     * Sin conexión la tarea queda pendiente hasta sincronizar.
     *
     * Si el servidor rechaza la escritura
     * se informa a la pantalla.
     */
    private fun WriteBatch.commitLogged(
        operacion: String
    ) {
        commit()
            .addOnFailureListener {
                reportarError(
                    operacion,
                    it
                )
            }
    }

    private fun reportarError(
        operacion: String,
        e: Exception,
    ) {
        Log.w(
            TAG,
            "Falló $operacion",
            e
        )

        _erroresEscritura.tryEmit(
            describirErrorFirestore(e)
        )
    }

    // ---------------------------------------------------------------- Mapeos catálogo

    private fun TipoEvento.toMapCatalogo(
        version: Int,
        incluirCreacion: Boolean,
    ): Map<String, Any?> {

        val datos =
            mutableMapOf<String, Any?>(
                "nombre" to
                        nombre.trim(),

                "ambito" to
                        ambito.name,

                "camposObligatorios" to
                        camposObligatorios,

                "camposOpcionales" to
                        camposOpcionales,

                "sumaMarcador" to
                        sumaMarcador,

                "orden" to
                        orden,

                "activo" to
                        activo,

                "version" to
                        version,

                "icono" to
                        icono,

                "color" to
                        color,

                "actualizadoPor" to
                        uidActual,

                "actualizadoEn" to
                        FieldValue
                            .serverTimestamp(),
            )

        if (
            incluirCreacion
        ) {
            datos["creadoPor"] =
                uidActual

            datos["creadoEn"] =
                FieldValue
                    .serverTimestamp()
        }

        return datos
    }

    // ---------------------------------------------------------------- Mapeos eventos

    private fun BorradorEvento.toMap(
        partidoId: String,
        grupoId: String,
        version: Int,
        incompleto: Boolean,
        fueraDeOrden: Boolean,
        excedeCambios: Boolean,
        eventoOrigenId: String?,
    ): Map<String, Any?> =
        mapOf(
            "partidoId" to partidoId,
            "grupoId" to grupoId,
            "version" to version,
            "tipo" to tipo.id,
            "tipoNombre" to tipo.nombre,
            "minuto" to minuto,
            "equipo" to equipo?.name,
            "jugadorId" to jugadorId,
            "jugadorNombre" to jugadorNombre,
            "jugadorSecundarioId" to secundarioId,
            "jugadorSecundarioNombre" to secundarioNombre,
            "resultadoPenal" to resultadoPenal?.name,
            "observacion" to observacion?.takeIf { it.isNotBlank() },
            "operadorUid" to uidActual,
            "operadorNombre" to nombreActual,
            "registradoEn" to FieldValue.serverTimestamp(),
            "estado" to EstadoEvento.VIGENTE.name,
            "versionCatalogo" to tipo.version,
            "incompleto" to incompleto,
            "fueraDeOrden" to fueraDeOrden,
            "excedeCambios" to excedeCambios,
            "eventoOrigenId" to eventoOrigenId,
        ) + listOfNotNull(periodo?.let { "periodo" to it })

    // ---------------------------------------------------------------- Helpers de fechas

    private fun DocumentSnapshot.millis(
        campo: String
    ): Long? =
        getTimestamp(
            campo,
            DocumentSnapshot
                .ServerTimestampBehavior
                .ESTIMATE
        )
            ?.toDate()
            ?.time

    // ---------------------------------------------------------------- Mapper Partido

    private fun DocumentSnapshot.toPartido():
            Partido? {

        if (!exists()) {
            return null
        }

        return Partido(
            id = id,
            nombreEquipo = getString("nombreEquipo") ?: "Mi equipo",
            nombreRival = getString("nombreRival") ?: "Rival",
            categoria = getString("categoria"),

            estado =
                if (getBoolean("actaCerrada") == true) {
                    EstadoPartido.ACTA_CERRADA
                } else {
                    enumOr(
                        getString("estado"),
                        EstadoPartido.PROGRAMADO,
                    )
                },

            alineacionConfirmada =
                getBoolean("alineacionConfirmada") ?: false,

            cambiosPermitidos =
                getLong("cambiosPermitidos")?.toInt() ?: 5,

            duracionTiempoMin =
                getLong("duracionTiempoMin")?.toInt() ?: 45,

            inicioPrimerTiempoMs =
                millis("inicioPrimerTiempo"),

            inicioSegundoTiempoMs =
                millis("inicioSegundoTiempo"),

            enDescanso =
                getBoolean("enDescanso") ?: false,

            operadorUid =
                getString("operadorUid"),

            entrenadorUid =
                getString("entrenadorUid"),

            creadoPor =
                getString("creadoPor"),

            fechaMs =
                millis("fecha"),

            actaCerradaConIncompletos =
                getLong("actaCerradaConIncompletos")?.toInt(),

            tienePendientes =
                metadata.hasPendingWrites(),

            // US-014
            hora =
                getString("hora"),

            lugar =
                getString("lugar"),

            tipoPartido =
                getString("tipoPartido"),

            maxConvocados =
                getLong("maxConvocados")?.toInt() ?: 23,

            fechaLimiteRespuestaMs =
                millis("fechaLimiteRespuesta"),

            // Campos incorporados desde main
            equipoId =
                getString("equipoId"),

            academiaId =
                getString("academiaId"),

            tipo =
                getString("tipo") ?: TiposPartido.AMISTOSO,

            temporada =
                getString("temporada"),

            golesEquipo =
                getLong("golesEquipo")?.toInt() ?: 0,

            golesRival =
                getLong("golesRival")?.toInt() ?: 0,

            finPrimerTiempoMs =
                millis("finPrimerTiempo"),

            ultimoRecordatorioMs =
                millis("ultimoRecordatorioConvocatoria"),
        )
    }

    // ---------------------------------------------------------------- Mapper EventoPartido

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toEvento():
            EventoPartido? {

        val tipo =
            getString("tipo")
                ?: return null

        val estado =
            enumOr(
                getString("estado"),
                EstadoEvento.VIGENTE
            )

        return EventoPartido(
            id =
                id,

            grupoId =
                getString(
                    "grupoId"
                ) ?: id,

            version =
                getLong(
                    "version"
                )?.toInt()
                    ?: 1,

            tipoId =
                tipo,

            tipoNombre =
                getString(
                    "tipoNombre"
                )
                    ?: CatalogoBase
                        .porId(tipo)
                        ?.nombre
                    ?: tipo,

            minuto =
                getLong(
                    "minuto"
                )?.toInt()
                    ?: 0,

            equipo =
                getString(
                    "equipo"
                )?.let {
                    enumOrNull<EquipoEvento>(
                        it
                    )
                },

            jugadorId =
                getString(
                    "jugadorId"
                ),

            jugadorNombre =
                getString(
                    "jugadorNombre"
                ),

            jugadorSecundarioId =
                getString(
                    "jugadorSecundarioId"
                ),

            jugadorSecundarioNombre =
                getString(
                    "jugadorSecundarioNombre"
                ),

            resultadoPenal =
                getString(
                    "resultadoPenal"
                )?.let {
                    enumOrNull<ResultadoPenal>(
                        it
                    )
                },

            observacion =
                getString(
                    "observacion"
                ),

            operadorUid =
                getString(
                    "operadorUid"
                ),

            operadorNombre =
                getString(
                    "operadorNombre"
                ),

            registradoEnMs =
                millis(
                    "registradoEn"
                ),

            estado =
                estado,

            versionCatalogo =
                getLong(
                    "versionCatalogo"
                )?.toInt()
                    ?: 1,

            incompleto =
                getBoolean(
                    "incompleto"
                ) ?: false,

            fueraDeOrden =
                getBoolean(
                    "fueraDeOrden"
                ) ?: false,

            excedeCambios =
                getBoolean(
                    "excedeCambios"
                ) ?: false,

            eventoOrigenId =
                getString(
                    "eventoOrigenId"
                ),

            motivo =
                if (
                    estado ==
                    EstadoEvento.ANULADO
                ) {
                    getString(
                        "motivoAnulacion"
                    )
                } else {
                    getString(
                        "motivo"
                    )
                },

            camposModificados =
                (
                        get(
                            "camposModificados"
                        ) as? List<String>
                        ).orEmpty(),

            reemplazadoPor =
                getString(
                    "reemplazadoPor"
                ),

            anuladoPor =
                getString(
                    "anuladoPor"
                ),

            anuladoPorNombre =
                getString(
                    "anuladoPorNombre"
                ),

            anuladoEnMs =
                millis(
                    "anuladoEn"
                ),

            pendienteSincronizar =
                metadata
                    .hasPendingWrites(),

            periodo =
                getLong(
                    "periodo"
                )?.toInt(),

            reemplazadoEnMs =
                millis(
                    "reemplazadoEn"
                ),
        )
    }

    // ---------------------------------------------------------------- Mapper TipoEvento

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toTipoEvento():
            TipoEvento? {

        val nombre =
            getString(
                "nombre"
            )
                ?: return null

        return TipoEvento(
            id =
                id,

            nombre =
                nombre,

            ambito =
                enumOr(
                    getString(
                        "ambito"
                    ),
                    AmbitoEvento
                        .JUGADOR
                ),

            camposObligatorios =
                (
                        get(
                            "camposObligatorios"
                        ) as? List<String>
                        ).orEmpty(),

            sumaMarcador =
                getBoolean(
                    "sumaMarcador"
                ) ?: false,

            orden =
                getLong(
                    "orden"
                )?.toInt()
                    ?: 99,

            activo =
                getBoolean(
                    "activo"
                ) ?: true,

            version =
                getLong(
                    "version"
                )?.toInt()
                    ?: 1,

            icono =
                getString(
                    "icono"
                )
                    ?: "sports_soccer",

            color =
                getString(
                    "color"
                )
                    ?: "#2E7D32",

            camposOpcionales =
                (
                        get(
                            "camposOpcionales"
                        ) as? List<*>
                        )
                    ?.filterIsInstance<String>()
                    .orEmpty(),
        )
    }

    // ---------------------------------------------------------------- Enum helpers

    private inline fun <
            reified T : Enum<T>
            > enumOrNull(
        valor: String
    ): T? =
        enumValues<T>()
            .firstOrNull {
                it.name == valor
            }

    private inline fun <
            reified T : Enum<T>
            > enumOr(
        valor: String?,
        defecto: T,
    ): T =
        valor?.let {
            enumOrNull<T>(it)
        } ?: defecto

    // ---------------------------------------------------------------- Constantes

    companion object {

        private const val TAG =
            "PartidoRepository"

        const val COL_PARTIDOS =
            "partidos"

        const val COL_EVENTOS =
            "eventos"

        const val COL_CONVOCADOS =
            "convocados"

        const val COL_RESPUESTAS_CONVOCATORIA =
            "respuestasConvocatoria"

        const val COL_HISTORIAL =
            "historial"

        const val COL_CATALOGO =
            "catalogoEventos"

        const val COL_USUARIOS =
            "usuarios"
    }
}
