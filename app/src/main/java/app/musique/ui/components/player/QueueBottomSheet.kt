package app.musique.ui.components.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Explicit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoveUp
import androidx.compose.material.icons.rounded.RemoveCircle
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.musique.ALBUM_COVER_URL
import app.musique.R
import app.musique.core.model.Artist
import app.musique.core.model.CoverSize
import app.musique.core.model.PlayableAudio
import app.musique.core.model.Track
import app.musique.core.model.getCover
import app.musique.playback.model.RepeatMode
import app.musique.ui.components.SmartImage
import app.musique.ui.components.ToggleSegmentButton
import app.musique.ui.components.rows.SwipeGesture
import app.musique.ui.components.rows.SwipeableRowWithGestures
import app.musique.ui.screens.player.NowPlayingBackdrop
import app.musique.ui.screens.player.PlayerIconButton
import app.musique.ui.screens.player.SheetGrabber
import app.musique.ui.screens.player.formatPlayerTime
import app.musique.ui.theme.LocalNowPlayingPalette
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.theme.MusiqueDimens
import app.musique.ui.theme.MusiqueShapes
import app.musique.ui.theme.MusiqueType
import app.musique.ui.viewmodel.player.PlaybackContextLabel
import app.musique.ui.viewmodel.player.QueueViewModel
import app.musique.ui.viewmodel.player.MultiQueueViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private val PlayNextColor = Color(0xFF5856D6)
private val RemoveColor = Color(0xFFFF3B30)

