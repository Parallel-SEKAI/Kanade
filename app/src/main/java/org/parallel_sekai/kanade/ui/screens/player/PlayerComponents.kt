package org.parallel_sekai.kanade.ui.screens.player

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
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
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.util.concurrent.TimeUnit

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
                .height(64.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, dragAmount ->
                        change.consume()
                        if (dragAmount < -15f) {
                            onIntent(PlayerIntent.Expand)
                        }
                    }
                },
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
                AsyncImage(
                    model = state.currentSong?.coverUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .sharedElement(
                            rememberSharedContentState(key = "album_art"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
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
                        modifier = Modifier.sharedElement(
                            rememberSharedContentState(key = "song_title"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    )
                    Text(
                        text = state.currentSong?.artist ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.sharedElement(
                            rememberSharedContentState(key = "song_artist"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
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
                progressFlow.collect { /* handle back progress */ }
                onIntent(PlayerIntent.Collapse)
            } catch (e: Exception) {
            }
        }

        var showLyrics by remember { mutableStateOf(false) }
        var controlsVisible by remember { mutableStateOf(true) }
        val density = LocalDensity.current

        LaunchedEffect(controlsVisible, state.isPlaying) {
            if (controlsVisible && state.isPlaying) {
                kotlinx.coroutines.delay(5000)
                controlsVisible = false
            }
        }

        Surface(
            modifier = Modifier
                .sharedBounds(
                    rememberSharedContentState(key = "player_container"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
                )
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, dragAmount ->
                        change.consume()
                        if (dragAmount > 15f) {
                            onIntent(PlayerIntent.Collapse)
                        }
                    }
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { controlsVisible = !controlsVisible },
            color = Color.Black
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val screenWidth = maxWidth
                val screenHeight = maxHeight

                val targetArtSize = screenWidth * 0.85f
                val artX = (screenWidth - targetArtSize) / 2

                val controlsAlpha by animateFloatAsState(
                    targetValue = if (controlsVisible) 1f else 0f,
                    animationSpec = tween(500),
                    label = "ControlsAlpha"
                )
                val albumArtSize by animateDpAsState(
                    targetValue = if (showLyrics) 42.dp else targetArtSize,
                    animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow),
                    label = "ArtSize"
                )
                val albumArtCornerRadius by animateDpAsState(
                    targetValue = if (showLyrics) 4.dp else 24.dp,
                    label = "ArtCorner"
                )

                FluidBackground(
                    colors = state.gradientColors,
                    modifier = Modifier.fillMaxSize().alpha(0.8f)
                )
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))

                val albumArtOffset by animateIntOffsetAsState(
                    targetValue = if (showLyrics) {
                        IntOffset(with(density) { 24.dp.roundToPx() }, with(density) { 66.dp.roundToPx() })
                    } else {
                        IntOffset(with(density) { artX.roundToPx() }, with(density) { 140.dp.roundToPx() })
                    }
                )
                val titleOffset by animateIntOffsetAsState(
                    targetValue = if (showLyrics) {
                        IntOffset(with(density) { 76.dp.roundToPx() }, with(density) { 64.dp.roundToPx() })
                    } else {
                        IntOffset(with(density) { artX.roundToPx() }, with(density) { (screenHeight - 380.dp).roundToPx() })
                    }
                )
                val artistOffset by animateIntOffsetAsState(
                    targetValue = if (showLyrics) {
                        IntOffset(with(density) { 76.dp.roundToPx() }, with(density) { 88.dp.roundToPx() })
                    } else {
                        IntOffset(with(density) { artX.roundToPx() }, with(density) { (screenHeight - 340.dp).roundToPx() })
                    }
                )

                AsyncImage(
                    model = state.currentSong?.coverUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .offset { albumArtOffset }
                        .size(albumArtSize)
                        .sharedElement(rememberSharedContentState(key = "album_art"), animatedVisibilityScope)
                        .clip(RoundedCornerShape(albumArtCornerRadius))
                        .background(Color.DarkGray),
                    contentScale = ContentScale.Crop
                )

                Text(
                    text = state.currentSong?.title ?: "",
                    style = if (showLyrics) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier
                        .offset { titleOffset }
                        .width(if (showLyrics) screenWidth - 120.dp else targetArtSize)
                        .sharedElement(rememberSharedContentState(key = "song_title"), animatedVisibilityScope)
                        .alpha(if (showLyrics) 1f else controlsAlpha)
                )

                Text(
                    text = state.currentSong?.artist ?: "",
                    style = if (showLyrics) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleLarge,
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 1,
                    modifier = Modifier
                        .offset { artistOffset }
                        .width(if (showLyrics) screenWidth - 120.dp else targetArtSize)
                        .sharedElement(rememberSharedContentState(key = "song_artist"), animatedVisibilityScope)
                        .alpha(if (showLyrics) 1f else controlsAlpha)
                )

                Column(
                    modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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
                                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End) {
                                    IconButton(onClick = { }) { Icon(Icons.Default.MoreHoriz, null, tint = Color.White) }
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showLyrics,
                            enter = fadeIn(tween(500)) + expandVertically(expandFrom = Alignment.CenterVertically),
                            exit = fadeOut(tween(500)) + shrinkVertically(shrinkTowards = Alignment.CenterVertically)
                        ) {
                            LyricContent(state, onIntent)
                        }
                        
                        if (!showLyrics) {
                            Box(modifier = Modifier
                                .offset { albumArtOffset }
                                .size(albumArtSize)
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
                        visible = controlsVisible,
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
                            Text(
                                text = line.content,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                textAlign = TextAlign.Start,
                                lineHeight = MaterialTheme.typography.headlineLarge.lineHeight * 1.2f,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
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