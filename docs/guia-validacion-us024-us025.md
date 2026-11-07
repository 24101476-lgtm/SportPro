# Guía de validación de US-024 y US-025

Pruebas manuales en un dispositivo para la revisión, aprobación y evaluación del resumen con IA. Parten de que US-021, US-022 y US-023 ya funcionan: hace falta un partido con el **acta cerrada** y un resumen generado (ver `docs/guia-validacion-us021-us022-us023.md`).

## 0. Antes de empezar

1. Traer la rama y compilar: `./gradlew testDebugUnitTest assembleDebug`. Las pruebas nuevas están en `RevisionResumenTest` y `EvaluacionResumenTest`.
2. **Publicar `firestore.rules`** en Firebase Console > Firestore Database > Reglas (cambian las de `resumenes` y aparece `evaluacionesResumen`). Sin eso, aprobar y evaluar dan error de permisos.
3. Cuentas necesarias: un **entrenador** que sea operador, entrenador responsable o creador del partido; un **administrador**; un **jugador** o **padre** de ese equipo; y otro **entrenador** que no tenga relación con el partido.

## 1. US-024: revisar y aprobar

| # | Pasos | Resultado esperado |
|---|---|---|
| 1 | Entrenador: Partidos > partido con acta cerrada > «Resumen con IA» > generar | Aparece la versión en estado **Borrador** con tres campos editables y, junto a ellos (o debajo en pantallas angostas), la cronología de eventos |
| 2 | Con el jugador o el padre, abrir el seguimiento del partido > pestaña **Resumen** | Dice que el entrenador aún no publicó el resumen: el borrador no se ve |
| 3 | Si hay «Dato no verificado», pulsar **Aprobar y publicar** sin tocarlo | Mensaje «Corrige las menciones no verificadas antes de aprobar» y no se abre la confirmación |
| 4 | Corregir o borrar la mención | Desaparece de la lista roja. Una mención resuelta no vuelve |
| 5 | Dejar los hechos vacíos y aprobar | «Escribe al menos un hecho del partido» |
| 6 | Pulsar **Guardar borrador**, salir y volver a entrar | El texto corregido sigue ahí; sigue en Borrador |
| 7 | Pulsar **Aprobar y publicar** | Diálogo con la casilla «Confirmo que revisé el resumen y sus datos corresponden a lo ocurrido en el partido». El botón está apagado hasta marcarla |
| 8 | Marcar la casilla y aprobar (sin comunidad) | Aviso de aprobado, la versión pasa a **Aprobado** y el texto original de la IA sigue en Firestore (`hechos`, `desarrollo`) distinto de `hechosFinal` y `desarrolloFinal` |
| 9 | Jugador o padre: seguimiento > pestaña **Resumen** | Ve el texto final, el marcador y la leyenda «Resumen generado con IA y revisado por el entrenador» |
| 10 | Entrenador: «Editar de nuevo» > confirmar | La versión vuelve a Borrador; el jugador ya no la ve. Al aprobar otra vez reaparece |
| 11 | Repetir la aprobación con el interruptor **Publicar también en la comunidad** | Aparece una publicación abierta en el muro de Comunidad con el marcador y la leyenda. Al «Editar de nuevo», esa publicación desaparece del muro |
| 12 | Otro entrenador sin relación con el partido abre el resumen | No puede aprobar: no ve el editor |

En Firestore, `partidos/{id}/resumenes/{resumenId}` debe tener `aprobadoPor` con el uid del entrenador, `aprobadoEn` con fecha y `confirmacionAprobacion: true`.

## 2. US-025: evaluar

| # | Pasos | Resultado esperado |
|---|---|---|
| 1 | Al aprobar, en el diálogo elegir **Más tarde** | Se cierra. En la pantalla del partido (registro) aparece «Evaluación del resumen pendiente» |
| 2 | Pulsar «Evaluar el resumen» | Se abre el formulario con las correcciones ya escritas (por ejemplo «Desarrollo, se corrigió: …») |
| 3 | Guardar con campos vacíos, utilidad sin elegir y un comentario de 10 caracteres | Mensajes de error en cada campo. El comentario pide entre 20 y 500 caracteres |
| 4 | Completar y guardar | «Evaluación guardada». El aviso de pendiente desaparece |
| 5 | Módulo **IA** (entrenador o administrador) | Pantalla Evaluaciones con el total, el promedio de utilidad y las sumas de omisiones, errores e invenciones |
| 6 | «Exportar como texto» | Selector de Android con el texto del consolidado (`EVALUACIÓN DEL RESUMEN NARRATIVO CON IA - SportPro`, totales y detalle por partido) |
| 7 | Administrador: módulo IA | Ve las evaluaciones de toda la academia; el entrenador, solo las de los resúmenes que aprobó |
| 8 | Otro entrenador | No ve esas evaluaciones y no puede registrarlas |

## 3. Qué enviarle a Yoel

La tabla con cada fila en OK o fallo, capturas del paso 3 de la sección 1 y del consolidado, y el texto exportado. Si algo falla con «permisos», confirma primero que las reglas están publicadas.
