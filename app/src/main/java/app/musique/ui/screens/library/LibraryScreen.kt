package app.musique.ui.screens.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MicExternalOn
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import app.musique.ALBUM_COVER_URL
import app.musique.R
import app.musique.ScreenBottomPadding
import app.musique.core.model.Album
import app.musique.core.model.Artist
import app.musique.core.model.CoverSize
import app.musique.core.model.Playlist
import app.musique.core.model.PlaylistFolder
import app.musique.core.model.Profile
import app.musique.core.model.getCover
import app.musique.core.model.toColor
import app.musique.ui.GlobalPopupController
import app.musique.ui.PopupSpec
import app.musique.ui.components.ErrorScreen
import app.musique.ui.components.bottomsheet.library.CreateFolderBottomSheet
import app.musique.ui.components.bottomsheet.library.MoveToFolderBottomSheet
import app.musique.ui.components.musique.AppleListRow
import app.musique.ui.components.musique.CapsuleSearchField
import app.musique.ui.components.musique.CategoryRow
import app.musique.ui.components.musique.IconTile
import app.musique.ui.components.musique.LargeTitleTopBar
import app.musique.ui.components.musique.ListHairline
import app.musique.ui.components.musique.RowMoreButton
import app.musique.ui.components.musique.SectionHeader
import app.musique.ui.components.musique.SquareCard
import app.musique.ui.components.musique.TopBarIconButton
import app.musique.ui.components.musique.rememberCollapseFraction
import app.musique.ui.components.navigation.Route
import app.musique.ui.components.rows.AlbumRow
import app.musique.ui.components.rows.ArtistRow
import app.musique.ui.components.rows.PlaylistRow
import app.musique.ui.components.rows.ShowRow
import app.musique.ui.components.rows.SwipeableEpisodeRowConfigured
import app.musique.ui.components.user.UserChipAvatar
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.theme.MusiqueDimens
import app.musique.ui.theme.MusiqueType
import app.musique.ui.viewmodel.library.LibraryTab
import app.musique.ui.viewmodel.library.LibraryViewModel
import kotlinx.coroutines.launch

/** Sub pages of the Library tab. Songs is not here: it opens [Route.LikedScreen]. */
private enum class LibraryPage { Overview, Playlists, Artists, Albums, Podcasts }

private sealed class LibraryItem {
    abstract val key: String

    data class FolderHeader(val folder: PlaylistFolder, val isExpanded: Boolean) : LibraryItem() {
        override val key get() = "folder:${folder.id}"
    }

    data class PlaylistRow(val playlist: Playlist, val folderId: String?) : LibraryItem() {
        override val key get() = "playlist:${playlist.uri}"
    }
}

/** One tile of the Recently Added grid. */
private sealed class RecentItem {
    abstract val key: String

    data class PlaylistTile(val playlist: Playlist) : RecentItem() {
        override val key get() = "recent_playlist:${playlist.uri}"
    }

    data class AlbumTile(val album: Album) : RecentItem() {
        override val key get() = "recent_album:${album.uri}"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedTransitionScope.LibraryScreen(
    viewModel: LibraryViewModel,
    backStack: NavBackStack<NavKey>,
    modifier: Modifier = Modifier,
) {
    val libraryState by viewModel.libraryState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadPlaylistUris()
        // Albums feed the Recently Added grid and the Artists list.
        viewModel.loadAlbumUris()
    }

    var page by rememberSaveable { mutableStateOf(LibraryPage.Overview) }
    BackHandler(enabled = page != LibraryPage.Overview) { page = LibraryPage.Overview }

    var showCreateFolder by remember { mutableStateOf(false) }
    var editingFolder by remember { mutableStateOf<PlaylistFolder?>(null) }
    var movePlaylistUri by remember { mutableStateOf<String?>(null) }
    var moveCurrentFolderId by remember { mutableStateOf<String?>(null) }
    var deleteFolderId by remember { mutableStateOf<String?>(null) }

