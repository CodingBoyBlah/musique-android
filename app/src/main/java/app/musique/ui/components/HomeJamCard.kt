package app.musique.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.musique.R
import app.musique.core.jam.Jam
import app.musique.core.jam.JamMember
import app.musique.data.setting.LocalUiSettings
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.theme.MusiqueDimens
import app.musique.ui.theme.MusiqueMotion
import app.musique.ui.theme.MusiqueShapes
import app.musique.ui.theme.MusiqueType
import app.musique.ui.theme.pressScale

/** Opens Jam controls; never starts a session or playback just by tapping. */
@Composable
internal fun HomeJamCard(
    jam: Jam?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val active = jam != null
    val accent = MaterialTheme.colorScheme.primary
    val interaction = remember { MutableInteractionSource() }

    val container by animateColorAsState(
        targetValue = if (active) MusiqueColors.cardRaised else MusiqueColors.card,
        animationSpec = MusiqueMotion.color(),
        label = "jamCardContainer",
    )
    val borderColor = if (active) accent.copy(alpha = 0.42f) else MusiqueColors.hairline

    Surface(
        onClick = onClick,
        interactionSource = interaction,
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interaction, pressedScale = 0.975f)
            .border(MusiqueDimens.hairlineWidth, borderColor, MusiqueShapes.cardLarge),
        shape = MusiqueShapes.cardLarge,
        color = container,
        contentColor = MusiqueColors.textPrimary,
    ) {
        if (jam != null) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Group,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(22.dp),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_jam_active),
                        style = MusiqueType.cardTitle,
                        color = MusiqueColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = pluralStringResource(R.plurals.jam_listeners, jam.members.size, jam.members.size),
                        style = MusiqueType.cardSubtitle,
                        color = MusiqueColors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (jam.members.isNotEmpty()) {
                    JamMemberStack(
                        members = jam.members,
                        ringColor = container,
                        maxVisible = 3,
                        avatarSize = 32.dp,
                        overlap = 10.dp,
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.jam_manage),
                    tint = MusiqueColors.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        } else {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(MusiqueShapes.tile)
                        .background(MusiqueColors.fillOnArtwork),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Group,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(22.dp),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.jam_start_headline),
                        style = MusiqueType.cardTitle,
                        color = MusiqueColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.home_jam_description),
                        style = MusiqueType.cardSubtitle,
                        color = MusiqueColors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MusiqueColors.fillOnArtwork,
                    contentColor = MusiqueColors.textPrimary,
                ) {
                    Text(
                        text = stringResource(R.string.jam_start),
                        style = MusiqueType.caption.copy(fontWeight = FontWeight.SemiBold),
                        color = MusiqueColors.textPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun JamMemberStack(
    members: List<JamMember>,
    ringColor: Color,
    maxVisible: Int = 5,
    avatarSize: Dp = 36.dp,
    overlap: Dp = 10.dp,
) {
    val visible = members.take(maxVisible)
    val extra = members.size - visible.size

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(-overlap),
    ) {
        visible.forEach { member ->
            val hideProfile = LocalUiSettings.current.screenshotMode && member.you
            StackAvatar(
                name = if (hideProfile) stringResource(R.string.screenshot_mode_username) else member.name,
                imageUrl = if (hideProfile) null else member.image,
                isHost = member.host,
                size = avatarSize,
                ringColor = ringColor,
            )
        }
        if (extra > 0) {
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .background(ringColor, CircleShape)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(MusiqueColors.chrome),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+$extra",
                    style = MusiqueType.caption,
                    fontWeight = FontWeight.SemiBold,
                    color = MusiqueColors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun StackAvatar(
    name: String,
    imageUrl: String?,
    isHost: Boolean,
    size: Dp,
    ringColor: Color,
    ringWidth: Dp = 2.dp,
) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(size)
            .background(ringColor, CircleShape)
            .padding(ringWidth)
            .clip(CircleShape)
            .background(if (isHost) accent.copy(alpha = 0.30f) else MusiqueColors.chrome),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.take(1).uppercase().ifEmpty { "?" },
            style = MusiqueType.caption,
            fontWeight = FontWeight.SemiBold,
            color = MusiqueColors.textPrimary,
        )
        if (!imageUrl.isNullOrEmpty()) {
            SmartImage(
                url = imageUrl,
                imageSize = size - ringWidth * 2,
                shape = CircleShape,
            )
        }
    }
}
