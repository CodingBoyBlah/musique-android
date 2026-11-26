package app.musique.ui.components.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.musique.ALBUM_COVER_URL
import app.musique.R
import app.musique.core.model.CoverSize
import app.musique.core.model.getCover
import app.musique.data.setting.LocalUiSettings
import app.musique.ui.Haptics
import app.musique.ui.components.SmartImage
import app.musique.ui.theme.ArtworkPalette
import app.musique.ui.theme.LocalNowPlayingPalette
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.theme.MusiqueDimens
import app.musique.ui.theme.MusiqueMotion
import app.musique.ui.theme.MusiqueShapes
import app.musique.ui.theme.MusiqueType
import app.musique.ui.theme.pressScale
import app.musique.ui.viewmodel.player.MiniPlayerViewModel
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

private enum class MiniDragAxis { None, Horizontal, VerticalUp, VerticalDown }

private const val HorizontalRubberBandFactor = 0.35f
private const val VerticalDismissFollowFactor = 0.75f
private val SkipSwipeThreshold = 72.dp
private val DismissSwipeThreshold = 40.dp
private val FlingVelocityThreshold = 450.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
fun MiniPlayer(
    viewModel: MiniPlayerViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: PlayerSheetState? = null,
    onClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val currentAudio by viewModel.currentAudio.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState(initial = false)
    val isBuffering by viewModel.isBuffering().collectAsState(initial = false)
    val currentTime by viewModel.positionMs.collectAsStateWithLifecycle()
    val spirc = viewModel.spirc

    val totalTime = currentAudio?.duration ?: 0L
    val progress = if (totalTime > 0L) (currentTime.toFloat() / totalTime).coerceIn(0f, 1f) else 0f
    val artworkUrl = currentAudio?.getCover(CoverSize.SMALL)?.uri?.let { ALBUM_COVER_URL + it }

    val title = currentAudio?.name ?: stringResource(R.string.player_nothing_playing)
    val subtitle = currentAudio?.artists?.joinToString { it.name }?.takeIf { it.isNotBlank() }
        ?: currentAudio?.showName
        ?: stringResource(R.string.player_unknown_source)

    val coroutineScope = rememberCoroutineScope()
    val dragOffsetX = remember { Animatable(0f) }
    val dismissOffsetY = remember { Animatable(0f) }
    val interactive by remember(sheetState) {
        derivedStateOf { (sheetState?.progress ?: 0f) < 0.25f }
    }

    val latestOnClick by rememberUpdatedState(onClick)
    val latestOnDismiss by rememberUpdatedState(onDismiss)

    val cardInteractionSource = remember { MutableInteractionSource() }
    val miniPlayerCd = stringResource(R.string.cd_mini_player)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MusiqueDimens.miniPlayerSideMargin)
            .pointerInput(sheetState, spirc) {
                val skipThresholdPx = SkipSwipeThreshold.toPx()
                val dismissThresholdPx = DismissSwipeThreshold.toPx()
                val flingVelocityPx = FlingVelocityThreshold.toPx()
                val fastHorizMinPx = 32.dp.toPx()
                val fastDownMinPx = 20.dp.toPx()
                val touchSlop = viewConfiguration.touchSlop

                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial,
                    )
                    if ((sheetState?.progress ?: 0f) >= 0.25f) return@awaitEachGesture

                    var activePointerId = down.id
                    var axis = MiniDragAxis.None
                    var totalDx = 0f
                    var totalDy = 0f
                    var rawDragX = dragOffsetX.value / HorizontalRubberBandFactor
                    var rawDragY = 0f
                    var prevLocalX = down.position.x
                    // Add the sheet's current translation so upward drags track screen coordinates
                    // cleanly without oscillating as PlayerSheet translates with rawOffset.
                    fun currentSheetY(localY: Float): Float {
                        val sheetOffset = sheetState?.draggableState?.offset?.takeIf { !it.isNaN() } ?: 0f
                        return localY + sheetOffset
                    }

                    var prevSheetY = currentSheetY(down.position.y)
                    var horizontalArmed = false
                    var dismissArmed = false
                    var releasedCleanly = false
                    val velocityTracker = VelocityTracker()
                    velocityTracker.addPosition(down.uptimeMillis, Offset(down.position.x, prevSheetY))

                    try {
                        while (true) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == activePointerId }
                                ?: event.changes.firstOrNull { it.pressed }
                                ?: break
                            activePointerId = change.id

                            val sheetY = currentSheetY(change.position.y)
                            val dx = change.position.x - prevLocalX
                            val dy = sheetY - prevSheetY
                            prevLocalX = change.position.x
                            prevSheetY = sheetY
                            velocityTracker.addPosition(change.uptimeMillis, Offset(change.position.x, sheetY))

                            if (axis == MiniDragAxis.None) {
                                totalDx += dx
                                totalDy += dy
                                if (hypot(totalDx, totalDy) > touchSlop) {
                                    axis = when {
                                        abs(totalDx) > abs(totalDy) -> MiniDragAxis.Horizontal
                                        totalDy < 0f -> MiniDragAxis.VerticalUp
                                        else -> MiniDragAxis.VerticalDown
                                    }
                                    change.consume()
                                    when (axis) {
                                        MiniDragAxis.Horizontal -> {
                                            rawDragX += totalDx
                                            coroutineScope.launch {
                                                dragOffsetX.snapTo(rawDragX * HorizontalRubberBandFactor)
                                            }
                                            horizontalArmed = abs(rawDragX) >= skipThresholdPx
                                            if (horizontalArmed) Haptics.swipeArm(context, view)
                                        }
                                        MiniDragAxis.VerticalDown -> {
                                            rawDragY = max(0f, totalDy)
                                            coroutineScope.launch {
                                                dismissOffsetY.snapTo(rawDragY * VerticalDismissFollowFactor)
                                            }
                                            dismissArmed = rawDragY >= dismissThresholdPx
                                            if (dismissArmed) Haptics.swipeArm(context, view)
                                        }
                                        MiniDragAxis.VerticalUp -> {
                                            if (sheetState != null) {
                                                sheetState.draggableState.dispatchRawDelta(totalDy)
                                            } else {
                                                rawDragY = totalDy
                                            }
                                        }
                                        MiniDragAxis.None -> Unit
                                    }
                                }
                            } else {
                                change.consume()
                                when (axis) {
                                    MiniDragAxis.Horizontal -> {
                                        rawDragX += dx
                                        coroutineScope.launch {
                                            dragOffsetX.snapTo(rawDragX * HorizontalRubberBandFactor)
                                        }
                                        val nowArmed = abs(rawDragX) >= skipThresholdPx
                                        if (nowArmed && !horizontalArmed) {
                                            Haptics.swipeArm(context, view)
                                        }
                                        horizontalArmed = nowArmed
                                    }
                                    MiniDragAxis.VerticalDown -> {
                                        rawDragY = max(0f, rawDragY + dy)
                                        coroutineScope.launch {
                                            dismissOffsetY.snapTo(rawDragY * VerticalDismissFollowFactor)
                                        }
                                        val nowArmed = rawDragY >= dismissThresholdPx
                                        if (nowArmed && !dismissArmed) {
                                            Haptics.swipeArm(context, view)
                                        }
                                        dismissArmed = nowArmed
                                    }
                                    MiniDragAxis.VerticalUp -> {
                                        if (sheetState != null) {
                                            sheetState.draggableState.dispatchRawDelta(dy)
                                        } else {
                                            rawDragY += dy
                                        }
                                    }
                                    MiniDragAxis.None -> Unit
                                }
                            }

                            if (change.changedToUpIgnoreConsumed() && event.changes.none { it.pressed }) {
                                releasedCleanly = true
                                break
                            }
                        }
                    } finally {
                        val velocity = velocityTracker.calculateVelocity()
                        when (axis) {
                            MiniDragAxis.Horizontal -> {
                                if (releasedCleanly) {
                                    val fastLeft = velocity.x < -flingVelocityPx && rawDragX < -fastHorizMinPx
                                    val fastRight = velocity.x > flingVelocityPx && rawDragX > fastHorizMinPx
                                    when {
                                        rawDragX <= -skipThresholdPx || fastLeft -> {
                                            if (!horizontalArmed) Haptics.swipeArm(context, view)
                                            spirc.playerNext()
                                        }
                                        rawDragX >= skipThresholdPx || fastRight -> {
                                            if (!horizontalArmed) Haptics.swipeArm(context, view)
                                            spirc.playerPrevious()
                                        }
                                    }
                                }
                                coroutineScope.launch {
                                    dragOffsetX.animateTo(0f, animationSpec = MusiqueMotion.press())
                                }
                            }
                            MiniDragAxis.VerticalDown -> {
                                val fastDown = velocity.y > flingVelocityPx && rawDragY > fastDownMinPx
                                if (releasedCleanly && (rawDragY >= dismissThresholdPx || fastDown)) {
                                    Haptics.confirm(context, view)
                                    latestOnDismiss()
                                } else {
                                    coroutineScope.launch {
                                        dismissOffsetY.animateTo(0f, animationSpec = MusiqueMotion.press())
                                    }
                                }
                            }
                            MiniDragAxis.VerticalUp -> {
                                if (sheetState != null) {
                                    coroutineScope.launch {
                                        if (releasedCleanly) sheetState.settle(velocity.y) else sheetState.collapse()
                                    }
                                } else if (releasedCleanly && (rawDragY < -dismissThresholdPx || velocity.y < -flingVelocityPx)) {
                                    latestOnClick?.invoke()
                                }
                            }
                            MiniDragAxis.None -> Unit
                        }
                    }
                }
            },
    ) {
        MiniPlayerCard(
            artworkUrl = artworkUrl,
            title = title,
            subtitle = subtitle,
            trackKey = currentAudio?.uri,
            progress = progress,
            isPlaying = isPlaying,
            isBuffering = isBuffering,
            monochromeArtwork = LocalUiSettings.current.monochromePlayer,
            interactive = interactive,
            cardInteractionSource = cardInteractionSource,
            onCardClick = { latestOnClick?.invoke() },
            onPlayPause = {
                viewModel.setAudio(currentAudio)
                spirc.playerPlayPause()
                Haptics.toggle(context, view)
            },
            onNext = {
                spirc.playerNext()
                Haptics.textHandleMove(context, view)
            },
            modifier = Modifier
                .graphicsLayer {
                    translationX = dragOffsetX.value
                    translationY = dismissOffsetY.value
                    val dismissRangePx = DismissSwipeThreshold.toPx().coerceAtLeast(1f)
                    alpha = (1f - (dismissOffsetY.value / dismissRangePx) * 0.28f).coerceIn(0.6f, 1f)
                }
                .semantics { contentDescription = miniPlayerCd },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
private fun MiniPlayerCard(
    artworkUrl: String?,
    title: String,
    subtitle: String,
    trackKey: String?,
    progress: Float,
    isPlaying: Boolean,
    isBuffering: Boolean,
    monochromeArtwork: Boolean,
    interactive: Boolean,
    cardInteractionSource: MutableInteractionSource,
    onCardClick: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalNowPlayingPalette.current
    val playInteractionSource = remember { MutableInteractionSource() }
    val nextInteractionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(MusiqueDimens.miniPlayerHeight)
            .pressScale(cardInteractionSource, pressedScale = 0.985f)
            .clickable(
                enabled = interactive,
                interactionSource = cardInteractionSource,
                indication = null,
                role = Role.Button,
                onClick = onCardClick,
            ),
        shape = MusiqueShapes.miniPlayer,
        color = palette.surface,
        border = BorderStroke(1.dp, MusiqueColors.hairline),
        tonalElevation = 0.dp,
        shadowElevation = 12.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 9.dp, end = 6.dp),
        ) {
            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center,
            ) {
                SmartImage(
                    url = artworkUrl,
                    contentDescription = stringResource(R.string.common_artwork),
                    shape = MusiqueShapes.artworkMini,
                    monochrome = monochromeArtwork,
                    modifier = Modifier.fillMaxSize(),
                )
                if (isBuffering) {
                    ContainedLoadingIndicator(
                        modifier = Modifier
                            .size(44.dp)
                            .alpha(0.65f),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
            ) {
                Text(
                    text = title,
                    style = MusiqueType.miniTitle,
                    color = MusiqueColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.then(
                        if (isPlaying) {
                            Modifier.basicMarquee(
                                iterations = Int.MAX_VALUE,
                                initialDelayMillis = 2_000,
                            )
                        } else {
                            Modifier
                        }
                    ),
                )

                Text(
                    text = subtitle,
                    style = MusiqueType.miniSubtitle,
                    color = MusiqueColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(3.dp))

                MiniInlineProgressBar(
                    progress = progress,
                    isPlaying = isPlaying,
                    trackKey = trackKey,
                    activeColor = palette.accent,
                    inactiveColor = MusiqueColors.trackInactive,
                )
            }

            Surface(
                modifier = Modifier
                    .size(width = 56.dp, height = 44.dp)
                    .pressScale(playInteractionSource)
                    .clickable(
                        enabled = interactive,
                        interactionSource = playInteractionSource,
                        indication = null,
                        role = Role.Button,
                        onClick = onPlayPause,
                    ),
                shape = MusiqueShapes.miniPlayerPlayButton,
                color = palette.accent,
                contentColor = palette.onAccent,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AnimatedContent(
                        targetState = isPlaying,
                        transitionSpec = {
                            (scaleIn(animationSpec = MusiqueMotion.press()) + fadeIn()) togetherWith
                                (scaleOut() + fadeOut())
                        },
                        label = "miniPlayPauseIcon",
                    ) { playing ->
                        Icon(
                            imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(
                                if (playing) R.string.common_pause else R.string.common_play
                            ),
                            tint = palette.onAccent,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(2.dp))

            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 44.dp)
                    .pressScale(nextInteractionSource)
                    .clickable(
                        enabled = interactive,
                        interactionSource = nextInteractionSource,
                        indication = null,
                        role = Role.Button,
                        onClick = onNext,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = stringResource(R.string.common_next),
                    tint = MusiqueColors.textPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

/**
 * Non-draggable inline progress indicator under the MiniPlayer text. Shows an animated sine wave
 * while playing and flattens smoothly into a straight line when paused.
 */
@Composable
private fun MiniInlineProgressBar(
    progress: Float,
    isPlaying: Boolean,
    trackKey: String?,
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier,
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val animatedProgress = remember(trackKey) { Animatable(clampedProgress) }
    LaunchedEffect(trackKey, clampedProgress) {
        if (abs(clampedProgress - animatedProgress.value) > 0.2f) {
            animatedProgress.snapTo(clampedProgress)
        } else {
            animatedProgress.animateTo(
                targetValue = clampedProgress,
                animationSpec = tween(durationMillis = 240, easing = LinearEasing),
            )
        }
    }

    val waveAmplitude by animateDpAsState(
        targetValue = if (isPlaying) 1.5.dp else 0.dp,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "miniWaveAmplitude",
    )

    val phaseShift = remember { Animatable(0f) }
    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        val fullCycle = (2.0 * PI).toFloat()
        while (true) {
            val start = (phaseShift.value % fullCycle).let { if (it < 0f) it + fullCycle else it }
            phaseShift.snapTo(start)
            phaseShift.animateTo(
                targetValue = start + fullCycle,
                animationSpec = tween(durationMillis = 1_600, easing = LinearEasing),
            )
        }
    }

    val wavePath = remember { Path() }
    val progressLabel = stringResource(R.string.cd_mini_player_progress)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .semantics { contentDescription = progressLabel },
    ) {
        val strokePx = 2.dp.toPx()
        val endCapRadius = 1.5.dp.toPx()
        val startX = strokePx / 2f
        val endX = (size.width - endCapRadius).coerceAtLeast(startX)
        val usableWidth = (endX - startX).coerceAtLeast(0f)
        val centerY = size.height / 2f
        val progressX = startX + usableWidth * animatedProgress.value.coerceIn(0f, 1f)

        if (progressX < endX) {
            drawLine(
                color = inactiveColor,
                start = Offset(progressX, centerY),
                end = Offset(endX, centerY),
                strokeWidth = strokePx,
                cap = StrokeCap.Round,
            )
        }
        drawCircle(
            color = inactiveColor,
            radius = endCapRadius,
            center = Offset(endX, centerY),
        )

        if (progressX > startX) {
            val amplitudePx = waveAmplitude.toPx()
            val waveLengthPx = 20.dp.toPx()
            if (amplitudePx > 0.1f && waveLengthPx > 0f) {
                val freq = ((2.0 * PI) / waveLengthPx).toFloat()
                val step = 1.5.dp.toPx().coerceAtLeast(1.5f)
                val phase = phaseShift.value

                wavePath.reset()
                var prevX = startX
                var prevY = centerY + amplitudePx * sin(freq * prevX + phase)
                wavePath.moveTo(prevX, prevY)

                var x = prevX + step
                while (x < progressX) {
                    val y = centerY + amplitudePx * sin(freq * x + phase)
                    wavePath.quadraticTo(prevX, prevY, (prevX + x) * 0.5f, (prevY + y) * 0.5f)
                    prevX = x
                    prevY = y
                    x += step
                }
                val finalY = centerY + amplitudePx * sin(freq * progressX + phase)
                wavePath.quadraticTo(prevX, prevY, progressX, finalY)

                drawPath(
                    path = wavePath,
                    color = activeColor,
                    style = Stroke(
                        width = strokePx,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            } else {
                drawLine(
                    color = activeColor,
                    start = Offset(startX, centerY),
                    end = Offset(progressX, centerY),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun MiniPlayerPreview() {
    CompositionLocalProvider(
        LocalNowPlayingPalette provides ArtworkPalette.Default.copy(
            surface = Color(0xFF464039),
            accent = Color(0xFFEED8C4),
            onAccent = Color(0xFF221B15),
        )
    ) {
        Box(
            modifier = Modifier
                .background(MusiqueColors.canvas)
                .padding(vertical = 16.dp),
        ) {
            MiniPlayerCard(
                artworkUrl = null,
                title = "blue eyes",
                subtitle = "Nohidea",
                trackKey = "preview",
                progress = 0.62f,
                isPlaying = true,
                isBuffering = false,
                monochromeArtwork = false,
                interactive = true,
                cardInteractionSource = remember { MutableInteractionSource() },
                onCardClick = {},
                onPlayPause = {},
                onNext = {},
                modifier = Modifier.padding(horizontal = MusiqueDimens.miniPlayerSideMargin),
            )
        }
    }
}
