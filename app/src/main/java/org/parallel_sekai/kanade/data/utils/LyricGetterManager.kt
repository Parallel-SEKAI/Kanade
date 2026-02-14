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
     * Send current lyric to all supported APIs.
     * @param lyric The current lyric line text.
     * @param translation The current lyric line translation.
     * @param song The current playing music model for metadata.
     * @param delay The duration of the current lyric line in ms.
     * @param words The word-by-word lyric data for the current line.
     */
    fun sendLyric(
        lyric: String,
        translation: String? = null,
        song: MusicModel? = null,
        delay: Long = 0,
        words: List<org.parallel_sekai.kanade.data.model.WordInfo> = emptyList(),
    ) {
        if (lyric.isBlank()) {
            clearLyric()
            return
        }

        // 1. Lyric-Getter-API
        try {
            val extra =
                ExtraData().apply {
                    packageName = this@LyricGetterManager.packageName
                }
            lyricGetterApi.sendLyric(lyric, extra)
        } catch (e: Exception) {
            Log.e("LyricGetterManager", "Failed to send to Lyric-Getter-API", e)
        }

        // 2. SuperLyricApi
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

            // Note: MediaMetadata and PlaybackState can be added here if needed

            SuperLyricPush.onSuperLyric(data)
        } catch (e: Exception) {
            Log.e("LyricGetterManager", "Failed to send to SuperLyricApi", e)
        }
    }

    /**
     * Clear the current lyric from all APIs.
     */
    fun clearLyric() {
        // 1. Lyric-Getter-API
        try {
            lyricGetterApi.clearLyric()
        } catch (e: Exception) {
            Log.e("LyricGetterManager", "Failed to clear Lyric-Getter-API", e)
        }

        // 2. SuperLyricApi
        try {
            val stopData = SuperLyricData().setPackageName(packageName)
            SuperLyricPush.onStop(stopData)
        } catch (e: Exception) {
            Log.e("LyricGetterManager", "Failed to clear SuperLyricApi", e)
        }
    }
}
