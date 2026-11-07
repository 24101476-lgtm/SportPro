# Diseño técnico: registro de eventos del partido en tiempo real

Módulo **Partido** de SportPro. Este documento describe cómo se registran los eventos de un partido desde el celular mientras se juega, cómo llegan en tiempo real a los demás dispositivos y cómo se conserva la información si falla la conexión o si los datos están incompletos.

## 1. Objetivos

| Objetivo | Cómo se resuelve |
|---|---|
| Registrar eventos en máximo tres toques | Paleta dinámica desde el catálogo + hoja inferior con minuto y equipo precargados |
| Marcador y cronología en vivo en todos los dispositivos | Listeners de Cloud Firestore (`addSnapshotListener`) sobre el partido y sus eventos |
| Corregir sin perder trazabilidad | Versionado de eventos: nunca se sobrescribe ni se borra un documento |
| Funcionar sin conexión en la cancha | Persistencia local de Firestore + escrituras sin `await()` |
| No detener el registro por falta de datos | Opciones *Jugador no identificado* / *Jugador del rival* y etiqueta *Incompleto* |

## 2. Arquitectura

```
ui/partido        Compose + ViewModel (MVVM, StateFlow)
   │                PartidoScreen ─ NavHost interno (lista ↔ registro)
   │                RegistroPartidoScreen, EventoBottomSheet, DialogosPartido
   ▼
domain/partido    Modelos, CatalogoBase, ReglasPartido (funciones puras), PartidoRepository
   ▲
data/partido      FirestorePartidoRepository, PartidoEnCursoStore
core/red          ConectividadObserver (ConnectivityManager → Flow<Boolean>)
di                PartidoModule (Hilt: PartidoRepository → FirestorePartidoRepository)
```

- **ReglasPartido** no depende de Android ni de Firebase: calcula marcador, jugadores en cancha, suplentes disponibles, incompletos, duplicados, minuto del cronómetro y validaciones. Así el mismo cálculo sirve para la vista del espectador, las estadísticas y el insumo del resumen con IA.
- **RegistroPartidoViewModel** combina seis flujos (partido, eventos, convocados, catálogo, rol, conectividad) y un ticker de 1 s para el cronómetro, y expone un único `StateFlow<RegistroUiState>`.

## 3. Modelo de datos en Firestore

```
partidos/{partidoId}
   equipoId, academiaId                     (equipo de la academia que juega el partido)
   nombreEquipo, nombreRival, rivalNombre, categoria, fecha
   tipo: AMISTOSO | LIGA | TORNEO, temporada  (filtros de Estadísticas)
   golesEquipo, golesRival                  (marcador sincronizado desde los eventos vigentes)
   actaCerrada                              (true al cerrar el acta; el estado queda en FINALIZADO)
   estado: PROGRAMADO | EN_CURSO | FINALIZADO   (la app muestra "Acta cerrada" si actaCerrada = true)
   alineacionConfirmada, cambiosPermitidos, duracionTiempoMin
   inicioPrimerTiempo, inicioSegundoTiempo  (serverTimestamp)
   enDescanso, finPrimerTiempoMinuto, finPartidoMinuto
   operadorUid, entrenadorUid, creadoPor
   actaCerradaPor, actaCerradaEn, actaCerradaConIncompletos

partidos/{partidoId}/convocados/{jugadorId}
   nombre, dorsal, titular, posicion

partidos/{partidoId}/eventos/{eventoId}          ← una versión por documento
   grupoId, version                              (todas las versiones de un evento comparten grupoId)
   tipo, tipoNombre, versionCatalogo
   minuto, equipo (PROPIO | RIVAL)
   jugadorId, jugadorNombre                      (o RIVAL / NO_IDENTIFICADO)
   jugadorSecundarioId, jugadorSecundarioNombre  (asistencia o jugador que entra)
   resultadoPenal, observacion
   operadorUid, operadorNombre, registradoEn (serverTimestamp)
   estado: VIGENTE | REEMPLAZADO | ANULADO
   incompleto, fueraDeOrden, excedeCambios, eventoOrigenId
   motivo, camposModificados, versionAnteriorId                   (correcciones)
   reemplazadoPor, reemplazadoPorUid, reemplazadoEn               (versión anterior)
   motivoAnulacion, anuladoPor, anuladoPorNombre, anuladoEn       (anulación)

catalogoEventos/{tipoId}
   nombre, ambito (PARTIDO | EQUIPO | JUGADOR), camposObligatorios[], sumaMarcador, orden, activo, version
```

