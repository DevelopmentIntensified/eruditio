package app.eruditio.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.TextField
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.eruditio.AppContainer
import app.eruditio.EruditioApp
import app.eruditio.R
import app.eruditio.core.CompletedSession
import app.eruditio.core.Annotation
import app.eruditio.core.AnnotationDraft
import app.eruditio.core.AnnotationKind
import app.eruditio.core.Groups
import app.eruditio.core.GleaningHandoff
import app.eruditio.core.ReaderItem
import app.eruditio.core.ReaderText
import app.eruditio.core.wordRangeAt
import app.eruditio.ui.theme.EruditioTheme
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * The quiet reading surface (spec): full-bleed text, no feeds, no adjacency.
 * Position memory across restarts; check-in records the completed segment.
 * Time actually spent in the surface is accumulated on pause (progress: time
 * learning, not scrolling). Both entry paths land here.
 */
class ReaderActivity : androidx.activity.ComponentActivity() {

    private var readingSince: Long? = null

    /** Registered by ReadingBody; flushes the current top-of-screen line position immediately. */
    private var flushPosition: (() -> Unit)? = null

    /**
     * ReadingBody wires (or clears) its top-line flush through this callback —
     * the onCompleted pattern, so composition never downcasts to this activity.
     */
    internal fun registerFlush(flush: (() -> Unit)?) {
        flushPosition = flush
    }

    /**
     * Checkpoint-pause leave path (ticket 14): clear the flush synchronously so
     * the pause-time onPause cannot overwrite the checkpoint save, then finish.
     * When this reader is the task ROOT — an interception-initiated session:
     * the pause overlay starts it with FLAG_ACTIVITY_NEW_TASK and finishes
     * itself, leaving no shell beneath — bring the shell back explicitly so
     * the user lands on Home/Today instead of dropping to the launcher.
     * With a shell beneath (in-app sessions), plain finish() returns to it and
     * App.kt consumes pauseToHome.
     */
    internal fun leaveAtCheckpoint(gleaningSessionId: Long? = null) {
        flushPosition = null
        if (gleaningSessionId != null) {
            startActivity(GleaningActivity.intent(this, gleaningSessionId))
        } else if (isTaskRoot) {
            returnToTodayAndSelect(this)
        }
        finish()
    }

    override fun onResume() {
        super.onResume()
        readingSince = System.currentTimeMillis()
    }

    override fun onPause() {
        flushReadingTime()
        // The top visible line is the user's place: persist it now, not at the
        // next debounce tick, so leaving mid-scroll still restores the exact line.
        flushPosition?.invoke()
        super.onPause()
    }

    /** Time learning, not scrolling: accumulated only while the reader shows. */
    private fun flushReadingTime() {
        val since = readingSince ?: return
        readingSince = null
        val sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1L)
        if (sessionId <= 0L) return
        val spent = System.currentTimeMillis() - since
        val container = (application as EruditioApp).container
        lifecycleScope.launch {
            container.sessions.addReadingTime(sessionId, spent)
        }
    }

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
    val readerText: ReaderText,
    val startItem: Int,
)

