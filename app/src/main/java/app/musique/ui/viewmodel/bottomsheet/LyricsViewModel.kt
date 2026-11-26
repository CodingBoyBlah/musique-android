package app.musique.ui.viewmodel.bottomsheet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.musique.core.model.LyricLine
import app.musique.core.model.PlayableAudio
import app.musique.core.model.Track
import app.musique.core.spirc.SpircWrapper
import app.musique.data.repository.PlayerRepository
import app.musique.data.repository.SettingsRepository
import app.musique.playback.PlaybackStateHolder
import app.musique.ui.lyrics.LyricsUiState
import app.musique.ui.lyrics.PlaybackClockSnapshot
import app.musique.ui.viewmodel.player.positionTicks
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@HiltViewModel
class LyricsViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val playbackStateHolder: PlaybackStateHolder,
    private val spirc: SpircWrapper,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _lyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val lyrics: StateFlow<List<LyricLine>> = _lyrics.asStateFlow()

    val positionMs: StateFlow<Long> = playbackStateHolder.positionTicks()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            playbackStateHolder.estimatePosition().inWholeMilliseconds
        )

    private val _isCurrentTrack = MutableStateFlow(false)
    val isCurrentTrack: StateFlow<Boolean> = _isCurrentTrack.asStateFlow()

    private val _displayedTrack = MutableStateFlow<Track?>(null)
    val displayedTrack: StateFlow<Track?> = _displayedTrack.asStateFlow()

    private val _isEpisode = MutableStateFlow(false)
    val isEpisode: StateFlow<Boolean> = _isEpisode.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    val hasSyncedContent: StateFlow<Boolean> = _lyrics
        .map { lines -> lines.any { it.synced } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val shouldRomanize = settingsRepository.romanizeLyrics
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val lyricsCache = mutableMapOf<String, List<LyricLine>>()
    private var followCurrentTrack = false
    private var currentAudioObserver: Job? = null
    private var fetchJob: Job? = null
    private var nowPlayingObserver: Job? = null

    val currentAudio: StateFlow<PlayableAudio?> = playbackStateHolder.state
        .map { it.currentAudio }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val isPlaying: StateFlow<Boolean> = playbackStateHolder.state
        .map { it.isPlaying }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val durationMs: StateFlow<Long> = playbackStateHolder.state
        .map { it.currentAudio?.duration ?: 0L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    /** Everything the lyrics view needs to decide between loading, missing, static and synced. */
    val uiState: StateFlow<LyricsUiState> = combine(
        _lyrics, _loading, _isEpisode, _displayedTrack,
    ) { lines, loading, episode, track ->
        when {
            episode -> LyricsUiState.Episode
            track == null -> LyricsUiState.Idle
            loading -> LyricsUiState.Loading
            lines.isEmpty() -> LyricsUiState.Missing
            else -> LyricsUiState.Ready(track.id, lines)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LyricsUiState.Loading)

    /**
     * Playback position reports for the lyric clock: one per 250ms while playing and one on every
     * play, pause, seek, speed or track change. The view interpolates between them per frame.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val clock: Flow<PlaybackClockSnapshot> = playbackStateHolder.state.flatMapLatest { state ->
        fun snapshot() = PlaybackClockSnapshot(
            trackId = state.currentAudio?.id,
            basePositionMs = playbackStateHolder.estimatePosition().inWholeMilliseconds,
            durationMs = state.currentAudio?.duration ?: 0L,
            isPlaying = state.isPlaying,
            isBuffering = state.isBuffering,
            playbackSpeed = state.playbackSpeed,
        )
        if (state.isPlaying) {
            flow {
                while (true) {
                    emit(snapshot())
                    delay(CLOCK_TICK_MS)
                }
            }
        } else {
            flowOf(snapshot())
        }
    }

    /**
     * Shows lyrics for whatever is playing and keeps following it across track changes.
     * Used by the Now Playing lyrics mode; safe to call again.
     */
    fun followNowPlaying() {
        if (nowPlayingObserver?.isActive == true) return
        currentAudioObserver?.cancel()
        followCurrentTrack = true
        nowPlayingObserver = viewModelScope.launch {
            playbackStateHolder.state
                .map { it.currentAudio }
                .distinctUntilChangedBy { it?.id }
                .collect { audio -> showAudio(audio) }
        }
    }

    private fun showAudio(audio: PlayableAudio?) {
        when {
            audio == null -> clearFor(episode = false)
            audio.isTrack() -> {
                val track = audio.sourceTrack ?: audio.toLyricsTrack()
                _displayedTrack.value = track
                _isEpisode.value = false
                _isCurrentTrack.value = true
                fetchLyrics(track)
            }
            else -> clearFor(episode = true)
        }
    }

    private fun clearFor(episode: Boolean) {
        fetchJob?.cancel()
        _displayedTrack.value = null
        _isEpisode.value = episode
        _loading.value = false
        _lyrics.value = emptyList()
        _isCurrentTrack.value = false
    }

    fun loadLyrics(track: Track, followCurrentTrack: Boolean = false) {
        nowPlayingObserver?.cancel()
        nowPlayingObserver = null
        this.followCurrentTrack = followCurrentTrack
        _displayedTrack.value = track
        _isEpisode.value = false

        _isCurrentTrack.value = playbackStateHolder.state.value.currentAudio?.id == track.id

        fetchLyrics(track)

        currentAudioObserver?.cancel()
        currentAudioObserver = viewModelScope.launch {
            currentAudio.collect { audio ->
                if (audio == null) return@collect

                val currentId = audio.id
                val displayedId = _displayedTrack.value?.id
                val matchesDisplayed = currentId == displayedId

                if (matchesDisplayed) {
                    _isCurrentTrack.value = true
                    return@collect
                }

                if (!this@LyricsViewModel.followCurrentTrack) {
                    _isCurrentTrack.value = false
                    return@collect
                }

                // following new audio track
                if (audio.isTrack()) {
                    val sourceTrack = audio.sourceTrack
                    if (sourceTrack != null) {
                        _displayedTrack.value = sourceTrack
                        _isEpisode.value = false
                        fetchLyrics(sourceTrack)
                        _isCurrentTrack.value = true
                    } else {
                        _isCurrentTrack.value = false
                    }
                } else {
                    fetchJob?.cancel()
                    _isEpisode.value = true
                    _loading.value = false
                    _lyrics.value = emptyList()
                    _isCurrentTrack.value = false
                }
            }
        }
    }

    private fun fetchLyrics(track: Track) {
        val trackId = track.id
        fetchJob?.cancel()
        val cached = lyricsCache[trackId]
        if (cached != null) {
            _lyrics.value = cached
            _loading.value = false
            return
        }
        _lyrics.value = emptyList()
        _loading.value = true
        fetchJob = viewModelScope.launch {
            val result = playerRepository.getLyrics(track)
            // A slower answer for a track we already moved past must not replace the current one.
            if (_displayedTrack.value?.id != trackId) return@launch
            _lyrics.value = result
            _loading.value = false
            // Empty can mean a timeout; let the next visit try again.
            if (result.isNotEmpty()) lyricsCache[trackId] = result
        }
    }

    fun seekTo(timestampMs: Long) {
        viewModelScope.launch {
            spirc.seekTo(timestampMs)
            playbackStateHolder.seekTo(timestampMs.toDuration(DurationUnit.MILLISECONDS))
        }
    }

    fun playPause() {
        viewModelScope.launch {
            spirc.playerPlayPause()
        }
    }

    fun skipPrevious() {
        viewModelScope.launch {
            spirc.playerPrevious()
        }
    }

    fun skipNext() {
        viewModelScope.launch {
            spirc.playerNext()
        }
    }

    fun setRomanize(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setRomanizeLyrics(enabled)
        }
    }
}

private const val CLOCK_TICK_MS = 250L

/** Enough of a [Track] for the lyrics lookup when playback has no full track attached (remote Connect). */
private fun PlayableAudio.toLyricsTrack(): Track = Track(
    id = id,
    uri = uri,
    name = name,
    artists = artists.orEmpty(),
    duration = duration,
    explicit = explicit,
    files = emptyList(),
)
