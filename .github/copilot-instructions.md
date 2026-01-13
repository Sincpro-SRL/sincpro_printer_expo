# GitHub Copilot Instructions for Expo Bixolon Module

## Project Overview

This is an **Expo native module** for controlling Bixolon thermal printers in React Native applications. The module follows **Clean Architecture** with **Hexagonal Architecture** (Ports & Adapters) patterns.

**Key Technologies:**

- **Frontend**: TypeScript, React Native, Expo Modules API
- **Backend**: Kotlin, Android SDK, Bixolon SDK
- **Architecture**: Clean Architecture, Hexagonal Architecture, SOLID principles
- **Async**: Kotlin Coroutines, Expo AsyncFunction
- **Error Handling**: Result<T> pattern

---

## Architecture Layers (CRITICAL)

When generating or modifying code, **ALWAYS** respect the dependency rule:

```
┌─────────────────────────────────────────────────────────┐
│  Dependencies ALWAYS point inward (toward Domain)       │
└─────────────────────────────────────────────────────────┘

Entrypoint → Service → Domain ← Adapter
                ↓
        Infrastructure → Domain
```

### 1. **ENTRYPOINT** (`entrypoint/`)

**Purpose**: React Native ↔ Kotlin bridge

**Rules:**

- ✅ Use `AsyncFunction` for async operations
- ✅ Use `Function` for sync operations
- ✅ Throw exceptions for JS (`.getOrThrow()`)
- ✅ Emit events with `sendEvent()`
- ❌ NO business logic
- ❌ NO direct infrastructure calls

**Template:**

```kotlin
package sincpro.expo.printer.entrypoint

import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class PrinterModule : Module() {
    override fun definition() = ModuleDefinition {
        Name("ExpoBixolon")

        AsyncFunction("functionName") { param1: String, param2: Int ->
            service.doSomething(param1, param2)
                .getOrThrow() // Converts Result to exception
        }

        Events("onDeviceDiscovered", "onConnectionChanged")
    }
}
```

---

### 2. **SERVICE** (`service/`)

**Purpose**: Use cases and business workflows

**Rules:**

- ✅ Return `Result<T>` for error handling
- ✅ Use `withContext(Dispatchers.IO)` for blocking operations
- ✅ Depend on interfaces (IPrinterAdapter, IBluetoothProvider)
- ✅ Publish domain events via EventBus
- ❌ NO direct Android SDK calls
- ❌ NO concrete adapter instantiation

**Template:**

```kotlin
package sincpro.expo.printer.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import sincpro.expo.printer.domain.IPrinterAdapter
import sincpro.expo.printer.domain.ConnectionConfig

class ConnectivityService(
    private val bluetoothProvider: IBluetoothProvider,
    private val printerAdapter: IPrinterAdapter,
    private val eventBus: EventBus,
) {
    suspend fun connect(config: ConnectionConfig): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                printerAdapter.connect(config.address, config.port)
                    .onSuccess {
                        eventBus.publish(PrinterEvent.Connected)
                    }
            } catch (e: Exception) {
                Result.failure(ConnectionFailedException(config.address, e))
            }
        }
    }
}
```

---

### 3. **DOMAIN** (`domain/`)

**Purpose**: Core business entities and rules (framework-independent)

**Rules:**

- ✅ Pure Kotlin only (NO Android imports)
- ✅ Define interfaces (IPrinterAdapter, IBluetoothProvider)
- ✅ Define domain exceptions
- ✅ Use data classes for entities
- ❌ NO implementation details
- ❌ NO external dependencies

**Template:**

```kotlin
package sincpro.expo.printer.domain

/**
 * DOMAIN - Entity Name
 *
 * Description of the entity and its purpose.
 */
data class EntityName(
    val property1: String,
    val property2: Int,
)

/**
 * DOMAIN - Interface Name
 *
 * Contract for adapter implementations.
 */
interface IAdapterName {
    suspend fun doSomething(param: String): Result<Unit>
}

/**
 * DOMAIN - Exception
 */
class SpecificException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
```

