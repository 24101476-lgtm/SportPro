# Guía de línea gráfica de SportPro

Todas las pantallas deben verse como una sola app. Esta guía resume qué usar en cada caso; los componentes están en `ui/common/EstiloSportPro.kt` y `ui/common/CamposFechaHora.kt`, y el tema en `core/theme/`.

## Colores, tipografía y formas

- **Colores:** siempre `MaterialTheme.colorScheme`, `SportProTema.colores` o `PaletaSportPro` (`core/theme/Color.kt`). Nunca `Color(0x…)` en una pantalla. Los verdes son los de Partido en vivo, que se adoptó como línea de toda la app; `ColoresPartido` ahora delega en la misma paleta.
- **Estados:** usa `ChipEstado(texto, TonoEstado.X)`. Verde (`Éxito`) = confirmado/activo, naranja (`Advertencia`) = pendiente/atención, rojo (`Error`) = rechazado/inactivo, azul (`Información`) = neutral, gris (`Neutro`) = desactivado. Los colores derivan de `PaletaSportPro.Exito`, `Advertencia`, `Error`, `Informacion`, `Neutro` según el modo (claro/oscuro).
- **Tipografía:** `MaterialTheme.typography` (escala completa en `Type.kt`). No fijes `fontSize` a mano.
- **Formas:** radio por jerarquía (todos en `FormasSportPro` via `MaterialTheme.shapes`):
  - `extraSmall` (12 dp): chips de estado, avisos pequeños
  - `small` (16 dp): campos de texto, botones secundarios, tiles
  - `medium` (24 dp): tarjetas (`TarjetaSportPro`)
  - `large` (28 dp): encabezados (`EncabezadoModulo`)
  - `extraLarge` (32 dp): hojas, diálogos
  - Botón principal: `CircleShape` (píldora)

  **Regla:** en pantallas usa siempre `MaterialTheme.shapes.*`, nunca `RoundedCornerShape(n.dp)` directo. Excepción documentada: `CanchaTactica` y `FormacionesTacticas` (dibujos de cancha con hexágonos).

## Estructura de una pantalla

| Caso | Componente | Notas |
|---|---|---|
| Pantalla raíz de un módulo | `EncabezadoModulo(titulo, subtitulo, icono, foto?)` | Arriba del contenido; foto por defecto `FotoSportPro.CANCHA`; fondo con velo musgo |
| Detalle o formulario (con volver) | `Scaffold(topBar = { BarraSuperiorSportPro(...) })` | Nunca `TopAppBar` directo; usa `BarraSuperiorSportPro` que ya tiene colores correctos |
| Elemento de una lista | `TarjetaSportPro { … }` | Con `AvatarIniciales` o `IconoCircular` a la izquierda; sin sombra, profundidad por contraste |
| Elemento clicable | `TarjetaSportPro(onClick = { ... })` | Se encoge 3 % al presionar; no duplicar interacción |
| Título de sección | `TituloSeccion("…")` | Texto fuerte, padding 16 dp horizontal |
| Dato de detalle | `FilaDatoSportPro("Etiqueta", valor)` | Vacío = "Sin información" |
| Cargando | `CargandoSportPro(modifier = Modifier.padding(padding))` | Sustituye `Box(CircularProgressIndicator())` |
| Error con reintento | `ErrorSportPro(mensaje, onReintentar = { ... }, modifier = Modifier.padding(padding))` | Sustituye `Box(Text(error))`; ícono CloudOff |
| Error sin reintento | `ErrorSportPro(mensaje, modifier = ...)` | Si no hay callback `onReintentar`, no muestra botón |
| Lista vacía | `EstadoVacioModulo(titulo, mensaje, icono, accion?)` | Ícono circular, invitación clara; `accion` es una lambda de composable |
| Spinner en botón | `IndicadorCargaBoton()` | Dentro de `Button { if (cargando) IndicadorCargaBoton() else Text(...) }` |
| Error en línea (formulario) | `AvisoErrorSportPro(mensaje)` | Caja errorContainer, ícono Warning; reemplaza `Text(error, color = error)` |
| Estado (chip) | `ChipEstado(texto, TonoEstado.X)` | Píldora sin borde; `X` = `EXITO`, `ADVERTENCIA`, `ERROR`, `INFORMACION`, `NEUTRO` |
| Acción principal | `BotonPrincipalSportPro(texto, onClick, cargando?)` | Píldora lima; una por pantalla; con `cargando = true` muestra spinner |
| Fecha | `CampoFecha(valorIso, onCambio, etiqueta, fechaMinima?, fechaMaxima?)` | Picker de Material 3; guarda ISO 8601 |
| Hora | `CampoHora(valor, onCambio, etiqueta)` | Picker de Material 3 |
| Login / registro | `MarcaSportPro(subtitulo)` | Foto estadio con nombre y trazo lima |

