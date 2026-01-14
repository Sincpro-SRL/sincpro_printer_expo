# Análisis Caso por Caso - Sincpro Printer SDK

## 1. Para text, qr, barcode - ¿Se utiliza solo una forma de configuración?

### Situación Actual:

**Hay DOS niveles de configuración:**

#### A. Configuración GLOBAL de la impresora (`PrinterConfig`)

```kotlin
// Se setea UNA VEZ después de conectar
printer.configure(PrinterConfig(
    marginLeft = 0,
    marginTop = 0,
    density = Density.MEDIUM,     // Oscuridad de impresión
    speed = Speed.FAST,            // Velocidad IPS
    orientation = Orientation.TOP_TO_BOTTOM,  // Dirección del papel
    autoCutter = CutterConfig.DISABLED
))
```

**Afecta:** TODAS las operaciones de impresión posteriores
**Cuándo se aplica:** ConnectivityService la llama AUTOMÁTICAMENTE después de `connect()`
**Código:**

```kotlin
// BixolonConnectivityService.kt línea 15-17
suspend fun connectBluetooth(address: String, config: PrinterConfig = PrinterConfig.DEFAULT_80MM_RECEIPT)
    = printer.connect(ConnectionConfig.bluetooth(address))
        .onSuccess { printer.configure(config) }  // <-- AUTO-CONFIGURE
```

#### B. Configuración PER-SESSION (`MediaConfig`)

```kotlin
// Se setea EN CADA printText/printQR/printBarcode
printText("Hello", media = MediaConfig.continuous80mm())
```

**Afecta:** SOLO esa sesión de impresión
**Parámetros:**

- `widthDots`: Ancho del papel (640 = 80mm, 464 = 58mm)
- `heightDots`: Alto de etiqueta (0 = continuo)
- `type`: CONTINUOUS, GAP, BLACK_MARK
- `gapDots`: Espacio entre etiquetas

**Código:**

```kotlin
// BixolonPrintService.kt
suspend fun printText(text: String, media: MediaConfig = MediaConfig.continuous80mm())
    = sessionManager.executeSession(media) {  // <-- Media config PER call
        getPrinter().drawText(...)
    }
```

### Respuesta:

❌ **NO**, hay DOS configuraciones:

1. **Global** (PrinterConfig): Orientación, densidad, velocidad → Se setea al conectar
2. **Per-session** (MediaConfig): Tipo de papel → Se setea en cada impresión

---

## 2. ¿La configuración es por cada evento? ¿Cómo funciona actualmente?

### Flujo Actual:

```
┌──────────────────────────────────────────────────────────┐
│ 1. USER: sdk.bixolon.connectivity.connectBluetooth()    │
└───────────────────────┬──────────────────────────────────┘
                        │
            ┌───────────▼─────────────┐
            │ 2. ConnectivityService  │
            │    .onSuccess {         │
            │      configure(config)  │ <-- AUTO CONFIG GLOBAL
            │    }                    │
            └───────────┬─────────────┘
                        │
            ┌───────────▼─────────────┐
            │ 3. Printer configurado  │
            │    con defaults:        │
            │    - Orientation: TOP   │
            │    - Density: MEDIUM    │
            │    - Speed: FAST        │
            └─────────────────────────┘

┌──────────────────────────────────────────────────────────┐
│ 4. USER: sdk.bixolon.print.printText("Hello")           │
└───────────────────────┬──────────────────────────────────┘
                        │
            ┌───────────▼─────────────────┐
            │ 5. PrintService             │
            │    executeSession(media) {  │ <-- Media PER SESSION
            │      begin(media)           │
            │      drawText(...)          │
            │      end()                  │
            │    }                        │
            └─────────────────────────────┘

            begin(media) →
                p.beginTransactionPrint()
                p.setWidth(media.widthDots)      // <-- CADA VEZ
                if (media != CONTINUOUS) {
                    p.setLength(...)              // <-- CADA VEZ (si es label)
                }
```

### Respuesta:

