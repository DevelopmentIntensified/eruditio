package app.eruditio.ui

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri

/** Variant A: an editorial reading desk with Resume as the first action. */
@Composable
internal fun EditorialHomeScreen(
    vm: EruditioViewModel,
    state: EruditioViewModel.HomeState,
    modifier: Modifier,
    onOpenLibrary: () -> Unit,
    onReview: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val gateLocked by vm.gateLocked.collectAsState()
    val gateError by vm.gateError.collectAsState()
    val progressById by vm.progressById.collectAsState()
    val unstarted = vm.texts.filter { isUnstarted(progressById[it.id] ?: 0) }
    val inProgress = vm.texts.filter {
        val percent = progressById[it.id] ?: 0
        isStarted(percent) && !isReadThrough(percent)
    }
    val resumeMeta = state.resumeTextId?.let { id -> vm.texts.firstOrNull { it.id == id } }
    val resumeId = resumeMeta?.id?.takeIf { !isReadThrough(progressById[it] ?: 0) }
        ?: inProgress.firstOrNull()?.id
        ?: unstarted.firstOrNull()?.id
        ?: vm.texts.firstOrNull()?.id
    val resumePercent = progressById[resumeId] ?: 0
    val shelf = (unstarted + inProgress)
        .distinctBy { it.id }
        .filter { it.id != resumeId }
        .take(3)

    Column(
        modifier.verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        PaperHeader(
            eyebrow = "Eruditio / reading desk",
            title = "Return to the page.",
            subtitle = "One considered session is enough for today.",
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "${state.sessionsThisWeek} sessions this week",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "${fmtMinutes(state.minutesToday)} read today",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (resumeMeta != null) {
            PaperSectionHeader("Resume session", detail = if (resumePercent > 0) "$resumePercent% read" else "new session")
            PaperSurface(Modifier.fillMaxWidth()) {
                Text(
                    if (resumePercent > 0) "RESUME / READING ANCHOR" else "BEGIN / NEW SESSION",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(resumeMeta.title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    resumeMeta.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                PaperProgress(resumePercent, Modifier.fillMaxWidth())
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = { vm.startReading(resumeMeta.id) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (resumePercent > 0) "Resume session →" else "Start reading →") }
                TextButton(
                    onClick = onOpenLibrary,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Choose another text") }
            }
        }

        PaperSectionHeader(
            if (state.reviewIsChallenge) "Mastery challenge" else "Review queue",
            detail = if (state.due > 0) "${state.due} review items" else "clear",
        )
        PaperSurface(Modifier.fillMaxWidth()) {
            if (state.due > 0) {
                Text(
                    if (state.reviewIsChallenge) "Nothing due. Keep the material honest." else "${state.due} review items",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    if (state.reviewIsChallenge) {
                        "Aging material is ready to surface before it disappears."
                    } else {
                        "A small return keeps the reading alive."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                state.reviewItems.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text("•", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.tertiary)
                        Column(Modifier.weight(1f)) {
                            Text(vm.titleOfConcept(item.conceptId), style = MaterialTheme.typography.titleMedium)
                            Text(
                                item.cue ?: "Return point",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                item.mastery.name.lowercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (state.due > state.reviewItems.size) {
                    Text(
                        "${state.due - state.reviewItems.size} more review items",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = onReview, modifier = Modifier.fillMaxWidth()) { Text("Begin review →") }
            } else {
                Text("Nothing due today.", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Read a segment and new review items will appear tomorrow.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        PaperSectionHeader("From the shelf", detail = "${vm.texts.size} bundled")
        if (shelf.isEmpty()) {
            Text(
                "Every text has progress — choose a classic from the Library.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            shelf.forEach { text ->
                val percent = progressById[text.id] ?: 0
                HomeTextRow(
                    title = text.title,
                    meta = "${text.author} · ${if (percent > 0) "$percent% read" else "not started"}",
                    action = if (percent > 0) "resume" else "start",
                ) { vm.startReading(text.id) }
            }
        }

        if (state.recent.isNotEmpty()) {
            PaperSectionHeader("Recent sessions", detail = "last ${state.recent.size}")
            state.recent.take(3).forEach { session ->
                HomeTextRow(vm.titleOf(session.textId), "completed session", "open again") {
                    vm.startReading(session.textId)
                }
            }
        }

        if (gateLocked) {
            PaperSectionHeader("Reading trial")
            PaperSurface(Modifier.fillMaxWidth()) {
                Text("Your reading trial is complete", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Buy Eruditio once — it is yours. No subscription, ever.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { activity?.let { vm.purchase(it) } },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Unlock Eruditio") }
                TextButton(onClick = vm::restorePurchase, modifier = Modifier.fillMaxWidth()) {
                    Text("Restore purchase")
                }
                gateError?.let { message ->
                    Text(message, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        PaperSectionHeader("Interception")
        PaperSurface(Modifier.fillMaxWidth()) {
            val okUsage = state.hasUsageAccess
            val okOverlay = state.canDrawOverlays
            if (okUsage && okOverlay) {
                Text("Interception is on.", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Open a target app to meet the pause screen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                if (!okUsage) {
                    Text("Grant app usage access so Eruditio can notice your scroll apps.")
                    OutlinedButton(
                        onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Grant usage access") }
                }
                if (!okOverlay) {
                    Text("Allow Eruditio to draw over other apps — that's the pause screen.")
                    OutlinedButton(
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    "package:${context.packageName}".toUri(),
                                ),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Allow overlay") }
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = 20.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline,
        )
        OutlinedButton(onClick = onOpenLibrary, modifier = Modifier.fillMaxWidth()) { Text("Open library →") }
    }
}

@Composable
private fun HomeTextRow(
    title: String,
    meta: String,
    action: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                meta,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(action, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
}
