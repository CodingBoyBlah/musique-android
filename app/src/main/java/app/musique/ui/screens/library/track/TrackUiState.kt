package app.musique.ui.screens.library.track

import app.musique.core.model.LyricLine
import app.musique.core.model.Track

data class TrackUiState(
    val isLoading: Boolean = true,
    val track: Track? = null,
    val lyrics: List<LyricLine> = emptyList(),
    val error: String? = null,
)
