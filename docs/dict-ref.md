# BIXOLON Android SDK (Label) – Diccionario + Matriz de componentes

> Propósito: tener un **mapa mental completo** de “qué es cada cosa” (JARs, .so, SDKs, comandos) y cómo se relacionan.

---

## 1) Diccionario (qué tenemos)

### 1.1 Capas principales

- **Aplicación (Expo/React Native)**
  - Llama a tu módulo nativo vía JS/TS.
  - Idealmente no maneja Bluetooth/USB directo.

- **Expo Module (Android / Kotlin/Java)**
  - Implementa la API de impresión para JS.
  - Orquesta conexión, buffer lifecycle, raster (PDF→Bitmap), dithering, colas y eventos.

- **BIXOLON Label Printer SDK (Android)**
  - Librería propietaria que expone la clase principal de impresión (buffer, drawText, drawBarcode, print, status, etc.).
  - Opera con un **image buffer** y comandos/operaciones específicas.

- **libcommon (BIXOLON Common Library)**
  - Librería “base” compartida por varios SDKs de BIXOLON.
  - Suele incluir utilidades comunes: transporte/IO, parsing de estado, helpers, etc.
  - Es una dependencia **crítica** del Label SDK.

- **JNI / NDK shared objects (`*.so`)**
  - Binarios nativos (ARMv7/ARM64) requeridos por `libcommon` o por el SDK.
  - Si falta el ABI correcto o hay mismatch de versiones, aparecen bugs erráticos.

---

### 1.2 Artefactos típicos (lo que normalmente hay en el paquete)

- `BixolonLabelPrinterLibrary_Vx.y.z.jar`
  - API principal Label: buffer, draw\*, print, status, direct IO, etc.

- `libcommon_Va.b.c.jar`
  - Dependencia base del ecosistema BIXOLON.

- `jniLibs/armeabi-v7a/libbxl_common.so`

- `jniLibs/arm64-v8a/libbxl_common.so`
  - Binarios nativos que deben acompañar a `libcommon`.

> Nota: los nombres exactos pueden variar por release, pero el patrón es este.

---

### 1.3 “Lenguajes” / modos de impresión

- **Modo SDK (image buffer / draw APIs)**
  - Construyes un layout con `drawText`, `drawBarcode`, `drawBitmap`, etc.
  - Luego `print(...)`.

- **Modo comandos (SLCS) vía Direct IO**
  - Envío de un “programa” SLCS como string/bytes.
  - Útil como _escape hatch_ y debugging.

---

### 1.4 Tipos de contenido (lo que realmente imprimimos)

- **Texto**
  - Puede depender de codepage/charset (acentos).
  - Para facturas, suele ser más robusto renderizar texto como imagen (si quieres fuentes).

- **QR / Barcodes**
  - Puede generarse como comando o como bitmap.
  - Para “facturas con QR”, lo más estable es QR→bitmap.

- **Imagen (Bitmap)**
  - Es el “denominador común” más confiable.
  - Requiere scale + dithering.

- **PDF**
  - Casi nunca se imprime “directo”; se **rasteriza** a bitmap y luego se imprime como imagen.

---

## 2) Matriz: ¿qué es cada cosa y para qué sirve?

| Elemento          | Tipo        | ¿Para qué sirve?                              | ¿Lo toca JS? | ¿Lo toca Nativo? | Riesgos típicos                                     |
| ----------------- | ----------- | --------------------------------------------- | ------------ | ---------------- | --------------------------------------------------- |
| Expo Module       | Capa        | API estable para la app (jobs, raster, cola)  | ✅           | ✅               | Concurrencia/estado si está mal diseñado            |
| Label SDK (jar)   | SDK         | Buffer/draw/print/status para impresora       | ❌           | ✅               | Bugs si no se respeta lifecycle (clearBuffer/print) |
| libcommon (jar)   | Dependencia | Base compartida: IO, helpers, parsing         | ❌           | ✅               | **Mismatch de versiones** con Label SDK             |
| `.so` JNI         | Binario     | Implementación nativa usada por libcommon/SDK | ❌           | ✅               | Falta ABI / mismatch → fallos erráticos             |
| SLCS              | Lenguaje    | Comandos directos para layout/impresión       | ❌ (ideal)   | ✅               | Terminadores/encoding; fácil equivocarse            |
| Code Pages        | Tabla       | Mapeo bytes→caracteres para texto             | ❌           | ✅               | Acentos “en blanco” por encoding mismatch           |
| Raster PDF→Bitmap | Pipeline    | Convierte PDF a imagen imprimible             | ❌           | ✅               | DPI bajo, memoria, manejo de URIs                   |
| Dithering         | Algoritmo   | Hace la imagen legible en monocromo           | ❌           | ✅               | Sin dithering sale “casi blanco”                    |
| Media config      | Config      | Ancho/largo/gap/tipo/offset (feed)            | ❌           | ✅               | Si está mal: “mucho espacio”/feed                   |

---

## 3) Matriz: dependencias y compatibilidad (lo que debe “matchear”)

### 3.1 Regla de compatibilidad #1

| Componente                   | Debe alinearse con | ¿Por qué?                                  |
| ---------------------------- | ------------------ | ------------------------------------------ |
| `BixolonLabelPrinterLibrary` | `libcommon`        | APIs internas y nativos compartidos        |
| `libcommon`                  | `libbxl_common.so` | El jar invoca JNI con símbolos específicos |
| `libbxl_common.so` (arm64)   | ABI del device     | Si no está: crash o no carga               |
| `libbxl_common.so` (armv7)   | ABI del device     | Legacy/compatibilidad                      |

**Síntomas de mismatch:**

- comportamiento errático
- “a veces imprime, a veces no”
- errores nativos al cargar librerías

---

## 4) Matriz: “Qué imprimir” vs “Cómo imprimir” (estrategia)

| Caso              | Entrada    | Estrategia recomendada                         | Resultado        |
| ----------------- | ---------- | ---------------------------------------------- | ---------------- |
| Factura PDF       | PDF        | PDF→Bitmap→Scale→Dither→PrintImage             | ✅ estable       |
| Comprobante HTML  | HTML       | Render (WebView/Skia)→Bitmap→Dither→PrintImage | ✅ estable       |
| QR (texto)        | String     | Generar QR bitmap (ZXing)→PrintImage           | ✅ estable       |
| Texto con fuentes | Text + TTF | Render texto con TTF→Bitmap→PrintImage         | ✅ estable       |
| Texto simple      | Text       | drawText + charset/codepage                    | ✅ pero limitado |

---

## 5) Checklist de “mínimo viable correcto” (para evitar bugs)

1. **Job lifecycle fijo** (por cada impresión)
   - `clearBuffer()`
   - `configureMedia()` (width + length)
   - construir contenido
   - `print()`
   - esperar `OUTPUT_COMPLETE`

2. **Mutex/Queue**
   - No permitir llamadas concurrentes a draw/print.

3. **PDF siempre raster**
   - `PdfRenderer` → `Bitmap`.

4. **Imagen siempre con scale + dithering**

5. **Alinear versiones**
   - Label jar + libcommon jar + jniLibs del mismo release.

---

## 6) Convenciones recomendadas (nombres para tu wrapper)

- `withSession(...)` / `beginJob()` / `commitJob()`
- `configureMedia(...)`
- `printPdf(...)` (raster interno)
- `printImage(...)` (scale+dither interno)
- `sendRawSlcs(...)` (escape hatch)
- `getStatus()` + `onPrintComplete`
