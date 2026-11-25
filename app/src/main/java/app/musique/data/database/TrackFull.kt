package app.musique.data.database

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import app.musique.core.model.Album
import app.musique.core.model.FileId
import app.musique.core.model.ReleaseDate
import app.musique.core.model.Track
import app.musique.data.database.album.AlbumWithArtists
import app.musique.data.database.album.toDomain

data class TrackFull(
    @Embedded val track: TrackEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "trackId"
    )
    val files: List<TrackFileEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "artistId",
        associateBy = Junction(
            value = TrackArtistEntity::class,
            parentColumn = "trackId",
            entityColumn = "artistId"
        )
    )
    val artists: List<ArtistEntity>,

    @Relation(
        parentColumn = "albumId",
        entityColumn = "albumId",
        entity = AlbumEntity::class
    )
    val albumWithArtists: AlbumWithArtists?
)

fun TrackFull.toDomain(): Track {
    val domainArtists = artists.map { it.toDomain() }

    val albumDomain = albumWithArtists?.toDomain() ?: track.albumId?.let {
        Album(
            id = it,
            uri = "spotify:album:$it",
            name = track.albumName ?: "",
            artists = emptyList(),
            popularity = 0,
            covers = emptyList(),
            date = ReleaseDate()
        )
    }

    val domainFiles = files.map {
        FileId(
            type = it.type,
            id = it.fileId
        )
    }

    return Track(
        id = track.id,
        uri = track.trackUri,
        name = track.name,
        album = albumDomain,
        artists = domainArtists,
        popularity = track.popularity,
        duration = track.durationMs,
        explicit = track.explicit,
        files = domainFiles
    )
}
