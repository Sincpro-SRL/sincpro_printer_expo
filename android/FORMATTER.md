# Kotlin Formatter

Este proyecto usa **ktlint** para formateo automático de código Kotlin.

## Comandos disponibles

### Desde npm (root del proyecto)

```bash
# Formatear todo el código Kotlin
npm run format:kotlin

# Solo verificar (sin modificar archivos)
npm run lint:kotlin
```

### Desde Gradle (carpeta android/)

```bash
cd android

# Formatear
./gradlew ktlintFormat

# Solo verificar
./gradlew ktlintCheck
```

## Configuración

- **Plugin**: `org.jlleitschuh.gradle.ktlint` v12.1.0
- **ktlint version**: 1.0.1
- **Reglas**: Android oficial + EditorConfig
- **Límite de línea**: 120 caracteres

## EditorConfig

El archivo `.editorconfig` en `android/` define:

- Indentación: 4 espacios
- Encoding: UTF-8
- Final de línea: LF
- Trailing commas permitidos

## Integración con Android Studio

Android Studio detectará automáticamente `.editorconfig` y aplicará las reglas al formatear (Cmd+Opt+L).

## Pre-commit (opcional)

Puedes agregar un hook de git para formatear antes de cada commit:

```bash
# .git/hooks/pre-commit
#!/bin/sh
cd android && ./gradlew ktlintFormat
```
