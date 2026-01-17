# Implementation Plan - External Scripts Support (KSS)

## Phase A: Planning & Specification (Completed)
- [x] Requirement Analysis: Define core goals for the scripting system.
- [x] Technical Specification: Draft Logic Flow, Interface Contract, Directory Structure, and Security/Sandboxing. (Created `SCRIPTS.md`)
- [ ] User Review: Obtain approval for the specification.

## Phase B: Execution
- [x] Infrastructure Setup:
    - [x] Add `quickjs-android` and `kotlinx-serialization-json` dependencies.
    - [x] Create `org.parallel_sekai.kanade.data.script` package.
- [x] Script Engine Core:
    - [x] Implement `ScriptEngine` wrapper for QuickJS with Promise support.
    - [x] Implement `ScriptManager` for file scanning, manifest parsing, and lifecycle management.
    - [x] Create `ScriptMusicSource` implementing `IMusicSource`.
- [x] Bridge Layer:
    - [x] Implement `HostBridge` with `kanade.http` (OkHttp), `kanade.cache`, and `kanade.log`.
    - [x] Map JS `MusicItem` and `StreamInfo` to Kotlin data classes.
- [x] UI Integration:
    - [x] Add "External Sources" section to Library screen.
    - [x] Add "Scripts" management page in Settings (List scripts, toggle, reload).
    - [x] Add "Import Script" functionality to handle permission issues.
- [ ] Source Selection in Search:
    - [ ] Implement Source Selection in Search screen (Filter search by specific scripts).
- [x] Song Info & Metadata:
    - [x] Implement `SongInfoScreen` to display detailed metadata and raw lyrics of the current song.
    - [x] Add "Song Info" option to the player "More" menu.
    - [x] Update navigation to support `SongInfoScreen`.
- [x] External Source Playback & Caching:
    - [x] Implement lazy URL resolution in `KanadePlaybackService` via `IMusicSource.getPlayUrl`.
    - [x] Implement `CacheManager` using Media3 `SimpleCache` for persistent audio caching.
    - [x] Integrate `CacheDataSource` into `ExoPlayer` for automatic remote stream caching.
- [x] Robustness & Bug Fixes:
    - [x] Implement `importScript(Uri)` in `ScriptManager`.
    - [x] Fix Netease script home page stuck loading issue (JS bridge wrapper, timeout, loading state).
    - [x] Prevent automatic script refresh on startup and setting changes to improve UX.
- [x] Debug: Netease Lyric Mismatch - Investigate why `netease.kanade.js` returns incorrect lyrics for specific songs, possibly by logging `musicId` and API responses. (Fixed by normalizing script output concatenation)
- [ ] Testing & Samples:
    - [ ] Create a sample `.js` script (e.g., `mock_provider.js`).
    - [ ] Verify search, media URL retrieval, and playback.

## Phase C: Finalization
- [ ] Testing: Verify script loading and data parsing with sample providers.
- [ ] Documentation: Provide a "Script Developer Guide" for end-users.
- [ ] Summary: Finalize changes and prepare for commit.
