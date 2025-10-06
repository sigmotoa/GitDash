# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

GitDash is an Android application built with Kotlin and Jetpack Compose. The project uses the modern Android development stack with Material 3 design system and targets Android 12+ (SDK 35-36).

**Package namespace**: `com.sigmotoa.gitdash`

## Build System

This project uses Gradle with Kotlin DSL (`.kts` files) and version catalogs for dependency management.

### Essential Commands

```bash
# Build the project
./gradlew build

# Assemble debug APK
./gradlew assembleDebug

# Install and run on connected device/emulator
./gradlew installDebug

# Run unit tests
./gradlew test
./gradlew testDebugUnitTest

# Run instrumented tests (requires device/emulator)
./gradlew connectedDebugAndroidTest

# Run lint checks
./gradlew lint

# Clean build artifacts
./gradlew clean
```

## Project Structure

### Source Code Organization

```
app/src/
├── main/
│   ├── java/com/sigmotoa/gitdash/
│   │   ├── MainActivity.kt              # Main entry point
│   │   └── ui/theme/                    # Theme configuration
│   │       ├── Color.kt                 # Color definitions
│   │       ├── Theme.kt                 # Material 3 theme setup
│   │       └── Type.kt                  # Typography definitions
│   ├── res/                             # Android resources
│   └── AndroidManifest.xml
├── test/                                # Unit tests
└── androidTest/                         # Instrumented tests
```

### Key Configuration Files

- **build.gradle.kts** (root): Top-level build configuration
- **app/build.gradle.kts**: App module configuration with dependencies
- **gradle/libs.versions.toml**: Version catalog for centralized dependency management
- **settings.gradle.kts**: Project settings and repository configuration

## Architecture & Tech Stack

### UI Framework
- **Jetpack Compose**: Declarative UI framework (no XML layouts)
- **Material 3**: Design system with dynamic color support (Android 12+)
- **Edge-to-edge display**: Enabled by default

### Dependencies (via Version Catalog)
Access dependencies using `libs.` notation:
- `libs.androidx.core.ktx` - AndroidX Core KTX
- `libs.androidx.activity.compose` - Compose Activity integration
- `libs.androidx.compose.bom` - Compose Bill of Materials
- `libs.androidx.material3` - Material 3 components

### Kotlin Configuration
- **JVM Target**: Java 11
- **Compose Compiler**: Kotlin 2.0.21 with Compose plugin

## Development Workflow

### Adding Dependencies
Dependencies are managed in `gradle/libs.versions.toml`. Add versions in `[versions]`, libraries in `[libraries]`, and reference them in `app/build.gradle.kts` using `implementation(libs.library.name)`.

### Creating Composables
All UI is built with Jetpack Compose. Follow the existing pattern in `MainActivity.kt`:
- Use `@Composable` annotation
- Add `@Preview` for Android Studio preview
- Apply theme using `GitDashTheme { }`
- Use `Modifier` for styling

### Theme Customization
Theme files are in `app/src/main/java/com/sigmotoa/gitdash/ui/theme/`:
- **Color.kt**: Define color values (currently uses Purple/Pink palette)
- **Theme.kt**: Configure light/dark schemes, dynamic color (Android 12+)
- **Type.kt**: Typography scale configuration

### Testing
- **Unit tests**: Place in `src/test/java/` - use JUnit
- **Instrumented tests**: Place in `src/androidTest/java/` - use Espresso and Compose testing APIs
- Test dependencies are configured with `testImplementation` and `androidTestImplementation`

## Build Configuration Notes

- **Min SDK**: 35 (Android 12+)
- **Target SDK**: 36
- **Compile SDK**: 36
- **ProGuard**: Disabled in debug, available for release builds
- **Namespace**: Uses new Android namespace declaration (no applicationId in manifest)