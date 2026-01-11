package org.parallel_sekai.kanade.ui.screens.search

import org.parallel_sekai.kanade.data.source.MusicModel

sealed interface SearchIntent {
    data class UpdateQuery(val query: String) : SearchIntent
    data class PerformSearch(val query: String) : SearchIntent
    object ClearHistory : SearchIntent
    data class PlayMusic(val music: MusicModel, val results: List<MusicModel>) : SearchIntent
    data class RemoveHistoryItem(val query: String) : SearchIntent
}

data class SearchState(
    val searchQuery: String = "",
    val searchResults: List<MusicModel> = emptyList(),
    val searchHistory: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val searchResultAsPlaylist: Boolean = true,
    val artistJoinString: String = ", "
)

sealed interface SearchEffect {
    data class ShowError(val message: String) : SearchEffect
}
