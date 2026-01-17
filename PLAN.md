# Plan: On-demand Data Fetching Optimization

## Status
- [x] Analyze data fetching patterns in `PlayerViewModel`.
- [x] Split `PlayerIntent.RefreshList` into granular intents (`RefreshArtists`, `RefreshAlbums`, etc.).
- [x] Refactor `PlayerViewModel` to defer non-essential data fetching.
- [x] Update UI screens (`LibraryScreen`, `ArtistListScreen`, etc.) to trigger data fetching using `LaunchedEffect`.
- [x] Verify build stability.

## Details
- **PlayerViewModel**: Initial fetch now only includes the local music list. Home items and category lists are removed from `init`.
- **Intents**: Added `RefreshArtists`, `RefreshAlbums`, `RefreshFolders`, `RefreshPlaylists`, and `RefreshHome`.
- **UI Integration**: `LaunchedEffect` in each list screen ensures data is fetched only when the user navigates to it.
- **Performance**: Reduced startup time and network/IO overhead by avoiding massive all-at-once data loading.