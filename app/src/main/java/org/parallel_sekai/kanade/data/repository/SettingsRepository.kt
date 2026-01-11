package org.parallel_sekai.kanade.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class LyricsSettings(
    val showTranslation: Boolean = true,
    val fontSize: Float = 18f,
    val fontWeight: Int = 400, // 400 = Normal, 700 = Bold
    val blurEnabled: Boolean = true,
    val alignment: Int = 0, // 0 = Left, 1 = Center, 2 = Right
    val balanceLines: Boolean = false
)

class SettingsRepository(private val context: Context) {
    private val SHOW_TRANSLATION = booleanPreferencesKey("show_translation")
    private val FONT_SIZE = floatPreferencesKey("font_size")
    private val FONT_WEIGHT = intPreferencesKey("font_weight")
    private val BLUR_ENABLED = booleanPreferencesKey("blur_enabled")
    private val ALIGNMENT = intPreferencesKey("alignment")
    private val BALANCE_LINES = booleanPreferencesKey("balance_lines")
    private val SEARCH_HISTORY = stringSetPreferencesKey("search_history")
    private val SEARCH_RESULT_AS_PLAYLIST = booleanPreferencesKey("search_result_as_playlist")

    val lyricsSettingsFlow: Flow<LyricsSettings> = context.dataStore.data
        .map { preferences ->
            LyricsSettings(
                showTranslation = preferences[SHOW_TRANSLATION] ?: true,
                fontSize = preferences[FONT_SIZE] ?: 18f,
                fontWeight = preferences[FONT_WEIGHT] ?: 400,
                blurEnabled = preferences[BLUR_ENABLED] ?: true,
                alignment = preferences[ALIGNMENT] ?: 0,
                balanceLines = preferences[BALANCE_LINES] ?: false
            )
        }

    val searchResultAsPlaylistFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SEARCH_RESULT_AS_PLAYLIST] ?: true
        }

    suspend fun updateSearchResultAsPlaylist(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SEARCH_RESULT_AS_PLAYLIST] = enabled
        }
    }

    val searchHistoryFlow: Flow<List<String>> = context.dataStore.data
        .map { preferences ->
            preferences[SEARCH_HISTORY]?.toList() ?: emptyList()
        }

    suspend fun addSearchHistory(query: String) {
        if (query.isBlank()) return
        context.dataStore.edit { preferences ->
            val current = preferences[SEARCH_HISTORY] ?: emptySet()
            val updated = current.toMutableSet().apply {
                remove(query) // Move to top by removing and adding
                add(query)
            }
            // Limit to 20 items
            preferences[SEARCH_HISTORY] = updated.toList().takeLast(20).toSet()
        }
    }

    suspend fun removeSearchHistory(query: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[SEARCH_HISTORY] ?: emptySet()
            preferences[SEARCH_HISTORY] = current.filter { it != query }.toSet()
        }
    }

    suspend fun clearSearchHistory() {
        context.dataStore.edit { preferences ->
            preferences.remove(SEARCH_HISTORY)
        }
    }

    suspend fun updateShowTranslation(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_TRANSLATION] = show
        }
    }

    suspend fun updateFontSize(size: Float) {
        context.dataStore.edit { preferences ->
            preferences[FONT_SIZE] = size
        }
    }

    suspend fun updateFontWeight(weight: Int) {
        context.dataStore.edit { preferences ->
            preferences[FONT_WEIGHT] = weight
        }
    }

    suspend fun updateBlurEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BLUR_ENABLED] = enabled
        }
    }

    suspend fun updateAlignment(alignment: Int) {
        context.dataStore.edit { preferences ->
            preferences[ALIGNMENT] = alignment
        }
    }

    suspend fun updateBalanceLines(balance: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BALANCE_LINES] = balance
        }
    }
}
