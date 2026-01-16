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
    val balanceLines: Boolean = false,
    val isSharingEnabled: Boolean = true
)

data class ArtistParsingSettings(
    val separators: List<String> = listOf("/", ";", "|", " & ", " feat. ", " ft. ", ","),
    val whitelist: List<String> = listOf("Leo/need"),
    val joinString: String = ", "
)

data class PlaybackState(
    val repeatMode: Int = 0,
    val shuffleMode: Boolean = false,
    val lastMediaId: String? = null,
    val lastPosition: Long = 0L,
    val lastPlaylistIds: List<String> = emptyList()
)

open class SettingsRepository(private val context: Context) {
    private val SHOW_TRANSLATION = booleanPreferencesKey("show_translation")
    private val FONT_SIZE = floatPreferencesKey("font_size")
    private val FONT_WEIGHT = intPreferencesKey("font_weight")
    private val BLUR_ENABLED = booleanPreferencesKey("blur_enabled")
    private val ALIGNMENT = intPreferencesKey("alignment")
    private val BALANCE_LINES = booleanPreferencesKey("balance_lines")
    private val LYRIC_SHARING_ENABLED = booleanPreferencesKey("lyric_sharing_enabled")
    private val SEARCH_HISTORY = stringSetPreferencesKey("search_history")
    private val SEARCH_RESULT_AS_PLAYLIST = booleanPreferencesKey("search_result_as_playlist")
    private val EXCLUDED_FOLDERS = stringSetPreferencesKey("excluded_folders")

    private val ARTIST_SEPARATORS = stringSetPreferencesKey("artist_separators")
    private val ARTIST_WHITELIST = stringSetPreferencesKey("artist_whitelist")
    private val ARTIST_JOIN_STRING = stringPreferencesKey("artist_join_string")

    // Playback state keys
    private val REPEAT_MODE = intPreferencesKey("playback_repeat_mode")
    private val SHUFFLE_MODE = booleanPreferencesKey("playback_shuffle_mode")
    private val LAST_PLAYED_MEDIA_ID = stringPreferencesKey("last_played_media_id")
    private val LAST_PLAYED_POSITION = longPreferencesKey("last_played_position")
    private val LAST_PLAYLIST_IDS = stringPreferencesKey("last_playlist_ids")

    open val lyricsSettingsFlow: Flow<LyricsSettings> = context.dataStore.data
        .map { preferences ->
            LyricsSettings(
                showTranslation = preferences[SHOW_TRANSLATION] ?: true,
                fontSize = preferences[FONT_SIZE] ?: 18f,
                fontWeight = preferences[FONT_WEIGHT] ?: 400,
                blurEnabled = preferences[BLUR_ENABLED] ?: true,
                alignment = preferences[ALIGNMENT] ?: 0,
                balanceLines = preferences[BALANCE_LINES] ?: false,
                isSharingEnabled = preferences[LYRIC_SHARING_ENABLED] ?: true
            )
        }

    open val searchResultAsPlaylistFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SEARCH_RESULT_AS_PLAYLIST] ?: true
        }

    open val excludedFoldersFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[EXCLUDED_FOLDERS] ?: emptySet()
        }

    open val artistParsingSettingsFlow: Flow<ArtistParsingSettings> = context.dataStore.data
        .map { preferences ->
            ArtistParsingSettings(
                separators = preferences[ARTIST_SEPARATORS]?.toList() ?: listOf("/", ";", "|", " & ", " feat. ", " ft. ", ","),
                whitelist = preferences[ARTIST_WHITELIST]?.toList() ?: listOf("Leo/need"),
                joinString = preferences[ARTIST_JOIN_STRING] ?: ", "
            )
        }

    open suspend fun updateArtistSeparators(separators: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[ARTIST_SEPARATORS] = separators.toSet()
        }
    }

    open suspend fun updateArtistWhitelist(whitelist: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[ARTIST_WHITELIST] = whitelist.toSet()
        }
    }

    open suspend fun updateArtistJoinString(joinString: String) {
        context.dataStore.edit { preferences ->
            preferences[ARTIST_JOIN_STRING] = joinString
        }
    }

    open suspend fun addExcludedFolder(path: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[EXCLUDED_FOLDERS] ?: emptySet()
            preferences[EXCLUDED_FOLDERS] = current + path
        }
    }

    open suspend fun removeExcludedFolder(path: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[EXCLUDED_FOLDERS] ?: emptySet()
            preferences[EXCLUDED_FOLDERS] = current - path
        }
    }

    open suspend fun updateSearchResultAsPlaylist(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SEARCH_RESULT_AS_PLAYLIST] = enabled
        }
    }

    open val searchHistoryFlow: Flow<List<String>> = context.dataStore.data
        .map { preferences ->
            preferences[SEARCH_HISTORY]?.toList() ?: emptyList()
        }

    open suspend fun addSearchHistory(query: String) {
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

    open suspend fun removeSearchHistory(query: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[SEARCH_HISTORY] ?: emptySet()
            preferences[SEARCH_HISTORY] = current.filter { it != query }.toSet()
        }
    }

    open suspend fun clearSearchHistory() {
        context.dataStore.edit { preferences ->
            preferences.remove(SEARCH_HISTORY)
        }
    }

    open suspend fun updateShowTranslation(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_TRANSLATION] = show
        }
    }

    open suspend fun updateFontSize(size: Float) {
        context.dataStore.edit { preferences ->
            preferences[FONT_SIZE] = size
        }
    }

    open suspend fun updateFontWeight(weight: Int) {
        context.dataStore.edit { preferences ->
            preferences[FONT_WEIGHT] = weight
        }
    }

    open suspend fun updateBlurEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BLUR_ENABLED] = enabled
        }
    }

    open suspend fun updateAlignment(alignment: Int) {
        context.dataStore.edit { preferences ->
            preferences[ALIGNMENT] = alignment
        }
    }

    open suspend fun updateBalanceLines(balance: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BALANCE_LINES] = balance
        }
    }

    open suspend fun updateLyricSharingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LYRIC_SHARING_ENABLED] = enabled
        }
    }

    open val playbackStateFlow: Flow<PlaybackState> = context.dataStore.data
        .map { preferences ->
            PlaybackState(
                repeatMode = preferences[REPEAT_MODE] ?: 0,
                shuffleMode = preferences[SHUFFLE_MODE] ?: false,
                lastMediaId = preferences[LAST_PLAYED_MEDIA_ID],
                lastPosition = preferences[LAST_PLAYED_POSITION] ?: 0L,
                lastPlaylistIds = preferences[LAST_PLAYLIST_IDS]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            )
        }

    open suspend fun updateRepeatMode(mode: Int) {
        context.dataStore.edit { preferences ->
            preferences[REPEAT_MODE] = mode
        }
    }

    open suspend fun updateShuffleMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHUFFLE_MODE] = enabled
        }
    }

    open suspend fun updateLastPlayedMediaId(mediaId: String?) {
        context.dataStore.edit { preferences ->
            if (mediaId == null) {
                preferences.remove(LAST_PLAYED_MEDIA_ID)
            } else {
                preferences[LAST_PLAYED_MEDIA_ID] = mediaId
            }
        }
    }

    open suspend fun updateLastPlayedPosition(position: Long) {
        context.dataStore.edit { preferences ->
            preferences[LAST_PLAYED_POSITION] = position
        }
    }

    open suspend fun updateLastPlaylistIds(ids: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[LAST_PLAYLIST_IDS] = ids.joinToString(",")
        }
    }
}
