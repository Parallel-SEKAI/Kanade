package org.parallel_sekai.kanade.data.script

import android.util.Log
import kotlinx.serialization.json.*
import kotlinx.serialization.json.Json
import org.parallel_sekai.kanade.data.model.*
import org.parallel_sekai.kanade.data.source.IMusicSource
import org.parallel_sekai.kanade.data.source.MusicListResult
import java.util.concurrent.ConcurrentHashMap

/**
 * 带 TTL 的缓存条目
 */
private data class CacheEntry<T>(
    val value: T,
    val expireTime: Long,
) {
    fun isExpired(): Boolean = System.currentTimeMillis() > expireTime
}

/**
 * ScriptMusicSource 的响应缓存管理器
 * TTL = 600 秒（10分钟）
 */
private object ScriptSourceCache {
    private const val TTL_MS = 600_000L // 600 seconds = 10 minutes

    private val cache = ConcurrentHashMap<String, CacheEntry<Any?>>()

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T? {
        val entry = cache[key] ?: return null
        if (entry.isExpired()) {
            cache.remove(key)
            return null
        }
        return entry.value as? T
    }

    fun <T> put(key: String, value: T) {
        cache[key] = CacheEntry(value, System.currentTimeMillis() + TTL_MS)
    }

    fun generateKey(sourceId: String, method: String, vararg params: Any?): String {
        val paramsStr = params.joinToString(":") { it?.toString() ?: "null" }
        return "$sourceId:$method:$paramsStr"
    }

    /**
     * 清理过期的缓存条目
     */
    fun cleanExpired() {
        val now = System.currentTimeMillis()
        cache.entries.removeIf { it.value.expireTime < now }
    }

    /**
     * 清除指定 sourceId 的所有缓存
     */
    fun clearForSource(sourceId: String) {
        cache.keys.removeIf { it.startsWith("$sourceId:") }
    }

    /**
     * 清除所有缓存
     */
    fun clearAll() {
        cache.clear()
    }
}

