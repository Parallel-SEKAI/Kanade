package org.parallel_sekai.kanade.data.source.local

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.parallel_sekai.kanade.data.source.IMusicSource
import org.parallel_sekai.kanade.data.source.MusicModel
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 本地音源实现：基于 MediaStore API 扫描手机存储中的音频文件
 */
class LocalMusicSource(private val context: Context) : IMusicSource {

    override val sourceId: String = "local_storage"
    override val sourceName: String = "Local Music"

    override suspend fun getMusicList(query: String): List<MusicModel> = withContext(Dispatchers.IO) {
        val musicList = mutableListOf<MusicModel>()
        
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
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

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val albumId = cursor.getLong(albumIdColumn)
                
                // 构建音频的 Uri
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                // 修正：构建专辑封面的 Uri
                val albumArtUri = ContentUris.withAppendedId(
                    android.net.Uri.parse("content://media/external/audio/albumart"),
                    albumId
                ).toString()

                musicList.add(
                    MusicModel(
                        id = id.toString(),
                        title = cursor.getString(titleColumn),
                        artist = cursor.getString(artistColumn),
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
                it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true)
            }
        }

        return@withContext musicList
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
