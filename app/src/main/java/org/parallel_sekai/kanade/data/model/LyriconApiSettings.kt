package org.parallel_sekai.kanade.data.model

/**
 * Settings for Lyricon Provider API integration.
 * These settings control how lyrics are formatted and sent to Lyricon-compatible apps.
 */
data class LyriconApiSettings(
    /**
     * Whether Lyricon API is enabled.
     */
    val enabled: Boolean = true,
    /**
     * Whether to send word-by-word (syllable) lyric data.
     */
    val enableWordByWord: Boolean = true,
    /**
     * Whether to enable scrolling truncate mode.
     */
    val scrollingTruncateEnabled: Boolean = false,
    /**
     * Maximum number of display units (characters/words) to show.
     */
    val maxDisplayUnits: Int = 40,
    /**
     * Whether to enable smart units calculation.
     */
    val smartUnitsEnabled: Boolean = true,
    /**
     * Whether to show timestamp in lyric display.
     */
    val showTimestamp: Boolean = false,
    /**
     * Display states to enable (0=playing, 1=paused, 2=stopped).
     */
    val displayStates: Set<Int> = setOf(0, 1, 2),
    /**
     * Whether to clear lyrics when playback is paused.
     */
    val clearOnPause: Boolean = true,
    /**
     * Whether to display translation in Lyricon.
     */
    val displayTranslation: Boolean = true,
    /**
     * Whether to display romanization in Lyricon.
     */
    val displayRoma: Boolean = false,
)
