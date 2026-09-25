package app.eruditio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.eruditio.core.RecallGrade

/** Review items are returns, not scores. */
@Composable
internal fun ReviewScreen(
    vm: EruditioViewModel,
    state: ReviewState,
    modifier: Modifier,
) {
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        PaperHeader(
            eyebrow = "Review / spaced repetition",
            title = "Return to what you learned.",
            subtitle = "Recall before you grade. Mastery stays honest.",
        )
        Spacer(Modifier.height(16.dp))
        val reviewItem = state.current
        if (reviewItem == null) {
            Text("Nothing due. Read a segment from the Library and new review items arrive tomorrow.")
        } else {
            Text(
                if (state.isChallenge) "Mastery challenge · learned material resurfacing"
                else "Review item ${state.index + 1} of ${state.total}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PaperSurface(Modifier.fillMaxWidth()) {
                Text(
                    vm.titleOfConcept(reviewItem.conceptId),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(state.cue ?: reviewItem.conceptId, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Recall it before you grade.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
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
