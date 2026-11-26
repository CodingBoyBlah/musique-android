package app.musique.ui.components.rows

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import app.musique.R
import app.musique.ui.components.SmartImage
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.theme.MusiqueDimens
import app.musique.ui.theme.MusiqueType
import androidx.compose.ui.unit.dp

/**
 * Shared Apple Music list row for albums, artists, playlists and shows: square (or round)
 * artwork, a title and an optional muted subtitle, and a trailing accessory slot.
 * [modifier] is applied once, to the outer row.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun EntityRowLayout(
    title: String,
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    subtitle: (@Composable () -> Unit)? = null,
    density: TrackRowDensity = TrackRowDensity.Default,
    artworkShape: Shape = RowArtworkShape,
    artworkModifier: Modifier = Modifier,
    artworkColor: Color = MusiqueColors.cardRaised,
    monochrome: Boolean? = null,
    onRowClick: (() -> Unit)? = null,
    onRowLongClick: (() -> Unit)? = null,
    onArtworkClick: (() -> Unit)? = null,
    contentDescription: String? = null,
    titleTestTag: String? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val imageDp = density.artworkSize()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onRowClick != null || onRowLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = { onRowClick?.invoke() },
                        onLongClick = onRowLongClick,
                    )
                } else Modifier
            )
            .semantics { contentDescription?.let { this.contentDescription = it } }
            .heightIn(min = MusiqueDimens.rowHeight)
            .padding(
                start = MusiqueDimens.screenPadding,
                end = 4.dp,
                top = density.verticalPadding(),
                bottom = density.verticalPadding(),
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(imageDp)
                .clip(artworkShape)
                .background(artworkColor),
        ) {
            SmartImage(
                url = artworkUrl,
                contentDescription = stringResource(R.string.common_artwork),
                shape = artworkShape,
                monochrome = monochrome,
                modifier = artworkModifier
                    .fillMaxSize()
                    .then(
                        if (onArtworkClick != null) {
                            Modifier.combinedClickable(onClick = onArtworkClick, onLongClick = onRowLongClick)
                        } else Modifier
                    ),
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = MusiqueType.rowTitle,
                color = MusiqueColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (titleTestTag != null) Modifier.testTag(titleTestTag) else Modifier,
            )
            subtitle?.invoke()
        }

        if (trailing != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(end = 8.dp),
                content = trailing,
            )
        }
    }
}

/** Muted one-line subtitle for [EntityRowLayout]. */
@Composable
internal fun EntityRowSubtitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MusiqueType.rowSubtitle,
        color = MusiqueColors.textSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}
