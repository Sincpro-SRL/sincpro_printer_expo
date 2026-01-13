# Bixolon Label Printer SDK (Android) – API de alto nivel + low-level (Guía para IA)

> **Fuente principal:** _Manual_Label_Printer_SDK_FOR_Android_API_Reference_Guide_ENG_V1.27.pdf_ (SDK BIXOLON Label).
>
> Esta guía define una **API propuesta** (wrapper Expo/JS o similar) y su **contrato nativo**. Está orientada a imprimir **comprobantes/facturas con QR** donde **todo termina siendo imagen o PDF**.

---

## 0) Modelo mental del SDK (imprescindible)

- El SDK trabaja con un **image buffer**: primero construyes (draw/put), **recién imprime** al llamar `print(...)`.
- Cada “job” debe **limpiar** el buffer antes de crear una nueva etiqueta/comprobante.
- La configuración de medio (ancho/largo/gap/tipo/offset) gobierna el **avance de papel**. Si está mal, salen “espacios” (feed) o saltos.

---

## 1) API pública recomendada (alto nivel)

### 1.1 Conexión y sesión

#### `connect(options)`

**Objetivo:** Conectarse a la impresora (BT Classic/BLE/USB según soporte).

**Debe hacer (nativo):**

1. Validar dirección/interfaz.
2. Conectar con timeout.
3. Emitir evento `onConnected`.
4. Preparar estado interno para cola de impresión.

**Errores típicos:**

- Conectar en el hilo UI sin control de estado.
- Reintentos sin desconectar.

---

#### `disconnect()`

**Objetivo:** Liberar recursos y conexión.

**Debe hacer:**

- Cerrar canal.
- Emitir `onDisconnected`.

---

#### `withSession(options, fn)` (estilo _context manager_)

**Objetivo:** Asegurar un ciclo completo y evitar prints intermitentes.

**Contrato:**

- Garantiza **mutex/lock** global para evitar concurrencia.
- Garantiza `connect` si hace falta.
- En `finally`: cierra transacción/flush y libera lock.

**Pseudoflujo nativo:**

1. `mutex.lock()`
2. `connectIfNeeded()`
3. `initializePrinter()` (opcional, para reset de settings)
4. `clearBuffer()`
5. `beginTransactionPrint()` (si se usa modo transacción)
6. ejecutar `fn(printer)`
7. `endTransactionPrint()` (o `print()` explícito según estrategia)
8. liberar recursos / unlock

**Eventos:**

- `onJobStart`, `onJobComplete`, `onJobError`

---

### 1.2 Configuración de medio (clave para “espacios”/feed)

#### `configureMedia({ widthDots, labelLengthDots, gapDots, mediaType, offsetDots })`

**Objetivo:** Configurar dimensiones reales del rollo.

**Debe mapear a nativo:**

- `setWidth(widthDots)`
- `setLength(labelLengthDots, gapDots, mediaType, offsetDots)`

**Notas:**

- En recibos/rollo continuo: `mediaType = CONTINUOUS` y `gapDots=0`.
- Si la impresora busca GAP y tu rollo es continuo (o viceversa) → feed blanco.

---

#### `setMargins({ horizontalDots, verticalDots })`

**Objetivo:** Márgenes globales en el buffer.

**Debe mapear:**

- `setMargin(horizontalDots, verticalDots)`

**Uso recomendado:**

- En debugging, mantener 0,0.

---

#### `setOffset(offsetDots)`

**Objetivo:** Ajuste fino entre marca/gap y línea punteada/corte.

**Debe mapear:**

- `setOffset(offsetDots)`

---

### 1.3 Impresión de alto nivel (tu caso: PDF + imágenes)

#### `printImage({ uri | base64 | bytes, widthDots?, align?, dither?, copies? })`

**Objetivo:** Imprimir comprobantes como imagen raster.

**Pipeline nativo obligatorio:**

1. Resolver entrada (URI/content/file/base64).
2. Decodificar a `Bitmap`.
3. Escalar a `widthDots` (ancho útil de impresora).
4. Convertir a monocromo (1-bit) con **dithering** (p.ej. Floyd–Steinberg).
5. Dibujar/transferir bitmap al buffer (API del SDK).
6. `print(copies)`.
7. Esperar evento `OUTPUT_COMPLETE` antes de siguiente job.