El marcador **no se guarda**: se calcula a partir de los eventos vigentes cuyo tipo tiene `sumaMarcador = true`. Así, anular o corregir un gol recalcula el marcador automáticamente en todos los dispositivos, sin escrituras adicionales que puedan quedar desincronizadas.

## 4. Flujo de registro en tiempo real

```mermaid
sequenceDiagram
    participant O as Operador (celular)
    participant VM as RegistroPartidoViewModel
    participant C as Caché local Firestore
    participant S as Servidor Firestore
    participant E as Otros dispositivos

    O->>VM: Toca "Gol" en la paleta
    VM-->>O: Hoja inferior (minuto y equipo precargados)
    O->>VM: Elige jugador y pulsa Guardar
    VM->>VM: ReglasPartido.validar(...)
    VM->>C: batch.set(evento) (+ eventos relacionados)
    C-->>VM: Snapshot local (hasPendingWrites = true) → ícono de reloj
    C->>S: Sincroniza cuando hay red
    S-->>VM: Confirmación (hasPendingWrites = false) → ícono de verificación
    S-->>E: Snapshot con el nuevo evento → marcador y cronología actualizados
```

- Las escrituras usan `WriteBatch.commit()` **sin `await()`**: sin conexión la tarea no se completa hasta recuperar la red, pero el evento ya está en la caché local y aparece de inmediato en la cronología.
- Los listeners usan `MetadataChanges.INCLUDE` para detectar el paso de *pendiente* a *confirmado* de cada evento.
- Eventos derivados en el mismo lote, enlazados con `eventoOrigenId`:
  - **Penal convertido** genera el evento *Gol*.
  - **Segunda amarilla** al mismo jugador genera *Tarjeta roja por doble amonestación*.
  - Al anular el evento origen se anulan también sus derivados.

### Cronómetro

El inicio de cada tiempo se guarda con `FieldValue.serverTimestamp()` y se lee con `ServerTimestampBehavior.ESTIMATE`. El minuto se calcula como `(ahora − inicio del periodo) / 60 s` (más la duración del primer tiempo en el segundo). No depende de un contador en memoria, así que sigue siendo correcto si la app pasa a segundo plano, se cierra o el teléfono se reinicia.

