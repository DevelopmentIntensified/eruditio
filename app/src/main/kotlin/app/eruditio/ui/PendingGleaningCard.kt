package app.eruditio.ui

import android.content.Context
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The durable hand-off surface: a completed session whose reflection has not
 * reached Keep yet — the reader died, crashed, or backed out — reappears here
 * until they finish it or explicitly discard it.
 */
@Composable
internal fun PendingGleaningCard(
    context: Context,
    sessionId: Long,
    modifier: Modifier = Modifier,
) {
    PaperSectionHeader("Waiting for you", detail = "one gleaning")
    PaperSurface(modifier.fillMaxWidth()) {
        Text(
            "A reading is ready for your reflection.",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Pick up where you left off and keep one idea worth carrying.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { context.startActivity(GleaningActivity.intent(context, sessionId)) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Continue gleaning →") }
    }
}
