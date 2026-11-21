// Edge Function "notificar" (US-027): envía la notificación push de un anuncio.
//
// La app sigue usando Firebase Auth, así que se valida el ID token de Firebase (verify_jwt = false,
// igual que "imagenes"). El anuncio y el rol se leen de Firestore con el token del propio usuario,
// así que las reglas de seguridad también aplican. El envío usa FCM HTTP v1 con una cuenta de
// servicio que vive solo como secreto de Supabase:
//
//   supabase secrets set FIREBASE_SERVICE_ACCOUNT="$(cat cuenta-servicio.json)"
//
// Cada dispositivo está suscrito a temas (app: data/notificaciones/TemasPush.kt):
//   academia_{id}, academia_{id}_rol_{ROL} y equipo_{id}.
//
//   POST /notificar   cuerpo: { "anuncioId": "..." }   ->  { "enviada": true }
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

Deno.serve(async (req) => {
  if (req.method !== "POST") return responder(405, { error: "Método no permitido" });
  const sesion = await autenticar(req);
  if (!sesion) return responder(401, { error: "No autenticado" });

  let anuncioId = "";
  try {
    anuncioId = String((await req.json())?.anuncioId ?? "");
  } catch {
    return responder(400, { error: "Cuerpo no válido" });
  }
  if (!/^[A-Za-z0-9]{1,40}$/.test(anuncioId)) return responder(400, { error: "anuncioId no válido" });

  try {
    const usuario = await leerDocumento(`usuarios/${sesion.uid}`, sesion.token);
    // Personal activo: un entrenador pendiente de aprobación o una cuenta desactivada no envía.
    const estado = texto(usuario ?? {}, "estado") || "ACTIVO";
    if (!usuario || !ROLES_QUE_PUBLICAN.includes(texto(usuario, "rol")) || estado !== "ACTIVO") {
      return responder(403, { error: "Solo el personal activo de la academia envía anuncios" });
    }
    const anuncio = await leerDocumento(`anuncios/${anuncioId}`, sesion.token);
    if (!anuncio) return responder(404, { error: "Anuncio no encontrado" });
    // Solo su autor dispara el envío: evita reenviar anuncios ajenos.
    if (texto(anuncio, "autorUid") !== sesion.uid) return responder(403, { error: "No eres el autor del anuncio" });

    const tema = temaDe(texto(anuncio, "destinatarios"), texto(anuncio, "academiaId"), texto(anuncio, "equipoId"));
    if (!tema) return responder(400, { error: "Destinatarios no válidos" });

    const urgente = texto(anuncio, "prioridad") === "URGENTE";
    const titulo = texto(anuncio, "titulo").slice(0, 80);
    const mensaje = texto(anuncio, "mensaje").slice(0, 500);
    const envio = await fetch(`https://fcm.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/messages:send`, {
      method: "POST",
      headers: { Authorization: `Bearer ${await tokenFcm()}`, "Content-Type": "application/json" },
      body: JSON.stringify({
        message: {
          topic: tema,
          notification: { title: titulo, body: mensaje },
          data: { tipo: "anuncio", anuncioId, titulo, mensaje, prioridad: urgente ? "URGENTE" : "INFORMATIVA" },
          android: {
            priority: urgente ? "HIGH" : "NORMAL",
            notification: { channel_id: urgente ? "avisos_urgentes" : "avisos" },
          },
        },
      }),
      signal: AbortSignal.timeout(8_000),
    });
    if (!envio.ok) {
      console.error("FCM respondió", envio.status, await envio.text());
      return responder(502, { error: "No se pudo enviar la notificación" });
    }
    return responder(200, { enviada: true, tema });
  } catch (e) {
    console.error("Error al notificar", e instanceof Error ? e.message : e);
    return responder(500, { error: "No se pudo enviar la notificación" });
  }
});
