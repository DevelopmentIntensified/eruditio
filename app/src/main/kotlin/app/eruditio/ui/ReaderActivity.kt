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
    val items: List<ReaderItem>,
    val startItem: Int,
    val textLength: Long,
)

private data class CompletedView(
    val title: String,
    val completed: CompletedSession,
    val handoff: app.eruditio.core.HandoffLink?,
    val next: app.eruditio.data.TextMeta?,
)

/** One scroll item: a paragraph, or a single line of an oversized block. */
private data class ReaderItem(
    val text: String,
    val startChar: Long,
    val startsParagraph: Boolean,
)

private const val MAX_ITEM_CHARS = 2000

/**
 * Split with cumulative character offsets for exact restore. Blocks split on
 * blank lines keep their offsets; a block larger than a couple of screens
 * (verse runs with no blank lines) is cut into per-line items so progress and
 * restore stay granular instead of jumping to 100% on one check-in.
 */
private fun readerItems(text: String): List<ReaderItem> {
    val items = mutableListOf<ReaderItem>()
    var start = 0L
    for (block in text.split("\n\n")) {
        val blockStart = start
        start += block.length.toLong() + 2
        if (block.isBlank()) continue
        val trimmed = block.trim()
        if (trimmed.length <= MAX_ITEM_CHARS) {
            items += ReaderItem(trimmed, blockStart, true)
        } else {
            var idx = 0
            var first = true
            for (line in block.split("\n")) {
                val t = line.trim()
                if (t.isNotEmpty()) {
                    items += ReaderItem(t, blockStart + idx, first)
                    first = false
                }
                idx += line.length + 1
            }
        }
    }
    if (items.isEmpty()) items += ReaderItem(text.trim(), 0L, true)
    return items
}

private fun itemIndexAt(items: List<ReaderItem>, charOffset: Long): Int {
    var best = 0
    for ((index, item) in items.withIndex()) {
        if (item.startChar <= charOffset) best = index else break
    }
    return best
}

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
        val items = readerItems(text)
        loaded.value = Loaded(
            sessionId = sessionId,
            textId = record.textId,
            title = meta?.title ?: record.textId,
            author = meta?.author.orEmpty(),
            group = meta?.group.orEmpty(),
            items = items,
            startItem = itemIndexAt(items, record.startChar),
            textLength = text.length.toLong(),
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
    val items = loaded.items
    val starts = remember(items) { items.map { it.startChar } }
    val scope = rememberCoroutineScope()

    LaunchedEffect(loaded) {
        if (loaded.startItem > 0) listState.scrollToItem(loaded.startItem)
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
            items(items.size) { i ->
                val item = items[i]
                Text(
                    item.text,
                    Modifier.fillMaxWidth().padding(top = if (i > 0 && item.startsParagraph) 14.dp else 0.dp),
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 30.sp),
                )
            }
        }
        Button(
            onClick = {
                // "I read to here" = the bottom of what is actually on screen;
                // a firstVisible+8 heuristic overshot into unread content and
                // resumed further down than the user had read.
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                    ?: listState.firstVisibleItemIndex
                val end = starts.getOrNull(lastVisible + 1) ?: loaded.textLength
                scope.launch {
                    val done = container.sessions.completeSession(loaded.sessionId, end)
                    if (done != null) {
                        container.positions.save(loaded.textId, end, lastVisible)
                        // Recommended next reading: another unstarted classic,
                        // same content group first (in-app momentum over feeds).
                        val next = container.library.texts.firstOrNull {
                            it.id != loaded.textId && it.group == loaded.group && container.lastReadPercent(it.id) == 0
                        } ?: container.library.texts.firstOrNull {
                            it.id != loaded.textId && container.lastReadPercent(it.id) == 0
                        }
                        onCompleted(CompletedView(loaded.title, done, HandoffShelf.suggest(loaded.group), next))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(20.dp),
        ) { Text("Check in — I read to here") }
    }
}

@Composable
private fun CompletedBody(container: AppContainer, s: CompletedView) {
    // Compose context here is the ReaderActivity; the app context cannot start activities or finish.
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Session complete", style = MaterialTheme.typography.headlineMedium)
        Text("${s.title} · +${s.completed.newConcepts} review cards")
        s.next?.let { next ->
            val scope = rememberCoroutineScope()
            OutlinedButton(
                onClick = {
                    scope.launch {
                        when (val start = container.sessions.startSession(next.id, 0L)) {
                            is app.eruditio.core.SessionStart.Opened -> {
                                openReader(context, start.sessionId)
                                (context as? Activity)?.finish()
                            }
                            app.eruditio.core.SessionStart.Locked -> Unit
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Read next: ${next.title}") }
        }
        s.handoff?.let { link ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("One next step", style = MaterialTheme.typography.labelMedium)
                    Text(link.title, style = MaterialTheme.typography.bodyLarge)
                    OutlinedButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, link.url.toUri()))
                    }) { Text("Open") }
                    Text("Shown only right after a session.", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Button(onClick = { (context as? Activity)?.finish() }, modifier = Modifier.fillMaxWidth()) { Text("Done") }
    }
}

