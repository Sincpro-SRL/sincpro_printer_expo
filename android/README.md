# Android Development para Módulo Expo

## ⚠️ ERROR CONOCIDO: `debugRuntimeClasspathCopy`

Este error **NO tiene solución** y **es normal**. Android Studio intenta sincronizar el módulo como una app Android completa, pero los módulos Expo son bibliotecas.

```
Cannot select root node 'debugRuntimeClasspathCopy' as a variant
```

**IGNORA ESTE ERROR.** El módulo funciona correctamente en apps host.

---

## Lo que SÍ funciona en Android Studio:

A pesar del error rojo de Gradle sync, puedes desarrollar normalmente:

✅ **Autocompletado de Kotlin** - Funciona  
✅ **Navegación de código** - Cmd+Click en clases/funciones  
✅ **Análisis de errores** - Ve errores de sintaxis y tipos  
✅ **Formateo de código** - Cmd+Opt+L  
✅ **Refactoring** - Renombrar, extraer métodos, etc.  
✅ **Git integration** - Commits, diff, branches  
✅ **Debug** - Puedes poner breakpoints (cuando uses en app host)

---

## Setup rápido

```bash
# Desde la raíz del proyecto
make android-env

# Abrir Android Studio
cd android
open -a "Android Studio" .
```

**Ignora el banner rojo de Gradle sync y trabaja normalmente.**

---

## ¿Cómo probar el módulo?

El módulo se prueba en una **app host**, no standalone:

```bash
# Crear app de prueba
cd ..
npx create-expo-app test-app
cd test-app
npm install ../sincpro_printer_expo

# Compilar y correr
npx expo run:android
```

---

## Alternativa sin errores: IntelliJ IDEA

Si el error te molesta mucho:

```bash
brew install --cask intellij-idea-ce
cd android
open -a "IntelliJ IDEA CE" .
```

IntelliJ **no intenta** sincronizar como app Android, funciona mejor con bibliotecas.

---

## Comandos útiles

```bash
make clean-android  # Limpia caché
make android-env    # Setup ambiente
make format         # Formatear Kotlin
npm run lint:kotlin # Ver errores de lint
```

---

## Estructura

```
android-dev-env/          ← Templates versionados
├── settings.gradle
├── gradle.properties
└── README.md

android/
├── build.gradle          ← ✅ Commitear
├── libs/                 ← ✅ Commitear
├── src/main/             ← ✅ Commitear
│
├── settings.gradle       ← ❌ Local (copiado)
├── gradle.properties     ← ❌ Local (copiado)
├── .gradle/, .idea/      ← ❌ Ignorados
└── build/                ← ❌ Ignorado
```
