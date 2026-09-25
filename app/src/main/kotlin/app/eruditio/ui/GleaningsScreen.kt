package app.eruditio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.eruditio.core.Gleaning

/** The collected output of reading: personal takeaways tied to their source. */
@Composable
internal fun GleaningsScreen(
    gleanings: List<Gleaning>,
    totalCount: Int,
    modifier: Modifier,
    titleOf: (String) -> String,
) {
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        PaperHeader(
            eyebrow = "Gleanings / collected ideas",
            title = "What you kept.",
            subtitle = "Your reading, in your own words.",
        )
        Spacer(Modifier.height(16.dp))
        if (gleanings.isEmpty()) {
            Text(
                "Nothing kept yet. Finish a reading session and name one idea worth carrying.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            PaperSectionHeader("Your gleanings", detail = "$totalCount total")
            gleanings.forEach { gleaning ->
                PaperSurface(Modifier.fillMaxWidth()) {
                    Text(
                        titleOf(gleaning.textId),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(gleaning.takeaway, style = MaterialTheme.typography.bodyLarge)
                    gleaning.curatedContext?.let { curated ->
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "CURATED LENS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(curated, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "SOURCE ANCHOR · ${gleaning.startChar}–${gleaning.endChar}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "SOURCE CONTEXT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        gleaning.sourceContext,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}
