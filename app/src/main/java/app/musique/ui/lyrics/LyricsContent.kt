package app.musique.ui.lyrics

import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.musique.R
import app.musique.core.model.LyricLine
import app.musique.core.model.LyricRole
import app.musique.core.model.LyricWord
import app.musique.data.setting.LocalUiSettings
import app.musique.ui.Haptics
import app.musique.ui.theme.LocalNowPlayingPalette
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.theme.MusiqueMotion
import app.musique.ui.theme.MusiqueType
import app.musique.ui.theme.pressScale
import app.musique.utils.RomanizationUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Inactive voices are drawn at this share of the active size, from their own alignment edge. */
private const val INACTIVE_SCALE = 0.84f

/** Lyric tone blur is tuned in desktop text-shadow pixels; this maps it onto a Compose blur radius. */
private const val BLUR_TO_DP = 0.6f

/** Long lines leave breathing room on the far side. */
private const val TEXT_WIDTH_SHARE = 0.9f

private val RowPadding = 11.dp
private val SideGutter = 24.dp
private val BackingIndent = 14.dp
private val BackingRule = 2.dp
private val BackingRulePad = 10.dp
private val PressBleed = 12.dp

private val ToneEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

/**
 * The lyrics renderer used by the Now Playing lyrics mode and the lyrics sheet. Stateless about
 * where lyrics come from: give it the [state] to show, the playback [clock] reports and a seek
 * callback. It draws on a transparent background.
 *
 * @param synced false shows timed lyrics as static text (for a track that is not playing, or the
 *   sheet's static tab).
 */
@Composable
fun LyricsContent(
    state: LyricsUiState,
    clock: Flow<PlaybackClockSnapshot>,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    synced: Boolean = true,
    romanize: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(),
    onUserScrollingChange: (Boolean) -> Unit = {},
    onBackgroundTap: () -> Unit = {},
) {
    val currentBackgroundTap by rememberUpdatedState(onBackgroundTap)
    Box(
        modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onTap = { currentBackgroundTap() }) },
    ) {
        updateTransition(state, label = "lyricsState").Crossfade(
            animationSpec = tween(320),
            contentKey = { it.contentKey() },
        ) { shown ->
            when (shown) {
                LyricsUiState.Loading -> LyricsShimmer(contentPadding)
                LyricsUiState.Missing -> LyricsMessage(stringResource(R.string.lyrics_missing), contentPadding)
                LyricsUiState.Episode, LyricsUiState.Idle -> Box(Modifier.fillMaxSize())
                is LyricsUiState.Ready -> LyricsColumn(
                    lines = shown.lines,
                    clock = clock,
                    synced = synced,
                    romanize = romanize,
                    onSeek = onSeek,
                    contentPadding = contentPadding,
                    onUserScrollingChange = onUserScrollingChange,
                )
            }
        }
    }
}

private fun LyricsUiState.contentKey(): Any = when (this) {
    is LyricsUiState.Ready -> "ready:$trackId"
    else -> this::class
}

/** Mutable follow bookkeeping that must not trigger recomposition. */
private class FollowState {
    val spring = LyricFollowSpring()
    val history = ScrollHistory()
    var placed = false
}