---

### 4. **ADAPTER** (`adapter/`)

**Purpose**: Printer brand-specific implementations

**Rules:**

- ✅ Implement `IPrinterAdapter` interface
- ✅ Integrate vendor SDKs (Bixolon, Zebra, etc.)
- ✅ Return `Result<T>` for operations
- ✅ Use `withContext(Dispatchers.IO)` for blocking calls
- ❌ NO business logic
- ❌ NO Service layer dependencies

**Template:**

```kotlin
package sincpro.expo.printer.adapter.bixolon

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import sincpro.expo.printer.domain.IPrinterAdapter
import sincpro.expo.printer.domain.ConnectionFailedException

class BixolonPrinterAdapter : IPrinterAdapter {
    private val bxlService = BixolonPrinter()

    override suspend fun connect(address: String, port: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                bxlService.connect(address, port, timeout = 10)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(ConnectionFailedException(address, e))
            }
        }
    }
}
```

---

### 5. **INFRASTRUCTURE** (`infrastructure/`)

**Purpose**: External frameworks and platform-specific code

**Rules:**

- ✅ Wrap Android APIs (Bluetooth, Permissions)
- ✅ Implement domain interfaces (IBluetoothProvider)
- ✅ Handle platform-specific concerns
- ❌ NO business logic
- ❌ NO Service dependencies

**Template:**

```kotlin
package sincpro.expo.printer.infrastructure.bluetooth

import android.bluetooth.BluetoothAdapter
import android.content.Context
import sincpro.expo.printer.domain.IBluetoothProvider
import sincpro.expo.printer.domain.BluetoothDeviceInfo

class AndroidBluetoothProvider(
    context: Context,
) : IBluetoothProvider {
    private val bluetoothAdapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager)
            .adapter

    override fun getPairedDevices(): Result<List<BluetoothDeviceInfo>> {
        return try {
            val devices = bluetoothAdapter?.bondedDevices?.map { device ->
                BluetoothDeviceInfo(
                    name = device.name,
                    address = device.address,
                    type = mapDeviceType(device.type),
                    isPrinter = isPrinterDevice(device.name)
                )
            } ?: emptyList()
            Result.success(devices)
        } catch (e: SecurityException) {
            Result.failure(BluetoothPermissionDeniedException())
        }
    }
}
```

---

## Code Style Guidelines

### Kotlin

**Formatting**: Use ktlint rules (see `android/.editorconfig`)

```kotlin
// ✅ Good: Explicit imports
import sincpro.expo.printer.domain.ConnectionType
import sincpro.expo.printer.domain.ConnectionStatus
import sincpro.expo.printer.domain.BluetoothDeviceInfo

// ❌ Bad: Wildcard imports
import sincpro.expo.printer.domain.*

// ✅ Good: Single KDoc block
/**
 * Connect to printer via Bluetooth
 *
 * @param address MAC address
 * @param port TCP port (default: 9100)
 * @return Result indicating success or failure
 */
suspend fun connect(address: String, port: Int = 9100): Result<Unit>

// ❌ Bad: Multiple comment blocks
/**
 * Connect to printer
 */
/**
 * via Bluetooth
 */
suspend fun connect(address: String, port: Int): Result<Unit>

// ✅ Good: PascalCase for classes, camelCase for functions
class PrinterAdapter {
    suspend fun connectBluetooth(address: String): Result<Unit>
}

// ❌ Bad: Inconsistent naming
class printerAdapter {
    suspend fun ConnectBluetooth(Address: String): Result<Unit>
}
```

**Error Handling**: Always use `Result<T>`

```kotlin
// ✅ Good: Result<T> pattern
suspend fun connect(address: String): Result<Unit> {
    return try {
        adapter.connect(address)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(ConnectionFailedException(address, e))
    }
}

// ❌ Bad: Swallow exceptions
suspend fun connect(address: String) {
    try {
        adapter.connect(address)
    } catch (e: Exception) {
        // Silent failure - BAD!
    }
}
```

---

### TypeScript