    if (libraryState.error != null && libraryState.playlists.isEmpty() &&
        (page == LibraryPage.Overview || page == LibraryPage.Playlists)
    ) {
        ErrorScreen(
            message = libraryState.error!!,
            onRetry = { viewModel.retry() },
            modifier = modifier,
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MusiqueColors.canvas)
    ) {
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                val forward = targetState != LibraryPage.Overview
                (slideInHorizontally { if (forward) it / 4 else -it / 4 } + fadeIn()) togetherWith
                    (slideOutHorizontally { if (forward) -it / 4 else it / 4 } + fadeOut())
            },
            label = "libraryPage",
        ) { current ->
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize(),
            ) {
                when (current) {
                    LibraryPage.Overview -> LibraryOverview(
                        viewModel = viewModel,
                        playlists = libraryState.playlists,
                        albums = libraryState.albums,
                        onOpenPage = { page = it },
                        onOpenLiked = { backStack.add(Route.LikedScreen()) },
                        onOpenPlaylist = { backStack.add(Route.PlaylistScreen(it)) },
                        onOpenAlbum = { backStack.add(Route.AlbumScreen(it)) },
                        onCreatePlaylist = { GlobalPopupController.show(PopupSpec.CreatePlaylist()) },
                    )

                    else -> LibraryCategoryPage(
                        page = current,
                        viewModel = viewModel,
                        backStack = backStack,
                        onBack = { page = LibraryPage.Overview },
                        onCreateFolder = { showCreateFolder = true },
                        onEditFolder = { editingFolder = it },
                        onDeleteFolder = { deleteFolderId = it.id },
                        onMovePlaylist = { uri, folderId ->
                            movePlaylistUri = uri
                            moveCurrentFolderId = folderId
                        },
                    )
                }
            }
        }
    }

    if (showCreateFolder || editingFolder != null) {
        val folder = editingFolder
        CreateFolderBottomSheet(
            initialName = folder?.name ?: "",
            initialColor = folder?.toColor() ?: Color(0xFF1DB954),
            folderId = folder?.id,
            onDismiss = {
                showCreateFolder = false
                editingFolder = null
            },
            onCreated = { newFolder ->
                if (folder != null) viewModel.updateFolder(newFolder)
                else viewModel.createFolder(newFolder)
            }
        )
    }

    movePlaylistUri?.let { uri ->
        MoveToFolderBottomSheet(
            folders = libraryState.folders,
            currentFolderId = moveCurrentFolderId,
            onDismiss = {
                movePlaylistUri = null
                moveCurrentFolderId = null
            },
            onSelectFolder = { folderId -> viewModel.movePlaylistToFolder(uri, folderId) }
        )
    }

    deleteFolderId?.let { folderId ->
        val folder = libraryState.folders.find { it.id == folderId }
        AlertDialog(
            onDismissRequest = { deleteFolderId = null },
            title = { Text(stringResource(R.string.folder_delete)) },
            text = { Text(stringResource(R.string.folder_delete_confirm, folder?.name ?: "")) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFolder(folderId)
                    deleteFolderId = null
                }) {
                    Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteFolderId = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

private fun topContentPadding(statusBar: Dp) = statusBar + MusiqueDimens.topBarHeight + 4.dp

@Composable
private fun LibraryOverview(
    viewModel: LibraryViewModel,
    playlists: List<Playlist>,
    albums: List<Album>,
    onOpenPage: (LibraryPage) -> Unit,
    onOpenLiked: () -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onCreatePlaylist: () -> Unit,
) {
    val listState = rememberLazyListState()
    val collapseFraction = rememberCollapseFraction(listState)
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val accent = MaterialTheme.colorScheme.primary

    val recent = remember(playlists, albums) { buildRecentlyAdded(playlists, albums) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                top = topContentPadding(statusBar),
                bottom = ScreenBottomPadding + 16.dp,
            ),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "categories") {
                val categories = listOf(
                    Triple(Icons.AutoMirrored.Rounded.QueueMusic, R.string.library_cat_playlists) { onOpenPage(LibraryPage.Playlists) },
                    Triple(Icons.Rounded.MicExternalOn, R.string.library_cat_artists) { onOpenPage(LibraryPage.Artists) },
                    Triple(Icons.Rounded.Album, R.string.library_cat_albums) { onOpenPage(LibraryPage.Albums) },
                    Triple(Icons.Rounded.MusicNote, R.string.library_cat_songs, onOpenLiked),
                    Triple(Icons.Rounded.Podcasts, R.string.library_cat_podcasts) { onOpenPage(LibraryPage.Podcasts) },
                )
                Column(Modifier.fillMaxWidth()) {
                    categories.forEachIndexed { index, (icon, label, onClick) ->
                        CategoryRow(
                            icon = icon,
                            label = stringResource(label),
                            onClick = onClick,
                            accentColor = accent,
                        )
                        if (index < categories.lastIndex) ListHairline()
                    }
                }
            }

            if (recent.isNotEmpty()) {
                item(key = "recent_header") {
                    SectionHeader(
                        title = stringResource(R.string.library_recently_added),
                        modifier = Modifier.padding(top = 28.dp, bottom = 14.dp),
                    )
                }
                val rows = recent.chunked(2)
                items(rows, key = { row -> row.joinToString("|") { it.key } }) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MusiqueDimens.screenPadding)
                            .padding(bottom = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        row.forEach { item ->
                            RecentTile(
                                item = item,
                                viewModel = viewModel,
                                onOpenPlaylist = onOpenPlaylist,
                                onOpenAlbum = onOpenAlbum,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        LargeTitleTopBar(
            title = stringResource(R.string.library_title),
            collapseFraction = collapseFraction,
            trailingContent = {
                TopBarIconButton(
                    icon = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.library_new_playlist),
                    onClick = onCreatePlaylist,
                    tint = accent,
                )
            },
        )
    }
}

@Composable
private fun RecentTile(
    item: RecentItem,
    viewModel: LibraryViewModel,
    onOpenPlaylist: (String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (item) {
        is RecentItem.PlaylistTile -> {
            val playlist = item.playlist
            val artworkUrl by produceState<String?>(null, playlist.uri) {
                value = viewModel.getArtworkUrl(playlist)
            }
            SquareCard(
                title = playlist.attributes.name,
                subtitle = stringResource(R.string.detail_playlist_type),
                imageUrl = artworkUrl,
                size = null,
                onClick = { onOpenPlaylist(playlist.uri) },
                onLongClick = { GlobalPopupController.show(PopupSpec.PlaylistInfo(playlist, artworkUrl)) },
                modifier = modifier,
            )
        }

        is RecentItem.AlbumTile -> {
            val album = item.album
            SquareCard(
                title = album.name,
                subtitle = album.artists.joinToString(", ") { it.name }
                    .ifBlank { stringResource(R.string.detail_album_type) },
                imageUrl = album.coverUrl(),
                size = null,
                onClick = { onOpenAlbum(album.uri) },
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun SharedTransitionScope.LibraryCategoryPage(
    page: LibraryPage,
    viewModel: LibraryViewModel,
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit,
    onCreateFolder: () -> Unit,
    onEditFolder: (PlaylistFolder) -> Unit,
    onDeleteFolder: (PlaylistFolder) -> Unit,
    onMovePlaylist: (String, String?) -> Unit,
) {
    val libraryState by viewModel.libraryState.collectAsState()
    val listState = rememberLazyListState()
    val collapseFraction = rememberCollapseFraction(listState)
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val scope = rememberCoroutineScope()
    val accent = MaterialTheme.colorScheme.primary

    var query by rememberSaveable(page) { mutableStateOf("") }
    var expandedFolderIds by remember { mutableStateOf(setOf<String>()) }
    val libraryArtists = remember(libraryState.albums) { artistsFromAlbums(libraryState.albums) }

    // Podcasts shows saved shows or saved episodes; the view model loads each on selection.
    LaunchedEffect(page) {
        if (page == LibraryPage.Podcasts) {
            val tab = libraryState.selectedTab
            viewModel.selectTab(if (tab == LibraryTab.Episodes) LibraryTab.Episodes else LibraryTab.Shows)
        }
    }
    val podcastTab = if (libraryState.selectedTab == LibraryTab.Episodes) LibraryTab.Episodes else LibraryTab.Shows

    val title = stringResource(
        when (page) {
            LibraryPage.Playlists -> R.string.library_cat_playlists
            LibraryPage.Artists -> R.string.library_cat_artists
            LibraryPage.Albums -> R.string.library_cat_albums
            else -> R.string.library_cat_podcasts
        }
    )
    val placeholder = stringResource(
        when (page) {
            LibraryPage.Playlists -> R.string.search_library_playlists
            LibraryPage.Artists -> R.string.library_search_artists
            LibraryPage.Albums -> R.string.search_library_albums
            else -> if (podcastTab == LibraryTab.Episodes) R.string.search_library_episodes
            else R.string.search_library_shows
        }
    )

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                top = topContentPadding(statusBar),
                bottom = ScreenBottomPadding + 16.dp,
            ),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "search") {
                CapsuleSearchField(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = placeholder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MusiqueDimens.screenPadding)
                        .padding(bottom = 12.dp),
                )
            }

            when (page) {
                LibraryPage.Playlists -> playlistItems(
                    folders = libraryState.folders,
                    playlists = libraryState.playlists,
                    query = query,
                    expandedFolderIds = expandedFolderIds,
                    onToggleFolder = { id ->
                        expandedFolderIds = if (id in expandedFolderIds) expandedFolderIds - id else expandedFolderIds + id
                    },
                    accent = accent,
                    viewModel = viewModel,
                    backStack = backStack,
                    onCreateFolder = onCreateFolder,
                    onEditFolder = onEditFolder,
                    onDeleteFolder = onDeleteFolder,
                    onMovePlaylist = onMovePlaylist,
                )

                LibraryPage.Albums -> {
                    val albums = libraryState.albums.filter {
                        query.isBlank() || it.name.contains(query, ignoreCase = true) ||
                            it.artists.any { a -> a.name.contains(query, ignoreCase = true) }
                    }
                    statusItem(
                        loading = libraryState.isLoadingAlbums && albums.isEmpty(),
                        empty = albums.isEmpty(),
                        loadingText = R.string.search_loading_albums,
                        emptyText = if (query.isBlank()) R.string.library_albums_empty else R.string.library_albums_no_match,
                    )
                    items(albums, key = { it.uri }, contentType = { "album" }) { album ->
                        AlbumRow(
                            album = album,
                            artworkUrl = album.coverUrl(),
                            onRowClick = { backStack.add(Route.AlbumScreen(album.uri)) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }

                LibraryPage.Artists -> {
                    val artists = libraryArtists.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
                    statusItem(
                        loading = libraryState.isLoadingAlbums && artists.isEmpty(),
                        empty = artists.isEmpty(),
                        loadingText = R.string.search_loading_albums,
                        emptyText = if (query.isBlank()) R.string.library_artists_empty else R.string.library_artists_no_match,
                    )
                    items(artists, key = { it.uri }, contentType = { "artist" }) { artist ->
                        ArtistRow(
                            artist = artist,
                            artworkUrl = artist.portraits.firstOrNull()
                                ?: artist.covers.firstOrNull()?.let { ALBUM_COVER_URL + it.uri },
                            onRowClick = { backStack.add(Route.ArtistScreen(artist.uri)) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }

                else -> {
                    item(key = "podcast_segments") {
                        PodcastSegments(
                            selected = podcastTab,
                            onSelect = { viewModel.selectTab(it) },
                            accent = accent,
                        )
                    }
                    if (podcastTab == LibraryTab.Shows) {
                        val shows = libraryState.shows.filter {
                            query.isBlank() || it.name.contains(query, ignoreCase = true) ||
                                it.publisher.contains(query, ignoreCase = true)
                        }
                        statusItem(
                            loading = libraryState.isLoadingShows && shows.isEmpty(),
                            empty = shows.isEmpty(),
                            loadingText = R.string.search_loading_shows,
                            emptyText = if (query.isBlank()) R.string.library_shows_empty else R.string.library_shows_no_match,
                        )
                        items(shows, key = { it.uri }, contentType = { "show" }) { show ->
                            ShowRow(
                                show = show,
                                onRowClick = { backStack.add(Route.ShowScreen(show.uri)) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    } else {
                        val episodes = libraryState.episodes.filter {
                            query.isBlank() || it.name.contains(query, ignoreCase = true) ||
                                it.showName.contains(query, ignoreCase = true)
                        }
                        statusItem(
                            loading = libraryState.isLoadingEpisodes && episodes.isEmpty(),
                            empty = episodes.isEmpty(),
                            loadingText = R.string.search_loading_episodes,
                            emptyText = if (query.isBlank()) R.string.library_episodes_empty else R.string.library_episodes_no_match,
                        )
                        items(episodes, key = { it.uri }, contentType = { "episode" }) { episode ->
                            val showUri = episode.showUri.ifBlank {
                                libraryState.episodeShowUris[episode.uri]
                            } ?: ""
                            val openShow: () -> Unit = {
                                if (showUri.isNotBlank()) {
                                    backStack.add(Route.ShowScreen(showUri))
                                } else {
                                    scope.launch {
                                        val details = viewModel.resolveEpisodeDetails(episode.id)
                                        if (details != null && details.showUri.isNotBlank()) {
                                            backStack.add(Route.ShowScreen(details.showUri))
                                        }
                                    }
                                }
                            }
                            SwipeableEpisodeRowConfigured(
                                episode = episode,
                                onRowClick = { viewModel.playEpisode(episode) },
                                onArtworkClick = openShow,
                                onShowNameClick = openShow,
                                isLiked = true,
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
        }

        LargeTitleTopBar(
            title = title,
            collapseFraction = collapseFraction,
            onBack = onBack,
            trailingContent = {
                if (page == LibraryPage.Playlists) {
                    TopBarIconButton(
                        icon = Icons.Rounded.CreateNewFolder,
                        contentDescription = stringResource(R.string.library_new_folder),
                        onClick = onCreateFolder,
                        tint = accent,
                    )
                    TopBarIconButton(
                        icon = Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.library_new_playlist),
                        onClick = { GlobalPopupController.show(PopupSpec.CreatePlaylist()) },
                        tint = accent,
                    )
                }
            },
        )
    }
}

private fun LazyListScope.statusItem(
    loading: Boolean,
    empty: Boolean,
    loadingText: Int,
    emptyText: Int,
) {
    if (!loading && !empty) return
    item(key = "status") {
        Text(
            text = stringResource(if (loading) loadingText else emptyText),
            style = MusiqueType.rowSubtitle,
            color = MusiqueColors.textSecondary,
            modifier = Modifier.padding(horizontal = MusiqueDimens.screenPadding, vertical = 24.dp),
        )
    }
}

private fun LazyListScope.playlistItems(
    folders: List<PlaylistFolder>,
    playlists: List<Playlist>,
    query: String,
    expandedFolderIds: Set<String>,
    onToggleFolder: (String) -> Unit,
    accent: Color,
    viewModel: LibraryViewModel,
    backStack: NavBackStack<NavKey>,
    onCreateFolder: () -> Unit,
    onEditFolder: (PlaylistFolder) -> Unit,
    onDeleteFolder: (PlaylistFolder) -> Unit,
    onMovePlaylist: (String, String?) -> Unit,
) {
    val filtered = if (query.isBlank()) playlists else playlists.filter { p ->
        p.attributes.name.contains(query, ignoreCase = true) ||
            p.ownerUsername.contains(query, ignoreCase = true)
    }
    val flatItems = buildFlatList(folders, filtered, expandedFolderIds)

    if (query.isBlank()) {
        item(key = "new_playlist") {
            AppleListRow(
                title = stringResource(R.string.library_new_playlist),
                titleColor = accent,
                onClick = { GlobalPopupController.show(PopupSpec.CreatePlaylist()) },
                leading = { IconTile(Icons.AutoMirrored.Rounded.PlaylistAdd, iconTint = accent) },
            )
        }
        item(key = "new_folder") {
            AppleListRow(
                title = stringResource(R.string.library_new_folder),
                titleColor = accent,
                onClick = onCreateFolder,
                leading = { IconTile(Icons.Rounded.CreateNewFolder, iconTint = accent) },
            )
        }
    }

    if (flatItems.isEmpty()) {
        item(key = "playlists_empty") {
            Text(
                text = stringResource(
                    if (query.isBlank()) R.string.library_playlists_empty else R.string.library_playlists_no_match
                ),
                style = MusiqueType.rowSubtitle,
                color = MusiqueColors.textSecondary,
                modifier = Modifier.padding(horizontal = MusiqueDimens.screenPadding, vertical = 24.dp),
            )
        }
    }

    items(
        items = flatItems,
        key = { it.key },
        contentType = {
            when (it) {
                is LibraryItem.FolderHeader -> "folder_header"
                is LibraryItem.PlaylistRow -> "playlist"
            }
        }
    ) { item ->
        when (item) {
            is LibraryItem.FolderHeader -> FolderRow(
                folder = item.folder,
                isExpanded = item.isExpanded,
                onToggleExpand = { onToggleFolder(item.folder.id) },
                onEdit = { onEditFolder(item.folder) },
                onDelete = { onDeleteFolder(item.folder) },
            )

            is LibraryItem.PlaylistRow -> PlaylistRowContent(
                playlist = item.playlist,
                folderId = item.folderId,
                backStack = backStack,
                viewModel = viewModel,
                onMovePlaylist = onMovePlaylist,
            )
        }
    }
}

@Composable
private fun PodcastSegments(
    selected: LibraryTab,
    onSelect: (LibraryTab) -> Unit,
    accent: Color,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = MusiqueDimens.screenPadding)
            .padding(bottom = 8.dp),
    ) {
        listOf(
            LibraryTab.Shows to R.string.search_section_shows,
            LibraryTab.Episodes to R.string.library_cat_episodes,
        ).forEach { (tab, label) ->
            val isSelected = tab == selected
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSelected) accent else MusiqueColors.cardRaised)
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = stringResource(label),
                    style = MusiqueType.button,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MusiqueColors.textPrimary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun FolderRow(
    folder: PlaylistFolder,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    AppleListRow(
        title = folder.name,
        subtitle = stringResource(R.string.count_playlists, folder.playlistIds.size),
        onClick = onToggleExpand,
        leading = {
            IconTile(
                icon = Icons.Rounded.Folder,
                containerColor = folder.toColor(),
                iconTint = Color.White,
            )
        },
        trailing = {
            Icon(
                imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = stringResource(if (isExpanded) R.string.library_collapse else R.string.library_expand),
                tint = MusiqueColors.textTertiary,
                modifier = Modifier.size(22.dp),
            )
            Box {
                RowMoreButton(onClick = { menuOpen = true })
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.library_edit_folder)) },
                        onClick = {
                            menuOpen = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(stringResource(R.string.folder_delete), color = MaterialTheme.colorScheme.error)
                        },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        },
    )
}

@Composable
private fun PlaylistRowContent(
    playlist: Playlist,
    folderId: String?,
    backStack: NavBackStack<NavKey>,
    viewModel: LibraryViewModel,
    onMovePlaylist: (String, String?) -> Unit,
) {
    val artworkUrl by produceState<String?>(null, playlist.uri) {
        value = viewModel.getArtworkUrl(playlist)
    }
    val authors by produceState<List<Profile>>(emptyList(), playlist.uri) {
        value = viewModel.getAuthors(playlist).take(2)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (folderId != null) 20.dp else 0.dp),
    ) {
        Box(modifier = Modifier.weight(1f)) {
            PlaylistRow(
                playlist = playlist,
                artworkUrl = artworkUrl,
                onRowClick = { backStack.add(Route.PlaylistScreen(playlist.uri)) },
                onRowLongClick = {
                    GlobalPopupController.show(PopupSpec.PlaylistInfo(playlist, artworkUrl))
                },
                trailingContent = {
                    Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
                        authors.forEach { author ->
                            UserChipAvatar(
                                artworkUrl = author.imageUrl,
                                modifier = Modifier
                                    .size(22.dp)
                                    .clickable { backStack.add(Route.ProfileScreen(author.uri)) },
                            )
                        }
                    }
                },
            )
        }
        var menuOpen by remember { mutableStateOf(false) }
        Box(Modifier.padding(end = 4.dp)) {
            RowMoreButton(onClick = { menuOpen = true })
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.library_move_to_folder)) },
                    leadingIcon = { Icon(Icons.Rounded.Folder, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onMovePlaylist(playlist.uri, folderId)
                    },
                )
            }
        }
    }
}

private fun Album.coverUrl(): String? =
    (getCover(CoverSize.LARGE) ?: getCover(CoverSize.MEDIUM) ?: covers.firstOrNull())
        ?.let { ALBUM_COVER_URL + it.uri }

/** Distinct album artists in library order, so the Artists page has something to list. */
private fun artistsFromAlbums(albums: List<Album>): List<Artist> =
    albums.flatMap { it.artists }
        .filter { it.uri.isNotBlank() }
        .distinctBy { it.uri }
        .sortedBy { it.name.lowercase() }

/** Newest playlists (by revision timestamp) interleaved with saved albums, newest first. */
private fun buildRecentlyAdded(playlists: List<Playlist>, albums: List<Album>): List<RecentItem> {
    val recentPlaylists = playlists.sortedByDescending { it.timestamp }.take(8).map { RecentItem.PlaylistTile(it) }
    val recentAlbums = albums.take(8).map { RecentItem.AlbumTile(it) }
    val result = mutableListOf<RecentItem>()
    for (i in 0 until maxOf(recentPlaylists.size, recentAlbums.size)) {
        recentPlaylists.getOrNull(i)?.let(result::add)
        recentAlbums.getOrNull(i)?.let(result::add)
    }
    return result.take(12)
}

private fun buildFlatList(
    folders: List<PlaylistFolder>,
    playlists: List<Playlist>,
    expandedFolderIds: Set<String>,
): List<LibraryItem> {
    val items = mutableListOf<LibraryItem>()
    val organizedUris = folders.flatMap { it.playlistIds }.toSet()
    val byUri = playlists.associateBy { it.uri }

    for (folder in folders) {
        val exp = folder.id in expandedFolderIds
        items.add(LibraryItem.FolderHeader(folder, exp))
        if (exp) {
            for (uri in folder.playlistIds) {
                byUri[uri]?.let { items.add(LibraryItem.PlaylistRow(it, folder.id)) }
            }
        }
    }

    for (p in playlists) {
        if (p.uri !in organizedUris) items.add(LibraryItem.PlaylistRow(p, null))
    }
    return items
}
