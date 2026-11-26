package app.musique.ui.components.bottomsheet.playlist

import app.musique.ui.components.bottomsheet.*
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.theme.MusiqueType
import androidx.compose.foundation.background
import app.musique.ui.components.bottomsheet.MusiqueModalSheet
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.musique.ALBUM_COVER_URL
import app.musique.R
import app.musique.core.model.CoverSize
import app.musique.core.model.Playlist
import app.musique.core.model.Track
import app.musique.core.model.getCover
import app.musique.data.setting.LocalUiSettings
import app.musique.ui.components.SmartImage
import app.musique.ui.viewmodel.bottomsheet.AddToPlaylistViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistBottomSheet(
    viewModel: AddToPlaylistViewModel,
    tracks: List<Track>,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false,
    )

    val coroutineScope = rememberCoroutineScope()

    val playlists by viewModel.ownedPlaylists.collectAsState(initial = emptyList())

    val trackIds = remember(tracks) { tracks.map { it.id }.toSet() }

    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var isRemoveMode by remember { mutableStateOf(false) }

    if (selectedPlaylist != null) {
        TrackSelectionBottomSheet(
            viewModel = viewModel,
            tracks = tracks,
            playlist = selectedPlaylist!!,
            isRemoveMode = isRemoveMode,
            onDismiss = { selectedPlaylist = null },
            onConfirm = {
                selectedPlaylist = null
                onDismiss?.invoke()
            }
        )
    } else {
        MusiqueModalSheet(
            onDismissRequest = {
                coroutineScope.launch {
                    sheetState.hide()
                    onDismiss?.invoke()
                }
            },
            sheetState = sheetState,
            modifier = modifier,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            ) {
                // Header with track info
                TrackInfoHeader(tracks = tracks)

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                if (tracks.size == 1) {
                    SheetSectionLabel(stringResource(R.string.queue_your_playlists))
                } else {
                    SheetSectionLabel(stringResource(R.string.count_tracks, tracks.size))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Playlist list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(
                        items = playlists,
                        key = { it.playlist.id }
                    ) { playlistUi ->
                        val playlist = playlistUi.playlist
                        val existingCount = playlist.contents.count { it.id in trackIds }
                        val isInPlaylist = existingCount > 0

                        PlaylistItem(
                            playlistName = playlist.attributes.name,
                            artworkUrl = playlistUi.artworkUrl,
                            trackCount = playlist.length,
                            existingCount = existingCount,
                            isInPlaylist = isInPlaylist,
                            onAddClick = {
                                if (tracks.size == 1) {
                                    viewModel.addTrackToPlaylist(tracks.first(), playlist)
                                    onDismiss?.invoke()
                                } else {
                                    isRemoveMode = false
                                    selectedPlaylist = playlist
                                }
                            },
                            onRemoveClick = {
                                if (tracks.size == 1) {
                                    viewModel.removeTrackFromPlaylist(tracks.first(), playlist)
                                    onDismiss?.invoke()
                                } else {
                                    isRemoveMode = true
                                    selectedPlaylist = playlist
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrackInfoHeader(tracks: List<Track>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        if (tracks.size == 1) {
            val track = tracks.first()
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(56.dp)
                ) {
                    val artworkUrl = remember(track) {
                        ALBUM_COVER_URL + (track.album?.getCover(CoverSize.MEDIUM)?.uri ?: "")
                    }
                    SmartImage(
                        url = artworkUrl,
                        contentDescription = stringResource(R.string.common_artwork),
                        modifier = Modifier.fillMaxSize(),
                        monochrome = LocalUiSettings.current.monochromeTracks
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.name,
                        style = MusiqueType.cardTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artists.joinToString(", ") { it.name },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            // Multiple tracks - show preview
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tracks.take(3).forEach { track ->
                    val artworkUrl = remember(track) {
                        ALBUM_COVER_URL + (track.album?.getCover(CoverSize.SMALL)?.uri ?: "")
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.size(48.dp)
                    ) {
                        if (artworkUrl.isNotBlank()) {
                            SmartImage(
                                url = artworkUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                monochrome = LocalUiSettings.current.monochromeTracks
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MusicNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.queue_tracks_selected, tracks.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlaylistItem(
    playlistName: String,
    artworkUrl: String?,
    trackCount: Int,
    existingCount: Int,
    isInPlaylist: Boolean,
    onAddClick: () -> Unit,
    onRemoveClick: () -> Unit,
) {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(interactionSource = interaction, indication = null) {
                if (isInPlaylist) onRemoveClick() else onAddClick()
            }
            .padding(vertical = 6.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MusiqueSheetDefaults.groupColor)
        ) {
            if (!artworkUrl.isNullOrBlank()) {
                SmartImage(
                    url = artworkUrl,
                    contentDescription = null,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxSize(),
                    monochrome = LocalUiSettings.current.monochromeImages
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                    contentDescription = null,
                    tint = MusiqueColors.textTertiary
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlistName,
                style = MusiqueType.rowTitle,
                color = MusiqueColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (isInPlaylist) "$existingCount track${if (existingCount > 1) "s" else ""} in playlist" else "$trackCount tracks",
                style = MusiqueType.rowSubtitle,
                color = MusiqueColors.textTertiary,
                maxLines = 1
            )
        }

        Spacer(Modifier.width(8.dp))

        SheetRoundButton(
            icon = if (isInPlaylist) Icons.Default.Remove else Icons.Default.Add,
            contentDescription = stringResource(if (isInPlaylist) R.string.common_remove else R.string.common_add),
            tint = if (isInPlaylist) MusiqueSheetDefaults.destructive else MaterialTheme.colorScheme.primary,
            onClick = { if (isInPlaylist) onRemoveClick() else onAddClick() }
        )
    }
}
