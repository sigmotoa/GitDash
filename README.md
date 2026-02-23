# GitDash 📱

A modern Android application built with Jetpack Compose and Material 3 for exploring developer profiles and repositories across **GitHub and GitLab** — all in one place.

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Version](https://img.shields.io/badge/version-3.0-blue?style=for-the-badge)

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
- Generated date — no external PDF library needed (Android `PdfDocument`)

### 📖 Markdown README Viewer
Read repository READMEs rendered as rich Markdown inside the app.
- Full Markdown support: headers, tables, strikethrough, links, code blocks
- **SVG and PNG badge/icon rendering** (shields.io, devicons, vectorlogo.zone)
- Aspect-ratio-preserving image sizing
- Powered by [Markwon](https://github.com/noties/Markwon)

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

---

## 🗺️ App Evolution

| Version | Code | Highlights |
|---------|------|-----------|
| **3.0** | 6 | Contribution heatmap, PDF report, rewarded ad, SVG badge support |
| **2.1** | 5 | Hotfix: GitLab user search returning 404 |
| **2.0** | 3–4 | Multi-platform (GitHub + GitLab), Markdown README viewer, version check system |
| **1.0** | 1–2 | Initial release — GitHub profile and repository viewer |

---

## 🏗️ Architecture

The app follows **MVVM** with a clean layered architecture:

```
app/src/main/java/com/sigmotoa/gitdash/
├── data/
│   ├── model/               # UnifiedUser, UnifiedRepo, ContributionData, Platform…
│   ├── remote/              # GitHubApiService, GitLabApiService (Retrofit interfaces)
│   └── repository/          # UnifiedRepository — single source of truth for both platforms
├── ui/
│   ├── components/          # AdMobBanner, ContributionGraph, GitHubSearchBar, MarkdownText
│   ├── screen/              # ProfileScreen, RepositoryListScreen, RepoDetailScreen
│   ├── theme/               # Color, Type, Theme (Material 3)
│   ├── util/                # ProfileReportGenerator (PDF)
│   └── viewmodel/           # GitHubViewModel + GitHubUiState
└── MainActivity.kt          # Navigation host
```

### Key Design Decisions
- **`UnifiedRepository`** abstracts the differences between the GitHub REST API v3 and GitLab REST API v4 behind a single interface, returning platform-agnostic `UnifiedUser` and `UnifiedRepo` models.
- **`ContributionData`** bundles the date heatmap, event-category counts, and top-pushed repo into a single result, fetched once per profile load.
- **`ProfileReportGenerator`** uses Android's built-in `android.graphics.pdf.PdfDocument` drawn with `Canvas` — zero external dependencies for PDF generation.
- **`MarkdownText`** wraps a `TextView` inside `AndroidView` with a lifecycle-aware Markwon instance (cached with `remember`) to avoid recreating the renderer on every recomposition.

---

## 🛠️ Tech Stack

### Core
| Library | Purpose |
|---------|---------|
| **Kotlin 2.0** | Primary language |
| **Jetpack Compose** | Declarative UI |
| **Material 3** | Design system + dynamic color |
| **Coroutines + StateFlow** | Async state management |

### Networking
| Library | Purpose |
|---------|---------|
| **Retrofit 2** | HTTP client |
| **OkHttp** | Interceptors & logging |
| **Kotlinx Serialization** | JSON deserialization |

### UI & Rendering
| Library | Purpose |
|---------|---------|
| **Coil 3** | Image loading (Compose-native) |
| **Markwon 4.6** | Markdown → TextView rendering |
| **markwon-image + androidsvg** | SVG badge & icon support |
| **Android `PdfDocument`** | PDF generation (built-in, no extra dep) |


### Navigation & DI
| Library | Purpose |
|---------|---------|
| **Navigation Compose** | Bottom-nav + back-stack |
| **Manual DI** | Repository + ViewModel wiring in `MainActivity` |

### Minimum Requirements
- **Min SDK**: 26 (Android 8.0 — needed for `java.time`)
- **Target / Compile SDK**: 36
- **JVM Target**: Java 11

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 11+
- Android device or emulator running Android 8.0+

### Installation

```bash
git clone https://github.com/sigmotoa/GitDash.git
cd GitDash
```

Open in Android Studio, then:

```bash
# Build
./gradlew assembleDebug

# Install on connected device/emulator
./gradlew installDebug
```

---

## 📋 Usage

### Search a profile
1. Tap the **Profile** tab
2. Select **GitHub** or **GitLab** using the platform toggle
3. Type a username and tap **Search**

### View activity heatmap
The contribution grid loads automatically below the profile stats — green cells indicate activity intensity over the last ~4 months.

### Export a PDF report
1. With a profile loaded, tap the **Share (↑)** icon in the TopBar
2. Watch the short rewarded ad
3. The PDF is generated and the system share sheet opens automatically

### Browse repositories
1. Tap the **Repositories** tab
2. Scroll through public repos; tap any to open the README viewer

---

## 📦 Build Variants

| Variant | Notes |
|---------|-------|
| `debug` | Logging enabled, test AdMob IDs as fallback |
| `release` | ProGuard available; use real AdMob IDs in `local.properties` |

```bash
./gradlew assembleRelease
```

---

## 🧪 Testing

```bash
# Unit tests
./gradlew test

# Lint
./gradlew lint

# Instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest
```

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
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Material Design 3](https://m3.material.io/)
- [Markwon](https://github.com/noties/Markwon) by @noties
- Language colors inspired by [GitHub Linguist](https://github.com/github/linguist)

---

## 📧 Contact

[@sigmotoa](https://github.com/sigmotoa) · [GitDash on GitHub](https://github.com/sigmotoa/GitDash)

---

**Made with ❤️ using Jetpack Compose**
