package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun fromSourceType(value: TrackSourceType): String = value.name

    @TypeConverter
    fun toSourceType(value: String): TrackSourceType = try {
        TrackSourceType.valueOf(value)
    } catch (e: Exception) {
        TrackSourceType.LIVE_MIC
    }
}

@Database(entities = [ZeroNoiseItem::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class ZeroNoiseDatabase : RoomDatabase() {
    abstract fun zeroNoiseDao(): ZeroNoiseDao

    companion object {
        @Volatile
        private var INSTANCE: ZeroNoiseDatabase? = null

        fun getDatabase(context: Context): ZeroNoiseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ZeroNoiseDatabase::class.java,
                    "zero_noise_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
