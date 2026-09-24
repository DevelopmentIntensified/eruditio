package app.eruditio.ui

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.eruditio.core.LedgerEntry
import app.eruditio.core.RecallGrade
import app.eruditio.core.ReviewScheduler

/**
 * Debug verification surface (ticket 02): the due set and challenge set for a
 * fixed synthetic ledger, so scheduler behaviour is verifiable without UI.
 * Debug builds only; the scenario is deterministic.
 */
object SyntheticLedger {
    const val TODAY = 1000L

    private fun chain(
        scheduler: ReviewScheduler,
        id: String,
        enrollDay: Long,
        reviews: List<Pair<Long, RecallGrade>>,
    ): LedgerEntry {
        var entry = scheduler.enroll(id, enrollDay)
        for ((day, grade) in reviews) entry = scheduler.review(entry, grade, day)
        return entry
    }

    fun entries(scheduler: ReviewScheduler): List<LedgerEntry> = listOf(
        chain(scheduler, "demo:apology#c0", 900, listOf(
            901L to RecallGrade.GOOD, 904L to RecallGrade.GOOD, 911L to RecallGrade.GOOD,
            927L to RecallGrade.GOOD, 962L to RecallGrade.GOOD, 998L to RecallGrade.GOOD,
        )), // 6 goods -> mastered, due day 1068 (future)
        chain(scheduler, "demo:crito#c0", 900, listOf(
            901L to RecallGrade.GOOD, 904L to RecallGrade.HARD, 908L to RecallGrade.GOOD,
            915L to RecallGrade.GOOD, 931L to RecallGrade.GOOD, 966L to RecallGrade.EASY,
        )), // 6 reps -> due day 1066 (future)
        chain(scheduler, "demo:euthyphro#c0", 900, listOf(901L to RecallGrade.GOOD)), // reps 2, due 904: overdue
        chain(scheduler, "demo:meno#c0", 900, listOf(
            901L to RecallGrade.GOOD, 904L to RecallGrade.GOOD,
        )), // reps 2, due 907: overdue
        chain(scheduler, "demo:psalms#c0", 990, listOf(993L to RecallGrade.GOOD)), // reps 2, due 996: overdue
        chain(scheduler, "demo:meditations#c0", 999, emptyList()), // enrolled yesterday, due 1000: due today
        chain(scheduler, "demo:plato-fresh#c0", 1000, emptyList()), // enrolled today, due 1001: NOT due
    )
}

class DebugDueActivity : androidx.activity.ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme { DebugDueScreen() }
        }
    }
}

@Composable
private fun DebugDueScreen() {
    val scheduler = ReviewScheduler()
    val ledger = SyntheticLedger.entries(scheduler)
    val due = scheduler.due(ledger, SyntheticLedger.TODAY)
    val challenge = scheduler.masteryChallenge(ledger, SyntheticLedger.TODAY)
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Debug · synthetic ledger (today = day ${SyntheticLedger.TODAY})",
            style = MaterialTheme.typography.titleMedium,
        )
        Text("Due set — ${due.size} item(s)", style = MaterialTheme.typography.labelLarge)
        due.forEach { entry ->
            Text("${entry.conceptId} · due day ${entry.dueEpochDay} · reps ${entry.repetitions} · ${scheduler.ledgerMastery(entry)}")
        }
        Text("Mastery challenge — ${challenge.size} item(s)", style = MaterialTheme.typography.labelLarge)
        challenge.forEach { entry ->
            Text("${entry.conceptId} · last reviewed day ${entry.lastEpochDay} · reps ${entry.repetitions}")
        }
    }
}
