package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TrackSourceType {
    LIVE_MIC,
    VIDEO_AUDIO,
    AUDIO_FILE
}

@Entity(tableName = "noise_cancelled_tracks")
data class ZeroNoiseItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val sourceType: TrackSourceType,
    val durationMs: Long,
    val filePath: String,
    val noiseReductionDb: Float,
    val voiceClarityScore: Int,
    val modeName: String,
    val timestamp: Long = System.currentTimeMillis()
)
