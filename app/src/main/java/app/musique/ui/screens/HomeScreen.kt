package app.musique.ui.screens

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import app.musique.ALBUM_COVER_URL
import app.musique.R
import app.musique.ScreenBottomPadding
import app.musique.core.home.HomeItem
import app.musique.core.model.CoverSize
import app.musique.core.model.Track
import app.musique.core.model.getCover
import app.musique.data.setting.LocalUiSettings
import app.musique.ui.GlobalPopupController
import app.musique.ui.PopupSpec
import app.musique.ui.components.HomeJamCard
import app.musique.ui.components.SkeletonBox
import app.musique.ui.components.musique.GroupedTrackList
import app.musique.ui.components.musique.HeroCard
import app.musique.ui.components.musique.LargeTitleTopBar
import app.musique.ui.components.musique.PortraitCard
import app.musique.ui.components.musique.SectionHeader
import app.musique.ui.components.musique.SquareCard
import app.musique.ui.components.musique.TopBarAccountActions
import app.musique.ui.components.musique.rememberCollapseFraction
import app.musique.ui.components.navigation.Route
import app.musique.ui.components.rows.SwipeableTrackRowConfigured
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.theme.MusiqueDimens
import app.musique.ui.theme.MusiqueShapes
import app.musique.ui.theme.MusiqueType
import app.musique.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedTransitionScope.HomeScreen(
    backStack: NavBackStack<NavKey>,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    onShowJam: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val avatar by viewModel.userImageUrl.collectAsState()
    val loggedIn by viewModel.isPlaybackLoggedIn.collectAsState()
    val current by viewModel.currentAudio.collectAsState()
    val playing by viewModel.isPlaying.collectAsState()
    val jam by viewModel.jam.collectAsState()
    val metadata by viewModel.trackMetadata.collectAsState()
    val likedIds by viewModel.likedIds.collectAsState()
    val tracks = metadata.takeIf { it.lease == state.lease }?.tracks.orEmpty()

    val listState = rememberLazyListState()
    val collapseFraction = rememberCollapseFraction(listState)
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val screenshotMode = LocalUiSettings.current.screenshotMode

    val open: (HomeItem) -> Unit = { item ->
        if (viewModel.isCurrent(state.lease)) {
            val route = when (item.kind) {
                "album" -> Route.AlbumScreen(item.uri)
                "playlist" -> Route.PlaylistScreen(item.uri)
                "artist" -> Route.ArtistScreen(item.uri)
                else -> Route.TrackScreen(item.uri)
            }
            backStack.add(route)
        }
    }
    val play: (HomeItem) -> Unit = { viewModel.play(it, state.lease) }

    val trackRow: @Composable (HomeItem) -> Unit = { item ->
        val track = tracks[item.uri] ?: item.track
        SwipeableTrackRowConfigured(
            track = track,
            currentAudio = current,
            isPlaybackPlaying = playing,
            isLiked = track?.id in likedIds,
            onRowClick = { play(item) },
            onArtworkClick = {
                if (viewModel.isCurrent(state.lease)) {
                    track?.album?.uri?.let { backStack.add(Route.AlbumScreen(it)) } ?: open(item)
                }
            },
            onArtistClick = { artist ->
                if (viewModel.isCurrent(state.lease)) backStack.add(Route.ArtistScreen(artist.uri))
            },
        )
    }

    val usePickedAsTopPortrait = state.pickedAlbums.isNotEmpty()
    val topSectionFromSections = if (!usePickedAsTopPortrait) state.sections.firstOrNull() else null
    val remainingSections = if (usePickedAsTopPortrait) state.sections else state.sections.drop(1)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MusiqueColors.canvas)
    ) {
        PullToRefreshBox(
            isRefreshing = state.refreshing && state.hasContent,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    top = statusBarTop + MusiqueDimens.topBarHeight + 8.dp,
                    bottom = ScreenBottomPadding + 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(MusiqueDimens.sectionSpacing),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (jam != null) {
                    item(key = "jam_active") {
                        HomeJamCard(
                            jam = jam,
                            onClick = onShowJam,
                            modifier = Modifier.padding(horizontal = MusiqueDimens.screenPadding),
                        )
                    }
                }

                when {
                    state.lease == null -> item(key = "login") {
                        HomeMessage(
                            title = stringResource(if (loggedIn) R.string.home_connecting else R.string.connect_to_spotify),
                            message = stringResource(
                                if (loggedIn) R.string.home_connecting_description
                                else R.string.home_playback_login_description
                            ),
                            action = stringResource(if (loggedIn) R.string.common_retry else R.string.connect_to_spotify),
                            onClick = { if (loggedIn) viewModel.retry() else backStack.add(Route.SettingsScreen) },
                        )
                    }

                    else -> {
                        if (state.error != null || state.partial) {
                            item(key = "warning") {
                                Surface(
                                    color = MusiqueColors.cardRaised,
                                    shape = MusiqueShapes.cardLarge,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = MusiqueDimens.screenPadding)
                                        .border(
                                            MusiqueDimens.hairlineWidth,
                                            MusiqueColors.hairline,
                                            MusiqueShapes.cardLarge,
                                        ),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = stringResource(
                                                if (state.hasContent) R.string.home_partial_feed
                                                else R.string.home_feed_error
                                            ),
                                            style = MusiqueType.rowSubtitle,
                                            color = MusiqueColors.textSecondary,
                                            modifier = Modifier.weight(1f),
                                        )
                                        TextButton(onClick = viewModel::refresh) {
                                            Text(
                                                text = stringResource(R.string.common_retry),
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Top Portrait Carousel: Top Picks for You
                        if (usePickedAsTopPortrait) {
                            item(key = "top_picks_header") {
                                SectionHeader(title = stringResource(R.string.home_top_picks))
                            }
                            item(key = "top_picks_carousel") {
                                PortraitCarousel(
                                    items = state.pickedAlbums,
                                    tracks = tracks,
                                    likedIds = likedIds,
                                    onOpen = open,
                                    onPlay = play,
                                )
                            }
                        } else if (topSectionFromSections != null) {
                            item(key = "top_section_header_${topSectionFromSections.id}") {
                                SectionHeader(title = topSectionFromSections.title)
                            }
                            item(key = "top_section_carousel_${topSectionFromSections.id}") {
                                PortraitCarousel(
                                    items = topSectionFromSections.items,
                                    tracks = tracks,
                                    likedIds = likedIds,
                                    onOpen = open,
                                    onPlay = play,
                                )
                            }
                        } else if (state.refreshing) {
                            item(key = "top_picks_loading_header") {
                                SectionHeader(title = stringResource(R.string.home_top_picks))
                            }
                            item(key = "top_picks_loading") {
                                PortraitCarouselSkeleton()
                            }
                        }

                        // Recently Played / Quick Picks Hero Carousel
                        if (state.quickPicks.isNotEmpty()) {
                            item(key = "quick_picks_header") {
                                SectionHeader(title = stringResource(R.string.home_recently_played))
                            }
                            item(key = "quick_picks_carousel") {
                                HeroCarousel(
                                    items = state.quickPicks,
                                    tracks = tracks,
                                    likedIds = likedIds,
                                    onOpen = open,
                                    onPlay = play,
                                )
                            }
                        } else if (state.refreshing) {
                            item(key = "quick_picks_loading_header") {
                                SectionHeader(title = stringResource(R.string.home_recently_played))
                            }
                            item(key = "quick_picks_loading") {
                                HeroCarouselSkeleton()
                            }
                        } else if (state.hasContent) {
                            item(key = "quick_empty") {
                                Text(
                                    text = stringResource(R.string.home_quick_picks_empty),
                                    style = MusiqueType.cardSubtitle,
                                    color = MusiqueColors.textSecondary,
                                    modifier = Modifier.padding(horizontal = MusiqueDimens.screenPadding),
                                )
                            }
                        }

                        if (jam == null) {
                            item(key = "jam_idle") {
                                HomeJamCard(
                                    jam = null,
                                    onClick = onShowJam,
                                    modifier = Modifier.padding(horizontal = MusiqueDimens.screenPadding),
                                )
                            }
                        }

                        // Remaining sections as SquareCard carousels (or GroupedTrackList for track-only items)
                        remainingSections.forEach { section ->
                            val (eyebrow, title) = splitSectionTitle(section.title)
                            val seedImage = if (eyebrow != null) {
                                section.items.firstOrNull()?.let { resolveHomeArtwork(it, tracks) }
                            } else null

                            item(key = "section_header_${section.id}") {
                                SectionHeader(
                                    title = title,
                                    eyebrow = eyebrow,
                                    seedArtworkUrl = seedImage,
                                )
                            }
                            item(key = "section_body_${section.id}") {
                                HomeSquareSection(
                                    items = section.items,
                                    tracks = tracks,
                                    likedIds = likedIds,
                                    onOpen = open,
                                    onPlay = play,
                                    trackRow = trackRow,
                                )
                            }
                        }

                        if (!state.hasContent && !state.refreshing && state.error == null) {
                            item(key = "empty") {
                                HomeMessage(
                                    title = stringResource(R.string.home_feed_empty),
                                    message = stringResource(R.string.home_feed_empty_description),
                                    action = stringResource(R.string.common_retry),
                                    onClick = viewModel::refresh,
                                )
                            }
                        }

                        item(key = "source") {
                            Text(
                                text = stringResource(R.string.home_source_description),
                                style = MusiqueType.caption,
                                color = MusiqueColors.textTertiary,
                                modifier = Modifier.padding(horizontal = MusiqueDimens.screenPadding),
                            )
                        }
                    }
                }
            }
        }

        LargeTitleTopBar(
            title = stringResource(R.string.home_title),
            collapseFraction = collapseFraction,
            trailingContent = {
                TopBarAccountActions(
                    avatarUrl = avatar,
                    screenshotMode = screenshotMode,
                    onDevicesClick = { GlobalPopupController.show(PopupSpec.PlaybackDevices()) },
                    onProfileClick = { backStack.add(Route.SettingsScreen) },
                )
            },
        )
    }
}

@Composable
private fun PortraitCarousel(
    items: List<HomeItem>,
    tracks: Map<String, Track>,
    likedIds: Set<String>,
    onOpen: (HomeItem) -> Unit,
    onPlay: (HomeItem) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = MusiqueDimens.screenPadding),
        horizontalArrangement = Arrangement.spacedBy(MusiqueDimens.cardSpacing),
    ) {
        items(items, key = { "portrait_${it.uri}" }) { item ->
            val resolvedTrack = tracks[item.uri] ?: item.track
            val imageUrl = resolveHomeArtwork(item, tracks)
            val eyebrow = if (item.kind == "playlist") {
                stringResource(R.string.home_eyebrow_made_for_you)
            } else {
                homeKind(item.kind)
            }
            PortraitCard(
                title = item.title,
                eyebrow = eyebrow,
                subtitle = item.subtitle.ifBlank { null },
                imageUrl = imageUrl,
                onClick = { if (item.kind == "track") onPlay(item) else onOpen(item) },
                onPlayClick = { onPlay(item) },
                onLongClick = resolvedTrack?.let { t ->
                    {
                        GlobalPopupController.show(
                            PopupSpec.TrackInfo(track = t, isLiked = t.id in likedIds)
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun HeroCarousel(
    items: List<HomeItem>,
    tracks: Map<String, Track>,
    likedIds: Set<String>,
    onOpen: (HomeItem) -> Unit,
    onPlay: (HomeItem) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = MusiqueDimens.screenPadding),
        horizontalArrangement = Arrangement.spacedBy(MusiqueDimens.cardSpacing),
    ) {
        items(items, key = { "hero_${it.uri}" }) { item ->
            val resolvedTrack = tracks[item.uri] ?: item.track
            val imageUrl = resolveHomeArtwork(item, tracks)
            val subtitle = item.subtitle.ifBlank {
                resolvedTrack?.artists?.joinToString(", ") { it.name } ?: homeKind(item.kind)
            }
            HeroCard(
                title = item.title,
                subtitle = subtitle,
                imageUrl = imageUrl,
                onClick = { if (item.kind == "track") onPlay(item) else onOpen(item) },
                onPlayClick = { onPlay(item) },
                onLongClick = resolvedTrack?.let { t ->
                    {
                        GlobalPopupController.show(
                            PopupSpec.TrackInfo(track = t, isLiked = t.id in likedIds)
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun HomeSquareSection(
    items: List<HomeItem>,
    tracks: Map<String, Track>,
    likedIds: Set<String>,
    onOpen: (HomeItem) -> Unit,
    onPlay: (HomeItem) -> Unit,
    trackRow: @Composable (HomeItem) -> Unit,
) {
    val cards = items.filter { it.kind != "track" }
    val trackItems = items.filter { it.kind == "track" }
    // Until track metadata arrives, show tracks as cards from the feed's own fields.
    val tracksAsCards = cards.isEmpty() && trackItems.all { (tracks[it.uri] ?: it.track) == null }
    val carouselItems = if (tracksAsCards) trackItems else cards

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (carouselItems.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = MusiqueDimens.screenPadding),
                horizontalArrangement = Arrangement.spacedBy(MusiqueDimens.cardSpacing),
            ) {
                items(carouselItems, key = { "card_${it.uri}" }) { item ->
                    SquareCard(
                        title = item.title,
                        subtitle = item.subtitle.ifBlank { homeKind(item.kind) },
                        imageUrl = resolveHomeArtwork(item, tracks),
                        isCircular = item.kind == "artist",
                        onClick = { if (item.kind == "track") onPlay(item) else onOpen(item) },
                        onPlayClick = { onPlay(item) },
                    )
                }
            }
        }

        // Swipeable rows stay vertical: a horizontal carousel would steal their swipe gestures.
        if (trackItems.isNotEmpty() && !tracksAsCards) {
            GroupedTrackList(
                containerColor = MusiqueColors.card,
                modifier = Modifier.padding(horizontal = MusiqueDimens.screenPadding),
            ) {
                trackItems.forEach { item ->
                    key(item.uri) {
                        trackRow(item)
                    }
                }
            }
        }
    }
}

private fun resolveHomeArtwork(item: HomeItem, tracks: Map<String, Track>): String? {
    item.imageUrl?.takeIf { it.isNotBlank() }?.let { return it }
    val track = tracks[item.uri] ?: item.track ?: return null
    val coverUri = track.album?.getCover(CoverSize.LARGE)?.uri
        ?: track.album?.getCover(CoverSize.MEDIUM)?.uri
        ?: track.album?.getCover(CoverSize.SMALL)?.uri
        ?: return null
    return ALBUM_COVER_URL + coverUri
}

private fun splitSectionTitle(rawTitle: String): Pair<String?, String> {
    val prefixes = listOf("More like ", "Similar to ", "Because you listened to ")
    for (prefix in prefixes) {
        if (rawTitle.startsWith(prefix, ignoreCase = true) && rawTitle.length > prefix.length) {
            return rawTitle.take(prefix.length).trim() to rawTitle.drop(prefix.length).trim()
        }
    }
    return null to rawTitle
}

@Composable
private fun homeKind(kind: String): String = stringResource(
    when (kind) {
        "album" -> R.string.home_kind_album
        "playlist" -> R.string.home_kind_playlist
        "artist" -> R.string.home_kind_artist
        else -> R.string.home_kind_track
    }
)

@Composable
private fun PortraitCarouselSkeleton() {
    LazyRow(
        contentPadding = PaddingValues(horizontal = MusiqueDimens.screenPadding),
        horizontalArrangement = Arrangement.spacedBy(MusiqueDimens.cardSpacing),
    ) {
        items(3) {
            SkeletonBox(
                modifier = Modifier
                    .width(MusiqueDimens.portraitCardWidth)
                    .height(MusiqueDimens.portraitCardHeight),
                shape = MusiqueShapes.cardLarge,
            )
        }
    }
}

@Composable
private fun HeroCarouselSkeleton() {
    LazyRow(
        contentPadding = PaddingValues(horizontal = MusiqueDimens.screenPadding),
        horizontalArrangement = Arrangement.spacedBy(MusiqueDimens.cardSpacing),
    ) {
        items(3) {
            SkeletonBox(
                modifier = Modifier
                    .width(MusiqueDimens.heroCardWidth)
                    .height(MusiqueDimens.heroCardHeight),
                shape = MusiqueShapes.card,
            )
        }
    }
}

@Composable
private fun HomeMessage(
    title: String,
    message: String,
    action: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = MusiqueShapes.cardLarge,
        color = MusiqueColors.card,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MusiqueDimens.screenPadding)
            .border(MusiqueDimens.hairlineWidth, MusiqueColors.hairline, MusiqueShapes.cardLarge),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MusiqueColors.fillOnArtwork, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = title,
                style = MusiqueType.sectionTitle,
                color = MusiqueColors.textPrimary,
            )
            Text(
                text = message,
                style = MusiqueType.rowSubtitle,
                color = MusiqueColors.textSecondary,
            )
            Button(
                onClick = onClick,
                shape = MusiqueShapes.actionButton,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = MusiqueColors.onDarkPlay,
                ),
            ) {
                Text(
                    text = action,
                    style = MusiqueType.button,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
