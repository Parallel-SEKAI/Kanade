package org.parallel_sekai.kanade.data.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

val Context.urlCacheDataStore: DataStore<Preferences> by preferencesDataStore(name = "url_cache")

/**
 * 播放链接缓存管理器
 * 用于永久持久化缓存脚本音源解析出的真实 URL
 * 只有在过期或播放失败时才会通过 clearCache 显式清除
 */
class UrlCacheManager(
    private val context: Context,
) {
    companion object {
        private const val EXPIRATION_TIME = 20 * 60 * 1000 // 20 分钟
    }

    suspend fun getCachedUrl(mediaId: String): String? {
        val urlKey = stringPreferencesKey("url_$mediaId")
        val timeKey = longPreferencesKey("time_$mediaId")
        val preferences = context.urlCacheDataStore.data.first()

        val url = preferences[urlKey] ?: return null
        val time = preferences[timeKey] ?: 0L

        // 检查是否过期
        if (System.currentTimeMillis() - time > EXPIRATION_TIME) {
            return null
        }

        return url
    }

    suspend fun saveUrl(
        mediaId: String,
        url: String,
    ) {
        val urlKey = stringPreferencesKey("url_$mediaId")
        val timeKey = longPreferencesKey("time_$mediaId")
        context.urlCacheDataStore.edit { preferences ->
            preferences[urlKey] = url
            preferences[timeKey] = System.currentTimeMillis()
        }
    }

    suspend fun clearCache(mediaId: String) {
        val urlKey = stringPreferencesKey("url_$mediaId")
        val timeKey = longPreferencesKey("time_$mediaId")
        context.urlCacheDataStore.edit { preferences ->
            preferences.remove(urlKey)
            preferences.remove(timeKey)
        }
    }
}