**Type Safety**: Export explicit types matching Kotlin

```typescript
// ✅ Good: Clear types matching Kotlin data classes
export interface BluetoothDevice {
  name: string;
  address: string;
  isPrinter: boolean;
}

export type ConnectionStatus = 'disconnected' | 'connecting' | 'connected' | 'error';

export async function connectBluetooth(address: string, timeout?: number): Promise<void> {
  return await ExpoBixolonModule.connectBluetooth(address, timeout ?? 30000);
}

// ❌ Bad: Any types, no matching
export function connect(addr: any): any {
  return ExpoBixolonModule.connectBluetooth(addr);
}
```

---

## When Adding New Features

### Step-by-Step Process

1. **Define Domain Entity** (`domain/`)

   ```kotlin
   // domain/QrCode.kt
   data class QrCode(
       val data: String,
       val size: Int,
       val errorCorrection: QrErrorCorrection
   )
   ```

2. **Update Interface** (`domain/`)

   ```kotlin
   interface IPrinterAdapter {
       suspend fun drawQR(qr: QrCode): Result<Unit>
   }
   ```

3. **Implement in Adapter** (`adapter/`)

   ```kotlin
   class BixolonPrinterAdapter : IPrinterAdapter {
       override suspend fun drawQR(qr: QrCode): Result<Unit> {
           // Bixolon SDK implementation
       }
   }
   ```

4. **Create Service Method** (`service/`)

   ```kotlin
   class PrintService {
       suspend fun printQRCode(qr: QrCode): Result<Unit> {
           return adapter.drawQR(qr)
       }
   }
   ```

5. **Expose to React Native** (`entrypoint/`)

   ```kotlin
   AsyncFunction("printQRCode") { data: String, size: Int ->
       printService.printQRCode(QrCode(data, size))
           .getOrThrow()
   }
   ```

6. **Add TypeScript Types** (`src/`)
   ```typescript
   export function printQRCode(data: string, size: number): Promise<void>;
   ```

---

## Common Patterns

### Async Operations

```kotlin
// Service layer
suspend fun fetchData(): Result<Data> {
    return withContext(Dispatchers.IO) {
        try {
            val data = adapter.getData()
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Entrypoint layer
AsyncFunction("fetchData") {
    service.fetchData()
        .getOrThrow() // Throws to JavaScript
}
```

### Event Emission

```kotlin
// Service publishes domain events
eventBus.publish(PrinterEvent.Connected)

// Entrypoint listens and forwards to React Native
private val eventListener = EventBus.Listener { event ->
    when (event) {
        is PrinterEvent.Connected -> {
            sendEvent("onConnectionChanged", mapOf(
                "status" to "connected"
            ))
        }
    }
}
```

### Dependency Injection (Constructor)

```kotlin
// ✅ Good: Inject interfaces via constructor
class PrintService(
    private val adapter: IPrinterAdapter,
    private val eventBus: EventBus
) {
    // Implementation
}

// ❌ Bad: Create concrete instances inside
class PrintService {
    private val adapter = BixolonPrinterAdapter() // Hard-coded!
}
```

---

## Package Configuration (Expo Module)

**Critical**: Use `peerDependencies` for Expo/React Native

```json
{
  "peerDependencies": {
    "expo": ">=52.0.0",
    "react": "*",
    "react-native": "*"
  },
  "devDependencies": {
    "expo-module-scripts": "^5.0.8",
    "typescript": "^5.3.0"
  }
}
```

**Why?** Prevents duplicate packages in host apps.

---

## Checklist Before Code Generation

- [ ] Respects architecture layers (no backward dependencies)
- [ ] Domain layer has NO Android imports
- [ ] Services return `Result<T>`
- [ ] Async operations use `withContext(Dispatchers.IO)`
- [ ] Entrypoint uses `AsyncFunction` for async, `Function` for sync
- [ ] Explicit imports (no wildcards)
- [ ] Single KDoc comment block
- [ ] TypeScript types match Kotlin data classes
- [ ] Error handling with domain exceptions

