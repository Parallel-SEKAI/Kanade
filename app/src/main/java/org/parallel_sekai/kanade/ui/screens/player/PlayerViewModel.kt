package org.parallel_sekai.kanade.ui.screens.player

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.parallel_sekai.kanade.data.repository.PlaybackRepository
import org.parallel_sekai.kanade.data.source.MusicModel

class PlayerViewModel(
    private val playbackRepository: PlaybackRepository,
    private val context: Context // 需要 Context 来初始化 Coil Request
) : ViewModel() {

    private val _state = MutableStateFlow(PlayerState())
    val state = _state.asStateFlow()
    private val imageLoader = ImageLoader(context)

    init {
        // 加载初始音乐列表
        viewModelScope.launch {
            val list = playbackRepository.fetchMusicList()
            val initialSong = list.firstOrNull()
            _state.update { it.copy(
                musicList = list,
                currentSong = initialSong
            ) }
            initialSong?.let { extractColors(it) }
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
                val song = state.value.musicList.find { it.id == mediaId }
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
                    val list = playbackRepository.fetchMusicList()
                    _state.update { it.copy(
                        musicList = list,
                        currentSong = it.currentSong ?: list.firstOrNull()
                    ) }
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
                val index = state.value.musicList.indexOf(intent.song).coerceAtLeast(0)
                playbackRepository.setPlaylist(state.value.musicList, index)
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
