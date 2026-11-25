package app.musique.ui.screens.library

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.musique.ALBUM_COVER_URL
import app.musique.R
import app.musique.ScreenBottomPadding
import app.musique.core.model.Artist
import app.musique.core.model.CoverSize
import app.musique.core.model.Profile
import app.musique.core.model.Track
import app.musique.core.model.getCover
import app.musique.core.model.toPlayableAudio
import app.musique.core.model.toSpotifyUri
import app.musique.data.setting.LocalSwipeActionHandler
import app.musique.data.setting.LocalSwipeGestureSettings
import app.musique.data.setting.LocalUiSettings
import app.musique.data.setting.buildLongPressAction
import app.musique.fabBottomPadding
import app.musique.ui.GlobalPopupController
import app.musique.ui.PopupSpec
import app.musique.ui.components.ErrorScreen
import app.musique.ui.components.PlaylistDetailSkeleton
import app.musique.ui.components.musique.CapsuleSearchField
import app.musique.ui.components.musique.CiderDetailTrackRow
import app.musique.ui.components.musique.CiderDetailTrackSkeletonRow
import app.musique.ui.components.musique.DetailFloatingTopBar
import app.musique.ui.components.musique.DetailHeroHeader
import app.musique.ui.components.musique.DetailPaletteGradientBackdrop
import app.musique.ui.components.musique.FloatingRoundButton
import app.musique.ui.components.musique.GroupedTrackItemContainer
import app.musique.ui.components.musique.GroupedTrackList
import app.musique.ui.components.musique.PlayShuffleRow
import app.musique.ui.components.musique.formatSongCountLabel
import app.musique.ui.components.musique.formatTotalRuntimeLabel
import app.musique.ui.components.musique.rememberCollapseFraction
import app.musique.ui.components.rows.SwipeableRowWithGestures
import app.musique.ui.components.rows.rememberTrackGestures
import app.musique.ui.components.user.UserChipAvatar
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.theme.MusiqueDimens
import app.musique.ui.theme.MusiqueType
import app.musique.ui.theme.rememberArtworkPalette
import app.musique.ui.viewmodel.detail.PlaylistDetailViewModel
import app.musique.ui.viewmodel.detail.PlaylistUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedTransitionScope.PlaylistScreen(
    viewModel: PlaylistDetailViewModel,
    onArtworkClick: (track: Track) -> Unit,
    onArtistClick: (artist: Artist) -> Unit,
    onAuthorClick: (profile: Profile) -> Unit,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    when (uiState) {
        is PlaylistUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize()) {
                PlaylistDetailSkeleton()
                DetailFloatingTopBar(
                    title = "",
                    collapseFraction = 0f,
                    onBack = onBack,
                )
            }
        }

        is PlaylistUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize()) {
                ErrorScreen(
                    message = (uiState as PlaylistUiState.Error).error,
                    onRetry = { viewModel.retry() },
                )
                DetailFloatingTopBar(
                    title = "",
                    collapseFraction = 0f,
                    onBack = onBack,
                )
            }
        }

        is PlaylistUiState.Success -> {
            val playlist = (uiState as PlaylistUiState.Success).playlist!!
            val tracks = playlist.contents
            val likedIds by viewModel.likedTrackIds.collectAsState(initial = emptySet())
            val isRefreshing by viewModel.isRefreshing.collectAsState()
            val isSaved by viewModel.isSaved.collectAsState()
            val currentTrack by viewModel.currentAudio.collectAsState(initial = null)
            val isPlaybackPlaying by viewModel.isPlaying.collectAsState(initial = false)
            val spirc = viewModel.spirc

            var searchQuery by remember { mutableStateOf("") }
            var showSearch by remember { mutableStateOf(false) }
            var showMoreMenu by remember { mutableStateOf(false) }

            var artworkUrl by remember { mutableStateOf<String?>(null) }
            val authorMap by viewModel.authors.collectAsState()
            var authors by remember { mutableStateOf(emptyList<Profile>()) }
            LaunchedEffect(playlist.uri) {
                val resolved = viewModel.getArtworkUrl(playlist)
                artworkUrl = resolved.takeIf { it.isNotBlank() && it != "unknown cover" }
                authors = viewModel.getAuthors(playlist)
            }

            val palette = rememberArtworkPalette(artworkUrl)
            val loadedTrackMetadata by viewModel.trackMetadata.collectAsState()

            val playlistRows = remember(playlist.uri, tracks) {
                viewModel.buildPlaylistRows(playlist)
            }
            val filteredRows = remember(playlistRows, searchQuery, loadedTrackMetadata) {
                if (searchQuery.isBlank()) playlistRows
                else playlistRows.filter { row ->
                    val state = viewModel.getTrackState(row.trackUri)
                    state?.name?.contains(searchQuery, ignoreCase = true) == true ||
                        state?.artists?.any { it.name.contains(searchQuery, ignoreCase = true) } == true
                }
            }

            val lazyList = rememberLazyListState()
            val scope = rememberCoroutineScope()
            val context = LocalContext.current
            val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val collapseFraction = rememberCollapseFraction(lazyList, thresholdDp = 280.dp)

            val showScrollToTop by remember {
                derivedStateOf {
                    lazyList.firstVisibleItemIndex > 2 ||
                        lazyList.firstVisibleItemScrollOffset > 300
                }
            }

            val playlistUri = playlist.toSpotifyUri()
            val primaryAuthor = authors.firstOrNull()
            val creatorName = primaryAuthor?.name?.takeIf { it.isNotBlank() }
                ?: playlist.ownerUsername.ifBlank { stringResource(R.string.detail_unknown_creator) }
            val creatorAvatars = remember(authors) {
                authors.mapNotNull { it.imageUrl }
            }
            val playlistTypeLabel = stringResource(R.string.detail_playlist_type)
            val songCountText = formatSongCountLabel(tracks.size)
            val metadataLine = "$playlistTypeLabel · $songCountText"
            val secondaryActionColor = palette.surface.copy(alpha = 0.94f)
            val swipeSettings = LocalSwipeGestureSettings.current
            val swipeHandler = LocalSwipeActionHandler.current

            Box(modifier = Modifier.fillMaxSize()) {
                DetailPaletteGradientBackdrop(palette = palette)

                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        state = lazyList,
                        contentPadding = PaddingValues(
                            top = statusBarTop + MusiqueDimens.collapsedBarHeight + 12.dp,
                            bottom = ScreenBottomPadding + 24.dp,
                        ),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item(key = "playlist_hero") {
                            DetailHeroHeader(
                                title = playlist.attributes.name,
                                artworkUrl = artworkUrl,
                                creatorName = creatorName,
                                creatorAvatarUrls = creatorAvatars,
                                metadataLine = metadataLine,
                                description = playlist.attributes.description.takeIf { it.isNotBlank() },
                                onCreatorClick = primaryAuthor?.let { author ->
                                    { onAuthorClick(author) }
                                },
                                monochrome = LocalUiSettings.current.monochromePlaylists,
                            )
                        }

                        item(key = "playlist_actions") {
                            PlayShuffleRow(
                                onPlayClick = {
                                    val firstTrack = filteredRows.firstOrNull()?.let { row ->
                                        viewModel.getTrackState(row.trackUri)
                                    }
                                    if (firstTrack != null) {
                                        spirc.load(playlistUri, firstTrack.toSpotifyUri())
                                        viewModel.setAudio(firstTrack.toPlayableAudio())
                                    } else {
                                        spirc.load(playlistUri)
                                    }
                                },
                                onShuffleClick = { spirc.shuffleLoad(playlist.uri) },
                                onSaveClick = { viewModel.toggleSave() },
                                isSaved = isSaved,
                                primaryColor = Color.White,
                                onPrimaryColor = MusiqueColors.onDarkPlay,
                                secondaryColor = secondaryActionColor,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                            )
                        }

                        if (showSearch || searchQuery.isNotEmpty()) {
                            item(key = "playlist_search") {
                                CapsuleSearchField(
                                    query = searchQuery,
                                    onQueryChange = { searchQuery = it },
                                    placeholder = stringResource(R.string.search_tracks_placeholder),
                                    containerColor = palette.surface.copy(alpha = 0.85f),
                                    modifier = Modifier.padding(
                                        start = MusiqueDimens.screenPadding,
                                        end = MusiqueDimens.screenPadding,
                                        bottom = 12.dp,
                                    ),
                                )
                            }
                        }

                        if (playlistRows.isEmpty()) {
                            item(key = "playlist_empty") {
                                GroupedTrackList(
                                    containerColor = palette.surface,
                                    modifier = Modifier.padding(horizontal = MusiqueDimens.screenPadding),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 36.dp, horizontal = 24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.MusicNote,
                                            contentDescription = null,
                                            modifier = Modifier.size(44.dp),
                                            tint = MusiqueColors.textTertiary,
                                        )
                                        Text(
                                            text = stringResource(R.string.playlist_empty),
                                            style = MusiqueType.rowTitle,
                                            color = MusiqueColors.textPrimary,
                                        )
                                        Text(
                                            text = stringResource(R.string.playlist_try_add),
                                            style = MusiqueType.cardSubtitle,
                                            color = MusiqueColors.textSecondary,
                                        )
                                    }
                                }
                            }
                        } else {
                            itemsIndexed(
                                items = filteredRows,
                                key = { _, row -> row.key },
                                contentType = { _, _ -> "playlist_track_row" },
                            ) { index, row ->
                                val track by remember(row.trackUri) {
                                    viewModel.trackFlow(row.trackUri)
                                }.collectAsState(initial = null)

                                LaunchedEffect(row.trackUri) {
                                    if (track == null) viewModel.getOrLoadTrack(row.trackUri)
                                }

                                GroupedTrackItemContainer(
                                    index = index,
                                    count = filteredRows.size,
                                    containerColor = palette.surface,
                                    dividerStartInset = 104.dp,
                                ) {
                                    val resolved = track
                                    if (resolved == null) {
                                        CiderDetailTrackSkeletonRow(showArtwork = true)
                                    } else {
                                        val isLiked = resolved.id in likedIds
                                        val isCurrent = currentTrack?.uri == resolved.uri
                                        val trackCoverUrl = (
                                            resolved.album?.getCover(CoverSize.SMALL)?.uri
                                                ?: resolved.album?.getCover(CoverSize.MEDIUM)?.uri
                                            )?.let { ALBUM_COVER_URL + it }
                                        val artistSubtitle = remember(resolved.artists) {
                                            resolved.artists.joinToString(", ") { it.name }
                                        }
                                        val (startGestures, endGestures) = rememberTrackGestures(
                                            resolved,
                                            isLiked,
                                        )
                                        val longPressAction = remember(swipeSettings, resolved) {
                                            buildLongPressAction(swipeSettings, swipeHandler, resolved)
                                        }
                                        val author = authorMap[row.addedBy]

                                        SwipeableRowWithGestures(
                                            startGestures = startGestures,
                                            endGestures = endGestures,
                                            containerColor = palette.surface,
                                        ) {
                                            CiderDetailTrackRow(
                                                trackIndex = index,
                                                title = resolved.name,
                                                durationMs = resolved.duration,
                                                subtitle = artistSubtitle,
                                                artworkUrl = trackCoverUrl,
                                                showArtwork = true,
                                                isExplicit = resolved.explicit,
                                                isCurrent = isCurrent,
                                                isPlaying = isPlaybackPlaying,
                                                accentColor = palette.accent,
                                                monochrome = LocalUiSettings.current.monochromeTracks,
                                                onClick = {
                                                    spirc.load(playlistUri, resolved.toSpotifyUri())
                                                    viewModel.setAudio(resolved.toPlayableAudio())
                                                },
                                                onLongClick = {
                                                    if (longPressAction != null) {
                                                        longPressAction()
                                                    } else {
                                                        GlobalPopupController.show(
                                                            PopupSpec.TrackInfo(
                                                                track = resolved,
                                                                isLiked = isLiked,
                                                            )
                                                        )
                                                    }
                                                },
                                                onArtworkClick = { onArtworkClick(resolved) },
                                                onMoreClick = {
                                                    GlobalPopupController.show(
                                                        PopupSpec.TrackInfo(
                                                            track = resolved,
                                                            isLiked = isLiked,
                                                        )
                                                    )
                                                },
                                                trailingAccessory = if (author != null && authors.size > 1) {
                                                    {
                                                        UserChipAvatar(
                                                            artworkUrl = author.imageUrl,
                                                            size = 20.dp,
                                                            modifier = Modifier.clickable {
                                                                onAuthorClick(author)
                                                            },
                                                        )
                                                    }
                                                } else null,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (tracks.isNotEmpty()) {
                            item(key = "playlist_footer") {
                                val loadedTotalMs = remember(loadedTrackMetadata, playlistRows) {
                                    playlistRows.sumOf { loadedTrackMetadata[it.trackUri]?.duration ?: 0L }
                                }
                                val footerText = if (loadedTotalMs > 0L) {
                                    stringResource(
                                        R.string.detail_count_runtime_footer,
                                        songCountText,
                                        formatTotalRuntimeLabel(loadedTotalMs),
                                    )
                                } else {
                                    songCountText
                                }
                                Text(
                                    text = footerText,
                                    style = MusiqueType.cardSubtitle,
                                    color = MusiqueColors.textSecondary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 14.dp),
                                )
                            }
                        }
                    }
                }

                DetailFloatingTopBar(
                    title = playlist.attributes.name,
                    collapseFraction = collapseFraction,
                    onBack = onBack,
                    tintColor = palette.backgroundTop,
                    trailingContent = {
                        FloatingRoundButton(
                            onClick = { showSearch = !showSearch },
                            contentDescription = stringResource(R.string.search),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                modifier = Modifier.size(19.dp),
                            )
                        }

                        Box {
                            FloatingRoundButton(
                                onClick = { showMoreMenu = true },
                                contentDescription = stringResource(R.string.common_more_information),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreHoriz,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                            }

                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false },
                                containerColor = MusiqueColors.chrome,
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.playlist_edit), color = MusiqueColors.textPrimary) },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.Edit, contentDescription = null, tint = MusiqueColors.textPrimary)
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        GlobalPopupController.show(
                                            PopupSpec.ModifyPlaylist(
                                                playlistId = playlist.id,
                                                name = playlist.attributes.name,
                                                description = playlist.attributes.description,
                                                collaborative = playlist.attributes.isCollaborative,
                                            )
                                        )
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.common_more_information), color = MusiqueColors.textPrimary) },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.Info, contentDescription = null, tint = MusiqueColors.textPrimary)
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        GlobalPopupController.show(
                                            PopupSpec.PlaylistInfo(
                                                playlist = playlist,
                                                artworkUrl = artworkUrl,
                                            )
                                        )
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.common_share), color = MusiqueColors.textPrimary) },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.Share, contentDescription = null, tint = MusiqueColors.textPrimary)
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            putExtra(Intent.EXTRA_TEXT, "https://open.spotify.com/playlist/${playlist.id}")
                                            type = "text/plain"
                                        }
                                        context.startActivity(
                                            Intent.createChooser(shareIntent, context.getString(R.string.common_share))
                                        )
                                    },
                                )
                            }
                        }
                    },
                )

                AnimatedVisibility(
                    visible = showScrollToTop,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = fabBottomPadding(currentTrack != null)),
                ) {
                    FloatingActionButton(
                        onClick = { scope.launch { lazyList.animateScrollToItem(0) } },
                        shape = CircleShape,
                        containerColor = MusiqueColors.chromeTranslucent,
                        contentColor = MusiqueColors.textPrimary,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowUp,
                            contentDescription = stringResource(R.string.common_scroll_to_top),
                        )
                    }
                }
            }
        }
    }
}