@Composable
private fun LyricsColumn(
    lines: List<LyricLine>,
    clock: Flow<PlaybackClockSnapshot>,
    synced: Boolean,
    romanize: Boolean,
    onSeek: (Long) -> Unit,
    contentPadding: PaddingValues,
    onUserScrollingChange: (Boolean) -> Unit,
) {
    val rows = remember(lines) { withInterludes(buildRows(lines)) }
    val isSynced = synced && rows.any { it.synced }
    val layouts = remember(rows) { rows.map(::voiceLayout) }
    val geometries = remember(rows) {
        rows.map { row -> row.voices.map { voice -> buildSweepGeometry(lyricWords(voice, row.startMs)) } }
    }
    val timed = remember(rows) { timedVoices(rows) }
    val romanized = remember(rows, romanize) {
        if (!romanize) null else rows.map { row ->
            row.voices.map { voice ->
                (voice.roman ?: RomanizationUtil.romanize(voice.text)).takeIf { it.isNotBlank() && it != voice.text }
            }
        }
    }

    // Lyric clock: 250ms reports, interpolated every frame while playing.
    val tracker = remember { LyricClockTracker() }
    var clockMs by remember { mutableLongStateOf(0L) }
    var playing by remember { mutableStateOf(false) }
    LaunchedEffect(clock) {
        clock.collect { snapshot ->
            val now = System.nanoTime()
            tracker.onSnapshot(snapshot, now)
            playing = snapshot.isPlaying && !snapshot.isBuffering
            clockMs = tracker.currentMs(now)
        }
    }
    LaunchedEffect(playing) {
        if (!playing) return@LaunchedEffect
        while (isActive) {
            withFrameNanos { clockMs = tracker.currentMs(it) }
        }
    }
    val readClock = remember { { clockMs } }

    val activeState by remember(rows, timed, isSynced) {
        derivedStateOf { activeLyricsAt(rows, timed, clockMs, isSynced) }
    }
    val active = activeState.activeRow
    val distances = remember(rows, active) { rowDistances(rows, active) }

    val palette = LocalNowPlayingPalette.current
    val ink = remember(palette) { lerp(Color.White, palette.accent, 0.12f) }
    val highContrast = LocalUiSettings.current.highContrastCompat
    val blurEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !highContrast
    val reduceMotion = rememberReduceMotion()
    val context = LocalContext.current
    val view = LocalView.current

    val listState = rememberLazyListState()
    val follow = remember { FollowState() }
    var detached by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var cascadeNow by remember { mutableLongStateOf(0L) }

    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is DragInteraction.Start -> {
                    dragging = true
                    detached = true
                    follow.spring.stop()
                    follow.history.clear()
                }
                is DragInteraction.Stop, is DragInteraction.Cancel -> dragging = false
            }
        }
    }
    val userScrolling = dragging || (detached && listState.isScrollInProgress)
    val currentScrollingChange by rememberUpdatedState(onUserScrollingChange)
    LaunchedEffect(userScrolling) { currentScrollingChange(userScrolling) }
    DisposableEffect(Unit) { onDispose { currentScrollingChange(false) } }

    val seek: (Long) -> Unit = { ms ->
        val target = lyricSeekTarget(ms)
        tracker.snap(target, System.nanoTime())
        clockMs = target
        detached = false
        onSeek(target)
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val layoutDirection = LocalLayoutDirection.current
        val padTop = contentPadding.calculateTopPadding()
        val padBottom = contentPadding.calculateBottomPadding()
        val startPad = contentPadding.calculateStartPadding(layoutDirection) + SideGutter
        val endPad = contentPadding.calculateEndPadding(layoutDirection) + SideGutter
        val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val padTopPx = with(density) { padTop.toPx() }
        val padBottomPx = with(density) { padBottom.toPx() }
        val usableHeightPx = (heightPx - padTopPx - padBottomPx).coerceAtLeast(1f)
        val textMaxWidth = ((maxWidth - startPad - endPad) * TEXT_WIDTH_SHARE).coerceAtLeast(0.dp)
        // Read through state so an animating host padding re-targets the spring instead of restarting it.
        val anchorTopPx by rememberUpdatedState(padTopPx)
        val anchorHeightPx by rememberUpdatedState(usableHeightPx)

        // Index the follower centres: the active row, or the first row before singing starts.
        fun followIndex(): Int = max(activeState.activeRow, 0)

        fun remainingPx(): Float? {
            val idx = followIndex()
            val info = listState.layoutInfo
            val items = info.visibleItemsInfo
            val item = items.firstOrNull { it.index == idx } ?: return null
            var top = item.offset
            val bottom = item.offset + item.size
            // A backing vocal from an earlier row still being sung stays in the centred span.
            val firstOverlap = activeState.overlapping.minOfOrNull { it.row }
            if (firstOverlap != null && firstOverlap < idx) {
                items.firstOrNull { it.index == firstOverlap }?.let { top = min(top, it.offset) }
            }
            val topInContainer = (top - info.viewportStartOffset).toFloat()
            val desired = lyricScrollAnchorTopPx(anchorTopPx, anchorHeightPx, (bottom - top).toFloat())
            return topInContainer - desired
        }

        fun activeInBand(): Boolean {
            val info = listState.layoutInfo
            val item = info.visibleItemsInfo.firstOrNull { it.index == followIndex() } ?: return false
            val center = (item.offset - info.viewportStartOffset + item.size / 2).toFloat()
            return isItemInManualReattachBand(center, anchorTopPx, anchorHeightPx)
        }

        // Suspends until the follow target moves by a pixel or more: a new line, a row resizing,
        // or the host padding changing. Waiting on change (not on "off target") keeps the loop
        // idle when the list cannot scroll any further.
        suspend fun awaitTargetMove() {
            val base = remainingPx()
            snapshotFlow { remainingPx() }.first { it == null || base == null || abs(it - base) >= 1f }
        }

        // The reader hands the column back after a quiet spell, sooner if they scrolled the line back into view.
        LaunchedEffect(detached, userScrolling) {
            if (!detached || userScrolling) return@LaunchedEffect
            delay(if (activeInBand()) MANUAL_BAND_REATTACH_MS else IDLE_REATTACH_MS)
            detached = false
        }

        // Follow the sung line: jump on first placement, then one critically damped spring.
        LaunchedEffect(rows, isSynced, detached, reduceMotion) {
            if (!isSynced || detached || rows.isEmpty()) return@LaunchedEffect
            while (isActive) {
                val idx = followIndex()
                if (listState.layoutInfo.visibleItemsInfo.none { it.index == idx }) {
                    follow.placed = false
                    follow.spring.stop()
                    listState.scrollToItem(idx)
                    withFrameNanos { }
                    continue
                }
                val remaining = remainingPx() ?: continue
                if (!follow.placed || reduceMotion) {
                    try {
                        listState.scroll { scrollBy(remaining) }
                    } catch (e: CancellationException) {
                        if (!isActive) throw e
                    }
                    follow.placed = true
                    follow.spring.stop()
                    follow.history.clear()
                    awaitTargetMove()
                    continue
                }
                if (abs(remaining) < 1f && !follow.spring.running) {
                    awaitTargetMove()
                    continue
                }
                var lastFrame = 0L
                try {
                    listState.scroll {
                        while (true) {
                            val now = withFrameNanos { it }
                            val dt = if (lastFrame == 0L) 1f / 60f else (now - lastFrame) / 1e9f
                            lastFrame = now
                            val rem = remainingPx() ?: break
                            val delta = follow.spring.stepDelta(rem, dt)
                            if (delta != 0f) {
                                val consumed = scrollBy(delta)
                                follow.history.record(now, consumed)
                                // Hit the end of the list: nothing left to glide into.
                                if (abs(consumed - delta) > 1f) follow.spring.stop()
                            }
                            cascadeNow = now
                            if (!follow.spring.running) break
                        }
                    }
                } catch (e: CancellationException) {
                    if (!isActive) throw e
                    follow.spring.stop()
                    follow.history.clear()
                }
                // Let the trailing rows catch up before going idle.
                while (isActive && !follow.history.settled(cascadeNow, CASCADE_MAX_DELAY_MS)) {
                    cascadeNow = withFrameNanos { it }
                }
                if (!follow.history.isEmpty) {
                    follow.history.clear()
                    cascadeNow = withFrameNanos { it }
                }
                awaitTargetMove()
            }
        }

        val listTop = padTop + if (isSynced) maxHeight * 0.3f else 24.dp
        val listBottom = padBottom + if (isSynced) maxHeight * 0.7f else 48.dp

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = startPad, end = endPad, top = listTop, bottom = listBottom),
            modifier = Modifier
                .fillMaxSize()
                .lyricsFadeMask(heightPx, padTopPx, padBottomPx, usableHeightPx),
        ) {
            itemsIndexed(
                rows,
                key = { index, _ -> index },
                contentType = { _, row -> if (row.isInterlude) 1 else 0 },
            ) { index, row ->
                val state = activeState
                val isActiveRow = isSynced && index == state.activeRow
                if (row.isInterlude) {
                    LyricInterlude(
                        startMs = row.startMs,
                        endMs = row.endMs,
                        active = isActiveRow,
                        clock = readClock,
                        ink = ink,
                        reduceMotion = reduceMotion,
                        contentDescription = stringResource(R.string.lyrics_instrumental),
                    )
                } else {
                    val singing = state.singing.filter { it.row == index }.mapTo(HashSet()) { it.voice }
                    val activeRow = state.activeRow
                    LyricRowItem(
                        row = row,
                        layouts = layouts[index],
                        geometries = geometries[index],
                        romanized = romanized?.get(index),
                        synced = isSynced,
                        active = isActiveRow,
                        singing = singing,
                        distance = distances.getOrElse(index) { 0 },
                        past = index < activeRow,
                        detached = detached,
                        blurEnabled = blurEnabled,
                        highContrast = highContrast,
                        reduceMotion = reduceMotion,
                        ink = ink,
                        textMaxWidth = textMaxWidth,
                        clock = readClock,
                        cascade = {
                            if (reduceMotion || detached) 0f
                            else follow.history.lagPx(cascadeNow, cascadeTrailDelayMs(index, activeRow))
                        },
                        onSeek = if (isSynced) {
                            {
                                Haptics.textHandleMove(context, view)
                                seek(row.startMs)
                            }
                        } else null,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isSynced && detached,
            enter = fadeIn(tween(180)) + slideInVertically(tween(180)) { it / 3 },
            exit = fadeOut(tween(120)) + slideOutVertically(tween(120)) { it / 3 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = padBottom + 16.dp),
        ) {
            BackToLinePill(onClick = { detached = false })
        }
    }
}

@Composable
private fun LyricRowItem(
    row: LyricRow,
    layouts: List<VoiceLayout>,
    geometries: List<LineSweepGeometry>,
    romanized: List<String?>?,
    synced: Boolean,
    active: Boolean,
    singing: Set<Int>,
    distance: Int,
    past: Boolean,
    detached: Boolean,
    blurEnabled: Boolean,
    highContrast: Boolean,
    reduceMotion: Boolean,
    ink: Color,
    textMaxWidth: Dp,
    clock: () -> Long,
    cascade: () -> Float,
    onSeek: (() -> Unit)?,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && !reduceMotion) 0.985f else 1f,
        animationSpec = MusiqueMotion.press(),
        label = "lyricPress",
    )
    val pressFill by animateFloatAsState(
        targetValue = if (pressed) 0.08f else 0f,
        animationSpec = tween(160),
        label = "lyricPressFill",
    )

    Column(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = cascade()
                scaleX = pressScale
                scaleY = pressScale
            }
            .padding(vertical = RowPadding),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val count = row.voices.size
        row.voices.forEachIndexed { vi, voice ->
            val layout = layouts[vi]
            val timedVoice = !voice.words.isNullOrEmpty() || voice.endMs != null
            val voiceActive = synced && if (timedVoice) vi in singing else active
            val collapsed = synced && layout.secondary && !voiceActive
            val tone = when {
                !synced -> Tone(blurDp = 0f, alpha = 0.92f)
                voiceActive -> Tone(blurDp = 0f, alpha = 1f)
                detached -> lyricTone(distance, past, highContrast).let { Tone(0f, max(0.65f, it.alpha)) }
                else -> lyricTone(distance, past, highContrast)
            }
            val right = layout.side == VoiceSide.RIGHT
            val originY = when {
                count == 1 -> 0.5f
                vi == 0 -> 1f
                vi == count - 1 -> 0f
                else -> 0.5f
            }
            val alpha by animateFloatAsState(
                targetValue = tone.alpha * if (layout.secondary) 0.78f else 1f,
                animationSpec = if (reduceMotion) snap() else tween(450, easing = ToneEasing),
                label = "lyricAlpha",
            )
            val scale by animateFloatAsState(
                targetValue = if (!synced || voiceActive) 1f else INACTIVE_SCALE,
                animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.92f, stiffness = 240f),
                label = "lyricScale",
            )
            val blur by animateFloatAsState(
                targetValue = if (blurEnabled && synced && !detached && !voiceActive) tone.blurDp * BLUR_TO_DP else 0f,
                animationSpec = if (reduceMotion) snap() else tween(450, easing = ToneEasing),
                label = "lyricBlur",
            )

            val content: @Composable () -> Unit = {
                Box(
                    Modifier.fillMaxWidth(),
                    contentAlignment = if (right) Alignment.TopEnd else Alignment.TopStart,
                ) {
                    Column(
                        Modifier
                            .widthIn(max = textMaxWidth)
                            .then(if (layout.secondary) Modifier.backingInset(right) else Modifier)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                this.alpha = alpha
                                transformOrigin = TransformOrigin(if (right) 1f else 0f, originY)
                            }
                            .then(
                                if (blur > 0.05f) Modifier.blur(blur.dp, BlurredEdgeTreatment.Unbounded)
                                else Modifier,
                            )
                            .pressPlate { pressFill }
                            .then(
                                if (onSeek != null) Modifier.clickable(
                                    interactionSource = interaction,
                                    indication = null,
                                    onClick = onSeek,
                                ) else Modifier,
                            ),
                        horizontalAlignment = if (right) Alignment.End else Alignment.Start,
                    ) {
                        LyricVoiceText(
                            geometry = geometries[vi],
                            style = if (layout.secondary) MusiqueType.lyricBackground else MusiqueType.lyricLine,
                            align = if (right) TextAlign.End else TextAlign.Start,
                            ink = ink,
                            sweeping = voiceActive,
                            clock = clock,
                        )
                        val roman = romanized?.getOrNull(vi)
                        if (roman != null) {
                            Text(
                                text = roman,
                                style = MusiqueType.lyricTranslation,
                                color = ink.copy(alpha = if (voiceActive || !synced) 0.75f else 0.35f),
                                textAlign = if (right) TextAlign.End else TextAlign.Start,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }

            if (layout.secondary && synced) {
                AnimatedVisibility(
                    visible = !collapsed,
                    enter = if (reduceMotion) fadeIn(snap()) else
                        expandVertically(tween(380, easing = FastOutSlowInEasing)) + fadeIn(tween(420)),
                    exit = if (reduceMotion) fadeOut(snap()) else
                        shrinkVertically(tween(380, easing = FastOutSlowInEasing)) + fadeOut(tween(260)),
                ) { content() }
            } else {
                content()
            }
        }
    }
}

