package app.musique.ui.viewmodel.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.musique.core.SpClient
import app.musique.core.model.CoverSize
import app.musique.core.model.LyricLine
import app.musique.core.model.OutifyUri
import app.musique.core.model.PlayableAudio
import app.musique.core.model.SpotifyUri
import app.musique.core.model.getCover
import app.musique.core.spirc.SpircWrapper
import app.musique.data.dao.LikedDao
import app.musique.data.metadata.Metadata
import app.musique.data.repository.LikedRepository
import app.musique.data.repository.PlayerRepository
import app.musique.data.repository.SettingsRepository
import app.musique.playback.PlaybackStateHolder
import app.musique.playback.model.PlaybackOwner
import app.musique.playback.model.PlaybackState
import app.musique.playback.model.RepeatMode
import app.musique.playback.model.getSpeed
import app.musique.ui.GlobalPopupController
import app.musique.ui.PopupSpec
import app.musique.ui.model.player.PlayerAction
import app.musique.ui.model.player.PlayerUIState
import coil3.ImageLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@HiltViewModel
class PlayerViewModel @Inject constructor(
    val spirc: SpircWrapper,
    val imageLoader: ImageLoader,
    private val playerRepository: PlayerRepository,
    private val playbackStateHolder: PlaybackStateHolder,
    private val settingsRepository: SettingsRepository,
    private val likedDao: LikedDao,
    private val likedRepository: LikedRepository,
    private val spClient: SpClient,
    private val metadata: Metadata,
) : ViewModel() {
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val _lyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val lyrics: StateFlow<List<LyricLine>> = _lyrics

    val positionMs: StateFlow<Long> = playbackStateHolder.positionTicks()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            playbackStateHolder.estimatePosition().inWholeMilliseconds
        )

    val isShuffling = playbackStateHolder.state.map { it.shuffleEnabled }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val romanizeLyrics: StateFlow<Boolean> = settingsRepository.romanizeLyrics
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val repeatMode: Flow<RepeatMode> = playbackStateHolder.state.map { it.repeatMode }

    @OptIn(ExperimentalCoroutinesApi::class)
    val isLiked: StateFlow<Boolean> =
        playbackStateHolder.state
            .map { it.currentAudio }
            .flatMapLatest { audio ->
                if (audio == null || audio.isEpisode()) {
                    flowOf(false)
                } else {
                    likedDao.observeIsTrackLiked(audio.id)
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                false
            )

    val forwardMilliseconds: Flow<Long> =
        settingsRepository.playbackSettings
            .map { it.forwardMilliseconds }

    init {
        viewModelScope.launch {
            playbackStateHolder.state.collect { playback ->
                _state.value = playback
            }
        }
    }

    val uiState: StateFlow<PlayerUIState> =
        playbackStateHolder.state
            .map { state ->
                val audio = state.currentAudio
                val position = state.position
                PlayerUIState(
                    title = audio?.name ?: "Unknown Track",
                    artists = audio?.artists ?: emptyList(),
                    albumArt = audio?.getCover(CoverSize.LARGE)?.uri,
                    isPlaying = state.isPlaying,
                    isExplicit = audio?.explicit ?: false,
                    playbackSpeed = state.playbackSpeed,
                    totalLengthMs = audio?.duration ?: 0L,
                    positionMs = position.active.inWholeMilliseconds,
                    lastUpdateTime = position.lastSync,
                    isBuffering = state.isBuffering,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayerUIState())

    /**
     * On Player UI action - like play/pause/..
     */
    fun onAction(action: PlayerAction) {
        viewModelScope.launch {
            when (action) {
                PlayerAction.PlayPause -> {
                    spirc.playerPlayPause()
                }

                PlayerAction.Next -> spirc.playerNext()
                PlayerAction.Previous -> {
                    spirc.playerPrevious()
                }

                is PlayerAction.SeekTo -> {
                    viewModelScope.launch {
                        spirc.seekTo(action.position)
                    }
                }

                PlayerAction.RepeatToggle -> {
                    if (!playbackStateHolder.localPlaybackAllowed) return@launch
                    val current = settingsRepository.repeatMode.first()
                    val next = current.next()
                    viewModelScope.launch {
                        settingsRepository.setRepeat(next.repeat)
                        settingsRepository.setRepeatTrack(next.repeatTrack)
                        spirc.repeat(next.repeat, next.repeatTrack)
                    }
                }

                PlayerAction.ShuffleToggle -> {
                    if (!playbackStateHolder.localPlaybackAllowed) return@launch
                    val newValue = !isShuffling.value
                    viewModelScope.launch {
                        settingsRepository.setShuffle(newValue)
                        spirc.shuffle(newValue)
                    }
                }
            }
        }
    }

    fun toggleFavorite() {
        val trackId = currentAudio.value?.id ?: return
        viewModelScope.launch {
            val wasLiked = likedRepository.isLiked(trackId)

            if (wasLiked) {
                likedRepository.removeLiked(trackId)
            } else {
                likedRepository.addLiked(trackId)
            }

            val success = withContext(Dispatchers.IO) {
                if (wasLiked) {
                    spClient.deleteItems(arrayOf("spotify:track:$trackId"))
                } else {
                    spClient.saveItems(arrayOf("spotify:track:$trackId"))
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

    @OptIn(ExperimentalCoroutinesApi::class)
    val playbackContextLabel: StateFlow<PlaybackContextLabel?> = combine(
        settingsRepository.lastContextUri.distinctUntilChanged(),
        playbackStateHolder.state.map { it.currentAudio to it.owner }.distinctUntilChanged(),
    ) { contextUri, (audio, owner) ->
        Triple(contextUri, audio, owner)
    }.flatMapLatest { (contextUri, audio, owner) ->
        flow {
            emit(resolvePlaybackContextLabel(contextUri, audio, owner == PlaybackOwner.REMOTE, metadata))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    val audioBitrateKbps: StateFlow<Int?> = combine(
        settingsRepository.playbackSettings.map { it.bitrate.getSpeed() }.distinctUntilChanged(),
        playbackStateHolder.state.map { (it.currentAudio != null) to (it.owner == PlaybackOwner.LOCAL) }.distinctUntilChanged(),
    ) { bitrateKbps, (hasAudio, isLocal) ->
        if (hasAudio && isLocal) bitrateKbps else null
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    val isRemotePlayback: StateFlow<Boolean> = playbackStateHolder.state
        .map { it.owner == PlaybackOwner.REMOTE }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    fun openDevices() {
        GlobalPopupController.show(PopupSpec.PlaybackDevices())
    }

    fun setPlaybackSpeed(speed: Float) {
        playbackStateHolder.setPlaybackSpeed(speed)
    }
}

sealed interface PlaybackContextLabel {
    data object LikedSongs : PlaybackContextLabel
    data class Named(val title: String) : PlaybackContextLabel
}

internal suspend fun resolvePlaybackContextLabel(
    contextUri: String?,
    audio: PlayableAudio?,
    isRemote: Boolean,
    metadata: Metadata,
): PlaybackContextLabel? {
    if (audio == null) return null

    if (!isRemote && !contextUri.isNullOrBlank()) {
        when (val parsed = OutifyUri.fromUriString(contextUri)) {
            is OutifyUri.Liked -> return PlaybackContextLabel.LikedSongs
            is OutifyUri.ArtistLiked -> {
                val artistName = audio.artists?.firstOrNull { it.id == parsed.artistId }?.name
                    ?: withContext(Dispatchers.IO) {
                        runCatching { metadata.getArtistMetadata("spotify:artist:${parsed.artistId}")?.name }.getOrNull()
                    }
                if (!artistName.isNullOrBlank()) return PlaybackContextLabel.Named(artistName)
            }
            is SpotifyUri.Album -> {
                val inlineAlbum = audio.sourceTrack?.album?.takeIf { it.uri == contextUri }?.name
                val name = inlineAlbum ?: withContext(Dispatchers.IO) {
                    runCatching { metadata.getAlbumMetadata(contextUri)?.name }.getOrNull()
                }
                if (!name.isNullOrBlank()) return PlaybackContextLabel.Named(name)
            }
            is SpotifyUri.Playlist -> {
                val name = withContext(Dispatchers.IO) {
                    runCatching { metadata.getPlaylistMetadata(contextUri, allowCached = true)?.attributes?.name }.getOrNull()
                }
                if (!name.isNullOrBlank()) return PlaybackContextLabel.Named(name)
            }
            is SpotifyUri.Artist -> {
                val inlineArtist = audio.artists?.firstOrNull { it.uri == contextUri }?.name
                val name = inlineArtist ?: withContext(Dispatchers.IO) {
                    runCatching { metadata.getArtistMetadata(contextUri)?.name }.getOrNull()
                }
                if (!name.isNullOrBlank()) return PlaybackContextLabel.Named(name)
            }
            is SpotifyUri.Show -> {
                val name = audio.showName?.takeIf { it.isNotBlank() } ?: withContext(Dispatchers.IO) {
                    runCatching { metadata.getShowMetadata(contextUri)?.name }.getOrNull()
                }
                if (!name.isNullOrBlank()) return PlaybackContextLabel.Named(name)
            }
            else -> Unit
        }
    }

    val fallback = audio.sourceTrack?.album?.name?.takeIf { it.isNotBlank() }
        ?: audio.showName?.takeIf { it.isNotBlank() }
    return fallback?.let { PlaybackContextLabel.Named(it) }
}
