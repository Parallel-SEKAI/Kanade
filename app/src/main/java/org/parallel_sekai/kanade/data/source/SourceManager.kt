package org.parallel_sekai.kanade.data.source

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.parallel_sekai.kanade.data.model.MusicModel
import org.parallel_sekai.kanade.data.script.ScriptManager
import org.parallel_sekai.kanade.data.script.ScriptMusicSource
import org.parallel_sekai.kanade.data.source.local.LocalMusicSource
import org.parallel_sekai.kanade.data.repository.SettingsRepository

class SourceManager private constructor(
    context: Context,
    private val settingsRepository: SettingsRepository
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

    fun getAllSources(): List<IMusicSource> {
        return listOf(localMusicSource) + _scriptSources.value
    }

    fun getSource(sourceId: String): IMusicSource? {
        if (sourceId == localMusicSource.sourceId) return localMusicSource
        return _scriptSources.value.find { it.sourceId == sourceId }
    }

    suspend fun getHomeList(): List<MusicModel> {
        val activeId = _activeScriptId.value ?: return emptyList()
        val source = _scriptSources.value.find { it.manifest.id == activeId }
        return source?.getHomeList() ?: emptyList()
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

        fun getInstance(context: Context, settingsRepository: SettingsRepository): SourceManager {
            return instance ?: synchronized(this) {
                instance ?: SourceManager(context.applicationContext, settingsRepository).also { instance = it }
            }
        }
    }
}
