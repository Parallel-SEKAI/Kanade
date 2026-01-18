# Plan: Lyrics Image Sharing

## Status
- [ ] Analyze requirement and design UI for lyrics sharing.
- [ ] Add `OpenLyricShare` intent and state to `PlayerContract.kt`.
- [ ] Implement `LyricShareBottomSheet` in `PlayerComponents.kt`.
- [ ] Implement long-press gesture in `LyricContent`.
- [ ] Create `LyricCardGenerator` to render lyrics and metadata to a `Bitmap`.
- [ ] Implement "Save to Gallery" and "Share" functionality using `MediaStore` and `Intent`.
- [ ] Verify build and functionality on Android 16.

## Details
- **Trigger**: Long press on a lyric line in full-screen player.
- **UI**: A `ModalBottomSheet` allowing users to select multiple lines of lyrics.
- **Card Design**: 
    - Background: Blurred album art or gradient.
    - Content: Album art thumbnail, song title, artist, selected lyrics, and a "Kanade" branding.
- **Sharing**: 
    - Use `Intent.ACTION_SEND` with `FileProvider` (or `MediaStore` URI).
    - Save to `Pictures/Kanade/`.

---

# Plan: Cache Management Settings (Completed)

## Status
- [x] Analyze current cache implementation in `CacheManager`.
- [x] Add cache size limit setting to `SettingsRepository`.
- [x] Enhance `CacheManager` with size calculation and clearing capabilities.
- [x] Implement cache management logic in `SettingsViewModel`.
- [x] Create `CacheSettingsScreen` with usage display and limit control.
- [x] Refine `CacheSettingsScreen`: Set limit to 64GB and remove manual clear button.
- [x] Integrate `CacheSettingsScreen` into navigation and main settings.
- [x] Ensure `KanadePlaybackService` respects the user-defined cache limit.
- [x] Verify build stability.