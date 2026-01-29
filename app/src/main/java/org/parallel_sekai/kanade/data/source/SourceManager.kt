package org.parallel_sekai.kanade.data.source

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.parallel_sekai.kanade.data.model.*
import org.parallel_sekai.kanade.data.repository.SettingsRepository
import org.parallel_sekai.kanade.data.script.ScriptManager
import org.parallel_sekai.kanade.data.script.ScriptMusicSource
import org.parallel_sekai.kanade.data.source.local.LocalMusicSource

class SourceManager private constructor(
    context: Context,
    private val settingsRepository: SettingsRepository,
) {
    private val scriptManager = ScriptManager(context, settingsRepository)
    val localMusicSource = LocalMusicSource(context)

    private val scope = MainScope()
    private val _scriptSources = MutableStateFlow<List<ScriptMusicSource>>(emptyList())
    val scriptSources = _scriptSources.asStateFlow()

    private val _activeScriptId = MutableStateFlow<String?>(null)

    val activeScriptSource: Flow<ScriptMusicSource?> = combine(_activeScriptId, _scriptSources) { id, sources ->
        sources.find { it.manifest.id == id }
    }

    init {
        settingsRepository.activeScriptIdFlow
            .onEach { id -> _activeScriptId.value = id }
            .launchIn(scope)
    }

    fun setActiveScriptId(id: String?) {
        scope.launch {
            settingsRepository.updateActiveScriptId(id)
        }
    }

    fun getAllSources(): List<IMusicSource> = listOf(localMusicSource) + _scriptSources.value

    fun getSource(sourceId: String): IMusicSource? {
        if (sourceId == localMusicSource.sourceId) return localMusicSource
        return _scriptSources.value.find { it.sourceId == sourceId }
    }

    suspend fun getMusicListByMediaIds(mediaIds: List<String>): List<MusicModel> {
        // mediaId format: "sourceId:originalId"
        val groupedIds = mediaIds.mapNotNull { mediaId ->
            val parts = mediaId.split(":", limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else null
        }.groupBy({ it.first }, { it.second })

        val allMusic = mutableListOf<MusicModel>()
        groupedIds.forEach { (sourceId, ids) ->
            val source = getSource(sourceId)
            if (source != null) {
                try {
                    allMusic.addAll(source.getMusicListByIds(ids))
                } catch (e: Exception) {
                    // Ignore errors for specific sources
                }
            }
        }

        // Return in the original order of mediaIds
        val musicMap = allMusic.associateBy { "${it.sourceId}:${it.id}" }
        return mediaIds.mapNotNull { musicMap[it] }
    }

    /**
     * 获取外部音源首页列表（仅限已启用的脚本）
     */
    suspend fun getHomeList(page: Int = 1): MusicListResult {
        val activeId = settingsRepository.activeScriptIdFlow.first() ?: return MusicListResult(emptyList())
        val source = scriptSources.value.find { it.sourceId == "script_$activeId" } ?: return MusicListResult(emptyList())
        return source.getHomeList(page)
    }

    suspend fun getLyrics(sourceId: String, musicId: String): String? {
        val source = getSource(sourceId)
        return source?.getLyrics(musicId)
    }

    suspend fun refreshScripts() {
        val manifests = scriptManager.scanScripts()
        _scriptSources.value = manifests.map { ScriptMusicSource(it, scriptManager) }
    }

    suspend fun importScript(uri: Uri): Result<Unit> {
        val result = scriptManager.importScript(uri)
        if (result.isSuccess) {
            refreshScripts()
        }
        return result
    }

    companion object {
        @Volatile
        private var instance: SourceManager? = null

        fun getInstance(context: Context, settingsRepository: SettingsRepository): SourceManager = instance ?: synchronized(this) {
            instance ?: SourceManager(context.applicationContext, settingsRepository).also { instance = it }
        }
    }
}
