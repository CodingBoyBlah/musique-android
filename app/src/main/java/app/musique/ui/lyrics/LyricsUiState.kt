package app.musique.ui.lyrics

import androidx.compose.runtime.Immutable
import app.musique.core.model.LyricLine

/** What the lyrics surface shows for the track it follows. */
@Immutable
sealed interface LyricsUiState {
    /** Lyrics are being fetched. */
    data object Loading : LyricsUiState

    /** A podcast episode is playing: there is nothing to show. */
    data object Episode : LyricsUiState

    /** Nothing is playing. */
    data object Idle : LyricsUiState

    /** The provider has no lyrics for this track. */
    data object Missing : LyricsUiState

    /** Lyrics for [trackId]; synced lines carry their timing, unsynced ones are static text. */
    data class Ready(val trackId: String, val lines: List<LyricLine>) : LyricsUiState
}
