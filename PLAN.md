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

## Tasks
- [x] Analyze existing player code.
- [x] Update `PLAN.md` and `UI.md`.
- [x] Implement `PlaylistContent` in `PlayerComponents.kt`.
- [x] Update `FullScreenContent` to support playlist mode.
- [x] Add logic to highlight the currently playing song in the playlist.
- [x] Enable clicking a song in the playlist to play it.
- [x] Add Shuffle and Repeat buttons to the playlist view.
- [x] Fix: MiniPlayer container blocking Bottom Navigation clicks when collapsed.
- [ ] Verify by building and running the app.