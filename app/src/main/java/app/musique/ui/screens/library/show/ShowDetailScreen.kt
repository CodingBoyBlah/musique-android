package app.musique.ui.screens.library.show

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import app.musique.ALBUM_COVER_URL
import app.musique.R
import app.musique.ScreenBottomPadding
import app.musique.core.model.ConsumptionOrder
import app.musique.core.model.CoverSize
import app.musique.core.model.getCover
import app.musique.core.model.sharedTransitionKey
import app.musique.data.setting.LocalUiSettings
import app.musique.fabBottomPadding
import app.musique.ui.components.AlbumDetailSkeleton
import app.musique.ui.components.ErrorScreen
import app.musique.ui.components.musique.ActionSquircleButton
import app.musique.ui.components.musique.DetailFloatingTopBar
import app.musique.ui.components.musique.DetailHeroHeader
import app.musique.ui.components.musique.DetailPaletteGradientBackdrop
import app.musique.ui.components.musique.FloatingRoundButton
import app.musique.ui.components.musique.GroupedTrackItemContainer
import app.musique.ui.components.musique.rememberCollapseFraction
import app.musique.ui.components.rows.LocalRowContainerColor
import app.musique.ui.components.rows.SwipeableEpisodeRowConfigured
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.theme.MusiqueDimens
import app.musique.ui.theme.MusiqueType
import app.musique.ui.theme.rememberArtworkPalette
import app.musique.ui.viewmodel.library.ShowDetailViewModel
import kotlinx.coroutines.launch

private const val LOADING_MORE_SKELETON_COUNT = 3

