package app.eruditio.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Percent ring (from wireframe variant C), restyled per palette. */
@Composable
fun Ring(progress: Float, size: Dp = 44.dp, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.outline
    val onAccent = MaterialTheme.colorScheme.onBackground
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier.size(size)) {
            val stroke = Stroke(width = size.toPx() * 0.075f, cap = StrokeCap.Round)
            val inset = stroke.width / 2
            drawArc(
                color = track,
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(this.size.width - 2 * inset, this.size.height - 2 * inset),
                style = stroke,
            )
            drawArc(
                color = accent,
                startAngle = -90f, sweepAngle = 360f * progress.coerceIn(0f, 1f), useCenter = false,
                topLeft = Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(this.size.width - 2 * inset, this.size.height - 2 * inset),
                style = stroke,
            )
        }
        Text(
            "${(progress.coerceIn(0f, 1f) * 100).roundToInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
    }
}
