package org.parallel_sekai.kanade.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.parallel_sekai.kanade.data.repository.LyricsSettings
import org.parallel_sekai.kanade.data.repository.SettingsRepository

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
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
}
