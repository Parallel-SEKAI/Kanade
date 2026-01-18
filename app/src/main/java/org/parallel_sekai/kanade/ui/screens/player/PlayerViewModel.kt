package org.parallel_sekai.kanade.ui.screens.player

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.parallel_sekai.kanade.R
import org.parallel_sekai.kanade.data.model.*
import org.parallel_sekai.kanade.data.parser.*
import org.parallel_sekai.kanade.data.repository.ArtistParsingSettings
import org.parallel_sekai.kanade.data.repository.PlaybackRepository
import org.parallel_sekai.kanade.data.repository.SettingsRepository
import org.parallel_sekai.kanade.data.source.MusicUtils
import org.parallel_sekai.kanade.data.utils.LyricGetterManager
import org.parallel_sekai.kanade.data.utils.LyricImageUtils
import org.parallel_sekai.kanade.ui.screens.player.DetailType
import org.parallel_sekai.kanade.ui.screens.player.PlayerEffect
import org.parallel_sekai.kanade.ui.screens.player.PlayerIntent
import org.parallel_sekai.kanade.ui.screens.player.PlayerState
import org.parallel_sekai.kanade.ui.screens.player.RepeatMode

class PlayerViewModel(
    private val playbackRepository: PlaybackRepository,
    private val settingsRepository: SettingsRepository,
    private val applicationContext: Context,
    private val imageLoader: ImageLoader,
    private val lyricGetterManager: LyricGetterManager,
) : ViewModel() {

    private val _state = MutableStateFlow(PlayerState())
    val state = _state.asStateFlow()

    private val _effect = MutableSharedFlow<PlayerEffect>()
    val effect = _effect.asSharedFlow()

    private var lastSentLyric: String? = null
    private var lastLyricUpdateTimestamp = 0L
    private var lastRefreshedScriptId: String? = null
    private var refreshJob: Job? = null

    // 新增：用于存储和传递艺术家解析设置
    private val _artistParsingSettings = MutableStateFlow(ArtistParsingSettings())

    init {
        // 监听歌词设置
        settingsRepository.lyricsSettingsFlow
            .onEach { settings ->
                _state.update { it.copy(lyricsSettings = settings) }
                if (!settings.isSharingEnabled) {
                    lyricGetterManager.clearLyric()
                    lastSentLyric = null
                }
            }
            .launchIn(viewModelScope)

        // 监听艺术家解析设置并更新 PlayerState
        settingsRepository.artistParsingSettingsFlow
            .onEach { settings ->
                _artistParsingSettings.value = settings
                _state.update { it.copy(artistJoinString = settings.joinString) } // 更新 PlayerState 中的 joinString
            }
            .launchIn(viewModelScope)

        // 监听排除文件夹
        settingsRepository.excludedFoldersFlow
            .drop(1) // 跳过启动时的初始加载，因为 init 块已经处理了
            .onEach { folders ->
                playbackRepository.setExcludedFolders(folders)
                handleIntent(PlayerIntent.RefreshList(forceScriptRefresh = false))
            }
            .launchIn(viewModelScope)

        // 监听播放队列变化并同步到 UI
        playbackRepository.currentPlaylist
            .onEach { mediaItems ->
                val list = mediaItems.map { item ->
                    MusicModel(
                        id = item.mediaMetadata.extras?.getString("original_id") ?: item.mediaId,
                        title = item.mediaMetadata.title?.toString() ?: "",
                        artists = MusicUtils.parseArtists(
                            artistString = item.mediaMetadata.artist?.toString(),
                            settings = _artistParsingSettings.value, // 传入设置
                        ),
                        album = item.mediaMetadata.albumTitle?.toString() ?: "",
                        coverUrl = item.mediaMetadata.artworkUri?.toString() ?: "",
                        mediaUri = item.requestMetadata.mediaUri?.toString() ?: "",
                        duration = item.mediaMetadata.extras?.getLong("duration") ?: 0L,
                        sourceId = item.mediaMetadata.extras?.getString("source_id") ?: "unknown",
                    )
                }
                _state.update { it.copy(currentPlaylist = list) }
            }
            .launchIn(viewModelScope)

        // 加载初始音乐列表 (仅加载本地音乐)
        viewModelScope.launch {
            _state.update { it.copy(isHomeLoading = true) }

            val list = playbackRepository.fetchMusicList()

            _state.update {
                it.copy(
                    allMusicList = list,
                    // 如果当前没有播放列表，则将本地列表设为当前队列（防止初次启动时列表为空）
                    currentPlaylist = if (it.currentPlaylist.isEmpty()) list else it.currentPlaylist,
                    isHomeLoading = false,
                )
            }

            // 只有当当前没有歌曲时，才使用本地第一首作为预览（但不播放）
            if (_state.value.currentSong == null) {
                list.firstOrNull()?.let { initialSong ->
                    _state.update { it.copy(currentSong = initialSong) }
                    extractColors(initialSong)
                }
            }
        }

        // 监听播放状态并同步到 UI State
        playbackRepository.isPlaying
            .onEach { isPlaying ->
                _state.update { it.copy(isPlaying = isPlaying) }
                if (!isPlaying) {
                    lyricGetterManager.clearLyric()
                    lastSentLyric = null
                }
            }
            .launchIn(viewModelScope)

        // 监听当前播放的 MediaItem 并更新 currentSong
        playbackRepository.currentMediaItem
            .onEach { mediaItem ->
                if (mediaItem != null) {
                    val song = MusicModel(
                        id = mediaItem.mediaMetadata.extras?.getString("original_id") ?: mediaItem.mediaId,
                        title = mediaItem.mediaMetadata.title?.toString() ?: "",
                        artists = MusicUtils.parseArtists(
                            artistString = mediaItem.mediaMetadata.artist?.toString(),
                            settings = _artistParsingSettings.value,
                        ),
                        album = mediaItem.mediaMetadata.albumTitle?.toString() ?: "",
                        coverUrl = mediaItem.mediaMetadata.artworkUri?.toString() ?: "",
                        mediaUri = mediaItem.requestMetadata.mediaUri?.toString() ?: "",
                        duration = mediaItem.mediaMetadata.extras?.getLong("duration") ?: 0L,
                        sourceId = mediaItem.mediaMetadata.extras?.getString("source_id") ?: "unknown",
                    )

                    if (song.id != state.value.currentSong?.id || song.sourceId != state.value.currentSong?.sourceId) {
                        _state.update {
                            it.copy(
                                currentSong = song,
                                lyrics = null, // 重置旧歌词
                            )
                        }
                        lyricGetterManager.clearLyric()
                        lastSentLyric = null

                        // 异步加载歌词和颜色
                        viewModelScope.launch {
                            extractColors(song)
                            val lyrics = playbackRepository.fetchLyrics(song.id, song.sourceId)
                            val lyricData = lyrics?.let { LyricParserFactory.getParser(it).parse(it) }
                            _state.update {
                                it.copy(
                                    lyrics = lyrics,
                                    lyricData = lyricData,
                                )
                            }
                        }
                    }
                }
            }
            .launchIn(viewModelScope)

        // 监听循环模式
        playbackRepository.repeatMode
            .onEach { mode ->
                // ... (existing logic)
            }
            .launchIn(viewModelScope)

        // 监听脚本列表
        playbackRepository.scriptSources
            .onEach { sources ->
                _state.update { it.copy(scriptManifests = sources.map { it.manifest }) }
            }
            .launchIn(viewModelScope)

        // 监听当前活跃脚本 ID
        settingsRepository.activeScriptIdFlow
            .distinctUntilChanged()
            .onEach { id ->
                val previousId = _state.value.activeScriptId
                _state.update { it.copy(activeScriptId = id) }

                // 只有当 ID 真正改变（且不是第一次恢复状态）时才刷新
                if (previousId != null && previousId != id) {
                    handleIntent(PlayerIntent.RefreshList(forceScriptRefresh = true))
                }
            }
            .launchIn(viewModelScope)

        // 监听随机模式
        playbackRepository.shuffleModeEnabled
            .onEach { enabled ->
                _state.update { it.copy(shuffleModeEnabled = enabled) }
            }
            .launchIn(viewModelScope)

        // 监听进度流
        playbackRepository.progressFlow
            .onEach { (pos, duration) ->
                _state.update {
                    it.copy(
                        progress = pos,
                        duration = duration,
                    )
                }

                // 发送歌词到外部 API (带节流和重复内容过滤)
                val now = System.currentTimeMillis()
                if (now - lastLyricUpdateTimestamp >= 200) { // 200ms 节流，避免频繁 IPC
                    lastLyricUpdateTimestamp = now
                    if (state.value.isPlaying && state.value.lyricsSettings.isSharingEnabled) {
                        val lines = state.value.lyricData?.lines
                        val currentLineIndex = lines?.indexOfLast { it.startTime <= pos } ?: -1
                        val currentLine = lines?.getOrNull(currentLineIndex)
                        val lyricContent = currentLine?.content ?: ""

                        if (lyricContent != lastSentLyric) {
                            val nextLine = lines?.getOrNull(currentLineIndex + 1)
                            val delay = if (currentLine != null && nextLine != null) {
                                nextLine.startTime - currentLine.startTime
                            } else if (currentLine != null && state.value.duration > 0) {
                                (state.value.duration - currentLine.startTime).coerceAtLeast(0L)
                            } else {
                                0L
                            }

                            lyricGetterManager.sendLyric(
                                lyric = lyricContent,
                                translation = currentLine?.translation,
                                song = state.value.currentSong,
                                delay = delay,
                                words = currentLine?.words ?: emptyList(),
                            )
                            lastSentLyric = lyricContent
                        }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun handleIntent(intent: PlayerIntent) {
        // ... (rest of handleIntent)
        when (intent) {
            is PlayerIntent.PlayPause -> {
                if (state.value.isPlaying) {
                    playbackRepository.pause()
                } else {
                    playbackRepository.play()
                }
            }
            is PlayerIntent.Expand -> {
                _state.update { it.copy(isExpanded = true) }
            }
            is PlayerIntent.Collapse -> {
                _state.update { it.copy(isExpanded = false) }
            }
            is PlayerIntent.RefreshList -> {
                refreshJob?.cancel()
                refreshJob = viewModelScope.launch {
                    val currentScriptId = state.value.activeScriptId
                    val shouldRefreshHome = intent.forceScriptRefresh && (currentScriptId != null)

                    if (shouldRefreshHome) {
                        _state.update { it.copy(isHomeLoading = true) }
                    }

                    val list = playbackRepository.fetchMusicList()

                    if (shouldRefreshHome) {
                        var homeItems = emptyList<MusicModel>()
                        var retryCount = 0
                        while (homeItems.isEmpty() && retryCount < 2) {
                            if (retryCount > 0) delay(1000)
                            homeItems = withContext(Dispatchers.IO) {
                                try {
                                    playbackRepository.fetchHomeList()
                                } catch (e: Exception) {
                                    emptyList()
                                }
                            }
                            retryCount++
                        }

                        _state.update {
                            it.copy(
                                homeMusicList = homeItems,
                                isHomeLoading = false,
                            )
                        }
                        lastRefreshedScriptId = currentScriptId
                    }

                    _state.update {
                        it.copy(
                            allMusicList = list,
                            currentSong = it.currentSong ?: list.firstOrNull(),
                        )
                    }
                }
            }
            is PlayerIntent.RefreshArtists -> {
                viewModelScope.launch {
                    val artists = playbackRepository.fetchArtistList()
                    _state.update { it.copy(artistList = artists) }
                }
            }
            is PlayerIntent.RefreshAlbums -> {
                viewModelScope.launch {
                    val albums = playbackRepository.fetchAlbumList()
                    _state.update { it.copy(albumList = albums) }
                }
            }
            is PlayerIntent.RefreshFolders -> {
                viewModelScope.launch {
                    val folders = playbackRepository.fetchFolderList()
                    _state.update { it.copy(folderList = folders) }
                }
            }
            is PlayerIntent.RefreshPlaylists -> {
                viewModelScope.launch {
                    val playlists = playbackRepository.fetchPlaylistList()
                    _state.update { it.copy(playlistList = playlists) }
                }
            }
            is PlayerIntent.RefreshHome -> {
                val currentScriptId = state.value.activeScriptId
                if (currentScriptId != null) {
                    refreshJob?.cancel()
                    refreshJob = viewModelScope.launch {
                        _state.update { it.copy(isHomeLoading = true) }

                        var homeItems = emptyList<MusicModel>()
                        var retryCount = 0
                        while (homeItems.isEmpty() && retryCount < 2) {
                            if (retryCount > 0) delay(1000) // 重试间隔
                            homeItems = withContext(Dispatchers.IO) {
                                try {
                                    playbackRepository.fetchHomeList()
                                } catch (e: Exception) {
                                    emptyList()
                                }
                            }
                            retryCount++
                        }

                        _state.update {
                            it.copy(
                                homeMusicList = homeItems,
                                isHomeLoading = false,
                            )
                        }
                        lastRefreshedScriptId = currentScriptId
                    }
                }
            }
            is PlayerIntent.FetchDetailList -> {
                viewModelScope.launch {
                    val list = when (intent.type) {
                        DetailType.ARTIST -> playbackRepository.fetchSongsByArtist(intent.id)
                        DetailType.ALBUM -> playbackRepository.fetchSongsByAlbum(intent.id)
                        DetailType.FOLDER -> playbackRepository.fetchSongsByFolder(intent.id)
                        DetailType.PLAYLIST -> playbackRepository.fetchSongsByPlaylist(intent.id)
                    }
                    _state.update { it.copy(detailMusicList = list) }
                }
            }
            is PlayerIntent.SelectSong -> {
                _state.update {
                    it.copy(
                        currentSong = intent.song,
                        lyrics = null,
                    )
                }
                // 异步加载歌词
                viewModelScope.launch {
                    val lyrics = playbackRepository.fetchLyrics(intent.song.id, intent.song.sourceId)
                    val lyricData = lyrics?.let { LyricParserFactory.getParser(it).parse(it) }
                    _state.update {
                        it.copy(
                            lyrics = lyrics,
                            lyricData = lyricData,
                        )
                    }
                }

                val isScriptActive = state.value.activeScriptId != null
                val listToPlay = intent.customList
                    ?: (if (isScriptActive) state.value.homeMusicList else state.value.allMusicList)

                val index = listToPlay.indexOf(intent.song).coerceAtLeast(0)
                playbackRepository.setPlaylist(listToPlay, index)
            }
            is PlayerIntent.Next -> {
                playbackRepository.next()
            }
            is PlayerIntent.Previous -> {
                playbackRepository.previous()
            }
            is PlayerIntent.SeekTo -> {
                playbackRepository.seekTo(intent.position)
            }
            is PlayerIntent.ToggleRepeat -> {
                val nextMode = when (state.value.repeatMode) {
                    RepeatMode.OFF -> androidx.media3.common.Player.REPEAT_MODE_ALL
                    RepeatMode.ALL -> androidx.media3.common.Player.REPEAT_MODE_ONE
                    RepeatMode.ONE -> androidx.media3.common.Player.REPEAT_MODE_OFF
                }
                playbackRepository.setRepeatMode(nextMode)
            }
            is PlayerIntent.ToggleShuffle -> {
                playbackRepository.setShuffleModeEnabled(!state.value.shuffleModeEnabled)
            }
            is PlayerIntent.ReloadScripts -> {
                viewModelScope.launch {
                    playbackRepository.refreshScriptSources()
                    handleIntent(PlayerIntent.RefreshList(forceScriptRefresh = true))
                }
            }
            is PlayerIntent.ImportScript -> {
                viewModelScope.launch {
                    playbackRepository.importScript(intent.uri)
                }
            }
            is PlayerIntent.ToggleActiveScript -> {
                viewModelScope.launch {
                    settingsRepository.updateActiveScriptId(intent.scriptId)
                }
            }
            is PlayerIntent.UpdateScriptConfig -> {
                viewModelScope.launch {
                    val configsJson = settingsRepository.scriptConfigsFlow.first()
                    val allConfigs: MutableMap<String, MutableMap<String, String>> = configsJson?.let {
                        try {
                            Json.decodeFromString<MutableMap<String, MutableMap<String, String>>>(it)
                        } catch (e: Exception) {
                            mutableMapOf<String, MutableMap<String, String>>()
                        }
                    } ?: mutableMapOf<String, MutableMap<String, String>>()

                    val scriptConfig = allConfigs.getOrPut(intent.scriptId) { mutableMapOf<String, String>() }
                    scriptConfig[intent.key] = intent.value

                    settingsRepository.updateScriptConfigs(Json.encodeToString(allConfigs))
                    handleIntent(PlayerIntent.ReloadScripts) // Restart engine to apply config
                }
            }
            is PlayerIntent.OpenLyricShare -> {
                _state.update {
                    it.copy(
                        showLyricShare = true,
                        selectedLyricIndices = setOf(intent.index),
                    )
                }
            }
            is PlayerIntent.CloseLyricShare -> {
                _state.update {
                    it.copy(
                        showLyricShare = false,
                        selectedLyricIndices = emptySet(),
                    )
                }
            }
            is PlayerIntent.ToggleLyricSelection -> {
                _state.update {
                    val current = it.selectedLyricIndices
                    val next = if (current.contains(intent.index)) {
                        current - intent.index
                    } else {
                        current + intent.index
                    }
                    it.copy(selectedLyricIndices = next)
                }
            }
            is PlayerIntent.SaveLyricImage -> {
                viewModelScope.launch {
                    val bitmap = prepareLyricBitmap()
                    if (bitmap != null) {
                        val uri = LyricImageUtils.saveBitmapToGallery(applicationContext, bitmap)
                        if (uri != null) {
                            _effect.emit(PlayerEffect.ShowMessage(applicationContext.getString(R.string.msg_lyric_saved)))
                        }
                        _state.update { it.copy(showLyricShare = false, selectedLyricIndices = emptySet()) }
                    }
                }
            }
            is PlayerIntent.ShareLyricImage -> {
                viewModelScope.launch {
                    val bitmap = prepareLyricBitmap()
                    if (bitmap != null) {
                        LyricImageUtils.shareBitmap(applicationContext, bitmap)
                        _state.update { it.copy(showLyricShare = false, selectedLyricIndices = emptySet()) }
                    }
                }
            }
        }
    }

    private suspend fun prepareLyricBitmap(): android.graphics.Bitmap? {
        val song = state.value.currentSong ?: return null
        val lyrics = state.value.lyricData?.lines ?: return null
        val selectedIndices = state.value.selectedLyricIndices
        val colors = state.value.gradientColors
        val quality = state.value.lyricsSettings.shareQuality
        val alignment = state.value.lyricsSettings.alignment
        val artistJoinString = state.value.artistJoinString

        return LyricImageUtils.generateLyricImage(
            applicationContext,
            song,
            lyrics,
            selectedIndices,
            colors,
            quality,
            alignment,
            artistJoinString,
        )
    }

    private suspend fun extractColors(song: MusicModel) {
        val request = ImageRequest.Builder(applicationContext)
            .data(song.coverUrl)
            .allowHardware(false) // Palette 需要获取 Bitmap 的像素，不能是 Hardware Bitmap
            .build()

        val result = imageLoader.execute(request)
        if (result is SuccessResult) {
            val bitmap = result.drawable.toBitmap()
            val colors = withContext(Dispatchers.Default) {
                val palette = Palette.from(bitmap).generate()
                listOfNotNull(
                    palette.getVibrantColor(0),
                    palette.getMutedColor(0),
                    palette.getDominantColor(0),
                    palette.getDarkVibrantColor(0),
                ).filter { it != 0 }
                    .distinct()
                    .map { Color(it) }
            }
            if (colors.isNotEmpty()) {
                _state.update { it.copy(gradientColors = colors) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playbackRepository.release()
    }
}
