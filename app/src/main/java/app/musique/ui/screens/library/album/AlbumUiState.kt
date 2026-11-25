package app.musique.ui.screens.library.album

import app.musique.core.model.Album
import app.musique.core.model.Track
import kotlinx.serialization.Serializable

@Serializable
data class AlbumUiState(
    val isLoading: Boolean = true,
    val album: Album? = null,
    val tracks: List<Track> = emptyList(),
    val isPlayable: Boolean = true,
    val error: String? = null
)

