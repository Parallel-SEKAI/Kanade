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
    val isSharingEnabled: Boolean = true,
    val shareQuality: Float = 1.0f,
)

data class ArtistParsingSettings(
    val separators: List<String> = listOf("/", ";", "|", " & ", " feat. ", " ft. ", ","),
    val whitelist: List<String> = listOf("Leo/need"),
    val joinString: String = ", ",
)

data class PlaybackState(
    val repeatMode: Int = 0,
    val shuffleMode: Boolean = false,
    val lastMediaId: String? = null,
    val lastPosition: Long = 0L,
    val lastPlaylistJson: String? = null,
)

open class SettingsRepository(
    private val context: Context,
) {
    private val SHOW_TRANSLATION = booleanPreferencesKey("show_translation")
    private val FONT_SIZE = floatPreferencesKey("font_size")
    private val FONT_WEIGHT = intPreferencesKey("font_weight")
    private val BLUR_ENABLED = booleanPreferencesKey("blur_enabled")
    private val ALIGNMENT = intPreferencesKey("alignment")
    private val BALANCE_LINES = booleanPreferencesKey("balance_lines")
    private val LYRIC_SHARING_ENABLED = booleanPreferencesKey("lyric_sharing_enabled")
    private val LYRIC_SHARE_QUALITY = floatPreferencesKey("lyric_share_quality")
    private val SEARCH_HISTORY = stringSetPreferencesKey("search_history")
    private val SEARCH_RESULT_AS_PLAYLIST = booleanPreferencesKey("search_result_as_playlist")
    private val EXCLUDED_FOLDERS = stringSetPreferencesKey("excluded_folders")
    private val MAX_CACHE_SIZE = longPreferencesKey("max_cache_size")

    private val ARTIST_SEPARATORS = stringSetPreferencesKey("artist_separators")
    private val ARTIST_WHITELIST = stringSetPreferencesKey("artist_whitelist")
    private val ARTIST_JOIN_STRING = stringPreferencesKey("artist_join_string")

    // Playback state keys
    private val REPEAT_MODE = intPreferencesKey("playback_repeat_mode")
    private val SHUFFLE_MODE = booleanPreferencesKey("playback_shuffle_mode")
    private val LAST_PLAYED_MEDIA_ID = stringPreferencesKey("last_played_media_id")
    private val LAST_PLAYED_POSITION = longPreferencesKey("last_played_position")
    private val LAST_PLAYLIST_JSON = stringPreferencesKey("last_playlist_json")

    private val ACTIVE_SCRIPT_ID = stringPreferencesKey("active_script_id")

    // JSON string of Map<String, Map<String, String>>
    private val SCRIPT_CONFIGS = stringPreferencesKey("script_configs")

    // Media notification lyrics keys
    private val MEDIA_NOTIFICATION_LYRICS_ENABLED = booleanPreferencesKey("media_notification_lyrics_enabled")
    private val MEDIA_NOTIFICATION_LYRICS_MODE = intPreferencesKey("media_notification_lyrics_mode")
    private val MEDIA_NOTIFICATION_LYRICS_SCROLLING_TRUNCATE_ENABLED =
        booleanPreferencesKey("media_notification_lyrics_scrolling_truncate_enabled")
    private val MEDIA_NOTIFICATION_LYRICS_MAX_DISPLAY_UNITS =
        intPreferencesKey("media_notification_lyrics_max_display_units")
    private val MEDIA_NOTIFICATION_LYRICS_SMART_UNITS_ENABLED =
        booleanPreferencesKey("media_notification_lyrics_smart_units_enabled")
    private val MEDIA_NOTIFICATION_LYRICS_SHOW_TIMESTAMP =
        booleanPreferencesKey("media_notification_lyrics_show_timestamp")
    private val MEDIA_NOTIFICATION_LYRICS_DISPLAY_STATES =
        stringSetPreferencesKey("media_notification_lyrics_display_states")
    private val MEDIA_NOTIFICATION_LYRICS_RESTORE_ON_PAUSE =
        booleanPreferencesKey("media_notification_lyrics_restore_on_pause")
    private val MEDIA_NOTIFICATION_LYRICS_TRANSLATION_MAX_DISPLAY_UNITS =
        intPreferencesKey("media_notification_lyrics_translation_max_display_units")

    // LyricGetter API keys
    private val LYRIC_GETTER_API_ENABLED = booleanPreferencesKey("lyric_getter_api_enabled")
    private val LYRIC_GETTER_API_SCROLLING_TRUNCATE_ENABLED =
        booleanPreferencesKey("lyric_getter_api_scrolling_truncate_enabled")
    private val LYRIC_GETTER_API_MAX_DISPLAY_UNITS =
        intPreferencesKey("lyric_getter_api_max_display_units")
    private val LYRIC_GETTER_API_SMART_UNITS_ENABLED =
        booleanPreferencesKey("lyric_getter_api_smart_units_enabled")
    private val LYRIC_GETTER_API_SHOW_TIMESTAMP =
        booleanPreferencesKey("lyric_getter_api_show_timestamp")
    private val LYRIC_GETTER_API_DISPLAY_STATES =
        stringSetPreferencesKey("lyric_getter_api_display_states")
    private val LYRIC_GETTER_API_CLEAR_ON_PAUSE =
        booleanPreferencesKey("lyric_getter_api_clear_on_pause")

    // SuperLyric API keys
    private val SUPER_LYRIC_API_ENABLED = booleanPreferencesKey("super_lyric_api_enabled")
    private val SUPER_LYRIC_API_SCROLLING_TRUNCATE_ENABLED =
        booleanPreferencesKey("super_lyric_api_scrolling_truncate_enabled")
    private val SUPER_LYRIC_API_MAX_DISPLAY_UNITS =
        intPreferencesKey("super_lyric_api_max_display_units")
    private val SUPER_LYRIC_API_SMART_UNITS_ENABLED =
        booleanPreferencesKey("super_lyric_api_smart_units_enabled")
    private val SUPER_LYRIC_API_SHOW_TIMESTAMP =
        booleanPreferencesKey("super_lyric_api_show_timestamp")
    private val SUPER_LYRIC_API_DISPLAY_STATES =
        stringSetPreferencesKey("super_lyric_api_display_states")
    private val SUPER_LYRIC_API_CLEAR_ON_PAUSE =
        booleanPreferencesKey("super_lyric_api_clear_on_pause")

    open val activeScriptIdFlow: Flow<String?> =
        context.dataStore.data
            .map { preferences -> preferences[ACTIVE_SCRIPT_ID] }

    open val scriptConfigsFlow: Flow<String?> =
        context.dataStore.data
            .map { preferences -> preferences[SCRIPT_CONFIGS] }

    open suspend fun updateActiveScriptId(id: String?) {
        context.dataStore.edit { preferences ->
            if (id == null) {
                preferences.remove(ACTIVE_SCRIPT_ID)
            } else {
                preferences[ACTIVE_SCRIPT_ID] = id
            }
        }
    }

    open suspend fun updateScriptConfigs(configsJson: String) {
        context.dataStore.edit { preferences ->
            preferences[SCRIPT_CONFIGS] = configsJson
        }
    }

    open val lyricsSettingsFlow: Flow<LyricsSettings> =
        context.dataStore.data
            .map { preferences ->
                LyricsSettings(
                    showTranslation = preferences[SHOW_TRANSLATION] ?: true,
                    fontSize = preferences[FONT_SIZE] ?: 18f,
                    fontWeight = preferences[FONT_WEIGHT] ?: 400,
                    blurEnabled = preferences[BLUR_ENABLED] ?: true,
                    alignment = preferences[ALIGNMENT] ?: 0,
                    balanceLines = preferences[BALANCE_LINES] ?: false,
                    isSharingEnabled = preferences[LYRIC_SHARING_ENABLED] ?: true,
                    shareQuality = preferences[LYRIC_SHARE_QUALITY] ?: 1.0f,
                )
            }

    open val searchResultAsPlaylistFlow: Flow<Boolean> =
        context.dataStore.data
            .map { preferences ->
                preferences[SEARCH_RESULT_AS_PLAYLIST] ?: true
            }

    open val excludedFoldersFlow: Flow<Set<String>> =
        context.dataStore.data
            .map { preferences ->
                preferences[EXCLUDED_FOLDERS] ?: emptySet()
            }

    open val maxCacheSizeFlow: Flow<Long> =
        context.dataStore.data
            .map { preferences ->
                preferences[MAX_CACHE_SIZE] ?: (500 * 1024 * 1024L) // Default 500MB
            }

    open val artistParsingSettingsFlow: Flow<ArtistParsingSettings> =
        context.dataStore.data
            .map { preferences ->
                ArtistParsingSettings(
                    separators =
                        preferences[ARTIST_SEPARATORS]?.toList() ?: listOf(
                            "/",
                            ";",
                            "|",
                            " & ",
                            " feat. ",
                            " ft. ",
                            ",",
                        ),
                    whitelist = preferences[ARTIST_WHITELIST]?.toList() ?: listOf("Leo/need"),
                    joinString = preferences[ARTIST_JOIN_STRING] ?: ", ",
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

    open suspend fun updateMaxCacheSize(size: Long) {
        context.dataStore.edit { preferences ->
            preferences[MAX_CACHE_SIZE] = size
        }
    }

    open suspend fun updateSearchResultAsPlaylist(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SEARCH_RESULT_AS_PLAYLIST] = enabled
        }
    }

    open val searchHistoryFlow: Flow<List<String>> =
        context.dataStore.data
            .map { preferences ->
                preferences[SEARCH_HISTORY]?.toList() ?: emptyList()
            }

    open suspend fun addSearchHistory(query: String) {
        if (query.isBlank()) return
        context.dataStore.edit { preferences ->
            val current = preferences[SEARCH_HISTORY] ?: emptySet()
            val updated =
                current.toMutableSet().apply {
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

    open suspend fun updateLyricShareQuality(quality: Float) {
        context.dataStore.edit { preferences ->
            preferences[LYRIC_SHARE_QUALITY] = quality
        }
    }

    open val playbackStateFlow: Flow<PlaybackState> =
        context.dataStore.data
            .map { preferences ->
                PlaybackState(
                    repeatMode = preferences[REPEAT_MODE] ?: 0,
                    shuffleMode = preferences[SHUFFLE_MODE] ?: false,
                    lastMediaId = preferences[LAST_PLAYED_MEDIA_ID],
                    lastPosition = preferences[LAST_PLAYED_POSITION] ?: 0L,
                    lastPlaylistJson = preferences[LAST_PLAYLIST_JSON],
                )
            }

    open val mediaNotificationLyricsSettingsFlow:
        Flow<org.parallel_sekai.kanade.data.model.MediaNotificationLyricsSettings> =
        context.dataStore.data
            .map { preferences ->
                org.parallel_sekai.kanade.data.model.MediaNotificationLyricsSettings(
                    enabled = preferences[MEDIA_NOTIFICATION_LYRICS_ENABLED] ?: false,
                    mode = preferences[MEDIA_NOTIFICATION_LYRICS_MODE] ?: 0,
                    scrollingTruncateEnabled =
                        preferences[MEDIA_NOTIFICATION_LYRICS_SCROLLING_TRUNCATE_ENABLED] ?: false,
                    maxDisplayUnits = preferences[MEDIA_NOTIFICATION_LYRICS_MAX_DISPLAY_UNITS] ?: 40,
                    smartUnitsEnabled = preferences[MEDIA_NOTIFICATION_LYRICS_SMART_UNITS_ENABLED] ?: true,
                    showTimestamp = preferences[MEDIA_NOTIFICATION_LYRICS_SHOW_TIMESTAMP] ?: false,
                    displayStates =
                        preferences[MEDIA_NOTIFICATION_LYRICS_DISPLAY_STATES]?.mapNotNull { it.toIntOrNull() }?.toSet()
                            ?: setOf(0, 1, 2),
                    restoreOnPause = preferences[MEDIA_NOTIFICATION_LYRICS_RESTORE_ON_PAUSE] ?: true,
                    translationMaxDisplayUnits =
                        preferences[MEDIA_NOTIFICATION_LYRICS_TRANSLATION_MAX_DISPLAY_UNITS] ?: 40,
                )
            }

    open val lyricGetterApiSettingsFlow:
        Flow<org.parallel_sekai.kanade.data.model.ExternalLyricApiSettings> =
        context.dataStore.data
            .map { preferences ->
                org.parallel_sekai.kanade.data.model.ExternalLyricApiSettings(
                    enabled = preferences[LYRIC_GETTER_API_ENABLED] ?: true,
                    scrollingTruncateEnabled =
                        preferences[LYRIC_GETTER_API_SCROLLING_TRUNCATE_ENABLED] ?: false,
                    maxDisplayUnits = preferences[LYRIC_GETTER_API_MAX_DISPLAY_UNITS] ?: 40,
                    smartUnitsEnabled = preferences[LYRIC_GETTER_API_SMART_UNITS_ENABLED] ?: true,
                    showTimestamp = preferences[LYRIC_GETTER_API_SHOW_TIMESTAMP] ?: false,
                    displayStates =
                        preferences[LYRIC_GETTER_API_DISPLAY_STATES]?.mapNotNull { it.toIntOrNull() }?.toSet()
                            ?: setOf(0, 1, 2),
                    clearOnPause = preferences[LYRIC_GETTER_API_CLEAR_ON_PAUSE] ?: true,
                )
            }

    open val superLyricApiSettingsFlow:
        Flow<org.parallel_sekai.kanade.data.model.ExternalLyricApiSettings> =
        context.dataStore.data
            .map { preferences ->
                org.parallel_sekai.kanade.data.model.ExternalLyricApiSettings(
                    enabled = preferences[SUPER_LYRIC_API_ENABLED] ?: true,
                    scrollingTruncateEnabled =
                        preferences[SUPER_LYRIC_API_SCROLLING_TRUNCATE_ENABLED] ?: false,
                    maxDisplayUnits = preferences[SUPER_LYRIC_API_MAX_DISPLAY_UNITS] ?: 40,
                    smartUnitsEnabled = preferences[SUPER_LYRIC_API_SMART_UNITS_ENABLED] ?: true,
                    showTimestamp = preferences[SUPER_LYRIC_API_SHOW_TIMESTAMP] ?: false,
                    displayStates =
                        preferences[SUPER_LYRIC_API_DISPLAY_STATES]?.mapNotNull { it.toIntOrNull() }?.toSet()
                            ?: setOf(0, 1, 2),
                    clearOnPause = preferences[SUPER_LYRIC_API_CLEAR_ON_PAUSE] ?: true,
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

    open suspend fun updateLastPlaylistJson(json: String?) {
        context.dataStore.edit { preferences ->
            if (json == null) {
                preferences.remove(LAST_PLAYLIST_JSON)
            } else {
                preferences[LAST_PLAYLIST_JSON] = json
            }
        }
    }

    open suspend fun updateMediaNotificationLyricsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MEDIA_NOTIFICATION_LYRICS_ENABLED] = enabled
        }
    }

    open suspend fun updateMediaNotificationLyricsMode(mode: Int) {
        context.dataStore.edit { preferences ->
            preferences[MEDIA_NOTIFICATION_LYRICS_MODE] = mode
        }
    }

    open suspend fun updateMediaNotificationLyricsScrollingTruncateEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MEDIA_NOTIFICATION_LYRICS_SCROLLING_TRUNCATE_ENABLED] = enabled
        }
    }

    open suspend fun updateMediaNotificationLyricsMaxDisplayUnits(units: Int) {
        context.dataStore.edit { preferences ->
            preferences[MEDIA_NOTIFICATION_LYRICS_MAX_DISPLAY_UNITS] = units.coerceIn(3, 120)
        }
    }

    open suspend fun updateMediaNotificationLyricsSmartUnitsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MEDIA_NOTIFICATION_LYRICS_SMART_UNITS_ENABLED] = enabled
        }
    }

    open suspend fun updateMediaNotificationLyricsShowTimestamp(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MEDIA_NOTIFICATION_LYRICS_SHOW_TIMESTAMP] = enabled
        }
    }

    open suspend fun updateMediaNotificationLyricsDisplayStates(states: Set<Int>) {
        context.dataStore.edit { preferences ->
            val validStates = states.filter { it in 0..2 }.toSet()
            val finalStates = if (validStates.isEmpty()) setOf(0) else validStates
            preferences[MEDIA_NOTIFICATION_LYRICS_DISPLAY_STATES] = finalStates.map { it.toString() }.toSet()
        }
    }

    open suspend fun updateMediaNotificationLyricsRestoreOnPause(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MEDIA_NOTIFICATION_LYRICS_RESTORE_ON_PAUSE] = enabled
        }
    }

    open suspend fun updateMediaNotificationLyricsTranslationMaxDisplayUnits(units: Int) {
        context.dataStore.edit { preferences ->
            preferences[MEDIA_NOTIFICATION_LYRICS_TRANSLATION_MAX_DISPLAY_UNITS] = units.coerceIn(3, 120)
        }
    }

    // LyricGetter API update methods
    open suspend fun updateLyricGetterApiEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LYRIC_GETTER_API_ENABLED] = enabled
        }
    }

    open suspend fun updateLyricGetterApiScrollingTruncateEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LYRIC_GETTER_API_SCROLLING_TRUNCATE_ENABLED] = enabled
        }
    }

    open suspend fun updateLyricGetterApiMaxDisplayUnits(units: Int) {
        context.dataStore.edit { preferences ->
            preferences[LYRIC_GETTER_API_MAX_DISPLAY_UNITS] = units.coerceIn(3, 120)
        }
    }

    open suspend fun updateLyricGetterApiSmartUnitsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LYRIC_GETTER_API_SMART_UNITS_ENABLED] = enabled
        }
    }

    open suspend fun updateLyricGetterApiShowTimestamp(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LYRIC_GETTER_API_SHOW_TIMESTAMP] = enabled
        }
    }

    open suspend fun updateLyricGetterApiDisplayStates(states: Set<Int>) {
        context.dataStore.edit { preferences ->
            val validStates = states.filter { it in 0..2 }.toSet()
            val finalStates = if (validStates.isEmpty()) setOf(0) else validStates
            preferences[LYRIC_GETTER_API_DISPLAY_STATES] = finalStates.map { it.toString() }.toSet()
        }
    }

    open suspend fun updateLyricGetterApiClearOnPause(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LYRIC_GETTER_API_CLEAR_ON_PAUSE] = enabled
        }
    }

    // SuperLyric API update methods
    open suspend fun updateSuperLyricApiEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SUPER_LYRIC_API_ENABLED] = enabled
        }
    }

    open suspend fun updateSuperLyricApiScrollingTruncateEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SUPER_LYRIC_API_SCROLLING_TRUNCATE_ENABLED] = enabled
        }
    }

    open suspend fun updateSuperLyricApiMaxDisplayUnits(units: Int) {
        context.dataStore.edit { preferences ->
            preferences[SUPER_LYRIC_API_MAX_DISPLAY_UNITS] = units.coerceIn(3, 120)
        }
    }

    open suspend fun updateSuperLyricApiSmartUnitsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SUPER_LYRIC_API_SMART_UNITS_ENABLED] = enabled
        }
    }

    open suspend fun updateSuperLyricApiShowTimestamp(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SUPER_LYRIC_API_SHOW_TIMESTAMP] = enabled
        }
    }

    open suspend fun updateSuperLyricApiDisplayStates(states: Set<Int>) {
        context.dataStore.edit { preferences ->
            val validStates = states.filter { it in 0..2 }.toSet()
            val finalStates = if (validStates.isEmpty()) setOf(0) else validStates
            preferences[SUPER_LYRIC_API_DISPLAY_STATES] = finalStates.map { it.toString() }.toSet()
        }
    }

    open suspend fun updateSuperLyricApiClearOnPause(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SUPER_LYRIC_API_CLEAR_ON_PAUSE] = enabled
        }
    }
}
