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
- El proyecto **no incluye `gradlew`/`gradle-wrapper.jar`** todavía: al abrir la carpeta en
  Android Studio, acepta la generación automática del wrapper (o ejecuta
  `gradle wrapper --gradle-version 8.11` una vez si tienes Gradle instalado localmente) y
  commitea esos archivos en tu primer PR de infraestructura.

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
