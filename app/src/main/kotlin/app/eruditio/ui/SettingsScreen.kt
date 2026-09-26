package app.eruditio.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import app.eruditio.BuildConfig
import app.eruditio.ui.theme.Palettes

@Composable
internal fun SettingsScreen(vm: EruditioViewModel, modifier: Modifier) {
    val context = LocalContext.current
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        PaperHeader(
            eyebrow = "Settings / quiet defaults",
            title = "Make the quiet path yours.",
            subtitle = "Everything stays on this device.",
        )
        PaperSectionHeader("Appearance")
        PaperSurface(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val palette by vm.palette.collectAsState()
                Text("Theme", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ThemePicker(vm, palette)
            }
        }
        PaperSectionHeader("Interception")
        PaperSurface(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val selected by vm.selectedTargets.collectAsState()
                Text("Intercepted apps", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "The pause screen appears when one of these opens.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                vm.targets.forEach { app ->
                    Row(
                        Modifier.fillMaxWidth().clickable { vm.toggleTarget(app.packageId) }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = app.packageId in selected,
                            onCheckedChange = { vm.toggleTarget(app.packageId) },
                        )
                        Text(app.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                if (selected.isEmpty()) {
                    Text(
                        "No apps selected — interception is idle.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        WatcherCard()
        PaperSectionHeader("Reading")
        PaperSurface(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val railEnabled by vm.railEnabled.collectAsState()
                Text("Reading", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Margin rail", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = railEnabled, onCheckedChange = vm::setRailEnabled)
                }
                Text(
                    "A slim margin rail in the reader: tap or drag to jump, checkpoint ticks included.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        var showBugSheet by remember { mutableStateOf(false) }
        PaperSectionHeader("About")
        PaperSurface(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("About", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    Modifier.fillMaxWidth().clickable { showBugSheet = true }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Report a bug", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.tertiary)
                }
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Version", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Eruditio ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (showBugSheet) BugReportSheet(onDismiss = { showBugSheet = false })
        if (BuildConfig.DEBUG) {
            TextButton(onClick = { context.startActivity(Intent(context, DebugDueActivity::class.java)) }) {
                Text("Debug: synthetic ledger due set")
            }
        }
    }
}

@Composable
private fun ThemePicker(vm: EruditioViewModel, current: app.eruditio.ui.theme.Palette) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Theme", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Palettes.all.forEach { palette ->
                val selected = palette.id == current.id
                Surface(
                    onClick = { vm.selectTheme(palette) },
                    shape = RoundedCornerShape(50),
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Text(
                        palette.name,
                        Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun BugReportSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var text by rememberSaveable { mutableStateOf("") }
    val versionLine = "Eruditio ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    val androidLine = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Report a bug", style = MaterialTheme.typography.titleLarge)
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("What went wrong? Your own words — no logs or screenshots are attached.") },
                minLines = 5,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "$versionLine · $androidLine",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                OutlinedButton(onClick = onDismiss) { Text("Discard") }
                Button(
                    onClick = {
                        val report = "${text.trim()}\n\n---\n$versionLine · $androidLine"
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = "mailto:bugs@eruditio.app".toUri()
                            putExtra(Intent.EXTRA_SUBJECT, "Eruditio bug report")
                            putExtra(Intent.EXTRA_TEXT, report)
                        }
                        try {
                            context.startActivity(Intent.createChooser(intent, "Report a bug"))
                            onDismiss()
                        } catch (_: ActivityNotFoundException) {
                            Toast.makeText(
                                context,
                                "No mail app found — the report was not sent.",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                    enabled = text.isNotBlank(),
                ) { Text("Send") }
            }
            Text(
                "Nothing is collected, logged, or sent until you tap Send. Send hands this " +
                    "text to your mail client — your words plus the version lines, nothing else.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
