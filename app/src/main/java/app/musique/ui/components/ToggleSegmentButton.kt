package app.musique.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.theme.MusiqueMotion
import app.musique.ui.theme.MusiqueType
import app.musique.ui.theme.pressScale

@Composable
fun ToggleSegmentButton(
    modifier: Modifier = Modifier,
    active: Boolean,
    enabled: Boolean = true,
    activeColor: Color = MusiqueColors.pillSelected,
    inactiveColor: Color = MusiqueColors.fillOnArtwork,
    activeContentColor: Color = MusiqueColors.onPillSelected,
    inactiveContentColor: Color = MusiqueColors.textPrimary,
    activeCornerRadius: Dp = 22.dp,
    inactiveCornerRadius: Dp = 16.dp,
    onClick: () -> Unit,
    iconId: Int,
    contentDesc: String,
) {
    ToggleSegmentButtonContainer(
        modifier = modifier,
        active = active,
        enabled = enabled,
        activeColor = activeColor,
        inactiveColor = inactiveColor,
        activeCornerRadius = activeCornerRadius,
        inactiveCornerRadius = inactiveCornerRadius,
        onClick = onClick,
    ) {
        Icon(
            painter = painterResource(iconId),
            contentDescription = contentDesc,
            tint = if (active) activeContentColor else inactiveContentColor,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun ToggleSegmentButton(
    modifier: Modifier = Modifier,
    active: Boolean,
    enabled: Boolean = true,
    activeColor: Color = MusiqueColors.pillSelected,
    inactiveColor: Color = MusiqueColors.fillOnArtwork,
    activeContentColor: Color = MusiqueColors.onPillSelected,
    inactiveContentColor: Color = MusiqueColors.textPrimary,
    activeCornerRadius: Dp = 22.dp,
    inactiveCornerRadius: Dp = 16.dp,
    onClick: () -> Unit,
    imageVector: ImageVector,
    contentDesc: String,
) {
    ToggleSegmentButtonContainer(
        modifier = modifier,
        active = active,
        enabled = enabled,
        activeColor = activeColor,
        inactiveColor = inactiveColor,
        activeCornerRadius = activeCornerRadius,
        inactiveCornerRadius = inactiveCornerRadius,
        onClick = onClick,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDesc,
            tint = if (active) activeContentColor else inactiveContentColor,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun ToggleSegmentButton(
    modifier: Modifier = Modifier,
    active: Boolean,
    enabled: Boolean = true,
    activeColor: Color = MusiqueColors.pillSelected,
    inactiveColor: Color = MusiqueColors.fillOnArtwork,
    activeContentColor: Color = MusiqueColors.onPillSelected,
    inactiveContentColor: Color = MusiqueColors.textPrimary,
    activeCornerRadius: Dp = 22.dp,
    inactiveCornerRadius: Dp = 16.dp,
    onClick: () -> Unit,
    text: String,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    textAlign: TextAlign = TextAlign.Center,
) {
    ToggleSegmentButtonContainer(
        modifier = modifier,
        active = active,
        enabled = enabled,
        activeColor = activeColor,
        inactiveColor = inactiveColor,
        activeCornerRadius = activeCornerRadius,
        inactiveCornerRadius = inactiveCornerRadius,
        onClick = onClick,
    ) {
        Text(
            text = text,
            color = if (active) activeContentColor else inactiveContentColor,
            style = MusiqueType.button,
            maxLines = maxLines,
            overflow = overflow,
            textAlign = textAlign,
        )
    }
}

@Composable
fun ToggleSegmentButton(
    modifier: Modifier = Modifier,
    active: Boolean,
    enabled: Boolean = true,
    activeColor: Color = MusiqueColors.pillSelected,
    inactiveColor: Color = MusiqueColors.fillOnArtwork,
    activeContentColor: Color = MusiqueColors.onPillSelected,
    inactiveContentColor: Color = MusiqueColors.textPrimary,
    activeCornerRadius: Dp = 22.dp,
    inactiveCornerRadius: Dp = 16.dp,
    onClick: () -> Unit,
    text: String,
    imageVector: ImageVector,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    textAlign: TextAlign = TextAlign.Center,
) {
    ToggleSegmentButtonContainer(
        modifier = modifier,
        active = active,
        enabled = enabled,
        activeColor = activeColor,
        inactiveColor = inactiveColor,
        activeCornerRadius = activeCornerRadius,
        inactiveCornerRadius = inactiveCornerRadius,
        onClick = onClick,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp),
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                tint = if (active) activeContentColor else inactiveContentColor,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = if (active) activeContentColor else inactiveContentColor,
                style = MusiqueType.button,
                maxLines = maxLines,
                overflow = overflow,
                textAlign = textAlign,
            )
        }
    }
}

@Composable
private fun ToggleSegmentButtonContainer(
    modifier: Modifier,
    active: Boolean,
    enabled: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    activeCornerRadius: Dp,
    inactiveCornerRadius: Dp,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val targetBgColor = if (active) activeColor else inactiveColor
    val bgColor by animateColorAsState(
        targetValue = if (enabled) targetBgColor else targetBgColor.copy(alpha = 0.4f),
        animationSpec = tween(durationMillis = 220),
        label = "segmentBg",
    )
    val cornerRadius by animateDpAsState(
        targetValue = if (active) activeCornerRadius else inactiveCornerRadius,
        animationSpec = MusiqueMotion.layout(),
        label = "segmentCorner",
    )

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .pressScale(interactionSource, pressedScale = 0.94f)
            .clip(RoundedCornerShape(cornerRadius))
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer(alpha = if (enabled) 1f else 0.38f)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}
