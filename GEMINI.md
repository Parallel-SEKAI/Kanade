# Kanade Project Documentation

## 1. Purpose
**Kanade** is a high-performance, modern music player for Android designed to provide a seamless experience for both local music and external media providers. Its core architecture focuses on extensibility and immersive UI.
- **Unified Playback**: Combines local `MediaStore` tracks with external sources via a custom scripting engine.
- **Advanced Lyrics**: Supports standard LRC, Enhanced LRC (word-by-word), and Apple-style TTML lyrics with smooth transitions and sharing capabilities.
- **Kanade Scripting System (KSS)**: A QuickJS-powered engine that allows developers to write JavaScript scripts to fetch music, search, and resolve streaming URLs from third-party services.
- **Modern UI/UX**: Built entirely with Jetpack Compose, featuring dynamic gradients (Palette API), immersive player transitions, and a "last-minute" resolution strategy to optimize network usage.

## 2. Tech Stack
- **Languages**: Kotlin (JVM 17), JavaScript (ES2015+ via QuickJS).
- **Core Frameworks**: Jetpack Compose (Material 3), Android Media3 (ExoPlayer + MediaSession).
- **Target Platform**: Android 8.0 (API 26) to Android 16 (API 36).
- **Minimum Runtime**: Android SDK 26, Gradle 8.13+.
- **Build System**: Kotlin DSL (`.gradle.kts`) with version catalogs (`libs.versions.toml`).

## 3. File Directory & Responsibilities
```text
app/src/main/java/org/parallel_sekai/kanade/
├── data/
│   ├── model/                  # Immutable Data Classes (Music, Lyric, Artist, Album)
│   ├── parser/                 # Parsers for LRC (Standard/Enhanced) and TTML formats
│   ├── repository/             # Domain Logic & State Management
│   │   ├── PlaybackRepository  # Media3 controller bridge; persists playback state
│   │   └── SettingsRepository  # User preferences via DataStore
│   ├── script/                 # Kanade Scripting System (KSS)
│   │   ├── ScriptEngine        # QuickJS runtime with Promise/Async/Bridge support
│   │   ├── HostBridge          # Kotlin-to-JS bridges (HTTP, Crypto, Logging)
│   │   ├── ScriptManager       # Script lifecycle, manifest parsing, and local storage
│   │   └── ScriptMusicSource   # Adapter converting JS script output to MusicModels
│   ├── source/                 # Data Sources
│   │   ├── IMusicSource        # Interface for local/script-based sources
│   │   ├── SourceManager       # Aggregates local and active script sources
│   │   └── local/              # Local MediaStore integration
│   └── utils/                  # Utilities (LyricSplitter, CacheManager, URL handling)
├── service/
│   └── KanadePlaybackService   # Media3 MediaSessionService; handles background playback
├── ui/
│   ├── screens/                # MVI UI Components
│   │   ├── player/             # Player View, Lyrics, and UI state (Contract/ViewModel)
│   │   ├── library/            # Local/Home browsing
│   │   ├── search/             # Multi-source debounced search
│   │   └── settings/           # Config for KSS, UI, and Lyric broadcasting
│   └── theme/                  # Theme definition, Dimens, and Material 3 palettes
└── MainActivity.kt             # Entry point, navigation, and permission handling
```

## 4. Dependencies & Modules
| Library | Role |
| :--- | :--- |
| `androidx.media3` | Comprehensive audio engine, session management, and UI controls. |
| `app.cash.quickjs` | Lightweight JS engine for executing KSS provider scripts. |
| `io.coil-kt:coil-compose` | Efficient image loading for album art. |
| `androidx.palette` | Color extraction from album art to drive dynamic UI gradients. |
| `androidx.datastore` | Type-safe preference storage for app settings and history. |
| `kotlinx.serialization` | JSON parsing for script communication and state persistence. |
| `Lyric-Getter-Api` | Integration for system-wide lyric broadcasting. |

## 5. Naming & Coding Conventions
- **MVI Pattern**: Every screen follows a strict `State`, `Intent`, `Effect` contract.
    - `PlayerState`: Immutable snapshot of the UI.
    - `PlayerIntent`: User actions (e.g., `PlayPause`, `SeekTo`).
    - `PlayerEffect`: One-time events (e.g., `ShowError`).
- **Clean Architecture**: Separation between Data (Sources), Domain (Repositories), and Presentation (ViewModels).
- **Naming**:
    - Classes: `PascalCase`.
    - Functions/Variables: `camelCase`.
    - Constants: `SCREAMING_SNAKE_CASE`.
- **Asynchronous Flow**: Heavy use of Kotlin `Coroutines` and `Flow`. UI observes `StateFlow`.

## 6. Core Classes & Functions Index
- `PlaybackRepository`: The "Source of Truth" for playback. Bridges the UI with `MediaController`. Handles `savePlaybackState` and `restorePlaybackState`.
- `ScriptEngine`: Manages a dedicated single-threaded JS runtime. Supports `callAsync` for Promise-based JS calls and provides `http`/`crypto` bridges to scripts.
- `LyricParserFactory`: Centralized factory that detects format (LRC/TTML) and returns the correct `LyricParser`.
- `KanadePlaybackService`: Extends `MediaSessionService`. Implements "last-minute" resolution for script-based URIs using a custom `ResolvingDataSource`.
- `MusicUtils`: Shared logic for parsing artist strings with custom delimiters and formatting metadata.

## 7. Implementation Logic
1. **The kanade:// Scheme**: External songs use the `kanade://resolve?source_id=...&original_id=...` URI. The `KanadePlaybackService` intercepts this and triggers a script `resolve()` call only when the song is about to play.
2. **Lyric Synchronization**: `PlayerViewModel` observes the `progressFlow` from the repository. It throttles updates to 200ms to calculate the current lyric line and broadcasts it to external lyric APIs if enabled.
3. **Dynamic Theming**: When a song changes, `PlayerViewModel` uses `Palette` to extract colors from the cover art. These colors are stored in `PlayerState.gradientColors` and applied to the player background with smooth transitions.
4. **Script Scoping**: Each script is wrapped in a `ScriptMusicSource`. `SourceManager` handles the activation/deactivation of scripts based on user settings.

## 8. Agent Development Instructions (AI Context)
- **State Management**: Always use `MutableStateFlow` in ViewModels and expose as `StateFlow`. UI MUST be stateless and only react to the state.
- **Media Actions**: Never interact with `MediaController` or `ExoPlayer` directly in UI code. All actions MUST go through `PlaybackRepository`.
- **KSS Threading**: All host bridge calls (HTTP, Crypto) must be performed within the `ScriptEngine`'s dedicated `executor` thread to satisfy QuickJS thread safety.
- **Theming**: Use `MaterialTheme.colorScheme` and the `Dimens` object for spacing. Do not hardcode pixel values.
- **Error Handling**: Use `PlayerEffect.ShowError` or `ShowMessage` for UI feedback. In repositories, catch exceptions and return safe defaults (e.g., `emptyList()`) to avoid app crashes.
- **Testing**: Place unit tests in `src/test` and UI tests in `src/androidTest`.
- **Code Quality**: Run `./gradlew spotlessApply` before committing to ensure ktlint compliance.
