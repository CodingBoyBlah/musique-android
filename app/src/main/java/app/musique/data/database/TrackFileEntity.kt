package app.musique.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import app.musique.core.model.FileType

@Entity(
    tableName = "track_files",
    primaryKeys = ["trackId", "type"],
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("trackId")]
)
data class TrackFileEntity(
    val trackId: String,
    val type: FileType,
    val fileId: String
)
