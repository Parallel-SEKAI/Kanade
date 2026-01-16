package org.parallel_sekai.kanade.data.utils

import android.content.Context
import android.util.Log
import cn.lyric.getter.api.API
import cn.lyric.getter.api.data.ExtraData
import org.parallel_sekai.kanade.data.model.MusicModel

/**
 * Manager for interacting with the Lyric-Getter-API.
 * This allows other apps to receive lyric updates from Kanade.
 */
class LyricGetterManager(context: Context) {
    private val api = API()
    private val packageName = context.packageName

    /**
     * Check if the API is activated (hooked).
     */
    val isActivated: Boolean get() = try {
        api.hasEnable
    } catch (e: Exception) {
        Log.e("LyricGetterManager", "Failed to check if API is activated", e)
        false
    }

    /**
     * Send current lyric to the API.
     * @param lyric The current lyric line text.
     * @param song The current playing music model for metadata.
     */
    fun sendLyric(lyric: String, song: MusicModel?) {
        try {
            val extra = ExtraData().apply {
                packageName = this@LyricGetterManager.packageName
            }
            api.sendLyric(lyric, extra)
        } catch (e: Exception) {
            Log.e("LyricGetterManager", "Failed to send lyric: $lyric", e)
        }
    }

    /**
     * Clear the current lyric from the API (e.g., when playback stops or track changes).
     */
    fun clearLyric() {
        try {
            api.clearLyric()
        } catch (e: Exception) {
            Log.e("LyricGetterManager", "Failed to clear lyric", e)
        }
    }
}
