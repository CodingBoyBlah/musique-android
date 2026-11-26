package app.musique.ui.screens.player

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.musique.ALBUM_COVER_URL
import app.musique.R
import app.musique.core.model.Artist
import app.musique.core.model.CoverSize
import app.musique.core.model.Episode
import app.musique.core.model.getCover
import app.musique.ui.GlobalPopupController
import app.musique.ui.PopupSpec
import app.musique.ui.components.SmartImage
import app.musique.ui.lyrics.SyncedLyricsView
import app.musique.ui.model.player.PlayerAction
import app.musique.ui.theme.LocalNowPlayingPalette
import app.musique.ui.theme.MusiqueDimens
import app.musique.ui.theme.MusiqueMotion
import app.musique.ui.theme.MusiqueShapes
import app.musique.ui.viewmodel.player.PlaybackContextLabel
import app.musique.ui.viewmodel.player.PlayerViewModel
import kotlinx.coroutines.delay

/** How long the lyrics mode controls stay up after the last interaction while playing. */
private const val ControlsIdleMs = 3_000L
/** Height of the lyrics mode header row (thumbnail, title, buttons). */
private val LyricsHeaderHeight = 72.dp

@Composable
fun PlayerContent(
    viewModel: PlayerViewModel,
    expansionFractionProvider: () -> Float,
    onShowQueue: () -> Unit,
    onArtistClick: (Artist) -> Unit,
    onShowClick: (Episode?) -> Unit,
    listState: LazyListState,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val audio by viewModel.currentAudio.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isLiked by viewModel.isLiked.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val elapsedMs by viewModel.positionMs.collectAsStateWithLifecycle()
    val forwardMilliseconds by viewModel.forwardMilliseconds.collectAsState(15_000)
    val contextLabel by viewModel.playbackContextLabel.collectAsStateWithLifecycle()
    val bitrateKbps by viewModel.audioBitrateKbps.collectAsStateWithLifecycle()
    val isRemote by viewModel.isRemotePlayback.collectAsStateWithLifecycle()

    val palette = LocalNowPlayingPalette.current
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isEpisode = audio?.isEpisode() ?: false
    val track = audio?.sourceTrack

    var lyricsMode by rememberSaveable { mutableStateOf(false) }
    val canShowLyrics = !isEpisode && track != null
    LaunchedEffect(canShowLyrics) { if (!canShowLyrics) lyricsMode = false }
    BackHandler(enabled = lyricsMode) { lyricsMode = false }

    val artworkUrl = audio?.getCover(CoverSize.LARGE)?.uri?.let { ALBUM_COVER_URL + it }
    val contextName = when (val label = contextLabel) {
        PlaybackContextLabel.LikedSongs -> stringResource(R.string.common_liked_songs)
        is PlaybackContextLabel.Named -> label.title
        null -> track?.album?.name ?: audio?.showName
    }
    val qualityLabel = bitrateKbps?.let { stringResource(R.string.player_audio_quality_ogg, it) }

    val onMoreClick: (() -> Unit)? = track?.let { t ->
        { GlobalPopupController.show(PopupSpec.TrackInfo(t, isLiked = isLiked)) }
    }
    val onToggleFavorite: (() -> Unit)? = if (track != null) viewModel::toggleFavorite else null

    val chrome = PlayerChrome(
        titleRow = { compact, rowModifier ->
            NowPlayingTitleRow(
                audio = audio,
                isLiked = isLiked,
                isPlaying = isPlaying,
                expansionFractionProvider = expansionFractionProvider,
                onArtistClick = onArtistClick,
                onShowClick = onShowClick,
                onToggleFavorite = onToggleFavorite,
                onMoreClick = onMoreClick,
                compact = compact,
                modifier = rowModifier,
            )
        },
        speedControl = {
            if (isEpisode) {
                PlaybackSpeedControl(
                    currentSpeed = uiState.playbackSpeed,
                    onSpeedChange = viewModel::setPlaybackSpeed,
                )
            }
        },
        scrubber = {
            NowPlayingScrubber(
                elapsedMs = elapsedMs,
                durationMs = audio?.duration ?: 0L,
                isPlaying = isPlaying,
                qualityLabel = qualityLabel,
                onSeek = { viewModel.onAction(PlayerAction.SeekTo(it)) },
            )
        },
        transport = { height ->
            val canSeek = isEpisode && forwardMilliseconds > 0
            NowPlayingTransport(
                isAudioSet = audio != null,
                isBuffering = uiState.isBuffering,
                isPlaying = isPlaying,
                onPrevious = { viewModel.onAction(PlayerAction.Previous) },
                onNext = { viewModel.onAction(PlayerAction.Next) },
                onPlayPause = { viewModel.onAction(PlayerAction.PlayPause) },
                onRewind = if (canSeek) {
                    {
                        val position = (elapsedMs - forwardMilliseconds).coerceAtLeast(0)
                        viewModel.onAction(PlayerAction.SeekTo(position))
                    }
                } else null,
                onForward = if (canSeek) {
                    {
                        val position = (elapsedMs + forwardMilliseconds).coerceAtMost(uiState.totalLengthMs)
                        viewModel.onAction(PlayerAction.SeekTo(position))
                    }
                } else null,
                height = height,
            )
        },
        drawer = { bottomInset, rounded ->
            NowPlayingDrawer(
                contextName = contextName,
                showLyricsButton = canShowLyrics,
                lyricsActive = lyricsMode,
                isRemotePlayback = isRemote,
                onLyricsClick = { lyricsMode = !lyricsMode },
                onDevicesClick = viewModel::openDevices,
                onQueueClick = onShowQueue,
                bottomInset = bottomInset,
                shape = if (rounded) MusiqueShapes.sheetTop else MusiqueShapes.actionButton,
                showHandle = rounded,
            )
        },
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val viewportHeight = maxHeight

        NowPlayingBackdrop(palette = palette, isPlaying = isPlaying)

        if (isLandscape) {
            LandscapeNowPlaying(
                chrome = chrome,
                artworkUrl = artworkUrl,
                isPlaying = isPlaying,
                lyricsMode = lyricsMode,
                onExitLyrics = { lyricsMode = false },
                paddingValues = paddingValues,
            )
        } else {
            // A 1dp lead item lets lyrics mode park the list off its first item, so the sheet's
            // drag-to-dismiss doesn't take lyrics scrolls before the lyrics list sees them.
            LaunchedEffect(lyricsMode) {
                if (lyricsMode) listState.scrollToItem(1) else listState.scrollToItem(0)
            }
            LazyColumn(
                state = listState,
                userScrollEnabled = !lyricsMode,
                modifier = Modifier.fillMaxSize(),
            ) {
                item(key = "lead") { Spacer(Modifier.height(1.dp)) }
                item(key = "player") {
                    PortraitNowPlaying(
                        chrome = chrome,
                        artworkUrl = artworkUrl,
                        isPlaying = isPlaying,
                        lyricsMode = lyricsMode,
                        onExitLyrics = { lyricsMode = false },
                        paddingValues = paddingValues,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(viewportHeight),
                    )
                }
            }
        }
    }
}

