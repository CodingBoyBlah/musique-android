package app.musique.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import kotlinx.coroutines.launch

/**
 * The Apple Music / Cider press: the row lights up the instant the finger lands and fades out after
 * release, instead of Material's ripple spreading from the touch point. Installed as the default
 * indication, so every plain `clickable` gets it.
 */
object PressHighlight : IndicationNodeFactory {
    private val color = Color.White.copy(alpha = 0.10f)

    override fun create(interactionSource: InteractionSource): DelegatableNode =
        PressHighlightNode(interactionSource, color)

    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode(): Int = javaClass.hashCode()
}

private class PressHighlightNode(
    private val interactionSource: InteractionSource,
    private val color: Color,
) : Modifier.Node(), DrawModifierNode {
    private val alpha = Animatable(0f)

    override fun onAttach() {
        coroutineScope.launch {
            var pressed = 0
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> pressed++
                    is PressInteraction.Release, is PressInteraction.Cancel -> pressed = (pressed - 1).coerceAtLeast(0)
                    else -> return@collect
                }
                if (pressed > 0) {
                    // feedback on touch down, with no ramp: any delay reads as lag
                    launch { alpha.snapTo(1f); invalidateDraw() }
                } else {
                    launch {
                        alpha.animateTo(0f, tween(durationMillis = 220)) { invalidateDraw() }
                    }
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val a = alpha.value
        if (a > 0f) drawRect(color = color, alpha = a)
    }
}
