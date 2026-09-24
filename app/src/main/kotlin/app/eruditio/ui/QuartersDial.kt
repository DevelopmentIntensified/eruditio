package app.eruditio.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Quarters dial (prototype verdict A, 2026-09-24): outline circle, four tick
 * marks at the quadrants, progress arc starting at 12 o'clock, and a gold
 * position notch at the current angle (hidden at 0% and 100%).
 */
@Composable
fun QuartersDial(progress: Float, size: Dp = 44.dp, modifier: Modifier = Modifier) {
    val circle = MaterialTheme.colorScheme.outline
    val tick = MaterialTheme.colorScheme.onSurfaceVariant
    val arc = MaterialTheme.colorScheme.primary
    val notch = MaterialTheme.colorScheme.tertiary
    val pct = progress.coerceIn(0f, 1f)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(modifier.size(size)) {
            val r = size.toPx() / 2f
            val center = Offset(r, r)
            fun point(angleDeg: Float, radius: Float) = Offset(
                center.x + radius * cos(Math.toRadians(angleDeg.toDouble())).toFloat(),
                center.y + radius * sin(Math.toRadians(angleDeg.toDouble())).toFloat(),
            )
            drawCircle(color = circle, radius = r * 19f / 23f, center = center, style = Stroke(width = r * 0.065f))
            for (quarter in 0 until 4) {
                val angle = -90f + quarter * 90f
                drawLine(
                    color = tick,
                    start = point(angle, r * 15f / 23f),
                    end = point(angle, r * 18f / 23f),
                    strokeWidth = r * 0.043f,
                )
            }
            if (pct > 0f) {
                drawArc(
                    color = arc,
                    startAngle = -90f,
                    sweepAngle = min(pct * 360f, 359.9f),
                    useCenter = false,
                    topLeft = Offset(r - r * 17f / 23f, r - r * 17f / 23f),
                    size = Size(2f * r * 17f / 23f, 2f * r * 17f / 23f),
                    style = Stroke(width = r * 0.109f, cap = StrokeCap.Round),
                )
            }
            if (pct > 0f && pct < 1f) {
                drawCircle(color = notch, radius = r * 0.113f, center = point((pct * 3.6f * 100f) - 90f, r * 13f / 23f))
            }
        }
        Text(
            when {
                pct > 0f && pct < 1f -> "${(pct * 100).toInt()}%"
                pct == 1f -> "read"
                else -> "unread"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
