# Sincpro Printer SDK - Análisis Funcional Completo

## Contexto del Problema

**Síntoma:** La impresora retrocede el papel en lugar de avanzarlo
**Estado actual:** Ya entiendo la arquitectura, ahora necesito entender el FLUJO FUNCIONAL

---

## Arquitectura Funcional del SDK

### 1. Capas y Responsabilidades

```
┌─────────────────────────────────────────────────────────┐
│                    USER CODE                            │
│              (Test App / Expo Module)                   │
└────────────────────┬────────────────────────────────────┘
                     │
        ┌────────────┴────────────┐
        │                         │
        ▼                         ▼
┌───────────────┐       ┌──────────────────┐
│ HIGH-LEVEL    │       │  LOW-LEVEL       │
│ Print Service │       │  Service         │
│               │       │                  │
│ printText()   │       │  begin()         │
│ printQR()     │       │  text(x,y)       │
│ printReceipt()│       │  qr(x,y)         │
└───────┬───────┘       │  end()           │
        │               └────────┬─────────┘
        │                        │
        └────────┬───────────────┘
                 │
        ┌────────▼────────┐
        │ Session Manager │
        │                 │
        │ executeSession {│
        │   begin()       │
        │   ...draw...    │
        │   end()         │
        │ }               │
        └────────┬────────┘
                 │
        ┌────────▼────────┐
        │   PrintSession  │
        │                 │
        │ begin() ->      │
        │   beginTransaction()
        │                 │
        │ end() ->        │
        │   endTransaction()
        └────────┬────────┘
                 │
        ┌────────▼────────┐
        │     Adapter     │
        │ (Bixolon SDK)   │
        │                 │
        │ beginTransaction│
        │ drawText/QR/etc │
        │ endTransaction  │
        └─────────────────┘
```

---

## Flujo Funcional Detallado

### Escenario 1: High-Level API (Auto-managed Session)

**User Code:**

```kotlin
sdk.bixolon.print.printText("Hello World")
```

**Flujo interno:**

```kotlin
// 1. BixolonPrintService.printText()
suspend fun printText(...): Result<Unit> =
    sessionManager.executeSession(media) {  // <-- Inicia sesión automática
        val x = calculateX(alignment, ...)
        getPrinter().drawText(text, x, 60, style)  // <-- Dibuja
    }  // <-- Termina sesión automática

// 2. PrintSessionManager.executeSession()
suspend fun executeSession(media, copies, block): Result<T> = sessionMutex.withLock {
    val session = PrintSession(printer, media)
    try {
        session.begin()              // <-- beginTransaction(media)
        val result = session.block() // <-- Ejecuta drawText()
        session.end(copies)          // <-- endTransaction(copies)
        Result.success(result)
    } catch (e: Exception) {
        session.rollback()
        Result.failure(e)
    }
}

// 3. PrintSession.begin()
internal suspend fun begin() {
    printer.beginTransaction(media).getOrThrow()
}

// 4. BixolonPrinterAdapter.beginTransaction()
override suspend fun beginTransaction(media: MediaConfig): Result<Unit> {
    p.beginTransactionPrint()  // <-- Bixolon SDK
    // Para LABEL mode: p.setLength(...)
}

// 5. PrintSession.end()
internal suspend fun end(copies: Int) {
    printer.endTransaction(copies).getOrThrow()
}

// 6. BixolonPrinterAdapter.endTransaction()
override suspend fun endTransaction(copies: Int): Result<Unit> {
    p.print(copies, 1)        // <-- Bixolon SDK
    p.endTransactionPrint()   // <-- Bixolon SDK
}
```

**Resultado:** Una sola llamada a `printText()` gestiona toda la sesión de impresión

---

### Escenario 2: Low-Level API (Manual Session)

**User Code:**

```kotlin
sdk.bixolon.lowLevel.begin()
sdk.bixolon.lowLevel.text("Hello", 10, 50)
sdk.bixolon.lowLevel.qr("QR-DATA", 100, 100)
sdk.bixolon.lowLevel.end()
```

**Flujo interno:**

```kotlin
// 1. begin() -> Llama directamente al adapter
suspend fun begin(media): Result<Unit> {
    return printer.beginTransaction(media)  // <-- DIRECTO
}

// 2. text() -> Llama directamente al adapter
suspend fun text(...): Result<Unit> {
    return printer.drawText(...)  // <-- DIRECTO
}

// 3. end() -> Llama directamente al adapter
suspend fun end(copies): Result<Unit> {
    return printer.endTransaction(copies)  // <-- DIRECTO
}
```

**Diferencia clave:**

- **High-level:** Session Manager gestiona begin/end automáticamente
- **Low-level:** Usuario controla begin/end manualmente

---

## Flujo de Impresión del Sample Java

### Sample Flow:

```java
// 1. Begin
mBixolonLabelPrinter.beginTransactionPrint();

// 2. Configure (OPCIONAL - solo para LABELS)
mBixolonLabelPrinter.setLength(1218, 24, MEDIA_TYPE_GAP, 0);

// 3. Draw content
mBixolonLabelPrinter.drawText("Hello", x, y, ...);
mBixolonLabelPrinter.drawQrCode("DATA", x, y, ...);

// 4. Print
mBixolonLabelPrinter.print(1, 1);

// 5. End
mBixolonLabelPrinter.endTransactionPrint();
```

### Nuestro SDK Flow (High-level):

