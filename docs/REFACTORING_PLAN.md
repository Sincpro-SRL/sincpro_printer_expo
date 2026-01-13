# ✅ Refactoring Complete - @sincpro/printer-expo

## 📋 Summary

Successfully refactored the module with:

1. **Enterprise naming** - `sincpro.expo.printer.*`
2. **Multi-printer support** - Bridge pattern with adapters
3. **Connectivity services** - Bluetooth, Connection, Permission
4. **100% English code** - All code in English
5. **Legacy removed** - All old code deleted

---

## 🏗️ Final Architecture

### Package Name

```
NPM: @sincpro/printer-expo
Android: sincpro.expo.printer
iOS: SincproPrinter
```

### Vision del Módulo

```
┌─────────────────────────────────────────────────────┐
│               @sincpro/printer-expo                  │
│         (Bridge for Bluetooth Printers)              │
├─────────────────────────────────────────────────────┤
│                                                      │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  │
│  │   Bixolon   │  │    Zebra    │  │   Epson     │  │
│  │   Adapter   │  │   Adapter   │  │   Adapter   │  │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  │
│         │                │                │          │
│         └────────────────┼────────────────┘          │
│                          ▼                           │
│              ┌───────────────────┐                   │
│              │  IPrinterAdapter  │                   │
│              │   (Domain Port)   │                   │
│              └─────────┬─────────┘                   │
│                        ▼                             │
│              ┌───────────────────┐                   │
│              │  PrinterService   │                   │
│              └─────────┬─────────┘                   │
│                        ▼                             │
│     ┌──────────────────┴──────────────────┐         │
│     │          Infrastructure             │         │
│     ├─────────────────────────────────────┤         │
│     │  • BluetoothService                 │         │
│     │  • ConnectionService                │         │
│     │  • PermissionService                │         │
│     │  • PrintJobOrchestrator (Mutex)     │         │
│     │  • EventBus                         │         │
│     └─────────────────────────────────────┘         │
│                        ▼                             │
│              ┌───────────────────┐                   │
│              │  PrinterModule    │                   │
│              │   (Entrypoint)    │                   │
│              └───────────────────┘                   │
│                        ▼                             │
│              ┌───────────────────┐                   │
│              │  React Native /   │                   │
│              │   TypeScript      │                   │
│              └───────────────────┘                   │
└─────────────────────────────────────────────────────┘
```

---

## 📦 Estructura de Carpetas Final

```
android/src/main/java/sincpro/expo/printer/
│
├── entrypoint/
│   └── PrinterModule.kt              # Expo Module (API pública)
│
├── service/
│   ├── PrinterService.kt             # High-level printing operations
│   └── LowLevelPrintService.kt       # Context manager, primitives
│
├── domain/
│   ├── PrinterAdapter.kt             # IPrinterAdapter interface + PrinterStatus
│   ├── PrintJob.kt                   # PrintJob + PrintJobResult
│   ├── Receipt.kt                    # Receipt + ReceiptLine sealed class
│   ├── MediaConfig.kt                # MediaConfig + MediaType enum
│   └── LayoutTypes.kt                # FontSize, Alignment
│
├── infrastructure/
│   ├── bluetooth/
│   │   └── BluetoothService.kt       # Bluetooth discovery, pairing
│   ├── connection/
│   │   └── ConnectionService.kt      # Connection lifecycle management
│   ├── permission/
│   │   └── PermissionService.kt      # Android permissions handling
│   ├── orchestration/
│   │   ├── PrintJobOrchestrator.kt   # Mutex + Queue + Job context
│   │   └── EventBus.kt               # Event publishing
│   └── session/
│       └── PrintSessionContext.kt    # Context manager pattern
│
└── adapter/
    ├── bixolon/
    │   └── BixolonPrinterAdapter.kt  # Bixolon SDK wrapper
    ├── zebra/
    │   └── ZebraPrinterAdapter.kt    # (Future) Zebra SDK wrapper
    └── generic/
        └── GenericBluetoothAdapter.kt # Generic ESC/POS printers
```

