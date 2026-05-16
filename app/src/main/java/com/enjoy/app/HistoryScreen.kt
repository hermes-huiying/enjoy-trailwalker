package com.enjoy.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.gson.GsonBuilder
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavHostController,
    viewModel: TrailViewModel = viewModel()
) {
    val context = LocalContext.current
    val trails by viewModel.allTrails.collectAsStateWithLifecycle(initialValue = emptyList())
    var showDeleteConfirm by remember { mutableStateOf<Trail?>(null) }

    // Export launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val gson = GsonBuilder().setPrettyPrinting().create()
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
                "exportedAt" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()),
                "trails" to exportList
            ))
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(json.toByteArray())
            }
        }
    }

    // Import launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importData(uri, context) { count ->
                // count callback handled by ViewModel
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📋 Trail History") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        if (trails.isEmpty()) return@TextButton
                        val fileName = "enjoy-trails-${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}.json"
                        exportLauncher.launch(fileName)
                    }) {
                        Text("📤", fontSize = 16.sp)
                    }
                    TextButton(onClick = {
                        importLauncher.launch(arrayOf("application/json", "*/*"))
                    }) {
                        Text("📥", fontSize = 16.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color(0xFF0A0A0A)
    ) { padding ->
        if (trails.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🥾", fontSize = 40.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No trails saved yet.",
                        color = Color(0x4DFFFFFF),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Start your first walk!",
                        color = Color(0x4DFFFFFF),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(trails, key = { it.id }) { trail ->
                    TrailHistoryItem(
                        trail = trail,
                        onClick = {
                            navController.previousBackStackEntry?.savedStateHandle?.set("viewTrail", trail.id)
                            navController.popBackStack()
                        },
                        onDelete = { showDeleteConfirm = trail }
                    )
                }
            }
        }
    }

    // Delete confirmation
    showDeleteConfirm?.let { trail ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            containerColor = Color(0xFF1C1C1E),
            title = { Text("Delete trail?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to delete \"${trail.name}\"?",
                    color = Color(0x80FFFFFF)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTrail(trail)
                    showDeleteConfirm = null
                }) {
                    Text("Delete", color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Cancel", color = Color(0x99FFFFFF))
                }
            }
        )
    }
}

@Composable
private fun TrailHistoryItem(
    trail: Trail,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x0FFFFFFF))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1B5E20)),
            contentAlignment = Alignment.Center
        ) {
            Text("🥾", fontSize = 18.sp)
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = trail.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${formatDate(trail.date)} · ${formatTimeShort(trail.duration)}",
                color = Color(0x66FFFFFF),
                fontSize = 11.sp
            )
        }

        Text(
            text = if (trail.distance >= 1000)
                String.format("%.2f km", trail.distance / 1000)
            else "${trail.distance.toInt()} m",
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4CAF50),
            fontSize = 14.sp
        )

        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Text("🗑", fontSize = 14.sp)
        }
    }
}

private fun formatDate(iso: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        val d = sdf.parse(iso)
        SimpleDateFormat("MMM d", Locale.US).format(d!!)
    } catch (e: Exception) {
        iso.take(10)
    }
}

private fun formatTimeShort(ms: Long): String {
    val total = ms / 1000
    val m = total / 60
    val s = total % 60
    return String.format("%02d:%02d", m, s)
}
