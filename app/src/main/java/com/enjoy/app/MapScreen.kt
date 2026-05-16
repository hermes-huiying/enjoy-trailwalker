package com.enjoy.app

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.Marker
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: NavHostController,
    viewModel: TrailViewModel = viewModel()
) {
    val context = LocalContext.current
    val recState by viewModel.recState.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val currentLoc by viewModel.currentLocation.collectAsStateWithLifecycle()
    val idleElev by viewModel.idleElev.collectAsStateWithLifecycle()

    var mapView by remember { mutableStateOf<MapView?>(null) }
    var trailLine by remember { mutableStateOf<Polyline?>(null) }
    var userMarker by remember { mutableStateOf<Marker?>(null) }

    // Permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            viewModel.startLocationUpdates(context)
        }
    }

    // Request permissions on first composition
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            viewModel.startLocationUpdates(context)
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopLocationUpdates()
        }
    }

    // Update map when location changes
    LaunchedEffect(currentLoc) {
        currentLoc?.let { (lat, lng) ->
            val mv = mapView ?: return@let
            val geo = GeoPoint(lat, lng)
            mv.controller.animateTo(geo)

            // Update or create user marker
            if (userMarker == null) {
                Marker(mv).also {
                    it.position = geo
                    it.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    it.icon = context.getDrawable(org.osmdroid.library.R.drawable.marker_default)
                    mv.overlays.add(it)
                    userMarker = it
                }
            } else {
                userMarker?.position = geo
            }
            mv.invalidate()
        }
    }

    // Update trail polyline when points change
    LaunchedEffect(recState.points) {
        val mv = mapView ?: return@LaunchedEffect
        if (recState.points.size >= 2) {
            val geoPts = recState.points.map { GeoPoint(it.lat, it.lng) }
            if (trailLine == null) {
                Polyline().also {
                    it.outlinePaint.color = 0xFF4CAF50.toInt()
                    it.outlinePaint.strokeWidth = 6f
                    it.setPoints(geoPts)
                    mv.overlays.add(1, it)
                    trailLine = it
                }
            } else {
                trailLine?.setPoints(geoPts)
            }
            mv.invalidate()
        }
    }

    // Fit trail bounds when recording starts
    LaunchedEffect(recState.isRecording) {
        if (recState.isRecording && recState.points.isNotEmpty()) {
            delay(500)
            val mv = mapView ?: return@LaunchedEffect
            val pts = recState.points.map { GeoPoint(it.lat, it.lng) }
            if (pts.isNotEmpty()) {
                val box = org.osmdroid.util.BoundingBox.fromGeoPoints(pts)
                mv.zoomToBoundingBox(box, true, 50)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Map
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).also { mv ->
                    mv.setTileSource(TileSourceFactory.MAPNIK)
                    mv.setMultiTouchControls(true)
                    mv.controller.setZoom(14.0)
                    if (currentLoc != null) {
                        mv.controller.setCenter(GeoPoint(currentLoc!!.first, currentLoc!!.second))
                    } else {
                        mv.controller.setCenter(GeoPoint(37.7749, -122.4194))
                    }
                    mapView = mv
                }
            }
        )

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Enjoy",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            if (!recState.isRecording && summary == null) {
                IconButton(
                    onClick = { navController.navigate("history") }
                ) {
                    Text("📋", fontSize = 20.sp)
                }
            }
        }

        // Bottom panel
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp)
                .padding(bottom = 8.dp)
        ) {
            when {
                summary != null -> SummaryPanel(
                    summary = summary!!,
                    onSave = { name -> viewModel.saveTrail(name) },
                    onDiscard = { viewModel.discardTrail() },
                    onBack = { viewModel.resetToIdle() }
                )

                recState.isRecording -> RecordingPanel(
                    recState = recState,
                    onPause = { viewModel.togglePause() },
                    onFinish = { viewModel.finishTrail() }
                )

                else -> IdlePanel(
                    idleElev = idleElev,
                    onStart = { viewModel.startRecording() }
                )
            }
        }
    }
}

