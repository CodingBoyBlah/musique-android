package app.musique.ui.components.rows

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import app.musique.R
import app.musique.core.model.Album
import app.musique.data.setting.LocalUiSettings
import app.musique.ui.theme.MusiqueColors
import app.musique.utils.SharedElementKey

@Composable
fun SharedTransitionScope.AlbumRow(
    album: Album,
    artworkUrl: String?,

    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    isSelected: Boolean = false,
    density: TrackRowDensity = TrackRowDensity.Default,
    trailingContent: @Composable (() -> Unit)? = null,

    // Interaction handlers
    onRowClick: (() -> Unit)? = null,

    contentDescription: String? = null,

    sharedTransitionKey: String? = "${SharedElementKey.ALBUM_ARTWORK}_${album.uri}",
    color: Color = MusiqueColors.cardRaised,
) {
    // Morph the cover only when a key is given; never reuse the caller's modifier here.
    val artworkModifier = if (sharedTransitionKey != null) {
        Modifier.sharedBounds(
            rememberSharedContentState(sharedTransitionKey),
            animatedVisibilityScope = LocalNavAnimatedContentScope.current
        )
    } else Modifier

    val artists = album.artists.joinToString(", ") { it.name }
    val year = album.date.takeIf { it.isSet() }?.year?.toString()
    val subtitle = listOfNotNull(artists.ifBlank { null }, year).joinToString(" · ")
        .ifBlank { stringResource(R.string.detail_album_type) }

    EntityRowLayout(
        title = album.name,
        artworkUrl = artworkUrl,
        modifier = modifier,
        subtitle = { EntityRowSubtitle(subtitle) },
        density = density,
        artworkModifier = artworkModifier,
        artworkColor = color,
        monochrome = LocalUiSettings.current.monochromeAlbums,
        onRowClick = onRowClick,
        contentDescription = contentDescription,
        titleTestTag = "albumrow.title",
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
