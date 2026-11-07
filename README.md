# SportPro

SportPro - App móvil de gestión de academias deportivas (Desarrollo de Aplicaciones Móviles - Universidad ESAN)

## Stack

- Kotlin + Jetpack Compose (Material 3)
- Arquitectura MVVM: `ViewModel` + `StateFlow` + Navigation Compose
- Firebase: Authentication, Cloud Firestore (persistencia offline habilitada), Cloud Messaging
- Supabase Storage: imágenes de jugadores, ejercicios y publicaciones
- Inyección de dependencias con Hilt

## Requisitos

- Android Studio (verificado con la serie 2026.1.x del equipo; si al abrir el proyecto Android
  Studio ofrece actualizar el Android Gradle Plugin / Gradle Wrapper, es seguro aceptar).
- JDK 17.
- El repositorio incluye el Gradle Wrapper: ejecuta `./gradlew assembleDebug` (o `gradlew.bat` en
  Windows) y `./gradlew testDebugUnitTest` para las pruebas unitarias.

## Firebase

El proyecto Firebase (`sportpro-198a4`) ya existe y `app/google-services.json` ya está
incluido en el repo con la app Android registrada (`com.esan.sportpro`). Servicios habilitados:

- Authentication (Email/Password)
- Cloud Firestore (activar reglas de seguridad por colección antes de manejar datos reales)
- Cloud Messaging

## Supabase (imágenes)

Las imágenes se guardan en el bucket `imagenes` del proyecto Supabase `sportpro`. La app no las
sube directo: llama a la Edge Function `imagenes`, que valida el token de Firebase Auth y es la
única con la clave secreta. El código está en `supabase/` (función, migración y configuración).

### Resumen con IA (US-023)

La Edge Function `resumen-ia` llama a la API de Gemini; la clave vive solo como secreto de Supabase
y nunca está en la app ni en Git. Para desplegarla (una vez, con la CLI de Supabase y sesión iniciada):

```bash
supabase secrets set GEMINI_API_KEY=<clave de Google AI Studio> --project-ref ghowgsannwmpuwncwrcc
supabase functions deploy resumen-ia --project-ref ghowgsannwmpuwncwrcc --no-verify-jwt
```

Guardar el secreto exige ser Owner o Administrator del proyecto de Supabase (con el rol Developer falla).

Modelos: la función prueba hasta tres, en orden, y cada intento pasa al siguiente si el anterior
responde 503 o falla. Por defecto `gemini-3.5-flash`, `gemini-3.6-flash` y `gemini-3.5-flash-lite`
(los modelos más nuevos suelen saturarse). Se pueden cambiar sin tocar el código con
`supabase secrets set GEMINI_MODELOS=a,b,c` o fijar solo el preferido con `GEMINI_MODEL=<modelo>`.
El modelo que realmente respondió queda guardado en cada versión del resumen.
Pasos de validación para el equipo: [`docs/guia-validacion-us021-us022-us023.md`](docs/guia-validacion-us021-us022-us023.md).

Si necesitas acceso de administrador a la consola de Firebase, pídele al dueño del proyecto
que te agregue en **Configuración del proyecto → Usuarios y permisos**.

## Estructura de carpetas

```
app/src/main/java/com/esan/sportpro/
├── core/            # theme, ViewModel base, utilidades compartidas
├── di/              # módulos Hilt (Firebase, etc.)
├── navigation/       # rutas y NavHost raíz
├── messaging/        # FirebaseMessagingService
├── data/<modulo>/     # DTOs, fuentes remotas (Firestore/Storage), repos
├── domain/<modulo>/   # modelos de dominio, casos de uso, interfaces de repo
└── ui/<modulo>/       # pantallas Compose + ViewModels del módulo
```

Módulos (`<modulo>`): `cuentas`, `academia`, `jugadores`, `entrenamientos`, `partido`,
`comunidad`, `ia`. Cada integrante trabaja principalmente dentro de su propia carpeta de
módulo en `data/`, `domain/` y `ui/`; los únicos archivos compartidos que pueden generar
conflicto son `navigation/SportProNavHost.kt`, `navigation/NavRoutes.kt` y los módulos en
`di/` — al tocarlos, agrega solo tu propia entrada y avisa en el PR.

## Convención de ramas

```
feature/US-0XX-nombre-corto
```

