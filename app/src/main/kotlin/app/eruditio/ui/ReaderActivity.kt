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
import app.eruditio.core.Groups
import app.eruditio.core.ReaderText
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
            val completed = container.sessions.completeSession(loaded.sessionId, end)
            if (completed == null) {
                container.pauseToHome.value = true
            } else {
                container.gleaningCoordinator.preparePending(completed.sessionId, end)
            }
            // A checkpoint is a meaningful completion too: offer the same
            // Notice → Compare → Keep flow before returning to the shell.
            onLeave(completed?.sessionId)
        }
    }

    // Rail state: checkpoint item indexes and the togglable on/off.
    val railEnabled by container.reading.railEnabled.collectAsState()
    val checkpointIndexes = remember(items) {
        items.indices.filter { readerText.isCheckpoint(it) }.toIntArray()
    }

    // Full-screen plate viewer (map + timeline) over the ancient track.
    var plateViewer by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 24.dp)) {
            Text(loaded.title, style = MaterialTheme.typography.titleLarge)
            if (loaded.author.isNotEmpty()) {
                Text(loaded.author, style = MaterialTheme.typography.labelMedium)
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
                    Text(
                        item.text,
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 30.sp),
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
                    val done = container.sessions.completeSession(loaded.sessionId, end)
                    if (done != null) {
                        val resumeStart = readerText.items[firstVisible].startChar
                        container.positions.save(loaded.textId, resumeStart)
                        container.gleaningCoordinator.preparePending(done.sessionId, end)
                        onCompleted(done)
                    } else {
                        completing = false
                        onRegisterFlush { saveTopLine() }
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

