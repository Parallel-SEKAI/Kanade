@file:Suppress("KtlintStandardMaxLineLength")

package org.parallel_sekai.kanade.ui.screens.player

import android.view.View
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import org.parallel_sekai.kanade.R
import org.parallel_sekai.kanade.data.model.*
import org.parallel_sekai.kanade.data.repository.LyricsSettings
import org.parallel_sekai.kanade.data.utils.*
import org.parallel_sekai.kanade.ui.adaptive.rememberAdaptiveLayoutInfo
import org.parallel_sekai.kanade.ui.theme.Dimens
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

enum class PlayerExpansionValue {
    Collapsed,
    Expanded,
}

private enum class FullScreenSidePanel {
    Lyrics,
    Playlist,
}

/**
 * 跑马灯文本组件 - 当文本超出容器宽度时自动滚动
 */
@Composable
private fun MarqueeText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
) {
    var textWidthPx by remember { mutableStateOf(0f) }
    var containerWidthPx by remember { mutableStateOf(0f) }
    val density = LocalDensity.current

    val shouldScroll = textWidthPx > containerWidthPx && containerWidthPx > 0

    // 计算滚动动画
    val scrollDistance = if (shouldScroll) textWidthPx - containerWidthPx + with(density) { 32.dp.toPx() } else 0f
    val scrollDuration = (scrollDistance / 50f * 1000f).toInt().coerceIn(3000, 10000) // 3-10秒

    val infiniteTransition = rememberInfiniteTransition(label = "Marquee")
    val scrollOffset by infiniteTransition.animateValue(
        initialValue = 0f,
        targetValue = if (shouldScroll) -scrollDistance else 0f,
        typeConverter = Float.VectorConverter,
            animationSpec = infiniteRepeatable(
            animation = tween(scrollDuration, easing = LinearEasing, delayMillis = 1500),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart,
        ),
        label = "MarqueeOffset",
    )

    Box(
        modifier = modifier
            .onSizeChanged { containerWidthPx = it.width.toFloat() }
            .border(0.dp, Color.Transparent) // 强制裁剪
    ) {
        Text(
            text = text,
            style = style,
            color = color,
            fontWeight = fontWeight,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .graphicsLayer {
                    if (shouldScroll) {
                        translationX = scrollOffset
                    }
                }
                .onSizeChanged { textWidthPx = it.width.toFloat() },
        )
    }
}

@Composable
fun FluidBackground(
    colors: List<Color>,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "FluidBackground")

    val offset1 by infiniteTransition.animateValue(
        initialValue = IntOffset(0, 0),
        targetValue = IntOffset(300, 400),
        typeConverter = IntOffset.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "Blob1Offset",
    )

    val offset2 by infiniteTransition.animateValue(
        initialValue = IntOffset(400, 200),
        targetValue = IntOffset(0, 500),
        typeConverter = IntOffset.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "Blob2Offset",
    )

    val offset3 by infiniteTransition.animateValue(
        initialValue = IntOffset(100, 500),
        targetValue = IntOffset(500, 0),
        typeConverter = IntOffset.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "Blob3Offset",
    )

    val color1 by animateColorAsState(colors.getOrElse(0) { Color.DarkGray }, tween(1500))
    val color2 by animateColorAsState(colors.getOrElse(1) { Color.Black }, tween(1500))
    val color3 by animateColorAsState(colors.getOrElse(2) { color1 }, tween(1500))

    Box(modifier = modifier.fillMaxSize().blur(100.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = color2)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color1, Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(offset1.x.toFloat(), offset1.y.toFloat()),
                    radius = size.width,
                    tileMode = TileMode.Clamp,
                ),
                radius = size.width,
                center = androidx.compose.ui.geometry.Offset(offset1.x.toFloat(), offset1.y.toFloat()),
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color3, Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(size.width - offset2.x, offset2.y.toFloat()),
                    radius = size.width * 0.8f,
                    tileMode = TileMode.Clamp,
                ),
                radius = size.width * 0.8f,
                center = androidx.compose.ui.geometry.Offset(size.width - offset2.x, offset2.y.toFloat()),
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color2, Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(offset3.x.toFloat(), size.height - offset3.y),
                    radius = size.width * 1.2f,
                    tileMode = TileMode.Clamp,
                ),
                radius = size.width * 1.2f,
                center = androidx.compose.ui.geometry.Offset(offset3.x.toFloat(), size.height - offset3.y),
            )
        }
    }
}

