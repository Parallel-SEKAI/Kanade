package org.parallel_sekai.kanade.data.script

import android.util.Log
import kotlinx.serialization.json.Json
import org.parallel_sekai.kanade.data.model.*
import org.parallel_sekai.kanade.data.source.IMusicSource

class ScriptMusicSource(
    val manifest: ScriptManifest,
    private val scriptManager: ScriptManager
) : IMusicSource {
    override val sourceId: String = "script_${manifest.id}"
    override val sourceName: String = manifest.name

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getMusicList(query: String): List<MusicModel> {
        val engine = scriptManager.getEngine(manifest.id) ?: return emptyList()
        Log.d("ScriptMusicSource", "Searching [$query] on [${manifest.name}]")
        return try {
            val result = engine.callAsync(null, "search", query, 1)
            Log.d("ScriptMusicSource", "Raw result from [${manifest.name}]: $result")
            val items = json.decodeFromString<List<ScriptMusicItem>>(result)
            Log.d("ScriptMusicSource", "Parsed ${items.size} items from [${manifest.name}]")
            items.map { it.toMusicModel(sourceId) }
        } catch (e: Exception) {
            Log.e("ScriptMusicSource", "Search failed on [${manifest.name}]", e)
            emptyList()
        }
    }

    override suspend fun getHomeList(): List<MusicModel> {
        val engine = scriptManager.getEngine(manifest.id) ?: return emptyList()
        Log.d("ScriptMusicSource", "Fetching home list from [${manifest.name}]")
        return try {
            val result = engine.callAsync(null, "getHomeList")
            val items = json.decodeFromString<List<ScriptMusicItem>>(result)
            Log.d("ScriptMusicSource", "Parsed ${items.size} home items from [${manifest.name}]")
            items.map { it.toMusicModel(sourceId) }
        } catch (e: Exception) {
            // Optional function, don't log error if not found? 
            // Actually, callAsync logs "is not a function" as reject.
            emptyList()
        }
    }

    override suspend fun getPlayUrl(musicId: String): String {
        val engine = scriptManager.getEngine(manifest.id) ?: return ""
        Log.d("ScriptMusicSource", "Getting play URL for [$musicId] on [${manifest.name}]")
        return try {
            val result = engine.callAsync(null, "getMediaUrl", musicId)
            Log.d("ScriptMusicSource", "Raw media result from [${manifest.name}]: $result")
            val streamInfo = json.decodeFromString<ScriptStreamInfo>(result)
            streamInfo.url
        } catch (e: Exception) {
            Log.e("ScriptMusicSource", "GetMediaUrl failed on [${manifest.name}]", e)
            ""
        }
    }

    override suspend fun getLyrics(musicId: String): String? {
        val engine = scriptManager.getEngine(manifest.id) ?: return null
        return try {
            // getLyrics is optional in the contract
            engine.callAsync(null, "getLyrics", musicId)
        } catch (e: Exception) {
            null
        }
    }

    private fun ScriptMusicItem.toMusicModel(sourceId: String): MusicModel {
        return MusicModel(
            id = id,
            title = title,
            artists = artist.split(",").map { it.trim() },
            album = album ?: "",
            coverUrl = cover ?: "",
            mediaUri = "", // Remote sources need getPlayUrl
            duration = (duration ?: 0L) * 1000, // Convert to ms
            sourceId = sourceId
        )
    }
}
