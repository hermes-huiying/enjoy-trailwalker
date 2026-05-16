package com.enjoy.app

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "trails")
@TypeConverters(Converters::class)
data class Trail(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "Unnamed Trail",
    val date: String = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date()),
    val points: List<TrailPoint>,
    val distance: Double = 0.0,
    val duration: Long = 0L,
    val maxElev: Double? = null,
    val minElev: Double? = null,
    val pace: String = "—"
)

data class TrailPoint(
    val lat: Double,
    val lng: Double,
    val alt: Double?,
    val time: Long
)

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromPointList(value: List<TrailPoint>): String = gson.toJson(value)

    @TypeConverter
    fun toPointList(value: String): List<TrailPoint> {
        val type = object : TypeToken<List<TrailPoint>>() {}.type
        return gson.fromJson(value, type)
    }
}