## Patrón estándar de pantalla

Toda pantalla que liste ítems o muestre un detalle debe seguir este patrón para verse uniforme:

### Estructura de raíz + detalle/formulario

1. **Pantalla raíz de módulo** (ej. `PadreMensualidadesScreen`):
   ```
   EncabezadoModulo(titulo, subtitulo, icono) + contenido debajo
   ```

2. **Pantalla de detalle o formulario** (ej. `MensualidadHistorialScreen`):
   ```kotlin
   Scaffold(topBar = { BarraSuperiorSportPro(
       title = { Text("Título") },
       navigationIcon = { IconButton(onClick = navController.popBackStack) { Icon(...) } }
   ) }) { padding ->
       // estados y contenido aquí, con Modifier.padding(padding)
   }
   ```
   **Nunca** uses `TopAppBar` directo; siempre `BarraSuperiorSportPro`.

### Estados obligatorios en orden

Cada pantalla debe manejar estos estados con `when (state.xxx)`:

1. **Carga**: `CargandoSportPro(modifier = Modifier.padding(padding))`
2. **Error**: `ErrorSportPro(mensaje = state.errorMessage, onReintentar = { viewModel.recargar() }, modifier = Modifier.padding(padding))`
   - Si no hay `onReintentar`, omite ese parámetro y no muestra botón.
3. **Vacío**: `EstadoVacioModulo(titulo, mensaje, icono)` si no hay registros.
4. **Contenido**: `LazyColumn { items(...) { … } }`

```kotlin
// Esqueleto típico:
when {
    state.isLoading -> CargandoSportPro(modifier = Modifier.padding(padding))
    state.errorMessage != null -> ErrorSportPro(
        mensaje = state.errorMessage.orEmpty(),
        onReintentar = { viewModel.cargar(...) },
        modifier = Modifier.padding(padding)
    )
    state.registros.isEmpty() -> EstadoVacioModulo(
        titulo = "Sin registros",
        mensaje = "Aquí aparecerán...",
        icono = Icons.Default.EventBusy
    )
    else -> LazyColumn(
        modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(state.registros, key = { it.id }) { registro ->
            TarjetaSportPro(onClick = { navController.navigate(...) }) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    // contenido
                }
            }
        }
    }
}
```

### Ítems y bloques

- **Tarjeta o bloque sin interacción**: `TarjetaSportPro { ... }`
- **Tarjeta o bloque clicable**: `TarjetaSportPro(onClick = { ... }) { ... }`
- **Nunca** uses `Card`, `ElevatedCard` ni `Surface(tonalElevation = ...)`; `TarjetaSportPro` maneja todo.

### Acciones principales

- **Botón principal de la pantalla**: `BotonPrincipalSportPro(texto = "Guardar", onClick = { ... }, cargando = state.enviando)`
  - Solo una por pantalla.
  - Si está enviando: `cargando = true` muestra spinner automáticamente.
- **Spinner en botón secundario**: dentro del `Button` usa `if (cargando) IndicadorCargaBoton() else Text("Texto")`
- **Error en línea** (formulario, envío fallido): `state.errorMessage?.let { AvisoErrorSportPro(it) }`

### Formas y colores

- **Formas**: siempre `MaterialTheme.shapes.*`, nunca `RoundedCornerShape(n.dp)` directo.
- **Colores**: `MaterialTheme.colorScheme.*`, `SportProTema.colores.*`, `PaletaSportPro.*`. Nunca `Color(0x...)`.
- **Estados (chips)**: `ChipEstado(texto, TonoEstado.EXITO)` etc.

### Espaciado

