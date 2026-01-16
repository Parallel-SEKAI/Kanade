package org.parallel_sekai.kanade.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.parallel_sekai.kanade.data.repository.PlaybackRepository
import org.parallel_sekai.kanade.data.repository.SettingsRepository
import org.parallel_sekai.kanade.data.model.MusicModel
import org.parallel_sekai.kanade.R

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val playbackRepository: PlaybackRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SearchState())
    val state = _state.asStateFlow()

    private val _effect = MutableSharedFlow<SearchEffect>()
    val effect = _effect.asSharedFlow()

    init {
        // Observe search history
        viewModelScope.launch {
            settingsRepository.searchHistoryFlow.collect { history ->
                _state.update { it.copy(searchHistory = history.reversed()) }
            }
        }

        // Observe search results as playlist setting
        viewModelScope.launch {
            settingsRepository.searchResultAsPlaylistFlow.collect { enabled ->
                _state.update { it.copy(searchResultAsPlaylist = enabled) }
            }
        }

        // Observe artist parsing settings for join string
        viewModelScope.launch {
            settingsRepository.artistParsingSettingsFlow.collect { settings ->
                _state.update { it.copy(artistJoinString = settings.joinString) }
            }
        }

        // Debounced search logic
        viewModelScope.launch {
            _state.map { it.searchQuery }
                .distinctUntilChanged()
                .debounce(500)
                .collect { query ->
                    if (query.isNotBlank()) {
                        performSearch(query)
                    } else {
                        _state.update { it.copy(searchResults = emptyList(), isSearching = false) }
                    }
                }
        }
    }

    fun handleIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.UpdateQuery -> {
                _state.update { it.copy(searchQuery = intent.query) }
            }
            is SearchIntent.PerformSearch -> {
                _state.update { it.copy(searchQuery = intent.query) }
                viewModelScope.launch {
                    performSearch(intent.query)
                }
            }
            is SearchIntent.ClearHistory -> {
                viewModelScope.launch {
                    settingsRepository.clearSearchHistory()
                }
            }
            is SearchIntent.PlayMusic -> {
                viewModelScope.launch {
                    settingsRepository.addSearchHistory(_state.value.searchQuery)
                    if (_state.value.searchResultAsPlaylist) {
                        playbackRepository.setPlaylist(intent.results, intent.results.indexOf(intent.music))
                    } else {
                        playbackRepository.setPlaylist(listOf(intent.music), 0)
                    }
                }
            }
            is SearchIntent.RemoveHistoryItem -> {
                viewModelScope.launch {
                    settingsRepository.removeSearchHistory(intent.query)
                }
            }
        }
    }

    private suspend fun performSearch(query: String) {
        _state.update { it.copy(isLoading = true, isSearching = true) }
        try {
            val results = playbackRepository.fetchMusicList(query)
            _state.update { it.copy(searchResults = results, isLoading = false) }
        } catch (e: Exception) {
            _state.update { it.copy(isLoading = false) }
            _effect.emit(SearchEffect.ShowError(R.string.error_unknown))
        }
    }
}