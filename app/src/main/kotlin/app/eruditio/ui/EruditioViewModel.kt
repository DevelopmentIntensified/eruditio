package app.eruditio.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.eruditio.AppContainer
import app.eruditio.core.LedgerEntry
import app.eruditio.core.MasteryLevel
import app.eruditio.core.RecallGrade
import app.eruditio.core.SessionRecord
import app.eruditio.core.SessionStart
import app.eruditio.ui.theme.Palette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EruditioViewModel(private val container: AppContainer) : ViewModel() {

    val texts = container.library.texts

    data class HomeState(
        val due: Int = 0,
        val recent: List<SessionRecord> = emptyList(),
        val hasUsageAccess: Boolean = false,
        val canDrawOverlays: Boolean = false,
    )

    data class ReviewState(
        val current: LedgerEntry? = null,
        val cue: String? = null,
        val index: Int = 0,
        val total: Int = 0,
    )

    data class ProgressState(
        val familiar: Int = 0,
        val proficient: Int = 0,
        val mastered: Int = 0,
        val dueCount: Int = 0,
        val weekly: Int = 0,
        val replaced: Int = 0,
        val streak: Int = 0,
    )

    private val homeState = MutableStateFlow(HomeState())
    val home: StateFlow<HomeState> = homeState

    val palette: StateFlow<Palette> = container.themes.palette

    fun selectTheme(palette: Palette) = container.themes.select(palette)

    private val reviewState = MutableStateFlow(ReviewState())
    val review: StateFlow<ReviewState> = reviewState

    private val progressState = MutableStateFlow(ProgressState())
    val progress: StateFlow<ProgressState> = progressState

    private val textProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val progressById: StateFlow<Map<String, Int>> = textProgress

    fun refresh() {
        viewModelScope.launch {
            homeState.value = HomeState(
                due = container.sessions.dueToday().size,
                recent = container.sessionLog.recent(5),
                hasUsageAccess = container.hasUsageAccess,
                canDrawOverlays = container.canDrawOverlays,
            )
            textProgress.value = texts.associate { it.id to container.lastReadPercent(it.id) }
            refreshReview()
            refreshProgress()
        }
    }

    private fun refreshReview() {
        viewModelScope.launch {
            val due = container.sessions.dueToday()
            val head = due.firstOrNull()
            reviewState.value = ReviewState(
                current = head,
                cue = head?.let { container.concepts.cueFor(it.conceptId) },
                index = 0,
                total = due.size,
            )
        }
    }

    private fun refreshProgress() {
        viewModelScope.launch {
            val all = container.ledger.all()
            progressState.value = ProgressState(
                familiar = all.count { container.scheduler.effectiveMastery(it, container.today()) == MasteryLevel.FAMILIAR },
                proficient = all.count { container.scheduler.effectiveMastery(it, container.today()) == MasteryLevel.PROFICIENT },
                mastered = all.count { container.scheduler.effectiveMastery(it, container.today()) == MasteryLevel.MASTERED },
                dueCount = container.sessions.dueToday().size,
                weekly = container.sessions.weeklyInterceptions(),
                replaced = container.interceptionLog.weeklyReplaced(container.today()),
                streak = container.sessions.currentStreak(),
            )
        }
    }

    fun titleOf(textId: String): String = container.library.meta(textId)?.title ?: textId

    fun titleOfConcept(conceptId: String): String = titleOf(conceptId.substringBefore('#'))

    fun startReading(textId: String) {
        viewModelScope.launch {
            val startChar = container.positions.restore(textId)?.charOffset ?: 0L
            when (val start = container.sessions.startSession(textId, startChar)) {
                is SessionStart.Opened -> openReader(container.appContext, start.sessionId)
                SessionStart.Locked -> Unit // billing gate surface arrives with ticket 10
            }
        }
    }

    fun grade(grade: RecallGrade) {
        viewModelScope.launch {
            val current = reviewState.value.current ?: return@launch
            container.sessions.gradeConcept(current.conceptId, grade)
            val due = container.sessions.dueToday()
            val head = due.firstOrNull()
            reviewState.value = ReviewState(
                current = head,
                cue = head?.let { container.concepts.cueFor(it.conceptId) },
                index = reviewState.value.index + 1,
                total = reviewState.value.total,
            )
        }
    }
}

/** Reader is its own quiet activity; both entry paths (home, pause) land here. */
fun openReader(context: Context, sessionId: Long) {
    context.startActivity(
        Intent(context, ReaderActivity::class.java)
            .putExtra(ReaderActivity.EXTRA_SESSION_ID, sessionId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