/** A backing vocal hangs off its singer's side: indented from that edge, with a thin rule on it. */
private fun Modifier.backingInset(right: Boolean): Modifier =
    padding(start = if (right) 0.dp else BackingIndent, end = if (right) BackingIndent else 0.dp)
        .drawBehind {
            val w = BackingRule.toPx()
            val x = if (right) size.width - w else 0f
            drawRect(Color.White.copy(alpha = 0.2f), topLeft = Offset(x, 0f), size = Size(w, size.height))
        }
        .padding(start = if (right) 0.dp else BackingRulePad, end = if (right) BackingRulePad else 0.dp)

/** The soft plate a lyric line shows while pressed, bleeding past the text. */
private fun Modifier.pressPlate(fill: () -> Float): Modifier = drawBehind {
    val a = fill()
    if (a <= 0f) return@drawBehind
    val bleed = PressBleed.toPx()
    drawRoundRect(
        color = Color.White.copy(alpha = a),
        topLeft = Offset(-bleed, -bleed / 2f),
        size = Size(size.width + bleed * 2f, size.height + bleed),
        cornerRadius = CornerRadius(14.dp.toPx()),
    )
}

/**
 * Fades the column out under the compact header and above the controls (DstIn alpha mask):
 * 8% of the usable height fades in at the top, 16% fades out at the bottom.
 */