class ScriptMusicSource(
    val manifest: ScriptManifest,
    private val scriptManager: ScriptManager,
) : IMusicSource {
    override val sourceId: String = "script_${manifest.id}"
    override val sourceName: String = manifest.name

    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun ensureEngine(): ScriptEngine? = scriptManager.getEngine(manifest.id)

    override suspend fun getMusicList(query: String): MusicListResult {
        val cacheKey = ScriptSourceCache.generateKey(sourceId, "getMusicList", query)
        ScriptSourceCache.get<MusicListResult>(cacheKey)?.let {
            Log.d("ScriptMusicSource", "Cache hit for search [$query] on [${manifest.name}]")
            return it
        }

        val engine = ensureEngine() ?: return MusicListResult(emptyList())
        Log.d("ScriptMusicSource", "Searching [$query] on [${manifest.name}]")
        return try {
            val result = engine.callAsync(null, "search", query, 1)
            Log.d("ScriptMusicSource", "Raw result from [${manifest.name}]: $result")
            if (result == "null") return MusicListResult(emptyList())

            val jsonElement = json.parseToJsonElement(result)
            val musicListResult = if (jsonElement is JsonObject) {
                val response = json.decodeFromJsonElement<ScriptMusicListResponse>(jsonElement)
                MusicListResult(
                    items = response.items.map { it.toMusicModel(sourceId) },
                    totalCount = response.total,
                )
            } else {
                val items = json.decodeFromJsonElement<List<ScriptMusicItem>>(jsonElement)
                MusicListResult(items.map { it.toMusicModel(sourceId) })
            }
            ScriptSourceCache.put(cacheKey, musicListResult)
            musicListResult
        } catch (e: Exception) {
            Log.e("ScriptMusicSource", "Search failed on [${manifest.name}]", e)
            MusicListResult(emptyList())
        }
    }

    override suspend fun getHomeList(page: Int): MusicListResult {
        val cacheKey = ScriptSourceCache.generateKey(sourceId, "getHomeList", page)
        ScriptSourceCache.get<MusicListResult>(cacheKey)?.let {
            Log.d("ScriptMusicSource", "Cache hit for home list (page $page) on [${manifest.name}]")
            return it
        }

        val engine = ensureEngine() ?: return MusicListResult(emptyList())
        Log.d("ScriptMusicSource", "Fetching home list (page $page) from [${manifest.name}]")
        return try {
            val result = engine.callAsync(null, "getHomeList", page)
            if (result == "null") return MusicListResult(emptyList())

            val jsonElement = json.parseToJsonElement(result)
            val musicListResult = if (jsonElement is JsonObject) {
                val response = json.decodeFromJsonElement<ScriptMusicListResponse>(jsonElement)
                MusicListResult(
                    items = response.items.map { it.toMusicModel(sourceId) },
                    totalCount = response.total,
                )
            } else {
                val items = json.decodeFromJsonElement<List<ScriptMusicItem>>(jsonElement)
                MusicListResult(items.map { it.toMusicModel(sourceId) })
            }
            ScriptSourceCache.put(cacheKey, musicListResult)
            musicListResult
        } catch (e: Exception) {
            MusicListResult(emptyList())
        }
    }

    override suspend fun getAllHomeList(): MusicListResult {
        val cacheKey = ScriptSourceCache.generateKey(sourceId, "getAllHomeList")
        ScriptSourceCache.get<MusicListResult>(cacheKey)?.let {
            Log.d("ScriptMusicSource", "Cache hit for all home list on [${manifest.name}]")
            return it
        }

        val engine = ensureEngine() ?: return MusicListResult(emptyList())
        Log.d("ScriptMusicSource", "Fetching all home list from [${manifest.name}]")
        return try {
            val result = engine.callAsync(null, "getAllHomeList")
            if (result == "null") return MusicListResult(emptyList())

            val jsonElement = json.parseToJsonElement(result)
            val musicListResult = if (jsonElement is JsonObject) {
                val response = json.decodeFromJsonElement<ScriptMusicListResponse>(jsonElement)
                MusicListResult(
                    items = response.items.map { it.toMusicModel(sourceId) },
                    totalCount = response.total,
                )
            } else {
                val items = json.decodeFromJsonElement<List<ScriptMusicItem>>(jsonElement)
                MusicListResult(items.map { it.toMusicModel(sourceId) })
            }
            ScriptSourceCache.put(cacheKey, musicListResult)
            musicListResult
        } catch (e: Exception) {
            Log.e("ScriptMusicSource", "GetAllHomeList failed on [${manifest.name}]", e)
            MusicListResult(emptyList())
        }
    }

    override suspend fun getPlayUrl(musicId: String): String {
        val cacheKey = ScriptSourceCache.generateKey(sourceId, "getPlayUrl", musicId)
        ScriptSourceCache.get<String>(cacheKey)?.let {
            Log.d("ScriptMusicSource", "Cache hit for play URL [$musicId] on [${manifest.name}]")
            return it
        }

        val engine = ensureEngine() ?: return ""
        Log.d("ScriptMusicSource", "Getting play URL for [$musicId] on [${manifest.name}]")
        return try {
            val result = engine.callAsync(null, "getMediaUrl", musicId)
            Log.d("ScriptMusicSource", "Raw media result from [${manifest.name}]: $result")
            if (result == "null") return ""
            val streamInfo = json.decodeFromString<ScriptStreamInfo>(result)
            val url = streamInfo.url
            if (url.isNotEmpty()) {
                ScriptSourceCache.put(cacheKey, url)
            }
            url
        } catch (e: Exception) {
            Log.e("ScriptMusicSource", "GetMediaUrl failed on [${manifest.name}]", e)
            ""
        }
    }

    override suspend fun getLyrics(musicId: String): String? {
        val cacheKey = ScriptSourceCache.generateKey(sourceId, "getLyrics", musicId)
        ScriptSourceCache.get<String>(cacheKey)?.let {
            Log.d("ScriptMusicSource", "Cache hit for lyrics [$musicId] on [${manifest.name}]")
            return it
        }

        val engine = ensureEngine() ?: return null
        return try {
            // getLyrics is optional in the contract
            val rawLyrics = engine.callAsync(null, "getLyrics", musicId)
            // The bridge returns a JSON-stringified result, so we need to decode it
            val decoded =
                try {
                    json.decodeFromString<String?>(rawLyrics)
                } catch (e: Exception) {
                    rawLyrics // Fallback if it's already a plain string for some reason
                }
            // Normalize line endings: replace \r\n and \r with \n
            val normalizedLyrics = decoded?.replace("\r\n", "\n")?.replace("\r", "\n")
            if (normalizedLyrics != null) {
                ScriptSourceCache.put(cacheKey, normalizedLyrics)
            }
            normalizedLyrics
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getMusicListByIds(ids: List<String>): List<MusicModel> {
        val sortedIds = ids.sorted()
        val cacheKey = ScriptSourceCache.generateKey(sourceId, "getMusicListByIds", sortedIds.joinToString(","))
        ScriptSourceCache.get<List<MusicModel>>(cacheKey)?.let {
            Log.d("ScriptMusicSource", "Cache hit for getMusicListByIds [${ids.size} items] on [${manifest.name}]")
            return it
        }

        val engine = ensureEngine() ?: return emptyList()
        return try {
            // First try getMusicListByIds, then fallback to single getMusicDetail if it's just one ID
            val result = engine.callAsync(null, "getMusicListByIds", ids)
            if (result == "null") return emptyList()
            val items = json.decodeFromString<List<ScriptMusicItem>>(result)
            val musicList = items.map { it.toMusicModel(sourceId) }
            ScriptSourceCache.put(cacheKey, musicList)
            musicList
        } catch (e: Exception) {
            // Fallback for scripts that only support single item detail
            if (ids.size == 1) {
                try {
                    val result = engine.callAsync(null, "getMusicDetail", ids[0])
                    if (result != "null") {
                        val item = json.decodeFromString<ScriptMusicItem>(result)
                        val musicList = listOf(item.toMusicModel(sourceId))
                        ScriptSourceCache.put(cacheKey, musicList)
                        return musicList
                    }
                } catch (e2: Exception) {
                    // Ignore
                }
            }
            emptyList()
        }
    }

    /**
     * 清除此音源的所有缓存
     */
    fun clearCache() {
        ScriptSourceCache.clearForSource(sourceId)
    }

    companion object {
        /**
         * 清除所有 ScriptMusicSource 的缓存
         */
        fun clearAllCache() {
            ScriptSourceCache.clearAll()
        }

        /**
         * 清理过期的缓存条目
         */
        fun cleanExpiredCache() {
            ScriptSourceCache.cleanExpired()
        }
    }

    private fun ScriptMusicItem.toMusicModel(sourceId: String): MusicModel =
        MusicModel(
            id = id,
            title = title,
            artists = artist.split(",").map { it.trim() },
            album = album ?: "",
            coverUrl = cover ?: "",
            mediaUri = "", // Remote sources need getPlayUrl
            duration = (duration ?: 0L) * 1000, // Convert to ms
            sourceId = sourceId,
        )
}
