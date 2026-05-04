package org.parallel_sekai.kanade.data.utils

import android.content.Context
import android.util.Log
import cn.lyric.getter.api.API
import cn.lyric.getter.api.data.ExtraData
import com.hchen.superlyricapi.SuperLyricData
import com.hchen.superlyricapi.SuperLyricPush
import com.hchen.superlyricapi.SuperLyricTool
import io.github.proify.lyricon.lyric.model.Song
import io.github.proify.lyricon.provider.LyriconFactory
import io.github.proify.lyricon.provider.LyriconProvider
import org.parallel_sekai.kanade.data.model.LyricData
import org.parallel_sekai.kanade.data.model.LyriconApiSettings
import org.parallel_sekai.kanade.data.model.MusicModel

/**
 * Manager for interacting with external lyric APIs (Lyric-Getter-API and SuperLyricApi).
 * This allows other apps to receive lyric updates from Kanade.
 */
class LyricGetterManager(
    context: Context,
) {
    private val lyricGetterApi = API()
    private val packageName = context.packageName
    private val appContext = context.applicationContext

    // Lyricon Provider
    private var lyriconProvider: LyriconProvider? = null
    private var lyriconRegistered = false

    companion object {
        private const val TAG = "LyricGetterManager"
    }

    init {
        // Initialize Lyricon Provider
        initializeLyricon()
    }

    /**
     * Initialize Lyricon Provider.
     */
    private fun initializeLyricon() {
        try {
            lyriconProvider = LyriconFactory.createProvider(appContext)
            lyriconRegistered = lyriconProvider?.register() ?: false
            if (lyriconRegistered) {
                // Set default display options
                lyriconProvider?.player?.setDisplayTranslation(true)
                lyriconProvider?.player?.setDisplayRoma(false)
                Log.d(TAG, "Lyricon Provider registered successfully")
            } else {
                Log.w(TAG, "Lyricon Provider registration returned false")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Lyricon Provider", e)
            lyriconProvider = null
            lyriconRegistered = false
        }
    }

    /**
     * Check if any lyric API is activated.
     */
    val isActivated: Boolean get() = isLyricGetterActivated || isSuperLyricActivated || isLyriconActivated

    val isLyricGetterActivated: Boolean get() =
        try {
            lyricGetterApi.hasEnable
        } catch (e: Exception) {
            false
        }

    val isSuperLyricActivated: Boolean get() =
        try {
            SuperLyricTool.isEnabled
        } catch (e: Exception) {
            false
        }

    val isLyriconActivated: Boolean get() =
        try {
            lyriconRegistered && lyriconProvider?.player?.isActive == true
        } catch (e: Exception) {
            false
        }

    /**
     * Send current lyric to LyricGetter API only.
     * @param lyric The current lyric line text (already formatted with timestamp/scrolling if needed).
     * @param translation The current lyric line translation.
     * @param song The current playing music model for metadata.
     */
    fun sendLyricGetter(
        lyric: String,
        translation: String? = null,
        song: MusicModel? = null,
    ) {
        if (lyric.isBlank()) {
            clearLyricGetter()
            return
        }

        try {
            val extra =
                ExtraData().apply {
                    packageName = this@LyricGetterManager.packageName
                }
            lyricGetterApi.sendLyric(lyric, extra)
            Log.d(TAG, "Sent to LyricGetter: $lyric")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send to Lyric-Getter-API", e)
        }
    }

    /**
     * Send current lyric to SuperLyric API only.
     * @param lyric The current lyric line text (already formatted with timestamp/scrolling if needed).
     * @param translation The current lyric line translation.
     * @param song The current playing music model for metadata.
     * @param delay The duration of the current lyric line in ms.
     * @param words The word-by-word lyric data for the current line.
     */
    fun sendSuperLyric(
        lyric: String,
        translation: String? = null,
        song: MusicModel? = null,
        delay: Long = 0,
        words: List<org.parallel_sekai.kanade.data.model.WordInfo> = emptyList(),
    ) {
        if (lyric.isBlank()) {
            clearSuperLyric()
            return
        }

        try {
            val safeDelay = delay.coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
            val data =
                SuperLyricData()
                    .setLyric(lyric)
                    .setPackageName(packageName)
                    .setDelay(safeDelay)

            translation?.let { data.setTranslation(it) }

            if (words.isNotEmpty()) {
                val enhancedData =
                    words
                        .map {
                            val duration = (it.endTime - it.startTime).coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
                            SuperLyricData.EnhancedLRCData(it.text, duration)
                        }.toTypedArray()
                data.setEnhancedLRCData(enhancedData)
            }

            SuperLyricPush.onSuperLyric(data)
            Log.d(TAG, "Sent to SuperLyric: $lyric")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send to SuperLyricApi", e)
        }
    }

    /**
     * Clear the current lyric from LyricGetter API only.
     */
    fun clearLyricGetter() {
        try {
            lyricGetterApi.clearLyric()
            Log.d(TAG, "Cleared LyricGetter")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear Lyric-Getter-API", e)
        }
    }

    /**
     * Clear the current lyric from SuperLyric API only.
     */
    fun clearSuperLyric() {
        try {
            val stopData = SuperLyricData().setPackageName(packageName)
            SuperLyricPush.onStop(stopData)
            Log.d(TAG, "Cleared SuperLyric")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear SuperLyricApi", e)
        }
    }

    /**
     * Send current lyric to all supported APIs (legacy method for compatibility).
     * @param lyric The current lyric line text.
     * @param translation The current lyric line translation.
     * @param song The current playing music model for metadata.
     * @param delay The duration of the current lyric line in ms.
     * @param words The word-by-word lyric data for the current line.
     */
    @Deprecated("Use sendLyricGetter() and sendSuperLyric() separately for better control")
    fun sendLyric(
        lyric: String,
        translation: String? = null,
        song: MusicModel? = null,
        delay: Long = 0,
        words: List<org.parallel_sekai.kanade.data.model.WordInfo> = emptyList(),
    ) {
        sendLyricGetter(lyric, translation, song)
        sendSuperLyric(lyric, translation, song, delay, words)
    }

    /**
     * Clear the current lyric from all APIs (legacy method for compatibility).
     */
    @Deprecated("Use clearLyricGetter() and clearSuperLyric() separately for better control")
    fun clearLyric() {
        clearLyricGetter()
        clearSuperLyric()
    }

    // ========== Lyricon Provider Methods ==========

    /**
     * Set the current song for Lyricon Provider.
     * @param song The Lyricon Song model (use LyriconAdapter to convert from MusicModel).
     */
    fun setLyriconSong(song: Song?) {
        if (!lyriconRegistered) return

        try {
            lyriconProvider?.player?.setSong(song)
            Log.d(TAG, "Set Lyricon song: ${song?.name ?: "null"}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set Lyricon song", e)
        }
    }

    /**
     * Update Lyricon playback state.
     * @param isPlaying True if playing, false if paused/stopped.
     */
    fun updateLyriconPlaybackState(isPlaying: Boolean) {
        if (!lyriconRegistered) return

        try {
            lyriconProvider?.player?.setPlaybackState(isPlaying)
            Log.d(TAG, "Updated Lyricon playback state: $isPlaying")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update Lyricon playback state", e)
        }
    }

    /**
     * Update Lyricon playback position.
     * @param positionMs Current playback position in milliseconds.
     */
    fun updateLyriconPosition(positionMs: Long) {
        if (!lyriconRegistered) return

        try {
            lyriconProvider?.player?.setPosition(positionMs)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update Lyricon position", e)
        }
    }

    /**
     * Clear Lyricon lyrics (set song to null).
     */
    fun clearLyricon() {
        if (!lyriconRegistered) return

        try {
            lyriconProvider?.player?.setSong(null)
            Log.d(TAG, "Cleared Lyricon")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear Lyricon", e)
        }
    }

    /**
     * Update Lyricon display translation setting.
     * @param enabled True to show translation, false to hide.
     */
    fun updateLyriconDisplayTranslation(enabled: Boolean) {
        if (!lyriconRegistered) return

        try {
            lyriconProvider?.player?.setDisplayTranslation(enabled)
            Log.d(TAG, "Updated Lyricon display translation: $enabled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update Lyricon display translation", e)
        }
    }

    /**
     * Update Lyricon display romanization setting.
     * @param enabled True to show romanization, false to hide.
     */
    fun updateLyriconDisplayRoma(enabled: Boolean) {
        if (!lyriconRegistered) return

        try {
            lyriconProvider?.player?.setDisplayRoma(enabled)
            Log.d(TAG, "Updated Lyricon display roma: $enabled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update Lyricon display roma", e)
        }
    }

    /**
     * Destroy Lyricon Provider and release resources.
     */
    fun destroyLyricon() {
        try {
            lyriconProvider?.unregister()
            lyriconProvider = null
            lyriconRegistered = false
            Log.d(TAG, "Destroyed Lyricon Provider")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to destroy Lyricon Provider", e)
        }
    }

    /**
     * Helper method to convert and set song for Lyricon.
     * @param music The current playing music model.
     * @param lyricData The lyric data for the current song.
     * @param settings Lyricon API settings.
     */
    fun setLyriconSongFromKanadeModels(
        music: MusicModel?,
        lyricData: LyricData?,
        settings: LyriconApiSettings = LyriconApiSettings(),
    ) {
        if (!lyriconRegistered || music == null) {
            clearLyricon()
            return
        }

        try {
            val song = LyriconAdapter.toSong(music, lyricData, settings)
            setLyriconSong(song)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert and set Lyricon song", e)
        }
    }
}
