# Plantillas para Desarrollo Android (IntelliJ IDEA)

Esta carpeta contiene **plantillas de configuración** para desarrollar el módulo con IntelliJ IDEA.

## ⚠️ Importante

- Estos archivos **SÍ se commitean** (están versionados en git)
- Se copian automáticamente a `android/` con `make android-env`
- Los archivos copiados a `android/` **NO se commitean** (están en `.gitignore`)

---

## ✅ Setup con IntelliJ IDEA (Recomendado)

IntelliJ IDEA es la mejor opción para desarrollar módulos Expo porque:

- ✅ Trata el código como biblioteca Kotlin (no app Android)
- ✅ NO intenta crear variantes Android (debug/release)
- ✅ Autocompletado completo funciona
- ✅ Navegación de código fluida
- ✅ Refactoring y análisis de errores

### 1. Instalar IntelliJ IDEA Community (gratis):

```bash
brew install --cask intellij-idea-ce
```

### 2. Preparar ambiente:

```bash
# Desde la raíz del proyecto
make android-env
```

### 3. Abrir en IntelliJ IDEA:

```bash
cd android
open -a "IntelliJ IDEA CE" .
```

**Cuando pregunte "Import Gradle project?"** → Hacer clic en **"Import Gradle Project"**

### 4. Esperar indexación:

IntelliJ indexará las dependencias. Verás una barra de progreso abajo. Cuando termine:

- ✅ Autocompletado funcionará
- ✅ Navegación Cmd+Click funcionará
- ✅ Análisis de errores en tiempo real

---

## ❌ Android Studio (No recomendado)

Android Studio muestra error: `Cannot select root node 'debugRuntimeClasspathCopy'`

**Causa:** Android Studio intenta sincronizar como app Android (con variantes debug/release).  
**Resultado:** Sin autocompletado funcional.

Si usas Android Studio, el código funciona pero NO tendrás ayuda del IDE.

---

## 📁 Archivos en esta carpeta

### `settings.gradle`

Define dónde descargar plugins de Gradle:

- Google (para Android Gradle Plugin)
- Gradle Plugin Portal (para Kotlin)

También declara versiones de plugins:

```gradle
plugins {
    id 'com.android.library' version '8.7.3'
    id 'org.jetbrains.kotlin.android' version '1.9.25'
}
```

### `gradle.properties`

Configuración de compilación:

```properties
android.useAndroidX=true                  # Usar AndroidX (no Support Library)
android.suppressUnsupportedCompileSdk=35  # Permitir SDK 35 con AGP 8.7.3
org.gradle.jvmargs=-Xmx2048m             # Memoria para Gradle (2GB)
```

### `build.gradle`

**NO SE USA.** El archivo real del módulo es `android/build.gradle`.

---

## 🔧 Modificar configuración

Para cambiar versiones de SDK, plugins, etc:

1. **Editar archivos aquí** (en `android-dev-env/`)
2. Ejecutar:
   ```bash
   make clean-android  # Limpia archivos viejos
   make android-env    # Copia actualizados
   ```
3. **Reiniciar IntelliJ IDEA** y dejar que re-indexe

---

## 🚀 Workflow de desarrollo

```bash
# 1. Setup inicial
make android-env

# 2. Abrir IntelliJ IDEA
cd android
open -a "IntelliJ IDEA CE" .

# 3. Editar código Kotlin con autocompletado completo

# 4. Formatear código
make format

# 5. Ver errores de lint
npm run lint:kotlin

# 6. Probar en app host
cd test-app
npx expo run:android
```

---

## ❓ Por qué estos archivos no van en `android/`

Los **módulos Expo** se compilan dentro de apps host:

```
my-app/
  ├── android/
  │   ├── settings.gradle       ← La app define TODOS los repos
  │   ├── build.gradle          ← La app define plugins
  │   └── gradle.properties     ← La app define configuración
  └── node_modules/
      └── @sincpro/printer-expo/
          └── android/
              └── build.gradle  ← Solo define QUÉ compila
```

Tu módulo **hereda** la configuración de la app host. Los archivos locales solo sirven para que el IDE pueda indexar el código durante desarrollo.

---

## 🐛 Troubleshooting

### IntelliJ no indexa / autocompletado no funciona

1. **File → Invalidate Caches → Invalidate and Restart**
2. Esperar que termine de indexar de nuevo
3. Si persiste, eliminar `.idea/` y `.gradle/`:
   ```bash
   make clean-android
   make android-env
   ```

### Error "SDK location not found"

Crear `android/local.properties`:

```properties
sdk.dir=/Users/TU_USUARIO/Library/Android/sdk
```

### Dependencias no resueltas

Verificar que tienes Android SDK instalado:

```bash
# Instalar con Android Studio o:
brew install --cask android-commandlinetools
```

---

## 📚 Recursos

- [Expo Modules API](https://docs.expo.dev/modules/overview/)
- [IntelliJ IDEA Docs](https://www.jetbrains.com/help/idea/)
- [Kotlin Android Extensions](https://kotlinlang.org/docs/android-overview.html)
