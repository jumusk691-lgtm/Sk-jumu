package com.example.data

import kotlinx.coroutines.flow.Flow

class ZeroNoiseRepository(private val dao: ZeroNoiseDao) {
    val allTracks: Flow<List<ZeroNoiseItem>> = dao.getAllTracks()

    suspend fun insertTrack(track: ZeroNoiseItem): Long = dao.insertTrack(track)

    suspend fun deleteTrack(track: ZeroNoiseItem) = dao.deleteTrack(track)

    suspend fun getTrackById(id: Long): ZeroNoiseItem? = dao.getTrackById(id)

    suspend fun clearAll() = dao.clearAll()
}
