package org.parallel_sekai.kanade.ui.screens.player

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

enum class PlayerExpansionValue {
    Collapsed, Expanded
}

@Composable
fun FluidBackground(
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "FluidBackground")
    
    val offset1 by infiniteTransition.animateValue(
        initialValue = IntOffset(0, 0),
        targetValue = IntOffset(300, 400),
        typeConverter = IntOffset.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "Blob1Offset"
    )
    
    val offset2 by infiniteTransition.animateValue(
        initialValue = IntOffset(400, 200),
        targetValue = IntOffset(0, 500),
        typeConverter = IntOffset.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "Blob2Offset"
    )

    val offset3 by infiniteTransition.animateValue(
        initialValue = IntOffset(100, 500),
        targetValue = IntOffset(500, 0),
        typeConverter = IntOffset.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "Blob3Offset"
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
                    tileMode = TileMode.Clamp
                ),
                radius = size.width,
                center = androidx.compose.ui.geometry.Offset(offset1.x.toFloat(), offset1.y.toFloat())
            )
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color3, Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(size.width - offset2.x, offset2.y.toFloat()),
                    radius = size.width * 0.8f,
                    tileMode = TileMode.Clamp
                ),
                radius = size.width * 0.8f,
                center = androidx.compose.ui.geometry.Offset(size.width - offset2.x, offset2.y.toFloat())
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color2, Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(offset3.x.toFloat(), size.height - offset3.y),
                    radius = size.width * 1.2f,
                    tileMode = TileMode.Clamp
                ),
                radius = size.width * 1.2f,
                center = androidx.compose.ui.geometry.Offset(offset3.x.toFloat(), size.height - offset3.y)
            )
        }
    }
}

@Composable
fun KanadePlayerContainer(
    state: PlayerState,
    onIntent: (PlayerIntent) -> Unit,
    bottomPadding: Dp = 0.dp
) {
    if (state.currentSong == null) return

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    val miniPlayerHeightPx = with(density) { (64.dp + 16.dp).toPx() }
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
    } else 0f

    Box(modifier = Modifier.fillMaxSize()) {
        // 这是一个随 offsetY 移动的包装器，它承载了手势和内容
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, offsetY.value.roundToInt()) }
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
                            fraction > 0.5f -> expandedOffset   // 超过一半向上弹
                            else -> collapsedOffset             // 未过一半向下弹
                        }
                        
                        scope.launch {
                            offsetY.animateTo(target, spring(stiffness = Spring.StiffnessMediumLow))
                            if (target == expandedOffset && !state.isExpanded) {
                                onIntent(PlayerIntent.Expand)
                            } else if (target == collapsedOffset && state.isExpanded) {
                                onIntent(PlayerIntent.Collapse)
                            }
                        }
                    }
                )
        ) {
            val isTransitioning = fraction > 0f

            // 小播放器层
            if (fraction < 0.8f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (isTransitioning) 0f else 1f)
                        .alpha((1f - fraction * 1.25f).coerceIn(0f, 1f))
                ) {
                    MiniPlayerContent(
                        state = state,
                        onIntent = onIntent,
                        showContent = fraction <= 0f
                    )
                }
            }

            // 全屏播放器层
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(if (isTransitioning) 1f else 0f) // 滑动时在前面，切换前在后面
            ) {
                FullScreenContent(
                    state = state,
                    onIntent = onIntent,
                    expansionFraction = fraction,
                    offsetY = 0f
                )
            }
        }
    }
}