@OptIn(FlowPreview::class)
@Composable
private fun ReaderScreen(container: AppContainer, sessionId: Long) {
    val loaded = remember { mutableStateOf<Loaded?>(null) }
    val context = LocalContext.current

    LaunchedEffect(sessionId) {
        val record = container.sessions.sessionFor(sessionId) ?: return@LaunchedEffect
        val text = container.library.text(record.textId)
        val meta = container.library.meta(record.textId)
        val readerText = ReaderText.of(text, meta?.group.orEmpty())
        loaded.value = Loaded(
            sessionId = sessionId,
            textId = record.textId,
            title = meta?.title ?: record.textId,
            author = meta?.author.orEmpty(),
            group = meta?.group.orEmpty(),
            readerText = readerText,
            startItem = readerText.itemIndexAt(record.startChar),
        )
    }

    // The activity owns pause-time flush wiring and the checkpoint leave path;
    // the body reaches both through callbacks instead of downcasting Compose.
    val activity = context as? ReaderActivity
    val current = loaded.value
    if (current == null) {
        Column(Modifier.fillMaxSize().padding(24.dp)) { Text("Loading…") }
    } else {
        ReadingBody(
            container = container,
            loaded = current,
            onRegisterFlush = { flush -> activity?.registerFlush(flush) },
            onLeave = { completedSessionId -> activity?.leaveAtCheckpoint(completedSessionId) },
            onCompleted = { completed ->
                context.startActivity(GleaningActivity.intent(context, completed.sessionId))
                (context as? Activity)?.finish()
            },
        )
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun ReadingBody(
    container: AppContainer,
    loaded: Loaded,
    onRegisterFlush: (flush: (() -> Unit)?) -> Unit,
    onLeave: (Long?) -> Unit,
    onCompleted: (CompletedSession) -> Unit,
) {
    val listState = rememberLazyListState()
    val readerText = loaded.readerText
    val items = readerText.items
    val scope = rememberCoroutineScope()

    // The top visible line is the user's place: the debounced saver persists it
    // while scrolling, and activity pause flushes it immediately without waiting
    // for the debounce. Resume = scroll so this item is the FIRST visible item.
    // The list index is a text-item index + the plate offset; persisted saves
    // stay in TEXT-item space (the plate itself counts as "the beginning").
    fun saveTopLine() {
        val idx = readerText.textIndexOf(listState.firstVisibleItemIndex)
        val start = readerText.startCharAt(idx) ?: return
        scope.launch { container.positions.save(loaded.textId, start) }
    }

    LaunchedEffect(loaded) {
        if (loaded.startItem > 0) listState.scrollToItem(readerText.listIndexOf(loaded.startItem))
    }

    DisposableEffect(onRegisterFlush, loaded) {
        onRegisterFlush { saveTopLine() }
        onDispose { onRegisterFlush(null) }
    }

    // Held so the checkpoint pause can stop a pending debounced save from
    // overwriting the checkpoint position after it lands. remember-backed so
    // the cancel survives recomposition — a plain composition var reads the
    // current frame's value and nulls out (Rail pattern below).
    var saveJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var completing by remember { mutableStateOf(false) }
    LaunchedEffect(listState, loaded) {
        saveJob = launch {
            snapshotFlow { listState.firstVisibleItemIndex }
                .debounce(600)
                .distinctUntilChanged()
                .collect { listIdx ->
                    val idx = readerText.textIndexOf(listIdx)
                    val start = readerText.startCharAt(idx) ?: return@collect
                    container.positions.save(loaded.textId, start)
                }
        }
    }

    // A checkpoint is "on screen" while any currently visible item carries the
    // checkpoint spacing; the chip shows for the nearest (topmost) one only.
    // Visible list indexes are translated back to TEXT-item space first.
    val visibleCheckpoint by remember(listState, loaded) {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo
                .asSequence()
                .map { readerText.textIndexOf(it.index) }
                .filter { readerText.isCheckpoint(it) }
                .minOrNull()
        }
    }

    // Ticket 20: ALL on-screen checkpoint dots breathe — the visible TEXT-index
    // window (not just the topmost checkpoint) gates the animation; dots off
    // screen stay quiet. The pause chip below keeps its topmost-visible rule.
    val visibleTextWindow by remember(listState, loaded) {
        derivedStateOf {
            val info = listState.layoutInfo.visibleItemsInfo
            if (info.isEmpty()) null
            else readerText.textIndexOf(info.minOf { it.index })..readerText.textIndexOf(info.maxOf { it.index })
        }
    }

    // Pause at a checkpoint: the session completes AT that checkpoint (the next
    // one bounds the segment), the resume point is the checkpoint line itself,
    // and the gleaning flow takes over before the reader finishes.
    fun pauseAtCheckpoint(checkpoint: Int) {
        // Hardening: cancel the pending debounced save and suppress the
        // pause-time flush synchronously, so neither can overwrite the
        // checkpoint position after it lands.
        saveJob?.cancel()
        onRegisterFlush(null)
        val end = readerText.segmentEndAfter(checkpoint)
        scope.launch {
            container.positions.save(loaded.textId, items[checkpoint].startChar)
            // ADR-0006: the hand-off row is written BEFORE the session completes,
            // so "completed" always implies "a reflection is owed".
            when (container.gleaningCoordinator.preparePending(loaded.sessionId, end)) {
                GleaningHandoff.PREPARED -> {
                    val completed = container.sessions.completeSession(loaded.sessionId, end)
                    onLeave(completed?.sessionId)
                }
                GleaningHandoff.ALREADY_HANDLED -> {
                    container.pauseToHome.value = true
                    onLeave(null)
                }
                // No durable prompt means no completion: the reader stays put.
                GleaningHandoff.UNAVAILABLE -> Unit
            }
        }
    }

    // Rail state: checkpoint item indexes and the togglable on/off.
    val railEnabled by container.reading.railEnabled.collectAsState()
    val checkpointIndexes = remember(items) {
        items.indices.filter { readerText.isCheckpoint(it) }.toIntArray()
    }

    // Reader marks (tickets 26→27, variant A): bookmarks hang off each
    // paragraph's right edge, long-press opens the inline mark menu. All on
    // device, per text, anchored in the same char space as positions.
    var marks by remember(loaded.textId) { mutableStateOf(emptyList<Annotation>()) }
    LaunchedEffect(loaded.textId) {
        marks = container.annotations.forText(loaded.textId)
    }
    var markMenu by remember { mutableStateOf<MarkMenuContext?>(null) }
    var marksOpen by remember { mutableStateOf(false) }
    var noteSheet by remember { mutableStateOf<NoteTarget?>(null) }

    fun refreshMarks() {
        scope.launch { marks = container.annotations.forText(loaded.textId) }
    }

    fun toggleBookmark(textIndex: Int) {
        scope.launch {
            val anchor = readerText.startCharAt(textIndex) ?: return@launch
            val existing = marks.firstOrNull {
                it.kind == AnnotationKind.BOOKMARK && it.startChar == anchor
            }
            if (existing != null) container.annotations.drop(existing.id)
            else container.annotations.keep(
                AnnotationDraft(loaded.textId, AnnotationKind.BOOKMARK, anchor, anchor),
            )
            refreshMarks()
        }
    }

    fun addHighlight(context: MarkMenuContext) {
        val item = items[context.textIndex]
        val start = item.startChar + context.word.first
        val end = item.startChar + context.word.last + 1
        scope.launch {
            if (marks.none {
                    it.kind == AnnotationKind.HIGHLIGHT && it.startChar == start && it.endChar == end
                }
            ) {
                container.annotations.keep(
                    AnnotationDraft(loaded.textId, AnnotationKind.HIGHLIGHT, start, end),
                )
                refreshMarks()
            }
            markMenu = null
        }
    }

    /** The note editor opens with the pressed word as its anchor; an existing
     *  note at the same range edits in place instead of stacking a second. */
    fun openNoteSheet(context: MarkMenuContext) {
        val item = items[context.textIndex]
        val start = item.startChar + context.word.first
        val end = item.startChar + context.word.last + 1
        noteSheet = NoteTarget(
            startChar = start,
            endChar = end,
            existing = marks.firstOrNull {
                it.kind == AnnotationKind.NOTE && it.startChar == start && it.endChar == end
            },
        )
        markMenu = null
    }

    fun jumpTo(mark: Annotation) {
        marksOpen = false
        markMenu = null
        scope.launch {
            listState.scrollToItem(readerText.listIndexOf(readerText.itemIndexAt(mark.startChar)))
        }
    }

    fun saveNote(text: String) {
        val target = noteSheet ?: return
        scope.launch {
            if (target.existing != null) {
                container.annotations.rewrite(target.existing.id, text)
            } else {
                container.annotations.keep(
                    AnnotationDraft(loaded.textId, AnnotationKind.NOTE, target.startChar, target.endChar, text),
                )
            }
            refreshMarks()
            noteSheet = null
        }
    }

    // Full-screen plate viewer (map + timeline) over the ancient track.
    var plateViewer by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(start = 24.dp)) {
                Text(loaded.title, style = MaterialTheme.typography.titleLarge)
                if (loaded.author.isNotEmpty()) {
                    Text(loaded.author, style = MaterialTheme.typography.labelMedium)
                }
            }
            TextButton(onClick = { marksOpen = true }) {
                Text("✍ marks · ${marks.size}", style = MaterialTheme.typography.labelMedium)
            }
        }
        Row(Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
            ) {
            items(readerText.listItemCount) { i ->
                if (i < readerText.plateItems) {
                    // Ticket 21: the ancient-track frontispiece — a sourced
                    // map plate as the first list item, tap to open the
                    // full-screen plate viewer. Not part of the text: no
                    // gutter, no checkpoint math.
                    PlateFrontispiece(
                        onOpen = { plateViewer = true },
                        modifier = Modifier.fillParentMaxWidth().fillParentMaxHeight(0.9f),
                    )
                } else {
                val textIndex = readerText.textIndexOf(i)
                val item = items[textIndex]
                Row(
                    Modifier.fillMaxWidth()
                        .padding(top = if (textIndex > 0 && item.startsParagraph) 14.dp else 0.dp),
                ) {
                    // Constant 18dp start gutter; checkpoint rows carry a 9dp
                    // dot, every row indents alike so the text edge stays put.
                    // Checkpoints count TEXT items: the plate shifts the list
                    // index but never the checkpoint rhythm. Ticket 20 variant
                    // B: every dot whose TEXT index is on screen breathes in
                    // the accent color; the rest stay quiet (muted outline).
                    Box(Modifier.width(18.dp)) {
                        if (readerText.isCheckpoint(textIndex)) {
                            if (visibleTextWindow?.contains(textIndex) == true) {
                                val accent = MaterialTheme.colorScheme.tertiary
                                val breathe = rememberInfiniteTransition(label = "checkpoint-dot")
                                val p by breathe.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(
                                        tween(1600, easing = FastOutSlowInEasing),
                                    ),
                                    label = "breath-progress",
                                )
                                Canvas(
                                    Modifier.align(Alignment.TopCenter)
                                        .padding(top = 16.dp)
                                        .size(9.dp),
                                ) {
                                    val r = size.minDimension / 2f
                                    // 1 -> 1.25 -> 1, a soft ring fading out underneath.
                                    val cycle = 1f - kotlin.math.abs(1f - 2f * p)
                                    val scale = 1f + 0.25f * cycle
                                    drawCircle(
                                        color = accent.copy(alpha = 0.5f * (1f - cycle)),
                                        radius = r * scale + 5.dp.toPx() * cycle,
                                    )
                                    drawCircle(color = accent, radius = r * scale)
                                }
                            } else {
                                Box(
                                    Modifier.align(Alignment.TopCenter)
                                        .padding(top = 16.dp)
                                        .size(9.dp)
                                        .background(MaterialTheme.colorScheme.outline, CircleShape),
                                )
                            }
                        }
                    }
                    MarkLine(
                        item = item,
                        textIndex = textIndex,
                        marks = marks,
                        markMenu = markMenu,
                        onMenu = { markMenu = it },
                        onToggleBookmark = { toggleBookmark(it) },
                        onAddHighlight = { addHighlight(it) },
                        onOpenNote = { openNoteSheet(it) },
                    )
                }
                }
            }
            }
            // Ticket 21: rail fractions must agree with resume — the rail
            // tracks raw list indexes, so it counts the plate too, while the
            // checkpoint tick indexes shift into list space by the offset.
            Rail(
                checkpoints = checkpointIndexes.map { readerText.listIndexOf(it) }.toIntArray(),
                itemCount = readerText.listItemCount,
                listState = listState,
                enabled = railEnabled,
                scope = scope,
                modifier = Modifier.width(22.dp).fillMaxHeight(),
            )
        }
        // Quiet, sticky, not a nag: the chip exists only while a checkpoint is
        // actually on screen, then disappears with it.
        visibleCheckpoint?.let { checkpoint ->
            TextButton(
                onClick = { pauseAtCheckpoint(checkpoint) },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) { Text("Pause here", style = MaterialTheme.typography.labelLarge) }
        }
        Button(
            onClick = {
                // "I read to here" = the bottom of what is actually on screen
                // (counted into the session), but the RESUME point is the top
                // visible line itself, so a reopen lands on the exact line the
                // user was reading at — never past it. List index -> text-item
                // index via the plate offset; saves stay in TEXT-item space.
                val firstVisible = readerText.textIndexOf(listState.firstVisibleItemIndex)
                val end = readerText.segmentEndAfter(firstVisible)
                saveJob?.cancel()
                onRegisterFlush(null)
                completing = true
                scope.launch {
                    // Same hand-off-first rule as the checkpoint path: a completed
                    // session must always owe the reader a durable reflection.
                    when (container.gleaningCoordinator.preparePending(loaded.sessionId, end)) {
                        GleaningHandoff.PREPARED -> {
                            val done = container.sessions.completeSession(loaded.sessionId, end)
                            if (done == null) {
                                completing = false
                                onRegisterFlush { saveTopLine() }
                                return@launch
                            }
                            val resumeStart = readerText.items[firstVisible].startChar
                            container.positions.save(loaded.textId, resumeStart)
                            onCompleted(done)
                        }
                        GleaningHandoff.ALREADY_HANDLED -> {
                            completing = false
                            onRegisterFlush { saveTopLine() }
                        }
                        // The prompt could not be persisted, so do not complete:
                        // an unprompted completion is exactly what ADR-0006 forbids.
                        GleaningHandoff.UNAVAILABLE -> {
                            completing = false
                            onRegisterFlush { saveTopLine() }
                        }
                    }
                }
            },
            enabled = !completing,
            modifier = Modifier.fillMaxWidth().padding(20.dp),
        ) { Text("Check in — I read to here") }
    }

    if (plateViewer) {
        PlateViewer(onDismiss = { plateViewer = false })
    }

    if (marksOpen) {
        MarksSheet(
            marks = marks.sortedBy { it.startChar },
            items = items,
            readerText = readerText,
            onJump = { jumpTo(it) },
            onEditNote = { mark ->
                marksOpen = false
                noteSheet = NoteTarget(mark.startChar, mark.endChar, existing = mark)
            },
            onDrop = {
                scope.launch {
                    container.annotations.drop(it.id)
                    refreshMarks()
                }
            },
            onDismiss = { marksOpen = false },
        )
    }

    noteSheet?.let { target ->
        NoteSheet(
            target = target,
            items = items,
            onDismiss = { noteSheet = null },
            onSave = { text -> saveNote(text) },
        )
    }
}

