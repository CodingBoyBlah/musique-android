package app.musique.ui.components.rows

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import app.musique.R
import app.musique.core.model.Playlist
import app.musique.data.setting.LocalUiSettings
import app.musique.ui.components.musique.formatSongCountLabel
import app.musique.ui.theme.MusiqueColors
import app.musique.utils.SharedElementKey

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistRow(
    playlist: Playlist,
    artworkUrl: String?,

    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    isSelected: Boolean = false,
    density: TrackRowDensity = TrackRowDensity.Default,
    trailingContent: @Composable (() -> Unit)? = null,

    // Interaction handlers
    onRowClick: (() -> Unit)? = null,
    onRowLongClick: (() -> Unit)? = null,
    onArtworkClick: (() -> Unit)? = null,
    onTitleClick: (() -> Unit)? = null,
    onArtistClick: (() -> Unit)? = null,

    contentDescription: String? = null,

    sharedTransitionScope: SharedTransitionScope? = null,
    sharedTransitionKey: String? = "${SharedElementKey.PLAYLIST_ARTWORK}_${playlist.uri}",
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

    val subtitle = stringResource(R.string.detail_playlist_type) + " · " +
        formatSongCountLabel(playlist.length.coerceAtLeast(playlist.contents.size))

    EntityRowLayout(
        title = playlist.attributes.name,
        artworkUrl = artworkUrl,
        modifier = modifier,
        subtitle = {
            EntityRowSubtitle(
                text = subtitle,
                modifier = Modifier
                    .then(
                        if (onArtistClick != null) {
                            Modifier.combinedClickable(onClick = onArtistClick, onLongClick = onRowLongClick)
                        } else Modifier
                    )
                    .testTag("playlistrow.artist"),
            )
        },
        density = density,
        artworkModifier = artworkModifier,
        artworkColor = color,
        monochrome = LocalUiSettings.current.monochromePlaylists,
        onRowClick = onRowClick ?: onTitleClick,
        onRowLongClick = onRowLongClick,
        onArtworkClick = onArtworkClick,
        contentDescription = contentDescription,
        titleTestTag = "playlistrow.title",
        trailing = when {
            trailingContent != null -> ({ trailingContent() })
            isPlaying -> ({
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = stringResource(R.string.common_playing),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            })
            isSelected -> ({ Checkbox(checked = true, onCheckedChange = null) })
            else -> null
        },
    )
}
