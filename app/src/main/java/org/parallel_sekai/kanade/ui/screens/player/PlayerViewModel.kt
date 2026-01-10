package org.parallel_sekai.kanade.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.parallel_sekai.kanade.data.repository.PlaybackRepository

class PlayerViewModel(
    private val playbackRepository: PlaybackRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PlayerState())
    val state = _state.asStateFlow()

    init {
        // 加载初始音乐列表
        viewModelScope.launch {
            val list = playbackRepository.fetchMusicList()
            _state.update { it.copy(currentSong = list.firstOrNull()) }
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
                    // 异步加载歌词
                    viewModelScope.launch {
                        val lyrics = playbackRepository.fetchLyrics(song.id)
                        android.util.Log.d("LyricDebug", "Fetched lyrics for ${song.title}: ${lyrics?.take(50)}...")
                        val lyricData = lyrics?.let { LyricParserFactory.getParser(it).parse(it) }
                        android.util.Log.d("LyricDebug", "Parsed lines: ${lyricData?.lines?.size ?: 0}")
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

    override fun onCleared() {
        super.onCleared()
        playbackRepository.release()
    }
}
