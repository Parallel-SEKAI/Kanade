# Implementation Plan - Fullscreen Player Playlist

Implement a playlist (Up Next) feature for the fullscreen player with a UI style inspired by Apple Music.

## User Interface (UI.md)
- [ ] Create `PlaylistContent` component in `PlayerComponents.kt`.
- [ ] Implement Apple Music-style list items for the playlist.
- [ ] Add transition animations between Cover, Lyrics, and Playlist views.

## Logic & State
- [ ] Add `showPlaylist` state in `FullScreenContent`.
- [ ] Handle mutual exclusivity between Lyrics and Playlist views.
- [ ] Connect the `QueueMusic` button to toggle the playlist.

## More & Settings
- [x] Create `MoreScreen` with a button to navigate to Settings.
- [x] Create `SettingsScreen` placeholder.
- [x] Update `MainActivity` navigation graph to include Settings.
- [x] Add "UI" category to `SettingsScreen`.
- [x] Create `LyricsSettingsScreen`.
- [x] Implement navigation from Settings to Lyrics Settings.

## Balanced Line Mode (Smart Line Wrapping)
- [ ] **Configuration**:
    - [ ] Add `balanceLines: Boolean` to `LyricsSettings` & `SettingsRepository`.
    - [ ] Update `SettingsViewModel` and `LyricsSettingsScreen` (add switch & preview).
- [ ] **Algorithm (`LyricAtomSplitter`)**:
    - [ ] Create core logic to parse text/words into "Atoms" (protecting words & `WordInfo`).
    - [ ] Implement weighted scoring system:
        - [ ] Center proximity (Base score).
        - [ ] Removable punctuation (`，`, ` `, etc.) -> Remove & Break.
        - [ ] Sticky punctuation (`。`, `！`, etc.) -> Keep & Break.
        - [ ] Forbidden punctuation (`（`, `《`) -> Avoid.
        - [ ] Time gap analysis for word-by-word lyrics.
- [ ] **UI Component (`BalancedLyricView`)**:
    - [ ] Use `TextMeasurer` to detect line wrapping necessity.
    - [ ] Integrate `LyricAtomSplitter` to calculate split index if needed.
    - [ ] Render split lines using `Column` with proper alignment.
- [ ] **Integration**:
    - [ ] Replace `Text` & `WordByWordLine` with `BalancedLyricView` in `LyricContent`.
    - [ ] Verify performance (caching measurement results).

## Tasks
- [x] Analyze existing player code.
- [x] Update `PLAN.md` and `UI.md`.
- [x] Implement `PlaylistContent` in `PlayerComponents.kt`.
- [x] Update `FullScreenContent` to support playlist mode.
- [x] Add logic to highlight the currently playing song in the playlist.
- [x] Enable clicking a song in the playlist to play it.
- [x] Add Shuffle and Repeat buttons to the playlist view.
- [x] Fix: MiniPlayer container blocking Bottom Navigation clicks when collapsed.
- [x] Implement `MoreScreen` and navigation to `SettingsScreen`.
- [ ] Verify by building and running the app.