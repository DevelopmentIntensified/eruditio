package app.eruditio.ui

import android.content.Intent
import android.content.ActivityNotFoundException
import android.os.Build
import android.widget.Toast
import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import app.eruditio.BuildConfig
import app.eruditio.core.Groups
import app.eruditio.core.RecallGrade
import app.eruditio.core.WeekDot
import app.eruditio.ui.theme.EruditioTheme
import app.eruditio.ui.theme.Palettes
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

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

/** Library segment bar: content groups, cycled by tap (variant B). */
private enum class Segment(val label: String, val group: String?) {
    All("All", null),
    Classics("Classics", Groups.ANCIENT),
    Christian("Christian", Groups.CHRISTIAN),
    Lives("Lives", Groups.BIOGRAPHY),
}

/** Progress pill modes: read percent (0..100) decides inclusion. */
private enum class ProgressMode(val label: String, val matches: (Int) -> Boolean) {
    Any("any progress", { _ -> true }),
    InProgress("in progress", { pct -> pct in 1..99 }),
    Unread("unread", { pct -> pct == 0 }),
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
    var segment by rememberSaveable { mutableStateOf(Segment.All) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var progressMode by rememberSaveable { mutableStateOf(ProgressMode.Any) }

    val filtered = vm.texts.filter { t ->
        (segment.group == null || t.group == segment.group) &&
            (query.isBlank() ||
                t.title.contains(query, ignoreCase = true) ||
                t.author.contains(query, ignoreCase = true)) &&
            progressMode.matches(progressById[t.id] ?: 0)
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
            // Ticket 13: the cycling progress filter is a pill, styled with the
            // same RoundedCornerShape(999.dp) treatment as the segment bar
            // (surface fill, outline edge) so the two read as one control row.
            Text(
                progressMode.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(999.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(999.dp))
                    .clickable {
                        val modes = ProgressMode.entries
                        progressMode = modes[(progressMode.ordinal + 1) % modes.size]
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
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
            Segment.entries.forEach { candidate ->
                val selected = candidate == segment
                Text(
                    candidate.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(999.dp),
                        )
                        .clickable { segment = candidate }
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
                QuartersDial(pct / 100f)
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
        StreakHead(state)
        OutlinedButton(onClick = onOpenLibrary, modifier = Modifier.fillMaxWidth()) { Text("Open Library") }
    }
}

/**
 * Streak head (prototype page=week, variant B — owner verdict): the day count
 * is primary with "day streak" beneath, and the compact 7-dot strip is
 * secondary. Filled dot = a day with a completed session (session log),
 * gold ring = today, pending until a session completes today. Habit, not
 * mastery — the mastery card above stays untouched.
 */
@Composable
private fun StreakHead(state: EruditioViewModel.ProgressState) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column {
                    Text("${state.streak}", style = MaterialTheme.typography.headlineLarge)
                    Text(
                        "day streak",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.weight(1f))
                WeekDots(state.weekDots)
            }
            Text(
                "Habit, not mastery",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The trailing week as dots: 11dp circle over a one-letter day label. */
@Composable
private fun WeekDots(days: List<WeekDot>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        days.forEach { day ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                val filled = day.hasCompletedSession
                Box(
                    Modifier
                        .size(15.dp)
                        .then(
                            if (day.today) {
                                // Ticket 19: today's pending halo is the notch gold (tertiary).
                                Modifier.border(2.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f), CircleShape)
                            } else {
                                Modifier
                            },
                        )
                        .padding(2.dp)
                        .size(11.dp)
                        .background(
                            if (filled) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                            CircleShape,
                        )
                        .border(
                            width = 1.5.dp,
                            color = when {
                                // Ticket 19: today's ring is the notch gold (tertiary).
                                day.today -> MaterialTheme.colorScheme.tertiary
                                filled -> MaterialTheme.colorScheme.onSurface
                                else -> MaterialTheme.colorScheme.outline
                            },
                            shape = CircleShape,
                        ),
                )
                Text(
                    dayLetter(day.epochDay),
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Weekday initial (M, T, W, …) for a dot's label, in the device locale. */
private fun dayLetter(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay)
        .dayOfWeek
        .getDisplayName(TextStyle.NARROW, Locale.getDefault())

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
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val railEnabled by vm.railEnabled.collectAsState()
                Text("Reading", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Reading rail",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Switch(checked = railEnabled, onCheckedChange = vm::setRailEnabled)
                }
                Text(
                    "A slim position rail in the reader: tap or drag to jump, checkpoint ticks included.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // Ticket 22: About card with the in-app bug report composer (variant A).
        var showBugSheet by remember { mutableStateOf(false) }
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("About", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    Modifier.fillMaxWidth().clickable { showBugSheet = true }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Report a bug",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
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
        if (showBugSheet) {
            BugReportSheet(onDismiss = { showBugSheet = false })
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

/**
 * Ticket 22: the bug report composer (prototype variant A). The user writes in
 * their own words; Send hands the text plus version lines to the mail client.
 * Nothing is collected, logged, or sent until that tap.
 */
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
                placeholder = {
                    Text("What went wrong? Your own words — no logs or screenshots are attached.")
                },
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
                OutlinedButton(onClick = onDismiss) {
                    Text("Discard")
                }
                Button(
                    onClick = {
                        val report = "${text.trim()}\n\n---\n$versionLine · $androidLine"
                        val intent =
                            Intent(Intent.ACTION_SENDTO).apply {
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
                ) {
                    Text("Send")
                }
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
