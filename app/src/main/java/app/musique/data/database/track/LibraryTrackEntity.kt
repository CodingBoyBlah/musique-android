package app.musique.data.database.track

import androidx.room.Entity

@Entity(primaryKeys = ["trackId"])
data class LibraryTrackEntity(
    val trackId: String,
    val addedAt: Long,
)
