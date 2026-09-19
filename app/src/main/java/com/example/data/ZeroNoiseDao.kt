package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ZeroNoiseDao {
    @Query("SELECT * FROM noise_cancelled_tracks ORDER BY timestamp DESC")
    fun getAllTracks(): Flow<List<ZeroNoiseItem>>

    @Query("SELECT * FROM noise_cancelled_tracks WHERE id = :id")
    suspend fun getTrackById(id: Long): ZeroNoiseItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: ZeroNoiseItem): Long

    @Delete
    suspend fun deleteTrack(track: ZeroNoiseItem)

    @Query("DELETE FROM noise_cancelled_tracks")
    suspend fun clearAll()
}