- **Padding horizontal**: 16 dp en toda la pantalla (ver `Modifier.padding(horizontal = 16.dp)` en `LazyColumn`).
- **Entre tarjetas**: `spacedBy(8.dp)` en `LazyColumn`.
- **Dentro de tarjeta**: 16 dp de padding (ver `TarjetaSportPro { Row(Modifier.padding(16.dp)) }`).
- **Entre elementos verticales** (dentro de fila): `spacedBy(8.dp)` a `12.dp` según densidad.

### Accesibilidad

- **Íconos interactivos** (botones, iconos en tarjetas clicables): `contentDescription = "Descripción clara"`.
- **Íconos decorativos** (junto a texto, dentro de `EncabezadoModulo`): `contentDescription = null`.
- **Chips de estado**: no clicables, solo informativos.

## Navegación

La barra inferior tiene **4 pestañas por rol** (`MenuPrincipal` en `navigation/NavRoutes.kt`): Inicio, una sección propia del rol, Partidos y Más. Si agregas un módulo, ubícalo en una sección existente o en "Más"; no agregues pestañas.

| Rol | Pestañas | Dentro de la sección | En "Más" |
|---|---|---|---|
| Administrador | Inicio · Plantel · Partidos · Más | Academia, Jugadores, Entrenamientos, Mensualidades | Estadísticas, Comunidad, Resumen IA |
| Entrenador | Inicio · Plantel · Partidos · Más | Academia, Jugadores, Entrenamientos | Estadísticas, Comunidad, Resumen IA |
| Jugador | Inicio · Entrenos · Partidos · Más | Entrenamientos | Estadísticas, Comunidad |
| Padre de familia | Inicio · Mis hijos · Partidos · Más | Mis hijos, Mensualidades | Estadísticas, Comunidad |

## Estado de estandarización (PR actual)

Pantallas actualizadas en este PR (`git diff 5397693..HEAD`):

| Pantalla | Estado | Notas |
|---|---|---|
| `EstadoMensualidadChip` | Estandarizada | Componente de estado; color vía `TonoEstado` |
| `MensualidadHistorialScreen` | Estandarizada | `CargandoSportPro`, `ErrorSportPro(onReintentar)`, `EstadoVacioModulo`, `TarjetaSportPro` |
| `MensualidadRegistrarScreen` | Estandarizada | Form con estados completos, `BotonPrincipalSportPro`, `AvisoErrorSportPro` |
| `MisHijosScreen` | Estandarizada | Lista con `EncabezadoModulo`, `TarjetaSportPro`, errores con `ErrorSportPro` |
| `ConvocatoriaVencidaScreen` | Estandarizada | `BarraSuperiorSportPro`, `ErrorSportPro` con mensaje |
| `ConvocatoriaRespuestaRegistradaScreen` | Estandarizada | Estados claros, `TarjetaSportPro`, color texto via `MaterialTheme.colorScheme` |
| `AlineacionTableroScreen` | Estandarizada | Estados con `CargandoSportPro`, `BarraSuperiorSportPro` |
| `SeguimientoPartidoScreen` | Estandarizada | Estados complejos, `CargandoSportPro`, `TarjetaSportPro`, excepción: marcador 52 sp (legible en partido en vivo) |
| `JugadoresScreen` | Ya pulida, +`CargandoSportPro` | Carga estandarizada; estados y tarjetas OK |
| `AcademiaScreen` | Ya pulida, +`CargandoSportPro` | Carga estandarizada; estados y tarjetas OK |
| `AnunciosListaScreen` | Ya pulida, +`CargandoSportPro` | Carga estandarizada; estados y tarjetas OK |
| `PadreMensualidadesScreen` | Ya pulida, +`CargandoSportPro` | Carga estandarizada; estados y tarjetas OK |
| `EstadisticasEquipoScreen` | Ya pulida, +`CargandoSportPro` | Carga estandarizada; estados y tarjetas OK |
| `RegistroPartidoScreen` | Pendiente | Carga y error aún son `Box(CircularProgressIndicator)` y `Text` |
| `EventoBottomSheet` | Pendiente | Formulario sin estandarizar |
| `ResumenIaScreen` | Pendiente | Estados sin estandarizar |
| `RevisionResumenComponentes` | Pendiente | Componente sin estandarizar |
| `InicioScreen` | Pendiente | Pantalla principal sin estandarizar |
| `PartidosListaScreen` | Pendiente | Spinner directo |
| `ConvocatoriaNoDisponibleScreen` | Pendiente | Mensaje simple; evaluar si necesita `ErrorSportPro` |
| `BibliotecaEjerciciosScreen` | Pendiente | Spinner directo |
| `RecuperarContrasenaScreen` | Pendiente | Formulario sin estandarizar |
| `MensualidadesGrillaScreen` | Parcial | Carga estandarizada con `CargandoSportPro`; error aún es `Text` suelto |
| `CanchaTactica`, `FormacionesTacticas` | Excepción justificada | Dibujos de cancha; hexágonos con `RoundedCornerShape` legítimos por geometría |