✅ **SÍ**, hay dos tipos:

- **Global config**: Se setea UNA VEZ al conectar (orientation, density, speed)
- **Media config**: Se setea EN CADA sesión de impresión (width, height, type)

**Actualmente:**

- `setWidth()` se llama en CADA `beginTransaction()`
- `setLength()` se llama en CADA `beginTransaction()` SI es label mode
- `setOrientation()` se llama UNA VEZ en `configure()` después de connect

---

## 3. ¿Se puede setear la configuración o restablecer a los tipos permitidos?

### API Actual:

#### Configurar manualmente:

```kotlin
// Opción 1: Via Low-Level Service
sdk.bixolon.lowLevel.configure(PrinterConfig(
    density = Density.DARK,
    speed = Speed.SLOW,
    orientation = Orientation.TOP_TO_BOTTOM
))

// Opción 2: Pasar config al conectar
sdk.bixolon.connectivity.connectBluetooth(
    address = "00:11:22:33:44:55",
    config = PrinterConfig.HIGH_QUALITY  // <-- Custom config
)
```

#### Configs predefinidos:

```kotlin
PrinterConfig.DEFAULT_80MM_RECEIPT  // Density: MEDIUM, Speed: FAST
PrinterConfig.DEFAULT_58MM_RECEIPT  // Density: MEDIUM, Speed: FAST
PrinterConfig.HIGH_QUALITY          // Density: DARK, Speed: SLOW
```

### Respuesta:

✅ **SÍ**, hay 3 formas:

1. **Auto-default:** Al conectar sin parámetros → `PrinterConfig.DEFAULT_80MM_RECEIPT`
2. **Predefinido:** Al conectar con config → `connectBluetooth(addr, PrinterConfig.HIGH_QUALITY)`
3. **Manual:** Llamar `lowLevel.configure()` en cualquier momento

**Problema actual:** Si el usuario conecta sin pasar config, usa defaults. NO hay forma de "reset" a factory defaults de la impresora.

---

## 4. ¿Siempre que se instala este SDK debe utilizar/cargar la configuración en la impresora?

### Comportamiento Actual:

```kotlin
// ConnectivityService.kt
suspend fun connectBluetooth(
    address: String,
    config: PrinterConfig = PrinterConfig.DEFAULT_80MM_RECEIPT  // <-- DEFAULT
) = printer.connect(ConnectionConfig.bluetooth(address))
    .onSuccess { printer.configure(config) }  // <-- SIEMPRE se ejecuta
```

**Resultado:**

- ✅ Al conectar, SIEMPRE se configura la impresora
- ✅ Si no se pasa config, usa `PrinterConfig.DEFAULT_80MM_RECEIPT`
- ❌ NO respeta la configuración que tenía la impresora previamente

### Respuesta:

✅ **SÍ**, actualmente SIEMPRE configura al conectar.

**Problema:** Sobrescribe la config anterior de la impresora. Si otro app configuró algo, se pierde.

**Solución posible:**

```kotlin
// Opción A: Agregar flag skipConfigure
connectBluetooth(address, skipConfigure = true)

// Opción B: Pasar null para no configurar
connectBluetooth(address, config = null)

// Opción C: Leer config actual primero, luego aplicar
getConfig() → merge con nuevos valores → configure()
```

---

## 5. ¿Los casos de uso están obedeciendo todo?

### Caso de Uso 1: High-Level Print (printText, printQR, printBarcode)

**Flujo:**

```kotlin
printText("Hello", media = MediaConfig.continuous80mm())
  → executeSession(media) {
      begin(media)        → setWidth() + setLength()?
      drawText(...)       → p.drawText()
      end()               → p.print() + p.endTransactionPrint()
    }
```

**¿Obedece?**

- ✅ Usa `withContext(Dispatchers.IO)` correctamente
- ✅ Session Manager con Mutex para evitar concurrencia
- ✅ `setWidth()` se llama en cada transacción
- ⚠️ `setLength()` se llama SOLO para GAP/BLACK_MARK (correcto según sample)
- ❌ **PROBLEMA:** Y-coordinate empieza en 20, pero debería ser 60+ para evitar non-printable zone