@Composable
fun SharedTransitionScope.ShowDetailScreen(
    viewModel: ShowDetailViewModel,
    onBack: () -> Unit,
    artworkClick: (uri: String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    when {
        uiState.isLoading -> {
            Box(Modifier.fillMaxSize().background(MusiqueColors.canvas)) {
                AlbumDetailSkeleton()
                DetailFloatingTopBar(title = "", collapseFraction = 0f, onBack = onBack)
            }
        }

        uiState.error != null -> {
            Box(Modifier.fillMaxSize().background(MusiqueColors.canvas)) {
                ErrorScreen(
                    message = uiState.error!!,
                    onRetry = { viewModel.retry() },
                )
                DetailFloatingTopBar(title = "", collapseFraction = 0f, onBack = onBack)
            }
        }

        uiState.show != null -> {
            val show = uiState.show!!
            val totalEpisodes = show.episodes.size
            val episodes = uiState.episodes
            val isLoadingMore = uiState.isLoadingMore
            val hasMore = uiState.hasMore
            val effectiveOrder = uiState.consumptionOrder ?: show.consumptionOrder

            val artworkUrl = (show.getCover(CoverSize.LARGE) ?: show.getCover(CoverSize.MEDIUM))
                ?.uri?.let { ALBUM_COVER_URL + it }
            val paletteUrl = (show.getCover(CoverSize.SMALL) ?: show.getCover(CoverSize.MEDIUM))
                ?.uri?.let { ALBUM_COVER_URL + it } ?: artworkUrl
            val palette = rememberArtworkPalette(paletteUrl)

            val currentAudio by viewModel.currentAudio.collectAsState(initial = null)
            val isPlaybackPlaying by viewModel.isPlaying.collectAsState(initial = false)
            val likedEpisodeIds by viewModel.likedEpisodeIds.collectAsState()
            val isSaved by viewModel.isSaved.collectAsState()

            LaunchedEffect(episodes.size) {
                if (episodes.isNotEmpty()) {
                    viewModel.fetchEpisodeDetailsForShow()
                }
            }

            val lazyList = rememberLazyListState()
            val scope = rememberCoroutineScope()
            val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val collapseFraction = rememberCollapseFraction(lazyList, thresholdDp = 280.dp)

            val showScrollToTop by remember {
                derivedStateOf {
                    lazyList.firstVisibleItemIndex > 2 ||
                        lazyList.firstVisibleItemScrollOffset > 300
                }
            }

            // Hero, actions and the Episodes header come before the first episode.
            val headerItems = 3
            val shouldLoadMore by remember(episodes.size, hasMore, isLoadingMore) {
                derivedStateOf {
                    val lastVisible = lazyList.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    hasMore && !isLoadingMore && lastVisible - headerItems >= episodes.size - 5
                }
            }
            LaunchedEffect(shouldLoadMore) {
                if (shouldLoadMore) viewModel.loadMoreEpisodes()
            }

            val playNext: () -> Unit = {
                val target = when (effectiveOrder) {
                    ConsumptionOrder.SEQUENTIAL -> viewModel.findNextEpisode(episodes)
                    ConsumptionOrder.EPISODIC, ConsumptionOrder.RECENT -> episodes.firstOrNull()
                }
                target?.let { viewModel.playEpisode(it) }
            }
            val rowCount = episodes.size + if (isLoadingMore) LOADING_MORE_SKELETON_COUNT else 0

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
                    item(key = "show_hero") {
                        DetailHeroHeader(
                            title = show.name,
                            artworkUrl = artworkUrl,
                            creatorName = show.publisher,
                            metadataLine = pluralStringResource(
                                R.plurals.show_episode_count_plural,
                                totalEpisodes,
                                totalEpisodes,
                            ),
                            description = show.description.takeIf { it.isNotBlank() },
                            monochrome = LocalUiSettings.current.monochromeAlbums,
                            artworkModifier = Modifier.sharedBounds(
                                rememberSharedContentState(show.sharedTransitionKey()),
                                LocalNavAnimatedContentScope.current,
                            ),
                        )
                    }

                    item(key = "show_actions") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ActionSquircleButton(
                                onClick = playNext,
                                containerColor = Color.White,
                                contentColor = MusiqueColors.onDarkPlay,
                                enabled = episodes.isNotEmpty(),
                                modifier = Modifier
                                    .weight(0.76f)
                                    .height(MusiqueDimens.actionButtonHeight),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.detail_play), style = MusiqueType.button, maxLines = 1)
                                }
                            }
                            ActionSquircleButton(
                                onClick = { viewModel.toggleSave() },
                                containerColor = palette.surface.copy(alpha = 0.94f),
                                contentColor = MusiqueColors.textPrimary,
                                modifier = Modifier
                                    .weight(0.24f)
                                    .height(MusiqueDimens.actionButtonHeight),
                            ) {
                                Icon(
                                    imageVector = if (isSaved) Icons.Rounded.Check else Icons.Rounded.Add,
                                    contentDescription = stringResource(
                                        if (isSaved) R.string.common_unfollow else R.string.common_follow
                                    ),
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }

                    item(key = "show_episodes_header") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 12.dp, bottom = 6.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.search_section_episodes),
                                style = MusiqueType.sectionTitle,
                                color = MusiqueColors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            ConsumptionOrderMenu(
                                selected = effectiveOrder,
                                recommended = show.consumptionOrder,
                                onSelect = { viewModel.setConsumptionOrder(it) },
                            )
                        }
                    }

                    items(episodes.size, key = { index -> "show_episode_$index" }) { index ->
                        val episode = episodes[index]
                        GroupedTrackItemContainer(
                            index = index,
                            count = rowCount,
                            containerColor = palette.surface,
                            dividerStartInset = 76.dp,
                        ) {
                            CompositionLocalProvider(LocalRowContainerColor provides palette.surface) {
                                SwipeableEpisodeRowConfigured(
                                    episode = episode,
                                    isLoaded = currentAudio?.uri == episode.uri,
                                    isPlaybackPlaying = isPlaybackPlaying,
                                    isLiked = episode.id in likedEpisodeIds,
                                    onRowClick = remember(episode.uri) {
                                        { viewModel.playEpisode(episode) }
                                    },
                                    onArtworkClick = { artworkClick(episode.uri) }
                                )
                            }
                        }
                    }

                    if (isLoadingMore) {
                        items(
                            count = LOADING_MORE_SKELETON_COUNT,
                            key = { index -> "show_episode_skeleton_$index" }
                        ) { index ->
                            GroupedTrackItemContainer(
                                index = episodes.size + index,
                                count = rowCount,
                                containerColor = palette.surface,
                                dividerStartInset = 76.dp,
                            ) {
                                CompositionLocalProvider(LocalRowContainerColor provides palette.surface) {
                                    SwipeableEpisodeRowConfigured(episode = null)
                                }
                            }
                        }
                    }
                }

                DetailFloatingTopBar(
                    title = show.name,
                    collapseFraction = collapseFraction,
                    onBack = onBack,
                    tintColor = palette.backgroundTop,
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
                        onClick = { scope.launch { lazyList.scrollToItem(0) } },
                        contentDescription = stringResource(R.string.common_scroll_to_top),
                    ) {
                        Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = null, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsumptionOrderMenu(
    selected: ConsumptionOrder,
    recommended: ConsumptionOrder,
    onSelect: (ConsumptionOrder) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.AutoMirrored.Rounded.Sort,
                contentDescription = stringResource(R.string.library_change_episode_order),
                tint = MusiqueColors.textSecondary,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ConsumptionOrder.entries.forEach { order ->
                DropdownMenuItem(
                    text = {
                        Text(stringResource(order.label()) + if (order == recommended) stringResource(R.string.order_recommended) else "")
                    },
                    leadingIcon = if (order == selected) {
                        { Icon(Icons.Rounded.Check, contentDescription = null) }
                    } else null,
                    onClick = {
                        expanded = false
                        onSelect(order)
                    }
                )
            }
        }
    }
}

@StringRes
private fun ConsumptionOrder.label(): Int = when (this) {
    ConsumptionOrder.SEQUENTIAL -> R.string.order_sequential
    ConsumptionOrder.EPISODIC -> R.string.order_episodic
    ConsumptionOrder.RECENT -> R.string.order_recent
}
