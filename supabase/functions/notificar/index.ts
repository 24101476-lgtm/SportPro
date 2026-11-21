// Edge Function "notificar" (US-027): envía notificaciones push de la academia:
//   - un anuncio (US-027), a su tema según los destinatarios;
//   - una sesión de entrenamiento cancelada (US-010), al tema de su equipo;
//   - una vinculación aprobada, rechazada o revocada (US-007), al tema propio del padre.
//
// La app sigue usando Firebase Auth, así que se valida el ID token de Firebase (verify_jwt = false,
// igual que "imagenes"). El anuncio y el rol se leen de Firestore con el token del propio usuario,
// así que las reglas de seguridad también aplican. El envío usa FCM HTTP v1 con una cuenta de
// servicio que vive solo como secreto de Supabase:
//
//   supabase secrets set FIREBASE_SERVICE_ACCOUNT="$(cat cuenta-servicio.json)"
//
// Cada dispositivo está suscrito a temas (app: data/notificaciones/TemasPush.kt):
//   academia_{id}, academia_{id}_rol_{ROL}, equipo_{id} y usuario_{uid}.
//
//   POST /notificar   cuerpo: { "anuncioId": "..." }                                   ->  { "enviada": true }
//   POST /notificar   cuerpo: { "tipo": "sesion_cancelada", "entrenamientoId": "..." }  ->  { "enviada": true }
//   POST /notificar   cuerpo: { "tipo": "vinculacion", "vinculacionId": "..." }         ->  { "enviada": true }
//
// En todos los casos solo quien hizo la acción (autor del anuncio, quien canceló la sesión o quien
// resolvió la vinculación) puede pedir el envío, y el contenido se arma con los datos de Firestore,
// nunca con texto enviado por la app.
import { createRemoteJWKSet, importPKCS8, jwtVerify, SignJWT } from "npm:jose@5";

const FIREBASE_PROJECT_ID = "sportpro-198a4";
const ROLES_QUE_PUBLICAN = ["ADMINISTRADOR", "ENTRENADOR"];

const llavesFirebase = createRemoteJWKSet(
  new URL("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"),
);

const responder = (estado: number, cuerpo: Record<string, unknown>) =>
  Response.json(cuerpo, { status: estado });

/** Devuelve uid y token si el token es de este proyecto y el correo está verificado. */
async function autenticar(req: Request): Promise<{ uid: string; token: string } | null> {
  const token = req.headers.get("Authorization")?.match(/^Bearer (.+)$/)?.[1];
  if (!token) return null;
  try {
    const { payload } = await jwtVerify(token, llavesFirebase, {
      issuer: `https://securetoken.google.com/${FIREBASE_PROJECT_ID}`,
      audience: FIREBASE_PROJECT_ID,
      algorithms: ["RS256"],
    });
    return payload.sub && payload.email_verified === true ? { uid: payload.sub, token } : null;
  } catch {
    return null;
  }
}

/** Lee un documento de Firestore con el token del usuario: las reglas también aplican. */
// deno-lint-ignore no-explicit-any
async function leerDocumento(ruta: string, token: string): Promise<Record<string, any> | null> {
  const respuesta = await fetch(
    `https://firestore.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/databases/(default)/documents/${ruta}`,
    { headers: { Authorization: `Bearer ${token}` }, signal: AbortSignal.timeout(8_000) },
  );
  if (respuesta.status === 404 || respuesta.status === 403) return null;
  if (!respuesta.ok) throw new Error(`Firestore respondió ${respuesta.status}`);
  return (await respuesta.json()).fields ?? {};
}

// deno-lint-ignore no-explicit-any
const texto = (campos: Record<string, any>, nombre: string): string => campos?.[nombre]?.stringValue ?? "";

/** Mismo formato que la app: FCM solo acepta letras, números y -_.~% en el tema. */
const limpio = (valor: string) => valor.replace(/[^A-Za-z0-9_.~%-]/g, "-");

function temaDe(destinatarios: string, academiaId: string, equipoId: string): string | null {
  const academia = `academia_${limpio(academiaId)}`;
  switch (destinatarios) {
    case "TODO_EL_CLUB":
      return academia;
    case "EQUIPO_ESPECIFICO":
      return equipoId ? `equipo_${limpio(equipoId)}` : null;
    case "SOLO_ENTRENADORES":
      return `${academia}_rol_ENTRENADOR`;
    case "SOLO_JUGADORES":
      return `${academia}_rol_JUGADOR`;
    case "SOLO_PADRES":
      return `${academia}_rol_PADRE_DE_FAMILIA`;
    default:
      return null;
  }
}

let tokenGoogle: { valor: string; expira: number } | null = null;

