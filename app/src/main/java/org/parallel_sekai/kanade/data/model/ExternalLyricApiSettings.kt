package org.parallel_sekai.kanade.data.model

/**
 * 外部歌词 API 设置（用于 LyricGetter 和 SuperLyric）
 */
data class ExternalLyricApiSettings(
    /** 是否启用该 API 的歌词发送 */
    val enabled: Boolean = true,
    /** 是否启用滚动截取长文本 */
    val scrollingTruncateEnabled: Boolean = false,
    /** 最大显示单位数（用于截取） */
    val maxDisplayUnits: Int = 40,
    /** 是否启用智能单位计算（中日韩/全角=1，ASCII/半角=0.5） */
    val smartUnitsEnabled: Boolean = true,
    /** 是否显示时刻 */
    val showTimestamp: Boolean = false,
    /** 允许显示的设备状态：0=解锁，1=亮屏未解锁，2=熄屏 */
    val displayStates: Set<Int> = setOf(0, 1, 2),
    /** 暂停时是否自动清除歌词 */
    val clearOnPause: Boolean = true,
)
