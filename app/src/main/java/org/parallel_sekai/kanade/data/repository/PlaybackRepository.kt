package org.parallel_sekai.kanade.data.repository

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import org.parallel_sekai.kanade.data.source.MusicModel
import org.parallel_sekai.kanade.data.source.local.LocalMusicSource
import org.parallel_sekai.kanade.service.KanadePlaybackService

/**
 * 播放控制仓库，桥接 MVI ViewModel 与 Media3 Service
 */
class PlaybackRepository(context: Context) {

    private val localMusicSource = LocalMusicSource(context)
    private val sessionToken = SessionToken(context, ComponentName(context, KanadePlaybackService::class.java))
    private val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentMediaId = MutableStateFlow<String?>(null)
    val currentMediaId = _currentMediaId.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode = _repeatMode.asStateFlow()

    private val _shuffleModeEnabled = MutableStateFlow(false)
    val shuffleModeEnabled = _shuffleModeEnabled.asStateFlow()

    /**
     * 根据屏幕刷新率动态计算延迟时间
     */
    private val frameDelay: Long by lazy {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
        val refreshRate = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                context.display?.refreshRate ?: 60f
            } catch (e: Exception) {
                60f
            }
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.refreshRate
        }
        (1000 / refreshRate.coerceAtLeast(60f)).toLong()
    }

    /**
     * 提供实时的播放进度和时长
     */
    val progressFlow: Flow<Pair<Long, Long>> = flow {
        while (true) {
            if (controllerFuture.isDone) {
                val controller = controllerFuture.get()
                emit(controller.currentPosition to controller.duration.coerceAtLeast(0L))
            }
            delay(frameDelay) 
        }
    }

    init {
        controllerFuture.addListener({
            if (controllerFuture.isDone) {
                val controller = controllerFuture.get()
                controller.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                    }
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        _currentMediaId.value = mediaItem?.mediaId
                    }
                    override fun onRepeatModeChanged(repeatMode: Int) {
                        _repeatMode.value = repeatMode
                    }
                    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                        _shuffleModeEnabled.value = shuffleModeEnabled
                    }
                })
                // 初始化状态
                _repeatMode.value = controller.repeatMode
                _shuffleModeEnabled.value = controller.shuffleModeEnabled
            }
        }, MoreExecutors.directExecutor())
    }

    fun setRepeatMode(mode: Int) {
        if (controllerFuture.isDone) controllerFuture.get().repeatMode = mode
    }

    fun setShuffleModeEnabled(enabled: Boolean) {
        if (controllerFuture.isDone) controllerFuture.get().shuffleModeEnabled = enabled
    }

    suspend fun fetchMusicList(query: String = ""): List<MusicModel> {
        return localMusicSource.getMusicList(query)
    }

    suspend fun fetchLyrics(musicId: String): String? {
        return localMusicSource.getLyrics(musicId)
    }

    fun setPlaylist(list: List<MusicModel>, startIndex: Int = 0) {
        if (controllerFuture.isDone) {
            val controller = controllerFuture.get()
            val mediaItems = list.map { music ->
                MediaItem.Builder()
                    .setMediaId(music.id)
                    .setUri(music.mediaUri)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(music.title)
                            .setArtist(music.artist)
                            .setAlbumTitle(music.album)
                            .setArtworkUri(android.net.Uri.parse(music.coverUrl))
                            .build()
                    )
                    .build()
            }
            
            controller.setMediaItems(mediaItems, startIndex, 0L)
            controller.prepare()
            controller.play()
        }
    }

    fun play() {
        if (controllerFuture.isDone) controllerFuture.get().play()
    }

    fun pause() {
        if (controllerFuture.isDone) controllerFuture.get().pause()
    }

    fun next() {
        if (controllerFuture.isDone) controllerFuture.get().seekToNext()
    }

    fun previous() {
        if (controllerFuture.isDone) controllerFuture.get().seekToPrevious()
    }

    fun seekTo(position: Long) {
        if (controllerFuture.isDone) controllerFuture.get().seekTo(position)
    }

    fun release() {
        MediaController.releaseFuture(controllerFuture)
    }
}