@Composable
fun KanadePlayerContainer(
    state: PlayerState,
    onIntent: (PlayerIntent) -> Unit,
    onNavigateToSongInfo: () -> Unit = {},
    bottomPadding: Dp = 0.dp,
    contentStartPadding: Dp = 0.dp,
) {
    if (state.currentSong == null) return

    // 屏幕常亮：全屏展开时保持屏幕常亮
    val view = LocalView.current
    DisposableEffect(state.isExpanded) {
        view.keepScreenOn = state.isExpanded
        onDispose {
            view.keepScreenOn = false
        }
    }

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val adaptiveInfo = rememberAdaptiveLayoutInfo()
    var wideSidePanel by remember { mutableStateOf(FullScreenSidePanel.Lyrics) }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    val miniPlayerHeightPx = with(density) { (Dimens.MiniPlayerHeight + Dimens.PaddingMedium).toPx() } // MiniPlayer height + vertical padding
    val bottomPaddingPx = with(density) { bottomPadding.toPx() }

    val collapsedOffset = screenHeightPx - miniPlayerHeightPx - bottomPaddingPx
    val expandedOffset = 0f

    val offsetY = remember { Animatable(if (state.isExpanded) expandedOffset else collapsedOffset) }
    val scope = rememberCoroutineScope()

    // 同步外部状态
    LaunchedEffect(state.isExpanded) {
        if (state.isExpanded && offsetY.value != expandedOffset) {
            offsetY.animateTo(expandedOffset, spring(stiffness = Spring.StiffnessMediumLow))
        } else if (!state.isExpanded && offsetY.value != collapsedOffset) {
            offsetY.animateTo(collapsedOffset, spring(stiffness = Spring.StiffnessMediumLow))
        }
    }

    val fraction = if (collapsedOffset != expandedOffset) {
        ((collapsedOffset - offsetY.value) / (collapsedOffset - expandedOffset)).coerceIn(0f, 1f)
    } else {
        0f
    }

    var predictiveBackProgress by remember { mutableFloatStateOf(0f) }

    PredictiveBackHandler(enabled = state.isExpanded) { progressFlow ->
        try {
            progressFlow.collect { backEvent ->
                predictiveBackProgress = backEvent.progress
            }
            onIntent(PlayerIntent.Collapse)
        } catch (e: Exception) {
            // Cancelled
        } finally {
            predictiveBackProgress = 0f
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 这是一个随 offsetY 移动的包装器，它承载了手势和内容
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = contentStartPadding * (1f - fraction))
                .height(lerp(Dimens.MiniPlayerHeight + Dimens.PaddingMedium, configuration.screenHeightDp.dp, fraction))
                .offset {
                    val backOffset = (predictiveBackProgress * 100.dp.toPx()).roundToInt()
                    IntOffset(0, offsetY.value.roundToInt() + backOffset)
                }
                .graphicsLayer {
                    val scale = 1f - (predictiveBackProgress * 0.05f)
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                }
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            val newOffset = (offsetY.value + delta).coerceIn(expandedOffset, collapsedOffset)
                            offsetY.snapTo(newOffset)
                        }
                    },
                    onDragStopped = { velocity ->
                        val target = when {
                            velocity < -1000f -> expandedOffset // 快速上滑
                            velocity > 1000f -> collapsedOffset // 快速下滑
                            fraction > 0.5f -> expandedOffset // 超过一半向上弹
                            else -> collapsedOffset // 未过一半向下弹
                        }

                        scope.launch {
                            offsetY.animateTo(target, spring(stiffness = Spring.StiffnessMediumLow))
                            if (target == expandedOffset && !state.isExpanded) {
                                onIntent(PlayerIntent.Expand)
                            } else if (target == collapsedOffset && state.isExpanded) {
                                onIntent(PlayerIntent.Collapse)
                            }
                        }
                    },
                ),
        ) {
            val isTransitioning = fraction > 0f

            // 小播放器层
            if (fraction < 0.8f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (isTransitioning) 0f else 1f)
                        .alpha((1f - fraction * 1.25f).coerceIn(0f, 1f)),
                ) {
                    MiniPlayerContent(
                        state = state,
                        onIntent = onIntent,
                        showContent = fraction <= 0f,
                    )
                }
            }

            // 全屏播放器层
            if (fraction > 0f) {
                val fullPlayerEntryAlpha = FastOutSlowInEasing.transform((fraction / 0.12f).coerceIn(0f, 1f))
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(if (isTransitioning) 1f else 0f)
                        .alpha(fullPlayerEntryAlpha),
                ) {
                    if (adaptiveInfo.isWideScreen) {
                        WideFullScreenPlayerContent(
                            state = state,
                            onIntent = onIntent,
                            onNavigateToSongInfo = onNavigateToSongInfo,
                            sidePanel = wideSidePanel,
                            onSidePanelChange = { wideSidePanel = it },
                            expansionFraction = fraction,
                        )
                    } else {
                        FullScreenContent(
                            state = state,
                            onIntent = onIntent,
                            onNavigateToSongInfo = onNavigateToSongInfo,
                            expansionFraction = fraction,
                            offsetY = 0f,
                        )
                    }
                }
            }
        }

        if (state.showLyricShare) {
            LyricShareBottomSheet(state, onIntent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricShareBottomSheet(
    state: PlayerState,
    onIntent: (PlayerIntent) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { onIntent(PlayerIntent.CloseLyricShare) },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = Dimens.PaddingSmall,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.8f)
                .fillMaxWidth()
                .padding(horizontal = Dimens.PaddingLarge),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.title_share_lyrics),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )

                Row {
                    TextButton(onClick = { onIntent(PlayerIntent.SaveLyricImage) }) {
                        Text(stringResource(R.string.action_save))
                    }
                    Button(
                        onClick = { onIntent(PlayerIntent.ShareLyricImage) },
                        shape = RoundedCornerShape(Dimens.CornerRadiusMedium),
                    ) {
                        Text(stringResource(R.string.action_share))
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingMedium))

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = Dimens.PaddingLarge),
            ) {
                itemsIndexed(state.lyricData?.lines ?: emptyList()) { index, line ->
                    LyricShareItem(
                        line = line,
                        isSelected = state.selectedLyricIndices.contains(index),
                        onClick = { onIntent(PlayerIntent.ToggleLyricSelection(index)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LyricShareItem(
    line: LyricLine,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent,
        shape = RoundedCornerShape(Dimens.CornerRadiusSmall),
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(Dimens.PaddingSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
            )
            Column(modifier = Modifier.padding(start = Dimens.PaddingSmall)) {
                Text(
                    text = line.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
                if (!line.translation.isNullOrBlank()) {
                    Text(
                        text = line.translation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniPlayerContent(
    state: PlayerState,
    onIntent: (PlayerIntent) -> Unit,
    showContent: Boolean = true,
) {
    val progressFraction = if (state.duration > 0) state.progress.toFloat() / state.duration else 0f
    val playedColor = MaterialTheme.colorScheme.primaryContainer

    Surface(
        onClick = { onIntent(PlayerIntent.Expand) },
        modifier = Modifier
            .padding(horizontal = Dimens.PaddingSmall, vertical = Dimens.PaddingSmall)
            .fillMaxWidth()
            .height(Dimens.MiniPlayerHeight),
        shape = RoundedCornerShape(Dimens.CornerRadiusLarge),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(
                        color = playedColor,
                        size = Size(width = size.width * progressFraction, height = size.height),
                    )
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showContent) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val imageModel = coil.request.ImageRequest.Builder(context)
                    .data(state.currentSong?.coverUrl)
                    .crossfade(true)
                    .build()
                AsyncImage(
                    model = imageModel,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(Dimens.PaddingSmall)
                        .size(Dimens.AlbumCoverSizeMiniPlayer)
                        .clip(RoundedCornerShape(Dimens.CornerRadiusSmall)),
                    contentScale = ContentScale.Crop,
                )
                Column(modifier = Modifier.weight(1f).padding(start = Dimens.PaddingExtraSmall)) {
                    Text(
                        text = state.currentSong?.title ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                    Text(
                        text = state.currentSong?.artists?.joinToString(state.artistJoinString) ?: "",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = { onIntent(PlayerIntent.PlayPause) }) {
                    Icon(imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                }
                IconButton(onClick = { onIntent(PlayerIntent.Next) }) {
                    Icon(imageVector = Icons.Default.SkipNext, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun FullScreenContent(
    state: PlayerState,
    onIntent: (PlayerIntent) -> Unit,
    onNavigateToSongInfo: () -> Unit,
    expansionFraction: Float,
    offsetY: Float,
) {
    val adaptiveInfo = rememberAdaptiveLayoutInfo()
    var sidePanel by remember { mutableStateOf(FullScreenSidePanel.Lyrics) }

    var showLyrics by remember { mutableStateOf(false) }
    var showPlaylist by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    val density = LocalDensity.current

    val coverPrimary = state.gradientColors.getOrElse(0) { Color(0xFF3A4659) }
    val coverSecondary = state.gradientColors.getOrElse(1) { Color(0xFF1F2632) }
    val pageBackground = lerp(coverSecondary, Color.Black, 0.84f)
    val buttonBackground = lerp(coverPrimary, pageBackground, 0.74f)
    val buttonPressedBackground = lerp(coverPrimary, pageBackground, 0.46f)
    val primaryTextColor = lerp(coverPrimary, Color.White, 0.88f)
    val secondaryTextColor = lerp(primaryTextColor, pageBackground, 0.18f)
    val tertiaryTextColor = lerp(primaryTextColor, pageBackground, 0.34f)
    val panelIndicatorColor = lerp(coverPrimary, primaryTextColor, 0.55f)
    val playlistRowActiveColor = lerp(coverPrimary, pageBackground, 0.72f)
    val inactiveTrackColor = lerp(coverSecondary, pageBackground, 0.28f)
    val lyricPageBackground = pageBackground
    val lyricPrimaryTextColor = primaryTextColor
    val lyricInactiveColor = lerp(lyricPrimaryTextColor, lyricPageBackground, 0.28f)
    val lyricTranslationColor = lerp(lyricPrimaryTextColor, lyricPageBackground, 0.16f)
    val lyricEmptyColor = lerp(lyricPrimaryTextColor, lyricPageBackground, 0.42f)

    // 新增：歌词模式切换动画进度
    val lyricTransitionFraction by animateFloatAsState(
        targetValue = if (showLyrics || showPlaylist) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "LyricTransition",
    )

    // 仅在显示歌词或列表且正在播放时，5秒后自动隐藏控件
    LaunchedEffect(controlsVisible, state.isPlaying, showLyrics, showPlaylist) {
        if ((showLyrics || showPlaylist) && controlsVisible && state.isPlaying) {
            kotlinx.coroutines.delay(5000)
            controlsVisible = false
        }
        // 如果切回封面模式，确保控件立即显示
        if (!showLyrics && !showPlaylist) {
            controlsVisible = true
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                // 仅在歌词或列表模式下允许手动切换控件可见性
                if (showLyrics || showPlaylist) {
                    controlsVisible = !controlsVisible
                } else {
                    controlsVisible = true
                }
            },
        color = Color.Transparent,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight

            // 手动管理黑色背景的透明度，实现渐变变暗效果
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = expansionFraction)))

            // 1. 定义三种状态下的基础值

            // 小播放器 (Mini) - 竖屏和宽屏共用
            val miniArtSize = Dimens.AlbumCoverSizeMiniPlayer
            val miniArtX = Dimens.PaddingMedium
            val miniArtY = Dimens.PaddingMedium
            val miniTitleSize = 14.sp
            val miniArtistSize = 12.sp
            val miniTextX = 72.dp

            val isWide = adaptiveInfo.isWideScreen
            val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val navigationBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

            // 宽屏最终布局的真实几何，与 WideFullScreenPlayerContent 保持一致
            val wideHorizontalPadding = 40.dp
            val wideVerticalPadding = 28.dp
            val wideColumnSpacing = 32.dp
            val widePanelMaxWidth = 460.dp
            val wideHeaderButtonSize = 44.dp
            val wideTopInset = statusBarTop + wideVerticalPadding
            val wideAvailableWidth = (screenWidth - wideHorizontalPadding * 2 - wideColumnSpacing).coerceAtLeast(0.dp)
            val wideOuterColumnWidth = wideAvailableWidth / 2
            val widePanelWidth = minOf(wideOuterColumnWidth, widePanelMaxWidth)
            val widePanelStartX = wideHorizontalPadding + ((wideOuterColumnWidth - widePanelWidth) / 2)
            val wideAvailableHeight = (screenHeight - statusBarTop - navigationBarBottom - wideVerticalPadding * 2).coerceAtLeast(0.dp)
            val wideCoverArtSize = minOf(widePanelWidth * 0.8f, wideAvailableHeight * 0.4f)
            val wideCoverArtX = widePanelStartX + ((widePanelWidth - wideCoverArtSize) / 2)
            val wideCoverArtY = wideTopInset + wideHeaderButtonSize + 20.dp
            val wideTextX = widePanelStartX
            val wideTitleY = wideCoverArtY + wideCoverArtSize + 20.dp
            val wideMoreX = widePanelStartX + widePanelWidth - wideHeaderButtonSize
            val wideMoreY = wideTopInset
            val wideTextWidth = widePanelWidth

            // 全屏封面模式 (Full Cover)
            val fullCoverArtSize = if (isWide) wideCoverArtSize else screenWidth * 0.85f
            val fullCoverArtX = if (isWide) wideCoverArtX else (screenWidth - fullCoverArtSize) / 2
            val fullCoverArtY = if (isWide) wideCoverArtY else 140.dp
            val fullCoverTitleSize = 24.sp
            val fullCoverArtistSize = if (isWide) 16.sp else 20.sp
            val fullCoverTitleY = if (isWide) wideTitleY else screenHeight - 380.dp
            val fullCoverArtistY = if (isWide) wideTitleY + 32.dp else screenHeight - 340.dp
            val fullCoverTextX = if (isWide) wideTextX else fullCoverArtX
            val fullCoverTextWidth = if (isWide) wideTextWidth else fullCoverArtSize - 48.dp
            val fullCoverMoreX = if (isWide) wideMoreX else screenWidth - fullCoverArtX - Dimens.PaddingExtraLarge
            val fullCoverMoreY = if (isWide) wideMoreY else fullCoverTitleY + Dimens.PaddingExtraSmall

            // 全屏歌词/列表模式 (Full Lyric/Playlist)
            // 宽屏下左侧布局不会切成竖屏歌词小头图，所以直接复用宽屏封面布局坐标
            val fullLyricArtSize = if (isWide) fullCoverArtSize else Dimens.IconSizeHuge
            val fullLyricArtX = if (isWide) fullCoverArtX else Dimens.PaddingLarge
            val fullLyricArtY = if (isWide) fullCoverArtY else 66.dp
            val fullLyricTitleSize = if (isWide) fullCoverTitleSize else 16.sp
            val fullLyricArtistSize = if (isWide) fullCoverArtistSize else 14.sp
            val fullLyricTitleY = if (isWide) fullCoverTitleY else 64.dp
            val fullLyricArtistY = if (isWide) fullCoverArtistY else 88.dp
            val fullLyricTextX = if (isWide) fullCoverTextX else 76.dp
            val fullLyricTextWidth = if (isWide) fullCoverTextWidth else screenWidth - 160.dp
            val fullLyricMoreX = if (isWide) fullCoverMoreX else screenWidth - Dimens.AlbumCoverSizeMiniPlayer
            val fullLyricMoreY = if (isWide) fullCoverMoreY else 72.dp

            // 2. 首先在全屏的两个模式之间插值，得到“当前全屏目标”
            val fullTargetArtSize = lerp(fullCoverArtSize, fullLyricArtSize, lyricTransitionFraction)
            val fullTargetArtX = lerp(fullCoverArtX, fullLyricArtX, lyricTransitionFraction)
            val fullTargetArtY = lerp(fullCoverArtY, fullLyricArtY, lyricTransitionFraction)
            val fullTargetArtCornerRadius = lerp(24.dp, 4.dp, lyricTransitionFraction)

            val fullTargetTitleSize = lerp(fullCoverTitleSize, fullLyricTitleSize, lyricTransitionFraction)
            val fullTargetArtistSize = lerp(fullCoverArtistSize, fullLyricArtistSize, lyricTransitionFraction)
            val fullTargetTitleY = lerp(fullCoverTitleY, fullLyricTitleY, lyricTransitionFraction)
            val fullTargetArtistY = lerp(fullCoverArtistY, fullLyricArtistY, lyricTransitionFraction)
            val fullTargetTextX = lerp(fullCoverTextX, fullLyricTextX, lyricTransitionFraction)
            val fullTargetTextWidth = lerp(fullCoverTextWidth, fullLyricTextWidth, lyricTransitionFraction)

            // 三点按钮位置插值
            val fullTargetMoreX = lerp(fullCoverMoreX, fullLyricMoreX, lyricTransitionFraction)
            val fullTargetMoreY = lerp(fullCoverMoreY, fullLyricMoreY, lyricTransitionFraction)

            // 3. 最后根据 expansionFraction 在 Mini 和 “当前全屏目标” 之间插值
            val currentArtSize = lerp(miniArtSize, fullTargetArtSize, expansionFraction)
            val currentArtX = lerp(miniArtX, fullTargetArtX, expansionFraction)
            val currentArtY = lerp(miniArtY, fullTargetArtY, expansionFraction)
            val currentArtCornerRadius = lerp(Dimens.CornerRadiusSmall, fullTargetArtCornerRadius, expansionFraction)

            val currentTitleSize = lerp(miniTitleSize, fullTargetTitleSize, expansionFraction)
            val currentArtistSize = lerp(miniArtistSize, fullTargetArtistSize, expansionFraction)
            val currentTitleY = lerp(miniArtY + Dimens.PaddingSmall, fullTargetTitleY, expansionFraction)
            val currentArtistY = lerp(miniArtY + Dimens.PaddingExtraLarge, fullTargetArtistY, expansionFraction)
            val currentTextX = lerp(miniTextX, fullTargetTextX, expansionFraction)

            val miniTextWidth = screenWidth - miniTextX - Dimens.IconSizeGigantic // Mini player right padding for controls (100.dp)
            val currentTextWidth = lerp(miniTextWidth, fullTargetTextWidth, expansionFraction)

            val currentMoreX = lerp(screenWidth - Dimens.AlbumCoverSizeMiniPlayer, fullTargetMoreX, expansionFraction)
            val currentMoreY = lerp(miniArtY + Dimens.PaddingLarge, fullTargetMoreY, expansionFraction)

            val miniArtistColor = MaterialTheme.colorScheme.onSurfaceVariant
            val fullArtistColor = Color.White.copy(alpha = 0.78f)
            val currentArtistColor = lerp(miniArtistColor, fullArtistColor, expansionFraction)

            val controlsAlpha by animateFloatAsState(
                targetValue = if (controlsVisible) 1f else 0f,
                animationSpec = tween(500),
                label = "ControlsAlpha",
            )

            FluidBackground(
                colors = state.gradientColors,
                modifier = Modifier.fillMaxSize().alpha(0.8f * expansionFraction),
            )
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f * expansionFraction)))

            AsyncImage(
                model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(state.currentSong?.coverUrl)
                    .crossfade(true)
                    .size(coil.size.Size.ORIGINAL)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            currentArtX.roundToPx(),
                            currentArtY.roundToPx(),
                        )
                    }
                    .size(currentArtSize)
                    .clip(RoundedCornerShape(currentArtCornerRadius))
                    .background(Color.DarkGray),
                contentScale = ContentScale.Crop,
            )

            Column(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            currentTextX.roundToPx(),
                            currentTitleY.roundToPx(),
                        )
                    }
                    .width(currentTextWidth)
                    .alpha(if (showLyrics || showPlaylist || expansionFraction < 1f) 1f else controlsAlpha),
            ) {
                Text(
                    text = state.currentSong?.title ?: "",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = currentTitleSize,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    ),
                    maxLines = 2, // Allow 2 lines for title to prevent overflow if too long
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )

                Text(
                    text = state.currentSong?.artists?.joinToString(state.artistJoinString) ?: "",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = currentArtistSize,
                        color = currentArtistColor,
                    ),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }

            // Apple Music 风格的三点按钮
            var showMoreMenu by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .offset { IntOffset(currentMoreX.roundToPx(), currentMoreY.roundToPx()) }
                    .size(Dimens.PaddingExtraLarge) // 32.dp
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f * expansionFraction))
                    .clickable { showMoreMenu = true }
                    .alpha(if (showLyrics || showPlaylist || expansionFraction < 1f) 1f else controlsAlpha),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(Dimens.IconSizeMedium), // 20.dp
                )

                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.title_song_info)) },
                        onClick = {
                            showMoreMenu = false
                            onNavigateToSongInfo()
                        },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .alpha(expansionFraction),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 顶部拉动指示器
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(88.dp) // Keep as is for now, as it's a specific UI element
                        .padding(horizontal = Dimens.PaddingLarge),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = controlsVisible,
                        enter = fadeIn(tween(500)),
                        exit = fadeOut(tween(500)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = Dimens.CornerRadiusLarge), // 12.dp
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                            modifier = Modifier
                                .size(Dimens.IconSizeExtraLarge, Dimens.PaddingExtraSmall)
                                .clip(RoundedCornerShape(Dimens.CornerRadiusSmall))
                                .background(primaryTextColor.copy(alpha = 0.3f))
                                .align(Alignment.TopCenter),
                        ) // 36.dp, 4.dp, 2.dp
                        }
                    }
                }

                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showLyrics && expansionFraction > 0.9f,
                        enter = fadeIn(tween(500)) + expandVertically(expandFrom = Alignment.CenterVertically),
                        exit = fadeOut(tween(500)) + shrinkVertically(shrinkTowards = Alignment.CenterVertically),
                    ) {
                        LyricContent(
                            state = state,
                            onIntent = onIntent,
                            activeTextColor = lyricPrimaryTextColor,
                            inactiveTextColor = lyricInactiveColor,
                            translationTextColor = lyricTranslationColor,
                            emptyTextColor = lyricEmptyColor,
                            fadeMaskColor = lyricPageBackground,
                            glowColor = lyricPrimaryTextColor,
                        )
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = showPlaylist && expansionFraction > 0.9f,
                        enter = fadeIn(tween(500)) + expandVertically(expandFrom = Alignment.CenterVertically),
                        exit = fadeOut(tween(500)) + shrinkVertically(shrinkTowards = Alignment.CenterVertically),
                    ) {
                        PlaylistContent(state, onIntent)
                    }

                    if (!showLyrics && !showPlaylist) {
                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        currentArtX.roundToPx(),
                                        currentArtY.roundToPx(),
                                    )
                                }
                                .size(currentArtSize)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    showLyrics = true
                                    controlsVisible = true
                                },
                        )
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = controlsVisible && expansionFraction > 0.8f,
                    enter = fadeIn(tween(500)) + expandVertically(expandFrom = Alignment.Bottom),
                    exit = fadeOut(tween(500)) + shrinkVertically(shrinkTowards = Alignment.Bottom),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Dimens.PaddingLarge)
                            .padding(horizontal = Dimens.PaddingLarge),
                    ) {
                        val dynamicSpacerHeightValue by animateFloatAsState(
                            targetValue = if (showLyrics || showPlaylist) 0f else 160f, // Keep 160f as it is a specific layout value
                            animationSpec = tween(500, easing = FastOutSlowInEasing),
                            label = "LyricAreaHeightAnimation",
                        )
                        Spacer(modifier = Modifier.height(dynamicSpacerHeightValue.dp))
                        PlayerControlsSection(
                            state = state,
                            onIntent = onIntent,
                            showLyrics = showLyrics,
                            showPlaylist = showPlaylist,
                            onToggleLyrics = {
                                showLyrics = it
                                if (it) showPlaylist = false
                                controlsVisible = true
                            },
                            onTogglePlaylist = {
                                showPlaylist = it
                                if (it) showLyrics = false
                                controlsVisible = true
                            },
                            textColor = primaryTextColor,
                            secondaryTextColor = secondaryTextColor,
                            tertiaryTextColor = tertiaryTextColor,
                            buttonBackground = buttonBackground,
                            buttonPressedBackground = buttonPressedBackground,
                            inactiveTrackColor = inactiveTrackColor,
                        )
                    }
                }

                val bottomSafeMarginValue by animateFloatAsState(
                    targetValue = if (controlsVisible) 0f else Dimens.PaddingLarge.value, // 24.dp
                    animationSpec = tween(500),
                    label = "BottomSafeMarginAnimation",
                )
            Spacer(modifier = Modifier.height(bottomSafeMarginValue.dp))
            }
        }

    }
}



