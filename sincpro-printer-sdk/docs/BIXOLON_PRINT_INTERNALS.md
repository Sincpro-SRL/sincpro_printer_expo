# Bixolon Print Internals

Documentación técnica sobre el funcionamiento interno del SDK de impresión Bixolon.

---

## 1. Transacciones

### ¿Qué es una transacción?

Una transacción agrupa múltiples comandos de impresión en un solo bloque, optimizando la comunicación con la impresora.

```
beginTransactionPrint()  ─┐
    setWidth(...)         │  Todos estos comandos
    setLength(...)        │  se envían como un solo
    drawText(...)         │  paquete de datos
    drawQR(...)           │
    print(...)            │
endTransactionPrint()   ─┘
```

### Beneficios

| Sin transacción                         | Con transacción              |
| --------------------------------------- | ---------------------------- |
| Cada comando = 1 envío                  | Todos los comandos = 1 envío |
| Latencia alta (especialmente Bluetooth) | Latencia mínima              |
| Mayor consumo de batería                | Menor consumo                |

### Flujo de transacción

```
┌─────────────────────────────────────────┐
│  beginTransactionPrint()                │
│  ┌───────────────────────────────────┐  │
│  │  Comandos acumulados en memoria   │  │
│  │  - setWidth()                     │  │
│  │  - setLength()                    │  │
│  │  - drawText(), drawQR(), etc.     │  │
│  │  - print()                        │  │
│  └───────────────────────────────────┘  │
│  endTransactionPrint()  → Envío único   │
└─────────────────────────────────────────┘
```

---

## 2. Buffer de Impresión

### ¿Qué es el buffer?

El buffer es un área de memoria en la impresora donde se "dibuja" el contenido antes de imprimirlo físicamente.

```
              BUFFER (Memoria)
┌────────────────────────────────────┐
│  setWidth(640)   → Ancho: 640 dots │
│  setLength(800)  → Alto: 800 dots  │
│                                    │
│  ┌──────────────────────────────┐  │
│  │ "FACTURA #001"    (y=20)     │  │
│  │ "Cliente: Juan"   (y=50)     │  │
│  │ "Producto A"      (y=80)     │  │
│  │ "Total: $100"     (y=110)    │  │
│  │ [QR CODE]         (y=150)    │  │
│  │ "Gracias!"        (y=300)    │  │
│  └──────────────────────────────┘  │
│                                    │
│  print() → Envía buffer al cabezal │
└────────────────────────────────────┘
```

### Problema: Buffer con altura insuficiente

Si `setLength()` no se llama o tiene un valor pequeño, solo parte del contenido cabe en el buffer:

```
Buffer con altura = 100 dots (insuficiente)
┌──────────────────────────────────┐
│ "FACTURA #001"    (y=20)   ✓    │
│ "Cliente: Juan"   (y=50)   ✓    │
│ "Producto A"      (y=80)   ✓    │
├──────────────────────────────────┤  ← Límite del buffer
│ "Total: $100"     (y=110)  ✗    │  ← Queda pendiente
│ [QR CODE]         (y=150)  ✗    │
│ "Gracias!"        (y=300)  ✗    │
└──────────────────────────────────┘

Resultado:
- Impresión 1: Solo header (incompleto)
- Impresión 2: Sale el resto + nuevo contenido (solapado)
```

### Solución: Calcular altura dinámica

```kotlin
val contentHeight = calculateContentHeight(elements)
p.setLength(contentHeight + 100, 0, MEDIA_TYPE_CONTINUOUS, 0)
```

### clearBuffer()

Limpia el buffer antes de cada impresión para evitar residuos:

```kotlin
p.clearBuffer()          // Elimina datos anteriores
p.beginTransactionPrint()
// ... comandos ...
p.endTransactionPrint()
```

---

## 3. Tipos de Media

### Constantes del SDK

| Tipo                    | Valor | Uso                                |
| ----------------------- | ----- | ---------------------------------- |
| `MEDIA_TYPE_CONTINUOUS` | 67    | Papel continuo (rollos sin marcas) |
| `MEDIA_TYPE_GAP`        | 71    | Etiquetas con gap/separación       |
| `MEDIA_TYPE_BLACK_MARK` | 66    | Papel con marca negra              |

### Configuración por tipo

```kotlin
// Papel continuo (recibos)
setLength(
    heightDots,    // Altura calculada del contenido
    0,             // Sin gap
    67,            // MEDIA_TYPE_CONTINUOUS
    0              // Offset
)

// Etiquetas con gap
setLength(
    heightDots,    // Altura de la etiqueta
    gapDots,       // Altura del gap entre etiquetas
    71,            // MEDIA_TYPE_GAP
    0
)

// Papel con marca negra
setLength(
    heightDots,    // Altura hasta la marca
    markDots,      // Altura de la marca negra
    66,            // MEDIA_TYPE_BLACK_MARK
    0
)
```

### Diagrama de tipos

```
CONTINUOUS (Rollo continuo)     GAP (Etiquetas)         BLACK_MARK
┌─────────────────────┐         ┌─────────────┐         ┌─────────────┐
│                     │         │  Etiqueta   │         │  Contenido  │
│     Contenido       │         │             │         │             │
│                     │         ├─────────────┤         │             │
│     Contenido       │         │    Gap      │         ├█████████████┤ ← Marca
│                     │         ├─────────────┤         │  Contenido  │
│     Contenido       │         │  Etiqueta   │         │             │
│                     │         │             │         ├█████████████┤
└─────────────────────┘         └─────────────┘         └─────────────┘
```

---

## 4. Flujo Completo de Impresión

```kotlin
suspend fun print(elements: List<PrintElement>, media: MediaConfig): Result<Unit> {
    // 1. Limpiar buffer (evita residuos de impresiones anteriores)
    printer.clearBuffer()

    // 2. Iniciar transacción (optimiza comunicación)
    printer.beginTransactionPrint()

    // 3. Configurar dimensiones
    printer.setWidth(media.widthDots)
    val contentHeight = calculateContentHeight(elements)
    printer.setLength(contentHeight + 100, 0, MEDIA_TYPE_CONTINUOUS, 0)

    // 4. Renderizar contenido al buffer
    elements.forEach { element ->
        when (element) {
            is Text -> printer.drawText(...)
            is QR -> printer.drawQrCode(...)
            is Barcode -> printer.draw1dBarcode(...)
            is Image -> printer.drawBitmap(...)
        }
    }

    // 5. Ejecutar impresión física
    printer.print(copies = 1, labelSet = 1)

    // 6. Finalizar transacción
    printer.endTransactionPrint()
}
```

---

## 5. Resumen de Métodos Clave

| Método                                 | Propósito                               |
| -------------------------------------- | --------------------------------------- |
| `clearBuffer()`                        | Limpia memoria antes de nueva impresión |
| `beginTransactionPrint()`              | Inicia bloque de comandos agrupados     |
| `endTransactionPrint()`                | Envía bloque completo a la impresora    |
| `setWidth(dots)`                       | Define ancho del área de impresión      |
| `setLength(height, gap, type, offset)` | Define altura y tipo de media           |
| `print(copies, labelSet)`              | Ejecuta impresión física                |
| `drawText(...)`                        | Agrega texto al buffer                  |
| `drawQrCode(...)`                      | Agrega código QR al buffer              |
| `draw1dBarcode(...)`                   | Agrega código de barras al buffer       |
| `drawBitmap(...)`                      | Agrega imagen al buffer                 |
