package app.eruditio.ui

import android.content.Intent
import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import app.eruditio.BuildConfig
import app.eruditio.core.RecallGrade
import app.eruditio.ui.theme.EruditioTheme
import app.eruditio.ui.theme.Palettes

/**
 * The app shell (wireframe variant A "Paper", refined per direction): pinned
 * bottom navigation that never leaves the screen, quiet typographic surfaces,
 * percent rings from variant C, compact dense library rows from variant B.
 * Reader is its own quiet activity; the pause overlay lives in intercept/.
 */
@Composable
fun App(vm: EruditioViewModel) {
    val palette by vm.palette.collectAsState()
    EruditioTheme(palette) {
        var tab by rememberSaveable { mutableStateOf(0) }
        LaunchedEffect(Unit) { vm.refresh() }
        // Checkpoint pause (ticket 14): the reader sets this while this activity
        // was stopped; returning recomposes here and lands on Home/Today
        // instead of the tab the user left.
        if (vm.pauseToHome) {
            LaunchedEffect(Unit) { tab = 0; vm.consumePauseToHome() }
        }
        val homeState by vm.home.collectAsState()
        val reviewState by vm.review.collectAsState()
        val progressState by vm.progress.collectAsState()

        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Column(
                Modifier.weight(1f).fillMaxWidth(),
            ) {
                when (tab) {
                    0 -> HomeScreen(vm, homeState, Modifier.fillMaxSize(), onOpenLibrary = { tab = 1 }, onReview = { tab = 2 })
                    1 -> LibraryScreen(vm, Modifier.fillMaxSize())
                    2 -> ReviewScreen(vm, reviewState, Modifier.fillMaxSize())
                    3 -> ProgressScreen(progressState, Modifier.fillMaxSize(), onOpenLibrary = { tab = 1 })
                    4 -> SettingsScreen(vm, Modifier.fillMaxSize())
                }
            }
            BottomNav(tab) { tab = it; vm.refresh() }
        }
    }
}

