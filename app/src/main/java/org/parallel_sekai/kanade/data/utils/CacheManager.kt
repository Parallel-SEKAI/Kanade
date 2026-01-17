package org.parallel_sekai.kanade.data.utils

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.OkHttpClient
import java.io.File

/**
 * 全局缓存管理器，用于 Media3 播放器的音频缓存
 */
object CacheManager {
    private var cache: SimpleCache? = null
    private val CACHE_SIZE = 500 * 1024 * 1024L // 500MB

    @Synchronized
    fun getCache(context: Context): SimpleCache {
        if (cache == null) {
            val cacheDir = File(context.cacheDir, "media_cache")
            val databaseProvider = StandaloneDatabaseProvider(context)
            cache = SimpleCache(cacheDir, LeastRecentlyUsedCacheEvictor(CACHE_SIZE), databaseProvider)
        }
        return cache!!
    }

    /**
     * 创建支持缓存的 DataSource.Factory
     */
    fun getCacheDataSourceFactory(context: Context): DataSource.Factory {
        val okHttpClient = OkHttpClient.Builder().build()
        val httpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent("Kanade/1.0 (Android)")

        val defaultDataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

        return CacheDataSource.Factory()
            .setCache(getCache(context))
            .setUpstreamDataSourceFactory(defaultDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    fun release() {
        cache?.release()
        cache = null
    }
}
