package org.parallel_sekai.kanade.data.utils

import android.content.Context
import android.util.Log
import cn.lyric.getter.api.API
import cn.lyric.getter.api.data.ExtraData
import com.hchen.superlyricapi.SuperLyricData
import com.hchen.superlyricapi.SuperLyricPush
import com.hchen.superlyricapi.SuperLyricTool
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

    companion object {
        private const val TAG = "LyricGetterManager"
    }

    /**
     * Check if any lyric API is activated.
     */
    val isActivated: Boolean get() = isLyricGetterActivated || isSuperLyricActivated

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
}
