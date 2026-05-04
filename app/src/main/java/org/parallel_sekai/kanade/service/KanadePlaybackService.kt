@file:Suppress("ktlint:standard:max-line-length")

package org.parallel_sekai.kanade.service

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.FlagSet
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.parallel_sekai.kanade.MainActivity
import org.parallel_sekai.kanade.data.repository.SettingsRepository
import org.parallel_sekai.kanade.data.source.SourceManager
import org.parallel_sekai.kanade.data.utils.CacheManager
import org.parallel_sekai.kanade.data.utils.MediaNotificationLyricsState
import org.parallel_sekai.kanade.data.utils.UrlCacheManager

/**
 * 后台播放服务，基于 Media3 实现
 */
@OptIn(UnstableApi::class)
class KanadePlaybackService : MediaSessionService() {
    private var exoPlayer: ExoPlayer? = null
    private var metadataOverridePlayer: MetadataOverridePlayer? = null
    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var urlCacheManager: UrlCacheManager

    private var lastFailedMediaId: String? = null
    private var retryCount = 0

    override fun onCreate() {
        super.onCreate()

        urlCacheManager = UrlCacheManager(this)

        // 1. 初始化 ExoPlayer
        val audioAttributes =
            AudioAttributes
                .Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build()

        val settingsRepository = SettingsRepository(this)
        val sourceManager = SourceManager.getInstance(this, settingsRepository)
        val maxCacheSize =
            runBlocking {
                settingsRepository.maxCacheSizeFlow.first()
            }

        val resolvingDataSourceFactory =
            ResolvingDataSource.Factory(
                CacheManager.getCacheDataSourceFactory(this, maxCacheSize),
                object : ResolvingDataSource.Resolver {
                    override fun resolveDataSpec(
                        dataSpec: androidx.media3.datasource.DataSpec,
                    ): androidx.media3.datasource.DataSpec {
                        val uri = dataSpec.uri

                        if (uri.scheme == "kanade" && uri.host == "resolve") {
                            val sourceId = uri.getQueryParameter("source_id")
                            val originalId = uri.getQueryParameter("original_id")
                            val mediaId = "$sourceId:$originalId"

                            if (sourceId != null && originalId != null) {
                                val resolvedUrl =
                                    runBlocking {
                                        // 1. 尝试从永久缓存获取
                                        val cached = urlCacheManager.getCachedUrl(mediaId)
                                        if (cached != null) {
                                            android.util.Log.d(
                                                "KanadePlaybackService",
                                                "Using persistent cached URL for $mediaId",
                                            )
                                            return@runBlocking cached
                                        }

                                        // 2. 缓存不存在，重新获取
                                        android.util.Log.d(
                                            "KanadePlaybackService",
                                            "Cache miss for $mediaId, resolving...",
                                        )
                                        val source = sourceManager.getSource(sourceId)
                                        val url =
                                            try {
                                                source?.getPlayUrl(originalId) ?: ""
                                            } catch (e: Exception) {
                                                ""
                                            }

                                        // 3. 保存到永久缓存
                                        if (url.isNotEmpty()) {
                                            urlCacheManager.saveUrl(mediaId, url)
                                        }
                                        url
                                    }

                                if (resolvedUrl.isNotEmpty()) {
                                    return dataSpec.buildUpon().setUri(android.net.Uri.parse(resolvedUrl)).build()
                                }
                            }
                        }
                        return dataSpec
                    }
                },
            )

        val player =
            ExoPlayer
                .Builder(this)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .setMediaSourceFactory(
                    androidx.media3.exoplayer.source
                        .DefaultMediaSourceFactory(this)
                        .setDataSourceFactory(resolvingDataSourceFactory),
                ).build()
                .apply {
                    addListener(
                        object : androidx.media3.common.Player.Listener {
                            override fun onMediaItemTransition(
                                mediaItem: MediaItem?,
                                reason: Int,
                            ) {
                                // 切换歌曲时重置重试计数
                                if (reason == androidx.media3.common.Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
                                    reason == androidx.media3.common.Player.MEDIA_ITEM_TRANSITION_REASON_SEEK ||
                                    reason ==
                                    androidx.media3.common.Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED
                                ) {
                                    lastFailedMediaId = null
                                    retryCount = 0
                                }
                            }

                            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                                android.util.Log.e("KanadePlaybackService", "Player Error: ${error.message}", error)

                                // 如果播放出错且当前是脚本音源，尝试清除 URL 缓存，以便下次重试时重新解析
                                val currentItem = currentMediaItem
                                if (currentItem != null) {
                                    val mediaId = currentItem.mediaId
                                    if (mediaId.startsWith("script_")) {
                                        serviceScope.launch {
                                            urlCacheManager.clearCache(mediaId)
                                            android.util.Log.d(
                                                "KanadePlaybackService",
                                                "Cleared URL cache for $mediaId due to error",
                                            )

                                            // 如果是 403 错误，尝试自动重试一次
                                            val cause = error.cause
                                            val is403 =
                                                cause is
                                                    androidx.media3.datasource.HttpDataSource
                                                        .InvalidResponseCodeException &&
                                                    cause.responseCode == 403
                                            if (is403) {
                                                if (lastFailedMediaId != mediaId || retryCount < 1) {
                                                    lastFailedMediaId = mediaId
                                                    retryCount++
                                                    android.util.Log.w(
                                                        "KanadePlaybackService",
                                                        "Detected 403 error for $mediaId, retrying... (attempt $retryCount)",
                                                    )
                                                    prepare()
                                                    play()
                                                } else {
                                                    // 已重试过仍然失败，重置计数
                                                    lastFailedMediaId = null
                                                    retryCount = 0
                                                }
                                            } else {
                                                // 非 403 错误，重置计数
                                                lastFailedMediaId = null
                                                retryCount = 0
                                            }
                                        }
                                        return
                                    }
                                }

                                // 非脚本音源或无法重试的情况，确保重置
                                lastFailedMediaId = null
                                retryCount = 0
                            }
                        },
                    )
                }