/**
 * Ticket 21: the ancient-track frontispiece — the sourced public-domain map
 * (Shepherd 1926, Greece at the beginning of the Peloponnesian War, 431 BC)
 * shown at roughly screen height with a quiet caption, like a book plate.
 * The tap-to-open layer is added here, at the call site.
 */
@Composable
private fun PlateFrontispiece(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.pointerInput(onOpen) { detectTapGestures { onOpen() } }) {
        PlateImage(Modifier.weight(1f).fillMaxWidth())
        Text(
            "Plate I · Greece at the beginning of the Peloponnesian War, 431 BC — tap to open",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        )
    }
}

/**
 * Shared plate image (standards review): the bundled sourced map with its
 * content description, rendered fit-to-width — one source for the
 * frontispiece and the viewer's map page; captions stay at the call sites.
 */
@Composable
private fun PlateImage(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.plate_greece_431bc),
        contentDescription = "Map of Greece at the beginning of the Peloponnesian War, 431 BC",
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}

/** The viewer's pages: page 0 the sourced map, page 1 the timeline. */
private const val PLATE_PAGE_COUNT = 2

/**
 * Full-screen plate viewer: the sourced map and the typeset timeline, paged
 * horizontally with dot navigation, dismissed by tapping out (or back).
 * Quiet and offline: the image is bundled, nothing loads at view time.
 */
