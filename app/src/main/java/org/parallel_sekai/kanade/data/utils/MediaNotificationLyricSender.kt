package org.parallel_sekai.kanade.data.utils

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.parallel_sekai.kanade.data.model.LyricLine
import org.parallel_sekai.kanade.data.model.MediaNotificationLyricsSettings
import org.parallel_sekai.kanade.data.model.MusicModel
import org.parallel_sekai.kanade.data.repository.PlaybackRepository

/**
 * 媒体通知歌词发送器
 * 根据设置将歌词显示在媒体通知的 title/artist 字段中
 */
class MediaNotificationLyricSender(
    private val playbackRepository: PlaybackRepository,
    private val settingsFlow: StateFlow<MediaNotificationLyricsSettings>,
    private val deviceStateFlow: StateFlow<Int>,
    private val scope: CoroutineScope,
) {
    companion object {
        private const val TAG = "MediaNotificationLyricSender"
    }

    private var lastSentTitle: String? = null
    private var lastSentArtist: String? = null
    private var isEnabled = false
    private var originalTitle: String? = null
    private var originalArtist: String? = null

    init {
        // 监听设置和设备状态变化，只在 shouldEnable 变化时触发
        combine(settingsFlow, deviceStateFlow) { settings, deviceState ->
            settings.enabled && settings.displayStates.contains(deviceState)
        }.distinctUntilChanged() // 只在 shouldEnable 变化时触发
            .onEach { shouldEnable ->
                if (isEnabled && !shouldEnable) {
                    // 功能关闭或设备状态不允许，还原（只还原一次）
                    restore()
                }
                isEnabled = shouldEnable
            }.launchIn(scope)
    }

    /**
     * 发送歌词到媒体通知
     * @param currentLine 当前歌词行
     * @param song 当前歌曲信息
     * @param progress 当前播放进度（毫秒）
     */
    fun sendLyric(
        currentLine: LyricLine?,
        song: MusicModel?,
        progress: Long,
    ) {
        if (!isEnabled || currentLine == null || song == null) {
            return
        }

        val settings = settingsFlow.value

        // 保存原始信息（首次）
        if (originalTitle == null) {
            originalTitle = song.title
            originalArtist = song.artists.joinToString(", ")
        }

        // 生成显示文本
        val (title, artist) =
            generateDisplayText(
                currentLine = currentLine,
                song = song,
                progress = progress,
                settings = settings,
            )

        // 增强去重：title/artist 都相同时不更新，减少闪烁
        if (title != lastSentTitle || artist != lastSentArtist) {
            playbackRepository.updateCurrentMediaNotificationMetadata(title, artist)
            lastSentTitle = title
            lastSentArtist = artist
            Log.d(TAG, "Updated notification: title=$title, artist=$artist")
        }
    }

    /**
     * 还原媒体通知为原始信息
     */
    fun restore() {
        if (lastSentTitle != null || lastSentArtist != null) {
            playbackRepository.restoreCurrentMediaNotificationMetadata()
            lastSentTitle = null
            lastSentArtist = null
            Log.d(TAG, "Restored notification metadata")
        }
    }

    /**
     * 重置状态（切歌时调用）
     */
    fun reset() {
        lastSentTitle = null
        lastSentArtist = null
        originalTitle = null
        originalArtist = null
    }

    private fun generateDisplayText(
        currentLine: LyricLine,
        song: MusicModel,
        progress: Long,
        settings: MediaNotificationLyricsSettings,
    ): Pair<String, String> {
        val lyricContent = currentLine.content
        val translation = currentLine.translation

        // 根据模式生成
        val (rawTitle, rawArtist) =
            when (settings.mode) {
                1 -> {
                    // 模式 B：title=原文，artist=翻译（翻译为空回退模式 A）
                    if (!translation.isNullOrBlank()) {
                        lyricContent to translation
                    } else {
                        // 回退到模式 A
                        lyricContent to "${song.title} - ${song.artists.joinToString(", ")}"
                    }
                }
                else -> {
                    // 模式 A（默认）：title=原文，artist=原歌曲名+艺术家
                    lyricContent to "${song.title} - ${song.artists.joinToString(", ")}"
                }
            }

        // 添加时刻前缀（仅对 title 添加，artist 不添加）
        val titleWithTimestamp =
            if (settings.showTimestamp) {
                val timestamp = formatTimestamp(currentLine.startTime)
                "[$timestamp] $rawTitle"
            } else {
                rawTitle
            }

        // 处理滚动截取
        val finalTitle =
            if (settings.scrollingTruncateEnabled) {
                truncateWithScrolling(
                    text = titleWithTimestamp,
                    maxUnits = settings.maxDisplayUnits,
                    smartUnits = settings.smartUnitsEnabled,
                    progress = progress,
                    lineStartTime = currentLine.startTime,
                    lineDuration = (currentLine.endTime - currentLine.startTime).coerceAtLeast(1000L),
                )
            } else {
                titleWithTimestamp
            }

        // 根据模式和翻译状态决定 artist 使用的最大显示单位
        val artistMaxUnits =
            if (settings.mode == 1 && !translation.isNullOrBlank()) {
                settings.translationMaxDisplayUnits
            } else {
                settings.maxDisplayUnits
            }

        val finalArtist =
            if (settings.scrollingTruncateEnabled) {
                truncateWithScrolling(
                    text = rawArtist,
                    maxUnits = artistMaxUnits,
                    smartUnits = settings.smartUnitsEnabled,
                    progress = progress,
                    lineStartTime = currentLine.startTime,
                    lineDuration = (currentLine.endTime - currentLine.startTime).coerceAtLeast(1000L),
                )
            } else {
                rawArtist
            }

        return finalTitle to finalArtist
    }

    /**
     * 格式化时间戳为 mm:ss 格式
     */
    private fun formatTimestamp(timeMs: Long): String {
        val totalSeconds = (timeMs / 1000).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    /**
     * 滚动截取文本
     * @param text 原始文本
     * @param maxUnits 最大显示单位数
     * @param smartUnits 是否启用智能单位计算
     * @param progress 当前播放进度
     * @param lineStartTime 当前行开始时间
     * @param lineDuration 当前行持续时间
     */
    private fun truncateWithScrolling(
        text: String,
        maxUnits: Int,
        smartUnits: Boolean,
        progress: Long,
        lineStartTime: Long,
        lineDuration: Long,
    ): String {
        if (text.isEmpty()) return text

        val totalUnits = calculateTextUnits(text, smartUnits)
        if (totalUnits <= maxUnits) {
            return text
        }

        // 计算当前行内的相对进度（0.0 到 1.0）
        val relativeProgress = ((progress - lineStartTime).toFloat() / lineDuration.toFloat()).coerceIn(0f, 1f)

        // 计算滚动窗口的起始单位位置
        val maxStartUnit = (totalUnits - maxUnits).coerceAtLeast(0f)
        val startUnit = (maxStartUnit * relativeProgress).toInt()

        // 提取窗口内的文本
        return extractTextWindow(text, startUnit, maxUnits, smartUnits)
    }

    /**
     * 计算文本的单位数
     * @param text 文本
     * @param smartUnits 是否启用智能单位（中日韩等=1，ASCII字母数字空格半角标点=0.5，其他=1）
     */
    private fun calculateTextUnits(
        text: String,
        smartUnits: Boolean,
    ): Float {
        if (!smartUnits) {
            return text.length.toFloat()
        }

        var units = 0f
        for (char in text) {
            units +=
                when {
                    // ASCII 字母、数字、空格、半角标点
                    char in 'a'..'z' ||
                        char in 'A'..'Z' ||
                        char in '0'..'9' ||
                        char == ' ' ||
                        char in '!'..'/' ||
                        char in ':'..'@' ||
                        char in '['..'`' ||
                        char in '{'..'~' -> 0.5f
                    // 中日韩统一表意文字
                    char in '\u4E00'..'\u9FFF' -> 1f
                    // 日文平假名
                    char in '\u3040'..'\u309F' -> 1f
                    // 日文片假名
                    char in '\u30A0'..'\u30FF' -> 1f
                    // 韩文音节
                    char in '\uAC00'..'\uD7AF' -> 1f
                    // 全角字符
                    char in '\uFF00'..'\uFFEF' -> 1f
                    // 其他
                    else -> 1f
                }
        }
        return units
    }

    /**
     * 提取文本窗口
     * @param text 原始文本
     * @param startUnit 起始单位位置
     * @param maxUnits 最大单位数
     * @param smartUnits 是否启用智能单位
     */
    private fun extractTextWindow(
        text: String,
        startUnit: Int,
        maxUnits: Int,
        smartUnits: Boolean,
    ): String {
        if (text.isEmpty()) return text

        val ellipsisUnits = if (smartUnits) 1.5f else 3f // "..." 的单位数
        val needsLeadingEllipsis = startUnit > 0

        // 预判是否需要尾部省略号（粗略判断）
        val totalUnits = calculateTextUnits(text, smartUnits)
        val remainingUnits = totalUnits - startUnit
        val needsTrailingEllipsis = remainingUnits > maxUnits

        // 计算内容预算：如果需要前后省略号，都要计入预算
        val budgetForContent =
            when {
                needsLeadingEllipsis && needsTrailingEllipsis -> (maxUnits - ellipsisUnits * 2).coerceAtLeast(0f)
                needsLeadingEllipsis || needsTrailingEllipsis -> (maxUnits - ellipsisUnits).coerceAtLeast(0f)
                else -> maxUnits.toFloat()
            }

        // 如果预算不足，只显示省略号或最小内容
        if (budgetForContent <= 0f) {
            return when {
                needsLeadingEllipsis && needsTrailingEllipsis -> "......"
                needsLeadingEllipsis || needsTrailingEllipsis -> "..."
                else -> ""
            }
        }

        var currentUnit = 0f
        var startIndex = 0
        var endIndex = text.length

        // 找到起始位置
        for (i in text.indices) {
            val charUnit =
                if (smartUnits) {
                    getCharUnit(text[i])
                } else {
                    1f
                }

            if (currentUnit >= startUnit) {
                startIndex = i
                break
            }
            currentUnit += charUnit
        }

        // 从起始位置开始，找到结束位置（使用计算好的内容预算）
        currentUnit = 0f
        for (i in startIndex until text.length) {
            val charUnit =
                if (smartUnits) {
                    getCharUnit(text[i])
                } else {
                    1f
                }

            if (currentUnit + charUnit > budgetForContent) {
                endIndex = i
                break
            }
            currentUnit += charUnit
        }

        val window = text.substring(startIndex, endIndex)
        val actualNeedsTrailingEllipsis = endIndex < text.length

        return buildString {
            if (needsLeadingEllipsis) append("...")
            append(window)
            if (actualNeedsTrailingEllipsis) append("...")
        }
    }

    private fun getCharUnit(char: Char): Float =
        when {
            char in 'a'..'z' ||
                char in 'A'..'Z' ||
                char in '0'..'9' ||
                char == ' ' ||
                char in '!'..'/' ||
                char in ':'..'@' ||
                char in '['..'`' ||
                char in '{'..'~' -> 0.5f
            char in '\u4E00'..'\u9FFF' -> 1f
            char in '\u3040'..'\u309F' -> 1f
            char in '\u30A0'..'\u30FF' -> 1f
            char in '\uAC00'..'\uD7AF' -> 1f
            char in '\uFF00'..'\uFFEF' -> 1f
            else -> 1f
        }
}
