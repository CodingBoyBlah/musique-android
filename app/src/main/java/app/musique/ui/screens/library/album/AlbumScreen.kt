package app.musique.ui.screens.library.album

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import app.musique.ALBUM_COVER_URL
import app.musique.R
import app.musique.ScreenBottomPadding
import app.musique.core.model.CoverSize
import app.musique.core.model.getCover
import app.musique.core.model.sharedTransitionKey
import app.musique.core.model.toSpotifyUri
import app.musique.data.setting.LocalSwipeGestureSettings
import app.musique.data.setting.LocalSwipeActionHandler
import app.musique.data.setting.LocalUiSettings
import app.musique.data.setting.buildLongPressAction
import app.musique.fabBottomPadding
import app.musique.ui.GlobalPopupController
import app.musique.ui.PopupSpec
import app.musique.ui.components.AlbumDetailSkeleton
import app.musique.ui.components.ErrorScreen
import app.musique.ui.components.musique.CiderDetailTrackRow
import app.musique.ui.components.musique.DetailFloatingTopBar
import app.musique.ui.components.musique.DetailHeroHeader
import app.musique.ui.components.musique.DetailPaletteGradientBackdrop
import app.musique.ui.components.musique.FloatingRoundButton
import app.musique.ui.components.musique.GroupedTrackItemContainer
import app.musique.ui.components.musique.PlayShuffleRow
import app.musique.ui.components.musique.formatReleaseDateLong
import app.musique.ui.components.musique.formatSongCountLabel
import app.musique.ui.components.musique.formatTotalRuntimeLabel
import app.musique.ui.components.musique.rememberCollapseFraction
import app.musique.ui.components.rows.SwipeableRowWithGestures
import app.musique.ui.components.rows.rememberTrackGestures
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.theme.MusiqueDimens
import app.musique.ui.theme.MusiqueType
import app.musique.ui.theme.rememberArtworkPalette
import app.musique.ui.viewmodel.detail.AlbumDetailViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SharedTransitionScope.AlbumDetailScreen(
    viewModel: AlbumDetailViewModel,
    onBack: () -> Unit,
    artistClick: (uri: String) -> Unit,
    artworkClick: (uri: String) -> Unit,
    highlightTrackUri: String? = null,
) {
    val uiState by viewModel.uiState.collectAsState()

    when {
        uiState.isLoading -> {
            Box(modifier = Modifier.fillMaxSize().background(MusiqueColors.canvas)) {
                AlbumDetailSkeleton()
                DetailFloatingTopBar(
                    title = "",
                    collapseFraction = 0f,
                    onBack = onBack,
                )
            }
        }

        uiState.error != null -> {
            Box(modifier = Modifier.fillMaxSize().background(MusiqueColors.canvas)) {
                ErrorScreen(
                    message = uiState.error!!,
                    onRetry = { viewModel.retry() },
                )
                DetailFloatingTopBar(
                    title = "",
                    collapseFraction = 0f,
                    onBack = onBack,
                )
            }
        }

        uiState.album != null -> {
            val album = uiState.album!!
            val tracks = uiState.tracks
            val largeCoverUri = album.getCover(CoverSize.LARGE)?.uri
                ?: album.getCover(CoverSize.MEDIUM)?.uri
                ?: album.getCover(CoverSize.SMALL)?.uri
            val smallCoverUri = album.getCover(CoverSize.SMALL)?.uri ?: largeCoverUri
            val artworkUrl = largeCoverUri?.let { ALBUM_COVER_URL + it }
            val paletteArtworkUrl = smallCoverUri?.let { ALBUM_COVER_URL + it }
            val palette = rememberArtworkPalette(paletteArtworkUrl)

            val currentTrack by viewModel.currentAudio.collectAsState(initial = null)
            val isPlaybackPlaying by viewModel.isPlaying.collectAsState(initial = false)
            val spirc = viewModel.spirc
            val likedTracksId by viewModel.likedTrackIds.collectAsState()
            val isSaved by viewModel.isSaved.collectAsState()

            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val lazyList = rememberLazyListState()
            val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val collapseFraction = rememberCollapseFraction(lazyList, thresholdDp = 280.dp)

            var highlightedUri by remember { mutableStateOf(highlightTrackUri) }
            LaunchedEffect(highlightedUri) {
                if (highlightedUri != null) {
                    delay(2500)
                    highlightedUri = null
                }
            }
            LaunchedEffect(highlightTrackUri, tracks) {
                if (highlightTrackUri != null && tracks.isNotEmpty()) {
                    val index = tracks.indexOfFirst { it.uri == highlightTrackUri }
                    if (index >= 0) {
                        lazyList.scrollToItem(index + 2)
                    }
                }
            }

            val showScrollToTop by remember {
                derivedStateOf {
                    lazyList.firstVisibleItemIndex > 2 ||
                        lazyList.firstVisibleItemScrollOffset > 300
                }
            }

            val albumArtistsLine = remember(album.artists) {
                album.artists.joinToString(", ") { it.name }
            }
            val artistAvatarUrls = remember(album.artists) {
                album.artists.mapNotNull { artist ->
                    artist.getCover(CoverSize.SMALL)?.uri?.let { ALBUM_COVER_URL + it }
                }
            }
            val yearText = album.date.takeIf { it.isSet() }?.year?.toString()
            val albumTypeLabel = stringResource(R.string.detail_album_type)
            val metadataLine = if (yearText != null) "$albumTypeLabel · $yearText" else albumTypeLabel
            val totalRuntimeMs = remember(tracks) { tracks.sumOf { it.duration } }
            val secondaryActionColor = palette.surface.copy(alpha = 0.94f)
            val albumArtistIds = remember(album.artists) { album.artists.map { it.id }.toSet() }
            val swipeSettings = LocalSwipeGestureSettings.current
            val swipeHandler = LocalSwipeActionHandler.current

            Box(modifier = Modifier.fillMaxSize()) {
                DetailPaletteGradientBackdrop(palette = palette)

                LazyColumn(
                    state = lazyList,
                    contentPadding = PaddingValues(
                        top = statusBarTop + MusiqueDimens.collapsedBarHeight + 12.dp,
                        bottom = ScreenBottomPadding + 24.dp,
                    ),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item(key = "album_hero") {
                        DetailHeroHeader(
                            title = album.name,
                            artworkUrl = artworkUrl,
                            creatorName = albumArtistsLine,
                            creatorAvatarUrls = artistAvatarUrls,
                            metadataLine = metadataLine,
                            onCreatorClick = album.artists.firstOrNull()?.let { primaryArtist ->
                                { artistClick(primaryArtist.uri) }
                            },
                            monochrome = LocalUiSettings.current.monochromeAlbums,
                            artworkModifier = Modifier.sharedBounds(
                                rememberSharedContentState(album.sharedTransitionKey()),
                                LocalNavAnimatedContentScope.current,
                            ),
                        )
                    }

                    item(key = "album_actions") {
                        PlayShuffleRow(
                            onPlayClick = {
                                val first = tracks.firstOrNull()
                                if (first != null) {
                                    spirc.load(album.toSpotifyUri(), first.toSpotifyUri())
                                    viewModel.setTrack(first)
                                } else {
                                    spirc.load(album.toSpotifyUri())
                                }
                            },
                            onShuffleClick = { spirc.shuffleLoad(album.uri) },
                            onSaveClick = { viewModel.toggleSave() },
                            isSaved = isSaved,
                            primaryColor = Color.White,
                            onPrimaryColor = MusiqueColors.onDarkPlay,
                            secondaryColor = secondaryActionColor,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                        )
                    }

                    item(key = "album_tracks_spacer") {
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    itemsIndexed(
                        items = tracks,
                        key = { _, track -> "album_song_${track.uri}" },
                        contentType = { _, _ -> "album_track" },
                    ) { index, track ->
                        val isHighlighted = track.uri == highlightedUri
                        val isCurrent = currentTrack?.uri == track.uri
                        val isLiked = track.id in likedTracksId
                        val rowContainer = if (isHighlighted) {
                            palette.accent.copy(alpha = 0.22f)
                        } else {
                            palette.surface
                        }

                        val trackArtists = remember(track.artists, albumArtistIds) {
                            val trackIds = track.artists.map { it.id }.toSet()
                            if (trackIds.isNotEmpty() && trackIds != albumArtistIds) {
                                track.artists.joinToString(", ") { it.name }
                            } else null
                        }

                        val (startGestures, endGestures) = rememberTrackGestures(track, isLiked)
                        val longPressAction = remember(swipeSettings, track) {
                            buildLongPressAction(swipeSettings, swipeHandler, track)
                        }

                        GroupedTrackItemContainer(
                            index = index,
                            count = tracks.size,
                            containerColor = palette.surface,
                            dividerStartInset = 52.dp,
                        ) {
                            SwipeableRowWithGestures(
                                startGestures = startGestures,
                                endGestures = endGestures,
                                containerColor = rowContainer,
                            ) {
                                CiderDetailTrackRow(
                                    trackIndex = index,
                                    title = track.name,
                                    durationMs = track.duration,
                                    subtitle = trackArtists,
                                    isExplicit = track.explicit,
                                    isCurrent = isCurrent,
                                    isPlaying = isPlaybackPlaying,
                                    accentColor = palette.accent,
                                    monochrome = LocalUiSettings.current.monochromeTracks,
                                    onClick = {
                                        spirc.load(album.toSpotifyUri(), track.toSpotifyUri())
                                        highlightedUri = null
                                        viewModel.setTrack(track)
                                    },
                                    onLongClick = {
                                        if (longPressAction != null) {
                                            longPressAction()
                                        } else {
                                            GlobalPopupController.show(
                                                PopupSpec.TrackInfo(
                                                    track = track,
                                                    isLiked = isLiked,
                                                )
                                            )
                                        }
                                    },
                                    onMoreClick = {
                                        GlobalPopupController.show(
                                            PopupSpec.TrackInfo(
                                                track = track,
                                                isLiked = isLiked,
                                            )
                                        )
                                    },
                                    onArtworkClick = { artworkClick(track.uri) },
                                )
                            }
                        }
                    }

                    if (tracks.isNotEmpty()) {
                        item(key = "album_footer") {
                            val songLabel = formatSongCountLabel(tracks.size)
                            val runtimeLabel = formatTotalRuntimeLabel(totalRuntimeMs)
                            val dateLabel = album.date.takeIf { it.isSet() }?.let { formatReleaseDateLong(it) }
                            val footerText = if (!dateLabel.isNullOrBlank()) {
                                stringResource(
                                    R.string.detail_release_footer,
                                    dateLabel,
                                    songLabel,
                                    runtimeLabel,
                                )
                            } else {
                                stringResource(
                                    R.string.detail_count_runtime_footer,
                                    songLabel,
                                    runtimeLabel,
                                )
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

                DetailFloatingTopBar(
                    title = album.name,
                    collapseFraction = collapseFraction,
                    onBack = onBack,
                    tintColor = palette.backgroundTop,
                    trailingContent = {
                        FloatingRoundButton(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_TEXT, "https://open.spotify.com/album/${album.id}")
                                    type = "text/plain"
                                }
                                context.startActivity(
                                    Intent.createChooser(shareIntent, context.getString(R.string.common_share))
                                )
                            },
                            contentDescription = stringResource(R.string.common_share),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = null,
                                modifier = Modifier.size(19.dp),
                            )
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