@Composable
private fun PlateViewer(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val pagerState = rememberPagerState(pageCount = { PLATE_PAGE_COUNT })
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                when (page) {
                    0 -> Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        PlateImage(Modifier.weight(1f).fillMaxWidth())
                        PlateCaption("Plate I · Greece at the beginning of the Peloponnesian War, 431 BC")
                    }
                    else -> Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        TimelinePlate(
                            Modifier.weight(1f).fillMaxWidth(),
                        )
                        PlateCaption("Plate II · The span of the library, 428/427 BC – 1892")
                    }
                }
            }
            Column(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "${pagerState.currentPage + 1} / $PLATE_PAGE_COUNT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(Modifier.padding(top = 8.dp)) {
                    repeat(PLATE_PAGE_COUNT) { dot ->
                        Box(
                            Modifier
                                .padding(horizontal = 5.dp)
                                .size(7.dp)
                                .background(
                                    if (dot == pagerState.currentPage) {
                                        MaterialTheme.colorScheme.tertiary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                    CircleShape,
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlateCaption(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.padding(top = 12.dp),
    )
}

/**
 * Verified span of the bundled library: every date checked against
 * authoritative sources (recorded on ticket 21). Single events tick once;
 * lifespans draw a range between birth and death ticks.
 */
private data class PlateEvent(val label: String, val fromYear: Int, val toYear: Int, val row: Int)

private val PLATE_TIMELINE_EVENTS = listOf(
    PlateEvent("Plato b.", -427, -427, 0), // 428/427 BC (Britannica)
    PlateEvent("Socrates d.", -399, -399, 1), // d. 399 BC (Britannica)
    PlateEvent("Marcus Aurelius", 121, 180, 0),
    PlateEvent("Augustine", 354, 430, 1),
    PlateEvent("KJV", 1611, 1611, 0),
    PlateEvent("Spurgeon", 1834, 1892, 1),
)

/** Axis spans the first dated event of the plates (431 BC map) to the last (1892). */
private const val TIMELINE_MIN_YEAR = -431
private const val TIMELINE_MAX_YEAR = 1892

/** Typeset timeline: ticks + labels drawn on Canvas, colors from the theme. */
@Composable
private fun TimelinePlate(modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.outline
    val tickColor = MaterialTheme.colorScheme.tertiary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val yearColor = MaterialTheme.colorScheme.outline
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    val yearStyle = MaterialTheme.typography.labelMedium.copy(color = yearColor)
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val span = (TIMELINE_MAX_YEAR - TIMELINE_MIN_YEAR).toFloat()
        val xOf = { year: Int -> ((year - TIMELINE_MIN_YEAR) / span) * w }
        val axisY = h * 0.5f

        drawLine(lineColor, Offset(0f, axisY), Offset(w, axisY), strokeWidth = 1.5.dp.toPx())

        val rowsAbove = floatArrayOf(h * 0.34f, h * 0.12f)
        val rowsBelow = floatArrayOf(h * 0.72f, h * 0.92f)

        fun drawCentered(text: String, style: TextStyle, x: Float, y: Float) {
            val layout = textMeasurer.measure(text, style)
            drawText(layout, topLeft = Offset(x - layout.size.width / 2f, y - layout.size.height / 2f))
        }

        for (event in PLATE_TIMELINE_EVENTS) {
            val x1 = xOf(event.fromYear)
            val x2 = xOf(event.toYear)
            val range = event.fromYear != event.toYear
            if (range) {
                // Lifespan: thin bracket above the axis joining the two ticks.
                drawLine(tickColor, Offset(x1, axisY - h * 0.06f), Offset(x2, axisY - h * 0.06f), strokeWidth = 1.2.dp.toPx())
                drawCentered(event.label, labelStyle, (x1 + x2) / 2f, rowsAbove[event.row])
                drawCentered("${event.fromYear} – ${event.toYear}", yearStyle, (x1 + x2) / 2f, rowsBelow[event.row])
            } else {
                drawCentered(event.label, labelStyle, x1, rowsAbove[event.row])
                val yearLabel = if (event.fromYear < 0) "${-event.fromYear} BC" else "${event.fromYear}"
                drawCentered(yearLabel, yearStyle, x1, rowsBelow[event.row])
            }
            drawLine(tickColor, Offset(x1, axisY - 7.dp.toPx()), Offset(x1, axisY + 7.dp.toPx()), strokeWidth = 1.6.dp.toPx())
            if (range) {
                drawLine(tickColor, Offset(x2, axisY - 7.dp.toPx()), Offset(x2, axisY + 7.dp.toPx()), strokeWidth = 1.6.dp.toPx())
            }
        }
    }
}

@Composable
private fun Rail(
    checkpoints: IntArray,
    itemCount: Int,
    listState: LazyListState,
    enabled: Boolean,
    scope: kotlinx.coroutines.CoroutineScope,
    modifier: Modifier,
) {
    if (!enabled || itemCount <= 1) return

    // The rail tracks the SAME anchor as the resume pipeline: the first visible
    // item. Derived state keeps recomposition off the hot scroll path.
    val firstVisible by remember(listState) {
        derivedStateOf { listState.firstVisibleItemIndex }
    }

    var dragging by remember { mutableStateOf(false) }

    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val tickColor = MaterialTheme.colorScheme.outline
    val passedTickColor = MaterialTheme.colorScheme.primary
    val markerColor = MaterialTheme.colorScheme.primary

    val jump: (androidx.compose.ui.unit.IntSize, Float) -> Unit = { size, y ->
        val fraction = (y / size.height).coerceIn(0f, 0.999f)
        val target = (fraction * itemCount).toInt().coerceAtMost(itemCount - 1)
        scope.launch { listState.scrollToItem(target) }
    }

    Box(
        modifier
            .pointerInput(itemCount) {
                detectTapGestures { offset -> jump(size, offset.y) }
            }
            .pointerInput(itemCount) {
                detectVerticalDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false },
                ) { change, _ ->
                    jump(size, change.position.y)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // 6dp visual column, centered in the wider gesture strip.
        androidx.compose.foundation.Canvas(Modifier.fillMaxHeight().width(6.dp)) {
            val w = size.width
            val h = size.height
            drawLine(
                color = trackColor,
                start = Offset(w / 2f, 0f),
                end = Offset(w / 2f, h),
                strokeWidth = w * 0.34f,
            )
            val markerFraction = firstVisible.toFloat() / itemCount
            for (index in checkpoints) {
                val passed = index <= firstVisible
                drawLine(
                    color = if (passed) passedTickColor else tickColor,
                    start = Offset(0f, index / itemCount.toFloat() * h),
                    end = Offset(w, index / itemCount.toFloat() * h),
                    strokeWidth = if (passed) w * 0.8f else w * 0.55f,
                )
            }
            drawCircle(
                color = markerColor,
                radius = w * 0.9f,
                center = Offset(w / 2f, markerFraction * h),
            )
        }
    }
}


/* ---------------- reader marks (variant A, tickets 26→27) ---------------- */

/** The long-press menu's context: the item it belongs to and the word range in it. */
private data class MarkMenuContext(val textIndex: Int, val word: IntRange)

/** The note editor's anchor: the character range and an existing note, if editing. */
private data class NoteTarget(
    val startChar: Long,
    val endChar: Long,
    val existing: Annotation? = null,
)

/** Style pass: the accent behind every highlight range overlapping this item. */
private fun markedText(item: ReaderItem, marks: List<Annotation>, tint: Color): AnnotatedString =
    buildAnnotatedString {
        append(item.text)
        val itemEnd = item.startChar + item.text.length
        marks.filter {
            it.kind == AnnotationKind.HIGHLIGHT && it.endChar > item.startChar && it.startChar < itemEnd
        }.forEach { mark ->
            val from = (mark.startChar - item.startChar).toInt().coerceAtLeast(0)
            val to = (mark.endChar - item.startChar).toInt().coerceAtMost(item.text.length)
            if (to > from) addStyle(SpanStyle(background = tint.copy(alpha = 0.25f)), from, to)
        }
    }

/** Drawer excerpt: the reader's own words for a note, the source words otherwise. */
private fun markExcerpt(mark: Annotation, items: List<ReaderItem>, readerText: ReaderText): String =
    when (mark.kind) {
        AnnotationKind.NOTE -> mark.noteText.orEmpty()
        else -> {
            val item = items.getOrNull(readerText.itemIndexAt(mark.startChar)) ?: return ""
            val from = (mark.startChar - item.startChar).toInt().coerceIn(0, item.text.length)
            val to = (mark.endChar - item.startChar).toInt().coerceIn(from, item.text.length)
            item.text.substring(from, to)
        }
    }.trim().take(72)

/**
 * One text row with variant A's affordances: the hanging bookmark caret at the
 * right edge, a note marker when a note is anchored here, and the long-press
 * inline mark menu. The mark column lives outside the text metrics so the
 * reading line stays unchanged.
 */
@Composable
private fun MarkLine(
    item: ReaderItem,
    textIndex: Int,
    marks: List<Annotation>,
    markMenu: MarkMenuContext?,
    onMenu: (MarkMenuContext?) -> Unit,
    onToggleBookmark: (Int) -> Unit,
    onAddHighlight: (MarkMenuContext) -> Unit,
    onOpenNote: (MarkMenuContext) -> Unit,
) {
    val bookmarked = marks.any {
        it.kind == AnnotationKind.BOOKMARK && it.startChar == item.startChar
    }
    val hasNote = marks.any {
        it.kind == AnnotationKind.NOTE &&
            it.endChar > item.startChar && it.startChar < item.startChar + item.text.length
    }
    val tint = MaterialTheme.colorScheme.tertiary
    val quiet = MaterialTheme.colorScheme.outlineVariant
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Row(Modifier.fillMaxWidth()) {
        Box(Modifier.weight(1f)) {
            Text(
                markedText(item, marks, tint),
                Modifier
                    .fillMaxWidth()
                    .pointerInput(textIndex) {
                        detectTapGestures(onLongPress = { position ->
                            val layout = layoutResult ?: return@detectTapGestures
                            val word = wordRangeAt(item.text, layout.getOffsetForPosition(position))
                                ?: return@detectTapGestures
                            onMenu(MarkMenuContext(textIndex, word))
                        })
                    },
                onTextLayout = { layoutResult = it },
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 30.sp),
            )
            markMenu?.takeIf { it.textIndex == textIndex }?.let { menu ->
                val layout = layoutResult ?: return@let
                val hotBox = layout.getBoundingBox(menu.word.first)
                val offset = with(LocalDensity.current) {
                    IntOffset(hotBox.left.toInt(), (hotBox.top - 50.dp.toPx()).toInt().coerceAtLeast(0))
                }
                Popup(
                    alignment = Alignment.TopStart,
                    offset = offset,
                    onDismissRequest = { onMenu(null) },
                    properties = PopupProperties(focusable = true),
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.inverseSurface,
                        shadowElevation = 4.dp,
                    ) {
                        Row(Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
                            TextButton(onClick = { onAddHighlight(menu) }) {
                                Text("Highlight", color = MaterialTheme.colorScheme.inverseOnSurface)
                            }
                            TextButton(onClick = { onOpenNote(menu) }) {
                                Text("Note", color = MaterialTheme.colorScheme.inverseOnSurface)
                            }
                            TextButton(onClick = { onMenu(null) }) {
                                Text("✕", color = MaterialTheme.colorScheme.inverseOnSurface)
                            }
                        }
                    }
                }
            }
        }
        Column(
            Modifier.width(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "◇",
                Modifier
                    .clickable { onToggleBookmark(textIndex) }
                    .padding(top = 2.dp),
                color = if (bookmarked) tint else quiet,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (hasNote) {
                Text(
                    "¶",
                    Modifier.padding(top = 2.dp),
                    color = tint,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

/** The note composer: target range previewed in the surface's words. */
@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
private fun NoteSheet(
    target: NoteTarget,
    items: List<ReaderItem>,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var draft by remember(target.existing?.id, target.startChar, target.endChar) {
        mutableStateOf(target.existing?.noteText.orEmpty())
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text("Note", style = MaterialTheme.typography.titleMedium)
            Text(
                noteAnchorPreview(target, items),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            TextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .height(120.dp),
                placeholder = { Text("An idea, in your own words") },
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) { Text("Discard") }
                TextButton(
                    enabled = draft.isNotBlank(),
                    onClick = { onSave(draft) },
                ) { Text(if (target.existing != null) "Update" else "Save") }
            }
        }
    }
}

/** The note's source words, trimmed to the anchor, for the composer preview. */
private fun noteAnchorPreview(target: NoteTarget, items: List<ReaderItem>): String {
    val item = items.lastOrNull { it.startChar <= target.startChar } ?: return ""
    val from = (target.startChar - item.startChar).toInt().coerceIn(0, item.text.length)
    val to = (target.endChar - item.startChar).toInt().coerceIn(from, item.text.length)
    return if (to > from) {
        "“" + item.text.substring(from, to).trim() + "”"
    } else {
        ""
    }
}

/** The marks drawer: every kept mark for this text, jump to the anchor. */
@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
private fun MarksSheet(
    marks: List<Annotation>,
    items: List<ReaderItem>,
    readerText: ReaderText,
    onJump: (Annotation) -> Unit,
    onEditNote: (Annotation) -> Unit,
    onDrop: (Annotation) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text(
                "Marks in this text",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            if (marks.isEmpty()) {
                Text(
                    "Nothing marked yet. Long-press a word to highlight or note it; the hanging caret bookmarks the line.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
            marks.forEach { mark ->
                val glyph = when (mark.kind) {
                    AnnotationKind.BOOKMARK -> "◇"
                    AnnotationKind.HIGHLIGHT -> "▬"
                    AnnotationKind.NOTE -> "¶"
                }
                val label = "line ${readerText.itemIndexAt(mark.startChar) + 1} · ${mark.kind.name.lowercase()}"
                val tint = MaterialTheme.colorScheme.tertiary
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (mark.kind == AnnotationKind.NOTE) onEditNote(mark) else onJump(mark)
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(glyph, color = tint, style = MaterialTheme.typography.titleSmall)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (markExcerpt(mark, items, readerText).isNotBlank()) {
                            Text(
                                markExcerpt(mark, items, readerText),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                    Text(
                        "✕",
                        Modifier
                            .clickable { onDrop(mark) }
                            .padding(8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}