/**
 * Cider / Apple Music style queue on the Now Playing backdrop: pinned now playing card,
 * Shuffle and Repeat pills, then the reorderable, swipeable list (history, current, up next).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedTransitionScope.QueueBottomSheet(
    sheetState: SheetState,
    viewModel: QueueViewModel,
    multiQueueViewModel: MultiQueueViewModel,
    onDismissRequest: () -> Unit,
    onArtistClick: (Artist) -> Unit,
    onArtworkClick: (Track) -> Unit,
) {
    val palette = LocalNowPlayingPalette.current
    val hapticFeedback = LocalHapticFeedback.current
    val queueState by viewModel.queueState.collectAsState()

    val isPlaybackPlaying by viewModel.isPlaying.collectAsState(initial = false)
    val isShuffling by viewModel.isShuffling.collectAsState(initial = false)
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val contextLabel by viewModel.playbackContextLabel.collectAsStateWithLifecycle()
    val currentTrack by viewModel.currentAudio.collectAsState(initial = null)

    val flipQueueGestures by viewModel.flipQueueGestures.collectAsState()

    val activeQueueId by multiQueueViewModel.activeQueueId.collectAsState()
    val savedQueues by multiQueueViewModel.queues.collectAsState()
    val activeQueueName = remember(activeQueueId, savedQueues) {
        savedQueues.find { it.id == activeQueueId }?.name
    }

    var showSwitcher by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel, activeQueueId) {
        viewModel.loadQueue(currentTrack)
    }

    var localTracks by remember { mutableStateOf(queueState.tracks) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(queueState.tracks) {
        if (!isDragging) localTracks = queueState.tracks
    }

    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(
        onMove = { from, to ->
            val fromIndex = from.index - 1
            val toIndex = to.index - 1
            if (fromIndex >= 0 && toIndex >= 0 &&
                fromIndex in localTracks.indices &&
                toIndex in localTracks.indices
            ) {
                localTracks = localTracks.toMutableList().apply {
                    add(toIndex, removeAt(fromIndex))
                }
                isDragging = true
            }
        },
        lazyListState = listState
    )

    LaunchedEffect(listState, isDragging) {
        snapshotFlow {
            if (isDragging) null
            else {
                val info = listState.layoutInfo
                val first = info.visibleItemsInfo.firstOrNull()?.index ?: 0
                val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
                first to last
            }
        }
            .distinctUntilChanged()
            .collect { indices ->
                indices?.let { (first, last) ->
                    viewModel.onScrollPositionChanged(
                        maxOf(0, first - 1),
                        maxOf(0, last - 1),
                        currentTrack
                    )
                }
            }
    }

    var hasInitialScrolled by remember { mutableStateOf(false) }

    LaunchedEffect(queueState.isLoading, queueState.tracks) {
        if (
            !hasInitialScrolled &&
            !queueState.isLoading &&
            queueState.tracks.isNotEmpty()
        ) {
            val index = queueState.currentIndex
                .coerceIn(queueState.tracks.indices)

            listState.scrollToItem(index + 1)
            hasInitialScrolled = true
        }
    }

    val currentTrackIndex = remember(localTracks, currentTrack) {
        localTracks.indexOfFirst { it.audio.uri == currentTrack?.uri }
    }
    val hasManualQueue = remember(localTracks, currentTrackIndex) {
        localTracks.withIndex().any { (index, entry) -> entry.isQueue && index > currentTrackIndex }
    }

    val coroutineScope = rememberCoroutineScope()
    val showScrollToCurrent by remember(listState, currentTrackIndex) {
        derivedStateOf {
            if (currentTrackIndex == -1 || localTracks.isEmpty()) return@derivedStateOf false
            val targetIndex = currentTrackIndex + 1 // Offset by 1 for the lead spacer item
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            val firstVisible = visibleItems.firstOrNull()?.index ?: 0
            val lastVisible = visibleItems.lastOrNull()?.index ?: 0
            targetIndex !in firstVisible..lastVisible
        }
    }

    // Up is true, down is false
    val scrollDirectionToCurrent by remember(listState, currentTrackIndex) {
        derivedStateOf {
            if (currentTrackIndex == -1 || localTracks.isEmpty()) return@derivedStateOf null
            val targetIndex = currentTrackIndex + 1
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            val firstVisible = visibleItems.firstOrNull()?.index ?: 0
            val lastVisible = visibleItems.lastOrNull()?.index ?: 0
            if (targetIndex !in firstVisible..lastVisible) targetIndex < firstVisible else null
        }
    }

    val contextName = when (val label = contextLabel) {
        PlaybackContextLabel.LikedSongs -> stringResource(R.string.common_liked_songs)
        is PlaybackContextLabel.Named -> label.title
        null -> currentTrack?.sourceTrack?.album?.name ?: currentTrack?.showName
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = MusiqueShapes.sheetTop,
        containerColor = palette.backgroundBottom,
        contentColor = MusiqueColors.textPrimary,
        dragHandle = null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            NowPlayingBackdrop(palette = palette, isPlaying = isPlaybackPlaying)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
            ) {
                SheetGrabber(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 6.dp)
                        .align(Alignment.CenterHorizontally)
                )

                // Pinned now playing card
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 12.dp),
                ) {
                    SmartImage(
                        url = currentTrack?.queueArtworkUrl(),
                        shape = MusiqueShapes.artworkSmall,
                        modifier = Modifier
                            .size(MusiqueDimens.queueArtSize)
                            .shadow(8.dp, MusiqueShapes.artworkSmall),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentTrack?.name ?: stringResource(R.string.player_nothing_playing),
                            style = MusiqueType.cardTitle,
                            color = MusiqueColors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = currentTrack?.subtitle().orEmpty(),
                            style = MusiqueType.cardSubtitle,
                            color = MusiqueColors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val source = activeQueueName ?: contextName
                        if (source != null) {
                            Text(
                                text = stringResource(R.string.player_playing_from, source),
                                style = MusiqueType.caption,
                                color = MusiqueColors.textTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    PlayerIconButton(
                        icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                        contentDescription = stringResource(R.string.common_save_queue),
                        onClick = { showSaveDialog = true },
                        iconSize = 22.dp,
                    )
                    PlayerIconButton(
                        icon = Icons.Rounded.LibraryMusic,
                        contentDescription = stringResource(R.string.common_saved_queues),
                        onClick = { showSwitcher = true },
                        iconSize = 22.dp,
                        tint = if (activeQueueId != null) palette.accent else MusiqueColors.textPrimary,
                    )
                }

                // Playback mode pills
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                ) {
                    ToggleSegmentButton(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        active = isShuffling,
                        activeCornerRadius = 16.dp,
                        onClick = { viewModel.toggleShuffle() },
                        text = stringResource(R.string.common_shuffle),
                        imageVector = Icons.Rounded.Shuffle,
                    )
                    ToggleSegmentButton(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        active = repeatMode != RepeatMode.NONE,
                        activeCornerRadius = 16.dp,
                        onClick = { viewModel.toggleRepeat() },
                        text = stringResource(
                            when (repeatMode) {
                                RepeatMode.ONE -> R.string.queue_repeat_one
                                RepeatMode.ALL -> R.string.queue_repeat_all
                                else -> R.string.common_repeat
                            }
                        ),
                        imageVector = if (repeatMode == RepeatMode.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    )
                }

                // Up Next header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 12.dp, top = 18.dp, bottom = 4.dp)
                        .height(32.dp),
                ) {
                    Text(
                        text = stringResource(R.string.queue_tab_up_next),
                        style = MusiqueType.sectionTitle,
                        color = MusiqueColors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    if (!queueState.isLoading && queueState.totalSize > 0) {
                        Text(
                            text = stringResource(R.string.count_songs, queueState.totalSize),
                            style = MusiqueType.caption,
                            color = MusiqueColors.textTertiary,
                        )
                    }
                    if (hasManualQueue) {
                        Text(
                            text = stringResource(R.string.queue_clear),
                            style = MusiqueType.navLabel,
                            color = palette.accent,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .clip(CircleShape)
                                .clickable { viewModel.clearManualQueue() }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }

                if (queueState.isLoadingPrevious) {
                    QueueLoadingBar()
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    when {
                        queueState.isLoading -> QueueStatus {
                            CircularProgressIndicator(color = MusiqueColors.textPrimary)
                            Text(
                                text = stringResource(R.string.player_loading_queue),
                                style = MusiqueType.body,
                                color = MusiqueColors.textSecondary,
                            )
                        }

                        queueState.tracks.isEmpty() -> QueueStatus {
                            Icon(
                                Icons.Rounded.SearchOff,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MusiqueColors.textTertiary,
                            )
                            Text(
                                text = stringResource(R.string.player_queue_empty),
                                style = MusiqueType.body,
                                color = MusiqueColors.textSecondary,
                            )
                        }

                        else -> LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            // Lead spacer keeps the +1 index offset the reorder math relies on.
                            item { Spacer(modifier = Modifier.height(0.dp)) }

                            itemsIndexed(
                                items = localTracks,
                                key = { _, item -> item.id },
                                contentType = { _, _ -> "track" },
                            ) { index, item ->
                                val isCurrentTrack = currentTrackIndex != -1 && index == currentTrackIndex
                                val isPrevious = currentTrackIndex != -1 && index < currentTrackIndex
                                val isNext = currentTrackIndex == -1 || index > currentTrackIndex

                                val prevItem = localTracks.getOrNull(index - 1)
                                val prevIsNext = currentTrackIndex == -1 || (index - 1) > currentTrackIndex

                                var rawHeaderText: String? = null
                                val showPreviousHeader = isPrevious && index == 0

                                if (isCurrentTrack) {
                                    rawHeaderText = stringResource(R.string.queue_now_playing)
                                } else if (isNext) {
                                    if (item.isQueue) {
                                        if (!prevIsNext || prevItem?.isQueue != true) {
                                            rawHeaderText = stringResource(R.string.queue_next_in_queue)
                                        }
                                    } else if (!prevIsNext || prevItem?.isQueue == true) {
                                        rawHeaderText = stringResource(R.string.queue_continue_playing)
                                    }
                                }

                                ReorderableItem(reorderState, key = item.id) { isDraggingItem ->
                                    val elevation by animateDpAsState(
                                        targetValue = if (isDraggingItem) 12.dp else 0.dp,
                                        label = "queueRowElevation"
                                    )

                                    // Freeze the header while dragging so the row doesn't snap its height.
                                    var frozenHeaderText by remember { mutableStateOf(rawHeaderText) }
                                    LaunchedEffect(rawHeaderText, isDraggingItem) {
                                        if (!isDraggingItem) frozenHeaderText = rawHeaderText
                                    }
                                    val displayHeaderText = if (isDraggingItem) frozenHeaderText else rawHeaderText

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animateItem()
                                    ) {
                                        if (showPreviousHeader) {
                                            QueueSectionLabel(stringResource(R.string.player_previous_tracks))
                                        }

                                        AnimatedVisibility(
                                            visible = displayHeaderText != null,
                                            enter = expandVertically() + fadeIn(),
                                            exit = shrinkVertically() + fadeOut()
                                        ) {
                                            QueueSectionLabel(displayHeaderText.orEmpty())
                                        }

                                        val playNextGesture = listOf(
                                            SwipeGesture(
                                                thresholdFraction = 0.25f,
                                                backgroundColor = PlayNextColor,
                                                icon = {
                                                    Icon(
                                                        Icons.Rounded.MoveUp,
                                                        contentDescription = stringResource(R.string.queue_play_next_action),
                                                        tint = Color.White,
                                                    )
                                                },
                                                onTrigger = {
                                                    val currentUri = currentTrack?.uri
                                                    val mutable = localTracks.toMutableList()
                                                    mutable.remove(item)
                                                    val currentIdx = mutable.indexOfFirst { it.audio.uri == currentUri }
                                                    val insertAt = (currentIdx + 1).coerceIn(0, mutable.size)
                                                    mutable.add(insertAt, item)
                                                    val newCurrentIdx = mutable.indexOfFirst { it.audio.uri == currentUri }
                                                        .coerceAtLeast(0)
                                                    viewModel.setQueueEntries(mutable, newCurrentIdx)
                                                    viewModel.debouncedSaveToRepository(mutable)
                                                }
                                            ),
                                        )
                                        val removeFromQueueGesture = listOf(
                                            SwipeGesture(
                                                thresholdFraction = 0.25f,
                                                backgroundColor = RemoveColor,
                                                icon = {
                                                    Icon(
                                                        Icons.Rounded.RemoveCircle,
                                                        contentDescription = stringResource(R.string.queue_remove_action),
                                                        tint = Color.White,
                                                    )
                                                },
                                                onTrigger = {
                                                    val currentUri = currentTrack?.uri
                                                    val mutable = localTracks.toMutableList()
                                                    mutable.remove(item)
                                                    val newCurrentIdx = mutable.indexOfFirst { it.audio.uri == currentUri }
                                                        .coerceAtLeast(0)
                                                    viewModel.setQueueEntries(mutable, newCurrentIdx)
                                                    viewModel.debouncedSaveToRepository(mutable)
                                                }
                                            )
                                        )

                                        val startGestures = when {
                                            isCurrentTrack -> emptyList()
                                            flipQueueGestures -> removeFromQueueGesture
                                            else -> playNextGesture
                                        }
                                        val endGestures = when {
                                            isCurrentTrack -> emptyList()
                                            flipQueueGestures -> playNextGesture
                                            else -> removeFromQueueGesture
                                        }

                                        val rowBackground by animateColorAsState(
                                            when {
                                                isDraggingItem -> palette.surface
                                                isCurrentTrack -> Color.White.copy(alpha = 0.10f)
                                                else -> Color.Transparent
                                            },
                                            label = "queueRowBackground",
                                        )

                                        SwipeableRowWithGestures(
                                            startGestures = startGestures,
                                            endGestures = endGestures,
                                            modifier = Modifier
                                                .padding(horizontal = 8.dp)
                                                .shadow(elevation, RoundedCornerShape(12.dp))
                                                .clip(RoundedCornerShape(12.dp)),
                                        ) {
                                            QueueTrackRow(
                                                audio = item.audio,
                                                isCurrent = isCurrentTrack,
                                                dimmed = isPrevious,
                                                background = rowBackground,
                                                onArtworkClick = item.audio.sourceTrack?.let { t -> { onArtworkClick(t) } },
                                                onArtistClick = item.audio.artists?.firstOrNull()?.let { a -> { onArtistClick(a) } },
                                                dragHandle = {
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier
                                                            .size(MusiqueDimens.minTouchTarget)
                                                            .draggableHandle(
                                                                onDragStarted = {
                                                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                    isDragging = true
                                                                },
                                                                onDragStopped = {
                                                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                    isDragging = false
                                                                    viewModel.setQueueEntries(localTracks)
                                                                    viewModel.debouncedSaveToRepository(localTracks)
                                                                }
                                                            ),
                                                    ) {
                                                        Icon(
                                                            Icons.Rounded.DragHandle,
                                                            contentDescription = stringResource(R.string.cd_reorder),
                                                            tint = Color.White.copy(alpha = 0.45f),
                                                            modifier = Modifier.size(24.dp),
                                                        )
                                                    }
                                                },
                                            )
                                        }
                                    }
                                }
                            }

                            item { Spacer(Modifier.height(24.dp)) }
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        modifier = Modifier.align(Alignment.BottomEnd),
                        visible = showScrollToCurrent,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
                    ) {
                        PlayerIconButton(
                            icon = if (scrollDirectionToCurrent ?: true) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.cd_scroll_to_current),
                            onClick = {
                                coroutineScope.launch {
                                    if (currentTrackIndex >= 0) {
                                        listState.animateScrollToItem(currentTrackIndex + 1)
                                    }
                                }
                            },
                            tint = MusiqueColors.onPillSelected,
                            background = MusiqueColors.pillSelected,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }

                if (queueState.isLoadingNext) {
                    QueueLoadingBar()
                }

                queueState.error?.let { error ->
                    Text(
                        text = stringResource(R.string.error_format, error),
                        color = RemoveColor,
                        style = MusiqueType.caption,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }

    if (showSwitcher) {
        QueueSwitcherBottomSheet(
            viewModel = multiQueueViewModel,
            currentAudio = currentTrack,
            onDismiss = { showSwitcher = false },
        )
    }

    if (showSaveDialog) {
        QueueNameDialog(
            title = stringResource(R.string.common_save_queue),
            confirmLabel = stringResource(R.string.common_save),
            onConfirm = { name ->
                multiQueueViewModel.saveCurrentQueue(name, currentTrack)
                showSaveDialog = false
            },
            onDismiss = { showSaveDialog = false },
        )
    }
}

private fun PlayableAudio.queueArtworkUrl(): String? {
    val uri = getCover(CoverSize.SMALL)?.uri ?: return null
    return if (uri.startsWith("https://")) uri else ALBUM_COVER_URL + uri
}

private fun PlayableAudio.subtitle(): String? {
    val artistNames = artists?.takeIf { it.isNotEmpty() }?.joinToString { it.name }
    val album = sourceTrack?.album?.name
    return when {
        artistNames != null && !album.isNullOrBlank() -> "$artistNames · $album"
        artistNames != null -> artistNames
        else -> showName
    }
}

@Composable
private fun QueueSectionLabel(text: String) {
    Text(
        text = text,
        style = MusiqueType.eyebrow,
        color = MusiqueColors.textTertiary,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 4.dp),
    )
}

@Composable
private fun QueueLoadingBar() {
    LinearProgressIndicator(
        color = MusiqueColors.textPrimary,
        trackColor = MusiqueColors.trackInactive,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(2.dp)
            .clip(CircleShape),
    )
}

@Composable
private fun QueueStatus(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            content()
        }
    }
}

/** 64dp queue row: 48dp art, title, artist · album, duration and a drag handle. */
@Composable
private fun QueueTrackRow(
    audio: PlayableAudio,
    isCurrent: Boolean,
    dimmed: Boolean,
    background: Color,
    onArtworkClick: (() -> Unit)?,
    onArtistClick: (() -> Unit)?,
    dragHandle: @Composable () -> Unit,
) {
    val palette = LocalNowPlayingPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(MusiqueDimens.queueRowHeight)
            .background(background)
            .padding(start = 12.dp, end = 2.dp),
    ) {
        SmartImage(
            url = audio.queueArtworkUrl(),
            shape = MusiqueShapes.artworkSmall,
            modifier = Modifier
                .size(MusiqueDimens.queueArtSize)
                .graphicsLayer { alpha = if (dimmed) 0.55f else 1f }
                .clip(MusiqueShapes.artworkSmall)
                .then(if (onArtworkClick != null) Modifier.clickable(onClick = onArtworkClick) else Modifier),
        )
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .graphicsLayer { alpha = if (dimmed) 0.55f else 1f },
        ) {
            Text(
                text = audio.name,
                style = MusiqueType.cardTitle.copy(fontWeight = FontWeight.SemiBold),
                color = if (isCurrent) palette.accent else MusiqueColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (audio.explicit) {
                    Icon(
                        Icons.Rounded.Explicit,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(14.dp),
                    )
                }
                Text(
                    text = audio.subtitle().orEmpty(),
                    style = MusiqueType.cardSubtitle,
                    color = Color.White.copy(alpha = 0.68f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = if (onArtistClick != null) Modifier.clickable(onClick = onArtistClick) else Modifier,
                )
            }
        }
        if (audio.duration > 0) {
            Text(
                text = formatPlayerTime(audio.duration),
                style = MusiqueType.rowDuration,
                color = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        dragHandle()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberQueueBottomSheetState(): QueueBottomSheetController {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val visible = remember { mutableStateOf(false) }
    return remember { QueueBottomSheetController(sheetState, visible) }
}

class QueueBottomSheetController @OptIn(ExperimentalMaterial3Api::class) constructor(
    val sheetState: SheetState,
    val visible: MutableState<Boolean>,
) {
    fun show() {
        visible.value = true
    }

    fun hide() {
        visible.value = false
    }
}
