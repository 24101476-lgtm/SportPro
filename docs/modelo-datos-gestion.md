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
   codigoVinculacion (6 caracteres, único en la academia; US-007), fotoUrl
   activo, creadoPor

directorio_jugadores/{jugadorId}       (US-007) datos mínimos que ve el apoderado para pedir el vínculo
   academiaId, equipoId, equipoNombre ("Equipo · Categoría"), nombre, codigo, activo
   apoderadosAprobados (0-2; lo mantiene la aprobación/revocación, las reglas impiden pasar de 2)

vinculaciones/{padreUid}_{jugadorId}   (US-007) vínculo padre/madre ↔ jugador
   academiaId, padreUid, padreNombre, jugadorId, jugadorNombre, equipoId, equipoNombre
   estado: PENDIENTE | APROBADA | RECHAZADA | REVOCADA, motivoRechazo
   solicitadaEn, resueltaPor, resueltaPorNombre, resueltaEn
vinculaciones/{id}/auditoria/{registroId}   cada acción: accion (SOLICITUD | APROBACION | RECHAZO | REVOCACION), uid, nombre, detalle, fecha

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
| **Perfil del jugador** (datos físicos, contacto, emergencia) | Lee y edita | Lee y edita | **No accede** | **Solo sus hijos con vínculo aprobado** (lectura) |
| Solicitudes de vinculación | Aprueba, rechaza y **revoca** | Aprueba y rechaza | No | Crea las suyas y ve su estado |
| Entrenamientos y objetivos | Lee y edita | Lee y edita | Lee | No ve el módulo |
| Tomar asistencia | Sí | Sí | No | No |
| Biblioteca de ejercicios: ver y crear | Sí | Sí | No | No |
| Biblioteca: editar, desactivar, eliminar | Todos los del club | Solo los suyos | No | No |

La restricción se aplica en tres capas: (1) la pestaña no aparece (`HomeModule.visiblesPara`), (2) el ViewModel valida el rol antes de cargar o escribir, (3) `firestore.rules` rechaza la operación en el servidor. Las reglas de `jugadores` también limitan la lectura a la **misma academia** del usuario.

## Criterio de fechas

Implementado en `core/fecha/FechasSportPro.kt` (corrección de la semana 7). Cada dato se guarda según lo que representa:

| Tipo de dato | Cómo se guarda | Ejemplos |
|---|---|---|
| Fecha de calendario (sin hora) | Texto ISO `AAAA-MM-DD` | Nacimiento del jugador, día del entrenamiento |
| Hora del día | Texto `HH:mm` (24 horas) | Inicio del entrenamiento o del partido |
| Instante (cuándo ocurrió algo) | `Timestamp` del servidor o milisegundos UTC | Evento del partido (`registradoEnMs`), aprobación de un vínculo, publicación |
| Minuto de juego | Número entero (`Int`) | Minuto de una falta o un gol |

- Se **muestra** siempre en hora de Perú (`America/Lima`) con `dd/MM/yyyy`, `HH:mm` o `dd/MM/yyyy HH:mm`.
- Se **ingresa** con calendario y reloj (`CampoFecha`, `CampoHora`), que solo ofrecen fechas dentro del rango válido. Ya no se escriben fechas a mano.
- Ninguna pantalla crea su propio `SimpleDateFormat`: se reemplazaron 17 formateadores sueltos por `FechasSportPro`.

## Regla del apoderado

**Un jugador menor de 18 años no queda registrado hasta que tenga un apoderado vinculado** (`domain/jugadores/ReglasApoderado.kt`).

1. La ficha de un menor exige los datos del apoderado: nombre, parentesco (Madre, Padre, Tutor legal, Abuelo(a), Tío(a), Hermano(a) mayor), teléfono y correo.
2. Al guardarla sin apoderado aprobado queda con `estadoRegistro = PENDIENTE_APODERADO`. La app muestra el código de vinculación para entregarlo al apoderado.
3. Mientras está pendiente **no aparece** en la asistencia, en la convocatoria de partidos ni en los conteos del plantel.
4. Al aprobar la solicitud del primer apoderado (US-007) pasa a `COMPLETO` en el mismo lote de escritura. Si se revoca su único apoderado, vuelve a `PENDIENTE_APODERADO`.
5. Un jugador menor **no puede crear su propia cuenta** en el registro: lo registra la academia y accede a través de su apoderado.

Las fichas creadas antes de esta regla no tienen `estadoRegistro` y cuentan como registradas; al editarlas se recalcula el estado.

## Pendientes conocidos

- **Fotografía del jugador** (US-006): el modelo no incluye `fotoUrl` todavía; se agregará con Cloud Storage.
- **Notificación push** al personal cuando llega una solicitud de vinculación (US-007, criterio 2): depende del envío de notificaciones (US-027). Mientras tanto, la lista de Jugadores muestra "Solicitudes de vinculación (N pendientes)".
- **Lectura de `equipos`, `entrenamientos` y `asistencias_entrenamiento`** está abierta a cualquier usuario autenticado (no filtra por academia). Se endurece cuando exista el vínculo con la academia.
- ~~Regla de `usuarios`~~ **Resuelto:** nadie puede crear un perfil de `ADMINISTRADOR` sin un código de invitación válido, ni cambiar su propio rol, estado, academia o marca de menor de edad.
- **Registro como Entrenador**: cualquier persona puede elegir el rol Entrenador al registrarse y así ver datos del plantel. Conviene exigir también un código de invitación para ese rol.
- **Comunidad** (`publicaciones`, `avisos`, `comentarios`): las reglas siguen abiertas a usuarios autenticados porque la moderación y el contador de reportes los actualizan varios roles; falta restringir por campo.
