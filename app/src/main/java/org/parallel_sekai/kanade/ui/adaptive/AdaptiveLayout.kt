package org.parallel_sekai.kanade.ui.adaptive

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class AdaptiveLayoutInfo(
    val screenWidthDp: Int,
    val isWideScreen: Boolean,
    val navigationRailWidth: Dp,
    val screenHorizontalPadding: Dp,
    val contentMaxWidth: Dp,
    val sidebarWidth: Dp,
)

@Composable
fun rememberAdaptiveLayoutInfo(): AdaptiveLayoutInfo {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val isWideScreen = screenWidthDp >= 840
    val contentMaxWidth =
        when {
            screenWidthDp >= 1400 -> 1280.dp
            screenWidthDp >= 1200 -> 1120.dp
            screenWidthDp >= 840 -> 1000.dp
            else -> 640.dp
        }

    return AdaptiveLayoutInfo(
        screenWidthDp = screenWidthDp,
        isWideScreen = isWideScreen,
        navigationRailWidth = if (isWideScreen) 92.dp else 0.dp,
        screenHorizontalPadding = if (isWideScreen) 20.dp else 0.dp,
        contentMaxWidth = contentMaxWidth,
        sidebarWidth = if (screenWidthDp >= 1200) 280.dp else 240.dp,
    )
}

fun Modifier.adaptiveContentWidth(layoutInfo: AdaptiveLayoutInfo): Modifier =
    fillMaxWidth().widthIn(max = layoutInfo.contentMaxWidth)
