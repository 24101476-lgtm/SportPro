package com.esan.sportpro.data.partido

import android.util.Log
import com.esan.sportpro.domain.partido.AmbitoEvento
import com.esan.sportpro.domain.partido.BorradorEvento
import com.esan.sportpro.domain.partido.CatalogoBase
import com.esan.sportpro.domain.partido.EquipoEvento
import com.esan.sportpro.domain.partido.EstadoEvento
import com.esan.sportpro.domain.partido.EstadoPartido
import com.esan.sportpro.domain.partido.EventoPartido
import com.esan.sportpro.domain.partido.JugadorConvocado
import com.esan.sportpro.domain.partido.Partido
import com.esan.sportpro.domain.partido.PartidoRepository
import com.esan.sportpro.domain.partido.ResultadoPenal
import com.esan.sportpro.domain.partido.TipoEvento
import com.esan.sportpro.domain.partido.TiposEvento
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
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación con Cloud Firestore.
 *
 * Estructura:
 * - `partidos/{partidoId}`: datos generales, estado y marcas de tiempo del cronómetro.
 * - `partidos/{partidoId}/convocados/{jugadorId}`: plantel convocado (titular / suplente).
 * - `partidos/{partidoId}/eventos/{eventoId}`: cada versión de cada evento. Nunca se borra.
 * - `catalogoEventos/{tipoId}`: catálogo configurable de tipos de evento.
 *
 * Los listeners usan [MetadataChanges.INCLUDE] para enterarse de cuándo una escritura local
 * queda confirmada por el servidor (ícono de reloj → ícono de verificación).
 * Las escrituras NO usan await(): sin conexión la tarea no se completa hasta recuperar la red,
 * pero el dato ya está guardado localmente y visible en los listeners.
 */
@Singleton
class FirestorePartidoRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : PartidoRepository {

    private val partidos get() = firestore.collection(COL_PARTIDOS)
    private fun eventosRef(partidoId: String) = partidos.document(partidoId).collection(COL_EVENTOS)
    private fun convocadosRef(partidoId: String) = partidos.document(partidoId).collection(COL_CONVOCADOS)

    override val uidActual: String? get() = auth.currentUser?.uid

    private val _erroresEscritura = MutableSharedFlow<String>(extraBufferCapacity = 8)
    override val erroresEscritura: SharedFlow<String> = _erroresEscritura.asSharedFlow()
    private val nombreActual: String? get() = auth.currentUser?.let { it.displayName ?: it.email }

    // ---------------------------------------------------------------- Lecturas en tiempo real

    override fun observarPartidos(): Flow<List<Partido>> = callbackFlow {
        // Sin orderBy: se ordena en el dispositivo para incluir partidos recién creados sin conexión.
        val reg = partidos
            .addSnapshotListener(MetadataChanges.INCLUDE) { snap, error ->
                if (error != null) {
                    Log.w(TAG, "observarPartidos", error)
                    close(error)
                    return@addSnapshotListener
                }
                trySend(
                    snap?.documents.orEmpty()
                        .mapNotNull { it.toPartido() }
                        .sortedByDescending { it.fechaMs ?: Long.MAX_VALUE },
                )
            }
        awaitClose { reg.remove() }
    }