---

## Files to Reference

When generating code, always check these files for context:

### Architecture

- `ARCHITECTURE.md` - Detailed architecture guide
- `CONTRIBUTING.md` - Development guidelines

### Domain Layer (Framework-independent)

- `android/src/main/java/sincpro/expo/printer/domain/PrinterAdapter.kt` - Adapter interface
- `android/src/main/java/sincpro/expo/printer/domain/BluetoothTypes.kt` - Bluetooth entities
- `android/src/main/java/sincpro/expo/printer/domain/Receipt.kt` - Receipt entity
- `android/src/main/java/sincpro/expo/printer/domain/PrintJob.kt` - Print job entity

### Service Layer (Use cases)

- `android/src/main/java/sincpro/expo/printer/service/ConnectivityService.kt` - Connection management
- `android/src/main/java/sincpro/expo/printer/service/LowLevelPrintService.kt` - Low-level printing
- `android/src/main/java/sincpro/expo/printer/service/HighLevelPrintService.kt` - Receipt printing

### Adapter Layer (Vendor integrations)

- `android/src/main/java/sincpro/expo/printer/adapter/bixolon/BixolonPrinterAdapter.kt` - Bixolon SDK

### Infrastructure Layer (Platform-specific)

- `android/src/main/java/sincpro/expo/printer/infrastructure/bluetooth/AndroidBluetoothProvider.kt` - Android Bluetooth wrapper

### Entrypoint (React Native bridge)

- `android/src/main/java/sincpro/expo/printer/entrypoint/PrinterModule.kt` - Main module
- `android/src/main/java/sincpro/expo/printer/entrypoint/PrinterPackage.kt` - Registration
- `android/src/main/java/sincpro/expo/printer/entrypoint/PrinterLifecycleListener.kt` - Lifecycle

### TypeScript

- `src/index.ts` - Main export
- `src/ExpoBixolon.types.ts` - Type definitions
- `src/ExpoBixolonModule.ts` - Module wrapper

---

## Anti-Patterns to AVOID

### ❌ Violating Dependency Rule

```kotlin
// BAD: Domain importing from Infrastructure
package sincpro.expo.printer.domain

import sincpro.expo.printer.infrastructure.AndroidBluetoothProvider // ❌ NEVER!
```

### ❌ Business Logic in Entrypoint

```kotlin
// BAD: Entrypoint with business logic
AsyncFunction("connect") { address: String ->
    val adapter = BixolonPrinterAdapter()
    adapter.connect(address) // ❌ Business logic in entrypoint!
}
```

### ❌ Concrete Dependencies in Service

```kotlin
// BAD: Service using concrete class
class PrintService {
    private val adapter = BixolonPrinterAdapter() // ❌ Hard-coded!
}

// GOOD: Service using interface
class PrintService(
    private val adapter: IPrinterAdapter // ✅ Interface
)
```

### ❌ Wildcard Imports

```kotlin
// BAD
import sincpro.expo.printer.domain.*

// GOOD
import sincpro.expo.printer.domain.ConnectionType
import sincpro.expo.printer.domain.ConnectionStatus
```

### ❌ Swallowing Exceptions

```kotlin
// BAD: Silent failure
suspend fun connect(address: String) {
    try {
        adapter.connect(address)
    } catch (e: Exception) {
        // Nothing - BAD!
    }
}

// GOOD: Return Result
suspend fun connect(address: String): Result<Unit> {
    return try {
        adapter.connect(address)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(ConnectionFailedException(address, e))
    }
}
```

---

## Summary

When working on this codebase:

1. **Follow Clean Architecture** - Dependencies point inward
2. **Use interfaces** - Not concrete implementations
3. **Return Result<T>** - For error handling
4. **Use withContext(Dispatchers.IO)** - For blocking operations
5. **Explicit imports** - No wildcards
6. **Type safety** - TypeScript types match Kotlin
7. **Domain-first** - Start with entities and interfaces

**Goal**: Write maintainable, testable, and swappable code that can support multiple printer brands without changing business logic.
