# GitDash 📱

A modern **Kotlin Multiplatform** application built with **Compose Multiplatform** and Material 3 for exploring developer profiles and repositories across **GitHub and GitLab** — all in one place, on **Android and iOS**.

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![iOS](https://img.shields.io/badge/iOS-000000?style=for-the-badge&logo=apple&logoColor=white)
![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin%20Multiplatform-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Version](https://img.shields.io/badge/version-4.0-blue?style=for-the-badge)

---

## ✨ Features

### 👤 Multi-Platform Profile Viewer
Search and view developer profiles on both **GitHub** and **GitLab** from a single search bar.
- Avatar, name, username, bio, company, location, and website
- Followers, following, and public repository counts
- Platform badge indicating GitHub or GitLab

### 📊 Contribution Activity Heatmap
A native-style contribution graph showing activity over the **last ~4 months**.
- 18-week × 7-day color grid (5 intensity levels)
- Month and day-of-week labels
- Activity totals: **Commits · PRs · Issues · Comments · Other**
- Powered by the public Events API — no authentication required

### 📄 PDF Profile Report
Export a shareable **A4 PDF report** of any developer profile directly from the TopBar.
- Username, followers, following, stars, public repos
- Recent commit count and most-active repository
- Last worked-on repository and top 3 programming languages
- Generated date — rendered natively per platform (Android `PdfDocument`, iOS `UIGraphicsPDFRenderer`), no shared external PDF library needed

### 📖 Markdown README Viewer
Read repository READMEs rendered as rich Markdown inside the app.
- Full Markdown support: headers, tables, strikethrough, links, code blocks
- **SVG and PNG badge/icon rendering** (shields.io, devicons, vectorlogo.zone)
- Aspect-ratio-preserving image sizing
- Powered by [multiplatform-markdown-renderer](https://github.com/mikepenz/multiplatform-markdown-renderer) + Coil 3

### 🗂️ Repository Explorer
Browse a user's public repositories with key metadata at a glance.
- Repository name, description, and primary language badge
- Stars, forks, and last-updated timestamp
- Tap to view the full README

### 🔔 Automatic Version Check
The app checks for newer versions on launch and notifies the user with a dismissible dialog.
- Version info served from a remote `version.json`
- Supports mandatory and optional update prompts

### 🎨 Modern UI/UX
- Material 3 with dynamic color (Android 12+)
- Dark and light theme support
- Bottom navigation bar
- Skeleton loading, error, and empty states throughout
- Shared UI code renders natively on both platforms via Compose Multiplatform — no duplicated screens

---

## 🗺️ App Evolution

| Version | Highlights |
|---------|-----------|
| **4.0** | Kotlin Multiplatform migration — shared data/UI layer, native iOS app (Compose Multiplatform, Ktor, AdMob via Swift bridge, App Tracking Transparency), published to TestFlight/App Store |
| **3.0** | Contribution heatmap, PDF report, rewarded ad, SVG badge support |
| **2.1** | Hotfix: GitLab user search returning 404 |
| **2.0** | Multi-platform (GitHub + GitLab), Markdown README viewer, version check system |
| **1.0** | Initial release — GitHub profile and repository viewer |

---

## 🏗️ Architecture

The app follows **MVVM** with a shared, layered Kotlin Multiplatform architecture. Almost everything — data, networking, ViewModel, and UI — lives in `commonMain` and compiles natively to both Android and iOS; only true platform boundaries (Context, UIKit, ads SDKs, PDF rendering APIs) are split via `expect`/`actual`.

```
app/src/
├── commonMain/kotlin/com/sigmotoa/gitdash/
│   ├── data/
│   │   ├── model/               # UnifiedUser, UnifiedRepo, ContributionData, Platform…
│   │   ├── remote/               # GitHubApiService, GitLabApiService (Ktor)
│   │   └── repository/           # UnifiedRepository — single source of truth for both platforms
│   ├── platform/                 # expect PlatformInfo (os/app version)
│   ├── ui/
│   │   ├── App.kt                 # GitDashApp() — nav host, bottom bar, update dialog
│   │   ├── components/            # SearchBar, PieChart, ContributionGraph, AdMobBanner (expect)…
│   │   ├── platform/               # expect FileSharer, RewardedAd, Dispatchers
│   │   ├── report/                 # PdfCanvas (expect) + shared ReportGenerator layout
│   │   ├── screen/                 # ProfileScreen, RepositoryListScreen, StatsScreen…
│   │   ├── theme/                  # Color, Type, Theme (Material 3)
│   │   ├── utils/
│   │   └── viewmodel/              # GitHubViewModel + GitHubUiState
│   └── version/                  # VersionCheckManager
├── androidMain/kotlin/…           # MainActivity, InterstitialAdManager, actuals (PlatformInfo, AdMobBanner, FileSharer, RewardedAd, PdfCanvas…)
└── iosMain/kotlin/…               # MainViewController (ComposeUIViewController), IosAdBridge, actuals

iosApp/                            # Hand-authored Xcode project (SwiftUI shell)
├── iosApp/Ads.swift                # Google Mobile Ads (SPM) + ATT, feeds IosAdBridge factories
├── iosApp/Info.plist               # SKAdNetworkItems, ATT usage string, AdMob keys (from xcconfig)
├── iosApp/PrivacyInfo.xcprivacy    # Apple privacy manifest
└── Configuration/                  # Config.xcconfig (Team ID, Bundle ID, public AdMob defaults)
                                     # + Secrets.xcconfig (gitignored — real AdMob IDs)
```

### Key Design Decisions
- **`UnifiedRepository`** abstracts the differences between the GitHub REST API v3 and GitLab REST API v4 behind a single interface, returning platform-agnostic `UnifiedUser` and `UnifiedRepo` models.
- **`ContributionData`** bundles the date heatmap, event-category counts, and top-pushed repo into a single result, fetched once per profile load.
- **`ReportGenerator`** shares the entire PDF layout logic in `commonMain`; only the low-level drawing calls are `expect`/`actual` (`PdfCanvas`) — Android draws with `PdfDocument`/`Canvas`, iOS with `UIGraphicsPDFRenderer`/CoreText.
- **Ads are bridged, not shared**: Google's Mobile Ads SDK has no Kotlin Multiplatform artifact, so `commonMain` only declares `expect fun rememberRewardedAdController()` / `AdMobBanner`; the iOS `actual` calls into `IosAdBridge`, a pair of factory closures wired from Swift (`Ads.swift`) via Swift Package Manager — this keeps the Kotlin framework build completely decoupled from CocoaPods.
- **`MarkdownText`** wraps the shared `multiplatform-markdown-renderer` composable with a Coil 3 image transformer, rendering identically on both platforms.

---

## 🛠️ Tech Stack

### Core
| Library | Purpose |
|---------|---------|
| **Kotlin Multiplatform 2.1** | Shared language & compiler across Android + iOS |
| **Compose Multiplatform 1.7** | Declarative UI shared across both platforms |
| **Material 3** | Design system + dynamic color (Android) |
| **Coroutines + StateFlow** | Async state management |
| **kotlinx-datetime** | Cross-platform date/time (replaces `java.time` in shared code) |

### Networking
| Library | Purpose |
|---------|---------|
| **Ktor Client 3** | Multiplatform HTTP client |
| **ktor-client-okhttp** / **ktor-client-darwin** | Platform engines (Android / iOS) |
| **Kotlinx Serialization** | JSON deserialization |

### UI & Rendering
| Library | Purpose |
|---------|---------|
| **Coil 3** | Image loading (Compose-native, multiplatform) |
| **multiplatform-markdown-renderer** | Markdown → Compose rendering, shared |
| **Android `PdfDocument`** / **iOS `UIGraphicsPDFRenderer`** | Native per-platform PDF generation behind a shared layout |
| **Google Mobile Ads SDK** | AdMob banner + rewarded ads (Android native dep; iOS via Swift Package Manager, bridged into Kotlin) |

### Navigation & DI
| Library | Purpose |
|---------|---------|
| **org.jetbrains.androidx.navigation** | Compose Multiplatform-aligned Navigation, shared |
| **Manual DI** | Repository + ViewModel wiring in `MainActivity` / `MainViewController` |

### Minimum Requirements
- **Android**: Min SDK 26, Target/Compile SDK 36, JVM Target 11
- **iOS**: Deployment target 15.6, Xcode 16+, Swift 5

---

## 🚀 Getting Started

### Prerequisites
- **Android**: Android Studio Hedgehog+, JDK 11+, Android device/emulator 8.0+
- **iOS**: a Mac with Xcode 16+, CocoaPods **not** required (Swift Package Manager only), iOS 15.6+ device/simulator

### Installation

```bash
git clone https://github.com/sigmotoa/GitDash.git
cd GitDash
git checkout KMP
```

### Android

```bash
# Build
./gradlew assembleDebug

# Install on connected device/emulator
./gradlew installDebug
```

### iOS

The Kotlin shared framework (`GitDashKit`) is built by Gradle and embedded automatically by Xcode's "Compile Kotlin Framework" build phase — no manual step needed.

```bash
open iosApp/iosApp.xcodeproj
```
Or from the CLI:
```bash
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -sdk iphonesimulator -destination 'id=<simulator-udid>' build
```

Before building for iOS, copy `iosApp/Configuration/Secrets.xcconfig.example` to `Secrets.xcconfig` and fill in real AdMob IDs (optional — falls back to Google's public test IDs otherwise), and set `TEAM_ID`/`BUNDLE_ID` in `Config.xcconfig` for your own Apple Developer account.

---

## 📋 Usage

### Search a profile
1. Open the **Profile** tab
2. Select **GitHub** or **GitLab** using the platform toggle
3. Type a username and tap **Search**

### View activity heatmap
The contribution grid loads automatically below the profile stats — darker cells indicate activity intensity over the last ~4 months.

### Export a PDF report
1. With a profile loaded, tap the **Share (↑)** icon in the TopBar
2. Watch the short rewarded ad
3. The PDF is generated and the system share sheet opens automatically

### Browse repositories
1. Open the **Repositories** tab
2. Scroll through public repos; tap any to open the README viewer

---

## 📦 Build Variants

| Variant | Notes |
|---------|-------|
| `debug` | Logging enabled, test AdMob IDs as fallback |
| `release` | ProGuard available (Android); real AdMob IDs via `local.properties` (Android) or `Secrets.xcconfig` (iOS) |

```bash
./gradlew assembleRelease
```

---

## 🧪 Testing

Shared logic is tested once in `commonTest` and runs on **both** the JVM (Android) and Kotlin/Native (iOS) test runners.

```bash
# Shared + Android unit tests (JVM)
./gradlew :app:testDebugUnitTest

# Shared tests on the iOS Simulator (Kotlin/Native)
./gradlew :app:iosSimulatorArm64Test

# Lint
./gradlew lint

# Instrumented tests (requires Android device/emulator)
./gradlew connectedAndroidTest
```

> Note: PDF generation tests live in `iosTest` rather than `commonTest`, since `android.graphics` is stubbed (not mocked) in local JVM unit tests.

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit your changes: `git commit -m "feat: add my feature"`
4. Push: `git push origin feature/my-feature`
5. Open a Pull Request targeting `main`

---

## 📄 License

This project is licensed under the MIT License — see [LICENSE](LICENSE) for details.

---

## 🙏 Acknowledgments

- [GitHub REST API v3](https://docs.github.com/en/rest)
- [GitLab REST API v4](https://docs.gitlab.com/ee/api/rest/)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Ktor](https://ktor.io/)
- [Material Design 3](https://m3.material.io/)
- [multiplatform-markdown-renderer](https://github.com/mikepenz/multiplatform-markdown-renderer) by @mikepenz
- Language colors inspired by [GitHub Linguist](https://github.com/github/linguist)

---

## 📧 Contact

[@sigmotoa](https://github.com/sigmotoa) · [GitDash on GitHub](https://github.com/sigmotoa/GitDash)

---

**Made with ❤️ using Kotlin Multiplatform & Compose Multiplatform**
