package org.parallel_sekai.kanade.data.model

data class MusicModel(
    val id: String,
    val title: String,
    val artists: List<String>,
    val album: String,
    val coverUrl: String,
    val mediaUri: String, // 真实的播放 URI
    val duration: Long,
    val sourceId: String,
    val lyrics: String? = null
)

data class ArtistModel(
    val id: String,
    val name: String,
    val albumCount: Int,
    val songCount: Int
)

data class AlbumModel(
    val id: String,
    val title: String,
    val artists: List<String>,
    val coverUrl: String,
    val songCount: Int
)

data class FolderModel(
    val name: String,
    val path: String,
    val songCount: Int
)

data class PlaylistModel(
    val id: String,
    val name: String,
    val coverUrl: String?,
    val songCount: Int
)
