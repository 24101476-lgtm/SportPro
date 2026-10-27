# Guía de línea gráfica de SportPro

Todas las pantallas deben verse como una sola app. Esta guía resume qué usar en cada caso; los componentes están en `ui/common/EstiloSportPro.kt` y `ui/common/CamposFechaHora.kt`, y el tema en `core/theme/`.

## Colores, tipografía y formas

- **Colores:** siempre `MaterialTheme.colorScheme` o `PaletaSportPro` (`core/theme/Color.kt`). Nunca `Color(0x…)` en una pantalla. Los verdes son los de Partido en vivo, que se adoptó como línea de toda la app; `ColoresPartido` ahora delega en la misma paleta.
- **Estados:** usa `ChipEstado(texto, TonoEstado.X)`. Verde = éxito, naranja = advertencia/pendiente, rojo = error/inactivo, azul = información, gris = neutro.
- **Tipografía:** `MaterialTheme.typography` (escala completa en `Type.kt`). No fijes `fontSize` a mano.
- **Formas:** tarjetas 20 dp, encabezados 24 dp, diálogos 28 dp (`FormasSportPro`).

## Estructura de una pantalla

| Caso | Componente |
|---|---|
| Pantalla raíz de un módulo | `EncabezadoModulo(titulo, subtitulo, icono)` arriba del contenido |
| Detalle o formulario (con volver) | `BarraSuperiorSportPro` (mismos parámetros que `TopAppBar`) |
| Elemento de una lista | `TarjetaSportPro { … }`, con `AvatarIniciales` o `IconoCircular` a la izquierda |
| Título de sección | `TituloSeccion("…")` |
| Dato de detalle | `FilaDatoSportPro("Etiqueta", valor)` (vacío = "Sin información") |
| Cargando | `CargandoSportPro()` |
| Error | `ErrorSportPro(mensaje, onReintentar = …)` |
| Lista vacía | `EstadoVacioModulo(titulo, mensaje, icono)` |
| Fecha | `CampoFecha(valorIso, onCambio, etiqueta, fechaMinima, fechaMaxima)` |
| Hora | `CampoHora(valor, onCambio, etiqueta)` |
| Login / registro | `MarcaSportPro(subtitulo)` |

## Navegación

La barra inferior tiene **4 pestañas por rol** (`MenuPrincipal` en `navigation/NavRoutes.kt`): Inicio, una sección propia del rol, Partidos y Más. Si agregas un módulo, ubícalo en una sección existente o en "Más"; no agregues pestañas.

| Rol | Pestañas | Dentro de la sección | En "Más" |
|---|---|---|---|
| Administrador | Inicio · Plantel · Partidos · Más | Academia, Jugadores, Entrenamientos, Mensualidades | Estadísticas, Comunidad, Resumen IA |
| Entrenador | Inicio · Plantel · Partidos · Más | Academia, Jugadores, Entrenamientos | Estadísticas, Comunidad, Resumen IA |
| Jugador | Inicio · Entrenos · Partidos · Más | Entrenamientos | Estadísticas, Comunidad |
| Padre de familia | Inicio · Mis hijos · Partidos · Más | Mis hijos, Mensualidades | Estadísticas, Comunidad |

## Fechas

Nunca crees un `SimpleDateFormat` ni pidas fechas escritas a mano. Usa `core/fecha/FechasSportPro` para guardar, validar y mostrar (ver `docs/modelo-datos-gestion.md`, sección "Criterio de fechas").

## Lista de revisión antes de un PR

- [ ] Sin `Color(0x…)`, `fontSize` fijos ni `SimpleDateFormat` en la pantalla.
- [ ] Raíz del módulo con `EncabezadoModulo`; detalle y formulario con `BarraSuperiorSportPro`.
- [ ] Estados de carga, error y vacío con los componentes comunes.
- [ ] Fechas con `CampoFecha`/`CampoHora` y mostradas con `FechasSportPro`.
- [ ] Rol y academia leídos de `SesionRepository.perfilActual()`, nunca valores fijos.
