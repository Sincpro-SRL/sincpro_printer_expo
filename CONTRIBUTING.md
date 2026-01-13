# Contributing to @sincpro/printer-expo

¡Gracias por tu interés en contribuir! Este documento te guiará en el proceso de desarrollo.

## 🏗️ Arquitectura del Proyecto

Este módulo sigue **Clean Architecture** con **Hexagonal Architecture** (Ports & Adapters).

**IMPORTANTE**: Lee [ARCHITECTURE.md](ARCHITECTURE.md) y [.github/copilot-instructions.md](.github/copilot-instructions.md) antes de contribuir.

---

## 🔧 Setup de Desarrollo

### Requisitos

- **Node.js** >= 18.0.0
- **npm** o **yarn**
- **IntelliJ IDEA Community** (recomendado para Android)
- **Android SDK** (API 21-35)
- **Xcode** 14+ (para iOS, futuro)
- **ktlint** (para formateo Kotlin)

### 1. Clonar repositorio

```bash
git clone https://github.com/Sincpro-SRL/sincpro_printer_expo.git
cd sincpro_printer_expo
```

### 2. Instalar dependencias

```bash
npm install
```

### 3. Setup pre-commit hooks

```bash
make prepare-environment
# o manualmente:
pipx install pre-commit
pre-commit install
```

### 4. Setup ambiente Android

```bash
make android-env
```

Esto crea archivos de configuración locales para IntelliJ IDEA.

### 5. Abrir en IntelliJ IDEA

```bash
# Instalar IntelliJ IDEA Community (gratis)
brew install --cask intellij-idea-ce

# Abrir proyecto
cd android
open -a "IntelliJ IDEA CE" .
```

**Cuando pregunte "Import Gradle project?"** → **Sí**

---

## 📝 Reglas de Código

### TypeScript

- **Prettier** para formateo: `npm run format`
- **ESLint** para linting: `npm run lint`
- Tipado estricto, sin `any`
- Exportar tipos explícitos

### Kotlin

- **ktlint** para formateo: `make format`
- Verificar lint: `npm run lint:kotlin`
- Usar coroutines para operaciones asíncronas
- Siempre retornar `Result<T>`
- Comentarios KDoc para APIs públicas

### Estructura de directorios (CRÍTICA)

```
android/src/main/java/sincpro/expo/printer/
├── domain/           ← Entidades, interfaces (NO imports Android)
├── infrastructure/   ← Wrappers de Android SDK
├── service/          ← Casos de uso, orquestación
├── adapter/          ← Implementaciones de IPrinterAdapter
└── entrypoint/       ← Bridge React Native ↔ Kotlin
```

**Regla de dependencia**: `Entrypoint → Service → Domain ← Adapter, Infrastructure`

### Commits

Seguimos [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: nueva funcionalidad de impresión
fix: corregir conexión Bluetooth
docs: actualizar README
chore: actualizar dependencias
refactor: limpiar código en PrintService
```

---

## 🧪 Testing

### Ejecutar tests

```bash
npm test
```

### Crear tests

Los tests están en `__tests__/`:

```typescript
describe('ExpoBixolon', () => {
  it('should scan bluetooth devices', async () => {
    const devices = await ExpoBixolon.scanBluetoothDevices();
    expect(Array.isArray(devices)).toBe(true);
  });
});
```

---

## 🔄 Workflow de Desarrollo

### 1. Crear rama

```bash
git checkout -b feature/nueva-funcionalidad
# o
git checkout -b fix/corregir-bug
```

### 2. Desarrollar

```bash
# Editar código en IntelliJ IDEA
# Autocompletado y navegación funcionarán completamente

# Formatear antes de commitear
make format

# Verificar lint
npm run lint:kotlin
```

### 3. Probar en app de ejemplo

```bash
# Crear app de prueba
cd ..
npx create-expo-app test-printer-app
cd test-printer-app

# Instalar módulo local
npm install ../sincpro_printer_expo

# Probar
npx expo run:android
```

### 4. Commitear

Pre-commit hooks verificarán automáticamente:

- ✅ Formateo (Prettier, ktlint)
- ✅ Lint (ESLint, ktlint)
- ✅ Tipos TypeScript

```bash
git add .
git commit -m "feat: agregar soporte para papel de 80mm"
git push origin feature/nueva-funcionalidad
```

### 5. Pull Request

1. Ve a GitHub y crea el PR
2. Describe los cambios claramente
3. Menciona issue relacionado si aplica
4. Espera revisión

---

## 📦 Publicación (Solo Maintainers)

### 1. Actualizar versión

```bash
make update-version VERSION=1.2.0
```

### 2. Build

```bash
make build
```

### 3. Publicar

```bash
make publish
```

O configurar en CI/CD con `NPM_TOKEN`.

---

## 🏛️ Principios de Arquitectura

### Clean Architecture

1. **Domain** (núcleo): Sin imports externos
2. **Ports** (interfaces): Contratos entre capas
3. **Adapters**: Implementaciones concretas
4. **Entrypoint**: Bridge hacia exterior

### SOLID

- **S**ingle Responsibility: Una clase = una responsabilidad
- **O**pen/Closed: Abierto a extensión, cerrado a modificación
- **L**iskov Substitution: Interfaces intercambiables
- **I**nterface Segregation: Interfaces específicas
- **D**ependency Inversion: Depender de abstracciones

### Agregar nuevo adapter (ej: Zebra)

```kotlin
// 1. Domain: Interface ya existe (IPrinterAdapter)
// No cambios necesarios

// 2. Adapter: Implementar
class ZebraPrinterAdapter : IPrinterAdapter {
    override suspend fun connect(address: String, port: Int): Result<Unit> {
        // Zebra SDK implementation
    }
    // ... otros métodos
}

// 3. Service: Inyectar adapter
class PrintJobOrchestrator(
    private val adapter: IPrinterAdapter // ← Funciona con cualquier adapter
)

// 4. Entrypoint: Decidir cuál usar
val adapter = if (brand == "zebra") {
    ZebraPrinterAdapter(context)
} else {
    BixolonPrinterAdapter(context)
}
```

---

## 🐛 Debugging

### Android Studio errores de Gradle sync

**Es normal.** Android Studio muestra: `debugRuntimeClasspathCopy error`

**Solución:** Usar IntelliJ IDEA en su lugar (ver Setup arriba).

### Autocompletado no funciona

1. IntelliJ IDEA → **File → Invalidate Caches → Restart**
2. O limpiar cache:
   ```bash
   make clean-android
   make android-env
   ```

### Módulo no se encuentra en app host

Verificar `package.json`:

```json
{
  "files": ["build", "android", "ios", "src"]
}
```

---

## 📚 Recursos

- [Expo Modules API](https://docs.expo.dev/modules/overview/)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [ktlint](https://ktlint.github.io/)

---

## 💬 Preguntas

Si tienes dudas:

1. Revisa [ARCHITECTURE.md](ARCHITECTURE.md)
2. Revisa [.github/copilot-instructions.md](.github/copilot-instructions.md)
3. Abre un [Issue en GitHub](https://github.com/Sincpro-SRL/sincpro_printer_expo/issues)

---

**¡Gracias por contribuir!** 🚀
