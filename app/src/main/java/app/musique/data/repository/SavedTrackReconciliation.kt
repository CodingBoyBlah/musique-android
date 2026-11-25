package app.musique.data.repository

import app.musique.data.database.track.LikedTrackEntity
import app.musique.data.metadata.SavedTrack

internal data class SavedTrackReconciliation(
    val upserts: List<LikedTrackEntity>,
    val removedIds: List<String>,
    val addedUris: List<String>,
) {
    val changed: Boolean get() = upserts.isNotEmpty() || removedIds.isNotEmpty()
}

internal fun reconcileSavedTracks(
    remote: List<SavedTrack>,
    cached: List<LikedTrackEntity>,
    now: Long,
): SavedTrackReconciliation {
    val existing = cached.associateBy { it.trackId }
    val remoteIds = remote.map { it.uri.removePrefix("spotify:track:") }.toHashSet()
    val rows = remote.mapIndexed { index, track ->
        val id = track.uri.removePrefix("spotify:track:")
        LikedTrackEntity(
            trackId = id,
            position = index.toDouble(),
            addedAt = track.addedAt?.takeIf { it > 0 }?.times(1000)
                ?: existing[id]?.addedAt ?: now,
        )
    }
    return SavedTrackReconciliation(
        upserts = rows.filter { existing[it.trackId] != it },
        removedIds = existing.keys.filterNot(remoteIds::contains),
        addedUris = remote.filter { it.uri.removePrefix("spotify:track:") !in existing }.map { it.uri },
    )
}
