package org.parallel_sekai.kanade.ui.screens.player

import androidx.compose.ui.graphics.Color
import org.parallel_sekai.kanade.data.source.AlbumModel
import org.parallel_sekai.kanade.data.source.ArtistModel
import org.parallel_sekai.kanade.data.source.FolderModel
import org.parallel_sekai.kanade.data.source.PlaylistModel
import org.parallel_sekai.kanade.data.source.MusicModel
import org.parallel_sekai.kanade.data.repository.LyricsSettings

enum class RepeatMode {
    OFF, ONE, ALL
}

enum class DetailType {
    ARTIST, ALBUM, FOLDER, PLAYLIST
}

/**
 * MVI Contract for Kanade Player
 */
data class PlayerState(
    val currentSong: MusicModel? = null,
    val allMusicList: List<MusicModel> = emptyList(), // 完整的本地库列表
    val currentPlaylist: List<MusicModel> = emptyList(), // 当前播放队列
    val artistList: List<ArtistModel> = emptyList(),
    val albumList: List<AlbumModel> = emptyList(),
    val folderList: List<FolderModel> = emptyList(),
    val playlistList: List<PlaylistModel> = emptyList(),
    val detailMusicList: List<MusicModel> = emptyList(), // 详情页显示的歌曲列表
    val isPlaying: Boolean = false,
    val isExpanded: Boolean = false, 
    val progress: Long = 0L,
    val duration: Long = 0L,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleModeEnabled: Boolean = false,
    val lyrics: String? = null,
    val lyricData: LyricData? = null,
    val lyricsSettings: LyricsSettings = LyricsSettings(),
    val artistJoinString: String = ", ", // 新增：艺术家拼接字符串
    val gradientColors: List<Color> = listOf(Color(0xFF1A1A1A), Color(0xFF000000))
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
    data class FetchDetailList(val type: DetailType, val id: String) : PlayerIntent
    data class SelectSong(val song: MusicModel, val customList: List<MusicModel>? = null) : PlayerIntent // 选择歌曲
    data class SeekTo(val position: Long) : PlayerIntent
}

sealed interface PlayerEffect {
    data class ShowError(val message: String) : PlayerEffect
}
