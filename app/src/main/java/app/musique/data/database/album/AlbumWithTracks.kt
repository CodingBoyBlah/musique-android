package app.musique.data.database.album

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import app.musique.data.database.AlbumEntity
import app.musique.data.database.TrackEntity

data class AlbumWithTracks(
    @Embedded val album: AlbumEntity,

    @Relation(
        parentColumn = "albumId",
        entityColumn = "id",
        associateBy = Junction(
            value = AlbumTrackCrossRef::class,
            parentColumn = "albumId",
            entityColumn = "trackId"
        )
    )
    val tracks: List<TrackEntity>
)