El número coincide con el issue de GitHub de la historia de usuario correspondiente
(`US-001` a `US-030`). Ejemplo: `feature/US-014-programacion-partido`. Cada PR debe
referenciar `Closes #<numero>` para cerrar el issue al mergear.

## Historias de usuario

Las 30 historias de usuario están documentadas como GitHub Issues (`US-001` a `US-030`),
etiquetadas por rol (Administrador, Entrenador, Jugador, Padre de familia).

## Estado del proyecto (entrega semana 7)

| Módulo | Historias | Estado |
|---|---|---|
| Cuentas: registro con rol, login, recuperar contraseña, sesión persistente, cierre de sesión | US-001, US-002 | Implementado |
| Navegación principal por rol | US-003 | Implementado: barra de 4 pestañas por rol, Inicio con resumen y "Más" (`MenuPrincipal`) |
| Academia: equipos por categoría | US-004, US-005 | Implementado (alta, edición, desactivar) |
| Jugadores: perfil con datos físicos, contacto y emergencia | US-006 | Implementado (fotos en Supabase Storage; edición por el propio jugador pendiente del enlace cuenta–ficha) |
| Jugadores: vinculación padre–jugador (código, solicitudes, aprobación, revocación con auditoría) | US-007 | Implementado (notificación push pendiente de US-027) |
| Entrenamientos: biblioteca de ejercicios | US-009 | Implementado (ficha completa, buscador con filtros, permisos por autor, desactivación) |
| Entrenamientos: registro de asistencia (sin conexión, historial de cambios, cierre con % del equipo) | US-011 | Implementado |
| Entrenamientos: planificación de sesiones (duración, solapamiento, reordenar, duplicar, cancelar) | US-010 | Implementado (notificación push pendiente de US-027) |
| Entrenamientos: historial de participación del jugador (resumen, filtros, gráfico de 6 meses) | US-012 | Implementado para entrenador, administrador y padre (vista del jugador y filtro de partidos pendientes del enlace cuenta↔ficha y de las convocatorias) |
| Partido en vivo, eventos y correcciones | — | Implementado (ver `docs/diseno-tecnico-partido-en-vivo.md`) |
| Partido: cierre y acta final (resumen con minutos jugados, reapertura del administrador en 48 h con auditoría, compartir como texto) | US-022 | Implementado; hay que republicar `firestore.rules` |
| Partido: seguimiento en tiempo real para jugador y padre (cronología, alineación, estadísticas, animaciones, aviso sin conexión) | US-021 | Implementado; republicar `firestore.rules`. El jugador necesita el enlace cuenta↔ficha (`usuarioUid`), todavía pendiente |
| Mensualidades, Estadísticas, Anuncios, Comunidad | US-008, US-026 a US-030 | Implementado |
| Resumen narrativo con IA: generación del borrador con verificación de datos (Edge Function de Supabase + Gemini) | US-023 | Implementado (ver `docs/diseno-integracion-ia.md`, sección 13). Falta desplegar la función con su clave y republicar `firestore.rules`. Revisión, aprobación y evaluación (US-024, US-025) pendientes |
| Resumen con IA: revisión, corrección y aprobación del entrenador, con publicación en el partido y, si se elige, en la comunidad | US-024 | Implementado (ver `docs/diseno-integracion-ia.md`, sección 14); hay que republicar `firestore.rules` |
| Resumen con IA: evaluación documentada, consolidado y exportación como texto | US-025 | Implementado (ver sección 14); hay que republicar `firestore.rules` |

## Documentación técnica

- [`docs/diseno-tecnico-partido-en-vivo.md`](docs/diseno-tecnico-partido-en-vivo.md): registro de eventos en tiempo real, offline y trazabilidad.
- [`docs/diseno-integracion-ia.md`](docs/diseno-integracion-ia.md): integración segura con el proveedor de IA.
- [`docs/modelo-datos-gestion.md`](docs/modelo-datos-gestion.md): colecciones de equipos, jugadores y entrenamientos, permisos por rol, criterio de fechas y regla del apoderado.
- [`docs/guia-estilo-ui.md`](docs/guia-estilo-ui.md): línea gráfica común (tema, componentes, navegación) y lista de revisión para los PR.
- `firestore.rules`: reglas de seguridad. Copiarlas en Firebase Console → Firestore Database → Reglas → Publicar.
