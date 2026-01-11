# Plan - Complete Library Sub-Screens

## 1. Data Layer Enhancements
- [ ] **Define Models**:
    - [ ] Add `PlaylistModel(id, name, coverUrl, songCount)` to `IMusicSource.kt`.
- [ ] **Update IMusicSource Interface**:
    - [ ] `suspend fun getSongsByArtist(artistName: String): List<MusicModel>`
    - [ ] `suspend fun getSongsByAlbum(albumId: String): List<MusicModel>`
    - [ ] `suspend fun getSongsByFolder(path: String): List<MusicModel>`
    - [ ] `suspend fun getPlaylistList(): List<PlaylistModel>`
    - [ ] `suspend fun getSongsByPlaylist(playlistId: String): List<MusicModel>`
- [ ] **Implement in LocalMusicSource**:
    - [ ] Use `MediaStore.Audio.Media.ARTIST` selection for artist songs.
    - [ ] Use `MediaStore.Audio.Media.ALBUM_ID` selection for album songs.
    - [ ] Use `MediaStore.Audio.Media.DATA` (LIKE path/%) for folder songs.
    - [ ] Query `MediaStore.Audio.Playlists` for playlist support.

## 2. Navigation & ViewModels
- [ ] **Navigation Routes**:
    - [ ] `artist_detail/{artistName}`
    - [ ] `album_detail/{albumId}/{title}`
    - [ ] `folder_detail/{path}`
    - [ ] `playlist_detail/{playlistId}/{title}`
- [ ] **PlayerContract & ViewModel**:
    - [ ] Add `selectedMusicList` to `PlayerState` for detail views.
    - [ ] Add `FetchDetailList(type, id)` intent to load songs for the detail screen.

## 3. UI Implementation
- [ ] **PlaylistListScreen**: Implementation of the "Playlists" grid entry.
- [ ] **ArtistDetailScreen**: List of songs by a specific artist.
- [ ] **AlbumDetailScreen**: Immersive header with album art and tracklist.
- [ ] **FolderDetailScreen**: List of songs in a specific file system directory.
- [ ] **PlaylistDetailScreen**: Songs within a selected playlist.

## 4. Polishing
- [ ] Ensure "Play All" functionality in detail screens correctly sets the playlist.
- [ ] Add shared element transitions (if feasible) or smooth fade-ins for album art.