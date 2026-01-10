# Fix MiniPlayer Transparency and Layout - COMPLETED

## Goal
Fix the issue where the area outside the MiniPlayer's rounded corners does not show the page content. This involves moving the MiniPlayer out of the `Scaffold`'s `bottomBar` to allow it to float over the `NavHost` content.

## Tasks
- [x] Update `MainActivity.kt`:
    - [x] Move `KanadePlayerContainer` (MiniPlayer mode) from `bottomBar` to the main content `Box`.
    - [x] Adjust `innerPadding` usage to only account for the `NavigationBar`.
    - [x] Ensure `MiniPlayer` is positioned correctly above the `NavigationBar`.
- [x] Update `PlayerComponents.kt`:
    - [x] Use `shape` parameter in `Surface` instead of `Modifier.clip` for better shadow and transparency handling.
    - [x] Ensure the background of the player container is transparent.
- [x] Update `LibraryScreen.kt`:
    - [x] Remove redundant nested `Scaffold`.
    - [x] Use `contentPadding` with `WindowInsets.statusBars` to allow edge-to-edge scrolling under the status bar.
- [x] Update `MainActivity.kt`:
    - [x] Adjust `NavHost` padding to exclude top inset, allowing content to reach the screen top.
- [x] Update `PlayerComponents.kt`:
    - [x] Add scale transition and `sharedBounds` for smooth expansion from MiniPlayer to FullScreen.

## Feature: Shuffle and Repeat Modes
- [x] Update `PlayerContract.kt`:
    - [x] Add `repeatMode` (Off, One, All) and `shuffleModeEnabled` (Boolean) to `PlayerState`.
    - [x] Add `ToggleRepeat` and `ToggleShuffle` to `PlayerIntent`.
- [x] Update `PlaybackRepository.kt`:
    - [x] Implement methods to toggle repeat and shuffle modes in `MediaController`.
    - [x] Sync these modes from `MediaController` via `Player.Listener`.
- [x] Update `PlayerViewModel.kt`:
    - [x] Handle new toggle intents and update state from repository observers.
- [x] Update `PlayerComponents.kt`:
    - [x] Add Shuffle and Repeat toggle buttons to `FullScreenContent`.
    - [x] Add visual indicators for active states.

## Feature: Metadata Lyrics Support (Advanced)
- [ ] **Dependency Update**:
    - [ ] Add `jaudiotagger` to `gradle/libs.versions.toml` and `app/build.gradle.kts`.
- [ ] **Advanced Extraction Logic**:
    - [ ] Update `LocalMusicSource.getLyrics` to use `jaudiotagger`.
    - [ ] Implement `USLT` frame extraction for MP3 (ID3v2.3/2.4).
    - [ ] Implement `LYRICS`/`UNSYNCEDLYRICS` tag extraction for FLAC.
- [x] Update `IMusicSource.kt`:
    - [x] Add `lyrics: String?` field to `MusicModel`.
- [x] Update `LocalMusicSource.kt`:
    - [x] Implement initial lyric extraction structure.
- [x] Update `PlayerContract.kt`:
    - [x] Add `lyrics: String?` to `PlayerState`.
- [x] Update `PlayerViewModel.kt`:
    - [x] Ensure lyrics are propagated to the state when a song is selected or transitions.
- [x] Update `PlayerComponents.kt`:
    - [x] Implement `LyricContent` component.
    - [x] Add a way to toggle between Album Art and Lyrics in `FullScreenContent` (e.g., tap on the album art).

## Feature: Advanced Lyric Parsing (LRC & TTML)
- [ ] **Define Models**: Create `WordInfo`, `LyricLine`, and `LyricData` classes.
- [ ] **LRC Parser**:
    - [ ] Handle standard `[mm:ss.xxx]` timestamps.
    - [ ] Implement translation detection (consecutive lines with identical timestamps).
    - [ ] Support Enhanced LRC (word-by-word timing with `<mm:ss.xxx>`).
- [ ] **TTML Parser**:
    - [ ] Parse Apple-style TTML using `XmlPullParser`.
    - [ ] Extract line-level and word-level timing from `<span>`.
    - [ ] Handle `ttm:role="x-translation"` for localized lyrics.
- [ ] **Integration**:
    - [ ] Create `LyricParserFactory` to select parser based on content.
    - [ ] Update `PlayerViewModel` to store and sync current active lyric line.

## Verification
- [x] Verify that page content (Library) is visible behind the MiniPlayer's rounded corners.
- [x] Verify that the expansion transition still works correctly.
- [x] Verify that the `NavigationBar` is still accessible and correctly positioned.

