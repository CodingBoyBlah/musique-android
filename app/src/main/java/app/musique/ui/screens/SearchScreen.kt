package app.musique.ui.screens

import androidx.annotation.StringRes
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.NoAccounts
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import app.musique.core.model.Episode
import app.musique.core.model.Profile
import app.musique.ui.components.SmartImage
import app.musique.ui.components.musique.CapsuleSearchField
import app.musique.ui.components.musique.CategoryTile
import app.musique.ui.components.musique.FloatingRoundButton
import app.musique.ui.components.musique.LargeTitleTopBar
import app.musique.ui.components.musique.TopBarIconButton
import app.musique.ui.components.musique.rememberCollapseFraction
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.theme.MusiqueDimens
import app.musique.ui.theme.MusiqueShapes
import app.musique.ui.theme.MusiqueType
import app.musique.ui.theme.pressScale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.NoAccounts
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import app.musique.ALBUM_COVER_URL
import app.musique.fabBottomPadding
import app.musique.R
import app.musique.ScreenBottomPadding
import app.musique.core.EpisodeDetails
import app.musique.core.model.CoverSize
import app.musique.core.model.getCover
import app.musique.core.model.toOutifyUri
import app.musique.core.model.toPlayableAudio
import app.musique.core.model.toSpotifyUri
import app.musique.ui.GlobalPopupController
import app.musique.ui.PopupSpec
import app.musique.ui.components.SkeletonTrackRow
import app.musique.ui.components.navigation.Route
import app.musique.ui.components.navigation.popKeepingRoot
import app.musique.ui.components.navigation.Route.ArtistScreen
import app.musique.ui.components.navigation.Route.PlaylistScreen
import app.musique.ui.components.navigation.Route.TrackScreen
import app.musique.ui.components.rows.AlbumRow
import app.musique.ui.components.rows.ArtistRow
import app.musique.ui.components.rows.PlaylistRow
import app.musique.ui.components.rows.ShowRow
import app.musique.ui.components.rows.SwipeableEpisodeRowConfigured
import app.musique.ui.components.rows.SwipeableTrackRowConfigured
import app.musique.ui.components.user.UserChipAvatar
import app.musique.ui.viewmodel.SearchUiModel
import app.musique.ui.viewmodel.SearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedTransitionScope.SearchScreen(
    backStack: NavBackStack<NavKey>,
    viewModel: SearchViewModel,
    modifier: Modifier = Modifier,
    showSearchUi: Boolean = true,
) {
    val results by viewModel.results.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val historyResults by viewModel.historyResults.collectAsState()
    val spirc = viewModel.spirc

    val listState = rememberLazyListState()
    val currentTrack by viewModel.currentAudio.collectAsState(initial = null)
    val isPlaybackPlaying by viewModel.isPlaying.collectAsState(initial = false)
    val scope = rememberCoroutineScope()
    val collapseFraction = rememberCollapseFraction(listState)

    val showScrollToTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 2 ||
                    listState.firstVisibleItemScrollOffset > 100
        }
    }

    var query by rememberSaveable { mutableStateOf("") }
    var showAdvancedSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showTracks by rememberSaveable { mutableStateOf(true) }
    var showArtists by rememberSaveable { mutableStateOf(true) }
    var showAlbums by rememberSaveable { mutableStateOf(true) }
    var showPlaylists by rememberSaveable { mutableStateOf(true) }
    var showShows by rememberSaveable { mutableStateOf(true) }
    var showEpisodes by rememberSaveable { mutableStateOf(true) }

    val filteredResults = remember(
        results,
        showTracks,
        showArtists,
        showAlbums,
        showPlaylists,
        showShows,
        showEpisodes
    ) {
        applyFiltersToSectionedResults(
            results,
            showTracks,
            showArtists,
            showAlbums,
            showPlaylists,
            showShows,
            showEpisodes
        )
    }
    // The first real result is promoted to a Top Result card; the rest render as rows below it.
    val topResult = remember(filteredResults) {
        filteredResults.firstOrNull { it !is SearchUiModel.SectionHeader && it !is SearchUiModel.SkeletonItem }
    }

    val setQuery: (String) -> Unit = { newQuery ->
        query = newQuery
        viewModel.onQueryChange(newQuery)
    }

    val openShowForEpisode: (Episode) -> Unit = { episode ->
        val showUri = episode.showUri
        if (showUri.isNotBlank()) {
            backStack.add(Route.ShowScreen(showUri))
        } else {
            scope.launch {
                val details = withContext(Dispatchers.IO) {
                    runCatching {
                        val raw = viewModel.spClient.getEpisodeDetails(episode.id)
                        EpisodeDetails.fromJson(raw)
                    }.getOrNull()
                }
                if (details != null && details.showUri.isNotBlank()) {
                    backStack.add(Route.ShowScreen(details.showUri))
                }
            }
        }
    }

    /** One result row. [fromHistory] rows get a remove button and don't re-add themselves to history. */
    val resultRow: @Composable (SearchUiModel, Boolean, Modifier) -> Unit = { item, fromHistory, rowModifier ->
        val removeButton: (@Composable () -> Unit)? = if (fromHistory) {
            {
                IconButton(onClick = { viewModel.removeFromHistory(item.uri) }) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.cd_remove_from_history),
                        tint = MusiqueColors.textTertiary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        } else null
        val recordHistory: () -> Unit = { if (!fromHistory) viewModel.addToHistory(item) }

        when (item) {
            is SearchUiModel.SectionHeader -> {
                Text(
                    text = stringResource(id = item.titleRes),
                    style = MusiqueType.sectionTitle,
                    color = MusiqueColors.textPrimary,
                    modifier = rowModifier.padding(
                        start = MusiqueDimens.screenPadding,
                        end = MusiqueDimens.screenPadding,
                        top = 20.dp,
                        bottom = 6.dp,
                    )
                )
            }

            is SearchUiModel.SkeletonItem -> SkeletonTrackRow()

            is SearchUiModel.TrackItem -> {
                val track = item.track
                val isLiked by produceState(initialValue = false, track.uri) {
                    viewModel.isLiked(track.toOutifyUri()).collect { value = it }
                }
                SwipeableTrackRowConfigured(
                    track = track,
                    currentAudio = currentTrack,
                    isPlaybackPlaying = isPlaybackPlaying,
                    onRowClick = {
                        recordHistory()
                        spirc.load(track.toSpotifyUri()) // TODO: make context be the search screen
                        // Optimistic UI
                        viewModel.setAudio(track.toPlayableAudio())
                    },
                    onArtistClick = { backStack.add(ArtistScreen(it.uri)) },
                    onArtworkClick = {
                        val albumUri = track.album?.uri
                        if (albumUri != null) {
                            backStack.add(Route.AlbumScreen(albumUri))
                        } else {
                            backStack.add(TrackScreen(track.uri))
                        }
                    },
                    isLiked = isLiked,
                    trailingContent = removeButton,
                    modifier = rowModifier,
                )
            }

            is SearchUiModel.AlbumItem -> {
                val album = item.album
                AlbumRow(
                    album = album,
                    artworkUrl = album.getCover(CoverSize.MEDIUM)?.uri?.let { ALBUM_COVER_URL + it },
                    onRowClick = {
                        recordHistory()
                        backStack.add(Route.AlbumScreen(album.uri))
                    },
                    trailingContent = removeButton,
                    modifier = rowModifier,
                )
            }

            is SearchUiModel.ArtistItem -> {
                val artist = item.artist
                ArtistRow(
                    artist = artist,
                    artworkUrl = artist.getCover(CoverSize.MEDIUM)?.uri?.let { ALBUM_COVER_URL + it },
                    onRowClick = {
                        recordHistory()
                        backStack.add(ArtistScreen(artist.uri))
                    },
                    trailingContent = removeButton,
                    modifier = rowModifier,
                )
            }

            is SearchUiModel.PlaylistItem -> {
                val playlist = item.playlist
                var artworkUrl by remember(playlist.uri) { mutableStateOf<String?>(null) }
                val authors by produceState<List<Profile>>(emptyList(), playlist.uri) {
                    value = viewModel.getAuthors(playlist).take(2)
                }
                LaunchedEffect(playlist.uri) {
                    artworkUrl = viewModel.getArtworkUrl(playlist)
                }
                PlaylistRow(
                    playlist = playlist,
                    artworkUrl = artworkUrl,
                    onRowClick = {
                        recordHistory()
                        backStack.add(PlaylistScreen(playlist.uri))
                    },
                    onRowLongClick = {
                        GlobalPopupController.show(PopupSpec.PlaylistInfo(playlist, artworkUrl))
                    },
                    trailingContent = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy((-4).dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            authors.forEach { author ->
                                UserChipAvatar(
                                    artworkUrl = author.imageUrl,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clickable { backStack.add(Route.ProfileScreen(author.uri)) },
                                )
                            }
                            removeButton?.invoke()
                        }
                    },
                    contentDescription = playlist.attributes.description,
                    sharedTransitionScope = this@SearchScreen,
                    modifier = rowModifier,
                )
            }

            is SearchUiModel.EpisodeItem -> {
                val episode = item.episode
                val isLiked by produceState(initialValue = false, episode.uri) {
                    viewModel.isLiked(episode.toOutifyUri()).collect { value = it }
                }
                SwipeableEpisodeRowConfigured(
                    episode = episode,
                    isPlaybackPlaying = isPlaybackPlaying,
                    onRowClick = {
                        spirc.load(episode.toOutifyUri())
                        viewModel.setAudio(episode.toPlayableAudio())
                    },
                    onArtworkClick = { openShowForEpisode(episode) },
                    onShowNameClick = { openShowForEpisode(episode) },
                    isLiked = isLiked,
                    trailingContent = removeButton,
                    modifier = rowModifier,
                )
            }

            is SearchUiModel.ShowItem -> {
                val show = item.show
                ShowRow(
                    show = show,
                    onRowClick = {
                        backStack.add(Route.ShowScreen(show.uri))
                        recordHistory()
                    },
                    onRowLongClick = {},
                    onPublisherClick = {},
                    trailingContent = removeButton,
                    modifier = rowModifier,
                )
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MusiqueColors.canvas)
    ) {
        Column(Modifier.fillMaxSize()) {
            LargeTitleTopBar(
                title = stringResource(if (showSearchUi) R.string.search else R.string.search_recommendations),
                collapseFraction = collapseFraction,
                onBack = if (showSearchUi) null else ({ backStack.popKeepingRoot() }),
                barColor = MusiqueColors.canvas,
            )

            if (showSearchUi) {
                // Stays pinned under the title while results scroll, like Apple Music.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = MusiqueDimens.screenPadding, end = 8.dp, top = 4.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    CapsuleSearchField(
                        query = query,
                        onQueryChange = setQuery,
                        isLoading = isLoading,
                        modifier = Modifier.weight(1f),
                    )
                    TopBarIconButton(
                        icon = Icons.Rounded.Tune,
                        contentDescription = stringResource(R.string.cd_advanced_search),
                        onClick = { showAdvancedSheet = true },
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                if (query.isNotBlank()) {
                    FiltersBar(
                        showTracks = showTracks,
                        showArtists = showArtists,
                        showAlbums = showAlbums,
                        showPlaylists = showPlaylists,
                        showShows = showShows,
                        showEpisodes = showEpisodes,
                        onToggleTracks = { showTracks = it },
                        onToggleArtists = { showArtists = it },
                        onToggleAlbums = { showAlbums = it },
                        onTogglePlaylists = { showPlaylists = it },
                        onToggleShows = { showShows = it },
                        onToggleEpisodes = { showEpisodes = it }
                    )
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = ScreenBottomPadding + 16.dp)
            ) {
                if (!showSearchUi) {
                    if (isLoading) {
                        item(key = "rec_loading") {
                            Column { repeat(5) { SkeletonTrackRow() } }
                        }
                    } else {
                        items(items = results, key = { it.uri }) { item ->
                            if (item is SearchUiModel.TrackItem) {
                                resultRow(item, false, Modifier.animateItem())
                            }
                        }
                    }
                } else if (query.isBlank()) {
                    if (searchHistory.isNotEmpty()) {
                        item(key = "recent_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = MusiqueDimens.screenPadding, end = 4.dp, top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(R.string.search_recent),
                                    style = MusiqueType.sectionTitle,
                                    color = MusiqueColors.textPrimary,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = { viewModel.clearHistory() }) {
                                    Text(
                                        text = stringResource(R.string.clear_all),
                                        style = MusiqueType.button,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                        items(items = historyResults, key = { "history_${it.uri}" }) { item ->
                            resultRow(item, true, Modifier.animateItem())
                        }
                    }

                    item(key = "browse_header") {
                        Text(
                            text = stringResource(R.string.search_browse_categories),
                            style = MusiqueType.sectionTitle,
                            color = MusiqueColors.textPrimary,
                            modifier = Modifier.padding(
                                start = MusiqueDimens.screenPadding,
                                end = MusiqueDimens.screenPadding,
                                top = if (searchHistory.isNotEmpty()) 28.dp else 8.dp,
                                bottom = 12.dp,
                            ),
                        )
                    }
                    items(BrowseCategories.chunked(2), key = { row -> "browse_${row.first().label}" }) { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = MusiqueDimens.screenPadding)
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            row.forEach { category ->
                                val label = stringResource(category.label)
                                CategoryTile(
                                    title = label,
                                    gradientColors = category.colors,
                                    onClick = { setQuery(label) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                } else if (!isLoggedIn) {
                    item(key = "logged_out") {
                        SearchMessage(
                            icon = Icons.Rounded.NoAccounts,
                            title = stringResource(R.string.login_required),
                            action = stringResource(R.string.connect_to_spotify),
                            onAction = { backStack.add(Route.SettingsScreen) },
                        )
                    }
                } else if (filteredResults.isEmpty()) {
                    item(key = "no_results") {
                        SearchMessage(
                            icon = Icons.Rounded.SearchOff,
                            title = stringResource(R.string.search_no_results),
                            message = stringResource(R.string.search_type_query_select_filters),
                        )
                    }
                } else {
                    topResult?.let { top ->
                        item(key = "top_result_header") {
                            Text(
                                text = stringResource(R.string.search_top_result),
                                style = MusiqueType.sectionTitle,
                                color = MusiqueColors.textPrimary,
                                modifier = Modifier.padding(
                                    start = MusiqueDimens.screenPadding,
                                    end = MusiqueDimens.screenPadding,
                                    top = 8.dp,
                                    bottom = 10.dp,
                                ),
                            )
                        }
                        item(key = "top_result_${top.uri}") {
                            TopResultCard(
                                item = top,
                                viewModel = viewModel,
                                onOpen = {
                                    viewModel.addToHistory(top)
                                    when (top) {
                                        is SearchUiModel.TrackItem -> {
                                            spirc.load(top.track.toSpotifyUri())
                                            viewModel.setAudio(top.track.toPlayableAudio())
                                        }
                                        is SearchUiModel.AlbumItem -> backStack.add(Route.AlbumScreen(top.album.uri))
                                        is SearchUiModel.ArtistItem -> backStack.add(ArtistScreen(top.artist.uri))
                                        is SearchUiModel.PlaylistItem -> backStack.add(PlaylistScreen(top.playlist.uri))
                                        is SearchUiModel.ShowItem -> backStack.add(Route.ShowScreen(top.show.uri))
                                        is SearchUiModel.EpisodeItem -> openShowForEpisode(top.episode)
                                        else -> {}
                                    }
                                },
                                onPlay = {
                                    viewModel.addToHistory(top)
                                    when (top) {
                                        is SearchUiModel.TrackItem -> {
                                            spirc.load(top.track.toSpotifyUri())
                                            viewModel.setAudio(top.track.toPlayableAudio())
                                        }
                                        is SearchUiModel.AlbumItem -> spirc.load(top.album.toSpotifyUri())
                                        is SearchUiModel.PlaylistItem -> spirc.load(top.playlist.toSpotifyUri())
                                        is SearchUiModel.EpisodeItem -> {
                                            spirc.load(top.episode.toOutifyUri())
                                            viewModel.setAudio(top.episode.toPlayableAudio())
                                        }
                                        is SearchUiModel.ArtistItem -> backStack.add(ArtistScreen(top.artist.uri))
                                        is SearchUiModel.ShowItem -> backStack.add(Route.ShowScreen(top.show.uri))
                                        else -> {}
                                    }
                                },
                            )
                        }
                    }

                    items(
                        items = filteredResults.filter { it.uri != topResult?.uri },
                        key = { it.uri }
                    ) { item ->
                        resultRow(item, false, Modifier.animateItem())
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showScrollToTop,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = fabBottomPadding(currentTrack != null)),
        ) {
            FloatingRoundButton(
                onClick = { scope.launch { listState.scrollToItem(0) } },
                contentDescription = stringResource(R.string.common_scroll_to_top),
            ) {
                Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = null, modifier = Modifier.size(22.dp))
            }
        }
    }

    if (showAdvancedSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAdvancedSheet = false },
            sheetState = sheetState
        ) {
            AdvancedSearchContent(
                onSearch = { advancedQuery ->
                    setQuery(advancedQuery)
                    showAdvancedSheet = false
                },
                onDismiss = { showAdvancedSheet = false }
            )
        }
    }
}

private data class BrowseCategory(@StringRes val label: Int, val colors: List<Color>)

/** Idle-state genre tiles; tapping one searches for it. */
private val BrowseCategories = listOf(
    BrowseCategory(R.string.search_cat_chill, listOf(Color(0xFF2E7D8C), Color(0xFF15414D))),
    BrowseCategory(R.string.search_cat_focus, listOf(Color(0xFF5B5FC7), Color(0xFF2A2C6B))),
    BrowseCategory(R.string.search_cat_workout, listOf(Color(0xFFE5533D), Color(0xFF7A1F16))),
    BrowseCategory(R.string.search_cat_indie, listOf(Color(0xFFB0832F), Color(0xFF5A3F10))),
    BrowseCategory(R.string.search_cat_hiphop, listOf(Color(0xFF8E44AD), Color(0xFF3F1752))),
    BrowseCategory(R.string.search_cat_pop, listOf(Color(0xFFFA2F78), Color(0xFF7D0F38))),
    BrowseCategory(R.string.search_cat_electronic, listOf(Color(0xFF1E88E5), Color(0xFF0B3A66))),
    BrowseCategory(R.string.search_cat_rock, listOf(Color(0xFF6D6D6D), Color(0xFF242424))),
    BrowseCategory(R.string.search_cat_jazz, listOf(Color(0xFFC77D3A), Color(0xFF5C3110))),
    BrowseCategory(R.string.search_cat_late_night, listOf(Color(0xFF34417A), Color(0xFF10142E))),
)

/** Apple Music style Top Result: 68dp artwork, title, kind subtitle and a play button. */
@Composable
private fun TopResultCard(
    item: SearchUiModel,
    viewModel: SearchViewModel,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
) {
    var playlistArtwork by remember(item.uri) { mutableStateOf<String?>(null) }
    if (item is SearchUiModel.PlaylistItem) {
        LaunchedEffect(item.uri) { playlistArtwork = viewModel.getArtworkUrl(item.playlist) }
    }
    val (title, subtitle, artwork) = when (item) {
        is SearchUiModel.TrackItem -> Triple(
            item.track.name,
            stringResource(R.string.search_section_tracks) + " · " + item.track.artists.joinToString(", ") { it.name },
            item.track.album?.getCover(CoverSize.MEDIUM)?.uri?.let { ALBUM_COVER_URL + it },
        )
        is SearchUiModel.AlbumItem -> Triple(
            item.album.name,
            stringResource(R.string.detail_album_type) + " · " + item.album.artists.joinToString(", ") { it.name },
            item.album.getCover(CoverSize.MEDIUM)?.uri?.let { ALBUM_COVER_URL + it },
        )
        is SearchUiModel.ArtistItem -> Triple(
            item.artist.name,
            stringResource(R.string.home_kind_artist),
            item.artist.getCover(CoverSize.MEDIUM)?.uri?.let { ALBUM_COVER_URL + it },
        )
        is SearchUiModel.PlaylistItem -> Triple(
            item.playlist.attributes.name,
            stringResource(R.string.detail_playlist_type),
            playlistArtwork,
        )
        is SearchUiModel.ShowItem -> Triple(
            item.show.name,
            item.show.publisher,
            item.show.getCover(CoverSize.MEDIUM)?.uri?.let { ALBUM_COVER_URL + it },
        )
        is SearchUiModel.EpisodeItem -> Triple(item.episode.name, item.episode.showName, null)
        else -> Triple("", "", null)
    }
    val isArtist = item is SearchUiModel.ArtistItem
    val interaction = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MusiqueDimens.screenPadding)
            .pressScale(interaction, pressedScale = 0.98f)
            .clip(MusiqueShapes.card)
            .background(MusiqueColors.cardRaised)
            .clickable(interactionSource = interaction, indication = null, onClick = onOpen)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SmartImage(
            url = artwork,
            imageSize = 68.dp,
            shape = if (isArtist) CircleShape else MusiqueShapes.artworkSmall,
            contentDescription = null,
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MusiqueType.cardTitle.copy(fontSize = 17.sp),
                color = MusiqueColors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MusiqueType.cardSubtitle,
                color = MusiqueColors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        val playInteraction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .size(44.dp)
                .pressScale(playInteraction, pressedScale = 0.9f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(interactionSource = playInteraction, indication = null, onClick = onPlay),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = stringResource(R.string.cd_play_item),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun SearchMessage(
    icon: ImageVector,
    title: String,
    message: String? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MusiqueColors.textTertiary,
            modifier = Modifier.size(52.dp),
        )
        Text(
            text = title,
            style = MusiqueType.rowTitle,
            color = MusiqueColors.textPrimary,
            textAlign = TextAlign.Center,
        )
        if (message != null) {
            Text(
                text = message,
                style = MusiqueType.cardSubtitle,
                color = MusiqueColors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(text = action, style = MusiqueType.button, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MaterialSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = true,
    placeholderText: String = stringResource(R.string.search_spotify),
) {
    val expanded = false

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    SearchBar(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        windowInsets = WindowInsets(0.dp),
        inputField = {
            SearchBarDefaults.InputField(
                modifier = Modifier.focusRequester(focusRequester),
                query = query,
                onQueryChange = { new ->
                    onQueryChange(new)
                },
                onSearch = {
                    onQueryChange(query)
                    keyboardController?.hide()
                },
                expanded = expanded,
                onExpandedChange = { /* no-op: keep collapsed so results render below */ },
                placeholder = { Text(text = placeholderText) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = stringResource(R.string.search))
                },
                trailingIcon = {
                    if (isLoading) {
                        ContainedLoadingIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        IconButton(onClick = {
                            onQueryChange("")
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.cd_clear_query)
                            )
                        }
                    }
                }
            )
        },
        expanded = expanded,
        onExpandedChange = { /* ignored */ }
    ) {}
}

@Composable
fun FiltersBar(
    modifier: Modifier = Modifier,
    showTracks: Boolean,
    showArtists: Boolean,
    showAlbums: Boolean,
    showPlaylists: Boolean,
    showShows: Boolean,
    showEpisodes: Boolean,
    onToggleTracks: (Boolean) -> Unit,
    onToggleArtists: (Boolean) -> Unit,
    onToggleAlbums: (Boolean) -> Unit,
    onTogglePlaylists: (Boolean) -> Unit,
    onToggleShows: (Boolean) -> Unit,
    onToggleEpisodes: (Boolean) -> Unit,
    chipSpacing: Dp = 8.dp
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .horizontalScroll(scrollState)
            .padding(horizontal = MusiqueDimens.screenPadding, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(chipSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reusable builder for chips
        @Composable
        fun FilterChipWithTick(
            selected: Boolean,
            onClick: () -> Unit,
            label: @Composable () -> Unit
        ) {
            FilterChip(
                selected = selected,
                onClick = onClick,
                label = label,
                shape = RoundedCornerShape(20.dp),
                leadingIcon = {
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                },
                border = null,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MusiqueColors.cardRaised,
                    labelColor = MusiqueColors.textPrimary,
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                )
            )
        }

        FilterChipWithTick(
            selected = showTracks,
            onClick = { onToggleTracks(!showTracks) }
        ) { Text(stringResource(R.string.search_section_tracks)) }

        FilterChipWithTick(
            selected = showArtists,
            onClick = { onToggleArtists(!showArtists) }
        ) { Text(stringResource(R.string.search_section_artists)) }

        FilterChipWithTick(
            selected = showAlbums,
            onClick = { onToggleAlbums(!showAlbums) }
        ) { Text(stringResource(R.string.search_section_albums)) }

        FilterChipWithTick(
            selected = showPlaylists,
            onClick = { onTogglePlaylists(!showPlaylists) }
        ) { Text(stringResource(R.string.search_section_playlists)) }

        FilterChipWithTick(
            selected = showShows,
            onClick = { onToggleShows(!showShows) }
        ) { Text(stringResource(R.string.search_section_shows)) }

        FilterChipWithTick(
            selected = showEpisodes,
            onClick = { onToggleEpisodes(!showEpisodes) }
        ) { Text(stringResource(R.string.search_section_episodes)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdvancedSearchContent(
    onSearch: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var album by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var matchAll by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.search_advanced),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(stringResource(R.string.adv_title)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = artist,
            onValueChange = { artist = it },
            label = { Text(stringResource(R.string.common_artist)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = album,
            onValueChange = { album = it },
            label = { Text(stringResource(R.string.common_album)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = year,
            onValueChange = { year = it },
            label = { Text(stringResource(R.string.adv_year)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = genre,
            onValueChange = { genre = it },
            label = { Text(stringResource(R.string.adv_genre)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = label,
            onValueChange = { label = it },
            label = { Text(stringResource(R.string.adv_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.adv_match))
            Spacer(Modifier.width(12.dp))
            FilterChip(
                selected = matchAll,
                onClick = { matchAll = true },
                label = { Text(stringResource(R.string.adv_all_and)) }
            )
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = !matchAll,
                onClick = { matchAll = false },
                label = { Text(stringResource(R.string.adv_any_or)) }
            )
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = {
                title = ""; artist = ""; album = ""
                year = ""; genre = ""; label = ""
                matchAll = true
            }) { Text(stringResource(R.string.common_clear)) }
            Spacer(Modifier.width(12.dp))
            Button(onClick = {
                val advancedQuery = buildAdvancedQuery(
                    title, artist, album, year, genre, label, matchAll
                )
                onSearch(advancedQuery)
            }) { Text(stringResource(R.string.search)) }
        }

        Spacer(Modifier.height(16.dp))
    }
}

private fun buildAdvancedQuery(
    title: String,
    artist: String,
    album: String,
    year: String,
    genre: String,
    label: String,
    matchAll: Boolean
): String {
    val parts = mutableListOf<String>()

    fun add(field: String, value: String) {
        if (value.isNotBlank()) {
            val quoted = if (value.contains(" ") && field != "year") "\"$value\"" else value
            parts.add("$field:$quoted")
        }
    }

    add("title", title)
    add("artist", artist)
    add("album", album)
    add("year", year)
    add("genre", genre)
    add("label", label)

    return when {
        parts.isEmpty() -> ""
        parts.size == 1 || matchAll -> parts.joinToString(" ")
        else -> parts.joinToString(" OR ")
    }
}

private fun applyFiltersToSectionedResults(
    results: List<SearchUiModel>,
    showTracks: Boolean,
    showArtists: Boolean,
    showAlbums: Boolean,
    showPlaylists: Boolean,
    showShows: Boolean,
    showEpisodes: Boolean
): List<SearchUiModel> {
    val out = mutableListOf<SearchUiModel>()
    var i = 0
    while (i < results.size) {
        val item = results[i]
        if (item is SearchUiModel.SectionHeader) {
            // collect the section items
            val sectionItems = mutableListOf<SearchUiModel>()
            var j = i + 1
            while (j < results.size && results[j] !is SearchUiModel.SectionHeader) {
                sectionItems.add(results[j])
                j++
            }

            // filter the section items based on selected filters
            val filtered = sectionItems.filter { si ->
                when (si) {
                    is SearchUiModel.TrackItem -> showTracks
                    is SearchUiModel.ArtistItem -> showArtists
                    is SearchUiModel.AlbumItem -> showAlbums
                    is SearchUiModel.PlaylistItem -> showPlaylists
                    is SearchUiModel.ShowItem -> showShows
                    is SearchUiModel.EpisodeItem -> showEpisodes
                    else -> true
                }
            }

            if (filtered.isNotEmpty()) {
                out.add(item) // header
                out.addAll(filtered)
            }

            i = j // skip to next header or end
        } else {
            val include = when (item) {
                is SearchUiModel.TrackItem -> showTracks
                is SearchUiModel.ArtistItem -> showArtists
                is SearchUiModel.AlbumItem -> showAlbums
                is SearchUiModel.PlaylistItem -> showPlaylists
                is SearchUiModel.EpisodeItem -> showEpisodes
                is SearchUiModel.ShowItem -> showShows
                is SearchUiModel.SkeletonItem -> true
                is SearchUiModel.SectionHeader -> true
            }
            if (include) out.add(item)
            i++
        }
    }
    return out
}
