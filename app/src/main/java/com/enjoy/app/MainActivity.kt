package com.enjoy.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configure osmdroid
        Configuration.getInstance().apply {
            osmdroidBasePath = filesDir
            osmdroidTileCache = cacheDir
            userAgentValue = packageName
        }

        setContent {
            EnjoyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EnjoyApp()
                }
            }
        }
    }
}

private val DarkColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF4CAF50),
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = androidx.compose.ui.graphics.Color(0xFF1B5E20),
    onSecondary = androidx.compose.ui.graphics.Color.White,
    surface = androidx.compose.ui.graphics.Color(0xFF0A0A0A),
    onSurface = androidx.compose.ui.graphics.Color(0xFFE8E8E8),
    background = androidx.compose.ui.graphics.Color(0xFF0A0A0A),
    onBackground = androidx.compose.ui.graphics.Color(0xFFE8E8E8),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF1C1C1E),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFA0A0A0),
    error = androidx.compose.ui.graphics.Color(0xFFE53935),
)

@androidx.compose.runtime.Composable
fun EnjoyTheme(content: @androidx.compose.runtime.Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
