package app.musique.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.util.lerp
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.theme.MusiqueMotion
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Cider-styled playback scrubber: animated sine wave on elapsed progress while playing,
 * smoothly flattening when paused or scrubbing, a 4x28dp vertical pill thumb, and a thin
 * remaining track terminating in a crisp end dot.
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun WavyMusicSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    trackHeight: Dp = 3.dp,
    inactiveTrackHeight: Dp = 2.5.dp,
    thumbRadius: Dp = 2.dp,
    thumbWidth: Dp = 4.dp,
    thumbHeight: Dp = 28.dp,
    activeTrackColor: Color = Color.White.copy(alpha = 0.96f),
    inactiveTrackColor: Color = MusiqueColors.trackInactive,
    thumbColor: Color = Color.White,
    endDotColor: Color = Color.White.copy(alpha = 0.9f),
    endDotRadius: Dp = 2.dp,
    waveAmplitudeWhenPlaying: Dp = 3.5.dp,
    waveLength: Dp = 36.dp,
    waveAnimationDuration: Int = 1400,
    hideInactiveTrackPortion: Boolean = true,
    isPlaying: Boolean = true,
    thumbLineHeightWhenInteracting: Dp = 30.dp,
    isWaveEligible: Boolean = true,
    semanticsLabel: String? = null,
    semanticsProgressStep: Float = 0.01f,
) {
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val isInteracting = isDragged || isPressed

    val thumbInteractionFraction by animateFloatAsState(
        targetValue = if (isInteracting) 1f else 0f,
        animationSpec = MusiqueMotion.press(),
        label = "ThumbInteractionAnim",
    )

    val shouldShowWave = isWaveEligible && isPlaying && !isInteracting

    val animatedWaveAmplitude by animateDpAsState(
        targetValue = if (shouldShowWave) waveAmplitudeWhenPlaying else 0.dp,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "WaveAmplitudeAnim",
    )

    val phaseShiftAnim = remember { Animatable(0f) }

    LaunchedEffect(shouldShowWave, waveAnimationDuration) {
        if (shouldShowWave && waveAnimationDuration > 0) {
            val fullRotation = (2 * PI).toFloat()
            while (shouldShowWave) {
                val start =
                    (phaseShiftAnim.value % fullRotation).let { if (it < 0f) it + fullRotation else it }
                phaseShiftAnim.snapTo(start)
                phaseShiftAnim.animateTo(
                    targetValue = start + fullRotation,
                    animationSpec = tween(
                        durationMillis = waveAnimationDuration,
                        easing = LinearEasing,
                    ),
                )
            }
        }
    }

    val density = LocalDensity.current
    val trackHeightPx = with(density) { trackHeight.toPx() }
    val inactiveTrackHeightPx = with(density) { inactiveTrackHeight.toPx() }
    val thumbWidthPx = with(density) { max(thumbWidth, thumbRadius * 2).toPx() }
    val thumbHeightPx = with(density) { thumbHeight.toPx() }
    val waveAmplitudePxInternal = with(density) { animatedWaveAmplitude.toPx() }
    val waveLengthPx = with(density) { waveLength.toPx() }
    val waveFrequency = if (waveLengthPx > 0f) ((2 * PI) / waveLengthPx).toFloat() else 0f
    val thumbLineHeightPxInternal = with(density) { thumbLineHeightWhenInteracting.toPx() }
    val endDotRadiusPx = with(density) { endDotRadius.toPx() }
    val thumbGapBasePx = with(density) { 3.dp.toPx() }
    val maxTaperPx = with(density) { 8.dp.toPx() }

    val wavePath = remember { Path() }

    val sliderVisualHeight = remember(trackHeight, thumbHeight, thumbLineHeightWhenInteracting) {
        max(32.dp, max(thumbHeight, thumbLineHeightWhenInteracting) + 4.dp)
    }

    val hapticFeedback = LocalHapticFeedback.current
    val clampedValue = value.coerceIn(valueRange.start, valueRange.endInclusive)
    val normalizedValue = if (valueRange.endInclusive == valueRange.start) {
        0f
    } else {
        ((clampedValue - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
    }
    val safeSemanticsStep = semanticsProgressStep.coerceIn(0.005f, 0.25f)
    val semanticNormalizedValue = remember(normalizedValue, safeSemanticsStep) {
        ((normalizedValue / safeSemanticsStep).roundToInt() * safeSemanticsStep).coerceIn(0f, 1f)
    }
    val semanticSliderValue = remember(semanticNormalizedValue, valueRange) {
        valueRange.start + semanticNormalizedValue * (valueRange.endInclusive - valueRange.start)
    }

    Box(
        modifier = modifier.clipToBounds(),
        contentAlignment = Alignment.Center,
    ) {
        val lastHapticStep = remember { mutableIntStateOf(-1) }

        Slider(
            value = clampedValue,
            onValueChange = { newValue ->
                val normalizedNew = if (valueRange.endInclusive == valueRange.start) {
                    0f
                } else {
                    ((newValue - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
                }
                val currentStep = (normalizedNew * 50f).roundToInt()
                if (currentStep != lastHapticStep.intValue) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    lastHapticStep.intValue = currentStep
                }
                onValueChange(newValue)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(sliderVisualHeight)
                .clearAndSetSemantics {
                    if (!semanticsLabel.isNullOrBlank()) {
                        contentDescription = semanticsLabel
                    }
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        current = semanticSliderValue,
                        range = valueRange.start..valueRange.endInclusive,
                        steps = 0,
                    )
                    if (enabled) {
                        setProgress { requested ->
                            val coerced = requested.coerceIn(valueRange.start, valueRange.endInclusive)
                            onValueChange(coerced)
                            onValueChangeFinished?.invoke()
                            true
                        }
                    }
                },
            enabled = enabled,
            valueRange = valueRange,
            onValueChangeFinished = onValueChangeFinished,
            interactionSource = interactionSource,
            colors = SliderDefaults.colors(
                thumbColor = Color.Transparent,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
            ),
        )

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(sliderVisualHeight)
                .drawBehind {
                    val canvasWidth = size.width
                    val localCenterY = size.height / 2f
                    val edgeInset = maxOf(thumbWidthPx / 2f, endDotRadiusPx)
                    val localTrackStart = edgeInset
                    val localTrackEnd = (canvasWidth - edgeInset).coerceAtLeast(localTrackStart)
                    val localTrackWidth = (localTrackEnd - localTrackStart).coerceAtLeast(0f)

                    val phaseShift = phaseShiftAnim.value
                    val currentThumbCenterX = localTrackStart + localTrackWidth * normalizedValue
                    val thumbCurrentWidthPx = lerp(thumbWidthPx, thumbWidthPx * 1.25f, thumbInteractionFraction)
                    val thumbCurrentHeightPx = lerp(thumbHeightPx, thumbLineHeightPxInternal, thumbInteractionFraction)
                    val gapPx = thumbCurrentWidthPx / 2f + thumbGapBasePx

                    // Inactive track + end dot
                    val inactiveStart = if (hideInactiveTrackPortion) {
                        (currentThumbCenterX + gapPx).coerceAtMost(localTrackEnd)
                    } else {
                        localTrackStart
                    }
                    val dotCenterX = localTrackEnd - endDotRadiusPx
                    val inactiveEnd = (dotCenterX - endDotRadiusPx * 1.5f).coerceAtLeast(inactiveStart)
                    if (inactiveEnd > inactiveStart) {
                        drawLine(
                            color = inactiveTrackColor,
                            start = Offset(inactiveStart, localCenterY),
                            end = Offset(inactiveEnd, localCenterY),
                            strokeWidth = inactiveTrackHeightPx,
                            cap = StrokeCap.Round,
                        )
                    }
                    if (endDotRadiusPx > 0f && currentThumbCenterX + gapPx < dotCenterX - endDotRadiusPx * 0.5f) {
                        drawCircle(
                            color = endDotColor,
                            radius = endDotRadiusPx,
                            center = Offset(dotCenterX, localCenterY),
                        )
                    }

                    // Active elapsed track (animated sine wave or flat line)
                    val activeTrackVisualEnd = (currentThumbCenterX - gapPx).coerceAtLeast(localTrackStart)
                    if (normalizedValue > 0f && activeTrackVisualEnd > localTrackStart) {
                        if (waveAmplitudePxInternal > 0.05f && waveFrequency > 0f) {
                            wavePath.reset()
                            val waveStartDrawX = localTrackStart
                            val waveEndDrawX = activeTrackVisualEnd
                            val span = waveEndDrawX - waveStartDrawX
                            val taperPx = min(maxTaperPx, span * 0.25f)
                            val periodPx = ((2 * PI) / waveFrequency).toFloat()
                            val waveStep = (periodPx / 20f)
                                .coerceAtLeast(1.2f)
                                .coerceAtMost(trackHeightPx.coerceAtLeast(2f))

                            fun yAt(x: Float): Float {
                                val edgeEnvelope = if (taperPx > 0.5f) {
                                    min(
                                        ((x - waveStartDrawX) / taperPx).coerceIn(0f, 1f),
                                        ((waveEndDrawX - x) / taperPx).coerceIn(0f, 1f),
                                    )
                                } else {
                                    1f
                                }
                                val s = sin(waveFrequency * x + phaseShift)
                                return localCenterY + waveAmplitudePxInternal * edgeEnvelope * s
                            }

                            var prevX = waveStartDrawX
                            var prevY = yAt(prevX)
                            wavePath.moveTo(prevX, prevY)

                            var x = prevX + waveStep
                            while (x < waveEndDrawX) {
                                val y = yAt(x)
                                val midX = (prevX + x) * 0.5f
                                val midY = (prevY + y) * 0.5f
                                wavePath.quadraticTo(prevX, prevY, midX, midY)
                                prevX = x
                                prevY = y
                                x += waveStep
                            }
                            val endY = yAt(waveEndDrawX)
                            wavePath.quadraticTo(prevX, prevY, waveEndDrawX, endY)

                            drawPath(
                                path = wavePath,
                                color = activeTrackColor,
                                style = Stroke(
                                    width = trackHeightPx,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round,
                                    miter = 1f,
                                ),
                            )
                        } else {
                            drawLine(
                                color = activeTrackColor,
                                start = Offset(localTrackStart, localCenterY),
                                end = Offset(activeTrackVisualEnd, localCenterY),
                                strokeWidth = trackHeightPx,
                                cap = StrokeCap.Round,
                            )
                        }
                    }

                    // Vertical pill thumb
                    drawRoundRect(
                        color = thumbColor,
                        topLeft = Offset(
                            currentThumbCenterX - thumbCurrentWidthPx / 2f,
                            localCenterY - thumbCurrentHeightPx / 2f,
                        ),
                        size = Size(thumbCurrentWidthPx, thumbCurrentHeightPx),
                        cornerRadius = CornerRadius(thumbCurrentWidthPx / 2f),
                    )
                },
        )
    }
}
