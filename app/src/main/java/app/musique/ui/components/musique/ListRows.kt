package app.musique.ui.components.musique

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.musique.R
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.theme.MusiqueDimens
import app.musique.ui.theme.MusiqueType
import app.musique.ui.theme.pressScale

/**
 * Apple Music Library category row: 24dp accent icon, 18sp label and a muted chevron, 52dp tall.
 * Separate consecutive rows with [ListHairline] inset by [CategoryRowDividerInset].
 */
@Composable
fun CategoryRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MusiqueColors.accent,
    trailingText: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .pressScale(interaction, pressedScale = 0.985f, pressedAlpha = 0.7f)
            .padding(horizontal = MusiqueDimens.screenPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MusiqueType.rowTitle.copy(fontSize = 18.sp, lineHeight = 23.sp),
            color = MusiqueColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (trailingText != null) {
            Text(
                text = trailingText,
                style = MusiqueType.rowDuration,
                color = MusiqueColors.textTertiary,
                modifier = Modifier.padding(end = 6.dp),
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MusiqueColors.textTertiary,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Start inset of the hairline between [CategoryRow]s, so it lines up with the labels. */
val CategoryRowDividerInset: Dp = MusiqueDimens.screenPadding + 40.dp

/** 0.5dp hairline divider used between Apple style list rows. */
@Composable
fun ListHairline(
    modifier: Modifier = Modifier,
    startInset: Dp = CategoryRowDividerInset,
) {
    HorizontalDivider(
        modifier = modifier.padding(start = startInset),
        thickness = MusiqueDimens.hairlineWidth,
        color = MusiqueColors.hairline,
    )
}

/**
 * Generic Apple list row: leading slot (artwork or icon tile), title + subtitle and trailing
 * slot. Used for library utility rows (folders, New Playlist) so they match the track rows.
 */
@Composable
fun AppleListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    titleColor: Color = MusiqueColors.textPrimary,
    contentPadding: PaddingValues = PaddingValues(horizontal = MusiqueDimens.screenPadding, vertical = 6.dp),
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MusiqueDimens.rowHeight)
            .then(
                if (onClick != null) {
                    Modifier
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            role = Role.Button,
                            onClick = onClick,
                        )
                        .pressScale(interaction, pressedScale = 0.985f, pressedAlpha = 0.75f)
                } else Modifier
            )
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MusiqueType.rowTitle,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MusiqueType.rowSubtitle,
                    color = MusiqueColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                content = trailing,
            )
        }
    }
}

/** 48dp rounded tile holding an icon, the leading visual for rows without artwork. */
@Composable
fun IconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    containerColor: Color = MusiqueColors.cardRaised,
    iconTint: Color = MusiqueColors.accent,
    size: Dp = MusiqueDimens.rowArtworkSize,
    shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

/** Apple's `···` overflow button for list rows. */
@Composable
fun RowMoreButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = stringResource(R.string.detail_more),
    tint: Color = MusiqueColors.textSecondary,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(MusiqueDimens.minTouchTarget),
    ) {
        Icon(
            imageVector = Icons.Rounded.MoreHoriz,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Small circular icon button for top bar actions (+, filter). */
@Composable
fun TopBarIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MusiqueColors.accent,
) {
    val interaction = remember { MutableInteractionSource() }
    IconButton(
        onClick = onClick,
        interactionSource = interaction,
        modifier = modifier
            .size(MusiqueDimens.minTouchTarget)
            .pressScale(interaction, pressedScale = 0.9f),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MusiqueColors.fillOnArtwork),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
