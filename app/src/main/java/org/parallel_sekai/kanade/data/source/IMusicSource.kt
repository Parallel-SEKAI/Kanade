package org.parallel_sekai.kanade.data.source

import org.parallel_sekai.kanade.data.model.*

/**
 * 核心音源接口，支持本地和脚本化第三方音源
 */
interface IMusicSource {
    val sourceId: String
    val sourceName: String

    /**
     * 获取搜索结果或歌单内容
     */
    suspend fun getMusicList(query: String): List<MusicModel>

    /**
     * 获取首页推荐列表
     */
    suspend fun getHomeList(): List<MusicModel> = emptyList()

    /**
     * 获取真实的播放链接（对于流媒体脚本尤其重要）
     */
    suspend fun getPlayUrl(musicId: String): String

    /**
     * 按需获取歌词
     */
    suspend fun getLyrics(musicId: String): String?

    /**
     * 获取艺术家列表
     */
    suspend fun getArtistList(): List<ArtistModel> = emptyList()

    /**
     * 获取专辑列表
     */
    suspend fun getAlbumList(): List<AlbumModel> = emptyList()

    /**
     * 获取文件夹列表
     */
    suspend fun getFolderList(): List<FolderModel> = emptyList()

    /**
     * 获取特定艺术家的歌曲
     */
    suspend fun getSongsByArtist(artistName: String): List<MusicModel> = emptyList()

    /**
     * 获取特定专辑的歌曲
     */
    suspend fun getSongsByAlbum(albumId: String): List<MusicModel> = emptyList()

    /**
     * 获取特定文件夹的歌曲
     */
    suspend fun getSongsByFolder(path: String): List<MusicModel> = emptyList()

    /**
     * 获取播放列表列表
     */
    suspend fun getPlaylistList(): List<PlaylistModel> = emptyList()

    /**
     * 获取特定播放列表的歌曲
     */
    suspend fun getSongsByPlaylist(playlistId: String): List<MusicModel> = emptyList()

    /**
     * 根据 ID 列表获取歌曲
     */
    suspend fun getMusicListByIds(ids: List<String>): List<MusicModel> = emptyList()
}

/**
 * 脚本注入层逻辑伪代码
 * 允许未来通过加载 JS 或其他脚本动态创建 IMusicSource 实例
 */
class ScriptSourceManager {
    fun loadExternalScript(scriptPath: String): IMusicSource {
        // 1. 读取脚本内容
        // 2. 通过脚本引擎（如 QuickJS 或 J）解析
        // 3. 将脚本中的函数映射到 IMusicSource 接口
        return object : IMusicSource {
            override val sourceId = "dynamic_script_id"
            override val sourceName = "Remote Source via Script"
            override suspend fun getMusicList(query: String) = emptyList<MusicModel>()
            override suspend fun getPlayUrl(musicId: String) = "https://..."
            override suspend fun getLyrics(musicId: String): String? = null
        }
    }
}
