package app.musique.data.database

import androidx.room.Embedded
import androidx.room.Relation
import app.musique.core.model.Track
import app.musique.data.dao.AlbumDao
import app.musique.data.dao.LikedDao
import app.musique.data.database.album.AlbumWithArtists
import app.musique.data.database.track.LikedTrackEntity

/**
 * Join for cached liked track position with the TrackEntity
 */
data class LikedTrackWithTrack(
    @Embedded val liked: LikedTrackEntity,
    @Relation(
        parentColumn = "trackId",
        entityColumn = "trackUri"
    )
    val track: TrackEntity?
)

suspend fun LikedTrackWithTrack.toDomain(
    likedDao: LikedDao,
    albumDao: AlbumDao,
): Track {
    val trackEntity =
        track ?: throw IllegalStateException("Track missing for liked track ${liked.trackId}")

    val trackWithArtists = likedDao.getTracksWithArtists(listOf(trackEntity.id)).firstOrNull()
        ?: throw IllegalStateException("TrackWithArtists missing for ${trackEntity.id}")

    val albumWithArtists: AlbumWithArtists? = trackEntity.albumId?.let { albumId ->
        albumDao.getAlbumWithArtists(albumId)
    }

    return trackWithArtists.toDomain(albumWithArtists)
}