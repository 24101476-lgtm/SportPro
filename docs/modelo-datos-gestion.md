# Modelo de datos: gestión de equipos, jugadores y entrenamientos

Colecciones de Cloud Firestore de los módulos **Academia**, **Jugadores** y **Entrenamientos**, y las reglas de privacidad que aplican. Complementa `diseno-tecnico-partido-en-vivo.md` (partidos y eventos).

## Colecciones

```
usuarios/{uid}                         (US-001, módulo Cuentas)
   nombres, apellidos, correo, telefono, fechaNacimiento
   rol: ADMINISTRADOR | ENTRENADOR | JUGADOR | PADRE_DE_FAMILIA
   estado: ACTIVO | DESACTIVADO, esMenor, clubId (null hasta vincular a una academia)

equipos/{equipoId}                     (US-004/US-005)
   academiaId, nombre, categoria (Sub-8 … Primera), entrenadorNombre
   activo, creadoPor

jugadores/{jugadorId}                  (US-006/US-007)
   academiaId, equipoId, equipoNombre
   nombre (denormalizado para Estadísticas), nombres, apellidos, fechaNacimiento (AAAA-MM-DD)
   posicion, dorsal?, estaturaCm?, pesoKg?
   telefono, correo
   contactoEmergenciaNombre, contactoEmergenciaTelefono, contactoEmergenciaParentesco
   activo, creadoPor

entrenamientos/{entrenamientoId}       (US-009 a US-013)
   academiaId, equipoId, equipoNombre
   fecha (AAAA-MM-DD), hora (HH:mm), lugar, objetivo
   ejercicios[] { nombre, descripcion, duracionMin, ejercicioId }   (copia; ejercicioId = id en la biblioteca o "")
   asistencia { jugadorId: PRESENTE | TARDE | JUSTIFICADO | AUSENTE }   (ausente del mapa = "Sin marcar")
   asistenciaDetalle { jugadorId: { estado, minutosRetraso?, motivo, marcadoPor, marcadoPorNombre, marcadoEn (hora servidor) } }
   estado: PROGRAMADA | REALIZADA | CANCELADA   (vacío en sesiones antiguas = Programada)
   asistenciaRegistrada (true al cerrar; se mantiene por compatibilidad), porcentajeAsistencia?, realizadaEn
   creadoPor

entrenamientos/{id}/historial_asistencia/{registroId}   (US-011) cada cambio de asistencia; nunca se edita ni borra
   jugadorId, jugadorNombre, estado, minutosRetraso?, motivo, marcadoPor, marcadoPorNombre
   marcadoEn (hora servidor), marcadoEnDispositivo (ms, para ordenar pendientes)

ejercicios/{ejercicioId}               (US-009) biblioteca reutilizable de la academia
   academiaId, nombre (3-60, único en el club), objetivo, categoria (CategoriaTecnica.name)
   duracionMin (1-120), minJugadores (1-40), materiales, descripcion (20-1000)
   imagenUrl ("" si no tiene), activo, creadoPor
   Imagen en Cloud Storage: ejercicios/{ejercicioId}/imagen.jpg (JPEG, máx. 1024 px por lado)

asistencias_entrenamiento/{entrenamientoId_jugadorId}   consumida por Estadísticas
   entrenamientoId, equipoId, jugadorId, estado, presente (bool), minutosRetraso?, fecha (Timestamp)
   Se escribe al cerrar la sesión (estado Realizada).
```

## Decisiones

| Decisión | Motivo |
|---|---|
| Nada se borra: equipos y jugadores se **desactivan** (`activo = false`); las reglas tienen `allow delete: if false` | Los partidos, entrenamientos y estadísticas históricos referencian esos ids |
| Fechas y horas como **texto ISO** (`2026-10-15`, `16:30`) | Evita problemas de zona horaria; se ordenan alfabéticamente. Solo `asistencias_entrenamiento.fecha` es `Timestamp`, porque así lo espera Estadísticas |
| Consultas con **un solo filtro de igualdad** (`academiaId`) y orden en memoria | No requieren índices compuestos; el despliegue no depende de crearlos en la consola |
| Escrituras con `esperarConfirmacion()` (4 s) | Con la persistencia offline, la escritura queda en caché y no bloquea la pantalla; la app avisa "Se sincronizará cuando vuelva la conexión" |
| Las sesiones guardan una **copia** de cada ejercicio (con `ejercicioId`) | Editar o desactivar un ejercicio de la biblioteca no altera las sesiones históricas (US-009, criterio 8) |
| Un ejercicio usado en una sesión realizada **no se elimina, se desactiva** | La app lo verifica antes de borrar; "realizada" = `asistenciaRegistrada` hasta que US-011 agregue el estado de la sesión |
| Cada marca de asistencia **se guarda al instante** con la persistencia offline de Firestore | El registro funciona sin conexión; el indicador usa `hasPendingWrites` (MetadataChanges.INCLUDE) para mostrar "Sin conexión…" / "Sincronizado a las HH:mm" (US-011) |
| Conflictos: **gana la última escritura** que llega al servidor (`marcadoEn` = hora del servidor) | Ambos cambios quedan en `historial_asistencia`; las reglas impiden editar o borrar el historial y cambiar la asistencia de una sesión Realizada |
| `asistencia` dentro del documento del entrenamiento | Un solo listener alimenta la lista, el detalle y el historial por jugador; contiene solo ids y estados |
| `academiaId = clubId` del usuario, o `academia-demo` si es nulo | Es el valor temporal que ya usan Mensualidades y Comunidad, hasta que US-004 vincule usuarios a academias |

## Privacidad y permisos por rol

| Dato | Administrador | Entrenador | Jugador | Padre de familia |
|---|---|---|---|---|
| Equipos (nombre, categoría) | Lee y edita | Lee | Lee | Lee |
| **Perfil del jugador** (datos físicos, contacto, emergencia) | Lee y edita | Lee y edita | **No accede** | **No accede** |
| Entrenamientos y objetivos | Lee y edita | Lee y edita | Lee | No ve el módulo |
| Tomar asistencia | Sí | Sí | No | No |
| Biblioteca de ejercicios: ver y crear | Sí | Sí | No | No |
| Biblioteca: editar, desactivar, eliminar | Todos los del club | Solo los suyos | No | No |

La restricción se aplica en tres capas: (1) la pestaña no aparece (`HomeModule.visiblesPara`), (2) el ViewModel valida el rol antes de cargar o escribir, (3) `firestore.rules` rechaza la operación en el servidor. Las reglas de `jugadores` también limitan la lectura a la **misma academia** del usuario.

## Pendientes conocidos

- **Fotografía del jugador** (US-006): el modelo no incluye `fotoUrl` todavía; se agregará con Cloud Storage.
- **Vincular padre ↔ hijo** para que un padre vea datos de su propio hijo (hoy no accede al módulo Jugadores).
- **Lectura de `equipos`, `entrenamientos` y `asistencias_entrenamiento`** está abierta a cualquier usuario autenticado (no filtra por academia). Se endurece cuando exista el vínculo con la academia.
- **Regla de `usuarios`**: hoy un usuario puede escribir su propio documento, incluido el campo `rol`. Hay que restringir la creación de `ADMINISTRADOR` a quien tenga un código de invitación válido (verificación en servidor).
