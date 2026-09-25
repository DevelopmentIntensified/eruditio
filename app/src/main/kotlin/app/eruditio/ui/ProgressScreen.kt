package app.eruditio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.eruditio.core.WeekDot
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
internal fun ProgressScreen(
    state: EruditioViewModel.ProgressState,
    modifier: Modifier,
    onOpenLibrary: () -> Unit,
) {
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        PaperHeader(
            eyebrow = "Progress / return",
            title = "A quiet record of return.",
            subtitle = "Reading first. Mastery only when there is something to memorize.",
        )
        PaperSectionHeader("Gleanings")
        PaperSurface(Modifier.fillMaxWidth()) {
            Text("${state.gleaningCount} ideas kept", style = MaterialTheme.typography.headlineMedium)
            Text("Texts started: ${state.textsInProgress} · read through: ${state.textsReadThrough}")
        }
        PaperSectionHeader("This week")
        PaperSurface(Modifier.fillMaxWidth()) {
            Text("Openings intercepted: ${state.weekly}")
            Text("Readings that replaced them: ${state.replaced}")
            Text(
                "Time reading: ${fmtMinutes(state.minutesReadingWeek)} this week · ${fmtMinutes(state.minutesReadingTotal)} all time",
            )
        }
        PaperSectionHeader("Streak")
        StreakHead(state)
        OutlinedButton(onClick = onOpenLibrary, modifier = Modifier.fillMaxWidth()) { Text("Open Library") }
    }
}

internal fun fmtMinutes(minutes: Long): String =
    when {
        minutes >= 60 -> "${minutes / 60}h ${minutes % 60}m"
        else -> "${minutes}m"
    }

/** The day count is primary; the seven-dot strip stays secondary. */
@Composable
private fun StreakHead(state: EruditioViewModel.ProgressState) {
    PaperSurface(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

private fun dayLetter(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay)
        .dayOfWeek
        .getDisplayName(TextStyle.NARROW, Locale.getDefault())
