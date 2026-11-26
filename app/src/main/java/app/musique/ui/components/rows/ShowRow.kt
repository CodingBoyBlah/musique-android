package app.musique.ui.components.rows

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explicit
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import app.musique.ALBUM_COVER_URL
import app.musique.R
import app.musique.core.model.CoverSize
import app.musique.core.model.Show
import app.musique.core.model.ShowMediaType
import app.musique.core.model.getCover
import app.musique.data.setting.LocalUiSettings
import app.musique.ui.theme.MusiqueColors
import app.musique.utils.SharedElementKey

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShowRow(
    show: Show,

    modifier: Modifier = Modifier,
    density: TrackRowDensity = TrackRowDensity.Default,
    trailingContent: @Composable (() -> Unit)? = null,

    // Interaction handlers
    onRowClick: (() -> Unit)? = null,
    onRowLongClick: (() -> Unit)? = null,
    onPublisherClick: (() -> Unit)? = null,

    contentDescription: String? = null,

    sharedTransitionScope: SharedTransitionScope? = null,
    sharedTransitionKey: String? = "${SharedElementKey.ALBUM_ARTWORK}_${show.getCover(CoverSize.MEDIUM)}",
    color: Color = MusiqueColors.cardRaised,
) {
    val artworkModifier =
        if (sharedTransitionScope != null && sharedTransitionKey != null) {
            with(sharedTransitionScope) {
                Modifier.sharedBounds(
                    rememberSharedContentState(sharedTransitionKey),
                    animatedVisibilityScope = LocalNavAnimatedContentScope.current
                )
            }
        } else Modifier

    val episodeCount = show.episodes.size
    val subtitle = listOf(
        show.publisher,
        pluralStringResource(R.plurals.show_episode_count_plural, episodeCount, episodeCount),
    ).filter { it.isNotBlank() }.joinToString(" · ")

    EntityRowLayout(
        title = show.name,
        artworkUrl = show.getCover(CoverSize.MEDIUM)?.uri?.let { ALBUM_COVER_URL + it },
        modifier = modifier,
        subtitle = {
            EntityRowSubtitle(
                text = subtitle,
                modifier = Modifier
                    .then(
                        if (onPublisherClick != null) {
                            Modifier.combinedClickable(onClick = onPublisherClick, onLongClick = onRowLongClick)
                        } else Modifier
                    )
                    .testTag("showrow.publisher"),
            )
        },
        density = density,
        artworkModifier = artworkModifier,
        artworkColor = color,
        monochrome = LocalUiSettings.current.monochromeTracks,
        onRowClick = onRowClick,
        onRowLongClick = onRowLongClick,
        contentDescription = contentDescription,
        titleTestTag = "showrow.title",
        trailing = {
            if (show.mediaType == ShowMediaType.VIDEO || show.mediaType == ShowMediaType.MIXED) {
                Icon(
                    Icons.Rounded.Videocam,
                    contentDescription = stringResource(R.string.library_video_episodes),
                    tint = MusiqueColors.textTertiary,
                    modifier = Modifier.size(18.dp),
                )
            }
            if (show.isExplicit) {
                Icon(
                    Icons.Rounded.Explicit,
                    contentDescription = null,
                    tint = MusiqueColors.textTertiary,
                    modifier = Modifier.size(15.dp),
                )
            }
            trailingContent?.invoke()
        },
    )
}