/** Token OAuth de la cuenta de servicio para FCM (se reutiliza hasta un minuto antes de vencer). */
async function tokenFcm(): Promise<string> {
  if (tokenGoogle && tokenGoogle.expira > Date.now() + 60_000) return tokenGoogle.valor;
  const cuenta = JSON.parse(Deno.env.get("FIREBASE_SERVICE_ACCOUNT") ?? "{}");
  if (!cuenta.client_email || !cuenta.private_key) throw new Error("Falta el secreto FIREBASE_SERVICE_ACCOUNT");
  const llave = await importPKCS8(cuenta.private_key, "RS256");
  const ahora = Math.floor(Date.now() / 1000);
  const afirmacion = await new SignJWT({ scope: "https://www.googleapis.com/auth/firebase.messaging" })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" })
    .setIssuer(cuenta.client_email)
    .setAudience("https://oauth2.googleapis.com/token")
    .setIssuedAt(ahora)
    .setExpirationTime(ahora + 3600)
    .sign(llave);
  const respuesta = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion: afirmacion }),
    signal: AbortSignal.timeout(8_000),
  });
  if (!respuesta.ok) throw new Error(`Google OAuth respondió ${respuesta.status}`);
  const datos = await respuesta.json();
  tokenGoogle = { valor: datos.access_token, expira: Date.now() + datos.expires_in * 1000 };
  return tokenGoogle.valor;
}

/** Error con estado HTTP para responder sin repetir try/catch en cada tipo de aviso. */
class ErrorAviso extends Error {
  constructor(readonly estado: number, mensaje: string) {
    super(mensaje);
  }
}

interface Aviso {
  tema: string;
  titulo: string;
  mensaje: string;
  urgente: boolean;
  datos: Record<string, string>;
}

/** "2026-11-03" -> "03/11/2026"; cualquier otro formato se devuelve tal cual. */
const fechaLegible = (iso: string) => {
  const partes = iso.match(/^(\d{4})-(\d{2})-(\d{2})$/);
  return partes ? `${partes[3]}/${partes[2]}/${partes[1]}` : iso;
};

const idValido = (id: string) => /^[A-Za-z0-9_-]{1,80}$/.test(id);

async function avisoDeAnuncio(anuncioId: string, uid: string, token: string): Promise<Aviso> {
  if (!/^[A-Za-z0-9]{1,40}$/.test(anuncioId)) throw new ErrorAviso(400, "anuncioId no válido");
  const anuncio = await leerDocumento(`anuncios/${anuncioId}`, token);
  if (!anuncio) throw new ErrorAviso(404, "Anuncio no encontrado");
  // Solo su autor dispara el envío: evita reenviar anuncios ajenos.
  if (texto(anuncio, "autorUid") !== uid) throw new ErrorAviso(403, "No eres el autor del anuncio");

  const tema = temaDe(texto(anuncio, "destinatarios"), texto(anuncio, "academiaId"), texto(anuncio, "equipoId"));
  if (!tema) throw new ErrorAviso(400, "Destinatarios no válidos");

  const urgente = texto(anuncio, "prioridad") === "URGENTE";
  const titulo = texto(anuncio, "titulo").slice(0, 80);
  const mensaje = texto(anuncio, "mensaje").slice(0, 500);
  return {
    tema,
    titulo,
    mensaje,
    urgente,
    datos: { tipo: "anuncio", anuncioId, titulo, mensaje, prioridad: urgente ? "URGENTE" : "INFORMATIVA" },
  };
}

/** US-010: aviso a jugadores y padres del equipo cuando se cancela una sesión. */
async function avisoDeSesionCancelada(entrenamientoId: string, uid: string, token: string): Promise<Aviso> {
  if (!idValido(entrenamientoId)) throw new ErrorAviso(400, "entrenamientoId no válido");
  const sesion = await leerDocumento(`entrenamientos/${entrenamientoId}`, token);
  if (!sesion) throw new ErrorAviso(404, "Sesión no encontrada");
  if (texto(sesion, "estado") !== "CANCELADA") throw new ErrorAviso(409, "La sesión no está cancelada");
  if (texto(sesion, "canceladaPor") !== uid) throw new ErrorAviso(403, "No cancelaste esta sesión");
  const equipoId = texto(sesion, "equipoId");
  if (!equipoId) throw new ErrorAviso(400, "La sesión no tiene equipo");

  const equipo = texto(sesion, "equipoNombre") || "Tu equipo";
  const cuando = [fechaLegible(texto(sesion, "fecha")), texto(sesion, "hora")].filter(Boolean).join(" a las ");
  const motivo = texto(sesion, "motivoCancelacion");
  const titulo = "Entrenamiento cancelado";
  const mensaje = `${equipo}: se canceló la sesión del ${cuando}.${motivo ? ` Motivo: ${motivo}` : ""}`.slice(0, 500);
  return {
    tema: `equipo_${limpio(equipoId)}`,
    titulo,
    mensaje,
    urgente: true,
    datos: { tipo: "sesion_cancelada", entrenamientoId, titulo, mensaje, prioridad: "URGENTE" },
  };
}

