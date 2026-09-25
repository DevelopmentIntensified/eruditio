package app.eruditio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Library, prototype variant B: dense flat rows with compact filters. */
@Composable
internal fun LibraryScreen(vm: EruditioViewModel, modifier: Modifier) {
    val progressById by vm.progressById.collectAsState()
    var segment by rememberSaveable { mutableStateOf(Segment.All) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var progressMode by rememberSaveable { mutableStateOf(ProgressMode.Any) }
    val filtered = LibraryFilter(segment, query, progressMode).apply(vm.texts, progressById)

    Column(
        modifier.verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 20.dp),
    ) {
        PaperHeader(
            eyebrow = "Library / ownership",
            title = "Your working shelf.",
            subtitle = "${vm.texts.size} texts · bundled · offline",
        )
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("Search title or author") },
                    trailingIcon = {
                        Text("✕", modifier = Modifier.clickable { query = ""; searching = false })
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
        PaperSectionHeader("Texts", "${filtered.size} of ${vm.texts.size}")
        Spacer(Modifier.height(4.dp))
        filtered.forEach { text ->
            val percent = progressById[text.id] ?: 0
            Row(
                Modifier.fillMaxWidth().clickable { vm.startReading(text.id) }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(text.title, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text.author,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                QuartersDial(percent / 100f)
            }
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
        }
    }
}