@Composable
private fun WideFullScreenPlayerContent(
    state: PlayerState,
    onIntent: (PlayerIntent) -> Unit,
    onNavigateToSongInfo: () -> Unit,
    sidePanel: FullScreenSidePanel,
    onSidePanelChange: (FullScreenSidePanel) -> Unit,
    expansionFraction: Float = 1f,
) {
    val currentSong = state.currentSong ?: return
    var showMoreMenu by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val coverModel = remember(context, currentSong.coverUrl) {
        coil.request.ImageRequest.Builder(context)
            .data(currentSong.coverUrl)
            .crossfade(false)
            .size(coil.size.Size.ORIGINAL)
            .build()
    }
    val showCoverPlaceholder = currentSong.coverUrl.isNullOrBlank()

    val coverPrimary = state.gradientColors.getOrElse(0) { Color(0xFF3A4659) }
    val coverSecondary = state.gradientColors.getOrElse(1) { Color(0xFF1F2632) }
    val pageBackground = lerp(coverSecondary, Color.Black, 0.84f)
    val coverPlaceholder = lerp(coverPrimary, pageBackground, 0.5f)
    val buttonBackground = lerp(coverPrimary, pageBackground, 0.74f)
    val buttonPressedBackground = lerp(coverPrimary, pageBackground, 0.46f)
    val primaryTextColor = lerp(coverPrimary, Color.White, 0.88f)
    val secondaryTextColor = lerp(primaryTextColor, pageBackground, 0.18f)
    val tertiaryTextColor = lerp(primaryTextColor, pageBackground, 0.34f)
    val panelIndicatorColor = lerp(coverPrimary, primaryTextColor, 0.55f)
    val playlistRowActiveColor = lerp(coverPrimary, pageBackground, 0.72f)
    val inactiveTrackColor = lerp(coverSecondary, pageBackground, 0.28f)
    val lyricInactiveColor = lerp(primaryTextColor, pageBackground, 0.28f)
    val lyricTranslationColor = lerp(primaryTextColor, pageBackground, 0.16f)
    val lyricEmptyColor = lerp(primaryTextColor, pageBackground, 0.42f)

    val progress = expansionFraction.coerceIn(0f, 1f)
    val shellAlpha = ((progress - 0.45f) / 0.4f).coerceIn(0f, 1f)
    val staticSharedAlpha = ((progress - 0.94f) / 0.06f).coerceIn(0f, 1f)
    val overlaySharedAlpha = 1f - ((progress - 0.94f) / 0.06f).coerceIn(0f, 1f)

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(pageBackground),
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val navigationBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        val wideHorizontalPadding = 40.dp
        val wideVerticalPadding = 28.dp
        val wideColumnSpacing = 32.dp
        val widePanelMaxWidth = 460.dp
        val wideHeaderButtonSize = 44.dp
        val wideTopInset = statusBarTop + wideVerticalPadding
        val wideAvailableWidth = (screenWidth - wideHorizontalPadding * 2 - wideColumnSpacing).coerceAtLeast(0.dp)
        val wideOuterColumnWidth = wideAvailableWidth / 2
        val widePanelWidth = minOf(wideOuterColumnWidth, widePanelMaxWidth)
        val widePanelStartX = wideHorizontalPadding + ((wideOuterColumnWidth - widePanelWidth) / 2)
        val wideAvailableHeight = (screenHeight - statusBarTop - navigationBarBottom - wideVerticalPadding * 2).coerceAtLeast(0.dp)
        val wideCoverSize = minOf(widePanelWidth * 0.8f, wideAvailableHeight * 0.4f)
        val wideCoverX = widePanelStartX + ((widePanelWidth - wideCoverSize) / 2)
        val wideCoverY = wideTopInset + wideHeaderButtonSize + 20.dp
        val wideTextX = widePanelStartX
        val wideTitleY = wideCoverY + wideCoverSize + 20.dp
        val wideTextWidth = widePanelWidth

        val miniArtSize = Dimens.AlbumCoverSizeMiniPlayer
        val miniArtX = Dimens.PaddingMedium
        val miniArtY = Dimens.PaddingMedium
        val miniTitleSize = 14.sp
        val miniArtistSize = 12.sp
        val miniTextX = 72.dp
        val miniTextWidth = screenWidth - miniTextX - Dimens.IconSizeGigantic

        val currentArtSize = lerp(miniArtSize, wideCoverSize, progress)
        val currentArtX = lerp(miniArtX, wideCoverX, progress)
        val currentArtY = lerp(miniArtY, wideCoverY, progress)
        val currentArtCornerRadius = lerp(Dimens.CornerRadiusSmall, 28.dp, progress)
        val currentTitleSize = lerp(miniTitleSize, 24.sp, progress)
        val currentArtistSize = lerp(miniArtistSize, 16.sp, progress)
        val currentTextX = lerp(miniTextX, wideTextX, progress)
        val currentTitleY = lerp(miniArtY + Dimens.PaddingSmall, wideTitleY, progress)
        val currentTextWidth = lerp(miniTextWidth, wideTextWidth, progress)
        val currentArtistColor = lerp(MaterialTheme.colorScheme.onSurfaceVariant, secondaryTextColor, progress)

        FluidBackground(
            colors = state.gradientColors,
            modifier = Modifier.fillMaxSize().alpha(0.85f * progress),
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f * progress)))

        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 40.dp, vertical = 28.dp)
                    .alpha(shellAlpha),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentAlignment = Alignment.TopCenter,
            ) {
                BoxWithConstraints(
                    modifier = Modifier.widthIn(max = 460.dp).fillMaxHeight(),
                ) {
                    val coverSize = minOf(maxWidth * 0.8f, maxHeight * 0.4f)

                    Column(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppleMusicHeaderButton(
                                icon = Icons.Default.KeyboardArrowDown,
                                onClick = { onIntent(PlayerIntent.Collapse) },
                                backgroundColor = buttonBackground,
                                contentColor = primaryTextColor,
                            )

                            Box {
                                AppleMusicHeaderButton(
                                    icon = Icons.Default.MoreHoriz,
                                    onClick = { showMoreMenu = true },
                                    backgroundColor = buttonBackground,
                                    contentColor = primaryTextColor,
                                )

                                DropdownMenu(
                                    expanded = showMoreMenu,
                                    onDismissRequest = { showMoreMenu = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.title_song_info)) },
                                        onClick = {
                                            showMoreMenu = false
                                            onNavigateToSongInfo()
                                        },
                                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Box(
                            modifier = Modifier.fillMaxWidth().alpha(staticSharedAlpha),
                            contentAlignment = Alignment.Center,
                        ) {
                            AsyncImage(
                                model = coverModel,
                                contentDescription = null,
                                modifier =
                                    Modifier
                                        .size(coverSize)
                                        .clip(RoundedCornerShape(28.dp))
                                        .then(if (showCoverPlaceholder) Modifier.background(coverPlaceholder) else Modifier),
                                contentScale = ContentScale.Crop,
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Column(modifier = Modifier.fillMaxWidth().alpha(staticSharedAlpha)) {
                            MarqueeText(
                                text = currentSong.title,
                                style = MaterialTheme.typography.headlineLarge,
                                color = primaryTextColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            MarqueeText(
                                text = currentSong.artists.joinToString(state.artistJoinString),
                                style = MaterialTheme.typography.titleMedium,
                                color = secondaryTextColor,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            currentSong.album.takeIf { it.isNotBlank() }?.let { album ->
                                Spacer(modifier = Modifier.height(6.dp))
                                MarqueeText(
                                    text = album,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = tertiaryTextColor,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

        AppleMusicTransportControls(
            state = state,
            onIntent = onIntent,
            textColor = primaryTextColor,
            secondaryTextColor = secondaryTextColor,
            primaryButtonBackground = buttonPressedBackground,
            inactiveTrackColor = inactiveTrackColor,
        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            AppleMusicSmallControlButton(
                                icon = Icons.Default.Shuffle,
                                onClick = { onIntent(PlayerIntent.ToggleShuffle) },
                                active = state.shuffleModeEnabled,
                                backgroundColor = buttonBackground,
                                activeBackgroundColor = buttonPressedBackground,
                                contentColor = primaryTextColor,
                            )
                            AppleMusicSmallControlButton(
                                icon = if (state.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                onClick = { onIntent(PlayerIntent.ToggleRepeat) },
                                active = state.repeatMode != RepeatMode.OFF,
                                backgroundColor = buttonBackground,
                                activeBackgroundColor = buttonPressedBackground,
                                contentColor = primaryTextColor,
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
            ) {
                AppleMusicPanelSwitcher(
                    sidePanel = sidePanel,
                    onSidePanelChange = onSidePanelChange,
                    selectedTextColor = primaryTextColor,
                    unselectedTextColor = tertiaryTextColor,
                    indicatorColor = panelIndicatorColor,
                    modifier = Modifier.padding(start = Dimens.LyricHorizontalPadding),
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    when (sidePanel) {
                        FullScreenSidePanel.Lyrics -> {
                            LyricContent(
                                state = state,
                                onIntent = onIntent,
                                modifier = Modifier.fillMaxSize(),
                                topContentPadding = 8.dp,
                                bottomContentPadding = 140.dp,
                                activeTextColor = primaryTextColor,
                                inactiveTextColor = lyricInactiveColor,
                                translationTextColor = lyricTranslationColor,
                                emptyTextColor = lyricEmptyColor,
                                fadeMaskColor = pageBackground,
                                glowColor = primaryTextColor,
                            )
                        }
                        FullScreenSidePanel.Playlist -> {
                            PlaylistContent(
                                state = state,
                                onIntent = onIntent,
                                modifier = Modifier.fillMaxSize(),
                                bottomContentPadding = 24.dp,
                                currentSongHighlightColor = primaryTextColor,
                                primaryTextColor = primaryTextColor,
                                secondaryTextColor = secondaryTextColor,
                                handleTint = tertiaryTextColor,
                                activeRowColor = playlistRowActiveColor,
                                maskColor = pageBackground,
                                showHeader = false,
                                horizontalPadding = 0.dp,
                            )
                        }
                    }
                }
            }
        }

        if (overlaySharedAlpha > 0f) {
            AsyncImage(
                model = coverModel,
                contentDescription = null,
                modifier = Modifier
                    .offset { IntOffset(currentArtX.roundToPx(), currentArtY.roundToPx()) }
                    .size(currentArtSize)
                    .clip(RoundedCornerShape(currentArtCornerRadius))
                    .then(if (showCoverPlaceholder) Modifier.background(coverPlaceholder) else Modifier)
                    .alpha(overlaySharedAlpha),
                contentScale = ContentScale.Crop,
            )

            Column(
                modifier = Modifier
                    .offset { IntOffset(currentTextX.roundToPx(), currentTitleY.roundToPx()) }
                    .width(currentTextWidth)
                    .alpha(overlaySharedAlpha),
            ) {
                Text(
                    text = currentSong.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = currentTitleSize,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentSong.artists.joinToString(state.artistJoinString),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = currentArtistSize,
                        color = currentArtistColor,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AppleMusicHeaderButton(
    icon: ImageVector,
    onClick: () -> Unit,
    backgroundColor: Color,
    contentColor: Color,
) {
    Box(
        modifier =
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(backgroundColor)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(Dimens.IconSizeLarge),
        )
    }
}

@Composable
private fun AppleMusicPanelSwitcher(
    sidePanel: FullScreenSidePanel,
    onSidePanelChange: (FullScreenSidePanel) -> Unit,
    selectedTextColor: Color,
    unselectedTextColor: Color,
    indicatorColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppleMusicPanelSwitchItem(
            label = stringResource(R.string.label_lyrics),
            selected = sidePanel == FullScreenSidePanel.Lyrics,
            onClick = { onSidePanelChange(FullScreenSidePanel.Lyrics) },
            selectedTextColor = selectedTextColor,
            unselectedTextColor = unselectedTextColor,
            indicatorColor = indicatorColor,
        )
        AppleMusicPanelSwitchItem(
            label = stringResource(R.string.header_playing_next),
            selected = sidePanel == FullScreenSidePanel.Playlist,
            onClick = { onSidePanelChange(FullScreenSidePanel.Playlist) },
            selectedTextColor = selectedTextColor,
            unselectedTextColor = unselectedTextColor,
            indicatorColor = indicatorColor,
        )
    }
}

@Composable
private fun AppleMusicPanelSwitchItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    selectedTextColor: Color,
    unselectedTextColor: Color,
    indicatorColor: Color,
) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = label,
            color = if (selected) selectedTextColor else unselectedTextColor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier =
                Modifier
                    .height(2.dp)
                    .width(if (selected) 34.dp else 0.dp)
                    .background(indicatorColor),
        )
    }
}

@Composable
private fun AppleMusicTransportControls(
    state: PlayerState,
    onIntent: (PlayerIntent) -> Unit,
    textColor: Color,
    secondaryTextColor: Color,
    primaryButtonBackground: Color,
    inactiveTrackColor: Color,
) {
    var sliderPosition by remember { mutableFloatStateOf(state.progress.toFloat()) }
    val isDragging = remember { mutableStateOf(false) }

    LaunchedEffect(state.progress) {
        if (!isDragging.value) sliderPosition = state.progress.toFloat()
    }

    Slider(
        value = sliderPosition,
        onValueChange = {
            isDragging.value = true
            sliderPosition = it
        },
        onValueChangeFinished = {
            isDragging.value = false
            onIntent(PlayerIntent.SeekTo(sliderPosition.toLong()))
        },
        valueRange = 0f..state.duration.toFloat().coerceAtLeast(1f),
        modifier = Modifier.fillMaxWidth(),
        colors = SliderDefaults.colors(
            thumbColor = textColor,
            activeTrackColor = textColor,
            inactiveTrackColor = Color(0xFF2A3039),
        ),
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = formatTime(state.progress),
            style = MaterialTheme.typography.labelMedium,
            color = secondaryTextColor,
        )
        Text(
            text = "-" + formatTime((state.duration - state.progress).coerceAtLeast(0L)),
            style = MaterialTheme.typography.labelMedium,
            color = secondaryTextColor,
        )
    }

    Spacer(modifier = Modifier.height(28.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onIntent(PlayerIntent.Previous) }) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(42.dp),
            )
        }
        Box(
            modifier =
                Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(primaryButtonBackground)
                    .clickable { onIntent(PlayerIntent.PlayPause) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(38.dp),
            )
        }
        IconButton(onClick = { onIntent(PlayerIntent.Next) }) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(42.dp),
            )
        }
    }
}

@Composable
private fun AppleMusicSmallControlButton(
    icon: ImageVector,
    onClick: () -> Unit,
    active: Boolean,
    backgroundColor: Color,
    activeBackgroundColor: Color,
    contentColor: Color,
) {
    Box(
        modifier =
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (active) activeBackgroundColor else backgroundColor)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(Dimens.IconSizeMedium),
        )
    }
}

