package org.parallel_sekai.kanade.ui.screens.player

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
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
import org.parallel_sekai.kanade.ui.theme.Dimens
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

enum class PlayerExpansionValue {
    Collapsed,
    Expanded,
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
) {
    if (state.currentSong == null) return

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(if (isTransitioning) 1f else 0f), // 滑动时在前面，切换前在后面
                ) {
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
                        onClick = { onIntent(PlayerIntent.ToggleLyricSelection(index)) }
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
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                        .data(state.currentSong?.coverUrl)
                        .crossfade(true)
                        .build(),
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
    var showLyrics by remember { mutableStateOf(false) }
    var showPlaylist by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    val density = LocalDensity.current

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

            // 小播放器 (Mini)
            val miniArtSize = Dimens.AlbumCoverSizeMiniPlayer
            val miniArtX = Dimens.PaddingMedium
            val miniArtY = Dimens.PaddingMedium
            val miniTitleSize = 14.sp
            val miniArtistSize = 12.sp
            val miniTextX = 72.dp

            // 全屏封面模式 (Full Cover)
            val fullCoverArtSize = screenWidth * 0.85f
            val fullCoverArtX = (screenWidth - fullCoverArtSize) / 2
            val fullCoverArtY = 140.dp
            val fullCoverTitleSize = 24.sp
            val fullCoverArtistSize = 20.sp
            val fullCoverTitleY = screenHeight - 380.dp
            val fullCoverArtistY = screenHeight - 340.dp
            val fullCoverTextX = fullCoverArtX

            // 全屏歌词/列表模式 (Full Lyric/Playlist)
            val fullLyricArtSize = Dimens.IconSizeHuge
            val fullLyricArtX = Dimens.PaddingLarge
            val fullLyricArtY = 66.dp // This looks like it's tied to status bar height + padding, keep for now
            val fullLyricTitleSize = 16.sp
            val fullLyricArtistSize = 14.sp
            val fullLyricTitleY = 64.dp // Keep as is, possibly derived from status bar
            val fullLyricArtistY = 88.dp // Keep as is, possibly derived from status bar
            val fullLyricTextX = 76.dp // Keep as is, derived from fullLyricArtX + its size + padding

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
            val fullTargetTextWidth = lerp(fullCoverArtSize - 48.dp, screenWidth - 160.dp, lyricTransitionFraction)

            // 三点按钮位置插值
            val fullCoverMoreX = screenWidth - fullCoverArtX - Dimens.PaddingExtraLarge
            val fullCoverMoreY = fullCoverTitleY + Dimens.PaddingExtraSmall
            val fullLyricMoreX = screenWidth - Dimens.AlbumCoverSizeMiniPlayer
            val fullLyricMoreY = 72.dp // Keep as is for now

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
            val fullArtistColor = Color.White.copy(alpha = 0.6f)
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
                            Box(modifier = Modifier.size(Dimens.IconSizeExtraLarge, Dimens.PaddingExtraSmall).clip(RoundedCornerShape(Dimens.CornerRadiusSmall)).background(Color.White.copy(alpha = 0.3f)).align(Alignment.TopCenter)) // 36.dp, 4.dp, 2.dp
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
                        LyricContent(state, onIntent)
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
                            state,
                            onIntent,
                            showLyrics,
                            showPlaylist,
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
        valueRange = 0f..(state.duration.toFloat().coerceAtLeast(1f)),
        modifier = Modifier.fillMaxWidth(),
        colors = SliderDefaults.colors(
            thumbColor = Color.White,
            activeTrackColor = Color.White,
            inactiveTrackColor = Color.White.copy(alpha = 0.2f),
        ),
    )

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.PaddingExtraSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = formatTime(state.progress), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
        Text(text = "-" + formatTime(state.duration - state.progress), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
    }

    Spacer(modifier = Modifier.height(Dimens.SpacingMedium))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onIntent(PlayerIntent.Previous) }) {
            Icon(Icons.Default.SkipPrevious, contentDescription = null, modifier = Modifier.size(Dimens.IconSizeSuperHuge), tint = Color.White)
        }
        IconButton(onClick = { onIntent(PlayerIntent.PlayPause) }, modifier = Modifier.size(Dimens.IconSizeColossal)) {
            Icon(imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(Dimens.IconSizeGigantic), tint = Color.White)
        }
        IconButton(onClick = { onIntent(PlayerIntent.Next) }) {
            Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(Dimens.IconSizeSuperHuge), tint = Color.White)
        }
    }

    Spacer(modifier = Modifier.height(Dimens.SpacingLarge))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onToggleLyrics(!showLyrics) }) {
            Icon(imageVector = Icons.Default.Lyrics, contentDescription = null, tint = if (showLyrics) Color.White else Color.White.copy(alpha = 0.5f), modifier = Modifier.size(Dimens.IconSizeMedium))
        }
        IconButton(onClick = { }) {
            Icon(Icons.Default.Airplay, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(Dimens.IconSizeMedium))
        }
        IconButton(onClick = { onTogglePlaylist(!showPlaylist) }) {
            Icon(imageVector = Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = if (showPlaylist) Color.White else Color.White.copy(alpha = 0.5f), modifier = Modifier.size(Dimens.IconSizeMedium))
        }
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
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.currentSong) {
        val index = state.currentPlaylist.indexOf(state.currentSong)
        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Dimens.PaddingLarge),
        ) {
            // 缩减顶部占位，标题紧贴上方信息
            Spacer(modifier = Modifier.height(Dimens.SpacingSmall))

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
                    color = Color.White,
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
                            0.05f to Color.Black,
                            0.95f to Color.Black,
                            1f to Color.Transparent,
                        )
                        drawRect(brush = fadingBrush, blendMode = BlendMode.DstIn)
                    },
                contentPadding = PaddingValues(bottom = 100.dp),
            ) {
                itemsIndexed(state.currentPlaylist) { index, music ->
                    val isCurrent = music.id == state.currentSong?.id

                    Surface(
                        onClick = { onIntent(PlayerIntent.SelectSong(music)) },
                        color = if (isCurrent) Color.White.copy(alpha = 0.1f) else Color.Transparent,
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
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.White,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = music.artists.joinToString(state.artistJoinString),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.3f),
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

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = wordScale
                scaleY = wordScale
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.5f)
            },
    ) {
        Text(
            text = text,
            style = textStyle,
            color = Color.White.copy(alpha = baseAlpha),
            softWrap = false,
        )

        if (isActiveLine && fillProgress > 0f) {
            val shadow = if (shouldGlow && glowRadius > 0f) {
                Shadow(
                    color = Color.White.copy(alpha = (glowRadius / 20f) * 0.7f),
                    blurRadius = glowRadius,
                    offset = Offset.Zero,
                )
            } else {
                null
            }

            Text(
                text = text,
                style = textStyle.copy(shadow = shadow),
                color = Color.White,
                softWrap = false,
                modifier = Modifier.drawWithContent {
                    clipRect(right = size.width * fillProgress) {
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
                )
                RenderLyricLine(
                    content = splitResult.line2,
                    words = splitResult.words2 ?: emptyList(),
                    currentProgress = currentProgress,
                    isActive = isActive,
                    textStyle = baseTextStyle,
                    textAlign = textAlign,
                )
            } else {
                RenderLyricLine(
                    content = line.content,
                    words = line.words,
                    currentProgress = currentProgress,
                    isActive = isActive,
                    textStyle = baseTextStyle,
                    textAlign = textAlign,
                )
            }

            if (settings.showTranslation && !line.translation.isNullOrBlank()) {
                Text(
                    text = line.translation,
                    style = translationTextStyle,
                    color = Color.White.copy(alpha = 0.8f),
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
) {
    if (words.isNotEmpty()) {
        WordByWordLine(
            words = words,
            currentProgress = currentProgress,
            isActive = isActive,
            textStyle = textStyle.copy(textAlign = textAlign),
            modifier = Modifier.padding(horizontal = Dimens.LyricHorizontalPadding),
        )
    } else {
        Text(
            text = content,
            style = textStyle,
            color = Color.White,
            textAlign = textAlign,
            modifier = Modifier.padding(horizontal = Dimens.LyricHorizontalPadding),
        )
    }
}

@Composable
fun LyricContent(
    state: PlayerState,
    onIntent: (PlayerIntent) -> Unit,
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
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                val fadingBrush = Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.05f to Color.Black,
                    0.85f to Color.Black,
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
                color = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 40.dp, bottom = 500.dp), // Keep 40.dp and 500.dp for now, these seem specific layout values
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
                        (distance.toFloat() * 4f).dp.coerceAtMost(Dimens.PaddingMedium) // 16.dp
                    } else {
                        0.dp
                    }

                    val blurRadius by animateDpAsState(
                        targetValue = targetBlur,
                        animationSpec = tween(600),
                        label = "LyricBlur",
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        BalancedLyricView(
                            line = line,
                            currentProgress = currentProgress,
                            isActive = isActive,
                            baseTextStyle = baseTextStyle,
                            translationTextStyle = translationTextStyle,
                            settings = settings,
                            textAlign = lyricTextAlign,
                            horizontalAlignment = lyricHorizontalAlignment,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp) // Keep 20.dp for now, seems specific
                                .blur(blurRadius)
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
