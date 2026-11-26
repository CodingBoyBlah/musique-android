package app.musique.ui.screens.library.artist

import android.content.Intent
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.musique.ALBUM_COVER_URL
import app.musique.R
import app.musique.ScreenBottomPadding
import app.musique.core.model.Album
import app.musique.core.model.Artist
import app.musique.core.model.CoverSize
import app.musique.core.model.Track
import app.musique.core.model.getCover
import app.musique.core.model.toSpotifyUri
import app.musique.data.setting.LocalUiSettings
import app.musique.ui.GlobalPopupController
import app.musique.ui.PopupSpec
import app.musique.ui.components.ArtistDetailSkeleton
import app.musique.ui.components.ErrorScreen
import app.musique.ui.components.SmartImage
import app.musique.ui.components.bottomsheet.artist.ArtistLikedTracksBottomSheet
import app.musique.ui.components.musique.CiderDetailTrackSkeletonRow
import app.musique.ui.components.musique.DetailFloatingTopBar
import app.musique.ui.components.musique.DetailPaletteGradientBackdrop
import app.musique.ui.components.musique.FloatingRoundButton
import app.musique.ui.components.musique.GroupedTrackItemContainer
import app.musique.ui.components.musique.SectionHeader
import app.musique.ui.components.musique.SquareCard
import app.musique.ui.components.musique.rememberCollapseFraction
import app.musique.ui.components.rows.LocalRowContainerColor
import app.musique.ui.components.rows.SwipeableTrackRowConfigured
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.theme.MusiqueDimens
import app.musique.ui.theme.MusiqueShapes
import app.musique.ui.theme.MusiqueType
import app.musique.ui.theme.pressScale
import app.musique.ui.theme.rememberArtworkPalette
import app.musique.ui.viewmodel.detail.ArtistDetailViewModel
import app.musique.ui.viewmodel.detail.ArtistUiState

private val HeroHeight = 390.dp

