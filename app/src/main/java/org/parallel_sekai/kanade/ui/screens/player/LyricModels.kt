package org.parallel_sekai.kanade.ui.screens.player

data class WordInfo(
    val text: String,
    val startTime: Long, // 毫秒
    val endTime: Long
)

data class LyricLine(
    val startTime: Long,
    val endTime: Long = 0L,
    val content: String,
    val translation: String? = null,
    val words: List<WordInfo> = emptyList()
)

data class LyricData(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val lines: List<LyricLine> = emptyList()
)
