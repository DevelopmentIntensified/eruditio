package app.eruditio.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.eruditio.intercept.InterceptionFault
import app.eruditio.intercept.WatcherRuntime
import kotlinx.coroutines.delay

private const val REFRESH_MILLIS = 10_000L

/**
 * One honest line about the watcher, with the fix only the user can give
 * (ticket 24). A silent watcher is the worst outcome: the pause screen simply
 * never appears and nothing says why.
 */
@Composable
internal fun WatcherCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var status by remember { mutableStateOf(WatcherRuntime.status(context)) }
    var startRequested by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            val fresh = WatcherRuntime.status(context)
            status = fresh
            if (startRequested && fresh.fault != InterceptionFault.NOT_RUNNING) startRequested = false
            delay(REFRESH_MILLIS)
        }
    }

    PaperSectionHeader("Watcher", detail = "always on")
    PaperSurface(modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(headline(status.fault, startRequested), style = MaterialTheme.typography.titleMedium)
            Text(
                detailFor(status.fault),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (status.fault == InterceptionFault.NO_USAGE_ACCESS) {
                OutlinedButton(
                    onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Grant usage access") }
            }
            if (status.fault == InterceptionFault.NOT_RUNNING) {
                OutlinedButton(
                    onClick = {
                        startRequested = true
                        WatcherRuntime.arm(context)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (startRequested) "Starting the watcher…" else "Start the watcher now") }
            }
            if (status.batteryOptimized) {
                Text(
                    "Android may doze Eruditio in the background. Exempt it so the pause screen " +
                        "keeps meeting you.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // The settings list, not the direct request dialog: Play restricts
                // REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, so we never declare it.
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Battery settings") }
            }
        }
    }
}

private fun headline(fault: InterceptionFault, startRequested: Boolean): String = when (fault) {
    InterceptionFault.NONE -> "Watching."
    InterceptionFault.NO_USAGE_ACCESS -> "Usage access is off."
    InterceptionFault.NOT_RUNNING ->
        if (startRequested) "Starting the watcher…" else "Not watching."
    InterceptionFault.NO_TARGETS -> "No apps selected."
}

private fun detailFor(fault: InterceptionFault): String = when (fault) {
    InterceptionFault.NONE ->
        "Eruditio is on its own now — the pause screen appears whenever you open an intercepted app."
    InterceptionFault.NO_USAGE_ACCESS ->
        "Without usage access Eruditio cannot see an app open, so nothing is intercepted."
    InterceptionFault.NOT_RUNNING ->
        "The watcher is not running. It re-arms after a reboot and every 15 minutes; " +
            "Android can refuse the restart until you open Eruditio or exempt it below."
    InterceptionFault.NO_TARGETS -> "Choose the apps to intercept above and the pause screen will arm."
}
