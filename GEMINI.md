# Kanade Project Documentation

## 1. Purpose
**Kanade** is a high-performance music player for Android designed with a focus on clean architecture, modern UI, and robust playback capabilities. It provides a unified experience for local music storage with advanced features like:
- **Rich Lyric Support**: Parsing and displaying LRC (Standard/Enhanced) and TTML (Apple style) lyrics, including word-by-word sync, translations, and smooth Karaoke-style horizontal filling effects.
- **Background Playback**: Leveraging Android Media3 to ensure seamless audio sessions across the system.
- **Modern UI/UX**: A reactive interface built with Jetpack Compose following Material 3 guidelines, featuring a floating MiniPlayer, a fullscreen immersive player with rich lyric sync and an Apple Music-style playlist (Up Next) view.
- **Search Capability**: Integrated search for local music with history support and debounced querying.
- **Multi-language Support**: Full internationalization support, currently including English and Simplified Chinese.

## 2. Tech Stack
- **Language**: [Kotlin](https://kotlinlang.org/) (JVM 17)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/compose) with Material Design 3
- **Audio Engine**: [Android Media3](https://developer.android.com/guide/topics/media/media3) (ExoPlayer + MediaSession)
- **Architecture**: **MVI (Model-View-Intent)** + Clean Architecture
- **Concurrency**: Kotlin Coroutines & Flow
- **Image Loading**: [Coil](https://coil-kt.github.io/coil/)
- **Minimum SDK**: API 26 (Android 8.0)
- **Target SDK**: API 36 (Android 16)

## 3. File Directory & Responsibilities
```text
app/src/main/java/org/parallel_sekai/kanade/
├── data/
│   ├── model/                  # Data classes for music, lyrics, etc.
│   ├── parser/                 # Lyric parsers (LRC, TTML)
│   ├── repository/
│   │   ├── PlaybackRepository.kt   # Bridges UI with Media3; manages playback lifecycle and state flows
│   │   └── SettingsRepository.kt   # Manages user preferences and search history
│   ├── source/
│   │   ├── IMusicSource.kt         # Interface for music data providers
│   │   └── local/
│   │       └── LocalMusicSource.kt # MediaStore implementation; handles file scanning and lyric fetching
│   └── utils/                  # Shared utilities (LyricSplitter, etc.)
├── service/
│   └── KanadePlaybackService.kt    # Media3 MediaSessionService for robust background playback
├── ui/
│   ├── preview/                # Fake implementations for Compose Previews
│   ├── screens/
│   │   ├── library/                # UI for browsing music library
│   │   ├── search/                 # UI and logic for searching music
│   │   ├── more/                   # More screen with settings entry
│   │   ├── settings/               # Settings screen
│   │   └── player/                 # MVI components for player and lyrics
│   └── theme/                      # Material 3 Design system (Colors, Typography, Theme, Dimens)
└── MainActivity.kt                 # Entry point, permission handling, and navigation
```

## 4. Dependencies & Modules
| Library | Role |
| :--- | :--- |
| `androidx.media3:media3-exoplayer` | Core audio playback engine. |
| `androidx.media3:media3-session` | Manages system-wide media controls and background service. |
| `androidx.compose.material3` | Modern UI components and design system. |
| `io.coil-kt:coil-compose` | Async image loading for album art. |
| `com.google.accompanist:accompanist-permissions` | Reactive storage permission handling. |
| `androidx.navigation:navigation-compose` | Declarative UI navigation. |

## 5. Naming & Coding Conventions
- **Naming Style**: Standard Kotlin conventions (PascalCase for classes, camelCase for variables/functions).
- **MVI Pattern**:
    - **State**: Immutable `PlayerState` observed by the UI via `StateFlow`.
    - **Intent**: Sealed interface `PlayerIntent` representing user actions.
    - **Effect**: One-time events (e.g., `ShowError`) via `SharedFlow`.
- **Clean Architecture**: Strict separation between Data (`IMusicSource`), Domain/Logic (`PlaybackRepository`), and Presentation (`ViewModel`/`Compose`).
- **Resources**: Avoid hardcoded strings and dimensions. Use `stringResource()` and `Dimens` object.

## 6. Core Classes & Functions Index
- `PlaybackRepository`: The source of truth for playback state. Exposes `isPlaying`, `currentMediaId`, and a high-frequency `progressFlow` for smooth UI updates.
- `LrcParser` & `TtmlParser`: Specialized parsers for handling various lyric formats. Supports `WordInfo` for granular word-by-word animation.
- `PlaylistContent`: Displays the current playback queue with smooth transitions, allowing users to browse and select songs directly from the fullscreen player.
- `PlayerViewModel`: Manages the current playlist, handles playback intents, and fetches/parses lyrics when the track changes.
- `SearchViewModel`: Handles debounced search queries and manages search history persistence.
- `LocalMusicSource`: Uses `ContentResolver` to query `MediaStore`. Attempts to find `.lrc` or `.ttml` files in the same directory as the audio file.
- `LyricGetterManager`: Unified manager for external lyric broadcasting. Supports both `Lyric-Getter-API` and `SuperLyricApi`, providing translations and word-by-word sync to system-wide providers.

## 4. Dependencies & Modules

## 7. Implementation Logic
1. **Startup**: `MainActivity` requests media permissions. `PlayerViewModel` initializes and refreshes the library via `PlaybackRepository`.
2. **Playback Initiation**: When a user selects a song, `PlaybackRepository` builds a `MediaItem` list, sets it in `MediaController`, and begins playback.
3. **Gesture-driven Expansion**: 
    - `KanadePlayerContainer` uses `AnchoredDraggable` to handle swipe-to-expand gestures.
    - Transitions are driven by a continuous `expansionFraction` (0.0 to 1.0), enabling "hand-following" morphing between MiniPlayer and FullScreen views.
    - Key UI elements (album art, text) use linear interpolation (lerp) for smooth positioning and scaling during drag.
3. **Lyric & Playlist Syncing**: 
    - `PlayerViewModel` observes `currentMediaId`.
    - Upon change, it fetches lyrics and extracts theme colors from the album art via `Palette`.
    - The UI highlights the active `LyricLine` based on `progressFlow`.
    - `PlaylistContent` allows users to view and interact with the current queue, supporting shuffle and repeat modes.
4. **Search Flow**: 
    - `SearchViewModel` uses `debounce` to minimize unnecessary queries.
    - Results are displayed in a standard `MusicItem` list.
    - Search history is persisted using `DataStore`.
5. **State Persistence**: `MediaController` ensures that playback state is synchronized between the UI and the `KanadePlaybackService` even when the app is backgrounded.
6. **Predictive Back**: The app supports Android 14+ predictive back gestures. The `NavHost` handles screen transitions, and the `KanadePlayerContainer` implements a custom `PredictiveBackHandler` to provide visual feedback (scaling and offset) when dismissing the full-screen player.
7. **Album Optimization**:
    - **Smart Artist Calculation**: The album list and detail page now calculate the common artists across all tracks dynamically. If all tracks share the same artist set, the artist name is used; otherwise (e.g. compilations), "Various Artists" is displayed.
    - **Clean List**: Album detail view hides the individual track cover art for a cleaner, text-focused list style.

## Current Status
- [x] Basic playback functionality with Media3.
- [x] Local music scanning and library display.
- [x] Rich lyric support (LRC/TTML).
- [x] Search Page implementation with history support.
- [x] Album detail page optimization (smart artist display, hidden track covers).
- [x] Lyric-Getter-API & SuperLyricApi integration for system-wide lyric sharing.
- [x] CI Setup (GitHub Actions).
- [x] Multi-language support (EN/ZH).
- [x] Playback state persistence (playlist, track, position, modes).
- [x] Code structure and file organization optimization.
- [x] Show full-screen player when clicking media notification.
- [x] Redesigned playlist mode buttons in full-screen player for better UI consistency.
- [x] Improved robustness for lyric sharing (SuperLyricApi integration).
- [x] Enhanced permission handling with user feedback.

## 8. Agent Development Instructions (AI Context)
- **State Management**: Always use `MutableStateFlow` in ViewModels. UI must be stateless and react only to the `state` flow.
- **Lyric Handling**: When extending lyric features, ensure compatibility with both `LrcParser` and `TtmlParser`. Use `LyricUtils.parseTimestamp` for consistent time handling.
- **Repository Pattern**: Never interact with `MediaController` directly in the UI. All actions must be encapsulated in `PlaybackRepository`.
- **UI Consistency**: 
    - Use `MaterialTheme.colorScheme` and `MaterialTheme.typography`.
    - Use `Dimens` object for all dimensions.
    - MiniPlayer must maintain transparency and rounded corners as defined in `PlayerComponents.kt`.
- **Testing**:
    - Logic: `app/src/test` (JUnit).
    - UI: `app/src/androidTest` (Compose Test Rule).
- **Verification**: Always run `./gradlew assembleDebug` to verify changes.