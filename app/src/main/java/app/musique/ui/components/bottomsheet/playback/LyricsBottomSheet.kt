package app.musique.ui.components.bottomsheet.playback

import app.musique.R

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.Brush
import app.musique.ui.components.WavyMusicSlider
import app.musique.ui.lyrics.LyricsContent
import app.musique.ui.theme.LocalNowPlayingPalette
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.viewmodel.bottomsheet.LyricsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsBottomSheet(
    viewModel: LyricsViewModel,
    onDismissRequest: () -> Unit,
    onSeekToTimestamp: (Long) -> Unit,
    onPlayPause: () -> Unit = {},
    onSkipPrevious: () -> Unit = {},
    onSkipNext: () -> Unit = {},
    onSeek: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val positionMs by viewModel.positionMs.collectAsStateWithLifecycle()
    val isCurrentTrack by viewModel.isCurrentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val displayedTrack by viewModel.displayedTrack.collectAsState()
    val isEpisode by viewModel.isEpisode.collectAsState()
    val hasSyncedContent by viewModel.hasSyncedContent.collectAsState()
    val shouldRomanize by viewModel.shouldRomanize.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val palette = LocalNowPlayingPalette.current

    val showPlaybackControls = hasSyncedContent && isCurrentTrack

    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val activeTabColor = MaterialTheme.colorScheme.primaryContainer
    val inactiveTabColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val onActiveTabColor = MaterialTheme.colorScheme.onPrimaryContainer
    val onInactiveTabColor = MaterialTheme.colorScheme.onSurfaceVariant
    val playPauseButtonColor = MaterialTheme.colorScheme.secondaryContainer

    var selectedTab by remember { mutableIntStateOf(0) }
    val isSynced = selectedTab == 0 && showPlaybackControls

    fun formatTime(ms: Long): String {
        val s = (ms / 1000).coerceAtLeast(0L)
        return "%01d:%02d".format(s / 60, s % 60)
    }

    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(positionMs, durationMs, isDragging) {
        if (!isDragging && durationMs > 0) {
            sliderPosition = (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = palette.backgroundBottom,
        contentColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(palette.backgroundTop, palette.backgroundBottom)))
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Top bar: close button + track info
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .background(MusiqueColors.fillOnArtwork, CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_close),
                            tint = Color.White
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 56.dp)
                    ) {
                        Text(
                            text = displayedTrack?.name ?: "Unknown Track",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ),
                            color = Color.White
                        )
                        Text(
                            text = displayedTrack?.artists?.joinToString { it.name }
                                ?: "Unknown Artist",
                            style = MaterialTheme.typography.bodySmall,
                            color = MusiqueColors.textSecondary,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Synced / Static segmented selector
                if (showPlaybackControls) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val tabs = listOf(stringResource(R.string.lyrics_synced), stringResource(R.string.lyrics_static))
                        tabs.forEachIndexed { index, text ->
                            val isSelected = selectedTab == index
                            val tabBgColor by animateColorAsState(
                                targetValue = if (isSelected) activeTabColor else inactiveTabColor,
                                label = "tabBg"
                            )
                            val tabTextColor by animateColorAsState(
                                targetValue = if (isSelected) onActiveTabColor else onInactiveTabColor,
                                label = "tabText"
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(CircleShape)
                                    .background(tabBgColor)
                                    .clickable { selectedTab = index },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = text,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = tabTextColor
                                )
                            }
                        }

                        val tabBgColor by animateColorAsState(
                            targetValue = if (shouldRomanize) activeTabColor else inactiveTabColor,
                            label = "tabBg"
                        )
                        val tabTextColor by animateColorAsState(
                            targetValue = if (shouldRomanize) onActiveTabColor else onInactiveTabColor,
                            label = "tabText"
                        )

                        Box(
                            modifier = Modifier
                                .weight(0.5f)
                                .height(48.dp)
                                .clip(CircleShape)
                                .background(tabBgColor)
                                .clickable { viewModel.setRomanize(!shouldRomanize) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Translate,
                                contentDescription = null,
                                tint = tabTextColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Content area
                if (isEpisode) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.lyrics_no_episodes),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MusiqueColors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LyricsContent(
                        state = uiState,
                        clock = viewModel.clock,
                        onSeek = { if (showPlaybackControls) onSeekToTimestamp(it) },
                        synced = isSynced,
                        romanize = shouldRomanize,
                        contentPadding = PaddingValues(bottom = if (showPlaybackControls) 150.dp else 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }

            // Bottom overlay: pause button + slider bar
            if (showPlaybackControls) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            onClick = { onSkipPrevious() },
                            modifier = Modifier.size(width = 76.dp, height = 52.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = playPauseButtonColor,
                            tonalElevation = 6.dp
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = stringResource(R.string.common_previous),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(width = 96.dp, height = 72.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(playPauseButtonColor)
                                .clickable { onPlayPause() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Surface(
                            onClick = { onSkipNext() },
                            modifier = Modifier.size(width = 76.dp, height = 52.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = playPauseButtonColor,
                            tonalElevation = 6.dp
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = stringResource(R.string.common_next),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }


                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(54.dp)
                            .clip(CircleShape)
                            .background(surfaceVariant)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(positionMs),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        WavyMusicSlider(
                            value = sliderPosition,
                            onValueChange = {
                                isDragging = true
                                sliderPosition = it.coerceIn(0f, 1f)
                            },
                            onValueChangeFinished = {
                                onSeek(
                                    (sliderPosition * durationMs).toLong().coerceIn(0L, durationMs)
                                )
                                isDragging = false
                            },
                            inactiveTrackColor = MaterialTheme.colorScheme.secondary,
                            isPlaying = isPlaying,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )

                        Text(
                            text = formatTime(durationMs),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
