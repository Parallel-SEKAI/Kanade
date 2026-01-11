# Plan - Library Page Enhancements

## 1. Data Layer: Support for Artists, Albums, and Folders
- [ ] **Define Models**:
    - [ ] Add `ArtistModel(id, name, albumCount, songCount)` to `IMusicSource.kt`.
    - [ ] Add `AlbumModel(id, title, artist, coverUrl, songCount)` to `IMusicSource.kt`.
    - [ ] Add `FolderModel(name, path, songCount)` to `IMusicSource.kt`.
- [ ] **Update IMusicSource Interface**:
    - [ ] Add `suspend fun getArtistList(): List<ArtistModel>`.
    - [ ] Add `suspend fun getAlbumList(): List<AlbumModel>`.
    - [ ] Add `suspend fun getFolderList(): List<FolderModel>`.
- [ ] **Implement in LocalMusicSource**:
    - [ ] Query `MediaStore.Audio.Artists` for artist info.
    - [ ] Query `MediaStore.Audio.Albums` for album info.
    - [ ] Group `MediaStore.Audio.Media` results by parent directory for folder info.
- [ ] **Update PlaybackRepository**:
    - [ ] Expose the above methods to ViewModels.

## 2. UI Layer: Library Screen Modification
- [ ] **Modify LibraryScreen**:
    - [ ] Add a grid or list of 5 buttons at the top:
        1. **All Music** (全部音乐)
        2. **Artists** (艺术家)
        3. **Albums** (专辑)
        4. **Playlists** (播放列表 - Placeholder)
        5. **Folders** (文件夹)
    - [ ] Re-organize the "Your Library" header and content.
- [ ] **Create Sub-Screens**:
    - [ ] `ArtistListScreen`: Displays a list of artists.
    - [ ] `AlbumListScreen`: Displays a list of albums.
    - [ ] `FolderListScreen`: Displays a list of folders.
- [ ] **Update Navigation**:
    - [ ] Define routes for `Library_AllMusic`, `Library_Artists`, `Library_Albums`, `Library_Folders` in `MainActivity`.
    - [ ] Implement navigation from `LibraryScreen` buttons to these sub-screens.

## 3. Verification
- [ ] Verify artist and album counts are correct.
- [ ] Ensure folder navigation correctly lists music within selected directories.
- [ ] Check UI consistency with Material 3.
