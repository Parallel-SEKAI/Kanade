package org.parallel_sekai.kanade.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.parallel_sekai.kanade.data.model.ExternalLyricApiSettings
import org.parallel_sekai.kanade.data.model.MediaNotificationLyricsSettings
import org.parallel_sekai.kanade.data.repository.ArtistParsingSettings
import org.parallel_sekai.kanade.data.repository.LyricsSettings
import org.parallel_sekai.kanade.data.repository.SettingsRepository
import org.parallel_sekai.kanade.data.utils.CacheManager
import org.parallel_sekai.kanade.data.utils.LyricGetterManager

open class SettingsViewModel(
    private val repository: SettingsRepository,
    private val lyricGetterManager: LyricGetterManager,
) : ViewModel() {
    val isLyricsGetterActivated: Boolean get() = lyricGetterManager.isLyricGetterActivated
    val isSuperLyricActivated: Boolean get() = lyricGetterManager.isSuperLyricActivated
    val isAnyLyricApiActivated: Boolean get() = lyricGetterManager.isActivated

    val lyricsSettings: StateFlow<LyricsSettings> =
        repository.lyricsSettingsFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = LyricsSettings(),
            )

    val searchResultAsPlaylist: StateFlow<Boolean> =
        repository.searchResultAsPlaylistFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = true,
            )

    val excludedFolders: StateFlow<Set<String>> =
        repository.excludedFoldersFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptySet(),
            )

    val maxCacheSize: StateFlow<Long> =
        repository.maxCacheSizeFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 500 * 1024 * 1024L,
            )

    private val _currentCacheSize = MutableStateFlow(0L)
    val currentCacheSize: StateFlow<Long> = _currentCacheSize.asStateFlow()

    val artistParsingSettings: StateFlow<ArtistParsingSettings> =
        repository.artistParsingSettingsFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = ArtistParsingSettings(),
            )

    val mediaNotificationLyricsSettings: StateFlow<MediaNotificationLyricsSettings> =
        repository.mediaNotificationLyricsSettingsFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = MediaNotificationLyricsSettings(),
            )

    val lyricGetterApiSettings: StateFlow<ExternalLyricApiSettings> =
        repository.lyricGetterApiSettingsFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = ExternalLyricApiSettings(),
            )

    val superLyricApiSettings: StateFlow<ExternalLyricApiSettings> =
        repository.superLyricApiSettingsFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = ExternalLyricApiSettings(),
            )

    init {
        // ViewModel 不直接持有 Context，通常建议通过构造函数注入或使用 AndroidViewModel
        // 但此项目目前在 MainActivity 中手动构建 ViewModel，我们可以暂由外部调用刷新
    }

    fun refreshCacheSize(context: android.content.Context) {
        viewModelScope.launch {
            _currentCacheSize.value = CacheManager.getCurrentCacheSize(context)
        }
    }

    fun clearCache(context: android.content.Context) {
        viewModelScope.launch {
            CacheManager.clearCache(context)
            _currentCacheSize.value = 0L
        }
    }

    fun updateMaxCacheSize(size: Long) {
        viewModelScope.launch {
            repository.updateMaxCacheSize(size)
        }
    }

    fun addExcludedFolder(path: String) {
        viewModelScope.launch {
            repository.addExcludedFolder(path)
        }
    }

    fun removeExcludedFolder(path: String) {
        viewModelScope.launch {
            repository.removeExcludedFolder(path)
        }
    }

    fun updateSearchResultAsPlaylist(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSearchResultAsPlaylist(enabled)
        }
    }

    fun updateShowTranslation(show: Boolean) {
        viewModelScope.launch {
            repository.updateShowTranslation(show)
        }
    }

    fun updateFontSize(size: Float) {
        viewModelScope.launch {
            repository.updateFontSize(size)
        }
    }

    fun updateFontWeight(weight: Int) {
        viewModelScope.launch {
            repository.updateFontWeight(weight)
        }
    }

    fun updateBlurEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateBlurEnabled(enabled)
        }
    }

    fun updateAlignment(alignment: Int) {
        viewModelScope.launch {
            repository.updateAlignment(alignment)
        }
    }

    fun updateBalanceLines(balance: Boolean) {
        viewModelScope.launch {
            repository.updateBalanceLines(balance)
        }
    }

    fun updateLyricSharingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateLyricSharingEnabled(enabled)
        }
    }

    fun updateLyricShareQuality(quality: Float) {
        viewModelScope.launch {
            repository.updateLyricShareQuality(quality)
        }
    }

    fun updateArtistSeparators(separators: List<String>) {
        viewModelScope.launch {
            repository.updateArtistSeparators(separators)
        }
    }

    fun updateArtistWhitelist(whitelist: List<String>) {
        viewModelScope.launch {
            repository.updateArtistWhitelist(whitelist)
        }
    }

    fun updateArtistJoinString(joinString: String) {
        viewModelScope.launch {
            repository.updateArtistJoinString(joinString)
        }
    }

    fun updateMediaNotificationLyricsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateMediaNotificationLyricsEnabled(enabled)
        }
    }

    fun updateMediaNotificationLyricsMode(mode: Int) {
        viewModelScope.launch {
            repository.updateMediaNotificationLyricsMode(mode)
        }
    }

    fun updateMediaNotificationLyricsScrollingTruncateEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateMediaNotificationLyricsScrollingTruncateEnabled(enabled)
        }
    }

    fun updateMediaNotificationLyricsMaxDisplayUnits(units: Int) {
        viewModelScope.launch {
            repository.updateMediaNotificationLyricsMaxDisplayUnits(units)
        }
    }

    fun updateMediaNotificationLyricsSmartUnitsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateMediaNotificationLyricsSmartUnitsEnabled(enabled)
        }
    }

    fun updateMediaNotificationLyricsShowTimestamp(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateMediaNotificationLyricsShowTimestamp(enabled)
        }
    }

    fun updateMediaNotificationLyricsDisplayStates(states: Set<Int>) {
        viewModelScope.launch {
            repository.updateMediaNotificationLyricsDisplayStates(states)
        }
    }

    fun updateMediaNotificationLyricsRestoreOnPause(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateMediaNotificationLyricsRestoreOnPause(enabled)
        }
    }

    fun updateMediaNotificationLyricsTranslationMaxDisplayUnits(units: Int) {
        viewModelScope.launch {
            repository.updateMediaNotificationLyricsTranslationMaxDisplayUnits(units)
        }
    }

    // LyricGetter API update methods
    fun updateLyricGetterApiEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateLyricGetterApiEnabled(enabled)
        }
    }

    fun updateLyricGetterApiScrollingTruncateEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateLyricGetterApiScrollingTruncateEnabled(enabled)
        }
    }

    fun updateLyricGetterApiMaxDisplayUnits(units: Int) {
        viewModelScope.launch {
            repository.updateLyricGetterApiMaxDisplayUnits(units)
        }
    }

    fun updateLyricGetterApiSmartUnitsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateLyricGetterApiSmartUnitsEnabled(enabled)
        }
    }

    fun updateLyricGetterApiShowTimestamp(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateLyricGetterApiShowTimestamp(enabled)
        }
    }

    fun updateLyricGetterApiDisplayStates(states: Set<Int>) {
        viewModelScope.launch {
            repository.updateLyricGetterApiDisplayStates(states)
        }
    }

    fun updateLyricGetterApiClearOnPause(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateLyricGetterApiClearOnPause(enabled)
        }
    }

    // SuperLyric API update methods
    fun updateSuperLyricApiEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSuperLyricApiEnabled(enabled)
        }
    }

    fun updateSuperLyricApiScrollingTruncateEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSuperLyricApiScrollingTruncateEnabled(enabled)
        }
    }

    fun updateSuperLyricApiMaxDisplayUnits(units: Int) {
        viewModelScope.launch {
            repository.updateSuperLyricApiMaxDisplayUnits(units)
        }
    }

    fun updateSuperLyricApiSmartUnitsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSuperLyricApiSmartUnitsEnabled(enabled)
        }
    }

    fun updateSuperLyricApiShowTimestamp(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSuperLyricApiShowTimestamp(enabled)
        }
    }

    fun updateSuperLyricApiDisplayStates(states: Set<Int>) {
        viewModelScope.launch {
            repository.updateSuperLyricApiDisplayStates(states)
        }
    }

    fun updateSuperLyricApiClearOnPause(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSuperLyricApiClearOnPause(enabled)
        }
    }
}
