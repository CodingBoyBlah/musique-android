package app.musique.ui.viewmodel.library.album

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.musique.core.SpClient
import app.musique.core.model.PlayableAudio
import app.musique.core.model.Track
import app.musique.core.spirc.SpircWrapper
import app.musique.data.dao.LikedDao
import app.musique.data.metadata.Metadata
import app.musique.playback.PlaybackStateHolder
import app.musique.ui.screens.library.album.AlbumUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

private const val ALBUM_STATE_KEY = "album_state"

data class AlbumViewModelKey(val albumUri: String)

/**
 * View model for album screen
 */
@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val metadata: Metadata,
    private val playbackStateHolder: PlaybackStateHolder,
    val spirc: SpircWrapper,
    val spClient: SpClient,
    val json: Json,
    val likedDao: LikedDao,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        savedStateHandle.get<String>(ALBUM_STATE_KEY)?.let {
            try {
                json.decodeFromString<AlbumUiState>(it)
            } catch (e: Exception) {
                AlbumUiState()
            }
        } ?: AlbumUiState()
    )
    val uiState: StateFlow<AlbumUiState> = _uiState

    @OptIn(ExperimentalCoroutinesApi::class)
    val likedTrackIds: StateFlow<Set<String>> =
        likedDao.observeLikedIds()
            .map { it.toHashSet() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptySet()
            )

    val currentAudio: StateFlow<PlayableAudio?> = playbackStateHolder.state
        .map { it.currentAudio }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val isPlaying: StateFlow<Boolean> = playbackStateHolder.state
        .map { it.isPlaying }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    private fun saveState(state: AlbumUiState) {
        savedStateHandle[ALBUM_STATE_KEY] = json.encodeToString(AlbumUiState.serializer(), state)
    }

    suspend fun loadAlbum(albumUri: String) {
        try {
            val album = withContext(Dispatchers.IO) {
                metadata.getAlbumMetadata(albumUri)
            }

            if (album == null) {
                val newState = AlbumUiState(
                    isLoading = false,
                    error = "Album not found"
                )
                _uiState.value = newState
                saveState(newState)
                return
            }

            val trackUris: List<String> = album.tracks.map { it }

            val tracks: List<Track> = if (trackUris.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    metadata.getTrackMetadata(trackUris)
                }
            } else emptyList()

            val newState = AlbumUiState(
                isLoading = false,
                album = album,
                tracks = tracks,
            )
            _uiState.value = newState
            saveState(newState)
        } catch (e: Exception) {
            val newState = AlbumUiState(
                isLoading = false,
                error = e.message
            )
            _uiState.value = newState
            saveState(newState)
        }
    }

    suspend fun loadAlbumFromTrackUri(trackUri: String) {
        try {
            val albumId = withContext(Dispatchers.IO) {
                metadata.getTrackAlbumId(trackUri)
            }

            if (albumId == null) {
                val newState = AlbumUiState(
                    isLoading = false,
                    error = "Album for track not found"
                )
                _uiState.value = newState
                saveState(newState)
                return
            }

            loadAlbum("spotify:album:$albumId")
        } catch (e: Exception) {
            val newState = AlbumUiState(
                isLoading = false,
                error = e.message
            )
            _uiState.value = newState
            saveState(newState)
        }
    }
}