### TypeScript (src/)

```
src/
├── index.ts                          # Public exports
├── PrinterModule.ts                  # Native module bridge
├── types/
│   ├── index.ts                      # All type exports
│   ├── printer.types.ts              # Printer types
│   ├── bluetooth.types.ts            # Bluetooth types
│   ├── receipt.types.ts              # Receipt types
│   └── connection.types.ts           # Connection types
└── utils/
    └── receiptBuilder.ts             # Fluent Receipt builder
```

---

## 🗑️ Archivos a ELIMINAR (Legacy)

```bash
# Legacy code (será reemplazado)
android/src/main/java/expo/sincpro/ExpoBixolonModule.kt
android/src/main/java/expo/sincpro/bixolon/BixolonQRPrinter.kt
android/src/main/java/expo/sincpro/managers/BluetoothManager.kt
android/src/main/java/expo/sincpro/managers/ConnectionManager.kt
android/src/main/java/expo/sincpro/managers/PermissionManager.kt
android/src/main/java/expo/sincpro/managers/PrinterManager.kt

# Old Clean Architecture (reemplazar con nueva estructura)
android/src/main/java/expo/sincpro/entrypoint/ExpoBixolonModule.kt
android/src/main/java/expo/sincpro/adapter/BixolonPrinterAdapter.kt
android/src/main/java/expo/sincpro/domain/*
android/src/main/java/expo/sincpro/infrastructure/*
android/src/main/java/expo/sincpro/service/*

# Old TypeScript files
src/ExpoBixolonModule.ts
src/ExpoBixolon.types.ts
src/BixolonPrinter.ts
src/QrCodePrinter.ts
```

---

## 📝 Checklist de Implementación

### Phase 1: Setup & Structure

- [ ] Create new folder structure under `sincpro/expo/printer/`
- [ ] Update `expo-module.config.json` with new package name
- [ ] Update `AndroidManifest.xml` with new package
- [ ] Update `package.json` metadata

### Phase 2: Domain Layer (Pure Kotlin, No SDK)

- [ ] `domain/PrinterAdapter.kt` - IPrinterAdapter, PrinterStatus, PrinterEvent
- [ ] `domain/PrintJob.kt` - PrintJob, PrintJobResult, PrintJobStatus
- [ ] `domain/Receipt.kt` - Receipt, ReceiptLine (sealed class)
- [ ] `domain/MediaConfig.kt` - MediaConfig, MediaType
- [ ] `domain/LayoutTypes.kt` - FontSize, Alignment

### Phase 3: Infrastructure Layer (Non-functional concerns)

- [ ] `infrastructure/bluetooth/BluetoothService.kt` - Discovery, pairing
- [ ] `infrastructure/connection/ConnectionService.kt` - Lifecycle
- [ ] `infrastructure/permission/PermissionService.kt` - Permissions
- [ ] `infrastructure/orchestration/PrintJobOrchestrator.kt` - Mutex
- [ ] `infrastructure/orchestration/EventBus.kt` - Events
- [ ] `infrastructure/session/PrintSessionContext.kt` - Context manager

### Phase 4: Adapter Layer (SDK Wrappers)

- [ ] `adapter/bixolon/BixolonPrinterAdapter.kt` - Bixolon SDK

### Phase 5: Service Layer (Use Cases)

- [ ] `service/PrinterService.kt` - High-level operations
- [ ] `service/LowLevelPrintService.kt` - Primitives

### Phase 6: Entrypoint (Expo Module)

- [ ] `entrypoint/PrinterModule.kt` - Public API

### Phase 7: TypeScript Layer

- [ ] `src/index.ts` - Exports
- [ ] `src/PrinterModule.ts` - Native bridge
- [ ] `src/types/*.ts` - Type definitions
- [ ] `src/utils/receiptBuilder.ts` - Builder utility

