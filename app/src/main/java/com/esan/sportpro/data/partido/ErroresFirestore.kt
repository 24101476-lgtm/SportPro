package com.esan.sportpro.data.partido

import com.google.firebase.firestore.FirebaseFirestoreException

/** Traduce los errores de Firestore a mensajes que el usuario pueda entender y resolver. */
fun describirErrorFirestore(e: Throwable): String {
    val fe = e as? FirebaseFirestoreException
    val detalle = e.message.orEmpty()
    return when {
        fe?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "Firestore rechazó la operación por permisos. Publica las reglas de firestore.rules " +
                "en Firebase Console > Firestore Database > Reglas."
        fe?.code == FirebaseFirestoreException.Code.NOT_FOUND ||
            detalle.contains("does not exist", ignoreCase = true) ->
            "La base de datos de Firestore no existe. Créala en Firebase Console > Firestore Database."
        fe?.code == FirebaseFirestoreException.Code.UNAUTHENTICATED ->
            "Tu sesión expiró. Cierra sesión y vuelve a ingresar."
        fe?.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION ->
            "Firestore no está listo para esta consulta: $detalle"
        else -> "Error de Firestore: ${detalle.ifBlank { e.javaClass.simpleName }}"
    }
}