    override fun observarPartido(partidoId: String): Flow<Partido?> = callbackFlow {
        val reg = partidos.document(partidoId)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snap, error ->
                if (error != null) {
                    Log.w(TAG, "observarPartido", error)
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snap?.toPartido())
            }
        awaitClose { reg.remove() }
    }

    override fun observarEventos(partidoId: String): Flow<List<EventoPartido>> = callbackFlow {
        val reg = eventosRef(partidoId)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snap, error ->
                if (error != null) {
                    Log.w(TAG, "observarEventos", error)
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snap?.documents.orEmpty().mapNotNull { it.toEvento() })
            }
        awaitClose { reg.remove() }
    }

    override fun observarConvocados(partidoId: String): Flow<List<JugadorConvocado>> = callbackFlow {
        val reg = convocadosRef(partidoId).addSnapshotListener { snap, error ->
            if (error != null) {
                Log.w(TAG, "observarConvocados", error)
                close(error)
                return@addSnapshotListener
            }
            val lista = snap?.documents.orEmpty().map { d ->
                JugadorConvocado(
                    id = d.id,
                    nombre = d.getString("nombre") ?: "Sin nombre",
                    dorsal = d.getLong("dorsal")?.toInt(),
                    titular = d.getBoolean("titular") ?: false,
                    posicion = d.getString("posicion"),
                )
            }.sortedWith(compareBy({ !it.titular }, { it.dorsal ?: Int.MAX_VALUE }))
            trySend(lista)
        }
        awaitClose { reg.remove() }
    }

    override fun observarCatalogo(): Flow<List<TipoEvento>> = callbackFlow {
        val reg = firestore.collection(COL_CATALOGO).addSnapshotListener { snap, error ->
            if (error != null) {
                Log.w(TAG, "observarCatalogo", error)
                trySend(CatalogoBase.tipos)
                return@addSnapshotListener
            }
            if (snap == null || snap.isEmpty) {
                // Primera vez: se publica el catálogo base para que el administrador pueda editarlo.
                if (snap != null && !snap.metadata.isFromCache) sembrarCatalogo()
                trySend(CatalogoBase.tipos)
                return@addSnapshotListener
            }
            val tipos = snap.documents.mapNotNull { it.toTipoEvento() }
                .filter { it.activo }
                .sortedBy { it.orden }
            trySend(tipos)
        }
        awaitClose { reg.remove() }
    }

    override fun observarRolActual(): Flow<String?> = callbackFlow {
        val uid = uidActual
        if (uid == null) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val reg = firestore.collection(COL_USUARIOS).document(uid).addSnapshotListener { snap, _ ->
            trySend(snap?.getString("rol"))
        }
        awaitClose { reg.remove() }
    }

    // ---------------------------------------------------------------- Escrituras

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
                "estado" to EstadoPartido.PROGRAMADO.name,
                "alineacionConfirmada" to true,
                "cambiosPermitidos" to cambiosPermitidos,
                "duracionTiempoMin" to duracionTiempoMin,
                "enDescanso" to false,
                "operadorUid" to uid,
                "entrenadorUid" to uid,
                "creadoPor" to uid,
                "fecha" to FieldValue.serverTimestamp(),
            ),
        )
        val posiciones = listOf("POR", "DEF", "DEF", "DEF", "DEF", "MED", "MED", "MED", "DEL", "DEL", "DEL")
        (1..16).forEach { dorsal ->
            batch.set(
                ref.collection(COL_CONVOCADOS).document("j$dorsal"),
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

    override fun registrarEvento(
        partidoId: String,
        borrador: BorradorEvento,
        incompleto: Boolean,
        fueraDeOrden: Boolean,
        excedeCambios: Boolean,
        eventosRelacionados: List<BorradorEvento>,
    ): String {
        val batch = firestore.batch()
        val ref = eventosRef(partidoId).document()
        batch.set(
            ref,
            borrador.toMap(
                grupoId = ref.id,
                version = 1,
                incompleto = incompleto,
                fueraDeOrden = fueraDeOrden,
                excedeCambios = excedeCambios,
                eventoOrigenId = null,
            ),
        )
        eventosRelacionados.forEach { rel ->
            val relRef = eventosRef(partidoId).document()
            batch.set(
                relRef,
                rel.toMap(
                    grupoId = relRef.id,
                    version = 1,
                    incompleto = incompleto,
                    fueraDeOrden = fueraDeOrden,
                    excedeCambios = false,
                    eventoOrigenId = ref.id,
                ),
            )
        }
        actualizarEstadoPartido(batch, partidoId, borrador.tipo.id, borrador.minuto)
        batch.commitLogged("registrarEvento")
        return ref.id
    }

    /** Los eventos de ámbito partido mueven el estado y el cronómetro. */
    private fun actualizarEstadoPartido(batch: WriteBatch, partidoId: String, tipoId: String, minuto: Int) {
        val cambios: Map<String, Any?> = when (tipoId) {
            TiposEvento.INICIO_PARTIDO -> mapOf(
                "estado" to EstadoPartido.EN_CURSO.name,
                "inicioPrimerTiempo" to FieldValue.serverTimestamp(),
                "enDescanso" to false,
                "operadorUid" to uidActual,
            )
            TiposEvento.FIN_PRIMER_TIEMPO -> mapOf("enDescanso" to true, "finPrimerTiempoMinuto" to minuto)
            TiposEvento.INICIO_SEGUNDO_TIEMPO -> mapOf(
                "enDescanso" to false,
                "inicioSegundoTiempo" to FieldValue.serverTimestamp(),
            )
            TiposEvento.FIN_PARTIDO -> mapOf(
                "estado" to EstadoPartido.FINALIZADO.name,
                "finPartidoMinuto" to minuto,
            )
            else -> return
        }
        batch.update(partidos.document(partidoId), cambios)
    }

    override fun corregirEvento(
        partidoId: String,
        original: EventoPartido,
        borrador: BorradorEvento,
        motivo: String,
        camposModificados: List<String>,
        incompleto: Boolean,
    ) {
        val batch = firestore.batch()
        val nuevaRef = eventosRef(partidoId).document()
        val datos = borrador.toMap(
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
        batch.set(nuevaRef, datos)
        batch.update(
            eventosRef(partidoId).document(original.id),
            mapOf(
                "estado" to EstadoEvento.REEMPLAZADO.name,
                "reemplazadoPor" to nuevaRef.id,
                "reemplazadoPorUid" to uidActual,
                "reemplazadoEn" to FieldValue.serverTimestamp(),
            ),
        )
        batch.commitLogged("corregirEvento")
    }

    override fun anularEvento(
        partidoId: String,
        evento: EventoPartido,
        relacionados: List<EventoPartido>,
        motivo: String,
    ) {
        val batch = firestore.batch()
        (listOf(evento) + relacionados).forEach { e ->
            batch.update(
                eventosRef(partidoId).document(e.id),
                mapOf(
                    "estado" to EstadoEvento.ANULADO.name,
                    "motivoAnulacion" to motivo.trim(),
                    "anuladoPor" to uidActual,
                    "anuladoPorNombre" to nombreActual,
                    "anuladoEn" to FieldValue.serverTimestamp(),
                ),
            )
        }
        batch.commitLogged("anularEvento")
    }

    override fun cerrarActa(partidoId: String, eventosIncompletos: Int) {
        partidos.document(partidoId).update(
            mapOf(
                "estado" to EstadoPartido.ACTA_CERRADA.name,
                "actaCerradaPor" to uidActual,
                "actaCerradaEn" to FieldValue.serverTimestamp(),
                "actaCerradaConIncompletos" to eventosIncompletos,
            ),
        ).addOnFailureListener { reportarError("cerrarActa", it) }
    }

    private fun sembrarCatalogo() {
        val batch = firestore.batch()
        CatalogoBase.tipos.forEach { t ->
            batch.set(
                firestore.collection(COL_CATALOGO).document(t.id),
                mapOf(
                    "nombre" to t.nombre,
                    "ambito" to t.ambito.name,
                    "camposObligatorios" to t.camposObligatorios,
                    "sumaMarcador" to t.sumaMarcador,
                    "orden" to t.orden,
                    "activo" to t.activo,
                    "version" to CatalogoBase.VERSION,
                ),
            )
        }
        batch.commitLogged("sembrarCatalogo")
    }

    /**
     * Sin conexión la tarea queda pendiente hasta sincronizar; si el servidor rechaza la escritura
     * (por ejemplo, por las reglas de seguridad) se informa a la pantalla.
     */
    private fun WriteBatch.commitLogged(operacion: String) {
        commit().addOnFailureListener { reportarError(operacion, it) }
    }

    private fun reportarError(operacion: String, e: Exception) {
        Log.w(TAG, "Falló $operacion", e)
        _erroresEscritura.tryEmit(describirErrorFirestore(e))
    }

    // ---------------------------------------------------------------- Mapeos

    private fun BorradorEvento.toMap(
        grupoId: String,
        version: Int,
        incompleto: Boolean,
        fueraDeOrden: Boolean,
        excedeCambios: Boolean,
        eventoOrigenId: String?,
    ): Map<String, Any?> = mapOf(
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
    )

    private fun DocumentSnapshot.millis(campo: String): Long? =
        getTimestamp(campo, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time

    private fun DocumentSnapshot.toPartido(): Partido? {
        if (!exists()) return null
        return Partido(
            id = id,
            nombreEquipo = getString("nombreEquipo") ?: "Mi equipo",
            nombreRival = getString("nombreRival") ?: "Rival",
            categoria = getString("categoria"),
            estado = enumOr(getString("estado"), EstadoPartido.PROGRAMADO),
            alineacionConfirmada = getBoolean("alineacionConfirmada") ?: false,
            cambiosPermitidos = getLong("cambiosPermitidos")?.toInt() ?: 5,
            duracionTiempoMin = getLong("duracionTiempoMin")?.toInt() ?: 45,
            inicioPrimerTiempoMs = millis("inicioPrimerTiempo"),
            inicioSegundoTiempoMs = millis("inicioSegundoTiempo"),
            enDescanso = getBoolean("enDescanso") ?: false,
            operadorUid = getString("operadorUid"),
            entrenadorUid = getString("entrenadorUid"),
            creadoPor = getString("creadoPor"),
            fechaMs = millis("fecha"),
            actaCerradaConIncompletos = getLong("actaCerradaConIncompletos")?.toInt(),
            tienePendientes = metadata.hasPendingWrites(),
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toEvento(): EventoPartido? {
        val tipo = getString("tipo") ?: return null
        val estado = enumOr(getString("estado"), EstadoEvento.VIGENTE)
        return EventoPartido(
            id = id,
            grupoId = getString("grupoId") ?: id,
            version = getLong("version")?.toInt() ?: 1,
            tipoId = tipo,
            tipoNombre = getString("tipoNombre") ?: CatalogoBase.porId(tipo)?.nombre ?: tipo,
            minuto = getLong("minuto")?.toInt() ?: 0,
            equipo = getString("equipo")?.let { enumOrNull<EquipoEvento>(it) },
            jugadorId = getString("jugadorId"),
            jugadorNombre = getString("jugadorNombre"),
            jugadorSecundarioId = getString("jugadorSecundarioId"),
            jugadorSecundarioNombre = getString("jugadorSecundarioNombre"),
            resultadoPenal = getString("resultadoPenal")?.let { enumOrNull<ResultadoPenal>(it) },
            observacion = getString("observacion"),
            operadorUid = getString("operadorUid"),
            operadorNombre = getString("operadorNombre"),
            registradoEnMs = millis("registradoEn"),
            estado = estado,
            versionCatalogo = getLong("versionCatalogo")?.toInt() ?: 1,
            incompleto = getBoolean("incompleto") ?: false,
            fueraDeOrden = getBoolean("fueraDeOrden") ?: false,
            excedeCambios = getBoolean("excedeCambios") ?: false,
            eventoOrigenId = getString("eventoOrigenId"),
            motivo = if (estado == EstadoEvento.ANULADO) getString("motivoAnulacion") else getString("motivo"),
            camposModificados = (get("camposModificados") as? List<String>).orEmpty(),
            reemplazadoPor = getString("reemplazadoPor"),
            anuladoPor = getString("anuladoPor"),
            anuladoPorNombre = getString("anuladoPorNombre"),
            anuladoEnMs = millis("anuladoEn"),
            pendienteSincronizar = metadata.hasPendingWrites(),
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toTipoEvento(): TipoEvento? {
        val nombre = getString("nombre") ?: return null
        return TipoEvento(
            id = id,
            nombre = nombre,
            ambito = enumOr(getString("ambito"), AmbitoEvento.JUGADOR),
            camposObligatorios = (get("camposObligatorios") as? List<String>).orEmpty(),
            sumaMarcador = getBoolean("sumaMarcador") ?: false,
            orden = getLong("orden")?.toInt() ?: 99,
            activo = getBoolean("activo") ?: true,
            version = getLong("version")?.toInt() ?: 1,
        )
    }

    private inline fun <reified T : Enum<T>> enumOrNull(valor: String): T? =
        enumValues<T>().firstOrNull { it.name == valor }

    private inline fun <reified T : Enum<T>> enumOr(valor: String?, defecto: T): T =
        valor?.let { enumOrNull<T>(it) } ?: defecto

    companion object {
        private const val TAG = "PartidoRepository"
        const val COL_PARTIDOS = "partidos"
        const val COL_EVENTOS = "eventos"
        const val COL_CONVOCADOS = "convocados"
        const val COL_CATALOGO = "catalogoEventos"
        const val COL_USUARIOS = "usuarios"
    }
}
