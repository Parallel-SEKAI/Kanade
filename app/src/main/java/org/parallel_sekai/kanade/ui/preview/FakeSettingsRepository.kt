package org.parallel_sekai.kanade.ui.preview

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.parallel_sekai.kanade.data.repository.*

// This is a fake implementation for preview purposes only
class FakeSettingsRepository(context: Context) : SettingsRepository(context) {

    private val _fakeLyricsSettingsFlow = MutableStateFlow(LyricsSettings())
    override val lyricsSettingsFlow: StateFlow<LyricsSettings> = _fakeLyricsSettingsFlow.asStateFlow()

    private val _fakeSearchResultAsPlaylistFlow = MutableStateFlow(true)
    override val searchResultAsPlaylistFlow: StateFlow<Boolean> = _fakeSearchResultAsPlaylistFlow.asStateFlow()

    private val _fakeExcludedFoldersFlow = MutableStateFlow(emptySet<String>())
    override val excludedFoldersFlow: StateFlow<Set<String>> = _fakeExcludedFoldersFlow.asStateFlow()

    private val _fakeArtistParsingSettingsFlow = MutableStateFlow(
        ArtistParsingSettings(
            separators = listOf("/", ";", " & "),
            whitelist = listOf("Leo/need", "Artist with / in name"),
            joinString = " | ",
        ),
    )
    override val artistParsingSettingsFlow: StateFlow<ArtistParsingSettings> = _fakeArtistParsingSettingsFlow.asStateFlow()

    private val _fakeSearchHistoryFlow = MutableStateFlow(emptyList<String>())
    override val searchHistoryFlow: StateFlow<List<String>> = _fakeSearchHistoryFlow.asStateFlow()

    // Override update functions to do nothing or update the fake flows
    override suspend fun updateArtistSeparators(separators: List<String>) {
        _fakeArtistParsingSettingsFlow.value = _fakeArtistParsingSettingsFlow.value.copy(separators = separators)
    }

    override suspend fun updateArtistWhitelist(whitelist: List<String>) {
        _fakeArtistParsingSettingsFlow.value = _fakeArtistParsingSettingsFlow.value.copy(whitelist = whitelist)
    }

    override suspend fun updateArtistJoinString(joinString: String) {
        _fakeArtistParsingSettingsFlow.value = _fakeArtistParsingSettingsFlow.value.copy(joinString = joinString)
    }

    override suspend fun updateShowTranslation(show: Boolean) { /* do nothing */ }
    override suspend fun updateFontSize(size: Float) { /* do nothing */ }
    override suspend fun updateFontWeight(weight: Int) { /* do nothing */ }
    override suspend fun updateBlurEnabled(enabled: Boolean) { /* do nothing */ }
    override suspend fun updateAlignment(alignment: Int) { /* do nothing */ }
    override suspend fun updateBalanceLines(balance: Boolean) { /* do nothing */ }
    override suspend fun addExcludedFolder(path: String) { /* do nothing */ }
    override suspend fun removeExcludedFolder(path: String) { /* do nothing */ }
    override suspend fun updateSearchResultAsPlaylist(enabled: Boolean) { /* do nothing */ }
    override suspend fun addSearchHistory(query: String) { /* do nothing */ }
    override suspend fun removeSearchHistory(query: String) { /* do nothing */ }
    override suspend fun clearSearchHistory() { /* do nothing */ }
}
