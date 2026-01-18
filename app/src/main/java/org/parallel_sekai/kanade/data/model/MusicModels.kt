package org.parallel_sekai.kanade.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MusicModel(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("artists") val artists: List<String>,
    @SerialName("album") val album: String,
    @SerialName("coverUrl") val coverUrl: String,
    @SerialName("mediaUri") val mediaUri: String,
    @SerialName("duration") val duration: Long,
    @SerialName("sourceId") val sourceId: String,
    @SerialName("lyrics") val lyrics: String? = null,
)

@Serializable
data class ArtistModel(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("albumCount") val albumCount: Int,
    @SerialName("songCount") val songCount: Int,
)

@Serializable
data class AlbumModel(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("artists") val artists: List<String>,
    @SerialName("coverUrl") val coverUrl: String,
    @SerialName("songCount") val songCount: Int,
)

@Serializable
data class FolderModel(
    @SerialName("name") val name: String,
    @SerialName("path") val path: String,
    @SerialName("songCount") val songCount: Int,
)

@Serializable
data class PlaylistModel(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("coverUrl") val coverUrl: String?,
    @SerialName("songCount") val songCount: Int,
)
