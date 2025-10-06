# GitDash 📱

A modern Android application built with Jetpack Compose and Material 3 that allows users to explore GitHub profiles and repositories.

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)

## ✨ Features

- **GitHub Profile Viewer**: Search and view detailed GitHub user profiles
  - User avatar, name, username, and bio
  - Company, location, and website information
  - Statistics: public repositories, followers, and following counts

- **Repository Explorer**: Browse user's public repositories
  - Repository name and description
  - Language badges with authentic GitHub colors
  - Stars and forks count with formatted numbers
  - Pull-to-refresh functionality

- **Modern UI/UX**:
  - Material 3 Design with dynamic theming
  - Dark and light theme support
  - Bottom navigation for easy screen switching
  - Smooth animations and transitions
  - Loading, error, and empty states

## 🏗️ Architecture

The app follows **MVVM (Model-View-ViewModel)** architecture pattern with clean separation of concerns:

```
app/
├── data/
│   ├── model/          # Data models
│   ├── remote/         # API service and Retrofit configuration
│   └── repository/     # Repository layer for data operations
├── ui/
│   ├── screen/         # Composable screens
│   ├── theme/          # Material 3 theming
│   ├── utils/          # UI utilities
│   └── viewmodel/      # ViewModels for state management
└── MainActivity.kt     # Entry point with navigation
```

## 🛠️ Tech Stack

### Core
- **Kotlin** - Primary programming language
- **Jetpack Compose** - Modern declarative UI framework
- **Material 3** - Latest Material Design components

### Architecture & Lifecycle
- **MVVM Architecture** - Clean separation of concerns
- **ViewModel** - Lifecycle-aware state management
- **StateFlow** - Reactive state handling
- **Coroutines** - Asynchronous programming

### Networking
- **Retrofit 2** - HTTP client for REST API calls
- **OkHttp** - Network interceptor and logging
- **Kotlinx Serialization** - JSON parsing

### Image Loading
- **Coil 3** - Image loading library optimized for Compose

### Navigation
- **Navigation Compose** - Type-safe navigation for Compose

### Minimum Requirements
- **Min SDK**: 35 (Android 12+)
- **Target SDK**: 36
- **Compile SDK**: 36
- **JVM Target**: Java 11

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 11 or higher
- Android device or emulator running Android 12+

### Installation

1. Clone the repository:
```bash
git clone https://github.com/sigmotoa/gitdash.git
cd gitdash
```

2. Open the project in Android Studio

3. Sync Gradle files:
```bash
./gradlew build
```

4. Run the app:
```bash
./gradlew installDebug
```

Or click the Run button in Android Studio

## 📋 Usage

1. **Search for a User**:
   - Open the Profile or Repositories tab
   - Enter a GitHub username in the search field
   - Tap "Search" or "Load" to fetch data

2. **View Profile**:
   - Navigate to the Profile tab
   - View user details, stats, and information

3. **Browse Repositories**:
   - Navigate to the Repositories tab
   - Scroll through the user's public repositories
   - Pull down to refresh the list

## 🎨 Screenshots

<!-- Add screenshots here -->
```
[Profile Screen]  [Repository List]  [Dark Theme]
```

## 🔧 Configuration

The app uses the GitHub REST API v3. No API key is required for public data, but you may hit rate limits for unauthenticated requests.

To add authentication (optional):
1. Generate a GitHub Personal Access Token
2. Add it to `RetrofitInstance.kt` as a header interceptor

## 📦 Build Variants

- **Debug**: Development build with logging enabled
- **Release**: Production build with ProGuard optimization

Build the release APK:
```bash
./gradlew assembleRelease
```

## 🧪 Testing

Run unit tests:
```bash
./gradlew test
```

Run instrumented tests:
```bash
./gradlew connectedAndroidTest
```

## 📝 Project Structure

### Key Files
- `MainActivity.kt` - App entry point and navigation setup
- `GitHubViewModel.kt` - State management and business logic
- `GitHubRepository.kt` - Data repository layer
- `GitHubApiService.kt` - Retrofit API interface
- `ProfileScreen.kt` - User profile UI
- `RepositoryListScreen.kt` - Repository list UI

### Dependencies Management
Dependencies are managed using Gradle Version Catalog:
- `gradle/libs.versions.toml` - Version catalog
- `build.gradle.kts` - Root build configuration
- `app/build.gradle.kts` - App module configuration

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [GitHub REST API](https://docs.github.com/en/rest) for providing the data
- [Jetpack Compose](https://developer.android.com/jetpack/compose) for the modern UI toolkit
- [Material Design 3](https://m3.material.io/) for design guidelines
- Language colors inspired by [GitHub's linguist](https://github.com/github/linguist)

## 📧 Contact

Your Name - [@sigmotoa](https://github.com/sigmotoa)

Project Link: [https://github.com/sigmotoa/gitdash](https://github.com/sigmotoa/gitdash)

---

**Made with ❤️ using Jetpack Compose**