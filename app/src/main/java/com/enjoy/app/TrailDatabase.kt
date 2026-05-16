package com.enjoy.app

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow

@Database(entities = [Trail::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class TrailDatabase : RoomDatabase() {
    abstract fun trailDao(): TrailDao

    companion object {
        @Volatile
        private var INSTANCE: TrailDatabase? = null

        fun getInstance(context: Context): TrailDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TrailDatabase::class.java,
                    "enjoy_trails.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

@androidx.room.Dao
interface TrailDao {
    @androidx.room.Query("SELECT * FROM trails ORDER BY date DESC")
    fun getAllTrails(): Flow<List<Trail>>

    @androidx.room.Query("SELECT * FROM trails ORDER BY date DESC")
    suspend fun getAllTrailsList(): List<Trail>

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertTrail(trail: Trail): Long

    @androidx.room.Delete
    suspend fun deleteTrail(trail: Trail)

    @androidx.room.Query("DELETE FROM trails")
    suspend fun deleteAll()
}
