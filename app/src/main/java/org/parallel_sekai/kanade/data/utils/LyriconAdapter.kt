package org.parallel_sekai.kanade.data.utils

import android.util.Log
import io.github.proify.lyricon.lyric.model.LyricWord
import io.github.proify.lyricon.lyric.model.RichLyricLine
import io.github.proify.lyricon.lyric.model.Song
import org.parallel_sekai.kanade.data.model.LyricData
import org.parallel_sekai.kanade.data.model.LyricLine
import org.parallel_sekai.kanade.data.model.LyriconApiSettings
import org.parallel_sekai.kanade.data.model.MusicModel
import org.parallel_sekai.kanade.data.model.WordInfo

/**
 * Adapter to convert Kanade's internal models to Lyricon Provider API models.
 */
object LyriconAdapter {
    private const val TAG = "LyriconAdapter"

    /**
     * Convert Kanade MusicModel to Lyricon Song.
     */
    fun toSong(
        music: MusicModel,
        lyricData: LyricData?,
        settings: LyriconApiSettings,
    ): Song? =
        try {
            val lyrics =
                lyricData?.lines?.mapNotNull { line ->
                    toRichLyricLine(line, settings)
                } ?: emptyList()

            Song(
                id = music.id,
                name = music.title,
                artist = music.artists.joinToString(", "),
                duration = music.duration,
                lyrics = lyrics,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert MusicModel to Song", e)
            null
        }

    /**
     * Convert Kanade LyricLine to Lyricon RichLyricLine.
     */
    private fun toRichLyricLine(
        line: LyricLine,
        settings: LyriconApiSettings,
    ): RichLyricLine? =
        try {
            val words =
                if (settings.enableWordByWord && line.words.isNotEmpty()) {
                    line.words.mapNotNull { toLyricWord(it) }
                } else {
                    null
                }

            RichLyricLine(
                begin = line.startTime,
                end = line.endTime,
                text = line.content,
                translation = line.translation,
                words = words,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert LyricLine to RichLyricLine", e)
            null
        }

    /**
     * Convert Kanade WordInfo to Lyricon LyricWord.
     */
    private fun toLyricWord(word: WordInfo): LyricWord? =
        try {
            LyricWord(
                begin = word.startTime,
                end = word.endTime,
                text = word.text,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert WordInfo to LyricWord", e)
            null
        }
}