/** The pieces of Now Playing, shared by the portrait and landscape layouts. */
private class PlayerChrome(
    val titleRow: @Composable (compact: Boolean, modifier: Modifier) -> Unit,
    val speedControl: @Composable () -> Unit,
    val scrubber: @Composable () -> Unit,
    val transport: @Composable (height: Dp) -> Unit,
    val drawer: @Composable (bottomInset: Dp, rounded: Boolean) -> Unit,
)

/**
 * Lyrics mode controls auto hide: while playing they fade out after [ControlsIdleMs] without
 * interaction; a hand scroll, a background tap or pausing brings them back. Never hidden while
 * paused or outside lyrics mode.
 */
private class ControlsAutoHide {
    var visible by mutableStateOf(true)
    var userScrolling by mutableStateOf(false)
    var wakeCount by mutableIntStateOf(0)

    fun wake() {
        visible = true
        wakeCount++
    }
}

@Composable
private fun rememberControlsAutoHide(lyricsMode: Boolean, isPlaying: Boolean): ControlsAutoHide {
    val state = remember { ControlsAutoHide() }
    LaunchedEffect(lyricsMode, isPlaying, state.userScrolling, state.wakeCount) {
        if (!lyricsMode || !isPlaying || state.userScrolling) {
            state.visible = true
            return@LaunchedEffect
        }
        delay(ControlsIdleMs)
        state.visible = false
    }
    return state
}

