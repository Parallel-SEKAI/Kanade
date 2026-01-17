package org.parallel_sekai.kanade.data.repository

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.parallel_sekai.kanade.data.model.*
import org.parallel_sekai.kanade.data.source.IMusicSource
import org.parallel_sekai.kanade.data.source.SourceManager
import org.parallel_sekai.kanade.data.source.local.LocalMusicSource
import org.parallel_sekai.kanade.service.KanadePlaybackService
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.launchIn

/**
 * 播放控制仓库，桥接 MVI ViewModel 与 Media3 Service
 */
open class PlaybackRepository(
    context: Context,
    private val settingsRepository: SettingsRepository,
    private val scope: kotlinx.coroutines.CoroutineScope // 注入外部作用域（通常是 applicationScope）
) {

    private val sourceManager = SourceManager.getInstance(context, settingsRepository)
    val scriptSources = sourceManager.scriptSources
    
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

    private val _currentPlaylist = MutableStateFlow<List<MediaItem>>(emptyList())
    val currentPlaylist = _currentPlaylist.asStateFlow()

    private val _artistJoinString = MutableStateFlow(", ") // 默认值
    val artistJoinString = _artistJoinString.asStateFlow()

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
        setupControllerListener()
        setupSettingsObservers()
        setupPeriodicTasks()
        
        scope.launch {
            sourceManager.refreshScripts()
        }
    }

    suspend fun refreshScriptSources() {
        sourceManager.refreshScripts()
    }

    suspend fun importScript(uri: Uri) {
        sourceManager.importScript(uri)
    }

    private val allSources: List<IMusicSource>
        get() = sourceManager.getAllSources()
    
    private val localMusicSource: LocalMusicSource
        get() = sourceManager.localMusicSource

    private fun setupControllerListener() {
        controllerFuture.addListener({
            if (controllerFuture.isDone) {
                val controller = controllerFuture.get()
                controller.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                        if (!isPlaying) {
                            scope.launch {
                                settingsRepository.updateLastPlayedPosition(controller.currentPosition)
                            }
                        }
                    }
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        _currentMediaId.value = mediaItem?.mediaId
                        scope.launch {
                            settingsRepository.updateLastPlayedMediaId(mediaItem?.mediaId)
                            settingsRepository.updateLastPlayedPosition(controller.currentPosition)
                        }
                    }
                    override fun onRepeatModeChanged(repeatMode: Int) {
                        _repeatMode.value = repeatMode
                        scope.launch {
                            settingsRepository.updateRepeatMode(repeatMode)
                        }
                    }
                    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                        _shuffleModeEnabled.value = shuffleModeEnabled
                        scope.launch {
                            settingsRepository.updateShuffleMode(shuffleModeEnabled)
                        }
                    }

                    override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                        updatePlaylist(controller)
                        val ids = (0 until controller.mediaItemCount).map {
                            controller.getMediaItemAt(it).mediaId
                        }
                        scope.launch {
                            settingsRepository.updateLastPlaylistIds(ids)
                        }
                    }
                })
                // 初始化状态
                _repeatMode.value = controller.repeatMode
                _shuffleModeEnabled.value = controller.shuffleModeEnabled
                _currentMediaId.value = controller.currentMediaItem?.mediaId
                updatePlaylist(controller)

                // 恢复播放状态 (如果当前没有播放内容)
                if (controller.mediaItemCount == 0) {
                    scope.launch {
                        restorePlaybackState(controller)
                    }
                }
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setupSettingsObservers() {
        // 监听 artistParsingSettingsFlow
        settingsRepository.artistParsingSettingsFlow
            .onEach { settings ->
                _artistJoinString.value = settings.joinString
            }.launchIn(scope)

        // 监听 excludedFoldersFlow
        settingsRepository.excludedFoldersFlow
            .onEach { folders ->
                localMusicSource.excludedFolders = folders
            }.launchIn(scope)
    }

    private fun setupPeriodicTasks() {
        // 周期性保存播放进度
        flow {
            while (true) {
                emit(Unit)
                delay(10000) // 每 10 秒保存一次
            }
        }.onEach {
            if (controllerFuture.isDone) {
                val controller = controllerFuture.get()
                if (controller.isPlaying) {
                    settingsRepository.updateLastPlayedPosition(controller.currentPosition)
                }
            }
        }.launchIn(scope)
    }

    private suspend fun restorePlaybackState(controller: MediaController) {
        val state = settingsRepository.playbackStateFlow.first()
        if (state.lastPlaylistIds.isNotEmpty()) {
            val musicList = localMusicSource.getMusicListByIds(state.lastPlaylistIds)
            if (musicList.isNotEmpty()) {
                val mediaItems = musicList.map { music ->
                    createMediaItem(music)
                }
                val startIndex = state.lastPlaylistIds.indexOf(state.lastMediaId).coerceAtLeast(0)
                controller.setMediaItems(mediaItems, startIndex, state.lastPosition)
                controller.repeatMode = state.repeatMode
                controller.shuffleModeEnabled = state.shuffleMode
                controller.prepare()
                // 手动更新一次 ID，确保 UI 在恢复后立即响应
                _currentMediaId.value = controller.currentMediaItem?.mediaId
            }
        }
    }

    private fun updatePlaylist(controller: MediaController) {
        val items = mutableListOf<MediaItem>()
        for (i in 0 until controller.mediaItemCount) {
            items.add(controller.getMediaItemAt(i))
        }
        _currentPlaylist.value = items
    }

    fun setRepeatMode(mode: Int) {
        if (controllerFuture.isDone) controllerFuture.get().repeatMode = mode
    }

    fun setShuffleModeEnabled(enabled: Boolean) {
        if (controllerFuture.isDone) controllerFuture.get().shuffleModeEnabled = enabled
    }

    fun setExcludedFolders(folders: Set<String>) {
        localMusicSource.excludedFolders = folders
    }

    suspend fun fetchMusicList(query: String = "", sourceIds: List<String>? = null): List<MusicModel> = coroutineScope {
        if (query.isEmpty()) {
            localMusicSource.getMusicList("")
        } else {
            val sourcesToSearch = if (sourceIds == null) {
                allSources
            } else {
                allSources.filter { it.sourceId in sourceIds }
            }

            sourcesToSearch.map { source ->
                async { 
                    try {
                        source.getMusicList(query)
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            }.awaitAll().flatten()
        }
    }

    suspend fun fetchArtistList(): List<ArtistModel> {
        return localMusicSource.getArtistList()
    }

    suspend fun fetchAlbumList(): List<AlbumModel> {
        return localMusicSource.getAlbumList()
    }

    suspend fun fetchFolderList(): List<FolderModel> {
        return localMusicSource.getFolderList()
    }

    suspend fun fetchSongsByArtist(artistName: String): List<MusicModel> {
        return localMusicSource.getSongsByArtist(artistName)
    }

    suspend fun fetchSongsByAlbum(albumId: String): List<MusicModel> {
        return localMusicSource.getSongsByAlbum(albumId)
    }

    suspend fun fetchSongsByFolder(path: String): List<MusicModel> {
        return localMusicSource.getSongsByFolder(path)
    }

    suspend fun fetchPlaylistList(): List<PlaylistModel> {
        return localMusicSource.getPlaylistList()
    }

    suspend fun fetchHomeList(): List<MusicModel> {
        return sourceManager.getHomeList()
    }

    suspend fun fetchSongsByPlaylist(playlistId: String): List<MusicModel> {
        return localMusicSource.getSongsByPlaylist(playlistId)
    }

    suspend fun fetchLyrics(musicId: String): String? {
        return localMusicSource.getLyrics(musicId)
    }

    fun setPlaylist(list: List<MusicModel>, startIndex: Int = 0) {
        if (controllerFuture.isDone) {
            val controller = controllerFuture.get()
            val mediaItems = list.map { music ->
                createMediaItem(music)
            }
            
            controller.setMediaItems(mediaItems, startIndex, 0L)
            controller.prepare()
            controller.play()
        }
    }

    private fun createMediaItem(music: MusicModel): MediaItem {
        val extras = android.os.Bundle().apply {
            putString("source_id", music.sourceId)
        }
        return MediaItem.Builder()
            .setMediaId(music.id)
            .setUri(music.mediaUri)
            .setRequestMetadata(
                androidx.media3.common.MediaItem.RequestMetadata.Builder()
                    .setMediaUri(android.net.Uri.parse(music.mediaUri))
                    .build()
            )
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(music.title)
                    .setArtist(music.artists.joinToString(_artistJoinString.value))
                    .setAlbumTitle(music.album)
                    .setArtworkUri(android.net.Uri.parse(music.coverUrl))
                    .setExtras(extras)
                    .build()
            )
            .build()
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