@Composable
private fun IdlePanel(
    idleElev: String,
    onStart: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xEB141419))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Ready to walk 🥾",
                color = Color(0x80FFFFFF),
                fontSize = 13.sp
            )
            Text(
                text = idleElev,
                color = Color(0x59FFFFFF),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
            )
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Text("⏺  Start Trail", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RecordingPanel(
    recState: RecState,
    onPause: () -> Unit,
    onFinish: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xEB141419))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Distance",
                    value = if (recState.distance >= 1000)
                        String.format("%.2f", recState.distance / 1000)
                    else recState.distance.toInt().toString(),
                    unit = if (recState.distance >= 1000) "km" else "m"
                )
                StatItem(
                    label = "Duration",
                    value = formatTime(recState.elapsed),
                    unit = ""
                )
                StatItem(
                    label = "Elevation",
                    value = recState.currentAlt?.toInt()?.toString() ?: "—",
                    unit = "m"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onPause,
                    modifier = Modifier
                        .weight(0.35f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (recState.isPaused) Color(0xFF4CAF50) else Color(0xCCFFFFFF)
                    )
                ) {
                    Text(if (recState.isPaused) "▶" else "⏸", fontSize = 20.sp)
                }
                Button(
                    onClick = onFinish,
                    modifier = Modifier
                        .weight(0.65f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935)
                    )
                ) {
                    Text("⏹  Done", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (unit.isNotEmpty()) "$label ($unit)" else label,
            color = Color(0x73FFFFFF),
            fontSize = 10.sp
        )
    }
}

@Composable
private fun SummaryPanel(
    summary: SummaryData,
    onSave: (String) -> Unit,
    onDiscard: () -> Unit,
    onBack: () -> Unit
) {
    var showNameDialog by remember { mutableStateOf(false) }
    var trailName by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xEB141419))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "🏁 Trail Complete!",
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Summary stats grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    SummaryStat(
                        value = if (summary.distance >= 1000)
                            String.format("%.2f km", summary.distance / 1000)
                        else "${summary.distance.toInt()} m",
                        label = "Distance"
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    SummaryStat(
                        value = formatTime(summary.duration),
                        label = "Duration"
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    SummaryStat(
                        value = summary.maxElev?.toInt()?.toString() ?: "—",
                        label = "Max Elevation (m)"
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    SummaryStat(
                        value = summary.pace,
                        label = "Pace"
                    )
                }
            }

            // Elevation chart
            if (summary.points.size >= 2 && summary.points.any { it.alt != null }) {
                Spacer(modifier = Modifier.height(12.dp))
                ElevationChart(
                    points = summary.points,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x0AFFFFFF))
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showNameDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("✏️ Name", fontSize = 14.sp)
                }
                OutlinedButton(
                    onClick = onDiscard,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0x99FFFFFF))
                ) {
                    Text("🗑 Discard", fontSize = 14.sp)
                }
                Button(
                    onClick = {
                        if (trailName.isBlank()) trailName = "Unnamed Trail"
                        onSave(trailName)
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("💾 Save", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // Name dialog
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            containerColor = Color(0xFF1C1C1E),
            title = { Text("✏️ Name this trail", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Give your walk a name to remember it by.",
                        color = Color(0x80FFFFFF),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = trailName,
                        onValueChange = { trailName = it },
                        placeholder = { Text("e.g. Lands End Loop", color = Color(0x4DFFFFFF)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (trailName.isBlank()) trailName = "Unnamed Trail"
                    showNameDialog = false
                }) {
                    Text("OK", color = Color(0xFF4CAF50))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text("Cancel", color = Color(0x99FFFFFF))
                }
            }
        )
    }
}

@Composable
private fun SummaryStat(value: String, label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x0FFFFFFF))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
            Text(label, fontSize = 10.sp, color = Color(0x73FFFFFF))
        }
    }
}

private fun formatTime(ms: Long): String {
    val total = ms / 1000
    val m = total / 60
    val s = total % 60
    return String.format("%02d:%02d", m, s)
}