@Composable
fun SharedTransitionScope.ArtistDetailScreen(
    viewModel: ArtistDetailViewModel,
    onArtworkClick: (Track) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onArtistClick: (Artist) -> Unit,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLikedSheet by remember { mutableStateOf(false) }

    when (uiState) {
        ArtistUiState.Loading -> {
            Box(Modifier.fillMaxSize().background(MusiqueColors.canvas)) {
                ArtistDetailSkeleton()
                DetailFloatingTopBar(title = "", collapseFraction = 0f, onBack = onBack)
            }
        }

        is ArtistUiState.Error -> {
            Box(Modifier.fillMaxSize().background(MusiqueColors.canvas)) {
                ErrorScreen(
                    message = (uiState as ArtistUiState.Error).message,
                    onRetry = { viewModel.retry() },
                )
                DetailFloatingTopBar(title = "", collapseFraction = 0f, onBack = onBack)
            }
        }

        is ArtistUiState.Success -> {
            val artist = (uiState as ArtistUiState.Success).artist
            val isContentLoading by viewModel.isContentLoading.collectAsState()
            val spirc = viewModel.spirc
            val context = LocalContext.current

            val portraitUrl = (artist.getCover(CoverSize.LARGE) ?: artist.getCover(CoverSize.MEDIUM))
                ?.uri?.let { ALBUM_COVER_URL + it }
            val paletteUrl = (artist.getCover(CoverSize.SMALL) ?: artist.getCover(CoverSize.MEDIUM))
                ?.uri?.let { ALBUM_COVER_URL + it } ?: portraitUrl
            val palette = rememberArtworkPalette(paletteUrl)

            val likedTracks by viewModel.likedTracks.collectAsState()
            val likedTrackIds by viewModel.likedTrackIds.collectAsState(initial = emptySet())
            val isSaved by viewModel.isSaved.collectAsState()
            val albums by viewModel.albums.collectAsState()
            val singles by viewModel.singles.collectAsState()
            val popularTracks by viewModel.popularTracks.collectAsState()
            val currentTrack by viewModel.currentAudio.collectAsState(initial = null)
            val isPlaybackPlaying by viewModel.isPlaying.collectAsState(initial = false)

            val lazyList = rememberLazyListState()
            val collapseFraction = rememberCollapseFraction(lazyList, thresholdDp = HeroHeight - 80.dp)

            val playArtist: () -> Unit = {
                val first = popularTracks.firstOrNull()
                if (first != null) {
                    spirc.load(artist.toSpotifyUri(), first.toSpotifyUri())
                    viewModel.setTrack(first)
                } else {
                    spirc.load(artist.toSpotifyUri())
                }
            }
            val showInfo: () -> Unit = {
                GlobalPopupController.show(
                    PopupSpec.ArtistInfo(
                        artist = artist,
                        isSaved = isSaved,
                        onToggleSave = { viewModel.toggleSave() },
                    )
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                DetailPaletteGradientBackdrop(palette = palette)

                LazyColumn(
                    state = lazyList,
                    contentPadding = PaddingValues(bottom = ScreenBottomPadding + 24.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item(key = "artist_hero") {
                        ArtistHero(
                            name = artist.name,
                            portraitUrl = portraitUrl,
                            fadeColor = palette.backgroundTop,
                            isSaved = isSaved,
                            onPlay = playArtist,
                            onShuffle = { spirc.shuffleLoad(artist.uri) },
                            onToggleFollow = { viewModel.toggleSave() },
                            onMore = showInfo,
                        )
                    }

                    if (likedTracks.isNotEmpty()) {
                        item(key = "artist_liked") {
                            LikedByYouRow(
                                likedCount = likedTracks.size,
                                previewTracks = likedTracks,
                                containerColor = palette.surface,
                                onClick = { showLikedSheet = true },
                                modifier = Modifier.padding(
                                    start = MusiqueDimens.screenPadding,
                                    end = MusiqueDimens.screenPadding,
                                    top = 8.dp,
                                ),
                            )
                        }
                    }

                    if (popularTracks.isNotEmpty() || isContentLoading) {
                        item(key = "top_songs_header") {
                            SectionHeader(
                                title = stringResource(R.string.artist_top_songs),
                                modifier = Modifier.padding(top = 24.dp, bottom = 12.dp),
                            )
                        }
                    }

                    if (popularTracks.isEmpty() && isContentLoading) {
                        items(5, key = { "top_song_skeleton_$it" }) { index ->
                            GroupedTrackItemContainer(
                                index = index,
                                count = 5,
                                containerColor = palette.surface,
                            ) {
                                CiderDetailTrackSkeletonRow(showArtwork = true)
                            }
                        }
                    }

                    // Swipeable rows stay vertical so their horizontal gestures keep working.
                    itemsIndexed(popularTracks, key = { _, track -> "top_song_${track.uri}" }) { index, track ->
                        GroupedTrackItemContainer(
                            index = index,
                            count = popularTracks.size,
                            containerColor = palette.surface,
                            dividerStartInset = 76.dp,
                        ) {
                            CompositionLocalProvider(LocalRowContainerColor provides palette.surface) {
                            SwipeableTrackRowConfigured(
                                track = track,
                                currentAudio = currentTrack,
                                isLiked = track.id in likedTrackIds,
                                isPlaybackPlaying = isPlaybackPlaying,
                                showAlbumName = true,
                                onRowClick = remember(track.uri) {
                                    {
                                        spirc.load(artist.toSpotifyUri(), track.toSpotifyUri())
                                        // Optimistic UI
                                        viewModel.setTrack(track)
                                    }
                                },
                                onArtistClick = { onArtistClick(it) },
                                onArtworkClick = { onArtworkClick(track) },
                            )
                            }
                        }
                    }

                    albumCarousel(
                        key = "albums",
                        title = R.string.artist_albums,
                        albums = albums,
                        onAlbumClick = onAlbumClick,
                    )
                    albumCarousel(
                        key = "singles",
                        title = R.string.artist_singles_eps,
                        albums = singles,
                        onAlbumClick = onAlbumClick,
                    )
                }

                DetailFloatingTopBar(
                    title = artist.name,
                    collapseFraction = collapseFraction,
                    onBack = onBack,
                    tintColor = palette.backgroundTop,
                    trailingContent = {
                        FloatingRoundButton(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_TEXT, "https://open.spotify.com/artist/${artist.id}")
                                    type = "text/plain"
                                }
                                context.startActivity(
                                    Intent.createChooser(shareIntent, context.getString(R.string.common_share))
                                )
                            },
                            contentDescription = stringResource(R.string.common_share),
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(19.dp))
                        }
                    },
                )

                if (showLikedSheet) {
                    ArtistLikedTracksBottomSheet(
                        viewModel = viewModel,
                        onArtworkClick = onArtworkClick,
                        onDismiss = { showLikedSheet = false }
                    )
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.albumCarousel(
    key: String,
    title: Int,
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
) {
    if (albums.isEmpty()) return
    item(key = "${key}_header") {
        SectionHeader(
            title = stringResource(title),
            modifier = Modifier.padding(top = 28.dp, bottom = 12.dp),
        )
    }
    item(key = "${key}_row") {
        LazyRow(
            contentPadding = PaddingValues(horizontal = MusiqueDimens.screenPadding),
            horizontalArrangement = Arrangement.spacedBy(MusiqueDimens.cardSpacing),
        ) {
            items(albums, key = { "${key}_${it.uri}" }) { album ->
                SquareCard(
                    title = album.name,
                    subtitle = album.date.takeIf { it.isSet() }?.year?.toString(),
                    imageUrl = (album.getCover(CoverSize.MEDIUM) ?: album.covers.firstOrNull())
                        ?.uri?.let { ALBUM_COVER_URL + it },
                    monochrome = LocalUiSettings.current.monochromeAlbums,
                    onClick = { onAlbumClick(album) },
                )
            }
        }
    }
}

/** Full-bleed portrait feathered into the page gradient, name and actions bottom-left. */
@Composable
private fun ArtistHero(
    name: String,
    portraitUrl: String?,
    fadeColor: Color,
    isSaved: Boolean,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onToggleFollow: () -> Unit,
    onMore: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(HeroHeight)
    ) {
        SmartImage(
            url = portraitUrl,
            shape = androidx.compose.ui.graphics.RectangleShape,
            alignment = Alignment.TopCenter,
            contentDescription = null,
            monochrome = LocalUiSettings.current.monochromeArtists,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.35f),
                            0.22f to Color.Transparent,
                            0.55f to Color.Transparent,
                            1f to fadeColor,
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = MusiqueDimens.screenPadding, vertical = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.home_kind_artist).uppercase(),
                style = MusiqueType.eyebrow,
                color = MusiqueColors.textSecondary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = name,
                style = MusiqueType.artistName,
                color = MusiqueColors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(14.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HeroCircleButton(
                    icon = Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(R.string.detail_play),
                    onClick = onPlay,
                    size = 48.dp,
                    containerColor = Color.White,
                    contentColor = MusiqueColors.onDarkPlay,
                )
                HeroCircleButton(
                    icon = Icons.Rounded.Shuffle,
                    contentDescription = stringResource(R.string.detail_shuffle),
                    onClick = onShuffle,
                )
                Spacer(Modifier.weight(1f))
                HeroCircleButton(
                    icon = if (isSaved) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = stringResource(if (isSaved) R.string.common_unfollow else R.string.common_follow),
                    onClick = onToggleFollow,
                )
                HeroCircleButton(
                    icon = Icons.Rounded.MoreHoriz,
                    contentDescription = stringResource(R.string.common_more_information),
                    onClick = onMore,
                )
            }
        }
    }
}