        exoPlayer = player

        // 创建 MetadataOverridePlayer 包装真实 ExoPlayer
        metadataOverridePlayer = MetadataOverridePlayer(player)

        // 2. 初始化 MediaSession，使用 MetadataOverridePlayer
        val intent =
            Intent(this, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_EXPAND_PLAYER, true)
            }
        val pendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        metadataOverridePlayer?.let {
            mediaSession =
                MediaSession
                    .Builder(this, it)
                    .setSessionActivity(pendingIntent)
                    .setCallback(KanadeSessionCallback())
                    .build()
        }

        // 监听 MediaNotificationLyricsState.overrideFlow，状态变化时通知 metadata 更新
        serviceScope.launch {
            MediaNotificationLyricsState.overrideFlow.collect { override ->
                android.util.Log.d(
                    "KanadePlaybackService",
                    "Override changed: ${override?.title} - ${override?.artist}",
                )
                metadataOverridePlayer?.notifyMetadataChanged()
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    /**
     * MetadataOverridePlayer - 包装 ExoPlayer，根据 MediaNotificationLyricsState 覆盖 MediaMetadata
     */
    private inner class MetadataOverridePlayer(
        private val wrappedPlayer: Player,
    ) : androidx.media3.common.ForwardingPlayer(wrappedPlayer) {
        private val listeners = LinkedHashSet<Player.Listener>()

        override fun getMediaMetadata(): MediaMetadata {
            val override = MediaNotificationLyricsState.currentOverride
            return if (override != null) {
                // 基于原始 metadata 构建新的 metadata，只覆盖 title 和 artist
                wrappedPlayer.mediaMetadata
                    .buildUpon()
                    .setTitle(override.title)
                    .setArtist(override.artist)
                    .build()
            } else {
                wrappedPlayer.mediaMetadata
            }
        }

        override fun addListener(listener: Player.Listener) {
            super.addListener(listener)
            synchronized(listeners) {
                listeners.add(listener)
            }
        }

        override fun removeListener(listener: Player.Listener) {
            super.removeListener(listener)
            synchronized(listeners) {
                listeners.remove(listener)
            }
        }

        /**
         * 通知所有 listener metadata 已变化
         */
        @OptIn(UnstableApi::class)
        fun notifyMetadataChanged() {
            val currentMetadata = getMediaMetadata()
            val listenersCopy =
                synchronized(listeners) {
                    listeners.toList()
                }

            listenersCopy.forEach { listener ->
                try {
                    listener.onMediaMetadataChanged(currentMetadata)
                } catch (e: Exception) {
                    android.util.Log.e(
                        "KanadePlaybackService",
                        "Error notifying listener",
                        e,
                    )
                }
            }

            // 通知 onEvents，包含 EVENT_MEDIA_METADATA_CHANGED
            val flagSet =
                FlagSet
                    .Builder()
                    .add(Player.EVENT_MEDIA_METADATA_CHANGED)
                    .build()
            val events = Player.Events(flagSet)
            listenersCopy.forEach { listener ->
                try {
                    listener.onEvents(this, events)
                } catch (e: Exception) {
                    android.util.Log.e(
                        "KanadePlaybackService",
                        "Error notifying events",
                        e,
                    )
                }
            }
        }
    }

    private inner class KanadeSessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            android.util.Log.d("KanadePlaybackService", "Connecting: ${controller.packageName}")

            // 显式授予所有可用的指令
            val availableSessionCommands =
                MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                    .buildUpon()
                    .build()
            val availablePlayerCommands =
                MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS
                    .buildUpon()
                    .build()

            return MediaSession.ConnectionResult
                .AcceptedResultBuilder(session)
                .setAvailableSessionCommands(availableSessionCommands)
                .setAvailablePlayerCommands(availablePlayerCommands)
                .build()
        }

        override fun onPostConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ) {
            super.onPostConnect(session, controller)
            android.util.Log.d("KanadePlaybackService", "Post connect from ${controller.packageName}")
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> {
            // 不再在此处解析 URL，直接返回包含元数据的项目
            // ResolvingDataSource 会在播放前自动处理
            return Futures.immediateFuture(mediaItems)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        exoPlayer?.release()
        mediaSession?.run {
            release()
            mediaSession = null
        }
        metadataOverridePlayer = null
        exoPlayer = null
        super.onDestroy()
    }
}
