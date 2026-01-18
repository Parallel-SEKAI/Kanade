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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

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
    private var controller: MediaController? = null
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentMediaId = MutableStateFlow<String?>(null)
    val currentMediaId = _currentMediaId.asStateFlow()

    private val _currentMediaItem = MutableStateFlow<MediaItem?>(null)
    val currentMediaItem = _currentMediaItem.asStateFlow()

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
                context.display.refreshRate
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
            controller?.let {
                emit(it.currentPosition to it.duration.coerceAtLeast(0L))
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
            try {
                val ctrl = controllerFuture.get()
                controller = ctrl
                ctrl.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                        if (!isPlaying) {
                            savePlaybackState()
                        }
                    }
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        _currentMediaId.value = mediaItem?.mediaId
                        _currentMediaItem.value = mediaItem
                        savePlaybackState()
                    }
                    override fun onRepeatModeChanged(repeatMode: Int) {
                        _repeatMode.value = repeatMode
                        savePlaybackState()
                    }
                    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                        _shuffleModeEnabled.value = shuffleModeEnabled
                        savePlaybackState()
                    }

                    override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                        updatePlaylist(ctrl)
                        savePlaybackState()
                    }
                })
                // 初始化状态
                _repeatMode.value = ctrl.repeatMode
                _shuffleModeEnabled.value = ctrl.shuffleModeEnabled
                _currentMediaId.value = ctrl.currentMediaItem?.mediaId
                _currentMediaItem.value = ctrl.currentMediaItem
                updatePlaylist(ctrl)

                // 恢复播放状态
                scope.launch {
                    android.util.Log.d("PlaybackRepository", "Checking for playback restoration. Current item count: ${ctrl.mediaItemCount}")
                    if (ctrl.mediaItemCount == 0) {
                        restorePlaybackState(ctrl)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PlaybackRepository", "Failed to connect to MediaSession", e)
            }
        }, MoreExecutors.directExecutor())
    }

    private fun savePlaybackState() {
        val ctrl = controller ?: return
        val currentId = ctrl.currentMediaItem?.mediaId
        val position = ctrl.currentPosition
        val repeatMode = ctrl.repeatMode
        val shuffleMode = ctrl.shuffleModeEnabled
        
        // Convert MediaItems back to MusicModels for serialization
        val playlist = (0 until ctrl.mediaItemCount).map { i ->
            val item = ctrl.getMediaItemAt(i)
            MusicModel(
                id = item.mediaMetadata.extras?.getString("original_id") ?: item.mediaId,
                title = item.mediaMetadata.title?.toString() ?: "",
                artists = item.mediaMetadata.artist?.toString()?.split(_artistJoinString.value) ?: emptyList(),
                album = item.mediaMetadata.albumTitle?.toString() ?: "",
                coverUrl = item.mediaMetadata.artworkUri?.toString() ?: "",
                mediaUri = item.requestMetadata.mediaUri?.toString() ?: "",
                duration = item.mediaMetadata.extras?.getLong("duration") ?: 0L,
                sourceId = item.mediaMetadata.extras?.getString("source_id") ?: "local_storage"
            )
        }

        scope.launch {
            try {
                val json = Json.encodeToString<List<MusicModel>>(playlist)
                settingsRepository.updateLastPlayedMediaId(currentId)
                settingsRepository.updateLastPlayedPosition(position)
                settingsRepository.updateRepeatMode(repeatMode)
                settingsRepository.updateShuffleMode(shuffleMode)
                settingsRepository.updateLastPlaylistJson(json)
            } catch (e: Exception) {
                android.util.Log.e("PlaybackRepository", "Failed to save playback state JSON", e)
            }
        }
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
            if (isPlaying.value) {
                savePlaybackState()
            }
        }.launchIn(scope)
    }

    private suspend fun restorePlaybackState(controller: MediaController) {
        val state = settingsRepository.playbackStateFlow.first()
        android.util.Log.d("PlaybackRepository", "Restoring playback state from JSON. Last ID: ${state.lastMediaId}")
        
        if (!state.lastPlaylistJson.isNullOrBlank()) {
            try {
                val musicList = Json.decodeFromString<List<MusicModel>>(state.lastPlaylistJson)
                android.util.Log.d("PlaybackRepository", "Decoded ${musicList.size} music items from JSON")
                
                if (musicList.isNotEmpty()) {
                    val mediaItems = musicList.map { music ->
                        createMediaItem(music)
                    }
                    val startIndex = musicList.indexOfFirst { "${it.sourceId}:${it.id}" == state.lastMediaId }.coerceAtLeast(0)
                    
                    // 设置列表并跳转到保存的位置
                    controller.setMediaItems(mediaItems, startIndex, state.lastPosition)
                    controller.repeatMode = state.repeatMode
                    controller.shuffleModeEnabled = state.shuffleMode
                    controller.prepare()
                    
                    android.util.Log.d("PlaybackRepository", "Restoration applied via JSON: index=$startIndex, pos=${state.lastPosition}")

                    // 显式且立即更新内部状态
                    val restoredItem = mediaItems.getOrNull(startIndex)
                    if (restoredItem != null) {
                        _currentMediaId.value = restoredItem.mediaId
                        _currentMediaItem.value = restoredItem
                    }
                    
                    _repeatMode.value = state.repeatMode
                    _shuffleModeEnabled.value = state.shuffleMode
                    updatePlaylist(controller, mediaItems)
                }
            } catch (e: Exception) {
                android.util.Log.e("PlaybackRepository", "Failed to restore playback state from JSON", e)
            }
        } else {
            android.util.Log.d("PlaybackRepository", "No last playlist JSON found to restore")
        }
    }

    private fun updatePlaylist(controller: MediaController, items: List<MediaItem>? = null) {
        if (items != null) {
            _currentPlaylist.value = items
            return
        }
        val currentItems = mutableListOf<MediaItem>()
        for (i in 0 until controller.mediaItemCount) {
            currentItems.add(controller.getMediaItemAt(i))
        }
        _currentPlaylist.value = currentItems
    }

    fun setRepeatMode(mode: Int) {
        controller?.repeatMode = mode
    }

    fun setShuffleModeEnabled(enabled: Boolean) {
        controller?.shuffleModeEnabled = enabled
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

    suspend fun fetchLyrics(musicId: String, sourceId: String? = null): String? {
        if (sourceId == null || sourceId == localMusicSource.sourceId) {
            return localMusicSource.getLyrics(musicId)
        }
        return sourceManager.getLyrics(sourceId, musicId)
    }

    fun setPlaylist(list: List<MusicModel>, startIndex: Int = 0) {
        controller?.let { ctrl ->
            val mediaItems = list.map { music ->
                createMediaItem(music)
            }
            
            ctrl.setMediaItems(mediaItems, startIndex, 0L)
            ctrl.prepare()
            ctrl.play()
        }
    }

    private fun createMediaItem(music: MusicModel): MediaItem {
        val uniqueId = "${music.sourceId}:${music.id}"
        val extras = android.os.Bundle().apply {
            putString("source_id", music.sourceId)
            putString("original_id", music.id)
            putLong("duration", music.duration)
        }
        
        val isLocal = music.sourceId == localMusicSource.sourceId
        val mediaUri = if (isLocal) {
            music.mediaUri
        } else {
            "kanade://resolve?source_id=${music.sourceId}&original_id=${music.id}"
        }

        val builder = MediaItem.Builder()
            .setMediaId(uniqueId)
            .setUri(mediaUri)
            .setRequestMetadata(
                androidx.media3.common.MediaItem.RequestMetadata.Builder()
                    .setMediaUri(android.net.Uri.parse(mediaUri))
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

        return builder.build()
    }

    fun play() {
        controller?.play()
    }

    fun pause() {
        controller?.pause()
    }

    fun next() {
        controller?.seekToNext()
    }

    fun previous() {
        controller?.seekToPrevious()
    }

    fun seekTo(position: Long) {
        controller?.seekTo(position)
    }

    fun release() {
        MediaController.releaseFuture(controllerFuture)
    }
}
