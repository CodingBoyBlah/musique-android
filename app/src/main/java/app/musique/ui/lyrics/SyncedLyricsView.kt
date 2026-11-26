package app.musique.ui.lyrics

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.musique.ui.viewmodel.bottomsheet.LyricsViewModel

/**
 * Apple Music style synced lyrics for the track that is playing, drawn on a transparent
 * background so it sits on the Now Playing backdrop.
 *
 * Contract used by the Now Playing screen:
 * - Follows the current track by itself (loads lyrics, tracks position, seeks on line tap).
 * - [contentPadding] reserves space for the compact header above and the controls below.
 * - [onUserScrollingChange] is true while the user is scrolling the lyrics by hand, so the
 *   Now Playing screen can wake its auto hidden controls.
 * - [onBackgroundTap] fires on a tap that does not hit a line, to toggle the controls.
 */
@Composable
fun SyncedLyricsView(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    onUserScrollingChange: (Boolean) -> Unit = {},
    onBackgroundTap: () -> Unit = {},
) {
    // Keyed so it never shares state with the lyrics sheet, which loads a chosen track.
    val viewModel: LyricsViewModel = hiltViewModel(key = "now-playing-lyrics")
    LaunchedEffect(viewModel) { viewModel.followNowPlaying() }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val romanize by viewModel.shouldRomanize.collectAsStateWithLifecycle()

    LyricsContent(
        state = state,
        clock = viewModel.clock,
        onSeek = viewModel::seekTo,
        modifier = modifier,
        romanize = romanize,
        contentPadding = contentPadding,
        onUserScrollingChange = onUserScrollingChange,
        onBackgroundTap = onBackgroundTap,
    )
}
