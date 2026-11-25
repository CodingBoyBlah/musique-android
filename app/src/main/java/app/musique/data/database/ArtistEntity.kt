package app.musique.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.musique.core.model.Artist

@Entity(tableName = "artists")
data class ArtistEntity(
    @PrimaryKey val artistId: String,
    val uri: String,
    val name: String,
    val imageUrl: String?,
    val popularity: Int,

    val lastUpdated: Long,
)

fun ArtistEntity.toDomain(): Artist {
    return Artist(
        id = artistId,
        uri = uri,
        name = name,
        popularity = popularity,
        portraits = emptyList(),
        tracks = emptyList(),
    )
}