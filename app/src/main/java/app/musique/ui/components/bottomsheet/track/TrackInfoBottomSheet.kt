package app.musique.ui.components.bottomsheet.track

import app.musique.ui.components.bottomsheet.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.*
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.theme.MusiqueType
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import app.musique.ui.components.bottomsheet.MusiqueModalSheet
import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.musique.ALBUM_COVER_URL
import app.musique.R
import app.musique.core.model.Artist
import app.musique.core.model.CoverSize
import app.musique.core.model.Track
import app.musique.core.model.getCover
import app.musique.data.setting.LocalUiSettings
import app.musique.ui.components.SmartImage
import app.musique.ui.notifications.InAppNotificationController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TrackInfoBottomSheet(
    track: Track,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    likedTrackIndex: Int? = null,
    isLiked: Boolean = false,
    onArtworkClick: (() -> Unit)? = null,
    onArtistClick: ((Artist) -> Unit)? = null,
    onOpenAlbum: (() -> Unit)? = null,
    onOpenArtist: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onToggleLike: (() -> Unit)? = null,
    onUseAsRecommendationSeed: (() -> Unit)? = null,
    onStartRadio: (() -> Unit)? = null,
    onOpenRadio: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onCopyUri: (() -> Unit)? = null,
    onScrollToLiked: (() -> Unit)? = null,
    onAddToWidget: (() -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboard.current

    val imageSize = 96.dp

    val artworkUrl =
        remember(track) { ALBUM_COVER_URL + (track.album?.getCover(CoverSize.MEDIUM)?.uri ?: "") }

    val defaultShare: () -> Unit = {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, "https://open.spotify.com/track/${track.id}")
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share track"))
    }

    val defaultCopy: () -> Unit = {
        scope.launch {
            val clipData = ClipData.newPlainText(
                context.getString(R.string.title_with_app, track.name),
                "https://open.spotify.com/track/${track.id}"
            )
            clipboardManager.setClipEntry(ClipEntry(clipData))
            InAppNotificationController.show(context.getString(R.string.toast_copied_to_clipboard))
        }
    }

    MusiqueModalSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        SheetContent(
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            SheetMediaHeader(
                artworkUrl = artworkUrl,
                title = track.name,
                monochrome = LocalUiSettings.current.monochromeTracks,
                onArtworkClick = {
                    onArtworkClick?.invoke()
                    onDismiss()
                },
                subtitle = { ArtistLinks(track.artists, onArtistClick, onDismiss) },
                trailing = {
                    SheetRoundButton(
                        icon = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = stringResource(R.string.like),
                        tint = if (isLiked) MusiqueColors.accent else MusiqueColors.textPrimary,
                        onClick = {
                            onToggleLike?.invoke()
                            onDismiss()
                        }
                    )
                },
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SheetQuickAction(
                    icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                    label = stringResource(R.string.common_play_next),
                    onClick = {
                        onPlayNext?.invoke()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                )
                SheetQuickAction(
                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                    label = stringResource(R.string.sheet_play_last),
                    onClick = {
                        onAddToQueue?.invoke()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                )
                SheetQuickAction(
                    icon = Icons.Rounded.IosShare,
                    label = stringResource(R.string.common_share),
                    onClick = { onShare?.invoke() ?: defaultShare() },
                    modifier = Modifier.weight(1f)
                )
            }

            SheetActionGroup {
                SheetActionRow(
                    label = stringResource(R.string.menu_add_to_playlist),
                    icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                    onClick = {
                        onAddToPlaylist?.invoke()
                        onDismiss()
                    }
                )
                SheetDivider()
                SheetActionRow(
                    label = stringResource(
                        if (isLiked) R.string.sheet_remove_from_liked else R.string.sheet_add_to_liked
                    ),
                    icon = if (isLiked) Icons.Rounded.HeartBroken else Icons.Rounded.FavoriteBorder,
                    destructive = isLiked,
                    onClick = {
                        onToggleLike?.invoke()
                        onDismiss()
                    }
                )
                SheetDivider()
                SheetActionRow(
                    label = stringResource(R.string.common_copy_link),
                    icon = Icons.Rounded.Link,
                    onClick = { onCopyUri?.invoke() ?: defaultCopy() }
                )
                SheetDivider()
                SheetActionRow(
                    label = stringResource(R.string.cd_add_to_widget),
                    icon = Icons.Rounded.Widgets,
                    onClick = { onAddToWidget?.invoke() }
                )
            }

            SheetActionGroup {
                SheetActionRow(
                    label = stringResource(R.string.sheet_go_to_album),
                    supportingText = track.album?.name,
                    icon = Icons.Rounded.Album,
                    onClick = {
                        onOpenAlbum?.invoke()
                        onDismiss()
                    }
                )
                SheetDivider()
                SheetActionRow(
                    label = stringResource(R.string.sheet_go_to_artist),
                    supportingText = track.artists.joinToString { it.name },
                    icon = Icons.Rounded.Person,
                    onClick = {
                        onOpenArtist?.invoke()
                        onDismiss()
                    }
                )
                if (likedTrackIndex != null && likedTrackIndex >= 0) {
                    SheetDivider()
                    SheetActionRow(
                        label = stringResource(R.string.menu_in_liked_songs),
                        supportingText = stringResource(R.string.liked_position, likedTrackIndex + 1),
                        icon = Icons.Rounded.Favorite,
                        onClick = {
                            onScrollToLiked?.invoke()
                            onDismiss()
                        }
                    )
                }
            }

            SheetActionGroup {
                SheetActionRow(
                    label = stringResource(R.string.menu_start_radio),
                    icon = Icons.Rounded.Radio,
                    onClick = {
                        onStartRadio?.invoke()
                        onDismiss()
                    }
                )
                SheetDivider()
                SheetActionRow(
                    label = stringResource(R.string.menu_open_radio),
                    icon = Icons.Rounded.Podcasts,
                    onClick = {
                        onOpenRadio?.invoke()
                        onDismiss()
                    }
                )
                SheetDivider()
                SheetActionRow(
                    label = stringResource(R.string.menu_recommendation_seed),
                    icon = Icons.Rounded.AutoAwesome,
                    onClick = {
                        onUseAsRecommendationSeed?.invoke()
                        onDismiss()
                    }
                )
            }
        }
    }
}

/** Comma separated artist names; each name opens that artist. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ArtistLinks(
    artists: List<Artist>,
    onArtistClick: ((Artist) -> Unit)?,
    onDismiss: () -> Unit,
) {
    FlowRow {
        artists.forEachIndexed { index, artist ->
            Text(
                text = artist.name + if (index < artists.lastIndex) ", " else "",
                style = MusiqueType.rowSubtitle,
                color = MusiqueColors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable {
                    onArtistClick?.invoke(artist)
                    onDismiss()
                }
            )
        }
    }
}
