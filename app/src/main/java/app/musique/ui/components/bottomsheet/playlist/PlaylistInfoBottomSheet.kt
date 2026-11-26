package app.musique.ui.components.bottomsheet.playlist

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
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Share
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
import app.musique.R
import app.musique.core.model.Playlist
import app.musique.data.setting.LocalUiSettings
import app.musique.ui.components.SmartImage
import app.musique.ui.notifications.InAppNotificationController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlaylistInfoBottomSheet(
    playlist: Playlist,
    artworkUrl: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenPlaylist: (() -> Unit)? = null,
    onOpenCreator: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null,
    onToggleLike: (() -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboard.current

    val imageSize = 96.dp

    val defaultShare: () -> Unit = {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, "https://open.spotify.com/playlist/${playlist.id}")
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share playlist"))
    }

    val defaultCopy: () -> Unit = {
        scope.launch {
            val clipData = ClipData.newPlainText(
                context.getString(R.string.title_with_app, playlist.attributes.name),
                "https://open.spotify.com/playlist/${playlist.id}"
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
                title = playlist.attributes.name,
                monochrome = LocalUiSettings.current.monochromePlaylists,
                onArtworkClick = {
                    onOpenPlaylist?.invoke()
                    onDismiss()
                },
                subtitle = {
                    Text(
                        text = stringResource(R.string.count_tracks, playlist.length),
                        style = MusiqueType.rowSubtitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                trailing = {
                    SheetRoundButton(
                        icon = Icons.Rounded.Favorite,
                        contentDescription = stringResource(R.string.like),
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
                    onClick = { defaultShare() },
                    modifier = Modifier.weight(1f)
                )
            }

            SheetActionGroup {
                SheetActionRow(
                    label = stringResource(R.string.menu_open_playlist),
                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                    onClick = {
                        onOpenPlaylist?.invoke()
                        onDismiss()
                    }
                )
                SheetDivider()
                SheetActionRow(
                    label = stringResource(R.string.sheet_go_to_profile),
                    icon = Icons.Rounded.Person,
                    onClick = {
                        onOpenCreator?.invoke()
                        onDismiss()
                    }
                )
                SheetDivider()
                SheetActionRow(
                    label = stringResource(R.string.common_copy_link),
                    icon = Icons.Rounded.Link,
                    onClick = { defaultCopy() }
                )
            }
        }
    }
}