@Composable
private fun MiniPlayerContent(
    state: PlayerState,
    onIntent: (PlayerIntent) -> Unit,
    showContent: Boolean = true
) {
    val progressFraction = if (state.duration > 0) state.progress.toFloat() / state.duration else 0f
    val playedColor = MaterialTheme.colorScheme.primaryContainer

    Surface(
        onClick = { onIntent(PlayerIntent.Expand) },
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(
                        color = playedColor,
                        size = Size(width = size.width * progressFraction, height = size.height)
                    )
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showContent) {
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                        .data(state.currentSong?.coverUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(48.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
                Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                    Text(
                        text = state.currentSong?.title ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = state.currentSong?.artist ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
    expansionFraction: Float,
    offsetY: Float
) {
    PredictiveBackHandler(enabled = state.isExpanded) { progressFlow ->
        try {
            progressFlow.collect { /* handle back progress */ }
            onIntent(PlayerIntent.Collapse)
        } catch (e: Exception) {
        }
    }

    var showLyrics by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    val density = LocalDensity.current
    
    // 新增：歌词模式切换动画进度
    val lyricTransitionFraction by animateFloatAsState(
        targetValue = if (showLyrics) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "LyricTransition"
    )

    // 仅在显示歌词且正在播放时，5秒后自动隐藏控件
    LaunchedEffect(controlsVisible, state.isPlaying, showLyrics) {
        if (showLyrics && controlsVisible && state.isPlaying) {
            kotlinx.coroutines.delay(5000)
            controlsVisible = false
        }
        // 如果切回封面模式，确保控件立即显示
        if (!showLyrics) {
            controlsVisible = true
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { 
                // 仅在歌词模式下允许手动切换控件可见性
                if (showLyrics) {
                    controlsVisible = !controlsVisible 
                } else {
                    controlsVisible = true
                }
            },
        color = Color.Transparent
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight

            // 手动管理黑色背景的透明度，实现渐变变暗效果
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = expansionFraction)))

            // 1. 定义三种状态下的基础值
            
            // 小播放器 (Mini)
            val miniArtSize = 48.dp
            val miniArtX = 16.dp 
            val miniArtY = 16.dp 
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

            // 全屏歌词模式 (Full Lyric)
            val fullLyricArtSize = 42.dp
            val fullLyricArtX = 24.dp
            val fullLyricArtY = 66.dp
            val fullLyricTitleSize = 16.sp
            val fullLyricArtistSize = 14.sp
            val fullLyricTitleY = 64.dp
            val fullLyricArtistY = 88.dp
            val fullLyricTextX = 76.dp

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
            val fullCoverMoreX = screenWidth - fullCoverArtX - 32.dp
            val fullCoverMoreY = fullCoverTitleY + 4.dp
            val fullLyricMoreX = screenWidth - 48.dp
            val fullLyricMoreY = 72.dp

            val fullTargetMoreX = lerp(fullCoverMoreX, fullLyricMoreX, lyricTransitionFraction)
            val fullTargetMoreY = lerp(fullCoverMoreY, fullLyricMoreY, lyricTransitionFraction)

            // 3. 最后根据 expansionFraction 在 Mini 和 “当前全屏目标” 之间插值
            val currentArtSize = lerp(miniArtSize, fullTargetArtSize, expansionFraction)
            val currentArtX = lerp(miniArtX, fullTargetArtX, expansionFraction)
            val currentArtY = lerp(miniArtY, fullTargetArtY, expansionFraction)
            val currentArtCornerRadius = lerp(4.dp, fullTargetArtCornerRadius, expansionFraction)

            val currentTitleSize = lerp(miniTitleSize, fullTargetTitleSize, expansionFraction)
            val currentArtistSize = lerp(miniArtistSize, fullTargetArtistSize, expansionFraction)
            val currentTitleY = lerp(miniArtY + 6.dp, fullTargetTitleY, expansionFraction)
            val currentArtistY = lerp(miniArtY + 28.dp, fullTargetArtistY, expansionFraction)
            val currentTextX = lerp(miniTextX, fullTargetTextX, expansionFraction)
            
            val miniTextWidth = screenWidth - miniTextX - 100.dp
            val currentTextWidth = lerp(miniTextWidth, fullTargetTextWidth, expansionFraction)

            val currentMoreX = lerp(screenWidth - 48.dp, fullTargetMoreX, expansionFraction)
            val currentMoreY = lerp(miniArtY + 12.dp, fullTargetMoreY, expansionFraction)

            val miniArtistColor = MaterialTheme.colorScheme.onSurfaceVariant
            val fullArtistColor = Color.White.copy(alpha = 0.6f)
            val currentArtistColor = lerp(miniArtistColor, fullArtistColor, expansionFraction)

            val controlsAlpha by animateFloatAsState(
                targetValue = if (controlsVisible) 1f else 0f,
                animationSpec = tween(500),
                label = "ControlsAlpha"
            )

            FluidBackground(
                colors = state.gradientColors,
                modifier = Modifier.fillMaxSize().alpha(0.8f * expansionFraction)
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
                            currentArtY.roundToPx()
                        )
                    }
                    .size(currentArtSize)
                    .clip(RoundedCornerShape(currentArtCornerRadius))
                    .background(Color.DarkGray),
                contentScale = ContentScale.Crop
            )

            Text(
                text = state.currentSong?.title ?: "",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = currentTitleSize,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier
                    .offset { 
                        IntOffset(
                            currentTextX.roundToPx(),
                            currentTitleY.roundToPx()
                        )
                    }
                    .width(currentTextWidth)
                    .alpha(if (showLyrics || expansionFraction < 1f) 1f else controlsAlpha)
            )

            Text(
                text = state.currentSong?.artist ?: "",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = currentArtistSize,
                    color = currentArtistColor
                ),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier
                    .offset { 
                        IntOffset(
                            currentTextX.roundToPx(),
                            currentArtistY.roundToPx()
                        )
                    }
                    .width(currentTextWidth)
                    .alpha(if (showLyrics || expansionFraction < 1f) 1f else controlsAlpha)
            )

            // Apple Music 风格的三点按钮
            Box(
                modifier = Modifier
                    .offset { IntOffset(currentMoreX.roundToPx(), currentMoreY.roundToPx()) }
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f * expansionFraction))
                    .clickable { /* More actions */ }
                    .alpha(if (showLyrics || expansionFraction < 1f) 1f else controlsAlpha),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .alpha(expansionFraction),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 顶部拉动指示器
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(88.dp)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = controlsVisible,
                        enter = fadeIn(tween(500)),
                        exit = fadeOut(tween(500))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(modifier = Modifier.size(36.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.3f)).align(Alignment.TopCenter))
                        }
                    }
                }

                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showLyrics && expansionFraction > 0.9f,
                        enter = fadeIn(tween(500)) + expandVertically(expandFrom = Alignment.CenterVertically),
                        exit = fadeOut(tween(500)) + shrinkVertically(shrinkTowards = Alignment.CenterVertically)
                    ) {
                        LyricContent(state, onIntent)
                    }
                    
                    if (!showLyrics) {
                        Box(modifier = Modifier
                            .offset { 
                                IntOffset(
                                    currentArtX.roundToPx(),
                                    currentArtY.roundToPx()
                                )
                            }
                            .size(currentArtSize)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { 
                                showLyrics = true 
                                controlsVisible = true
                            }
                        )
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = controlsVisible && expansionFraction > 0.8f,
                    enter = fadeIn(tween(500)) + expandVertically(expandFrom = Alignment.Bottom),
                    exit = fadeOut(tween(500)) + shrinkVertically(shrinkTowards = Alignment.Bottom)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                            .padding(horizontal = 24.dp)
                    ) {
                        val dynamicSpacerHeight by animateDpAsState(
                            targetValue = if (showLyrics) 0.dp else 160.dp,
                            animationSpec = tween(500, easing = FastOutSlowInEasing),
                            label = "LyricAreaHeightAnimation"
                        )
                        Spacer(modifier = Modifier.height(dynamicSpacerHeight))
                        PlayerControlsSection(state, onIntent, showLyrics) { 
                            showLyrics = it
                            controlsVisible = true
                        }
                    }
                }
                
                val bottomSafeMargin by animateDpAsState(
                    targetValue = if (controlsVisible) 0.dp else 24.dp,
                    animationSpec = tween(500),
                    label = "BottomSafeMarginAnimation"
                )
                Spacer(modifier = Modifier.height(bottomSafeMargin))
            }
        }
    }
}

