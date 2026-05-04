package org.parallel_sekai.kanade.data.utils

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 管理设备显示状态（解锁/亮屏未解锁/熄屏）
 *
 * 状态定义：
 * - 0: 解锁（屏幕亮且未锁定）
 * - 1: 亮屏未解锁（屏幕亮但锁定）
 * - 2: 熄屏
 */
class DeviceDisplayStateManager(
    context: Context,
) {
    companion object {
        const val STATE_UNLOCKED = 0
        const val STATE_LOCKED_SCREEN_ON = 1
        const val STATE_SCREEN_OFF = 2
    }

    private val applicationContext = context.applicationContext
    private val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val keyguardManager = applicationContext.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

    private val _currentStateFlow = MutableStateFlow(getCurrentState())
    val currentStateFlow: StateFlow<Int> = _currentStateFlow.asStateFlow()

    private val screenReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_ON -> {
                        updateState()
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        _currentStateFlow.value = STATE_SCREEN_OFF
                    }
                    Intent.ACTION_USER_PRESENT -> {
                        // 用户解锁
                        _currentStateFlow.value = STATE_UNLOCKED
                    }
                }
            }
        }

    init {
        val filter =
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            }
        applicationContext.registerReceiver(screenReceiver, filter)
    }

    /**
     * 获取当前设备显示状态
     */
    fun getCurrentState(): Int {
        val isScreenOn = powerManager.isInteractive
        val isLocked = keyguardManager.isKeyguardLocked

        return when {
            !isScreenOn -> STATE_SCREEN_OFF
            isScreenOn && !isLocked -> STATE_UNLOCKED
            else -> STATE_LOCKED_SCREEN_ON
        }
    }

    private fun updateState() {
        _currentStateFlow.value = getCurrentState()
    }

    /**
     * 释放资源
     */
    fun release() {
        try {
            applicationContext.unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
            // Receiver may not be registered
        }
    }
}
