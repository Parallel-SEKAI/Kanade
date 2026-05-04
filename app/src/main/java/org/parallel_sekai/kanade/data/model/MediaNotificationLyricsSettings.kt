package org.parallel_sekai.kanade.data.model

/**
 * 媒体通知歌词设置
 */
data class MediaNotificationLyricsSettings(
    /** 是否启用媒体通知歌词 */
    val enabled: Boolean = false,
    /** 显示模式：0=模式A（title=原文，artist=歌曲名+艺术家），1=模式B（title=原文，artist=翻译） */
    val mode: Int = 0,
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
    /** 暂停时是否还原通知 */
    val restoreOnPause: Boolean = true,
    /** 翻译最大显示单位数（用于截取翻译文本） */
    val translationMaxDisplayUnits: Int = 40,
)
