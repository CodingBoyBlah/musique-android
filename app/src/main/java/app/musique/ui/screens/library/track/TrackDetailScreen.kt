package app.musique.ui.screens.library.track

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import app.musique.ALBUM_COVER_URL
import app.musique.R
import app.musique.ScreenBottomPadding
import app.musique.core.model.Album
import app.musique.core.model.CoverSize
import app.musique.core.model.LyricLine
import app.musique.core.model.getCover
import app.musique.core.model.sharedTransitionKey
import app.musique.core.model.toSpotifyUri
import app.musique.data.setting.LocalUiSettings
import app.musique.ui.components.ErrorScreen
import app.musique.ui.components.TrackDetailSkeleton
import app.musique.ui.components.musique.ActionSquircleButton
import app.musique.ui.components.musique.DetailFloatingTopBar
import app.musique.ui.components.musique.DetailHeroHeader
import app.musique.ui.components.musique.DetailPaletteGradientBackdrop
import app.musique.ui.components.musique.GroupedTrackItemContainer
import app.musique.ui.components.musique.GroupedTrackList
import app.musique.ui.components.musique.SectionHeader
import app.musique.ui.components.musique.formatTrackDuration
import app.musique.ui.components.rows.AlbumRow
import app.musique.ui.components.rows.ArtistRow
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.theme.MusiqueDimens
import app.musique.ui.theme.MusiqueType
import app.musique.ui.theme.rememberArtworkPalette
import app.musique.ui.viewmodel.detail.TrackDetailViewModel

@Composable
fun SharedTransitionScope.TrackDetailScreen(
    viewModel: TrackDetailViewModel,
    onBack: () -> Unit,
    artistClick: (uri: String) -> Unit,
    artworkClick: (album: Album?) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    when {
        uiState.isLoading -> {
            Box(Modifier.fillMaxSize().background(MusiqueColors.canvas)) {
                TrackDetailSkeleton()
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

        uiState.track != null -> {
            val track = uiState.track!!
            val lyrics = uiState.lyrics
            val album = track.album
            val artworkUrl = (album?.getCover(CoverSize.LARGE) ?: album?.getCover(CoverSize.MEDIUM))
                ?.uri?.let { ALBUM_COVER_URL + it }
            val paletteUrl = (album?.getCover(CoverSize.SMALL) ?: album?.getCover(CoverSize.MEDIUM))
                ?.uri?.let { ALBUM_COVER_URL + it } ?: artworkUrl
            val palette = rememberArtworkPalette(paletteUrl)
            val spirc = viewModel.spirc

            val likedTrackIds by viewModel.likedTrackIds.collectAsState()
            val isLiked = track.id in likedTrackIds

            val lazyList = androidx.compose.foundation.lazy.rememberLazyListState()
            val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val collapseFraction = app.musique.ui.components.musique.rememberCollapseFraction(
                lazyList,
                thresholdDp = 280.dp,
            )

            val songLabel = stringResource(R.string.common_track)
            val metadataLine = listOfNotNull(
                songLabel,
                album?.name,
                album?.date?.takeIf { it.isSet() }?.year?.toString(),
                formatTrackDuration(track.duration),
            ).joinToString(" · ")

            val play: () -> Unit = {
                spirc.load(track.toSpotifyUri())
                viewModel.setTrack(track)
            }

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
                    item(key = "track_hero") {
                        DetailHeroHeader(
                            title = track.name,
                            artworkUrl = artworkUrl,
                            creatorName = track.artists.joinToString(", ") { it.name },
                            creatorAvatarUrls = track.artists.mapNotNull { artist ->
                                artist.getCover(CoverSize.SMALL)?.uri?.let { ALBUM_COVER_URL + it }
                            },
                            metadataLine = metadataLine,
                            onCreatorClick = track.artists.firstOrNull()?.let { first ->
                                { artistClick(first.uri) }
                            },
                            monochrome = LocalUiSettings.current.monochromeTracks,
                            artworkModifier = album?.let {
                                Modifier.sharedBounds(
                                    rememberSharedContentState(it.sharedTransitionKey()),
                                    LocalNavAnimatedContentScope.current,
                                )
                            } ?: Modifier,
                        )
                    }

                    item(key = "track_actions") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ActionSquircleButton(
                                onClick = play,
                                containerColor = Color.White,
                                contentColor = MusiqueColors.onDarkPlay,
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
                                onClick = { viewModel.toggleLike(track.uri) },
                                containerColor = palette.surface.copy(alpha = 0.94f),
                                contentColor = MusiqueColors.textPrimary,
                                modifier = Modifier
                                    .weight(0.24f)
                                    .height(MusiqueDimens.actionButtonHeight),
                            ) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = stringResource(R.string.common_liked),
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }

                    if (album != null) {
                        item(key = "track_album_header") {
                            SectionHeader(
                                title = stringResource(R.string.detail_album_type),
                                modifier = Modifier.padding(top = 8.dp, bottom = 10.dp),
                            )
                        }
                        item(key = "track_album") {
                            GroupedTrackItemContainer(index = 0, count = 1, containerColor = palette.surface) {
                                AlbumRow(
                                    album = album,
                                    artworkUrl = album.getCover(CoverSize.MEDIUM)?.uri?.let { ALBUM_COVER_URL + it },
                                    onRowClick = { artworkClick(album) },
                                    sharedTransitionKey = null,
                                )
                            }
                        }
                    }

                    if (track.artists.isNotEmpty()) {
                        item(key = "track_artists_header") {
                            SectionHeader(
                                title = stringResource(R.string.search_section_artists),
                                modifier = Modifier.padding(top = 24.dp, bottom = 10.dp),
                            )
                        }
                        itemsIndexed(track.artists, key = { _, artist -> "artist_${artist.uri}" }) { index, artist ->
                            GroupedTrackItemContainer(
                                index = index,
                                count = track.artists.size,
                                containerColor = palette.surface,
                                dividerStartInset = 76.dp,
                            ) {
                                ArtistRow(
                                    artist = artist,
                                    artworkUrl = artist.getCover(CoverSize.MEDIUM)?.uri?.let { ALBUM_COVER_URL + it },
                                    onRowClick = { artistClick(artist.uri) },
                                    sharedTransitionKey = null,
                                )
                            }
                        }
                    }

                    if (lyrics.isNotEmpty()) {
                        item(key = "track_lyrics_header") {
                            SectionHeader(
                                title = stringResource(R.string.common_lyrics),
                                modifier = Modifier.padding(top = 24.dp, bottom = 10.dp),
                            )
                        }
                        item(key = "track_lyrics") {
                            GroupedTrackList(
                                containerColor = palette.surface,
                                modifier = Modifier.padding(horizontal = MusiqueDimens.screenPadding),
                            ) {
                                LyricsSection(lyrics = lyrics)
                            }
                        }
                    }
                }

                DetailFloatingTopBar(
                    title = track.name,
                    collapseFraction = collapseFraction,
                    onBack = onBack,
                    tintColor = palette.backgroundTop,
                )
            }
        }
    }
}

@Composable
private fun LyricsSection(lyrics: List<LyricLine>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        lyrics.forEach { line ->
            Text(
                text = line.text,
                style = MusiqueType.body,
                color = MusiqueColors.textSecondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