private fun Modifier.lyricsFadeMask(
    heightPx: Float,
    padTopPx: Float,
    padBottomPx: Float,
    usableHeightPx: Float,
): Modifier {
    val topStart = (padTopPx / heightPx).coerceIn(0f, 1f)
    val topEnd = ((padTopPx + usableHeightPx * 0.08f) / heightPx).coerceIn(topStart, 1f)
    val bottomEnd = ((heightPx - padBottomPx) / heightPx).coerceIn(topEnd, 1f)
    val bottomStart = ((heightPx - padBottomPx - usableHeightPx * 0.16f) / heightPx).coerceIn(topEnd, bottomEnd)
    val brush = Brush.verticalGradient(
        0f to Color.Transparent,
        topStart to Color.Transparent,
        topEnd to Color.Black,
        bottomStart to Color.Black,
        bottomEnd to Color.Transparent,
        1f to Color.Transparent,
    )
    return graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            drawRect(brush, blendMode = BlendMode.DstIn)
        }
}

/** Frosted "Back to current line" capsule shown while the reader has scrolled away. */
@Composable
private fun BackToLinePill(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(17.dp)
    Row(
        Modifier
            .pressScale(interaction, pressedScale = 0.94f)
            .height(34.dp)
            .clip(shape)
            .background(Color(0xC7181620))
            .border(1.dp, Color.White.copy(alpha = 0.15f), shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.lyrics_back_to_current),
            color = Color.White.copy(alpha = 0.95f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            style = MusiqueType.caption,
        )
        Icon(
            imageVector = Icons.Rounded.ArrowDownward,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.95f),
            modifier = Modifier.size(14.dp),
        )
    }
}

