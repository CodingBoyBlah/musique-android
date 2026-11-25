package app.musique.ui.viewmodel

import android.app.Application
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Icon
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.musique.R
import app.musique.core.EpisodeDetails
import app.musique.core.RadioResult
import app.musique.core.SpClient
import app.musique.core.model.OutifyUri
import app.musique.core.model.PlayableAudio
import app.musique.core.model.Track
import app.musique.core.model.toPlayableAudio
import app.musique.core.model.toSpotifyUri
import app.musique.core.spirc.SpircWrapper
import app.musique.data.repository.InterfaceSettings
import app.musique.data.repository.LikedRepository
import app.musique.data.repository.SettingsRepository
import app.musique.data.setting.EpisodeSwipeActionHandler
import app.musique.data.setting.GestureSetting
import app.musique.data.setting.SwipeActionHandler
import app.musique.playback.PlaybackStateHolder
import app.musique.ui.GlobalPopupController
import app.musique.ui.PopupSpec
import app.musique.ui.notifications.InAppNotificationController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val playbackStateHolder: PlaybackStateHolder,
    private val spirc: SpircWrapper,
    private val settingsRepository: SettingsRepository,
    private val spClient: SpClient,
    private val likedRepository: LikedRepository,
    private val json: Json,
) : AndroidViewModel(application) {
    val swipeSettings: Flow<List<GestureSetting>> =
        settingsRepository.interfaceSettings.map { it.gestureSettings }

    val interfaceSettings: Flow<InterfaceSettings> =
        settingsRepository.interfaceSettings

    val episodeSwipeActionHandler = object : EpisodeSwipeActionHandler {
        override fun addToQueue(uri: String) {
            this@MainViewModel.addToQueue(uri)
        }

        override fun playNext(uri: String) {
            this@MainViewModel.playNext(uri)
        }

        override fun favorite(episodeUri: String) {
            this@MainViewModel.favorite(episodeUri)
        }
    }

    val swipeActionHandler = object : SwipeActionHandler {
        override fun addToQueue(uri: String) {
            this@MainViewModel.addToQueue(uri)
        }

        override fun playNext(uri: String) {
            this@MainViewModel.playNext(uri)
        }

        override fun startRadio(track: Track) {
            this@MainViewModel.startRadio(track)
        }

        override fun favorite(trackUri: String) {
            this@MainViewModel.favorite(trackUri)
        }

        override fun addToPlaylist(track: Track) {
            this@MainViewModel.addToPlaylist(track)
        }

        override fun trackInfo(track: Track) {
            openTrackInfo(track)
        }
    }

    val currentAudio: StateFlow<PlayableAudio?> = combine(
        playbackStateHolder.state.map { it.currentAudio },
        playbackStateHolder.playerDismissed,
    ) { audio, dismissed -> audio?.takeIf { !dismissed } }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    fun addToQueue(uri: String) {
        spirc.addToQueue(uri)
        val message = getApplication<Application>().getString(R.string.toast_added_to_queue)
        InAppNotificationController.show(
            message,
            { Icon(Icons.Default.Queue, contentDescription = message) },
            1000L
        )
    }

    fun playNext(uri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            spirc.playNext(uri)
        }
        val message = getApplication<Application>().getString(R.string.toast_inserted_to_queue)
        InAppNotificationController.show(
            message,
            { Icon(Icons.Default.Queue, contentDescription = message) },
            1000L
        )
    }

    fun startRadio(track: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            spirc.startRadio(track.toSpotifyUri(), false)
        }
        playbackStateHolder.setAudio(track.toPlayableAudio())
        val message = getApplication<Application>().getString(R.string.toast_radio_started)
        InAppNotificationController.show(
            message,
            { Icon(Icons.Default.Radio, contentDescription = message) },
            1000L
        )
    }

    fun getRadioUri(track: Track): String? {
        val jsonResult = spClient.getRadioForTrack(track.uri) ?: return null
        val result: RadioResult = json.decodeFromString(jsonResult)

        if (result.total == 0 || result.mediaItems.isEmpty()) {
            return null
        }

        return result.mediaItems.first().uri
    }

    fun addToPlaylist(track: Track) {
        GlobalPopupController.show(PopupSpec.AddToPlaylist(listOf(track)))
    }

    fun addToPlaylist(tracks: List<Track>) {
        GlobalPopupController.show(PopupSpec.AddToPlaylist(tracks))
    }

    fun favorite(rawUri: String) {
        viewModelScope.launch {
            val uri = OutifyUri.fromUriString(rawUri)
            val id = uri.id
            val isTrack = uri.isTrack

            val wasLiked = if (isTrack) {
                likedRepository.isLiked(id)
            } else {
                likedRepository.isLikedEpisode(id)
            }

            if (wasLiked) {
                if (isTrack) likedRepository.removeLiked(id) else likedRepository.removeLikedEpisode(
                    id
                )
            } else {
                if (isTrack) likedRepository.addLiked(id) else likedRepository.addLikedEpisode(id)
            }

            val success = withContext(Dispatchers.IO) {
                if (wasLiked) {
                    spClient.deleteItems(arrayOf(rawUri))
                } else {
                    spClient.saveItems(arrayOf(rawUri))
                }
            }

            if (!success) {
                if (wasLiked) {
                    if (isTrack) likedRepository.addLiked(id) else likedRepository.addLikedEpisode(
                        id
                    )
                } else {
                    if (isTrack) likedRepository.removeLiked(id) else likedRepository.removeLikedEpisode(
                        id
                    )
                }
                InAppNotificationController.show(
                    "Failed to update favorite",
                    durationMillis = 2000L
                )
            }
        }
    }

    fun openTrackInfo(track: Track) {
        viewModelScope.launch {
            val likedIndex = try {
                likedRepository.getTrackIndex(track.uri)
            } catch (_: Exception) {
                -1
            }

            val isLiked = likedRepository.isLiked(track.id)

            GlobalPopupController.show(
                PopupSpec.TrackInfo(
                    track,
                    likedTrackIndex = if (likedIndex >= 0) likedIndex else null,
                    isLiked = isLiked
                )
            )
        }
    }

    fun getEpisodeDetails(episodeId: String): EpisodeDetails {
        val raw = spClient.getEpisodeDetails(episodeId)
        return json.decodeFromString<EpisodeDetails>(raw)
    }
}
