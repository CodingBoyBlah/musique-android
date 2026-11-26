package app.musique.ui.screens.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeExtendedFloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.musique.ALBUM_COVER_URL
import app.musique.fabBottomPadding
import app.musique.R
import app.musique.ScreenBottomPadding
import app.musique.core.model.Album
import app.musique.core.model.Artist
import app.musique.core.model.OutifyUri
import app.musique.core.model.toPlayableAudio
import app.musique.core.model.toSpotifyUri
import app.musique.ui.components.bottomsheet.library.FilterSortBottomSheet
import app.musique.ui.components.rows.SwipeableTrackRowConfigured
import app.musique.ui.components.musique.CapsuleSearchField
import app.musique.ui.components.musique.FloatingRoundButton
import app.musique.ui.components.musique.LargeTitleTopBar
import app.musique.ui.components.musique.PlayShuffleRow
import app.musique.ui.components.musique.TopBarIconButton
import app.musique.ui.components.musique.rememberCollapseFraction
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.theme.MusiqueDimens
import app.musique.ui.theme.MusiqueShapes
import app.musique.ui.theme.MusiqueType
import app.musique.ui.viewmodel.library.LikedViewModel
import coil3.request.ImageRequest
import coil3.request.allowHardware
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SharedTransitionScope.LikedScreen(
    viewModel: LikedViewModel,
    listState: LazyListState,
    scrollToIndex: Int = -1,
    onBack: () -> Unit,
    onArtistClick: (Artist) -> Unit,
    onArtworkClick: (Album) -> Unit,
) {
    val spirc = viewModel.spirc
    val tracks by viewModel.likedTracks.collectAsState()

    val context = LocalContext.current
    val density = LocalDensity.current

    var showFilterSheet by remember { mutableStateOf(false) }

    val explicitFilter by viewModel.filterExplicit.collectAsState()
    val artistNameFilter by viewModel.filterArtistName.collectAsState()
    val trackNameFilter by viewModel.filterTrackName.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()
    val sortAscending by viewModel.sortAscending.collectAsState()

    val imageSizePx = remember(density) {
        with(density) { 56.dp.roundToPx() }
    }

    // Prefetch images for visible range and trigger viewmodel to load more when near end
    LaunchedEffect(listState, tracks) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { first ->
                withContext(Dispatchers.IO) {
                    val end = (first + 8).coerceAtMost(tracks.lastIndex)
                    for (i in first..end) {
                        tracks.getOrNull(i)?.album?.covers?.firstOrNull()?.uri?.let { url ->
                            viewModel.imageLoader.enqueue(
                                ImageRequest.Builder(context)
                                    .data(ALBUM_COVER_URL + url)
                                    .size(imageSizePx)
                                    .allowHardware(true)
                                    .build()
                            )
                        }
                    }
                }
                viewModel.onVisibleIndex(first + 8)
            }
    }

    val currentAudio by viewModel.currentAudio.collectAsState(initial = null)
    val isPlaybackPlaying by viewModel.isPlaying.collectAsState(initial = false)
    val isFetchingTracks by viewModel.isFetchingTracks.collectAsState(initial = false)
    val fetchedCount by viewModel.fetchedCount.collectAsState(initial = 0)
    val totalCount by viewModel.totalCount.collectAsState(initial = 0)

    val searchQuery by viewModel.query.collectAsState()
    var transitioningTrackUri by remember { mutableStateOf<String?>(null) }
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val collapseFraction = rememberCollapseFraction(listState)
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val scope = rememberCoroutineScope()

    val showScrollToTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 2 ||
                    listState.firstVisibleItemScrollOffset > 100
        }
    }

    // Header items above the first track: count + play row, search row, optional sync banner.
    val headerItemCount = if (isRefreshing) 3 else 2
    LaunchedEffect(scrollToIndex, tracks) {
        if (scrollToIndex >= 0 && scrollToIndex < tracks.size) {
            listState.scrollToItem(scrollToIndex + headerItemCount)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MusiqueColors.canvas)
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    listState.scrollToItem(0)
                    viewModel.refresh()
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    top = statusBar + MusiqueDimens.topBarHeight + 4.dp,
                    bottom = ScreenBottomPadding + 16.dp,
                ),
                modifier = Modifier.fillMaxSize()
            ) {
                item(key = "liked_header") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MusiqueDimens.screenPadding)
                            .padding(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.account_song_count, totalCount),
                            style = MusiqueType.rowSubtitle,
                            color = MusiqueColors.textSecondary,
                        )
                        PlayShuffleRow(
                            onPlayClick = {
                                tracks.firstOrNull()?.let { first ->
                                    spirc.load(OutifyUri.Liked, first.toSpotifyUri())
                                    viewModel.setAudio(first.toPlayableAudio())
                                }
                            },
                            onShuffleClick = { spirc.shuffleLoad() },
                            primaryColor = MaterialTheme.colorScheme.primary,
                            onPrimaryColor = MaterialTheme.colorScheme.onPrimary,
                            enabled = tracks.isNotEmpty(),
                        )
                    }
                }

                item(key = "liked_search") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = MusiqueDimens.screenPadding, end = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CapsuleSearchField(
                            query = searchQuery,
                            onQueryChange = { new -> viewModel.onQueryChange(new) },
                            placeholder = stringResource(R.string.search_liked_placeholder),
                            modifier = Modifier.weight(1f)
                        )
                        TopBarIconButton(
                            icon = Icons.Rounded.FilterList,
                            contentDescription = stringResource(R.string.common_filter_and_sort),
                            onClick = { showFilterSheet = true },
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                if (isRefreshing) {
                    item(key = "liked_sync") {
                        SyncProgressBanner(
                            fetchedCount = fetchedCount,
                            totalCount = totalCount,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = MusiqueDimens.screenPadding, vertical = 4.dp)
                        )
                    }
                }

                items(
                    items = tracks,
                    key = { it.uri },
                    contentType = { "track" }
                ) { track ->
                    SwipeableTrackRowConfigured(
                        track = track,
                        currentAudio = currentAudio,
                        isPlaybackPlaying = isPlaybackPlaying,
                        onRowClick = remember(track.uri) {
                            {
                                transitioningTrackUri = track.uri
                                spirc.load(OutifyUri.Liked, track.toSpotifyUri())
                                // Optimistic UI
                                viewModel.setAudio(track.toPlayableAudio())
                            }
                        },
                        onArtworkClick = {
                            transitioningTrackUri = track.uri
                            track.album?.let(onArtworkClick)
                        },
                        onArtistClick = { artist ->
                            transitioningTrackUri = track.uri
                            onArtistClick(artist)
                        },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }

        LargeTitleTopBar(
            title = stringResource(R.string.common_liked_songs),
            collapseFraction = collapseFraction,
            onBack = onBack,
        )

        AnimatedVisibility(
            visible = showScrollToTop,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = fabBottomPadding(currentAudio != null)),
        ) {
            FloatingRoundButton(
                onClick = { scope.launch { listState.animateScrollToItem(0) } },
                contentDescription = stringResource(R.string.common_scroll_to_top),
            ) {
                Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = null, modifier = Modifier.size(22.dp))
            }
        }
    }

    if (showFilterSheet) {
        FilterSortBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            explicitFilter = explicitFilter,
            onExplicitFilterChange = viewModel::setFilterExplicit,
            artistNameFilter = artistNameFilter,
            onArtistNameFilterChange = viewModel::setFilterArtistName,
            trackNameFilter = trackNameFilter,
            onTrackNameFilterChange = viewModel::setFilterTrackName,
            sortBy = sortBy,
            onSortByChange = viewModel::setSortBy,
            sortAscending = sortAscending,
            onSortAscendingChange = viewModel::setSortAscending
        )
    }
}

@Composable
private fun SyncProgressBanner(
    fetchedCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MusiqueShapes.card,
        colors = CardDefaults.cardColors(
            containerColor = MusiqueColors.card
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.CloudSync,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.library_syncing_tracks),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MusiqueColors.textPrimary
                )
                Text(
                    text = if (totalCount > 50) {
                        "Syncing $fetchedCount of $totalCount. Larger libraries take longer."
                    } else {
                        "Syncing $fetchedCount of $totalCount"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MusiqueColors.textSecondary
                )
                if (totalCount > 0) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { fetchedCount.toFloat() / totalCount.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