/** Placeholder bars that breathe while lyrics load. */
@Composable
private fun LyricsShimmer(contentPadding: PaddingValues) {
    val reduceMotion = rememberReduceMotion()
    val transition = rememberInfiniteTransition(label = "lyricsShimmer")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "lyricsShimmerPhase",
    )
    val description = stringResource(R.string.lyrics_loading)
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .semantics { contentDescription = description },
    ) {
        val widths = listOf(0.86f, 0.62f, 0.78f, 0.5f, 0.7f)
        Column(
            Modifier
                .padding(horizontal = SideGutter)
                .padding(top = maxHeight * 0.22f),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            widths.forEachIndexed { i, w ->
                val local = if (reduceMotion) 0.5f else ((phase + i * 0.18f) % 1f)
                Box(
                    Modifier
                        .fillMaxWidth(w * TEXT_WIDTH_SHARE)
                        .height(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.06f + 0.08f * local)),
                )
            }
        }
    }
}

@Composable
private fun LyricsMessage(text: String, contentPadding: PaddingValues) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = SideGutter),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MusiqueType.sectionTitle,
            color = MusiqueColors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

/** True when the system animator scale is off ("Remove animations"). */
@Composable
private fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        try {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        } catch (_: Exception) {
            false
        }
    }
}

// Previews

