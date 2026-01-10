package org.parallel_sekai.kanade.ui.screens.player

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun KanadePlayerContainer(
    state: PlayerState,
    onIntent: (PlayerIntent) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    if (state.currentSong == null) return

    SharedTransitionLayout {
        AnimatedContent(
            targetState = state.isExpanded,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith
                fadeOut(animationSpec = tween(300))
            },
            label = "PlayerExpansionTransition",
            modifier = Modifier.fillMaxSize()
        ) { isExpanded ->
            val contentScope = this
            if (isExpanded) {
                FullScreenContent(state, onIntent, this@SharedTransitionLayout, contentScope)
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = bottomPadding),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    MiniPlayerContent(state, onIntent, this@SharedTransitionLayout, contentScope)
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun MiniPlayerContent(
    state: PlayerState,
    onIntent: (PlayerIntent) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val progressFraction = if (state.duration > 0) state.progress.toFloat() / state.duration else 0f
    val playedColor = MaterialTheme.colorScheme.primaryContainer

    with(sharedTransitionScope) {
        Surface(
            onClick = { onIntent(PlayerIntent.Expand) },
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .sharedBounds(
                    rememberSharedContentState(key = "player_container"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                    clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(12.dp))
                )
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant, // 进度条非激活区域（轨道）颜色
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        // 在内容之下、Surface背景之上绘制进度
                        drawRect(
                            color = playedColor,
                            size = Size(width = size.width * progressFraction, height = size.height)
                        )
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = state.currentSong?.coverUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .sharedElement(
                            rememberSharedContentState(key = "album_art"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                        .padding(8.dp).size(48.dp).clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
                Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                    Text(text = state.currentSong?.title ?: "", style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    Text(text = state.currentSong?.artist ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun FullScreenContent(
    state: PlayerState,
    onIntent: (PlayerIntent) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    with(sharedTransitionScope) {
        PredictiveBackHandler(enabled = true) { progressFlow ->
            try {
                progressFlow.collect { backEvent ->
                    // 可以在这里根据 backEvent.progress 调整 FullScreenContent 的 graphicsLayer
                }
                onIntent(PlayerIntent.Collapse)
            } catch (e: Exception) {
                // 手势取消
            }
        }
        Surface(
            modifier = Modifier
                .sharedBounds(
                    rememberSharedContentState(key = "player_container"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
                )
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            var showLyrics by remember { mutableStateOf(false) }

            Box(modifier = Modifier.fillMaxSize()) {
                // 背景模糊层 (仅在显示歌词时可见)
                if (showLyrics) {
                    AsyncImage(
                        model = state.currentSong?.coverUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(32.dp),
                        contentScale = ContentScale.Crop
                    )
                    // 黑色遮罩层，提升文字对比度
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onIntent(PlayerIntent.Collapse) }) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = if (showLyrics) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Now Playing",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (showLyrics) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = null,
                                tint = if (showLyrics) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .weight(1f) // 让这一块占据更多空间
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { showLyrics = !showLyrics }
                    ) {
                        AnimatedContent(
                            targetState = showLyrics,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(400)) togetherWith fadeOut(
                                    animationSpec = tween(400)
                                )
                            },
                            label = "LyricToggleTransition",
                            modifier = Modifier.fillMaxSize()
                        ) { targetShowLyrics ->
                            if (targetShowLyrics) {
                                LyricContent(state)
                            } else {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    AsyncImage(
                                        model = state.currentSong?.coverUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .sharedElement(
                                                rememberSharedContentState(key = "album_art"),
                                                animatedVisibilityScope = animatedVisibilityScope
                                            )
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = state.currentSong?.title ?: "Unknown",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            color = if (showLyrics) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = state.currentSong?.artist ?: "Unknown",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (showLyrics) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

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
                        colors = if (showLyrics) SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ) else SliderDefaults.colors()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(state.progress),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (showLyrics) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = formatTime(state.duration),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (showLyrics) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onIntent(PlayerIntent.ToggleShuffle) }) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = null,
                                tint = if (showLyrics) {
                                    if (state.shuffleModeEnabled) Color.White else Color.White.copy(alpha = 0.5f)
                                } else {
                                    if (state.shuffleModeEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                        IconButton(onClick = { onIntent(PlayerIntent.Previous) }) {
                            Icon(
                                Icons.Default.SkipPrevious,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = if (showLyrics) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        FilledIconButton(
                            onClick = { onIntent(PlayerIntent.PlayPause) },
                            modifier = Modifier.size(72.dp),
                            colors = if (showLyrics) IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ) else IconButtonDefaults.filledIconButtonColors()
                        ) {
                            Icon(
                                imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        IconButton(onClick = { onIntent(PlayerIntent.Next) }) {
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = if (showLyrics) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { onIntent(PlayerIntent.ToggleRepeat) }) {
                            val icon = when (state.repeatMode) {
                                RepeatMode.OFF -> Icons.Default.Repeat
                                RepeatMode.ALL -> Icons.Default.Repeat
                                RepeatMode.ONE -> Icons.Default.RepeatOne
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (showLyrics) {
                                    if (state.repeatMode != RepeatMode.OFF) Color.White else Color.White.copy(
                                        alpha = 0.5f
                                    )
                                } else {
                                    if (state.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
fun LyricContent(state: PlayerState) {
    val lyricData = state.lyricData
    val currentProgress = state.progress
    
    // 查找当前活跃的行索引
    val activeLineIndex = remember(lyricData, currentProgress) {
        lyricData?.lines?.indexOfLast { it.startTime <= currentProgress } ?: -1
    }

    val listState = rememberLazyListState()

    // 自动滚动到活跃行
    LaunchedEffect(activeLineIndex) {
        if (activeLineIndex >= 0) {
            listState.animateScrollToItem(activeLineIndex, scrollOffset = -200)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (lyricData == null || lyricData.lines.isEmpty()) {
            val debugText = when {
                state.lyrics == null -> "No lyrics found in file tags."
                state.lyrics.isBlank() -> "Lyrics tag exists but is empty."
                else -> "Parse failed. Raw: ${state.lyrics.take(100)}..."
            }
            Text(
                text = debugText,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(vertical = 120.dp) // 上下留白以便居中
            ) {
                itemsIndexed(lyricData.lines) { index, line ->
                    val isActive = index == activeLineIndex
                    val alpha by animateFloatAsState(
                        targetValue = if (isActive) 1f else 0.4f,
                        label = "LyricAlpha"
                    )
                    val scale by animateFloatAsState(
                        targetValue = if (isActive) 1.1f else 1f,
                        label = "LyricScale"
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .graphicsLayer(alpha = alpha, scaleX = scale, scaleY = scale),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = line.content,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        if (!line.translation.isNullOrBlank()) {
                            Text(
                                text = line.translation,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}