/**
 * US-007: aviso al padre cuando su solicitud se aprueba, se rechaza o se revoca. Va a su tema
 * personal; como cualquier dispositivo podría suscribirse a un tema, el texto no nombra al jugador
 * ni el motivo: el detalle se ve dentro de la app, en "Mis hijos".
 */
async function avisoDeVinculacion(vinculacionId: string, uid: string, token: string): Promise<Aviso> {
  if (!idValido(vinculacionId)) throw new ErrorAviso(400, "vinculacionId no válido");
  const vinculacion = await leerDocumento(`vinculaciones/${vinculacionId}`, token);
  if (!vinculacion) throw new ErrorAviso(404, "Vinculación no encontrada");
  if (texto(vinculacion, "resueltaPor") !== uid) throw new ErrorAviso(403, "No resolviste esta vinculación");
  const padreUid = texto(vinculacion, "padreUid");
  if (!padreUid) throw new ErrorAviso(400, "La vinculación no tiene apoderado");

  const textos: Record<string, [string, string]> = {
    APROBADA: ["Vinculación aprobada", "Ya puedes ver el perfil, la asistencia y las mensualidades de tu hijo en SportPro."],
    RECHAZADA: ["Solicitud de vinculación rechazada", "Abre SportPro, en Mis hijos, para ver el motivo."],
    REVOCADA: ["Vinculación revocada", "La academia revocó una de tus vinculaciones. Abre SportPro para ver el detalle."],
  };
  const estado = texto(vinculacion, "estado");
  const elegido = textos[estado];
  if (!elegido) throw new ErrorAviso(409, "La vinculación todavía no está resuelta");
  const [titulo, mensaje] = elegido;
  return {
    tema: `usuario_${limpio(padreUid)}`,
    titulo,
    mensaje,
    urgente: false,
    datos: { tipo: "vinculacion", vinculacionId, estado, titulo, mensaje, prioridad: "INFORMATIVA" },
  };
}

async function enviar(aviso: Aviso): Promise<void> {
  const envio = await fetch(`https://fcm.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/messages:send`, {
    method: "POST",
    headers: { Authorization: `Bearer ${await tokenFcm()}`, "Content-Type": "application/json" },
    body: JSON.stringify({
      message: {
        topic: aviso.tema,
        notification: { title: aviso.titulo, body: aviso.mensaje },
        data: aviso.datos,
        android: {
          priority: aviso.urgente ? "HIGH" : "NORMAL",
          notification: { channel_id: aviso.urgente ? "avisos_urgentes" : "avisos" },
        },
      },
    }),
    signal: AbortSignal.timeout(8_000),
  });
  if (!envio.ok) {
    console.error("FCM respondió", envio.status, await envio.text());
    throw new ErrorAviso(502, "No se pudo enviar la notificación");
  }
}

Deno.serve(async (req) => {
  if (req.method !== "POST") return responder(405, { error: "Método no permitido" });
  const sesion = await autenticar(req);
  if (!sesion) return responder(401, { error: "No autenticado" });

  // deno-lint-ignore no-explicit-any
  let cuerpo: Record<string, any>;
  try {
    cuerpo = (await req.json()) ?? {};
  } catch {
    return responder(400, { error: "Cuerpo no válido" });
  }

  try {
    const usuario = await leerDocumento(`usuarios/${sesion.uid}`, sesion.token);
    // Personal activo: un entrenador pendiente de aprobación o una cuenta desactivada no envía.
    const estado = texto(usuario ?? {}, "estado") || "ACTIVO";
    if (!usuario || !ROLES_QUE_PUBLICAN.includes(texto(usuario, "rol")) || estado !== "ACTIVO") {
      return responder(403, { error: "Solo el personal activo de la academia envía notificaciones" });
    }

    const tipo = String(cuerpo.tipo ?? (cuerpo.anuncioId ? "anuncio" : ""));
    let aviso: Aviso;
    switch (tipo) {
      case "anuncio":
        aviso = await avisoDeAnuncio(String(cuerpo.anuncioId ?? ""), sesion.uid, sesion.token);
        break;
      case "sesion_cancelada":
        aviso = await avisoDeSesionCancelada(String(cuerpo.entrenamientoId ?? ""), sesion.uid, sesion.token);
        break;
      case "vinculacion":
        aviso = await avisoDeVinculacion(String(cuerpo.vinculacionId ?? ""), sesion.uid, sesion.token);
        break;
      default:
        return responder(400, { error: "Tipo de notificación no válido" });
    }
    await enviar(aviso);
    return responder(200, { enviada: true, tema: aviso.tema });
  } catch (e) {
    if (e instanceof ErrorAviso) return responder(e.estado, { error: e.message });
    console.error("Error al notificar", e instanceof Error ? e.message : e);
    return responder(500, { error: "No se pudo enviar la notificación" });
  }
});
