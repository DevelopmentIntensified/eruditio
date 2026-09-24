package app.eruditio.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import app.eruditio.AppContainer
import app.eruditio.EruditioApp
import app.eruditio.core.CompletedSession
import app.eruditio.core.HandoffShelf
import app.eruditio.ui.theme.EruditioTheme
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * The quiet reading surface (spec): full-bleed text, no feeds, no adjacency.
 * Position memory across restarts; check-in records the completed segment and
 * produces the post-session handoff suggestion. Both entry paths land here.
 */
class ReaderActivity : androidx.activity.ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1L)
        val container = (application as EruditioApp).container
        setContent {
            EruditioTheme(container.themes.palette.value) {
                ReaderScreen(container, sessionId)
            }
        }
    }

    companion object {
        const val EXTRA_SESSION_ID = "session_id"
    }
}

private data class Loaded(
    val sessionId: Long,
    val textId: String,
    val title: String,
    val author: String,
    val group: String,
    val text: String,
    val startParagraph: Int,
)

private data class CompletedView(
    val title: String,
    val completed: CompletedSession,
    val handoff: app.eruditio.core.HandoffLink?,
)

@OptIn(FlowPreview::class)
@Composable
private fun ReaderScreen(container: AppContainer, sessionId: Long) {
    val loaded = remember { mutableStateOf<Loaded?>(null) }
    val completed = remember { mutableStateOf<CompletedView?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(sessionId) {
        val record = container.sessionLog.get(sessionId) ?: return@LaunchedEffect
        val text = container.library.text(record.textId)
        val meta = container.library.meta(record.textId)
        loaded.value = Loaded(
            sessionId = sessionId,
            textId = record.textId,
            title = meta?.title ?: record.textId,
            author = meta?.author.orEmpty(),
            group = meta?.group.orEmpty(),
            text = text,
            startParagraph = paragraphIndexAt(record.startChar, text),
        )
    }

    when {
        completed.value != null -> CompletedBody(container, completed.value!!)
        loaded.value != null -> ReadingBody(container, loaded.value!!) { completed.value = it }
        else -> Column(Modifier.fillMaxSize().padding(24.dp)) { Text("Loading…") }
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun ReadingBody(container: AppContainer, loaded: Loaded, onCompleted: (CompletedView) -> Unit) {
    val listState = rememberLazyListState()
    val paragraphs = remember(loaded.text) { splitParagraphs(loaded.text) }
    val starts = remember(paragraphs) { paragraphs.map { it.second } } // startChar per paragraph
    val scope = rememberCoroutineScope()

    LaunchedEffect(loaded) {
        if (loaded.startParagraph > 0) listState.scrollToItem(loaded.startParagraph)
    }

    LaunchedEffect(listState, loaded) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .debounce(600)
            .distinctUntilChanged()
            .collect { idx ->
                val start = starts.getOrNull(idx) ?: return@collect
                container.positions.save(loaded.textId, start, idx)
            }
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 24.dp)) {
            Text(loaded.title, style = MaterialTheme.typography.titleLarge)
            if (loaded.author.isNotEmpty()) {
                Text(loaded.author, style = MaterialTheme.typography.labelMedium)
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
        ) {
            items(paragraphs.size) { i ->
                Text(
                    paragraphs[i].first,
                    Modifier.fillMaxWidth().padding(bottom = 14.dp),
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 30.sp),
                )
            }
        }
        Button(
            onClick = {
                val last = (listState.firstVisibleItemIndex + 8).coerceAtMost(paragraphs.size - 1)
                val end = starts.getOrNull(last + 1) ?: loaded.text.length.toLong()
                scope.launch {
                    val done = container.sessions.completeSession(loaded.sessionId, end)
                    if (done != null) {
                        container.positions.save(loaded.textId, end, last)
                        onCompleted(CompletedView(loaded.title, done, HandoffShelf.suggest(loaded.group)))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(20.dp),
        ) { Text("Check in — I read to here") }
    }
}

@Composable
private fun CompletedBody(container: AppContainer, s: CompletedView) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Session complete", style = MaterialTheme.typography.headlineMedium)
        Text("${s.title} · +${s.completed.newConcepts} review cards")
        s.handoff?.let { link ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("One next step", style = MaterialTheme.typography.labelMedium)
                    Text(link.title, style = MaterialTheme.typography.bodyLarge)
                    OutlinedButton(onClick = {
                        (container.appContext as Activity).startActivity(
                            Intent(Intent.ACTION_VIEW, link.url.toUri()),
                        )
                    }) { Text("Open") }
                    Text("Shown only right after a session.", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Button(onClick = { (container.appContext as? Activity)?.finish() }, modifier = Modifier.fillMaxWidth()) { Text("Done") }
    }
}

/** Paragraph split keeping cumulative character offsets for exact restore. */
private fun splitParagraphs(text: String): List<Pair<String, Long>> {
    val out = mutableListOf<Pair<String, Long>>()
    var start = 0L
    for (block in text.split("\n\n")) {
        val trimmed = block.trim()
        if (trimmed.isNotEmpty()) out += trimmed to start
        start += block.length.toLong() + 2
    }
    if (out.isEmpty()) out += text to 0L
    return out
}

private fun paragraphIndexAt(charOffset: Long, text: String): Int {
    var index = 0
    var start = 0L
    for (block in text.split("\n\n")) {
        if (start > charOffset) return maxOf(0, index - 1)
        start += block.length.toLong() + 2
        index++
    }
    return maxOf(0, index - 1)
}
