package org.parallel_sekai.kanade.service

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.guava.future
import org.parallel_sekai.kanade.MainActivity
import org.parallel_sekai.kanade.data.repository.SettingsRepository
import org.parallel_sekai.kanade.data.source.SourceManager
import org.parallel_sekai.kanade.data.utils.CacheManager

/**
 * 后台播放服务，基于 Media3 实现
 */
class KanadePlaybackService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        
        // 1. 初始化 ExoPlayer
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true) 
            .setWakeMode(C.WAKE_MODE_NETWORK) 
            .setMediaSourceFactory(
                androidx.media3.exoplayer.source.DefaultMediaSourceFactory(this)
                    .setDataSourceFactory(CacheManager.getCacheDataSourceFactory(this))
            )
            .build().apply {
                addListener(object : androidx.media3.common.Player.Listener {
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        android.util.Log.e("KanadePlaybackService", "Player Error: ${error.message}", error)
                    }
                })
            }

        // 2. 初始化 MediaSession
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_EXPAND_PLAYER, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        player?.let {
            mediaSession = MediaSession.Builder(this, it)
                .setSessionActivity(pendingIntent)
                .setCallback(KanadeSessionCallback())
                .build()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    private inner class KanadeSessionCallback : MediaSession.Callback {
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            return serviceScope.future {
                mediaItems.mapNotNull { item ->
                    val uri = item.requestMetadata.mediaUri
                    if (uri == null || uri.toString().isEmpty()) {
                        resolveMediaItem(item)
                    } else {
                        item
                    }
                }.toMutableList()
            }
        }

        private suspend fun resolveMediaItem(item: MediaItem): MediaItem? {
            val extras = item.mediaMetadata.extras ?: return null
            val sourceId = extras.getString("source_id") ?: return null
            val originalId = extras.getString("original_id") ?: item.mediaId
            
            val settingsRepository = SettingsRepository(this@KanadePlaybackService)
            val sourceManager = SourceManager.getInstance(this@KanadePlaybackService, settingsRepository)
            val source = sourceManager.getSource(sourceId) ?: return null
            
            val playUrl = try {
                source.getPlayUrl(originalId)
            } catch (e: Exception) {
                ""
            }
            if (playUrl.isEmpty()) return null

            return item.buildUpon()
                .setUri(playUrl)
                .setRequestMetadata(
                    item.requestMetadata.buildUpon()
                        .setMediaUri(android.net.Uri.parse(playUrl))
                        .build()
                )
                .build()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