private fun previewWords(start: Long, vararg parts: Pair<String, Long>): List<LyricWord> {
    var t = start
    return parts.map { (text, dur) -> LyricWord(t, t + dur, text).also { t += dur } }
}

internal val PreviewLyrics: List<LyricLine> = listOf(
    LyricLine(
        timestampMs = 4_000L, text = "City lights are falling slow",
        words = previewWords(4_000L, "City " to 400, "lights " to 450, "are " to 250, "falling " to 600, "slow" to 900),
    ),
    LyricLine(
        timestampMs = 7_200L, text = "Every window glowing gold",
        words = previewWords(7_200L, "Every " to 450, "window " to 500, "glowing " to 550, "gold" to 1_000),
    ),
    LyricLine(
        timestampMs = 7_600L, text = "(glowing gold)", role = LyricRole.BG,
        words = previewWords(7_600L, "(glowing " to 600, "gold)" to 900),
    ),
    LyricLine(
        timestampMs = 10_400L, text = "Tell me that you hear it too", role = LyricRole.DUET,
        words = previewWords(10_400L, "Tell " to 300, "me " to 200, "that " to 250, "you " to 300, "hear " to 400, "it " to 250, "too" to 900),
    ),
    LyricLine(timestampMs = 13_800L, text = "♪"),
    LyricLine(timestampMs = 22_000L, text = "And we run until the morning comes"),
    LyricLine(timestampMs = 25_500L, text = "Hearts like drums"),
)

@Composable
private fun PreviewFrame(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF3B2A4F), Color(0xFF120E18)))),
    ) { content() }
}

@Preview(name = "Synced lyrics, mid line", widthDp = 390, heightDp = 780, showBackground = true)
@Composable
private fun SyncedLyricsPreview() {
    PreviewFrame {
        LyricsContent(
            state = LyricsUiState.Ready("preview", PreviewLyrics),
            clock = flowOf(PlaybackClockSnapshot(trackId = "preview", basePositionMs = 8_100L, durationMs = 200_000L)),
            onSeek = {},
            contentPadding = PaddingValues(top = 72.dp, bottom = 120.dp),
        )
    }
}

@Preview(name = "Static lyrics", widthDp = 390, heightDp = 780, showBackground = true)
@Composable
private fun StaticLyricsPreview() {
    PreviewFrame {
        LyricsContent(
            state = LyricsUiState.Ready(
                "static",
                PreviewLyrics.filter { it.text != "♪" }.map { it.copy(timestampMs = -1L, words = null, synced = false) },
            ),
            clock = flowOf(PlaybackClockSnapshot()),
            onSeek = {},
            contentPadding = PaddingValues(top = 72.dp, bottom = 120.dp),
        )
    }
}

@Preview(name = "Loading and missing", widthDp = 390, heightDp = 400, showBackground = true)
@Composable
private fun LyricsStatesPreview() {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            PreviewFrame {
                LyricsContent(state = LyricsUiState.Loading, clock = flowOf(PlaybackClockSnapshot()), onSeek = {})
            }
        }
        Spacer(Modifier.width(1.dp))
        Box(Modifier.weight(1f)) {
            PreviewFrame {
                LyricsContent(state = LyricsUiState.Missing, clock = flowOf(PlaybackClockSnapshot()), onSeek = {})
            }
        }
    }
}
