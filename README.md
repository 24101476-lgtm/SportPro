# SportPro

SportPro - App móvil de gestión de academias deportivas (Desarrollo de Aplicaciones Móviles - Universidad ESAN)

## Stack

- Kotlin + Jetpack Compose (Material 3)
- Arquitectura MVVM: `ViewModel` + `StateFlow` + Navigation Compose
- Firebase: Authentication, Cloud Firestore (persistencia offline habilitada), Cloud Storage, Cloud Messaging
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
- Cloud Storage
- Cloud Messaging

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
| Navegación principal por rol | US-003 | Implementado (`HomeModule.visiblesPara`) |
| Academia: equipos por categoría | US-004, US-005 | Implementado (alta, edición, desactivar) |
| Jugadores: perfil con datos físicos, contacto y emergencia | US-006, US-007 | Implementado (sin fotografía) |
| Entrenamientos: planificación, biblioteca de ejercicios, asistencia e historial | US-009 a US-013 | Implementado |
| Partido en vivo, eventos y correcciones | — | Implementado (ver `docs/diseno-tecnico-partido-en-vivo.md`) |
| Mensualidades, Estadísticas, Anuncios, Comunidad | US-008, US-026 a US-030 | Implementado |
| Resumen narrativo con IA | — | Diseño listo (`docs/diseno-integracion-ia.md`); implementación en semana 12 |

## Documentación técnica

- [`docs/diseno-tecnico-partido-en-vivo.md`](docs/diseno-tecnico-partido-en-vivo.md): registro de eventos en tiempo real, offline y trazabilidad.
- [`docs/diseno-integracion-ia.md`](docs/diseno-integracion-ia.md): integración segura con el proveedor de IA.
- [`docs/modelo-datos-gestion.md`](docs/modelo-datos-gestion.md): colecciones de equipos, jugadores y entrenamientos, y permisos por rol.
- `firestore.rules`: reglas de seguridad. Copiarlas en Firebase Console → Firestore Database → Reglas → Publicar.
