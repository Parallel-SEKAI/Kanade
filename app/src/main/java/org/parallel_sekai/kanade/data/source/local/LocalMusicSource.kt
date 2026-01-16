package org.parallel_sekai.kanade.data.source.local

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.parallel_sekai.kanade.data.model.*
import org.parallel_sekai.kanade.data.source.IMusicSource
import org.parallel_sekai.kanade.data.source.MusicUtils
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 本地音源实现：基于 MediaStore API 扫描手机存储中的音频文件
 */
class LocalMusicSource(private val context: Context) : IMusicSource {

    override val sourceId: String = "local_storage"
    override val sourceName: String = "Local Music"
    var excludedFolders: Set<String> = emptySet()

    override suspend fun getMusicList(query: String): List<MusicModel> = withContext(Dispatchers.IO) {
        val musicList = mutableListOf<MusicModel>()
        
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA
        )

        // 只扫描时长大于 30 秒的音频，过滤掉系统提示音
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 30000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val path = cursor.getString(dataColumn)
                // 排除文件夹过滤
                if (excludedFolders.any { path.startsWith(it) }) continue

                val id = cursor.getLong(idColumn)
                val albumId = cursor.getLong(albumIdColumn)
                
                // 构建音频的 Uri
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                // 恢复：使用专辑封面的 Uri
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                ).toString()

                musicList.add(
                    MusicModel(
                        id = id.toString(),
                        title = cursor.getString(titleColumn),
                        artists = MusicUtils.parseArtists(cursor.getString(artistColumn)),
                        album = cursor.getString(albumColumn),
                        coverUrl = albumArtUri,
                        mediaUri = contentUri.toString(), // 填充 content:// URI
                        duration = cursor.getLong(durationColumn),
                        sourceId = sourceId,
                        lyrics = null // Initial scan doesn't fetch lyrics for performance
                    )
                )
            }
        }
        
        // 如果有搜索词，进行简单的内存过滤
        if (query.isNotEmpty()) {
            return@withContext musicList.filter { 
                it.title.contains(query, ignoreCase = true) || it.artists.any { artist -> artist.contains(query, ignoreCase = true) }
            }
        }

        return@withContext musicList
    }

    override suspend fun getArtistList(): List<ArtistModel> = withContext(Dispatchers.IO) {
        val artistStats = mutableMapOf<String, Pair<Int, MutableSet<String>>>() // Name -> (SongCount, AlbumSet)

        val projection = arrayOf(
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 30000"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            null
        )?.use { cursor ->
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val path = cursor.getString(dataColumn)
                // 排除文件夹过滤
                if (excludedFolders.any { path.startsWith(it) }) continue

                val artistString = cursor.getString(artistColumn)
                val album = cursor.getString(albumColumn) ?: "Unknown Album"
                
                val parsedArtists = MusicUtils.parseArtists(artistString)
                
                parsedArtists.forEach { artistName ->
                    val stats = artistStats.getOrPut(artistName) { 0 to mutableSetOf() }
                    // Update song count (using Pair's first component is immutable, need to replace)
                    val newCount = stats.first + 1
                    stats.second.add(album)
                    artistStats[artistName] = newCount to stats.second
                }
            }
        }

        artistStats.map { (name, stats) ->
            ArtistModel(
                id = name, // Use name as ID for parsed artists
                name = name,
                albumCount = stats.second.size,
                songCount = stats.first
            )
        }.sortedBy { it.name }
    }

    override suspend fun getAlbumList(): List<AlbumModel> = withContext(Dispatchers.IO) {
        // ID -> (Title, List of ArtistSets)
        val albumMap = mutableMapOf<Long, Pair<String, MutableList<Set<String>>>>()

        val projection = arrayOf(
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 30000"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            null
        )?.use { cursor ->
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val path = cursor.getString(dataCol)
                if (excludedFolders.any { path.startsWith(it) }) continue

                val albumId = cursor.getLong(albumIdCol)
                val rawArtist = cursor.getString(artistCol)
                val albumTitle = cursor.getString(albumCol) ?: "Unknown Album"

                val artists = MusicUtils.parseArtists(rawArtist).toSet()

                val entry = albumMap.getOrPut(albumId) { albumTitle to mutableListOf() }
                entry.second.add(artists)
            }
        }

        albumMap.map { (id, pair) ->
            val (title, artistSets) = pair
            val commonArtists = if (artistSets.isNotEmpty()) {
                artistSets.reduce { acc, set -> acc.intersect(set) }
            } else {
                emptySet()
            }

            val finalArtists = if (commonArtists.isNotEmpty()) {
                commonArtists.toList().sorted()
            } else {
                listOf(context.getString(org.parallel_sekai.kanade.R.string.various_artists))
            }

            val albumArtUri = ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"),
                id
            ).toString()

            AlbumModel(
                id = id.toString(),
                title = title,
                artists = finalArtists,
                coverUrl = albumArtUri,
                songCount = artistSets.size
            )
        }.sortedBy { it.title }
    }

    override suspend fun getFolderList(): List<FolderModel> = withContext(Dispatchers.IO) {
        val folderMap = mutableMapOf<String, Int>()
        val projection = arrayOf(MediaStore.Audio.Media.DATA)
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            null
        )?.use { cursor ->
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            while (cursor.moveToNext()) {
                val path = cursor.getString(dataColumn)
                val folderPath = File(path).parent ?: continue
                folderMap[folderPath] = folderMap.getOrDefault(folderPath, 0) + 1
            }
        }

        folderMap.map { (path, count) ->
            FolderModel(
                name = File(path).name,
                path = path,
                songCount = count
            )
        }.filter { folder ->
            !excludedFolders.any { excluded -> folder.path.startsWith(excluded) }
        }.sortedBy { it.name }
    }

    override suspend fun getSongsByArtist(artistName: String): List<MusicModel> {
        val selection = "${MediaStore.Audio.Media.ARTIST} LIKE ?"
        val potentialSongs = getMusicListWithSelection(selection, arrayOf("%$artistName%"))
        return potentialSongs.filter { song ->
            song.artists.any { it.equals(artistName, ignoreCase = true) }
        }
    }

    override suspend fun getSongsByAlbum(albumId: String): List<MusicModel> {
        val selection = "${MediaStore.Audio.Media.ALBUM_ID} = ?"
        return getMusicListWithSelection(selection, arrayOf(albumId))
    }

    override suspend fun getSongsByFolder(path: String): List<MusicModel> {
        val selection = "${MediaStore.Audio.Media.DATA} LIKE ?"
        return getMusicListWithSelection(selection, arrayOf("$path/%"))
    }

    override suspend fun getPlaylistList(): List<PlaylistModel> = withContext(Dispatchers.IO) {
        val playlistList = mutableListOf<PlaylistModel>()
        val projection = arrayOf(
            MediaStore.Audio.Playlists._ID,
            MediaStore.Audio.Playlists.NAME
        )

        context.contentResolver.query(
            MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Audio.Playlists.NAME} ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                playlistList.add(
                    PlaylistModel(
                        id = id.toString(),
                        name = cursor.getString(nameColumn),
                        coverUrl = null, // Playlists usually don't have a direct cover in MediaStore
                        songCount = 0 // Needs separate query or updated in UI
                    )
                )
            }
        }
        playlistList
    }

    override suspend fun getSongsByPlaylist(playlistId: String): List<MusicModel> = withContext(Dispatchers.IO) {
        val musicList = mutableListOf<MusicModel>()
        val uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId.toLong())
        
        val projection = arrayOf(
            MediaStore.Audio.Playlists.Members.AUDIO_ID,
            MediaStore.Audio.Playlists.Members.TITLE,
            MediaStore.Audio.Playlists.Members.ARTIST,
            MediaStore.Audio.Playlists.Members.ALBUM,
            MediaStore.Audio.Playlists.Members.DURATION,
            MediaStore.Audio.Playlists.Members.ALBUM_ID
        )

        context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            MediaStore.Audio.Playlists.Members.PLAY_ORDER + " ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.AUDIO_ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.DURATION)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.ALBUM_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val albumId = cursor.getLong(albumIdColumn)
                val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                val albumArtUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId).toString()

                musicList.add(
                    MusicModel(
                        id = id.toString(),
                        title = cursor.getString(titleColumn),
                        artists = MusicUtils.parseArtists(cursor.getString(artistColumn)),
                        album = cursor.getString(albumColumn),
                        coverUrl = albumArtUri,
                        mediaUri = contentUri.toString(),
                        duration = cursor.getLong(durationColumn),
                        sourceId = sourceId
                    )
                )
            }
        }
        musicList
    }

    override suspend fun getMusicListByIds(ids: List<String>): List<MusicModel> {
        if (ids.isEmpty()) return emptyList()
        // 验证 ID 是否为纯数字，防止 SQL 注入（MediaStore ID 为 Long 类型）
        val safeIds = ids.filter { it.all { char -> char.isDigit() } }
        if (safeIds.isEmpty()) return emptyList()
        
        val selection = "${MediaStore.Audio.Media._ID} IN (${safeIds.joinToString(",")})"
        val musicList = getMusicListWithSelection(selection, emptyArray())
        // Preserve order
        val idToMusic = musicList.associateBy { it.id }
        return ids.mapNotNull { idToMusic[it] }
    }

    private suspend fun getMusicListWithSelection(selection: String, selectionArgs: Array<String>): List<MusicModel> = withContext(Dispatchers.IO) {
        val musicList = mutableListOf<MusicModel>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA
        )

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA) // Added dataColumn

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val albumId = cursor.getLong(albumIdColumn)
                val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                val albumArtUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId).toString()
                val path = cursor.getString(dataColumn) // Added path

                // 排除文件夹过滤
                if (excludedFolders.any { path.startsWith(it) }) continue

                musicList.add(
                    MusicModel(
                        id = id.toString(),
                        title = cursor.getString(titleColumn),
                        artists = MusicUtils.parseArtists(cursor.getString(artistColumn)),
                        album = cursor.getString(albumColumn),
                        coverUrl = albumArtUri,
                        mediaUri = contentUri.toString(),
                        duration = cursor.getLong(durationColumn),
                        sourceId = sourceId
                    )
                )
            }
        }
        musicList
    }

    override suspend fun getPlayUrl(musicId: String): String {
        // 对于本地音乐，musicId 即 MediaStore ID，播放链接即 ContentUri
        return ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            musicId.toLong()
        ).toString()
    }

    override suspend fun getLyrics(musicId: String): String? = withContext(Dispatchers.IO) {
        val contentUri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            musicId.toLong()
        )

        // 1. 尝试使用 MediaMetadataRetriever (支持部分 MP3 USLT)
        val retriever = MediaMetadataRetriever()
        var lyrics: String? = null
        try {
            retriever.setDataSource(context, contentUri)
            lyrics = retriever.extractMetadata(1000) // METADATA_KEY_LYRIC
        } catch (e: Exception) {
            // Ignore
        } finally {
            try { retriever.release() } catch (e: Exception) {}
        }

        if (!lyrics.isNullOrBlank()) return@withContext lyrics

        // 2. 如果是 FLAC，手动解析 Vorbis Comment
        try {
            context.contentResolver.openInputStream(contentUri)?.use { input ->
                lyrics = tryExtractFlacLyrics(input)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext if (!lyrics.isNullOrBlank()) lyrics else null
    }

    /**
     * 简易 FLAC Vorbis Comment 解析器
     */
    private fun tryExtractFlacLyrics(input: InputStream): String? {
        val header = ByteArray(4)
        if (input.read(header) != 4 || String(header) != "fLaC") return null

        while (true) {
            val blockHeader = input.read()
            if (blockHeader == -1) break
            val isLastBlock = (blockHeader and 0x80) != 0
            val blockType = blockHeader and 0x7F
            
            val lengthBytes = ByteArray(3)
            if (input.read(lengthBytes) != 3) break
            val length = (lengthBytes[0].toInt() and 0xFF shl 16) or
                         (lengthBytes[1].toInt() and 0xFF shl 8) or
                         (lengthBytes[2].toInt() and 0xFF)

            if (blockType == 4) { // VORBIS_COMMENT
                val commentData = ByteArray(length)
                if (input.read(commentData) != length) return null
                return parseVorbisComment(commentData)
            } else {
                input.skip(length.toLong())
            }
            if (isLastBlock) break
        }
        return null
    }

    private fun parseVorbisComment(data: ByteArray): String? {
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        
        // Vendor string
        if (buffer.remaining() < 4) return null
        val vendorLength = buffer.int
        if (vendorLength < 0 || vendorLength > buffer.remaining()) return null
        buffer.position(buffer.position() + vendorLength)
        
        // User comment list
        if (buffer.remaining() < 4) return null
        val userCommentListLength = buffer.int
        for (i in 0 until userCommentListLength) {
            if (buffer.remaining() < 4) break
            val commentLength = buffer.int
            if (commentLength < 0 || commentLength > buffer.remaining()) break
            val commentBytes = ByteArray(commentLength)
            buffer.get(commentBytes)
            val comment = String(commentBytes)
            
            val parts = comment.split("=", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].uppercase()
                if (key == "LYRICS" || key == "UNSYNCEDLYRICS" || key == "DESCRIPTION") {
                    return parts[1]
                }
            }
        }
        return null
    }
}
