package app.musique.ui.lyrics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.theme.MusiqueType

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
    // Placeholder until the lyrics engine lands.
    Box(modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
        Text("Lyrics", style = MusiqueType.lyricLine, color = MusiqueColors.textSecondary)
    }
}
