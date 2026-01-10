# Kanade Project Documentation

## 1. Purpose
**Kanade** is a high-performance music player for Android designed with a focus on clean architecture and extensibility. It aims to provide a unified playback experience across different music sources, including local storage and future scripted third-party providers. The project leverages modern Android technologies to ensure smooth background playback, system-wide media controls, and a reactive user interface.

## 2. Tech Stack
- **Language**: [Kotlin](https://kotlinlang.org/) (JVM 17)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/compose) with Material Design 3
- **Audio Engine**: [Android Media3](https://developer.android.com/guide/topics/media/media3) (ExoPlayer + MediaSession)
- **Architecture**: **MVI (Model-View-Intent)** + Clean Architecture
- **Concurrency**: Kotlin Coroutines & Flow
- **Image Loading**: [Coil](https://coil-kt.github.io/coil/)
- **Minimum SDK**: API 24 (Android 7.0)
- **Target SDK**: API 36 (Android 16)

## 3. File Directory & Responsibilities
```text
app/src/main/java/org/parallel_sekai/kanade/
├── data/
│   ├── repository/
│   │   └── PlaybackRepository.kt   # Bridges UI logic with Media3 MediaController; manages playback lifecycle
│   └── source/
│       ├── IMusicSource.kt         # Core interface for music data providers (Local, Scripted, etc.)
│       └── local/
│           └── LocalMusicSource.kt # MediaStore implementation for local audio files
├── service/
│   └── KanadePlaybackService.kt    # Media3 MediaSessionService for robust background playback
├── ui/
│   ├── screens/
│   │   ├── library/                # UI for browsing music library
│   │   └── player/                 # MVI components for player controls and UI
│   │       ├── PlayerContract.kt   # Defines State, Intent, and Effect for the player
│   │       ├── PlayerViewModel.kt  # Handles intents and manages UI state via flows
│   │       └── PlayerComponents.kt # Stateless UI components for the player
│   └── theme/                      # Material 3 Design system (Colors, Typography, Theme)
└── MainActivity.kt                 # Entry point, permission handling, and navigation setup
```

## 4. Dependencies & Modules
| Library                                          | Purpose                                                               |
| :----------------------------------------------- | :-------------------------------------------------------------------- |
| `androidx.media3:media3-exoplayer`               | Core audio playback engine implementation.                            |
| `androidx.media3:media3-session`                 | Handles background service and system media controls synchronization. |
| `androidx.compose.material3`                     | Modern UI components following Material Design 3.                     |
| `io.coil-kt:coil-compose`                        | Asynchronous image loading for album artwork.                         |
| `com.google.accompanist:accompanist-permissions` | Reactive permission handling for storage access.                      |
| `androidx.navigation:navigation-compose`         | Declarative UI navigation within the app.                             |

## 5. Naming & Coding Conventions
- **Naming Style**: Standard Kotlin conventions (PascalCase for classes, camelCase for variables/functions).
- **MVI Pattern**:
    - **State**: Immutable `PlayerState` object observed by the UI.
    - **Intent**: Sealed interface `PlayerIntent` representing user actions.
    - **Effect**: One-time events (e.g., showing a snackbar) via `PlayerEffect`.
- **Clean Architecture**: Separation of concerns between the data layer (`IMusicSource`), domain/logic layer (`PlaybackRepository`), and presentation layer (`ViewModel`/`Compose`).

## 6. Core Classes & Functions Index
- `IMusicSource`: Interface for fetching `MusicModel` lists and play URLs. Designed for future script-based extensibility.
- `PlaybackRepository`: The central control point. Wraps `MediaController`, exposes `isPlaying` and `progressFlow`, and handles playlist setup.
- `PlayerViewModel`: Orchestrates the MVI flow. Transforms `PlayerIntent` into repository actions and updates `PlayerState`.
- `KanadePlaybackService`: Background service that hosts the `MediaSession`, ensuring audio persists when the app is in the background.

## 7. Implementation Logic
1. **Startup**: `MainActivity` checks for media permissions. `PlayerViewModel` triggers a `RefreshList` intent on initialization.
2. **Scanning**: `LocalMusicSource` queries the Android `MediaStore` to build a list of `MusicModel` objects.
3. **Playback**: When a user selects a song (`SelectSong` intent), `PlaybackRepository` converts the model to `MediaItem`, sets the playlist in `MediaController`, and starts playback.
5. **UI Synchronization**: The UI observes `isPlaying`, `currentMediaId`, and `progressFlow` from the repository to update the MiniPlayer and Fullscreen player in real-time. The MiniPlayer is implemented as a floating overlay above the main navigation content to ensure proper transparency and visual consistency with rounded corners.
5. **Backgrounding**: `KanadePlaybackService` keeps the player active and provides controls via the notification shade and lock screen.

## 8. Agent Development Instructions (AI Context)
- **State Management**: Always use `MutableStateFlow` in ViewModels. UI must be stateless and react only to the `state` flow.
- **Error Handling**: Use `PlayerEffect` (e.g., `ShowError`) for transient errors. Repository methods should return results or throw specific exceptions handled by the ViewModel.
- **Repository Pattern**: Never interact with `MediaController` or `ExoPlayer` directly in the UI layer. All playback logic must go through `PlaybackRepository`.
- **UI Consistency**: Strictly use `MaterialTheme` color schemes and typography defined in the `ui.theme` package. Avoid hardcoded dimensions or colors.
- **Testing**:
    - Business logic: `app/src/test` using JUnit 4/5.
    - UI components: `app/src/androidTest` using Compose Test Rule.
- **Verification**: After code modifications, always execute `./gradlew installDebug` to install and launch the application for verification.
- **Building & Running on Device**:
    Use the following command to build, install, and run the app: ./gradlew assembleDebug && adb -s 13pro:5555 install app/build/outputs/apk/debug/app-debug.apk && adb -s 13pro:5555 shell am start -n org.parallel_sekai.kanade/.MainActivity
