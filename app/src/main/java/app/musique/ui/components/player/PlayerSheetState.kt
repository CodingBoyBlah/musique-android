package app.musique.ui.components.player

import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.musique.ui.theme.MusiqueDimens
import app.musique.ui.theme.MusiqueMotion

enum class PlayerSheetValue { Collapsed, Expanded }

/** Upward drag on the collapsed mini player that opens Now Playing. */
private val ExpandDragThreshold = 72.dp

/** Fling speed that settles the sheet in the fling's direction regardless of distance. */
private val SheetFlingVelocity = 450.dp

@Stable
class PlayerSheetState(
    val draggableState: AnchoredDraggableState<PlayerSheetValue>,
    private val dismissThresholdPx: Float,
    private val expandThresholdPx: Float,
    val flingVelocityPx: Float,
) {
    val currentValue get() = draggableState.currentValue

    /** True while the sheet rests open (or a drag started from open), not just past halfway. */
    val isExpanded get() = draggableState.settledValue == PlayerSheetValue.Expanded

    /** Offset of the collapsed anchor, 0f before the sheet has been laid out. */
    val collapsedOffset: Float
        get() = draggableState.anchors.positionOf(PlayerSheetValue.Collapsed).takeIf { !it.isNaN() } ?: 0f

    /** Current offset of the sheet top, collapsed when not laid out yet. */
    val offset: Float
        get() = draggableState.offset.takeIf { !it.isNaN() } ?: collapsedOffset

    /** 0f = fully collapsed, 1f = fully expanded */
    val progress: Float
        get() {
            val collapsed = collapsedOffset
            return if (collapsed == 0f) 0f else (1f - offset / collapsed).coerceIn(0f, 1f)
        }

    /**
     * Where a released drag should go: a fast fling wins, otherwise the open sheet closes once
     * it was pulled down ~120dp and the collapsed one opens once it was pulled up ~72dp.
     */
    fun settleTarget(velocityY: Float): PlayerSheetValue = when {
        velocityY > flingVelocityPx -> PlayerSheetValue.Collapsed
        velocityY < -flingVelocityPx -> PlayerSheetValue.Expanded
        isExpanded -> if (offset > dismissThresholdPx) PlayerSheetValue.Collapsed else PlayerSheetValue.Expanded
        else -> if (collapsedOffset - offset > expandThresholdPx) PlayerSheetValue.Expanded else PlayerSheetValue.Collapsed
    }

    suspend fun settle(velocityY: Float) = animateTo(settleTarget(velocityY))

    suspend fun animateTo(target: PlayerSheetValue) =
        draggableState.animateTo(target, animationSpec = MusiqueMotion.sheet())

    suspend fun expand() = animateTo(PlayerSheetValue.Expanded)
    suspend fun collapse() = animateTo(PlayerSheetValue.Collapsed)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun rememberPlayerSheetState(
    initial: PlayerSheetValue = PlayerSheetValue.Collapsed
): PlayerSheetState {
    val density = LocalDensity.current

    val decaySpec = rememberSplineBasedDecay<Float>()

    val state = remember {
        AnchoredDraggableState(
            initialValue = initial,
            positionalThreshold = { distance: Float ->
                minOf(distance * 0.4f, with(density) { MusiqueDimens.sheetDismissThreshold.toPx() })
            },
            velocityThreshold = { with(density) { SheetFlingVelocity.toPx() } },
            snapAnimationSpec = MusiqueMotion.sheet(),
            decayAnimationSpec = decaySpec
        )
    }

    return remember(state, density) {
        with(density) {
            PlayerSheetState(
                draggableState = state,
                dismissThresholdPx = MusiqueDimens.sheetDismissThreshold.toPx(),
                expandThresholdPx = ExpandDragThreshold.toPx(),
                flingVelocityPx = SheetFlingVelocity.toPx(),
            )
        }
    }
}
