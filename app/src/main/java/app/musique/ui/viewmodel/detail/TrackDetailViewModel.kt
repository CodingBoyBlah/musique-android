package app.musique.ui.viewmodel.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.musique.core.SpClient
import app.musique.core.model.PlayableAudio
import app.musique.core.model.Track
import app.musique.core.model.toPlayableAudio
import app.musique.core.spirc.SpircController
import app.musique.core.spirc.SpircWrapper
import app.musique.data.dao.LikedDao
import app.musique.data.metadata.Metadata
import app.musique.data.repository.LikedRepository
import app.musique.data.repository.PlayerRepository
import app.musique.playback.PlaybackStateHolder
import app.musique.ui.screens.library.track.TrackUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class TrackDetailViewModel @Inject constructor(
    private val metadata: Metadata,
    private val playbackStateHolder: PlaybackStateHolder,
    val spirc: SpircWrapper,
    private val spircController: SpircController,
    private val spClient: SpClient,
    private val playerRepository: PlayerRepository,
    private val likedRepository: LikedRepository,
    private val likedDao: LikedDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrackUiState())
    val uiState: StateFlow<TrackUiState> = _uiState

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

    private var _lastTrackUri: String? = null

    fun retry() {
        val uri = _lastTrackUri ?: return
        viewModelScope.launch {
            spircController.restartAndAwaitReady("track retry")
            _uiState.value = TrackUiState(isLoading = true, error = null)
            loadTrack(uri)
        }
    }

    fun loadTrack(trackUri: String) {
        _lastTrackUri = trackUri
        viewModelScope.launch {
            try {
                val tracks = withContext(Dispatchers.IO) {
                    metadata.getTrackMetadata(listOf(trackUri))
                }
                val track = tracks.firstOrNull()
                if (track == null) {
                    _uiState.value = TrackUiState(isLoading = false, error = "Track not found")
                    return@launch
                }

                val lyrics = withContext(Dispatchers.IO) {
                    playerRepository.getLyrics(track)
                }

                _uiState.value = TrackUiState(
                    isLoading = false,
                    track = track,
                    lyrics = lyrics,
                )
            } catch (e: Exception) {
                _uiState.value = TrackUiState(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun toggleLike(trackUri: String) {
        viewModelScope.launch {
            val trackId = trackUri.substringAfterLast(":")
            val wasLiked = likedRepository.isLiked(trackId)

            if (wasLiked) {
                likedRepository.removeLiked(trackId)
            } else {
                likedRepository.addLiked(trackId)
            }

            val success = withContext(Dispatchers.IO) {
                if (wasLiked) {
                    spClient.deleteItems(arrayOf(trackUri))
                } else {
                    spClient.saveItems(arrayOf(trackUri))
                }
            }

            if (!success) {
                if (wasLiked) {
                    likedRepository.addLiked(trackId)
                } else {
                    likedRepository.removeLiked(trackId)
                }
            }
        }
    }

    fun setTrack(track: Track) {
        playbackStateHolder.setAudio(track.toPlayableAudio())
    }
}
