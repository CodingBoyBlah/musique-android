package app.musique.ui.components.rows

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explicit
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import app.musique.R
import app.musique.core.model.Artist
import app.musique.data.setting.LocalUiSettings
import app.musique.ui.components.AudioBarsIndicator
import app.musique.ui.components.SmartImage
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.theme.MusiqueDimens
import app.musique.ui.theme.MusiqueType
import app.musique.utils.SharedElementKey

enum class TrackRowDensity { Compact, Default, Spacious }

/** Artwork size for each density; Default is Apple Music's 48dp list artwork. */
internal fun TrackRowDensity.artworkSize(): Dp = when (this) {
    TrackRowDensity.Compact -> 40.dp
    TrackRowDensity.Default -> MusiqueDimens.rowArtworkSize
    TrackRowDensity.Spacious -> 64.dp
}

internal fun TrackRowDensity.verticalPadding(): Dp = when (this) {
    TrackRowDensity.Compact -> 4.dp
    TrackRowDensity.Default -> 6.dp
    TrackRowDensity.Spacious -> 10.dp
}

/** Apple Music list row radius for square artwork. */
internal val RowArtworkShape = RoundedCornerShape(8.dp)

/**
 * Apple Music style song row: 48dp artwork (8dp radius), [MusiqueType.rowTitle] title and a muted
 * [MusiqueType.rowSubtitle] artist line, with a trailing slot for accessories such as `···`.
 * The row itself is transparent so the list or grouped card behind it shows through.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackRow(
    title: String,
    artists: List<Artist>,
    artworkUrl: String?,

    modifier: Modifier = Modifier,
    isExplicit: Boolean = false,
    isLoaded: Boolean = false,
    isPlaying: Boolean = false,
    isSelected: Boolean = false,
    density: TrackRowDensity = TrackRowDensity.Default,
    trailingContent: @Composable (() -> Unit)? = null,

    /**
     * Instead of showing artists names show the album name
     */
    showAlbumName: Boolean = false,
    albumName: String? = null,

    // Interaction handlers
    onRowClick: (() -> Unit)? = null,
    onRowLongClick: (() -> Unit)? = null,
    onArtworkClick: (() -> Unit)? = null,
    onTitleClick: (() -> Unit)? = null,
    onArtistClick: ((Artist) -> Unit)? = null,

    contentDescription: String? = null,

    sharedTransitionScope: SharedTransitionScope? = null,
    sharedTransitionKey: String? = "${SharedElementKey.ALBUM_ARTWORK}_${artworkUrl}",
    color: Color = MusiqueColors.cardRaised,
) {
    val imageDp = density.artworkSize()
    val accent = MaterialTheme.colorScheme.primary

    // The caller's modifier goes on the outer element only; reusing it on inner rows doubled
    // padding and click handlers.
    val rowModifier = modifier
        .fillMaxWidth()
        .then(
            if (onRowClick != null || onRowLongClick != null) {
                Modifier.combinedClickable(
                    onClick = { onRowClick?.invoke() },
                    onLongClick = { onRowLongClick?.invoke() }
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
        )

    val artworkModifier =
        if (sharedTransitionScope != null && sharedTransitionKey != null) {
            with(sharedTransitionScope) {
                Modifier.sharedBounds(
                    rememberSharedContentState(sharedTransitionKey),
                    animatedVisibilityScope = LocalNavAnimatedContentScope.current
                )
            }
        } else Modifier

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(imageDp)
                .clip(RowArtworkShape)
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            SmartImage(
                url = artworkUrl,
                contentDescription = stringResource(R.string.common_artwork),
                shape = RowArtworkShape,
                modifier = artworkModifier
                    .then(
                        if (onArtworkClick != null) {
                            Modifier.combinedClickable(
                                onClick = { onArtworkClick() },
                                onLongClick = { onRowLongClick?.invoke() }
                            )
                        } else Modifier
                    ),
                monochrome = LocalUiSettings.current.monochromeTracks
            )
            if (isLoaded) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    AudioBarsIndicator(
                        isPlaying = isPlaying,
                        barCount = 4,
                        barWidth = 3.dp,
                        barHeight = 14.dp,
                        spacing = 2.dp,
                        color = Color.White,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MusiqueType.rowTitle,
                    color = if (isLoaded) accent else MusiqueColors.textPrimary,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .then(
                            if (onTitleClick != null) {
                                Modifier.combinedClickable(
                                    onClick = { onTitleClick() },
                                    onLongClick = { onRowLongClick?.invoke() }
                                )
                            } else Modifier
                        )
                        .testTag("trackrow.title")
                )
                if (isExplicit) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Rounded.Explicit,
                        contentDescription = null,
                        tint = MusiqueColors.textTertiary,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }

            if (!showAlbumName) {
                Row {
                    artists.forEachIndexed { index, artist ->
                        Text(
                            text = if (index < artists.lastIndex) artist.name + ", " else artist.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MusiqueType.rowSubtitle,
                            color = MusiqueColors.textSecondary,
                            modifier = Modifier
                                .then(
                                    if (onArtistClick != null) {
                                        Modifier.combinedClickable(
                                            onClick = { onArtistClick(artist) },
                                            onLongClick = { onRowLongClick?.invoke() }
                                        )
                                    } else Modifier
                                )
                                .testTag("trackrow.artist.$index")
                        )
                    }
                }
            } else {
                Text(
                    text = albumName ?: "",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MusiqueType.rowSubtitle,
                    color = MusiqueColors.textSecondary,
                    modifier = Modifier
                        .then(
                            if (onArtworkClick != null) {
                                Modifier.combinedClickable(
                                    onClick = { onArtworkClick() },
                                    onLongClick = { onRowLongClick?.invoke() }
                                )
                            } else Modifier
                        )
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Checkbox(
                    checked = true,
                    onCheckedChange = null
                )
            }

            trailingContent?.invoke()
        }
    }
}
