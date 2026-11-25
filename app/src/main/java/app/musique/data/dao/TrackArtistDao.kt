package app.musique.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import app.musique.data.database.TrackArtistEntity
import javax.inject.Singleton

@Dao
@Singleton
interface TrackArtistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(joins: List<TrackArtistEntity>)
}