## Catálogo de componentes nuevos (PR actual)

Estos dos componentes debutan en este PR y sustituyen patrones comunes que antes eran manuales:

### `IndicadorCargaBoton`

Spinner pequeño (20 dp) que toma el color del texto del botón. Úsalo cuando envías datos en un botón secundario:

```kotlin
@Composable
fun IndicadorCargaBoton(modifier: Modifier = Modifier)
```

**Ejemplo:**
```kotlin
Button(onClick = { viewModel.enviar() }) {
    if (viewModel.enviando) {
        IndicadorCargaBoton()
    } else {
        Text("Enviar")
    }
}
```

### `AvisoErrorSportPro`

Aviso en línea con ícono Warning, fondo errorContainer y texto onErrorContainer. Para formularios y errores de envío:

```kotlin
@Composable
fun AvisoErrorSportPro(mensaje: String, modifier: Modifier = Modifier)
```

**Ejemplo:**
```kotlin
state.errorMessage?.let { 
    AvisoErrorSportPro(it, modifier = Modifier.padding(16.dp))
}
```

## Fechas

Nunca crees un `SimpleDateFormat` ni pidas fechas escritas a mano. Usa `core/fecha/FechasSportPro` para guardar, validar y mostrar (ver `docs/modelo-datos-gestion.md`, sección "Criterio de fechas").

## Lista de revisión antes de un PR

### Colores, formas y componentes
- [ ] Sin `Color(0x…)` directos en la pantalla; usa `MaterialTheme.colorScheme.*`, `SportProTema.colores.*` o `PaletaSportPro.*`.
- [ ] Sin `RoundedCornerShape(n.dp)` directo en pantallas; usa `MaterialTheme.shapes.*` (excepción: `CanchaTactica`, `FormacionesTacticas`).
- [ ] Sin `Card`, `ElevatedCard` ni `Surface(tonalElevation = ...)` en listas/tarjetas; usa `TarjetaSportPro`.
- [ ] Sin `CircularProgressIndicator()` directo; usa `CargandoSportPro()` para pantalla completa o `IndicadorCargaBoton()` en botón.
- [ ] Sin `TopAppBar` directo; usa `BarraSuperiorSportPro` en `Scaffold(topBar = ...)`.
- [ ] Errores en línea: `AvisoErrorSportPro(mensaje)` en formularios, no `Text(error, color = error)`.

### Estructura y navegación
- [ ] Pantalla raíz de módulo comienza con `EncabezadoModulo(titulo, subtitulo, icono)`.
- [ ] Pantalla de detalle/formulario usa `Scaffold` con `BarraSuperiorSportPro` en `topBar`.
- [ ] Estados (carga → error → vacío → contenido) con `when` usando `CargandoSportPro`, `ErrorSportPro(onReintentar)`, `EstadoVacioModulo`.
- [ ] No hay `Box(CircularProgressIndicator())` ni `Box(Text(...))` para estados.

### Acciones y campos
- [ ] Acción principal: `BotonPrincipalSportPro(texto, onClick, cargando?)`, máximo una por pantalla.
- [ ] Botones secundarios con spinner: `if (cargando) IndicadorCargaBoton() else Text("...")`.
- [ ] Fechas: `CampoFecha`/`CampoHora`, nunca `SimpleDateFormat` ni entrada manual.
- [ ] `ChipEstado(texto, TonoEstado.X)` para estados, no chips clicables.

### Accesibilidad
- [ ] Íconos pulsables tienen `contentDescription = "..."`, decorativos tienen `contentDescription = null`.
- [ ] Chips de estado no son clicables (`semantics { selected = false }`).

### Datos y permisos
- [ ] Rol y academia: leídos de `SesionRepository.perfilActual()`, nunca valores fijos.
- [ ] Ohne hardcoded colors, fonts sizes or layouts; respeta el sistema de temas.
