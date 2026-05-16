package com.enjoy.app

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun ElevationChart(
    points: List<TrailPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF4CAF50),
    fillColor: Color = Color(0x334CAF50)
) {
    val alts = points.mapNotNull { it.alt }
    if (alts.size < 2) return

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minA = alts.min().toFloat()
        val maxA = alts.max().toFloat()
        val range = (maxA - minA).coerceAtLeast(5f)
        val pad = 8f

        val indices = points.indices.filter { points[it].alt != null }
        if (indices.isEmpty()) return@Canvas

        val path = Path()
        var started = false

        for (i in indices) {
            val x = pad + (i.toFloat() / (points.size - 1).coerceAtLeast(1)) * (w - pad * 2)
            val y = h - pad - ((points[i].alt!!.toFloat() - minA) / range) * (h - pad * 2)
            if (!started) {
                path.moveTo(x, y)
                started = true
            } else {
                path.lineTo(x, y)
            }
        }

        // Fill area
        val fillPath = Path()
        fillPath.addPath(path)
        val lastIdx = indices.last()
        val lastX = pad + (lastIdx.toFloat() / (points.size - 1).coerceAtLeast(1)) * (w - pad * 2)
        fillPath.lineTo(lastX, h - pad)
        fillPath.lineTo(pad, h - pad)
        fillPath.close()
        drawPath(fillPath, fillColor)

        // Line
        drawPath(path, lineColor, style = Stroke(width = 3f))
    }
}
