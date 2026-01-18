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
 * 只有在播放失败时才会通过 clearCache 显式清除
 */
class UrlCacheManager(private val context: Context) {

    suspend fun getCachedUrl(mediaId: String): String? {
        val urlKey = stringPreferencesKey("url_$mediaId")
        val preferences = context.urlCacheDataStore.data.first()
        return preferences[urlKey]
    }

    suspend fun saveUrl(mediaId: String, url: String) {
        val urlKey = stringPreferencesKey("url_$mediaId")
        context.urlCacheDataStore.edit { preferences ->
            preferences[urlKey] = url
        }
    }

    suspend fun clearCache(mediaId: String) {
        val urlKey = stringPreferencesKey("url_$mediaId")
        context.urlCacheDataStore.edit { preferences ->
            preferences.remove(urlKey)
        }
    }
}
