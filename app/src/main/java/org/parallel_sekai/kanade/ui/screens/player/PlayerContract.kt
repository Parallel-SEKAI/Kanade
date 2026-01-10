package org.parallel_sekai.kanade.ui.screens.player

import org.parallel_sekai.kanade.data.source.MusicModel

enum class RepeatMode {
    OFF, ONE, ALL
}

/**
 * MVI Contract for Kanade Player
 */
data class PlayerState(
    val currentSong: MusicModel? = null,
    val musicList: List<MusicModel> = emptyList(), // 存储扫描到的列表
    val isPlaying: Boolean = false,
    val isExpanded: Boolean = false, 
    val progress: Long = 0L,
    val duration: Long = 0L,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleModeEnabled: Boolean = false,
    val lyrics: String? = null,
    val lyricData: LyricData? = null
)

sealed interface PlayerIntent {
    object PlayPause : PlayerIntent
    object Next : PlayerIntent
    object Previous : PlayerIntent
    object Expand : PlayerIntent
    object Collapse : PlayerIntent
    object RefreshList : PlayerIntent
    object ToggleRepeat : PlayerIntent
    object ToggleShuffle : PlayerIntent
    data class SelectSong(val song: MusicModel) : PlayerIntent // 选择歌曲
    data class SeekTo(val position: Long) : PlayerIntent
}

sealed interface PlayerEffect {
    data class ShowError(val message: String) : PlayerEffect
}
