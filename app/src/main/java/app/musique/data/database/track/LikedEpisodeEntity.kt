package app.musique.data.database.track

import androidx.room.Entity

@Entity(
    tableName = "liked_episodes",
    primaryKeys = ["episodeId"]
)
data class LikedEpisodeEntity(
    val episodeId: String,
    val position: Double,
    val addedAt: Long,
    val showUri: String = "",
)
