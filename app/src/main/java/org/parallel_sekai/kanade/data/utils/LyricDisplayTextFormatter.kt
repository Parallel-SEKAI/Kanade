package org.parallel_sekai.kanade.data.utils

import org.parallel_sekai.kanade.data.model.ExternalLyricApiSettings

/**
 * 歌词显示文本格式化工具
 * 提供自动滚动截取和时刻显示功能
 */
object LyricDisplayTextFormatter {
    /**
     * 格式化文本（支持自动滚动和时刻显示）
     * @param text 原始文本
     * @param settings 外部 API 设置
     * @param positionMs 当前播放进度（毫秒）
     * @param lineStartMs 当前行开始时间（毫秒）
     * @param lineEndOrNextMs 当前行结束时间或下一行开始时间（毫秒）
     * @return 格式化后的文本
     */
    fun formatText(
        text: String,
        settings: ExternalLyricApiSettings,
        positionMs: Long,
        lineStartMs: Long,
        lineEndOrNextMs: Long,
    ): String {
        if (text.isEmpty()) return text

        // 添加时刻前缀
        val textWithTimestamp =
            if (settings.showTimestamp) {
                val timestamp = formatTimestamp(lineStartMs)
                "[$timestamp] $text"
            } else {
                text
            }

        // 如果不启用滚动截取，直接返回
        if (!settings.scrollingTruncateEnabled) {
            return textWithTimestamp
        }

        // 应用滚动截取
        return truncateWithScrolling(
            text = textWithTimestamp,
            maxUnits = settings.maxDisplayUnits.coerceAtLeast(3), // 最小为 3
            smartUnits = settings.smartUnitsEnabled,
            progress = positionMs,
            lineStartTime = lineStartMs,
            lineDuration = (lineEndOrNextMs - lineStartMs).coerceAtLeast(1000L),
        )
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
            units += getCharUnit(char)
        }
        return units
    }

    /**
     * 提取文本窗口
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

    /**
     * 获取单个字符的单位数
     */
    private fun getCharUnit(char: Char): Float =
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
