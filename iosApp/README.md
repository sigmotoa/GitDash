# iosApp

App iOS de GitDash. Es una cáscara SwiftUI mínima que muestra la UI de Compose
Multiplatform compartida (`GitDashApp`), igual que `MainActivity` en Android.

## Estructura

```
iosApp/
├── iosApp.xcodeproj/        proyecto Xcode
├── iosApp/
│   ├── iOSApp.swift         @main — App SwiftUI
│   ├── ContentView.swift    envuelve MainViewControllerKt.MainViewController()
│   ├── Info.plist
│   └── Assets.xcassets/     icono y color de acento (vacíos, hay que rellenarlos)
└── Configuration/
    └── Config.xcconfig      TEAM_ID / BUNDLE_ID / APP_NAME
```

El punto de entrada Kotlin está en
`app/src/iosMain/kotlin/com/sigmotoa/gitdash/MainViewController.kt`, exportado por
el framework `GitDashKit` (definido en `app/build.gradle.kts`).

## Cómo ejecutar (requiere Mac + Xcode)

1. Rellena `Configuration/Config.xcconfig`:
   - `TEAM_ID` = tu Apple Developer Team ID (10 caracteres). Para el simulador
     puede quedar vacío; para dispositivo real es obligatorio.
2. Abre `iosApp/iosApp.xcodeproj` en Xcode.
   - Si Xcode se queja del formato del proyecto, deja que lo "actualice" y guarda.
3. La fase de build **Compile Kotlin Framework** ejecuta
   `./gradlew :app:embedAndSignAppleFrameworkForXcode`, que compila `GitDashKit`
   para la arquitectura/SDK que pida Xcode y lo copia al bundle.
4. Elige un simulador de iPhone y pulsa **Run** (⌘R).

## Notas

- `IPHONEOS_DEPLOYMENT_TARGET` = 15.0 (mínimo de Compose Multiplatform 1.7).
- La primera compilación descarga el toolchain de Kotlin/Native y Skiko: tarda.
- Funcionalidades aún no portadas a iOS (devuelven stub): generación de PDF
  (`ReportGenerator`), banner y anuncio recompensado de AdMob. La app funciona;
  esos botones simplemente no hacen nada visible todavía.
- Alternativa a este `.xcodeproj` hecho a mano: regenerarlo con el asistente
  *Kotlin Multiplatform* de Android Studio / el wizard de kmp.jetbrains.com y
  copiar `MainViewController.kt` + los dos `.swift`.