@Composable
private fun HeroCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    size: Dp = 44.dp,
    containerColor: Color = Color.White.copy(alpha = 0.16f),
    contentColor: Color = MusiqueColors.textPrimary,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(size)
            .pressScale(interaction, pressedScale = 0.9f, pressedAlpha = 0.8f)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClickLabel = contentDescription,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

/** "Liked by You" entry that opens the artist's liked songs sheet. */
@Composable
private fun LikedByYouRow(
    likedCount: Int,
    previewTracks: List<Track>,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interaction, pressedScale = 0.98f)
            .clip(MusiqueShapes.card)
            .background(containerColor)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(MusiqueDimens.rowArtworkSize)
                .clip(MusiqueShapes.artworkSmall)
                .background(MusiqueColors.accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.artist_liked_by_you),
                style = MusiqueType.rowTitle,
                color = MusiqueColors.textPrimary,
                maxLines = 1,
            )
            Text(
                text = stringResource(R.string.liked_you_count, likedCount),
                style = MusiqueType.rowSubtitle,
                color = MusiqueColors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
            previewTracks.take(3).forEach { track ->
                SmartImage(
                    url = track.album?.getCover(CoverSize.SMALL)?.uri?.let { ALBUM_COVER_URL + it },
                    imageSize = 30.dp,
                    shape = RoundedCornerShapeSmall,
                    contentDescription = null,
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = stringResource(R.string.library_open_liked),
            tint = MusiqueColors.textTertiary,
            modifier = Modifier
                .padding(start = 6.dp)
                .size(20.dp),
        )
    }
}

private val RoundedCornerShapeSmall = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