@Composable
private fun PortraitNowPlaying(
    chrome: PlayerChrome,
    artworkUrl: String?,
    isPlaying: Boolean,
    lyricsMode: Boolean,
    onExitLyrics: () -> Unit,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val statusTop = paddingValues.calculateTopPadding()
    val bottomInset = paddingValues.calculateBottomPadding()
    val side = MusiqueDimens.playerHorizontalPadding

    val pausedFraction = remember { Animatable(if (isPlaying) 0f else 1f) }
    LaunchedEffect(isPlaying) { pausedFraction.animateTo(if (isPlaying) 0f else 1f, MusiqueMotion.artwork()) }
    val lyricsFraction = remember { Animatable(if (lyricsMode) 1f else 0f) }
    LaunchedEffect(lyricsMode) {
        lyricsFraction.animateTo(if (lyricsMode) 1f else 0f, spring(dampingRatio = 0.86f, stiffness = 360f))
    }

    val autoHide = rememberControlsAutoHide(lyricsMode, isPlaying)
    // Height of the bottom chrome with the controls showing; the lyrics keep clear of it.
    var bottomChromeHeight by remember { mutableStateOf(0.dp) }

    val thumbX = 20.dp
    val thumbY = statusTop + 12.dp + (LyricsHeaderHeight - 12.dp - MusiqueDimens.lyricsCompactArtSize) / 2

    BoxWithConstraints(modifier = modifier) {
        val artworkSize = maxWidth

        if (lyricsMode || lyricsFraction.value > 0.01f) {
            SyncedLyricsView(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = lyricsFraction.value },
                contentPadding = PaddingValues(
                    top = statusTop + LyricsHeaderHeight,
                    bottom = bottomChromeHeight,
                ),
                onUserScrollingChange = { scrolling ->
                    autoHide.userScrolling = scrolling
                    if (scrolling) autoHide.wake()
                },
                onBackgroundTap = autoHide::wake,
            )
        }

        NowPlayingArtwork(
            artworkUrl = artworkUrl,
            fullSize = artworkSize,
            pausedTopInset = statusTop + 12.dp,
            thumbX = thumbX,
            thumbY = thumbY,
            pausedFraction = { pausedFraction.value },
            lyricsFraction = { lyricsFraction.value },
        )

        SheetGrabber(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = statusTop + 6.dp)
        )

        AnimatedVisibility(
            visible = lyricsMode,
            enter = fadeIn(tween(220, delayMillis = 80)),
            exit = fadeOut(tween(120)),
            modifier = Modifier
                .padding(top = statusTop + 12.dp)
                .fillMaxWidth()
                .height(LyricsHeaderHeight - 12.dp),
        ) {
            Box(Modifier.fillMaxSize()) {
                // Tapping the thumbnail goes back to the artwork.
                Box(
                    modifier = Modifier
                        .offset(x = thumbX)
                        .size(MusiqueDimens.lyricsCompactArtSize)
                        .align(Alignment.CenterStart)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onExitLyrics,
                        ),
                )
                chrome.titleRow(
                    true,
                    Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = thumbX + MusiqueDimens.lyricsCompactArtSize + 12.dp, end = 8.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged {
                    if (autoHide.visible) bottomChromeHeight = with(density) { it.height.toDp() }
                },
        ) {
            AnimatedVisibility(
                visible = !lyricsMode,
                enter = fadeIn(tween(220, delayMillis = 60)) + expandVertically(MusiqueMotion.sheet()),
                exit = fadeOut(tween(120)) + shrinkVertically(MusiqueMotion.sheet()),
            ) {
                Column(modifier = Modifier.padding(horizontal = side)) {
                    chrome.titleRow(false, Modifier)
                    Box(Modifier.padding(top = 8.dp)) { chrome.speedControl() }
                }
            }

            AnimatedVisibility(
                visible = autoHide.visible,
                enter = fadeIn(tween(120)) + slideInVertically(tween(120)) { with(density) { 10.dp.roundToPx() } },
                exit = fadeOut(tween(400, easing = FastOutSlowInEasing)) +
                    slideOutVertically(tween(400, easing = FastOutSlowInEasing)) { with(density) { 10.dp.roundToPx() } },
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = side)
                        .padding(top = 12.dp, bottom = 20.dp),
                ) {
                    chrome.scrubber()
                    Spacer(Modifier.height(18.dp))
                    chrome.transport(MusiqueDimens.transportHeight)
                }
            }

            chrome.drawer(bottomInset, true)
        }
    }
}

@Composable
private fun LandscapeNowPlaying(
    chrome: PlayerChrome,
    artworkUrl: String?,
    isPlaying: Boolean,
    lyricsMode: Boolean,
    onExitLyrics: () -> Unit,
    paddingValues: PaddingValues,
) {
    val autoHide = rememberControlsAutoHide(lyricsMode, isPlaying)
    val artworkScale = remember { Animatable(if (isPlaying) 1f else 0.9f) }
    LaunchedEffect(isPlaying) { artworkScale.animateTo(if (isPlaying) 1f else 0.9f, MusiqueMotion.artwork()) }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            if (lyricsMode) {
                Column(Modifier.fillMaxSize()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SmartImage(
                            url = artworkUrl,
                            shape = MusiqueShapes.artworkSmall,
                            modifier = Modifier
                                .size(MusiqueDimens.lyricsCompactArtSize)
                                .clickable(onClick = onExitLyrics),
                        )
                        Spacer(Modifier.size(12.dp))
                        chrome.titleRow(true, Modifier.weight(1f))
                    }
                    SyncedLyricsView(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        onUserScrollingChange = { scrolling ->
                            autoHide.userScrolling = scrolling
                            if (scrolling) autoHide.wake()
                        },
                        onBackgroundTap = autoHide::wake,
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    SmartImage(
                        url = artworkUrl,
                        shape = MusiqueShapes.cardLarge,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .widthIn(max = 420.dp)
                            .aspectRatio(1f)
                            .graphicsLayer {
                                scaleX = artworkScale.value
                                scaleY = artworkScale.value
                            },
                    )
                    chrome.titleRow(false, Modifier)
                    chrome.speedControl()
                }
            }
        }

        AnimatedVisibility(
            visible = autoHide.visible,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(400)),
        ) {
            Column {
                chrome.scrubber()
                Spacer(Modifier.height(10.dp))
                chrome.transport(64.dp)
            }
        }

        chrome.drawer(0.dp, false)
    }
}
