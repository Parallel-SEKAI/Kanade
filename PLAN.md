# Plan: Cache Management Settings

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

## Details
- **SettingsRepository**: Added `MAX_CACHE_SIZE` key to DataStore.
- **CacheManager**: 
    - `getCache` now supports dynamic resizing (recreates `SimpleCache` if limit changes).
    - Added `getCurrentCacheSize()` and `clearCache()`.
- **UI**: 
    - New `CacheSettingsScreen` provides a Slider (100MB - 2000MB) for limits.
    - Added "Used Space" display with auto-formatting (B, KB, MB, GB).
    - Added "Clear Cache" button with error container styling.
- **Service**: `KanadePlaybackService` reads the limit from repository during initialization.
