package org.parallel_sekai.kanade.data.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 媒体通知歌词覆盖状态的元数据
 */
data class MediaNotificationMetadataOverride(
    val title: String,
    val artist: String,
)

/**
 * 媒体通知歌词状态管理器（单例）
 * 用于管理媒体通知的 title/artist 覆盖状态，避免通过 replaceMediaItem 导致播放状态抖动
 */
object MediaNotificationLyricsState {
    private val _overrideFlow = MutableStateFlow<MediaNotificationMetadataOverride?>(null)

    /**
     * 当前覆盖状态的 Flow
     */
    val overrideFlow: StateFlow<MediaNotificationMetadataOverride?> = _overrideFlow.asStateFlow()

    /**
     * 当前覆盖状态的快照
     */
    val currentOverride: MediaNotificationMetadataOverride?
        get() = _overrideFlow.value

    /**
     * 更新覆盖状态
     * @param title 新的 title
     * @param artist 新的 artist
     * 注意：相同的 title/artist 不会重复发射
     */
    fun update(
        title: String,
        artist: String,
    ) {
        val newOverride = MediaNotificationMetadataOverride(title, artist)
        // 去重：只有当新值与当前值不同时才更新
        if (_overrideFlow.value != newOverride) {
            _overrideFlow.value = newOverride
        }
    }

    /**
     * 清除覆盖状态
     * 注意：重复调用不会重复发射 null
     */
    fun clear() {
        // 去重：只有当前有覆盖状态时才清除
        if (_overrideFlow.value != null) {
            _overrideFlow.value = null
        }
    }
}
