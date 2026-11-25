package app.musique.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.musique.core.AuthManager
import app.musique.core.AuthStateEvent
import app.musique.core.AuthStateEventBus
import app.musique.core.UserProfile
import app.musique.core.home.HomeItem
import app.musique.core.home.NativeHomeFeedClient
import app.musique.core.jam.JamClient
import app.musique.core.model.OutifyUri
import app.musique.core.model.Profile
import app.musique.core.spirc.SpircController
import app.musique.core.spirc.SpircWrapper
import app.musique.data.repository.CollectionLease
import app.musique.data.repository.CollectionSessionState
import app.musique.data.repository.HomeFeedCoordinator
import app.musique.data.repository.HomeLibraryPicks
import app.musique.data.repository.HomeTrackMetadata
import app.musique.data.dao.LikedDao
import app.musique.data.metadata.TrackMetadataHelper
import app.musique.playback.PlaybackStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    client: NativeHomeFeedClient,
    library: HomeLibraryPicks,
    private val sessions: CollectionSessionState,
    private val spirc: SpircWrapper,
    private val spircController: SpircController,
    playback: PlaybackStateHolder,
    private val userProfile: UserProfile,
    private val authManager: AuthManager,
    private val json: Json,
    jamClient: JamClient,
    metadata: TrackMetadataHelper,
    likedDao: LikedDao,
) : ViewModel() {
    private val coordinator = HomeFeedCoordinator(client, sessions, viewModelScope, library::observe)
    val uiState = coordinator.state
    val trackMetadata = HomeTrackMetadata(uiState, sessions, viewModelScope) { uris ->
        withContext(Dispatchers.IO) { metadata.getTrackMetadata(uris) }
    }.state
    @OptIn(ExperimentalCoroutinesApi::class)
    val likedIds = sessions.current.flatMapLatest { lease ->
        if (lease == null) flowOf(emptySet<String>()) else
            likedDao.observeHomeLikedIds(lease.session.accountId, lease.session.sessionId, lease.generation).map { it.toSet() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val jam = jamClient.state
    private val _username = MutableStateFlow<String?>(null)
    val username = _username.asStateFlow()
    private val _userImageUrl = MutableStateFlow<String?>(null)
    val userImageUrl = _userImageUrl.asStateFlow()
    private val _isPlaybackLoggedIn = MutableStateFlow(false)
    val isPlaybackLoggedIn = _isPlaybackLoggedIn.asStateFlow()
    val currentAudio = playback.state.map { it.currentAudio }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val isPlaying = playback.state.map { it.isPlaying }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        refreshPlaybackLoginState()
        viewModelScope.launch {
            AuthStateEventBus.events.collect { event ->
                if (event is AuthStateEvent.PlaybackLoggedIn || event is AuthStateEvent.PlaybackLoggedOut) {
                    refreshPlaybackLoginState()
                }
            }
        }
        viewModelScope.launch {
            sessions.current.onEach { lease ->
                _username.value = lease?.session?.accountId
                _userImageUrl.value = null
            }.collectLatest { lease ->
                if (lease == null) return@collectLatest
                _isPlaybackLoggedIn.value = true
                try {
                    val raw = withContext(Dispatchers.IO) { userProfile.getUserProfile(lease.session.accountId) }
                    sessions.ensureCurrent(lease)
                    val profile = raw?.let { json.decodeFromString<Profile>(it) }
                    _username.value = profile?.name?.takeIf { it.isNotBlank() } ?: lease.session.accountId
                    _userImageUrl.value = profile?.imageUrl
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { /* A profile failure does not gate recommendations. */ }
            }
        }
    }

    fun refreshPlaybackLoginState() {
        viewModelScope.launch {
            _isPlaybackLoggedIn.value = withContext(Dispatchers.IO) { authManager.hasCachedCredentials() }
        }
    }

    fun refresh() = coordinator.refresh()

    fun retry() {
        if (sessions.current.value == null) viewModelScope.launch {
            spircController.restartAndAwaitReady("home retry")
        } else coordinator.refresh()
    }

    fun isCurrent(lease: CollectionLease?): Boolean = lease != null && sessions.current.value == lease

    fun play(item: HomeItem, displayedLease: CollectionLease?) {
        if (!isCurrent(displayedLease)) return
        // The shared loader follows the active Connect device; browsing home never activates Outify.
        spirc.load(OutifyUri.fromUriString(item.uri))
    }
}