- **1.er tiempo:** corre hasta la duración configurada (45' por defecto) y luego cuenta tiempo añadido, como máximo 15 minutos ("45:00 +3'").
- **Descanso:** el reloj queda detenido.
- **Descanso:** el reloj de juego se detiene y se muestra cuánto lleva el descanso (desde `finPrimerTiempo`).
- **2.º tiempo:** corre hasta 2 × duración (90') y luego cuenta tiempo añadido ("90:00 +3'"). El operador registra *Fin de partido*. Si nadie lo hace, al agotar 15 minutos de añadido el dispositivo del operador lo registra automáticamente. Solo lo hace un dispositivo, para evitar duplicados.
- **Control del tiempo:** no se puede anular el inicio ni el final de un tiempo, porque dejaría el reloj incoherente. Solo se corrige su minuto con *Editar*.

### Reglas del registro

| Regla | Comportamiento |
|---|---|
| Inicio sin alineación confirmada | Bloqueado: "Debes confirmar la alineación antes de iniciar el partido" |
| Minuto fuera de 0–130 | "Minuto inválido" |
| Selector de jugador | Primero los titulares en cancha, luego los suplentes, además de *Jugador del rival* y *Jugador no identificado* |
| Cambio | El que sale debe estar en cancha y el que entra debe ser un suplente no utilizado. La lista en cancha se recalcula con los eventos |
| Exceso de cambios | Advertencia "Superaste el número de cambios permitidos"; si se confirma, el evento queda con `excedeCambios = true` |
| Tarjeta roja | Retira al jugador de la lista en cancha |
| Botones de la paleta | Alto mínimo de 56 dp (por encima del mínimo de 48 dp) |

## 5. Corrección y anulación con trazabilidad

- **Editar** crea un documento nuevo (`version + 1`, mismo `grupoId`) con motivo (5–200 caracteres) y `camposModificados`. En el mismo lote, la versión anterior pasa a `REEMPLAZADO` con quién y cuándo.
- **Anular** cambia el estado a `ANULADO` con motivo obligatorio. El evento sigue visible en la cronología, tachado, y se excluye del marcador, las estadísticas y el resumen con IA.
- **Ver historial** lista todas las versiones del `grupoId` con usuario, fecha y hora, campos modificados y motivo.
- **Permisos:** solo pueden corregir el operador asignado, el entrenador responsable, el creador del partido y el administrador. Se valida en la app y en las reglas de seguridad.
- **Borrado:** no existe en la app, y en `firestore.rules` la subcolección `eventos` tiene `allow delete: if false`. Además, la regla de `update` solo permite cambiar el estado y los campos de trazabilidad: el contenido original no se puede sobrescribir.

## 6. Datos incompletos

- Todo evento de jugador puede guardarse con *Jugador no identificado*. Queda con `incompleto = true`, cuenta para el equipo pero no para un jugador.
- Un contador permanente muestra los eventos incompletos junto a la acción **Completar pendientes**, que asigna el jugador faltante creando una nueva versión del evento.
- **Registro fuera de orden:** si el minuto es menor al del último evento, se avisa, se guarda con `fueraDeOrden = true` y la cronología se ordena por minuto (con la hora real de registro como desempate).
- **Inicio del segundo tiempo sin fin del primero:** aparece "Falta registrar el fin del primer tiempo" y se ofrece insertarlo con el minuto estimado.
- **Cierre del acta con incompletos:** el resumen del acta (sección 6.1) advierte "Se cerrará con X eventos incompletos". La cantidad queda registrada en `actaCerradaConIncompletos`.
- Los campos opcionales vacíos quedan en `null` y se muestran como "Sin información".

### 6.1 Cierre y acta final (US-022)

- **Cierre:** solo existe con el partido en `FINALIZADO`, es decir, después de registrar *Fin de partido*. Antes de confirmar, el diálogo muestra el resumen del acta: marcador final, eventos vigentes, anulados e incompletos, operador, hora real de inicio y fin, versión del catálogo de eventos y minutos jugados por jugador. Al confirmar se guardan `actaCerrada = true`, `actaCerradaPor` y `actaCerradaEn` (marca del servidor); el estado sigue en `FINALIZADO` para que Estadísticas lo consulte, y las reglas bloquean todo evento nuevo.
- **Minutos jugados** (`ReglasActa.minutosJugados`, función pura): titulares desde el 0'; suplentes desde que entran; hasta que salen por un cambio, son expulsados o termina el partido. Solo cuentan cambios y rojas vigentes del equipo propio. Como el minuto del 2.º tiempo vuelve a empezar en la duración del tiempo, se le suma el tiempo añadido del 1.º (45+3' → el 2.º tiempo arranca en el minuto real 48).
- **Reapertura:** solo el administrador, con el acta cerrada y dentro de las **48 horas** posteriores a `actaCerradaEn`, indicando un motivo de 5 a 200 caracteres. El partido vuelve a `FINALIZADO` con `actaCerrada = false`, se incrementa `vecesReabierto` y se agrega una entrada inmutable en `partidos/{id}/auditoria` (`accion = REABRIR_ACTA`, motivo, uid, nombre, fecha). La ventana se valida en la app (`ReglasActa.puedeReabrir`) y en `firestore.rules`.
- **Compartir:** el acta se comparte como texto plano con el selector nativo de Android (`ReglasActa.textoCompartible`).
- **Habilitación del resumen con IA (US-023):** un partido con `actaCerrada = true` es el que podrá generar el resumen narrativo.

## 7. Operación sin conexión y resincronización

| Situación | Comportamiento |
|---|---|
| Sin red | Franja fija "Sin conexión. X eventos pendientes de sincronizar" (vía `ConnectivityObserver`) |
| Evento guardado offline | Aparece al instante con ícono de reloj; pasa a verificación cuando el servidor confirma |
| Vuelve la red | Firestore sincroniza solo; la app muestra "Sincronización completa. X eventos enviados" |
| Orden de la cronología | El minuto viene del cronómetro del partido, no de la hora de sincronización |
| Dos operadores offline | Se conservan todos los eventos. Los de igual tipo, minuto, equipo y jugador se marcan como **posibles duplicados** para que el entrenador anule el que corresponda |
| App cerrada o teléfono apagado | `PartidoEnCursoStore` recuerda el partido en curso y lo reabre. Cronómetro, marcador y eventos pendientes se restauran desde la caché persistente de Firestore |

La persistencia se habilita en `di/FirebaseModule.kt` con `persistentCacheSettings {}`.

## 8. Seguridad

Las reglas están en `firestore.rules` y se publican en Firebase Console → Firestore Database → Reglas.

- Leer exige estar autenticado.
- Para crear eventos hay que ser el operador, el entrenador, el creador del partido o un administrador. Además, `operadorUid` debe ser el usuario autenticado, el estado inicial debe ser `VIGENTE` y el minuto debe estar entre 0 y 130.
- Con el acta cerrada se bloquea cualquier escritura del partido, salvo la reapertura del administrador dentro de las 48 h (solo cambian `actaCerrada`, `estado`, `motivoReapertura`, `reabiertoPor`, `reabiertoEn` y `vecesReabierto`).
- `auditoria` solo admite crear entradas: el administrador, a su nombre y junto con una reapertura; nunca se editan ni se borran.
- El catálogo de eventos solo lo modifica el administrador.

## 9. Matriz de pruebas de casos sucios

| # | Escenario | Resultado esperado |
|---|---|---|
| 1 | Iniciar un partido sin alineación confirmada | Mensaje de bloqueo; el estado no cambia |
| 2 | Falta con *Jugador no identificado* | Evento *Incompleto*; el contador sube en 1 |
| 3 | Completar ese pendiente asignando un jugador | Nueva versión sin la etiqueta; el contador baja |
| 4 | Registrar el minuto 30 después de un evento en el 40 | Aviso "Registro fuera de orden"; queda ordenado por minuto |
| 5 | Minuto 140 | "Minuto inválido"; no se guarda |
| 6 | Inicio del 2.º tiempo sin fin del 1.º | Diálogo y opción de insertar con minuto estimado |
| 7 | Cambio con un jugador que no está en cancha | "El jugador que sale no está en cancha" |
| 8 | Cambio número 6 con 5 permitidos | Advertencia; si se confirma, etiqueta *Excede cambios* |
| 9 | Segunda amarilla al mismo jugador | Roja automática y salida de la cancha |
| 10 | Penal convertido | Penal + gol; el marcador sube |
| 11 | Anular un gol | Tachado *Anulado*; el marcador baja en todos los dispositivos |
| 12 | Editar el minuto de un gol con motivo | Versión 2 vigente, versión 1 *Reemplazado*, historial completo |
| 13 | Modo avión, registrar 3 eventos | Franja "Sin conexión. 3 eventos pendientes…" e íconos de reloj |
| 14 | Quitar el modo avión | "Sincronización completa. 3 eventos enviados" e íconos de verificación |
| 15 | Dos celulares offline registran el mismo gol | Alerta de posibles duplicados; se anula uno |
| 16 | Cerrar la app durante el partido y reabrir | Se abre el partido en curso con el cronómetro correcto |
| 17 | Cerrar el acta con 2 incompletos | Confirmación explícita y cantidad registrada |
| 18 | Intentar borrar un evento desde la consola o un cliente | Rechazado por las reglas (`allow delete: if false`) |

Cada escenario debe documentarse con captura o video como evidencia. Los escenarios 13 y 14 se graban durante el partido de práctica.

## 10. Integración con los demás módulos

- **Catálogo de eventos:** la paleta se construye desde `catalogoEventos`. Si la colección está vacía, la app publica el catálogo base, que el administrador puede editar luego.
- **Convocatoria y alineación:** se leen de `partidos/{id}/convocados` (campo `titular`) y de `alineacionConfirmada`. Mientras ese módulo no tenga pantalla propia, la convocatoria y la alineación se arman en *Nuevo partido*.
- **Seguimiento en vivo, estadísticas y resumen con IA:** deben usar solo los eventos con `estado = VIGENTE` y pueden reutilizar `ReglasPartido` para el marcador y los cálculos.

## 11. Integración con Academia, Jugadores y Estadísticas

- **Creación del partido:** el entrenador o administrador elige un equipo de `equipos` (de su academia) y convoca jugadores activos de `jugadores`. Toca cada jugador para marcarlo como titular, suplente o no convocado. La alineación se guarda en `partidos/{id}/convocados`, con el id del jugador y solo datos deportivos (nombre, dorsal, posición), porque `jugadores` contiene datos de menores que solo puede leer el personal.
- **Roles:** solo el personal puede crear partidos (regla `esStaff()`). Jugadores y padres ven la lista y el marcador en vivo, pero no la paleta de registro.
- **Lista por academia:** cada usuario ve los partidos de su academia.
- **Estadísticas:**
  - `StatsRepository` lee los partidos con `estado == FINALIZADO`, `golesEquipo` y `golesRival`.
  - Los goles, asistencias, tarjetas, faltas y minutos jugados se calculan a partir de `partidos/{id}/eventos` (solo eventos vigentes del equipo propio) y de `convocados`.
  - Los eventos con "Jugador no identificado" aparecen en la fila "Sin asignar".

## 12. Seguimiento del espectador en tiempo real (US-021)

Pantalla de solo lectura (`SeguimientoPartidoScreen`, ruta `partidos/{partidoId}/en-vivo`) para jugador y padre de familia. Ninguna de sus pantallas ofrece registrar, editar ni anular eventos.

- **Tiempo real:** `SeguimientoPartidoViewModel` escucha con los mismos listeners de Firestore que el registro (`observarPartido`, `observarEventos`, `observarConvocados`, `observarCatalogo`). Cuando el operador registra un evento, el espectador lo ve sin hacer nada.
- **Cronología:** solo eventos `VIGENTE`, de más antiguo a más reciente (`ReglasSeguimiento.cronologiaEspectador`). Un evento anulado o corregido desaparece y el marcador se recalcula, porque sale de los mismos eventos (`ReglasPartido.marcador`).
- **Animaciones:** cada evento que llega después de abrir la pantalla entra con un fundido y un desplazamiento; los que ya estaban no se animan. Si sube la suma de goles, el marcador se agranda y se pinta de lima durante 3 s con la etiqueta "¡GOL!". La carga inicial y la anulación de un gol no lo disparan.
- **Conexión:** sin red aparece "Mostrando la última información recibida" con la hora de la última actualización. Solo cuenta como actualización la información que llega con conexión.
- **Pestañas:** Cronología, Alineación (titulares y suplentes con su situación: en cancha, sustituido, expulsado, ingresó, en banca) y Estadísticas por equipo (posesión de eventos, tiros, faltas, amarillas y rojas).
  - *Posesión de eventos:* reparte porcentualmente los eventos de juego de cada equipo, sin contar controles de tiempo ni cambios.
  - *Tiros:* goles más penales. El catálogo base no tiene un evento "Tiro"; un gol nacido de un penal convertido no se cuenta dos veces.
- **Quién ve qué (criterio 7):** `AccesoEspectador` resuelve los equipos permitidos. El padre ve los de sus jugadores con vínculo aprobado (`vinculaciones.equipoId`); el jugador, el de su ficha enlazada por `usuarioUid`. Administrador y entrenador no tienen restricción. La lista de partidos (`PartidosViewModel`) y la propia pantalla aplican la misma regla (`ReglasSeguimiento.puedeSeguir`).
- **Reglas de Firestore:** el jugador puede leer solo la ficha cuyo `usuarioUid` es su uid; es lo único que cambia en `jugadores`.
- **Pendiente conocido:** nada escribe todavía `usuarioUid` en las fichas (enlace cuenta↔ficha de US-006). Mientras no exista el enlace, un jugador no verá partidos. Para probar con el rol Jugador, escribe el uid de su usuario en el campo `usuarioUid` de su ficha en Firestore.