// 辅助插值函数
private fun lerp(start: Dp, stop: Dp, fraction: Float): Dp = start + (stop - start) * fraction

private fun lerp(start: TextUnit, stop: TextUnit, fraction: Float): TextUnit = (start.value + (stop.value - start.value) * fraction).sp

private fun lerp(start: Color, stop: Color, fraction: Float): Color = androidx.compose.ui.graphics.lerp(start, stop, fraction)

@Composable
private fun PlayerControlsSection(
    state: PlayerState,
    onIntent: (PlayerIntent) -> Unit,
    showLyrics: Boolean,
    showPlaylist: Boolean,
    onToggleLyrics: (Boolean) -> Unit,
    onTogglePlaylist: (Boolean) -> Unit,
    textColor: Color,
    secondaryTextColor: Color,
    tertiaryTextColor: Color,
    buttonBackground: Color,
    buttonPressedBackground: Color,
    inactiveTrackColor: Color,
    showPanelToggleButtons: Boolean = true,
) {
    AppleMusicTransportControls(
        state = state,
        onIntent = onIntent,
        textColor = textColor,
        secondaryTextColor = secondaryTextColor,
        primaryButtonBackground = buttonPressedBackground,
        inactiveTrackColor = inactiveTrackColor,
    )

    if (showPanelToggleButtons) {
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppleMusicSmallControlButton(
                icon = Icons.Default.Shuffle,
                onClick = { onIntent(PlayerIntent.ToggleShuffle) },
                active = state.shuffleModeEnabled,
                backgroundColor = buttonBackground,
                activeBackgroundColor = buttonPressedBackground,
                contentColor = textColor,
            )
            AppleMusicSmallControlButton(
                icon = Icons.Default.Lyrics,
                onClick = { onToggleLyrics(!showLyrics) },
                active = showLyrics,
                backgroundColor = buttonBackground,
                activeBackgroundColor = buttonPressedBackground,
                contentColor = textColor,
            )
            AppleMusicSmallControlButton(
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                onClick = { onTogglePlaylist(!showPlaylist) },
                active = showPlaylist,
                backgroundColor = buttonBackground,
                activeBackgroundColor = buttonPressedBackground,
                contentColor = textColor,
            )
            AppleMusicSmallControlButton(
                icon = if (state.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                onClick = { onIntent(PlayerIntent.ToggleRepeat) },
                active = state.repeatMode != RepeatMode.OFF,
                backgroundColor = buttonBackground,
                activeBackgroundColor = buttonPressedBackground,
                contentColor = textColor,
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = when {
                showLyrics -> stringResource(R.string.label_lyrics)
                showPlaylist -> stringResource(R.string.label_playlists)
                else -> ""
            },
            color = tertiaryTextColor,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}

private fun formatTime(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return String.format("%d:%02d", minutes, seconds)
}

@Composable
fun PlaylistContent(
    state: PlayerState,
    onIntent: (PlayerIntent) -> Unit,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 100.dp,
    currentSongHighlightColor: Color = MaterialTheme.colorScheme.primary,
    primaryTextColor: Color = Color.White,
    secondaryTextColor: Color = Color.White.copy(alpha = 0.6f),
    handleTint: Color = Color.White.copy(alpha = 0.3f),
    activeRowColor: Color = Color.White.copy(alpha = 0.1f),
    maskColor: Color = Color.Black,
    showHeader: Boolean = true,
    horizontalPadding: Dp = Dimens.PaddingLarge,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.currentSong) {
        val index = state.currentPlaylist.indexOf(state.currentSong)
        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding),
        ) {
            Spacer(modifier = Modifier.height(Dimens.SpacingSmall))

            if (showHeader) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Dimens.PaddingMedium),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.header_playing_next),
                        style = MaterialTheme.typography.titleMedium,
                        color = primaryTextColor,
                        fontWeight = FontWeight.Bold,
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlaylistModeButton(
                            icon = Icons.Default.Shuffle,
                            isActive = state.shuffleModeEnabled,
                            onClick = { onIntent(PlayerIntent.ToggleShuffle) },
                            contentDescription = stringResource(R.string.desc_shuffle),
                        )

                        Spacer(modifier = Modifier.width(Dimens.SpacingExtraSmall))

                        PlaylistModeButton(
                            icon = when (state.repeatMode) {
                                RepeatMode.ONE -> Icons.Default.RepeatOne
                                else -> Icons.Default.Repeat
                            },
                            isActive = state.repeatMode != RepeatMode.OFF,
                            onClick = { onIntent(PlayerIntent.ToggleRepeat) },
                            contentDescription = stringResource(R.string.desc_repeat),
                        )
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .drawWithContent {
                        drawContent()
                        val fadingBrush = Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.05f to maskColor,
                            0.95f to maskColor,
                            1f to Color.Transparent,
                        )
                        drawRect(brush = fadingBrush, blendMode = BlendMode.DstIn)
                    },
                contentPadding = PaddingValues(bottom = bottomContentPadding),
            ) {
                itemsIndexed(state.currentPlaylist) { index, music ->
                    val isCurrent = music.id == state.currentSong?.id

                    Surface(
                        onClick = { onIntent(PlayerIntent.SelectSong(music)) },
                        color = if (isCurrent) activeRowColor else Color.Transparent,
                        shape = RoundedCornerShape(Dimens.PaddingSmall),
                        modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.PaddingExtraSmall),
                    ) {
                        Row(
                            modifier = Modifier.padding(Dimens.PaddingSmall),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AsyncImage(
                                model = music.coverUrl,
                                contentDescription = null,
                                modifier = Modifier.size(Dimens.AlbumCoverSizeMiniPlayer).clip(RoundedCornerShape(Dimens.CornerRadiusSmall)),
                                contentScale = ContentScale.Crop,
                            )
                            Column(modifier = Modifier.weight(1f).padding(horizontal = Dimens.CornerRadiusLarge)) {
                                // 12.dp
                                Text(
                                    text = music.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isCurrent) currentSongHighlightColor else Color.White,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = music.artists.joinToString(state.artistJoinString),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = secondaryTextColor,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = null,
                                tint = handleTint,
                                modifier = Modifier.size(Dimens.IconSizeMedium),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KaraokeWord(
    text: String,
    startTime: Long,
    endTime: Long,
    currentProgress: Long,
    isActiveLine: Boolean,
    textStyle: androidx.compose.ui.text.TextStyle,
    inactiveColor: Color = Color.White.copy(alpha = 0.6f),
    activeColor: Color = Color.White,
    glowColor: Color = Color.White,
    modifier: Modifier = Modifier,
) {
    val duration = endTime - startTime
    val shouldGlow = duration > 500

    val fillProgress = remember(currentProgress, startTime, endTime) {
        if (currentProgress >= endTime) {
            1f
        } else if (currentProgress < startTime) {
            0f
        } else {
            (currentProgress - startTime).toFloat() / (endTime - startTime).coerceAtLeast(1).toFloat()
        }
    }.coerceIn(0f, 1f)

    val isWordActive = currentProgress in startTime until endTime
    val wordScale by animateFloatAsState(
        targetValue = if (isActiveLine && isWordActive) 1.05f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "WordScale",
    )

    val glowRadius by animateFloatAsState(
        targetValue = if (isActiveLine && isWordActive && shouldGlow) 20f else 0f,
        animationSpec = tween(300),
        label = "GlowRadius",
    )

    val baseAlpha = if (isActiveLine) 0.35f else 1f
    val highlightOverflow = if (shouldGlow) glowRadius.coerceAtLeast(8f) else 0f

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = wordScale
                scaleY = wordScale
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.5f)
                clip = false
            },
    ) {
        Text(
            text = text,
            style = textStyle,
            color = inactiveColor.copy(alpha = baseAlpha),
            softWrap = false,
        )

        if (isActiveLine && fillProgress > 0f) {
            val shadow = if (shouldGlow && glowRadius > 0f) {
                Shadow(
                    color = glowColor.copy(alpha = (glowRadius / 20f) * 0.7f),
                    blurRadius = glowRadius,
                    offset = Offset.Zero,
                )
            } else {
                null
            }

            Text(
                text = text,
                style = textStyle.copy(shadow = shadow),
                color = activeColor,
                softWrap = false,
                modifier = Modifier.graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    clip = false
                }.drawWithContent {
                    clipRect(
                        left = -highlightOverflow,
                        top = -highlightOverflow,
                        right = size.width * fillProgress + highlightOverflow,
                        bottom = size.height + highlightOverflow,
                    ) {
                        this@drawWithContent.drawContent()
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordByWordLine(
    words: List<WordInfo>,
    currentProgress: Long,
    isActive: Boolean,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    inactiveColor: Color = Color.White.copy(alpha = 0.6f),
    activeColor: Color = Color.White,
    glowColor: Color = Color.White,
) {
    val horizontalArrangement = when (textStyle.textAlign) {
        TextAlign.Center -> Arrangement.Center
        TextAlign.End -> Arrangement.End
        else -> Arrangement.Start
    }

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = horizontalArrangement,
    ) {
        words.forEach { word ->
            KaraokeWord(
                text = word.text,
                startTime = word.startTime,
                endTime = word.endTime,
                currentProgress = currentProgress,
                isActiveLine = isActive,
                textStyle = textStyle,
            )
        }
    }
}

@Composable
fun BalancedLyricView(
    line: LyricLine,
    currentProgress: Long,
    isActive: Boolean,
    baseTextStyle: TextStyle,
    translationTextStyle: TextStyle,
    settings: LyricsSettings,
    textAlign: TextAlign,
    horizontalAlignment: Alignment.Horizontal,
    modifier: Modifier = Modifier,
    inactiveColor: Color = Color.White.copy(alpha = 0.6f),
    activeColor: Color = Color.White,
    glowColor: Color = Color.White,
    translationColor: Color = Color.White.copy(alpha = 0.8f),
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier) {
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val horizontalPaddingPx = with(density) { (Dimens.LyricHorizontalPadding * 2).toPx() } // 32.dp * 2
        val availableWidthPx = (maxWidthPx - horizontalPaddingPx).coerceAtLeast(0f)

        // 缓存计算结果
        val splitResult = remember(line.content, availableWidthPx, settings.balanceLines, baseTextStyle) {
            if (!settings.balanceLines) return@remember null

            val layoutResult = textMeasurer.measure(
                text = line.content,
                style = baseTextStyle,
                constraints = androidx.compose.ui.unit.Constraints(maxWidth = availableWidthPx.toInt()),
            )

            // 如果行数 > 1，或者单行长度超过可用宽度的 80%，则尝试平衡（为了视觉整齐）
            if (layoutResult.lineCount > 1 || layoutResult.size.width > availableWidthPx * 0.8f) {
                LyricSplitter.findBestSplit(line.content, line.words.ifEmpty { null })
            } else {
                null
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = Arrangement.spacedBy(Dimens.LyricLineSpacing),
        ) {
            if (splitResult != null) {
                RenderLyricLine(
                    content = splitResult.line1,
                    words = splitResult.words1 ?: emptyList(),
                    currentProgress = currentProgress,
                    isActive = isActive,
                    textStyle = baseTextStyle,
                    textAlign = textAlign,
                    inactiveColor = inactiveColor,
                    activeColor = activeColor,
                    glowColor = glowColor,
                )
                RenderLyricLine(
                    content = splitResult.line2,
                    words = splitResult.words2 ?: emptyList(),
                    currentProgress = currentProgress,
                    isActive = isActive,
                    textStyle = baseTextStyle,
                    textAlign = textAlign,
                    inactiveColor = inactiveColor,
                    activeColor = activeColor,
                    glowColor = glowColor,
                )
            } else {
                RenderLyricLine(
                    content = line.content,
                    words = line.words,
                    currentProgress = currentProgress,
                    isActive = isActive,
                    textStyle = baseTextStyle,
                    textAlign = textAlign,
                    inactiveColor = inactiveColor,
                    activeColor = activeColor,
                    glowColor = glowColor,
                )
            }

            if (settings.showTranslation && !line.translation.isNullOrBlank()) {
                Text(
                    text = line.translation,
                    style = translationTextStyle,
                    color = translationColor,
                    textAlign = textAlign,
                    modifier = Modifier.padding(top = Dimens.PaddingExtraSmall).padding(horizontal = Dimens.LyricHorizontalPadding),
                )
            }
        }
    }
}

@Composable
private fun RenderLyricLine(
    content: String,
    words: List<WordInfo>,
    currentProgress: Long,
    isActive: Boolean,
    textStyle: TextStyle,
    textAlign: TextAlign,
    inactiveColor: Color = Color.White.copy(alpha = 0.6f),
    activeColor: Color = Color.White,
    glowColor: Color = Color.White,
) {
    if (words.isNotEmpty()) {
        WordByWordLine(
            words = words,
            currentProgress = currentProgress,
            isActive = isActive,
            textStyle = textStyle.copy(textAlign = textAlign),
            modifier = Modifier.padding(horizontal = Dimens.LyricHorizontalPadding),
            inactiveColor = inactiveColor,
            activeColor = activeColor,
            glowColor = glowColor,
        )
    } else {
        Text(
            text = content,
            style = textStyle,
            color = activeColor,
            textAlign = textAlign,
            modifier = Modifier.padding(horizontal = Dimens.LyricHorizontalPadding),
        )
    }
}

@Composable
fun LyricContent(
    state: PlayerState,
    onIntent: (PlayerIntent) -> Unit,
    modifier: Modifier = Modifier,
    topContentPadding: Dp = 40.dp,
    bottomContentPadding: Dp = 500.dp,
    activeTextColor: Color = Color.White,
    inactiveTextColor: Color = Color.White.copy(alpha = 0.6f),
    translationTextColor: Color = Color.White.copy(alpha = 0.8f),
    emptyTextColor: Color = Color.White.copy(alpha = 0.4f),
    fadeMaskColor: Color = Color.Black,
    glowColor: Color = Color.White,
) {
    val lyricData = state.lyricData
    val currentProgress = state.progress
    val haptic = LocalHapticFeedback.current
    val settings = state.lyricsSettings

    val activeLineIndex = remember(lyricData, currentProgress) {
        lyricData?.lines?.indexOfLast { it.startTime <= currentProgress } ?: -1
    }

    val listState = rememberLazyListState()
    val isDragged by listState.interactionSource.collectIsDraggedAsState()

    val density = LocalDensity.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp.dp

    LaunchedEffect(activeLineIndex) {
        if (activeLineIndex >= 0 && !isDragged) {
            val offset = with(density) { -(screenHeightDp * 0.10f).roundToPx() }
            listState.animateScrollToItem(activeLineIndex, scrollOffset = offset)
        }
    }

    val baseTextStyle = MaterialTheme.typography.headlineLarge.copy(
        fontSize = settings.fontSize.sp,
        fontWeight = FontWeight(settings.fontWeight),
        lineHeight = settings.fontSize.sp * 1.3f,
    )

    val translationTextStyle = MaterialTheme.typography.titleLarge.copy(
        fontSize = (settings.fontSize * 0.75f).sp,
        fontWeight = FontWeight(settings.fontWeight),
        lineHeight = (settings.fontSize * 0.75f).sp * 1.3f,
    )

    val lyricTextAlign = when (settings.alignment) {
        0 -> TextAlign.Start
        1 -> TextAlign.Center
        else -> TextAlign.End
    }

    val lyricHorizontalAlignment = when (settings.alignment) {
        0 -> Alignment.Start
        1 -> Alignment.CenterHorizontally
        else -> Alignment.End
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                val fadingBrush = Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.05f to fadeMaskColor,
                    0.85f to fadeMaskColor,
                    1f to Color.Transparent,
                )
                drawRect(brush = fadingBrush, blendMode = BlendMode.DstIn)
            },
        contentAlignment = Alignment.TopStart,
    ) {
            if (lyricData == null || lyricData.lines.isEmpty()) {
                Text(
                    text = stringResource(R.string.msg_no_lyrics),
                    style = MaterialTheme.typography.headlineMedium,
                    color = emptyTextColor,
                    modifier = Modifier.align(Alignment.Center),
                )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = topContentPadding, bottom = bottomContentPadding),
                horizontalAlignment = lyricHorizontalAlignment,
            ) {
                itemsIndexed(lyricData.lines) { index, line ->
                    val isActive = index == activeLineIndex
                    val distance = if (activeLineIndex == -1) 0 else kotlin.math.abs(index - activeLineIndex)

                    val targetAlpha = if (isActive || isDragged) 1f else (0.6f - (distance * 0.07f)).coerceAtLeast(0.25f)
                    val alpha by animateFloatAsState(
                        targetValue = targetAlpha,
                        animationSpec = tween(600),
                        label = "LyricAlpha",
                    )
                    val scale by animateFloatAsState(
                        targetValue = if (isActive) 1.06f else 1f,
                        animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow),
                        label = "LyricScale",
                    )

                    val targetBlur = if (settings.blurEnabled && (isActive || isDragged).not()) {
                        (distance.toFloat() * 4f).dp.coerceAtMost(Dimens.PaddingMedium)
                    } else {
                        0.dp
                    }

                    val blurRadius by animateDpAsState(
                        targetValue = targetBlur,
                        animationSpec = tween(600),
                        label = "LyricBlur",
                    )

                    // 关键修复：外扩空间保持固定，避免歌词项高度在播放过程中动态变化，导致滚动跳动
                    val blurOverflowPadding = if (settings.blurEnabled) 6.dp else 0.dp
                    val highlightOverflowPadding = 4.dp

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = blurOverflowPadding + highlightOverflowPadding),
                    ) {
                        BalancedLyricView(
                            line = line,
                            currentProgress = currentProgress,
                            isActive = isActive,
                            baseTextStyle = baseTextStyle,
                            translationTextStyle = translationTextStyle,
                            settings = settings,
                            textAlign = lyricTextAlign,
                            horizontalAlignment = lyricHorizontalAlignment,
                            inactiveColor = inactiveTextColor,
                            activeColor = activeTextColor,
                            glowColor = glowColor,
                            translationColor = translationTextColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .blur(
                                    radius = blurRadius,
                                    edgeTreatment = BlurredEdgeTreatment.Unbounded,
                                )
                                .graphicsLayer {
                                    this.alpha = alpha
                                    this.scaleX = scale
                                    this.scaleY = scale
                                    this.transformOrigin = androidx.compose.ui.graphics.TransformOrigin(
                                        when (settings.alignment) {
                                            0 -> 0f
                                            1 -> 0.5f
                                            else -> 1f
                                        },
                                        0.5f,
                                    )
                                },
                        )

                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .padding(horizontal = 32.dp)
                                .pointerInput(index) {
                                    detectTapGestures(
                                        onTap = {
                                            onIntent(PlayerIntent.SeekTo(line.startTime))
                                        },
                                        onLongPress = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onIntent(PlayerIntent.OpenLyricShare(index))
                                        },
                                    )
                                },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistModeButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    contentDescription: String?,
) {
    Surface(
        onClick = onClick,
        color = if (isActive) Color.White.copy(alpha = 0.2f) else Color.Transparent,
        shape = RoundedCornerShape(Dimens.CornerRadiusLarge),
        modifier = Modifier.size(Dimens.IconSizeHuge),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isActive) Color.White else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(Dimens.IconSizeMedium),
            )
        }
    }
}
