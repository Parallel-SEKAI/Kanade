package org.parallel_sekai.kanade.ui.screens.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import androidx.palette.graphics.Palette
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.drawable.toBitmap
import org.parallel_sekai.kanade.data.repository.ArtistParsingSettings
import org.parallel_sekai.kanade.data.repository.PlaybackRepository
import org.parallel_sekai.kanade.data.repository.SettingsRepository
import org.parallel_sekai.kanade.data.model.*
import org.parallel_sekai.kanade.data.parser.*
import org.parallel_sekai.kanade.data.source.MusicUtils
import org.parallel_sekai.kanade.ui.theme.PlayerGradientEnd
import org.parallel_sekai.kanade.ui.theme.PlayerGradientStart
import org.parallel_sekai.kanade.ui.screens.player.PlayerIntent
import org.parallel_sekai.kanade.ui.screens.player.PlayerState
import org.parallel_sekai.kanade.ui.screens.player.RepeatMode
import org.parallel_sekai.kanade.ui.screens.player.DetailType

class PlayerViewModel(
    private val playbackRepository: PlaybackRepository,
    private val settingsRepository: SettingsRepository,
    private val context: Context // 需要 Context 来初始化 Coil Request
) : ViewModel() {

    private val _state = MutableStateFlow(PlayerState())
    val state = _state.asStateFlow()
    private val imageLoader = ImageLoader(context)

    // 新增：用于存储和传递艺术家解析设置
    private val _artistParsingSettings = MutableStateFlow(ArtistParsingSettings())

    init {
        // 监听歌词设置
        settingsRepository.lyricsSettingsFlow
            .onEach { settings ->
                _state.update { it.copy(lyricsSettings = settings) }
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
            .onEach { folders ->
                playbackRepository.setExcludedFolders(folders)
                handleIntent(PlayerIntent.RefreshList)
            }
            .launchIn(viewModelScope)

        // 监听播放队列变化并同步到 UI
        playbackRepository.currentPlaylist
            .onEach { mediaItems ->
                val list = mediaItems.map { item ->
                    MusicModel(
                        id = item.mediaId,
                        title = item.mediaMetadata.title?.toString() ?: "",
                        artists = MusicUtils.parseArtists(
                            artistString = item.mediaMetadata.artist?.toString(),
                            settings = _artistParsingSettings.value // 传入设置
                        ),
                        album = item.mediaMetadata.albumTitle?.toString() ?: "",
                        coverUrl = item.mediaMetadata.artworkUri?.toString() ?: "",
                        mediaUri = item.requestMetadata.mediaUri?.toString() ?: "",
                        duration = 0,
                        sourceId = item.mediaMetadata.extras?.getString("source_id") ?: "unknown"
                    )
                }
                _state.update { it.copy(currentPlaylist = list) }
            }
            .launchIn(viewModelScope)

        // 加载初始音乐列表
        viewModelScope.launch {
            val list = playbackRepository.fetchMusicList()
            val artists = playbackRepository.fetchArtistList()
            val albums = playbackRepository.fetchAlbumList()
            val folders = playbackRepository.fetchFolderList()
            val playlists = playbackRepository.fetchPlaylistList()
            
            // 确保 initialSong 始终是 MusicModel? 类型
            val initialSong: MusicModel? = list.firstOrNull() 

            _state.update { it.copy(
                allMusicList = list,
                currentPlaylist = list, // 初始时将全部音乐设为当前队列
                artistList = artists,
                albumList = albums,
                folderList = folders,
                playlistList = playlists,
                currentSong = it.currentSong ?: initialSong // 使用明确类型的 initialSong
            ) }
            
            initialSong?.let {
                extractColors(it)
            }
        }

        // 监听播放状态并同步到 UI State
        playbackRepository.isPlaying
            .onEach { isPlaying ->
                _state.update { it.copy(isPlaying = isPlaying) }
            }
            .launchIn(viewModelScope)

        // 监听当前播放的 MediaId 并更新 currentSong
        playbackRepository.currentMediaId
            .onEach { mediaId ->
                val song = state.value.allMusicList.find { it.id == mediaId }
                if (song != null) {
                    _state.update { it.copy(
                        currentSong = song,
                        lyrics = null // 重置旧歌词
                    ) }
                    
                    // 异步加载歌词和颜色
                    viewModelScope.launch {
                        extractColors(song)
                        val lyrics = playbackRepository.fetchLyrics(song.id)
                        val lyricData = lyrics?.let { LyricParserFactory.getParser(it).parse(it) }
                        _state.update { it.copy(
                            lyrics = lyrics,
                            lyricData = lyricData
                        ) }
                    }
                }
            }
            .launchIn(viewModelScope)

        // 监听循环模式
        playbackRepository.repeatMode
            .onEach { mode ->
                val repeatMode = when (mode) {
                    androidx.media3.common.Player.REPEAT_MODE_ONE -> org.parallel_sekai.kanade.ui.screens.player.RepeatMode.ONE
                    androidx.media3.common.Player.REPEAT_MODE_ALL -> org.parallel_sekai.kanade.ui.screens.player.RepeatMode.ALL
                    else -> org.parallel_sekai.kanade.ui.screens.player.RepeatMode.OFF
                }
                _state.update { it.copy(repeatMode = repeatMode) }
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
                _state.update { it.copy(
                    progress = pos,
                    duration = duration
                ) }
            }
            .launchIn(viewModelScope)
    }

    fun handleIntent(intent: PlayerIntent) {
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
                viewModelScope.launch {
                    val currentArtistParsingSettings = _artistParsingSettings.value // 获取最新设置
                    // fetchMusicList 和 fetchAlbumList 内部的 MusicUtils.parseArtists 调用也需要更新
                    // 由于 MusicUtils.parseArtists 已经修改为带默认参数的，因此这里无需显式传入
                    // 但为了保持一致性，还是通过传入参数来明确指定
                    val list = playbackRepository.fetchMusicList() // fetchMusicList 内部会使用 MusicUtils.parseArtists
                    val artists = playbackRepository.fetchArtistList()
                    val albums = playbackRepository.fetchAlbumList()
                    val folders = playbackRepository.fetchFolderList()
                    val playlists = playbackRepository.fetchPlaylistList()
                    
                    _state.update { it.copy(
                        allMusicList = list,
                        artistList = artists,
                        albumList = albums,
                        folderList = folders,
                        playlistList = playlists,
                        currentSong = it.currentSong ?: list.firstOrNull()
                    ) }
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
                _state.update { it.copy(
                    currentSong = intent.song,
                    lyrics = null
                ) }
                // 异步加载歌词
                viewModelScope.launch {
                    val lyrics = playbackRepository.fetchLyrics(intent.song.id)
                    val lyricData = lyrics?.let { LyricParserFactory.getParser(it).parse(it) }
                    _state.update { it.copy(
                        lyrics = lyrics,
                        lyricData = lyricData
                    ) }
                }
                
                val listToPlay = intent.customList ?: state.value.allMusicList
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
        }
    }

    private suspend fun extractColors(song: MusicModel) {
        val request = ImageRequest.Builder(context)
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
                    palette.getDarkVibrantColor(0)
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
