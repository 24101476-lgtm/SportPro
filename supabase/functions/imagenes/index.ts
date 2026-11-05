// Edge Function "imagenes": sube y elimina las imágenes de SportPro en Supabase Storage.
//
// La app sigue usando Firebase Auth, así que aquí se valida el ID token de Firebase y la clave
// secreta de Supabase nunca sale del servidor. Por eso se despliega con verify_jwt = false.
//
//   POST   /imagenes?carpeta=jugadores   cuerpo: JPEG   ->  { "url": "https://..." }
//   DELETE /imagenes?url=https://...                    ->  { "eliminada": true }
import { createClient } from "npm:@supabase/supabase-js@2";
import { createRemoteJWKSet, jwtVerify } from "npm:jose@5";

const FIREBASE_PROJECT_ID = "sportpro-198a4";
const BUCKET = "imagenes";
const TAMANO_MAXIMO = 5 * 1024 * 1024;
const CARPETAS = ["jugadores", "ejercicios", "publicaciones", "escudos"];

// Las rutas son {carpeta}/{uid de quien subió}/{uuid}.jpg; el uid permite comprobar el dueño.
const RUTA_VALIDA = new RegExp(`^(${CARPETAS.join("|")})/([^/]+)/[0-9a-f-]{36}\\.jpg$`);
const MARCA_URL_PUBLICA = `/storage/v1/object/public/${BUCKET}/`;

const llavesFirebase = createRemoteJWKSet(
  new URL("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"),
);

function claveSecreta(): string {
  const claves = Deno.env.get("SUPABASE_SECRET_KEYS");
  return claves ? JSON.parse(claves)["default"] : Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
}

const supabase = createClient(Deno.env.get("SUPABASE_URL")!, claveSecreta());

const responder = (estado: number, cuerpo: Record<string, unknown>) =>
  Response.json(cuerpo, { status: estado });

/** Devuelve el uid si el token es de este proyecto de Firebase y el correo está verificado. */
async function uidAutenticado(req: Request): Promise<string | null> {
  const token = req.headers.get("Authorization")?.match(/^Bearer (.+)$/)?.[1];
  if (!token) return null;
  try {
    const { payload } = await jwtVerify(token, llavesFirebase, {
      issuer: `https://securetoken.google.com/${FIREBASE_PROJECT_ID}`,
      audience: FIREBASE_PROJECT_ID,
      algorithms: ["RS256"],
    });
    return payload.sub && payload.email_verified === true ? payload.sub : null;
  } catch {
    return null;
  }
}

const esJpeg = (bytes: Uint8Array) =>
  bytes.length > 3 && bytes[0] === 0xff && bytes[1] === 0xd8 && bytes[2] === 0xff;

async function subir(req: Request, uid: string): Promise<Response> {
  const carpeta = new URL(req.url).searchParams.get("carpeta") ?? "";
  if (!CARPETAS.includes(carpeta)) return responder(400, { error: "Carpeta no válida" });
  if (Number(req.headers.get("Content-Length") ?? 0) > TAMANO_MAXIMO) {
    return responder(413, { error: "La imagen supera 5 MB" });
  }

  const bytes = new Uint8Array(await req.arrayBuffer());
  if (bytes.length > TAMANO_MAXIMO) return responder(413, { error: "La imagen supera 5 MB" });
  if (!esJpeg(bytes)) return responder(415, { error: "Solo se aceptan imágenes JPEG" });

  const ruta = `${carpeta}/${uid}/${crypto.randomUUID()}.jpg`;
  const { error } = await supabase.storage.from(BUCKET).upload(ruta, bytes, {
    contentType: "image/jpeg",
    cacheControl: "31536000",
  });
  if (error) {
    console.error("Error al subir", error.message);
    return responder(500, { error: "No se pudo guardar la imagen" });
  }
  return responder(200, { url: supabase.storage.from(BUCKET).getPublicUrl(ruta).data.publicUrl });
}

async function eliminar(req: Request, uid: string): Promise<Response> {
  const url = new URL(req.url).searchParams.get("url") ?? "";
  const inicio = url.indexOf(MARCA_URL_PUBLICA);
  const ruta = inicio < 0 ? "" : url.slice(inicio + MARCA_URL_PUBLICA.length);
  const coincidencia = ruta.match(RUTA_VALIDA);
  if (!coincidencia) return responder(400, { error: "URL de imagen no válida" });
  if (coincidencia[2] !== uid) return responder(403, { error: "La imagen es de otro usuario" });

  const { error } = await supabase.storage.from(BUCKET).remove([ruta]);
  if (error) {
    console.error("Error al eliminar", error.message);
    return responder(500, { error: "No se pudo eliminar la imagen" });
  }
  return responder(200, { eliminada: true });
}

Deno.serve(async (req) => {
  if (req.method !== "POST" && req.method !== "DELETE") {
    return responder(405, { error: "Método no permitido" });
  }
  const uid = await uidAutenticado(req);
  if (!uid) return responder(401, { error: "Sesión no válida" });
  return req.method === "POST" ? subir(req, uid) : eliminar(req, uid);
});