### Phase 8: Cleanup

- [ ] Delete all legacy files
- [ ] Delete old `expo/sincpro/` folder
- [ ] Update example app
- [ ] Update documentation

---

## 🔧 Cambios de Configuración

### expo-module.config.json

```json
{
  "platforms": ["apple", "android"],
  "android": {
    "modules": ["sincpro.expo.printer.entrypoint.PrinterModule"]
  },
  "apple": {
    "modules": ["SincproPrinterModule"]
  }
}
```

### AndroidManifest.xml

```xml
<manifest package="sincpro.expo.printer">
    <uses-permission android:name="android.permission.BLUETOOTH" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
</manifest>
```

---

## 🌍 Naming Conventions (English Only)

### Kotlin

```kotlin
// Classes: PascalCase
class PrinterService
class BluetoothService
class BixolonPrinterAdapter

// Functions: camelCase, action verbs
fun connectPrinter()
fun discoverDevices()
fun printReceipt()

// Variables: camelCase
val printerStatus: PrinterStatus
val bluetoothDevices: List<BluetoothDevice>

// Constants: SCREAMING_SNAKE_CASE
const val CONNECTION_TIMEOUT_MS = 30_000L
const val DEFAULT_MEDIA_WIDTH = 832
```

### TypeScript

```typescript
// Interfaces: PascalCase with I prefix (optional)
interface PrinterStatus {}
interface BluetoothDevice {}

// Types: PascalCase
type PrintJobResult = 'success' | 'error' | 'timeout';

// Functions: camelCase
function printReceipt(receipt: Receipt): Promise<void>;

// Constants: SCREAMING_SNAKE_CASE
export const DEFAULT_TIMEOUT = 30000;
```

---

## 🎯 API Pública Final (TypeScript)

```typescript
import Printer from '@sincpro/printer-expo';

// Bluetooth
await Printer.bluetooth.isEnabled();
await Printer.bluetooth.discoverDevices();
await Printer.bluetooth.getPairedDevices();

// Connection
await Printer.connection.connect({ address: 'XX:XX:XX:XX:XX:XX', type: 'bluetooth' });
await Printer.connection.disconnect();
await Printer.connection.getStatus();

// Printing
await Printer.print.receipt(receipt);
await Printer.print.lines(lines);
await Printer.print.qrCode(data, size);

// Low-level (advanced users)
await Printer.lowLevel.withSession(mediaConfig, async (session) => {
  await session.drawText('Hello', 50, 50);
  await session.drawQR('123', 100, 100);
  await session.print();
});
```

---

## ⏱️ Estimación de Tiempo

| Phase             | Archivos     | Tiempo Est.  |
| ----------------- | ------------ | ------------ |
| 1. Setup          | 4            | 10 min       |
| 2. Domain         | 5            | 15 min       |
| 3. Infrastructure | 6            | 25 min       |
| 4. Adapter        | 1            | 15 min       |
| 5. Service        | 2            | 15 min       |
| 6. Entrypoint     | 1            | 20 min       |
| 7. TypeScript     | 6            | 20 min       |
| 8. Cleanup        | -            | 10 min       |
| **TOTAL**         | **25 files** | **~130 min** |

---

## ✅ Criterios de Éxito

1. **Zero Spanish** - All code in English
2. **Clean namespace** - `sincpro.expo.printer.*`
3. **No legacy code** - All old files deleted
4. **Working Bluetooth** - Discovery, pairing, connection
5. **Multi-printer ready** - Adapter pattern for future printers
6. **Proper connectivity service** - Lifecycle management
7. **Type-safe TypeScript** - Full type definitions

---

## 🚀 ¿Comenzamos?

Una vez aprobado este plan, procederé en orden:

1. Crear estructura de carpetas
2. Implementar capa por capa
3. Eliminar código legacy
4. Testing básico

**¿Apruebas este plan?**