### Caso de Uso 2: Low-Level API (begin/text/qr/end manual)

**Flujo:**

```kotlin
lowLevel.begin(media)
lowLevel.text("Hello", 10, 50)
lowLevel.qr("DATA", 100, 100)
lowLevel.end()
```

**¿Obedece?**

- ✅ Control manual completo
- ✅ No usa Session Manager (correcto)
- ✅ Usuario controla begin/end
- ❌ **PROBLEMA:** Usuario puede olvidar llamar `end()`

### Caso de Uso 3: Receipt Printing

**Flujo:**

```kotlin
printReceipt(receipt, media)
  → executeSession(media) {
      for (line in receipt.lines) {
        renderLine(line, y, width)
      }
    }
```

**¿Obedece?**

- ✅ Usa Session Manager
- ✅ Y-coordinate empieza en 60 (correcto)
- ✅ Ajusta Y automáticamente
- ⚠️ Usa receipt.widthDots en lugar de media.widthDots en algunos casos

### Respuesta General:

⚠️ **CASI**, pero hay issues:

**Issues encontrados:**

1. ❌ Y-coordinate en algunos métodos empieza en 20 (non-printable zone)
2. ❌ Auto-configure sobrescribe config anterior sin preguntar
3. ⚠️ `heightDots=0` para CONTINUOUS puede confundir (aunque es correcto)
4. ⚠️ Sample Java NO llama `setWidth()`, pero nosotros sí (puede ser correcto o no)

---

## Recomendaciones de Fixes

### Fix 1: Y-coordinate mínimo

```kotlin
// BixolonPrintService.kt - CAMBIAR todas las Y=20 a Y=60
private const val MIN_PRINTABLE_Y = 60

suspend fun printText(...) = sessionManager.executeSession(media) {
    val x = calculateX(alignment, getMedia().widthDots, text.length * 10)
    getPrinter().drawText(text, x, MIN_PRINTABLE_Y, style)  // <-- 60 instead of 20
}
```

### Fix 2: Configure opcional

```kotlin
// ConnectivityService.kt
suspend fun connectBluetooth(
    address: String,
    config: PrinterConfig? = PrinterConfig.DEFAULT_80MM_RECEIPT  // <-- Nullable
) = printer.connect(ConnectionConfig.bluetooth(address))
    .onSuccess {
        config?.let { printer.configure(it) }  // <-- Solo si no es null
    }
```

### Fix 3: Logs para debugging

```kotlin
override suspend fun beginTransaction(media: MediaConfig): Result<Unit> = withContext(Dispatchers.IO) {
    try {
        val p = printer ?: return@withContext Result.failure(Exception("Not connected"))
        Log.d("BixolonAdapter", "beginTransaction: media=${media}")
        p.beginTransactionPrint()
        p.setWidth(media.widthDots)
        Log.d("BixolonAdapter", "setWidth: ${media.widthDots}")
        // ...
    }
}
```

---

## Conclusión

| Aspecto                | Estado                   | Acción                   |
| ---------------------- | ------------------------ | ------------------------ |
| 1. Configuración única | ❌ NO, hay 2 niveles     | ✅ Documentar mejor      |
| 2. Config por evento   | ✅ Sí, media per-session | ✅ OK                    |
| 3. Setear/restablecer  | ⚠️ Parcial               | ➕ Agregar `config=null` |
| 4. Auto-configure      | ✅ Sí, siempre           | ⚠️ Puede ser problema    |
| 5. Casos de uso        | ⚠️ Casi todos            | ➕ Fix Y-coordinate      |

**Prioridad de fixes:**

1. 🔴 **CRÍTICO:** Fix Y=20 → Y=60 en todos los print methods
2. 🟡 **MEDIO:** Hacer `config` nullable en `connect()`
3. 🟢 **OPCIONAL:** Agregar logs para debugging
