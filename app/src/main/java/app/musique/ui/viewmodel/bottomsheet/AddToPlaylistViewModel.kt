package app.musique.ui.viewmodel.bottomsheet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.musique.core.SpClient
import app.musique.core.model.CoverSize
import app.musique.core.model.Playlist
import app.musique.core.model.Track
import app.musique.core.model.getCover
import app.musique.data.dao.PlaylistDao
import app.musique.data.database.playlist.PlaylistItemEntity
import app.musique.data.database.playlist.canModify
import app.musique.data.database.playlist.toDomain
import app.musique.data.metadata.Metadata
import app.musique.data.repository.CollectionSessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AddToPlaylistViewModel @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val spClient: SpClient,
    private val metadata: Metadata,
    private val collectionSessions: CollectionSessionState,
) : ViewModel() {
    private val mutationMutex = Mutex()
    private val usernameFlow = flow {
        emit(spClient.username() ?: "no-username")
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ""
    )

    private val playlistsWithArtwork = playlistDao.getPlaylistsWithItemsFlow()
        .combine(usernameFlow) { playlists, username ->
            playlists
                .filter { it.canModify(username) }
                .map { entity ->
                    val playlist = entity.toDomain()
                    val artwork = playlist.getCover(metadata, CoverSize.MEDIUM)
                    PlaylistUi(playlist = playlist, artworkUrl = artwork)
                }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val ownedPlaylists = playlistsWithArtwork

    fun addToPlaylist(tracks: List<Track>, playlist: Playlist) {
        viewModelScope.launch {
            mutationMutex.withLock {
                val lease = collectionSessions.requireCurrent()
                val trackUris = tracks.map { it.uri }
                val success = withContext(Dispatchers.IO) {
                    spClient.addToPlaylist(playlist.id, trackUris.toTypedArray())
                }
                collectionSessions.ensureCurrent(lease)
                if (!success) return@withLock
                val maxPosition = playlistDao.getMaxPosition(playlist.id)

                val newItems = tracks.mapIndexed { index, track ->
                    PlaylistItemEntity(
                        playlistId = playlist.id,
                        position = maxPosition + 1 + index,
                        trackUri = track.uri,
                        addedBy = lease.session.accountId,
                        timestamp = System.currentTimeMillis(),
                        seenAt = 0L,
                        isPublic = false,
                    )
                }
                playlistDao.insertItems(newItems)
            }
        }
    }

    fun addTrackToPlaylist(track: Track, playlist: Playlist) {
        addToPlaylist(listOf(track), playlist)
    }

    fun removeFromPlaylist(tracks: List<Track>, playlist: Playlist) {
        viewModelScope.launch {
            mutationMutex.withLock {
                val lease = collectionSessions.requireCurrent()
                val trackUris = tracks.map { it.uri }
                val success = withContext(Dispatchers.IO) {
                    spClient.deleteFromPlaylist(playlist.id, trackUris.toTypedArray())
                }
                collectionSessions.ensureCurrent(lease)
                if (success) playlistDao.deleteItemsByUris(playlist.id, trackUris)
            }
        }
    }

    fun removeTrackFromPlaylist(track: Track, playlist: Playlist) {
        removeFromPlaylist(listOf(track), playlist)
    }

    data class PlaylistUi(
        val playlist: Playlist,
        val artworkUrl: String?
    )
}
