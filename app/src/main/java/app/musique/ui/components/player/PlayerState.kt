package app.musique.ui.components.player

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import app.musique.ui.theme.MusiqueDimens

/** Mini chrome fades out over this slice of the open progress. */
private const val MiniFadeEnd = 0.25f

/** Full Now Playing fades in over this slice of the open progress. */
private const val FullFadeStart = 0.4f

/** How far (as open progress) the sheet has to travel down before its corners are fully round. */
private const val CornerRoundingRange = 0.12f

/** Maps the [start]..[end] slice of an open progress value to 0..1. */
private fun Float.slice(start: Float, end: Float) = ((this - start) / (end - start)).coerceIn(0f, 1f)

/**
 * Full-screen Now Playing sheet with the mini player as its collapsed handle.
 *
 * The mini player owns its own gestures (tap, swipe up, sideways skip, swipe down dismiss) and
 * drives [sheetState] directly, so this container adds no drag detector of its own on top of it.
 * Once open, dragging the Now Playing list down past its top pulls the sheet down through
 * nested scroll; it closes past [MusiqueDimens.sheetDismissThreshold] or on a fast fling.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayerSheet(
    sheetState: PlayerSheetState,
    listState: LazyListState,
    miniContent: @Composable () -> Unit,
    fullContent: @Composable (progress: () -> Float) -> Unit,
    modifier: Modifier = Modifier,
    miniPlayerHeight: Dp = MusiqueDimens.miniPlayerHeight,
    collapsedBottomInset: Dp = MusiqueDimens.miniPlayerBottomInset,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val miniHeightPx = with(density) { miniPlayerHeight.toPx() }
        val insetPx = with(density) { collapsedBottomInset.toPx() }
        val collapsedOffset = screenHeightPx - miniHeightPx - insetPx
        val cornerRadiusPx = with(density) { MusiqueDimens.sheetCornerDragRadius.toPx() }

        LaunchedEffect(collapsedOffset) {
            sheetState.draggableState.updateAnchors(
                DraggableAnchors {
                    PlayerSheetValue.Expanded at 0f
                    PlayerSheetValue.Collapsed at collapsedOffset
                }
            )
        }

        // Only these two flips recompose; per-frame values are read inside graphicsLayer blocks.
        val showFull by remember(sheetState) { derivedStateOf { sheetState.progress > 0.01f } }
        // The mini player stays composed (invisible past MiniFadeEnd) until the sheet is fully
        // open, so a swipe up that started on it keeps tracking the finger to the end.
        val showMini by remember(sheetState) { derivedStateOf { sheetState.progress < 0.99f } }

        val nestedScrollConnection = remember(sheetState, listState) {
            object : NestedScrollConnection {
                private fun listAtTop() =
                    listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0

                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    val delta = available.y
                    val sheetMoved = sheetState.progress < 0.999f
                    // Pulling down from the top of the list, or any scroll while the sheet is
                    // already part way down, moves the sheet instead of the list.
                    if (source == NestedScrollSource.UserInput &&
                        ((delta > 0 && sheetState.isExpanded && listAtTop()) || (delta < 0 && sheetMoved))
                    ) {
                        return Offset(0f, sheetState.draggableState.dispatchRawDelta(delta))
                    }
                    return Offset.Zero
                }

                override fun onPostScroll(
                    consumed: Offset, available: Offset, source: NestedScrollSource
                ): Offset {
                    if (source == NestedScrollSource.UserInput && available.y > 0) {
                        return Offset(0f, sheetState.draggableState.dispatchRawDelta(available.y))
                    }
                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    val midDrag = sheetState.progress > 0.001f && sheetState.progress < 0.999f
                    val fastFlingDown = available.y > sheetState.flingVelocityPx &&
                        sheetState.isExpanded && listAtTop()

                    if (midDrag || fastFlingDown) {
                        sheetState.settle(available.y)
                        return available
                    }
                    return Velocity.Zero
                }

                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity
                ): Velocity {
                    if (sheetState.progress > 0.001f && sheetState.progress < 0.999f) {
                        sheetState.settle(available.y)
                    }
                    return Velocity.Zero
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = sheetState.draggableState.offset.takeIf { !it.isNaN() } ?: collapsedOffset
                }
                .nestedScroll(nestedScrollConnection)
        ) {
            if (showFull) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val progress = sheetState.progress
                            alpha = progress.slice(FullFadeStart, 1f)
                            // Round the top corners as the open sheet is pulled down, like a card.
                            val pulled = (1f - progress).slice(0f, CornerRoundingRange)
                            val radius = cornerRadiusPx * pulled
                            if (radius > 0.5f) {
                                shape = RoundedCornerShape(topStart = radius, topEnd = radius)
                                clip = true
                            } else {
                                shape = RectangleShape
                                clip = false
                            }
                        }
                ) {
                    fullContent { sheetState.progress }
                }
            }

            if (showMini) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = 1f - sheetState.progress.slice(0f, MiniFadeEnd)
                        }
                ) {
                    miniContent()
                }
            }
        }
    }
}

/**
 * Pushes the screen behind the sheet back like an Apple card stack: it scales down to ~94%,
 * rounds its corners and dims as the sheet opens. Reads the progress in the draw phase only.
 */
fun Modifier.behindPlayerSheet(sheetState: PlayerSheetState, enabled: Boolean = true): Modifier =
    if (!enabled) this else graphicsLayer {
        val progress = sheetState.progress
        if (progress > 0.001f) {
            val scale = 1f - (1f - BehindSheetScale) * progress
            scaleX = scale
            scaleY = scale
            alpha = 1f - BehindSheetDim * progress
            shape = RoundedCornerShape(BehindSheetCornerRadius * progress)
            clip = true
        } else {
            scaleX = 1f
            scaleY = 1f
            alpha = 1f
            shape = RectangleShape
            clip = false
        }
    }

private val BehindSheetCornerRadius = 16.dp
private const val BehindSheetScale = 0.94f
private const val BehindSheetDim = 0.45f
