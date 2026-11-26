package app.musique.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.musique.R
import app.musique.ScreenBottomPadding
import app.musique.core.model.ProfilePlaylist
import app.musique.data.setting.LocalUiSettings
import app.musique.ui.components.ErrorScreen
import app.musique.ui.components.ProfileDetailSkeleton
import app.musique.ui.components.ScreenshotAvatar
import app.musique.ui.components.SmartImage
import app.musique.ui.components.musique.ActionSquircleButton
import app.musique.ui.components.musique.AppleListRow
import app.musique.ui.components.musique.DetailFloatingTopBar
import app.musique.ui.components.musique.DetailPaletteGradientBackdrop
import app.musique.ui.components.musique.GroupedTrackItemContainer
import app.musique.ui.components.musique.SectionHeader
import app.musique.ui.components.musique.rememberCollapseFraction
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.theme.MusiqueDimens
import app.musique.ui.theme.MusiqueShapes
import app.musique.ui.theme.MusiqueType
import app.musique.ui.theme.rememberArtworkPalette
import app.musique.ui.viewmodel.library.ProfileDetailViewModel
import app.musique.ui.viewmodel.library.ProfileUiState

@Composable
fun ProfileDetailScreen(
    viewModel: ProfileDetailViewModel,
    onBack: () -> Unit,
    onPlaylistClick: (String) -> Unit,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState(initial = null)
    val screenshotMode = LocalUiSettings.current.screenshotMode
    val lazyList = rememberLazyListState()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val collapseFraction = rememberCollapseFraction(lazyList, thresholdDp = 220.dp)

    when (val state = uiState) {
        is ProfileUiState.Loading -> {
            Box(modifier.fillMaxSize().background(MusiqueColors.canvas)) {
                ProfileDetailSkeleton(modifier = Modifier.fillMaxSize())
                DetailFloatingTopBar(title = "", collapseFraction = 0f, onBack = onBack)
            }
        }

        is ProfileUiState.Error -> {
            Box(modifier.fillMaxSize().background(MusiqueColors.canvas)) {
                ErrorScreen(
                    message = state.message,
                    onRetry = { viewModel.retry() },
                )
                DetailFloatingTopBar(title = "", collapseFraction = 0f, onBack = onBack)
            }
        }

        is ProfileUiState.Success -> {
            val profile = state.profile
            val hideProfile = screenshotMode && currentUserId != null &&
                profile?.uri == "spotify:user:$currentUserId"
            val artworkUrl = if (hideProfile) null else profile?.imageUrl
            val playlists = profile?.publicPlaylists ?: emptyList()
            val name = if (hideProfile) stringResource(R.string.screenshot_mode_username) else profile?.name ?: ""
            val palette = rememberArtworkPalette(artworkUrl)

            Box(modifier = modifier.fillMaxSize()) {
                DetailPaletteGradientBackdrop(palette = palette)

                LazyColumn(
                    state = lazyList,
                    contentPadding = PaddingValues(
                        top = statusBarTop + MusiqueDimens.collapsedBarHeight + 12.dp,
                        bottom = ScreenBottomPadding + 24.dp,
                    ),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(key = "profile_hero") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(168.dp)
                                    .shadow(16.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(MusiqueColors.avatarPlaceholder),
                                contentAlignment = Alignment.Center,
                            ) {
                                when {
                                    hideProfile -> ScreenshotAvatar(Modifier.size(168.dp))
                                    !artworkUrl.isNullOrBlank() -> SmartImage(
                                        url = artworkUrl,
                                        shape = CircleShape,
                                        contentDescription = name,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                    else -> Icon(
                                        Icons.Rounded.Person,
                                        contentDescription = null,
                                        tint = MusiqueColors.avatarPlaceholderIcon,
                                        modifier = Modifier.size(72.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = name,
                                style = MusiqueType.heroTitle,
                                color = MusiqueColors.textPrimary,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                ProfileStat(
                                    value = formatCount(state.followersCount),
                                    label = stringResource(R.string.library_followers),
                                    onClick = onFollowersClick,
                                )
                                ProfileStat(
                                    value = formatCount(state.followingCount),
                                    label = stringResource(R.string.library_following),
                                    onClick = onFollowingClick,
                                )
                            }
                        }
                    }

                    item(key = "profile_follow") {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            ActionSquircleButton(
                                onClick = { viewModel.toggleFollow() },
                                containerColor = if (state.isFollowing) palette.surface.copy(alpha = 0.94f) else Color.White,
                                contentColor = if (state.isFollowing) MusiqueColors.textPrimary else MusiqueColors.onDarkPlay,
                                shape = CircleShape,
                                modifier = Modifier
                                    .padding(vertical = 18.dp)
                                    .width(180.dp)
                                    .height(46.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (state.isFollowing) Icons.Rounded.PersonRemove else Icons.Rounded.PersonAdd,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(if (state.isFollowing) R.string.common_unfollow else R.string.common_follow),
                                        style = MusiqueType.button,
                                    )
                                }
                            }
                        }
                    }

                    if (playlists.isNotEmpty()) {
                        item(key = "profile_playlists_header") {
                            SectionHeader(
                                title = stringResource(R.string.playlist_public_playlists),
                                modifier = Modifier.padding(top = 8.dp, bottom = 10.dp),
                            )
                        }
                    }

                    itemsIndexed(playlists, key = { _, p -> p.uri }) { index, playlist ->
                        GroupedTrackItemContainer(
                            index = index,
                            count = playlists.size,
                            containerColor = palette.surface,
                            dividerStartInset = 76.dp,
                        ) {
                            ProfilePlaylistRow(
                                playlist = playlist,
                                onClick = { onPlaylistClick(playlist.uri) },
                            )
                        }
                    }
                }

                DetailFloatingTopBar(
                    title = name,
                    collapseFraction = collapseFraction,
                    onBack = onBack,
                    tintColor = palette.backgroundTop,
                )
            }
        }
    }
}

@Composable
private fun ProfileStat(value: String, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(MusiqueShapes.artworkSmall)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = value, style = MusiqueType.cardTitle, color = MusiqueColors.textPrimary)
        Spacer(Modifier.width(4.dp))
        Text(text = label, style = MusiqueType.cardSubtitle, color = MusiqueColors.textSecondary)
    }
}

@Composable
private fun ProfilePlaylistRow(
    playlist: ProfilePlaylist,
    onClick: () -> Unit,
) {
    AppleListRow(
        title = playlist.name,
        subtitle = stringResource(R.string.count_followers, playlist.followersCount),
        onClick = onClick,
        leading = {
            SmartImage(
                url = playlist.imageUrl,
                imageSize = MusiqueDimens.rowArtworkSize,
                shape = MusiqueShapes.artworkSmall,
                monochrome = LocalUiSettings.current.monochromePlaylists,
            )
        },
    )
}

private fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}