/** Bottom navigation: always visible, anchored to the bottom edge. */
@Composable
private fun BottomNav(selected: Int, onSelect: (Int) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
            Row(
                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                listOf(
                    Triple(0, "⌂", "Today"),
                    Triple(1, "☰", "Library"),
                    Triple(2, "↺", "Review"),
                    Triple(3, "◉", "Progress"),
                    Triple(4, "⚙", "Settings"),
                ).forEach { (index, glyph, label) ->
                    val color =
                        if (index == selected) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    Column(
                        modifier = Modifier.clickable { onSelect(index) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(glyph, color = color, style = MaterialTheme.typography.titleLarge)
                        Text(label, color = color, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
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
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Eruditio", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Today", style = MaterialTheme.typography.headlineMedium)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Sessions this week", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${state.sessionsThisWeek}", style = MaterialTheme.typography.headlineSmall)
                }
            }
            Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Reading today", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(fmtMinutes(state.minutesToday), style = MaterialTheme.typography.headlineSmall)
                }
            }
        }

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Reviews due today", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${state.due} cards", style = MaterialTheme.typography.headlineLarge)
                if (state.due > 0) {
                    Button(
                        onClick = onReview,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) { Text("Begin review") }
                }
            }
        }

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (state.recent.isEmpty()) {
                    Text("Begin here", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val progressById by vm.progressById.collectAsState()
                    val unstarted = vm.texts.filter { (progressById[it.id] ?: 0) == 0 }.take(3)
                    if (unstarted.isEmpty()) {
                        Text("All texts have progress — pick up any classic from the Library.", style = MaterialTheme.typography.bodyMedium)
                    }
                    unstarted.forEach { t ->
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.startReading(t.id) }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(t.title, style = MaterialTheme.typography.bodyLarge)
                                Text(t.author, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                "start",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    Text("Recent sessions", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    state.recent.forEach { s ->
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.startReading(s.textId) }.padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(vm.titleOf(s.textId), style = MaterialTheme.typography.bodyLarge)
                            Text("resume", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        if (gateLocked) {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Your reading trial is complete", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Buy Eruditio once — it is yours. No subscription, ever.", style = MaterialTheme.typography.bodyMedium)
                    Button(
                        onClick = { activity?.let { vm.purchase(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) { Text("Unlock Eruditio") }
                    TextButton(onClick = { vm.restorePurchase() }) { Text("Restore purchase") }
                    gateError?.let { message ->
                        Text(
                            message,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Interception", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val okUsage = state.hasUsageAccess
                val okOverlay = state.canDrawOverlays
                if (okUsage && okOverlay) {
                    Text(
                        "Interception is on. Open a target app to meet the pause screen.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    if (!okUsage) {
                        Text(
                            "Grant app usage access so Eruditio can notice your scroll apps.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        OutlinedButton(
                            onClick = { context.startActivity(Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Grant usage access") }
                    }
                    if (!okOverlay) {
                        Text(
                            "Allow Eruditio to draw over other apps — that's the pause screen.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        OutlinedButton(
                            onClick = {
                                context.startActivity(
                                    Intent(
                                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        "package:${context.packageName}".toUri(),
                                    ),
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Allow overlay") }
                    }
                }
            }
        }

        OutlinedButton(onClick = onOpenLibrary, modifier = Modifier.fillMaxWidth()) { Text("Open library") }
    }
}

/**
 * Library, prototype variant B (owner verdict): segmented group bar replaces
 * section headers, a search field hides behind a tap, and a progress pill
 * cycles any / in progress / unread. The list is always flat with a match
 * count that reflects all active filters composed. Rows keep the reader flow.
 */
@Composable
private fun LibraryScreen(vm: EruditioViewModel, modifier: Modifier) {
    val progressById by vm.progressById.collectAsState()
    val segments = listOf(
        "All" to null,
        "Classics" to "ancient",
        "Christian" to "christian",
        "Lives" to "biography",
    )
    var segment by rememberSaveable { mutableStateOf(0) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val progressModes = listOf("any progress", "in progress", "unread")
    var progressMode by rememberSaveable { mutableStateOf(0) }

    val filtered = vm.texts.filter { t ->
        val (_, group) = segments[segment]
        (group == null || t.group == group) &&
            (query.isBlank() ||
                t.title.contains(query, ignoreCase = true) ||
                t.author.contains(query, ignoreCase = true)) &&
            when (progressMode) {
                1 -> (progressById[t.id] ?: 0) in 1..99
                2 -> (progressById[t.id] ?: 0) == 0
                else -> true
            }
    }

    Column(
        modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
    ) {
        Text("Library", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 16.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                progressModes[progressMode],
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clickable { progressMode = (progressMode + 1) % progressModes.size },
            )
            if (searching) {
                androidx.compose.material3.TextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("Search title or author") },
                    trailingIcon = {
                        Text(
                            "✕",
                            modifier = Modifier.clickable { query = ""; searching = false },
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
            } else {
                Text(
                    "Search",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { searching = true },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            segments.forEachIndexed { index, (label, _) ->
                val selected = index == segment
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(999.dp),
                        )
                        .clickable { segment = index }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
        Text(
            "${filtered.size} of ${vm.texts.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
        Spacer(Modifier.height(4.dp))
        filtered.forEach { t ->
            val pct = progressById[t.id] ?: 0
            Row(
                Modifier.fillMaxWidth().clickable { vm.startReading(t.id) }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(t.title, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        t.author,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                when {
                    pct == 100 -> Text(
                        "read",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    pct > 0 -> Ring(pct / 100f)
                    else -> Text(
                        "unread",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun ReviewScreen(vm: EruditioViewModel, state: EruditioViewModel.ReviewState, modifier: Modifier) {
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Review", style = MaterialTheme.typography.headlineMedium)
        val card = state.current
        if (card == null) {
            Text("Nothing due. Read a segment from the Library and new cards arrive tomorrow.")
        } else {
            Text(
                "Card ${state.index + 1} of ${state.total}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        vm.titleOfConcept(card.conceptId),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(state.cue ?: card.conceptId, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Recall it before you grade.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(RecallGrade.AGAIN, RecallGrade.HARD, RecallGrade.GOOD, RecallGrade.EASY).forEach { grade ->
                    OutlinedButton(onClick = { vm.grade(grade) }, modifier = Modifier.weight(1f)) {
                        Text(grade.name.lowercase())
                    }
                }
            }
        }
    }
}

private fun fmtMinutes(minutes: Long): String =
    when {
        minutes >= 60 -> "${minutes / 60}h ${minutes % 60}m"
        else -> "${minutes}m"
    }

@Composable
private fun ProgressScreen(
    state: EruditioViewModel.ProgressState,
    modifier: Modifier,
    onOpenLibrary: () -> Unit,
) {
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Progress", style = MaterialTheme.typography.headlineMedium)
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Concepts by mastery", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("familiar ${state.familiar} · proficient ${state.proficient} · mastered ${state.mastered}")
                Text("Reviews due: ${state.dueCount}")
                Text("Texts started: ${state.textsInProgress} · read through: ${state.textsReadThrough}")
            }
        }
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("This week", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Openings intercepted: ${state.weekly}")
                Text("Readings that replaced them: ${state.replaced}")
                Text(
                    "Time reading: ${fmtMinutes(state.minutesReadingWeek)} this week · ${fmtMinutes(state.minutesReadingTotal)} all time",
                )
            }
        }
        Text("🔥 Streak: ${state.streak} day(s) — habit, not mastery", style = MaterialTheme.typography.bodyLarge)
        OutlinedButton(onClick = onOpenLibrary, modifier = Modifier.fillMaxWidth()) { Text("Open Library") }
    }
}

/** Settings: theme, intercepted apps, debug surfaces. */
@Composable
private fun SettingsScreen(vm: EruditioViewModel, modifier: Modifier) {
    val context = LocalContext.current
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val palette by vm.palette.collectAsState()
                Text("Theme", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ThemePicker(vm, palette)
            }
        }
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
        if (BuildConfig.DEBUG) {
            TextButton(onClick = { context.startActivity(Intent(context, DebugDueActivity::class.java)) }) {
                Text("Debug: synthetic ledger due set")
            }
        }
    }
}

/** Theme picker: curated named palettes, Omarchy-style. */
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