// 辅助插值函数
private fun lerp(start: Dp, stop: Dp, fraction: Float): Dp {
    return start + (stop - start) * fraction
}

private fun lerp(start: TextUnit, stop: TextUnit, fraction: Float): TextUnit {
    return (start.value + (stop.value - start.value) * fraction).sp
}

private fun lerp(start: Color, stop: Color, fraction: Float): Color {
    return androidx.compose.ui.graphics.lerp(start, stop, fraction)
}

@Composable
private fun PlayerControlsSection(
    state: PlayerState,
    onIntent: (PlayerIntent) -> Unit,
    showLyrics: Boolean,
    onToggleLyrics: (Boolean) -> Unit
) {
    var sliderPosition by remember { mutableFloatStateOf(state.progress.toFloat()) }
    val isDragging = remember { mutableStateOf(false) }
    LaunchedEffect(state.progress) {
        if (!isDragging.value) sliderPosition = state.progress.toFloat()
    }

    Slider(
        value = sliderPosition,
        onValueChange = { isDragging.value = true; sliderPosition = it },
        onValueChangeFinished = {
            isDragging.value = false; onIntent(PlayerIntent.SeekTo(sliderPosition.toLong()))
        },
        valueRange = 0f..(state.duration.toFloat().coerceAtLeast(1f)),
        modifier = Modifier.fillMaxWidth(),
        colors = SliderDefaults.colors(
            thumbColor = Color.White,
            activeTrackColor = Color.White,
            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
        )
    )

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = formatTime(state.progress), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
        Text(text = "-" + formatTime(state.duration - state.progress), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
    }

    Spacer(modifier = Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onIntent(PlayerIntent.Previous) }) {
            Icon(Icons.Default.SkipPrevious, contentDescription = null, modifier = Modifier.size(42.dp), tint = Color.White)
        }
        IconButton(onClick = { onIntent(PlayerIntent.PlayPause) }, modifier = Modifier.size(84.dp)) {
            Icon(imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.White)
        }
        IconButton(onClick = { onIntent(PlayerIntent.Next) }) {
            Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(42.dp), tint = Color.White)
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onToggleLyrics(!showLyrics) }) {
            Icon(imageVector = Icons.Default.Lyrics, contentDescription = null, tint = if (showLyrics) Color.White else Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = { }) {
            Icon(Icons.Default.Airplay, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = { }) {
            Icon(Icons.Default.QueueMusic, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
        }
    }
}

private fun formatTime(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return String.format("%d:%02d", minutes, seconds)
}

@Composable
private fun KaraokeWord(
    text: String,
    startTime: Long,
    endTime: Long,
    currentProgress: Long,
    isActiveLine: Boolean,
    textStyle: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier
) {
    val duration = endTime - startTime
    val shouldGlow = duration > 500 // 超过0.5s

    val fillProgress = remember(currentProgress, startTime, endTime) {
        if (currentProgress >= endTime) 1f
        else if (currentProgress < startTime) 0f
        else (currentProgress - startTime).toFloat() / (endTime - startTime).coerceAtLeast(1).toFloat()
    }.coerceIn(0f, 1f)

    val isWordActive = currentProgress in startTime until endTime
    val wordScale by animateFloatAsState(
        targetValue = if (isActiveLine && isWordActive) 1.05f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "WordScale"
    )

    // 恒定发光：仅在正在唱的时候显示，不带呼吸效果
    val glowRadius by animateFloatAsState(
        targetValue = if (isActiveLine && isWordActive && shouldGlow) 20f else 0f,
        animationSpec = tween(300),
        label = "GlowRadius"
    )

    val baseAlpha = if (isActiveLine) 0.35f else 1f

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = wordScale
                scaleY = wordScale
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.5f)
            }
    ) {
        // 底层：基础色
        Text(
            text = text,
            style = textStyle,
            color = Color.White.copy(alpha = baseAlpha),
            fontWeight = FontWeight.ExtraBold,
            softWrap = false
        )

        // 顶层：填充层 + 发光效果
        if (isActiveLine && fillProgress > 0f) {
            // 只要动画半径 > 0，就保留 Shadow 以实现淡出过渡
            val shadow = if (shouldGlow && glowRadius > 0f) {
                Shadow(
                    color = Color.White.copy(alpha = (glowRadius / 20f) * 0.7f),
                    blurRadius = glowRadius,
                    offset = Offset.Zero
                )
            } else null

            Text(
                text = text,
                style = textStyle.copy(shadow = shadow),
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                softWrap = false,
                modifier = Modifier.drawWithContent {
                    clipRect(right = size.width * fillProgress) {
                        this@drawWithContent.drawContent()
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordByWordLine(
    words: List<WordInfo>,
    currentProgress: Long,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start
    ) {
        words.forEach { word ->
            KaraokeWord(
                text = word.text,
                startTime = word.startTime,
                endTime = word.endTime,
                currentProgress = currentProgress,
                isActiveLine = isActive,
                textStyle = MaterialTheme.typography.headlineLarge.copy(
                    lineHeight = MaterialTheme.typography.headlineLarge.lineHeight * 1.2f
                )
            )
        }
    }
}

@Composable
fun LyricContent(
    state: PlayerState,
    onIntent: (PlayerIntent) -> Unit
) {
    val lyricData = state.lyricData
    val currentProgress = state.progress
    val haptic = LocalHapticFeedback.current
    
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
                    1f to Color.Transparent
                )
                drawRect(brush = fadingBrush, blendMode = BlendMode.DstIn)
            },
        contentAlignment = Alignment.TopStart
    ) {
        if (lyricData == null || lyricData.lines.isEmpty()) {
            Text(
                text = "No Lyrics",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 40.dp, bottom = 500.dp)
            ) {
                itemsIndexed(lyricData.lines) { index, line ->
                    val isActive = index == activeLineIndex
                    val distance = if (activeLineIndex == -1) 0 else kotlin.math.abs(index - activeLineIndex)
                    
                    val targetAlpha = if (isActive || isDragged) 1f else (0.6f - (distance * 0.07f)).coerceAtLeast(0.25f)
                    val alpha by animateFloatAsState(
                        targetValue = targetAlpha,
                        animationSpec = tween(600),
                        label = "LyricAlpha"
                    )
                    val scale by animateFloatAsState(
                        targetValue = if (isActive) 1.06f else 1f,
                        animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow),
                        label = "LyricScale"
                    )
                    
                    val targetBlur = if (isActive || isDragged) 0.dp else (distance.toFloat() * 4f).dp.coerceAtMost(16.dp)
                    val blurRadius by animateDpAsState(
                        targetValue = targetBlur,
                        animationSpec = tween(600),
                        label = "LyricBlur"
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp)
                                .blur(blurRadius)
                                .graphicsLayer {
                                    this.alpha = alpha
                                    this.scaleX = scale
                                    this.scaleY = scale
                                    this.transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                                },
                            horizontalAlignment = Alignment.Start
                        ) {
                            if (line.words.isNotEmpty()) {
                                WordByWordLine(
                                    words = line.words,
                                    currentProgress = currentProgress,
                                    isActive = isActive,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                )
                            } else {
                                Text(
                                    text = line.content,
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    textAlign = TextAlign.Start,
                                    lineHeight = MaterialTheme.typography.headlineLarge.lineHeight * 1.2f,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                )
                            }
                            
                            if (!line.translation.isNullOrBlank()) {
                                Text(
                                    text = line.translation,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.padding(top = 8.dp).padding(horizontal = 32.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .padding(horizontal = 32.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onIntent(PlayerIntent.SeekTo(line.startTime))
                                }
                        )
                    }
                }
            }
        }
    }
}