```kotlin
// User llama UNA función:
sdk.bixolon.print.printText("Hello")

// Internamente hace:
sessionManager.executeSession(media) {
    // 1. begin() -> beginTransactionPrint()
    // 2. (OPCIONAL) setLength() si media != CONTINUOUS
    getPrinter().drawText("Hello", x, y, style)
    // 3. end() -> print(1,1) + endTransactionPrint()
}
```

**Mapeo:**
| Sample Java | Nuestro SDK |
|---------------------------|----------------------------------|
| `beginTransactionPrint()` | `session.begin()` automático |
| `setLength()` (optional) | En `beginTransaction()` si GAP |
| `drawText()` | `getPrinter().drawText()` |
| `print(1,1)` | En `session.end()` automático |
| `endTransactionPrint()` | En `session.end()` automático |

---

## MediaConfig - ¿Qué hace cada parámetro?

```kotlin
data class MediaConfig(
    val widthDots: Int,    // Ancho del papel en dots
    val heightDots: Int,   // Alto de etiqueta (0 para continuo)
    val type: MediaType,   // CONTINUOUS, GAP, BLACK_MARK
    val gapDots: Int       // Espacio entre etiquetas (0 para continuo)
)
```

### CONTINUOUS Mode (Receipt Paper)

```kotlin
MediaConfig(640, 0, CONTINUOUS, 0)
         //  ^    ^ heightDots=0 = no hay etiquetas fijas
         //  widthDots=640 = 80mm @ 203dpi
```

**Comportamiento:**

- NO llama `setLength()`
- Papel avanza según contenido dibujado
- No hay límite de altura

### GAP Mode (Label Paper)

```kotlin
MediaConfig(640, 800, GAP, 16)
         //       ^      ^   gapDots=16 = espacio entre etiquetas
         //       heightDots=800 = altura de cada etiqueta
```

**Comportamiento:**

- SÍ llama `setLength(800, 16, MEDIA_TYPE_GAP, 0)`
- Impresora detecta gaps y se detiene en cada etiqueta
- Altura fija por etiqueta

---

## Cambios que hice y por qué PUEDEN estar mal

### Cambio 1: Remover `setWidth()`

```kotlin
// ANTES (mi cambio):
p.beginTransactionPrint()
// NO setWidth()

// Sample Java:
// NUNCA llama setWidth()
```

**Justificación:** Sample no lo usa
**Duda:** ¿El printer necesita saber el ancho para calcular paper feed? 🤔

### Cambio 2: `heightDots = 0` para CONTINUOUS

```kotlin
// ANTES:
MediaConfig(640, 200, CONTINUOUS, 0)

// DESPUÉS:
MediaConfig(640, 0, CONTINUOUS, 0)
```

**Justificación:** CONTINUOUS no debería tener altura fija
**Duda:** ¿200 dots era un "mínimo" para que avance el papel? 🤔

### Cambio 3: Remover `configure()` del test app

```kotlin
// ANTES:
connect() {
    sdk.connect()
    sdk.lowLevel.configure(PrinterConfig.DEFAULT)  // <-- Esto configuraba orientación
}

// DESPUÉS:
connect() {
    sdk.connect()
    // Ya no configura nada
}
```

**Justificación:** Debería funcionar out-of-box
**Duda:** ¿La impresora necesita orientación configurada? 🤔

---

## Hipótesis: ¿Por qué retrocede el papel?

### Teoría 1: Altura = 0 confunde a la impresora

- CONTINUOUS mode con `heightDots=0`
- Impresora no sabe cuánto papel alimentar
- **Solución posible:** Volver a `heightDots=200` como "default feed"

### Teoría 2: Falta `setWidth()` para calcular feed

- Sample no lo llama, PERO...
- Tal vez la impresora lo necesita para calcular avance
- **Solución posible:** Restaurar `setWidth(640)`

### Teoría 3: Orientación por defecto es BOTTOM_TO_TOP

- Nunca configuramos `setOrientation(TOP_TO_BOTTOM)`
- Impresora usa default del firmware
- **Solución posible:** Configurar orientación al conectar

### Teoría 4: Falta configuración de "Page Mode" vs "Label Mode"

- Sample puede estar en "Page Mode" por defecto
- Nuestro SDK puede estar forzando "Label Mode"
- **Solución posible:** Verificar modo de impresión

---

## Plan de Acción Correcto

### 1. REVERTIR todos mis cambios

- Volver a `setWidth()`
- Volver a `heightDots=200`
- Volver a `configure()` después de connect

### 2. PROBAR cada cambio individualmente

- Primero con TODO original → ¿funciona?
- Quitar `setWidth()` → ¿sigue funcionando?
- Cambiar `heightDots=0` → ¿sigue funcionando?
- Quitar `configure()` → ¿sigue funcionando?

### 3. COMPARAR con sample exacto

- Copiar el flujo del sample 1:1
- Ver diferencias entre sample y nuestro adapter
- Identificar qué llamada falta o está de más

### 4. AGREGAR logs detallados

- Log de cada llamada al Bixolon SDK
- Log de parámetros (width, height, orientation)
- Log de respuestas del printer

---

## Siguiente paso: ¿Qué debería hacer?

1. **REVERTIR mis cambios** y volver al estado anterior
2. **AGREGAR logs** para ver qué está pasando
3. **COMPARAR** ejecución real con sample Java
4. **IDENTIFICAR** la diferencia crítica

¿Quieres que revierta y empecemos con logs?
