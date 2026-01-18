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
    private var currentMaxCacheSize = 500 * 1024 * 1024L // Default 500MB

    @Synchronized
    fun getCache(context: Context, maxSize: Long = currentMaxCacheSize): SimpleCache {
        if (cache == null) {
            currentMaxCacheSize = maxSize
            val cacheDir = File(context.cacheDir, "media_cache")
            val databaseProvider = StandaloneDatabaseProvider(context)
            cache = SimpleCache(cacheDir, LeastRecentlyUsedCacheEvictor(maxSize), databaseProvider)
        }
        return cache!!
    }

    fun getImageCacheDir(context: Context): File = File(context.cacheDir, "image_cache")

    /**
     * 获取当前缓存占用的空间（字节）
     * 如果 cache 已初始化，直接从 cache 获取；否则手动计算文件夹大小。
     */
    fun getCurrentCacheSize(context: Context): Long {
        synchronized(this) {
            if (cache != null) {
                return cache!!.cacheSpace + calculateFolderSize(getImageCacheDir(context))
            }
        }

        val mediaCacheDir = File(context.cacheDir, "media_cache")
        val imageCacheDir = getImageCacheDir(context)
        return calculateFolderSize(mediaCacheDir) + calculateFolderSize(imageCacheDir)
    }

    private fun calculateFolderSize(file: File): Long {
        if (!file.exists()) return 0L
        var size: Long = 0
        if (file.isDirectory) {
            val files = file.listFiles()
            if (files != null) {
                for (f in files) {
                    size += calculateFolderSize(f)
                }
            }
        } else {
            size = file.length()
        }
        return size
    }

    /**
     * 清理所有缓存
     */
    fun clearCache(context: Context) {
        synchronized(this) {
            if (cache != null) {
                cache?.keys?.forEach { key ->
                    cache?.removeResource(key)
                }
            }
        }
        // 直接删除文件夹
        File(context.cacheDir, "media_cache").deleteRecursively()
        getImageCacheDir(context).deleteRecursively()
    }

    /**
     * 创建支持缓存的 DataSource.Factory
     */
    fun getCacheDataSourceFactory(context: Context, maxSize: Long = 500 * 1024 * 1024L): DataSource.Factory {
        val okHttpClient = OkHttpClient.Builder().build()
        val httpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent("Kanade/1.0 (Android)")

        val defaultDataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

        return CacheDataSource.Factory()
            .setCache(getCache(context, maxSize))
            .setUpstreamDataSourceFactory(defaultDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    fun release() {
        cache?.release()
        cache = null
    }
}
