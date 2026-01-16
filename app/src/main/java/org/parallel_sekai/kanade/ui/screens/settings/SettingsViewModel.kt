package org.parallel_sekai.kanade.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.parallel_sekai.kanade.data.repository.ArtistParsingSettings
import org.parallel_sekai.kanade.data.repository.LyricsSettings
import org.parallel_sekai.kanade.data.repository.SettingsRepository
import org.parallel_sekai.kanade.data.utils.LyricGetterManager

open class SettingsViewModel(
    private val repository: SettingsRepository,
    private val lyricGetterManager: LyricGetterManager
) : ViewModel() {
    
    val isLyricsGetterActivated: Boolean get() = lyricGetterManager.isLyricGetterActivated
    val isSuperLyricActivated: Boolean get() = lyricGetterManager.isSuperLyricActivated
    val isAnyLyricApiActivated: Boolean get() = lyricGetterManager.isActivated

    val lyricsSettings: StateFlow<LyricsSettings> = repository.lyricsSettingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LyricsSettings()
        )

    val searchResultAsPlaylist: StateFlow<Boolean> = repository.searchResultAsPlaylistFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val excludedFolders: StateFlow<Set<String>> = repository.excludedFoldersFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    val artistParsingSettings: StateFlow<ArtistParsingSettings> = repository.artistParsingSettingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ArtistParsingSettings()
        )

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
}
