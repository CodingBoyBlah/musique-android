package app.musique.ui.components.musique

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.musique.ui.components.SmartImage
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.theme.MusiqueDimens
import app.musique.ui.theme.MusiqueType
import app.musique.ui.theme.musiqueTypography
import app.musique.ui.theme.pressScale

/**
 * Cider / Apple Music section header: 20sp bold title with optional leading seed artwork,
 * eyebrow ("More Like"), trailing chevron, or "See All" label.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    seedArtworkUrl: String? = null,
    actionLabel: String? = null,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = onClick != null && actionLabel == null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val rowModifier = modifier
        .fillMaxWidth()
        .then(
            if (onClick != null) {
                Modifier
                    .pressScale(interactionSource, pressedScale = 0.985f)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Button,
                        onClick = onClick,
                    )
            } else {
                Modifier
            }
        )
        .padding(horizontal = MusiqueDimens.screenPadding)

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!seedArtworkUrl.isNullOrBlank()) {
            SmartImage(
                url = seedArtworkUrl,
                imageSize = 36.dp,
                shape = RoundedCornerShape(6.dp),
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(10.dp))
        }

        Column(modifier = Modifier.weight(1f, fill = false)) {
            if (!eyebrow.isNullOrBlank()) {
                Text(
                    text = eyebrow,
                    style = MusiqueType.caption,
                    color = MusiqueColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MusiqueType.sectionTitle,
                    color = MusiqueColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (showChevron) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MusiqueColors.textSecondary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        if (!actionLabel.isNullOrBlank() && onClick != null) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = actionLabel,
                style = MusiqueType.button,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SectionHeaderPreview() {
    MaterialTheme(typography = musiqueTypography()) {
        Column(
            modifier = Modifier
                .background(MusiqueColors.canvas)
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionHeader(title = "Top Picks for You")
            SectionHeader(title = "Recently Played", onClick = {})
            SectionHeader(title = "Albums", actionLabel = "See All", onClick = {})
        }
    }
}
