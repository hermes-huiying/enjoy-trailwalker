package com.enjoy.app

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.location.Location
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

data class RecState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val points: List<TrailPoint> = emptyList(),
    val startTime: Long = 0L,
    val currentLatLng: Pair<Double, Double>? = null,
    val currentAlt: Double? = null,
    val distance: Double = 0.0,
    val elapsed: Long = 0L,
    val maxElev: Double? = null,
    val minElev: Double? = null,
)

data class SummaryData(
    val name: String = "Unnamed Trail",
    val distance: Double = 0.0,
    val duration: Long = 0L,
    val maxElev: Double? = null,
    val minElev: Double? = null,
    val pace: String = "—",
    val points: List<TrailPoint> = emptyList()
)

class TrailViewModel(application: Application) : AndroidViewModel(application) {

    private val db = TrailDatabase.getInstance(application)
    private val dao = db.trailDao()
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)
    private val gson = Gson()

    val allTrails = dao.getAllTrails()

    private val _recState = MutableStateFlow(RecState())
    val recState: StateFlow<RecState> = _recState.asStateFlow()

    private val _summary = MutableStateFlow<SummaryData?>(null)
    val summary: StateFlow<SummaryData?> = _summary.asStateFlow()

    private val _currentLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val currentLocation: StateFlow<Pair<Double, Double>?> = _currentLocation.asStateFlow()

    private val _idleElev = MutableStateFlow("Elevation: —")
    val idleElev: StateFlow<String> = _idleElev.asStateFlow()

    private var locationCallback: com.google.android.gms.location.LocationCallback? = null
    private var lastInputTime = 0L
    private var hasFirstFix = false

    fun startLocationUpdates(context: Context) {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
            .setMinUpdateIntervalMillis(1000)
            .build()

        locationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                val loc = result.lastLocation ?: return
                val latlng = loc.latitude to loc.longitude
                val alt = if (loc.hasAltitude()) loc.altitude else null
                _currentLocation.value = latlng

                if (!_recState.value.isRecording) {
                    _idleElev.value = if (alt != null) "Elevation: ${alt.toInt()} m" else "Elevation: —"
                    return
                }

                if (_recState.value.isPaused) return

                // Debounce - max 1 point per second
                val now = System.currentTimeMillis()
                if (now - lastInputTime < 800) return
                lastInputTime = now

                val pt = TrailPoint(loc.latitude, loc.longitude, alt, now)
                val newPts = _recState.value.points + pt
                val newDist = if (newPts.size >= 2) {
                    val prev = newPts[newPts.size - 2]
                    _recState.value.distance + haversine(prev.lat, prev.lng, pt.lat, pt.lng)
                } else _recState.value.distance

                var maxE = _recState.value.maxElev
                var minE = _recState.value.minElev
                if (alt != null) {
                    if (maxE == null || alt > maxE) maxE = alt
                    if (minE == null || alt < minE) minE = alt
                }

                _recState.value = _recState.value.copy(
                    points = newPts,
                    distance = newDist,
                    currentLatLng = latlng,
                    currentAlt = alt,
                    maxElev = maxE,
                    minElev = minE,
                    elapsed = now - _recState.value.startTime
                )
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(request, locationCallback!!, null)
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    fun stopLocationUpdates() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        locationCallback = null
    }

    fun startRecording() {
        val now = System.currentTimeMillis()
        lastInputTime = now
        _recState.value = RecState(
            isRecording = true,
            startTime = now
        )
        _summary.value = null
    }

    fun togglePause() {
        _recState.value = _recState.value.copy(
            isPaused = !_recState.value.isPaused
        )
    }

    fun finishTrail() {
        val state = _recState.value
        if (state.points.size < 2) {
            resetToIdle()
            return
        }
        val pts = state.points
        val elapsed = state.elapsed
        val dist = state.distance
        val pace = if (dist < 10) "—" else {
            val minPerKm = (elapsed / 60000.0) / (dist / 1000.0)
            "${minPerKm.toInt()}'${((minPerKm % 1) * 60).toInt()}\"/km"
        }

        val elevValues = pts.mapNotNull { it.alt }
        _summary.value = SummaryData(
            distance = dist,
            duration = elapsed,
            maxElev = if (elevValues.isNotEmpty()) elevValues.max() else null,
            minElev = if (elevValues.isNotEmpty()) elevValues.min() else null,
            pace = pace,
            points = pts
        )

        _recState.value = RecState()
    }

    fun saveTrail(name: String) {
        val s = _summary.value ?: return
        viewModelScope.launch {
            dao.insertTrail(Trail(
                name = name,
                points = s.points,
                distance = s.distance,
                duration = s.duration,
                maxElev = s.maxElev,
                minElev = s.minElev,
                pace = s.pace
            ))
            resetToIdle()
        }
    }

    fun discardTrail() {
        _summary.value = null
        resetToIdle()
    }

    fun resetToIdle() {
        _recState.value = RecState()
        _summary.value = null
    }

    fun deleteTrail(trail: Trail) {
        viewModelScope.launch { dao.deleteTrail(trail) }
    }

    fun getTrailById(id: Long, callback: (Trail?) -> Unit) {
        viewModelScope.launch {
            val trails = dao.getAllTrailsList()
            callback(trails.find { it.id == id })
        }
    }

    // Export via callback pattern
    fun exportData(context: Context, callback: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val trails = dao.getAllTrailsList()
                if (trails.isEmpty()) {
                    callback(null)
                    return@launch
                }

                val exportList = trails.map { t ->
                    mapOf(
                        "name" to t.name,
                        "date" to t.date,
                        "points" to t.points.map { p ->
                            mapOf("lat" to p.lat, "lng" to p.lng, "alt" to p.alt, "time" to p.time)
                        },
                        "distance" to t.distance,
                        "duration" to t.duration,
                        "maxElev" to t.maxElev,
                        "minElev" to t.minElev,
                        "pace" to t.pace
                    )
                }

                val json = gson.toJson(mapOf(
                    "version" to 1,
                    "exportedAt" to java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date()),
                    "trails" to exportList
                ))

                val fileName = "enjoy-trails-${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())}.json"
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                val file = java.io.File(downloadsDir, fileName)
                file.writeText(json)
                callback(file.absolutePath)
            } catch (e: Exception) {
                callback(null)
            }
        }
    }

    fun importData(uri: android.net.Uri, context: Context, callback: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                val reader = BufferedReader(InputStreamReader(inputStream))
                val text = reader.readText()
                reader.close()

                val data = gson.fromJson(text, Map::class.java)
                val rawTrails = data["trails"]
                if (rawTrails !is List<*>) {
                    callback(-1)
                    return@launch
                }

                var count = 0
                for (item in rawTrails) {
                    if (item !is Map<*, *>) continue
                    try {
                        @Suppress("UNCHECKED_CAST")
                        val ptsRaw = item["points"] as? List<Map<String, Any>>
                        if (ptsRaw == null || ptsRaw.size < 2) continue

                        val pts = ptsRaw.map { p ->
                            TrailPoint(
                                lat = (p["lat"] as Number).toDouble(),
                                lng = (p["lng"] as Number).toDouble(),
                                alt = (p["alt"] as? Number)?.toDouble(),
                                time = (p["time"] as Number).toLong()
                            )
                        }

                        dao.insertTrail(
                            Trail(
                                name = item["name"] as? String ?: "Imported Trail",
                                date = item["date"] as? String ?: java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date()),
                                points = pts,
                                distance = (item["distance"] as? Number)?.toDouble() ?: 0.0,
                                duration = (item["duration"] as? Number)?.toLong() ?: 0L,
                                maxElev = (item["maxElev"] as? Number)?.toDouble(),
                                minElev = (item["minElev"] as? Number)?.toDouble(),
                                pace = item["pace"] as? String ?: "—"
                            )
                        )
                        count++
                    } catch (_: Exception) {
                        continue
                    }
                }
                callback(count)
            } catch (e: Exception) {
                callback(-1)
            }
        }
    }

    private fun updateIdleState() {
        // Placeholder for future use
    }

    companion object {
        fun haversine(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
            val R = 6371000.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLng = Math.toRadians(lng2 - lng1)
            val a = Math.sin(dLat / 2).pow(2) +
                    Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                    Math.sin(dLng / 2).pow(2)
            return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        }
    }
}

private fun Double.pow(n: Int): Double = Math.pow(this, n.toDouble())
