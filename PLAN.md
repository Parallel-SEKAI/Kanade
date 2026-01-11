# Plan - Complete Library Sub-Screens & Artist Parsing

## 1. Artist Parsing Enhancement (Array Support)
- [x] **Refine Parsing Logic**:
    - [x] Create `MusicUtils.kt` with `parseArtists(artistString: String?): List<String>`.
    - [x] Implement **Whitelist** (e.g., "Leo/need") to prevent incorrect splitting.
    - [x] Implement **Priority-based Cascading Split**:
        - Order: `["/", ";", "|", " & ", " feat. ", " ft. ", ","]`.
        - Logic: Use the first matching separator found in the string as the delimiter.
- [x] **Data Model Refactoring**:
    - [x] Modify `MusicModel` in `IMusicSource.kt`: `artist: String` -> `artists: List<String>`.
    - [x] Modify `AlbumModel` in `IMusicSource.kt`: `artist: String` -> `artists: List<String>`.
- [x] **Source Implementation**:
    - [x] Update `LocalMusicSource.kt` to use `MusicUtils.parseArtists` for all artist fields.
    - [x] Update `getSongsByArtist` to handle matching against a list if necessary (or keep simple string match for MediaStore queries).
- [x] **Repository & Playback**:
    - [x] Update `PlaybackRepository.kt` to handle `List<String>` when converting to Media3 `MediaItem`.
- [x] **UI Adaptation**:
    - [x] Global search and replace: Update all UI components to use `artists.joinToString(" & ")` or similar for display.
    - [x] Affected files: `LibraryScreen.kt`, `PlayerComponents.kt`, `SearchScreen.kt`, `MusicItem` components.

## 2. Configurable Artist Parsing and Display

### 2.1. 设置数据模型定义
- [ ] 在 `data/repository/SettingsRepository.kt` 中创建新的数据类 `ArtistParsingSettings`，包含：
    - `separators: List<String>` (分隔符列表，存储顺序即优先级，默认值：`["/", ";", "|", " & ", " feat. ", " ft. ", ","]`)
    - `whitelist: List<String>` (白名单列表，默认值：`["Leo/need"]`)
    - `joinString: String` (艺术家拼接字符串，默认值：`", "`)

### 2.2. `SettingsRepository` 更新
- [ ] 在 `SettingsRepository.kt` 中添加方法来保存和读取 `ArtistParsingSettings` 到 `DataStore`。
- [ ] 暴露 `artistParsingSettingsFlow: StateFlow<ArtistParsingSettings>`。

### 2.3. 设置 UI 实现
- [ ] 在 `ui/screens/settings` 目录下创建新屏幕 `ArtistParsingSettingsScreen.kt`。
- [ ] 在 `SettingsScreen.kt` 中添加一个入口，跳转到 `ArtistParsingSettingsScreen`。
- [ ] **UI 布局 (`UI.md` 中定义)**:
    - **分隔符列表**：可拖动排序，每个分隔符可编辑或删除，并可添加新的分隔符。
    - **白名单列表**：每个白名单项可编辑或删除，并可添加新的白名单项。
    - **拼接字符串**：一个文本输入框，允许用户自定义拼接字符串。

### 2.4. `MusicUtils` 集成
- [ ] 修改 `MusicUtils`，使其能够接收 `ArtistParsingSettings` 作为配置。
- [ ] `parseArtists` 方法将使用这些配置的分隔符和白名单。

### 2.5. UI 显示适配
- [ ] 查找所有使用 `music.artists.joinToString(...)` 的地方。
- [ ] 修改这些地方，使其从 `SettingsRepository` 获取 `joinString` 并应用。

### 2.6. `PlayerViewModel` 更新
- [ ] 注入 `SettingsRepository` 到 `PlayerViewModel`。
- [ ] 监听 `artistParsingSettingsFlow`，并将最新设置传递给 `MusicUtils`。

### 2.7. 验证
- [ ] 运行应用，测试设置界面功能。
- [ ] 修改设置并验证艺术家解析和显示是否按预期工作。
- [ ] 确保设置持久化。

## 3. Data Layer Enhancements
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

## 4. Navigation & ViewModels
- [ ] **Navigation Routes**:
    - [ ] `artist_detail/{artistName}`
    - [ ] `album_detail/{albumId}/{title}`
    - [ ] `folder_detail/{path}`
    - [ ] `playlist_detail/{playlistId}/{title}`
- [ ] **PlayerContract & ViewModel**:
    - [ ] Add `selectedMusicList` to `PlayerState` for detail views.
    - [ ] Add `FetchDetailList(type, id)` intent to load songs for the detail screen.

## 5. UI Implementation
- [ ] **PlaylistListScreen**: Implementation of the "Playlists" grid entry.
- [ ] **ArtistDetailScreen**: List of songs by a specific artist.
- [ ] **AlbumDetailScreen**: Immersive header with album art and tracklist.
- [ ] **FolderDetailScreen**: List of songs in a specific file system directory.
- [ ] **PlaylistDetailScreen**: Songs within a selected playlist.

## 6. Polishing
- [ ] Ensure "Play All" functionality in detail screens correctly sets the playlist.
- [ ] Add shared element transitions (if feasible) or smooth fade-ins for album art.