**Errores típicos:**

- No escalar → se sale del ancho → blanco/truncado.
- No dithering → casi blanco/ilegible.
- OOM por bitmaps grandes.

---

#### `printPdf({ uri, page?, dpi?, widthDots?, dither?, copies? })`

**Objetivo:** Imprimir PDF (factura) _rasterizando_.

**Pipeline nativo (Android):**

1. Abrir PDF con `PdfRenderer`.
2. Renderizar página a `Bitmap` a `dpi` suficiente.
3. Reusar pipeline de `printImage`.

**Notas:**

- “Enviar PDF directo” casi nunca funciona: se debe rasterizar.

---

#### `printQr({ text, sizeDots, ecc?, x?, y? })`

**Objetivo:** QR para comprobante.

**Recomendación:**

- Generar QR en nativo (ZXing) → bitmap → `printImage`/drawBitmap.

---

### 1.4 Operaciones de texto/códigos (si se usan)

#### `drawText({ text, x, y, fontSize, bold, underline, align, rightSpace })`

**Objetivo:** Dibujar texto en el buffer.

**Puntos críticos:**

- `rightSpace` puede generar “espacios” entre caracteres. Mantener 0 en debugging.
- Si hay Ñ/acentos: setear code page/charset.

---

#### `setCharacterSet({ internationalCharacterSet, codePage })`

**Objetivo:** Configurar cómo se interpretan bytes → caracteres.

**Cuándo usar:**

- Si imprimes texto con acentos y salen blancos/símbolos raros.

---

## 2) API low-level (escape hatch)

### 2.1 `executeDirectIo(command: string | bytes)`

**Objetivo:** Enviar comandos crudos (SLCS).

**Uso:**

- Debug para aislar si el problema es layout/buffer o medio/calibración.

**Recomendación:**

- Soportar `bytes` para control chars (CR, etc.).

---

### 2.2 `sendRawSlcs(program: string | bytes)`

**Objetivo:** Atajo semántico: enviar un “programa” SLCS completo.

**Notas:**

- Asegurar terminadores correctos (según SLCS).

---

## 3) Estado y eventos (observabilidad)

### 3.1 `getStatus({ detailed?: boolean })`

**Objetivo:** Leer estado (paper out, cover open, sensing failure, etc.).

**Uso:**

- Diagnóstico de “no imprime” o feed errático.

---

### 3.2 Eventos requeridos

- `onConnected`
- `onDisconnected`
- `onPrintStart`
- `onPrintComplete`
- `onError({ code, message, statusBytes? })`

**Recomendación:**

- Exponer status bytes crudos en errores para depuración.

---

## 4) Contratos internos (nativo) que deben existir aunque no se expongan

### 4.1 Control de concurrencia

- Un **mutex/queue** por impresora.
- No permitir dos jobs simultáneos.

### 4.2 Ciclo por job (plantilla)

1. `clearBuffer()`
2. `configureMedia()` (width + length)
3. `setMargins(0,0)` (si no se usa)
4. Construir contenido (text/bitmap/qr)
5. `print(copies)`
6. Esperar `OUTPUT_COMPLETE`

### 4.3 Normalización de inputs

- Resolver `content://` vs `file://`.
- Manejo de permisos de lectura.
- Limitar tamaño máximo de bitmap y aplicar downscale.

---

## 5) Recomendación final para el caso “facturas con QR”

- Tratar **todo como raster**: PDF → bitmap; HTML → bitmap; QR → bitmap.
- Exponer una API high-level `printPdf` y `printImage` que haga el pipeline completo.
- Mantener `executeDirectIo/sendRawSlcs` como escape hatch.
- Implementar `withSession` (context manager) para estabilidad.

---

## 6) Glosario

- **Dots**: unidad de resolución de impresión (píxeles físicos). 203 dpi ≈ 8 dots/mm.
- **MediaType**: GAP / CONTINUOUS / BLACK_MARK.
- **Dithering**: algoritmo para convertir a monocromo manteniendo legibilidad.
