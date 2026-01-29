package org.parallel_sekai.kanade.data.script

import android.util.Log
import kotlinx.serialization.json.Json
import org.parallel_sekai.kanade.data.model.*
import org.parallel_sekai.kanade.data.source.IMusicSource
import org.parallel_sekai.kanade.data.source.MusicListResult

import kotlinx.serialization.json.*

class ScriptMusicSource(
    val manifest: ScriptManifest,
    private val scriptManager: ScriptManager,
) : IMusicSource {
    override val sourceId: String = "script_${manifest.id}"
    override val sourceName: String = manifest.name

    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun ensureEngine(): ScriptEngine? = scriptManager.getEngine(manifest.id)

    override suspend fun getMusicList(query: String): MusicListResult {
        val engine = ensureEngine() ?: return MusicListResult(emptyList())
        Log.d("ScriptMusicSource", "Searching [$query] on [${manifest.name}]")
        return try {
            val result = engine.callAsync(null, "search", query, 1)
            Log.d("ScriptMusicSource", "Raw result from [${manifest.name}]: $result")
            if (result == "null") return MusicListResult(emptyList())
            
            val jsonElement = json.parseToJsonElement(result)
            if (jsonElement is JsonObject) {
                val response = json.decodeFromJsonElement<ScriptMusicListResponse>(jsonElement)
                MusicListResult(
                    items = response.items.map { it.toMusicModel(sourceId) },
                    totalCount = response.total
                )
            } else {
                val items = json.decodeFromJsonElement<List<ScriptMusicItem>>(jsonElement)
                MusicListResult(items.map { it.toMusicModel(sourceId) })
            }
        } catch (e: Exception) {
            Log.e("ScriptMusicSource", "Search failed on [${manifest.name}]", e)
            MusicListResult(emptyList())
        }
    }

    override suspend fun getHomeList(page: Int): MusicListResult {
        val engine = ensureEngine() ?: return MusicListResult(emptyList())
        Log.d("ScriptMusicSource", "Fetching home list (page $page) from [${manifest.name}]")
        return try {
            val result = engine.callAsync(null, "getHomeList", page)
            if (result == "null") return MusicListResult(emptyList())
            
            val jsonElement = json.parseToJsonElement(result)
            if (jsonElement is JsonObject) {
                val response = json.decodeFromJsonElement<ScriptMusicListResponse>(jsonElement)
                MusicListResult(
                    items = response.items.map { it.toMusicModel(sourceId) },
                    totalCount = response.total
                )
            } else {
                val items = json.decodeFromJsonElement<List<ScriptMusicItem>>(jsonElement)
                MusicListResult(items.map { it.toMusicModel(sourceId) })
            }
        } catch (e: Exception) {
            MusicListResult(emptyList())
        }
    }

    override suspend fun getPlayUrl(musicId: String): String {
        val engine = ensureEngine() ?: return ""
        Log.d("ScriptMusicSource", "Getting play URL for [$musicId] on [${manifest.name}]")
        return try {
            val result = engine.callAsync(null, "getMediaUrl", musicId)
            Log.d("ScriptMusicSource", "Raw media result from [${manifest.name}]: $result")
            if (result == "null") return ""
            val streamInfo = json.decodeFromString<ScriptStreamInfo>(result)
            streamInfo.url
        } catch (e: Exception) {
            Log.e("ScriptMusicSource", "GetMediaUrl failed on [${manifest.name}]", e)
            ""
        }
    }

    override suspend fun getLyrics(musicId: String): String? {
        val engine = ensureEngine() ?: return null
        return try {
            // getLyrics is optional in the contract
            val rawLyrics = engine.callAsync(null, "getLyrics", musicId)
            // The bridge returns a JSON-stringified result, so we need to decode it
            val decoded = try {
                json.decodeFromString<String?>(rawLyrics)
            } catch (e: Exception) {
                rawLyrics // Fallback if it's already a plain string for some reason
            }
            // Normalize line endings: replace \r\n and \r with \n
            decoded?.replace("\r\n", "\n")?.replace("\r", "\n")
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getMusicListByIds(ids: List<String>): List<MusicModel> {
        val engine = ensureEngine() ?: return emptyList()
        return try {
            // First try getMusicListByIds, then fallback to single getMusicDetail if it's just one ID
            val result = engine.callAsync(null, "getMusicListByIds", ids)
            if (result == "null") return emptyList()
            val items = json.decodeFromString<List<ScriptMusicItem>>(result)
            items.map { it.toMusicModel(sourceId) }
        } catch (e: Exception) {
            // Fallback for scripts that only support single item detail
            if (ids.size == 1) {
                try {
                    val result = engine.callAsync(null, "getMusicDetail", ids[0])
                    if (result != "null") {
                        val item = json.decodeFromString<ScriptMusicItem>(result)
                        return listOf(item.toMusicModel(sourceId))
                    }
                } catch (e2: Exception) {
                    // Ignore
                }
            }
            emptyList()
        }
    }

    private fun ScriptMusicItem.toMusicModel(sourceId: String): MusicModel = MusicModel(
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
