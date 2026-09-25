package app.eruditio.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.eruditio.AppContainer
import app.eruditio.core.GleaningDraft
import app.eruditio.core.GleaningLens
import app.eruditio.core.GleaningSourceBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

internal enum class GleaningStep { NOTICE, COMPARE, KEEP }

internal data class GleaningSource(
    val sessionId: Long,
    val textId: String,
    val title: String,
    val author: String,
    val startChar: Long,
    val endChar: Long,
    val sourceContext: String,
    val curatedContext: String,
)

internal data class GleaningUiState(
    val source: GleaningSource? = null,
    val loadError: String? = null,
    val step: GleaningStep = GleaningStep.NOTICE,
    val takeaway: String = "",
    val saved: Boolean = false,
    val saving: Boolean = false,
    val saveError: String? = null,
    val showExitDialog: Boolean = false,
)

/** Owns the Gleaning flow state and persistence; the Compose screen only renders it. */
internal class GleaningViewModel(
    private val container: AppContainer,
    private val sessionId: Long,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val mutableState = MutableStateFlow(
        GleaningUiState(
            step = savedState.get<String>(KEY_STEP)
                ?.let { runCatching { GleaningStep.valueOf(it) }.getOrNull() }
                ?: GleaningStep.NOTICE,
            takeaway = savedState[KEY_TAKEAWAY] ?: "",
            saved = savedState[KEY_SAVED] ?: false,
        ),
    )
    val state: StateFlow<GleaningUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                loadSource()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                updateState { state -> state.copy(loadError = "This reading session is no longer available.") }
            }
        }
    }

    fun setTakeaway(value: String) {
        updateState { it.copy(takeaway = value) }
    }

    fun requestExit() {
        updateState {
            if (it.takeaway.isNotBlank() && !it.saved) it.copy(showExitDialog = true) else it
        }
    }

    fun keepEditing() {
        updateState { it.copy(showExitDialog = false) }
    }

    fun discardExit() {
        updateState { it.copy(showExitDialog = false) }
    }

    fun compare() {
        updateState {
            if (it.takeaway.isNotBlank()) it.copy(step = GleaningStep.COMPARE) else it
        }
    }

    fun keepPair() {
        updateState {
            if (it.step == GleaningStep.COMPARE) it.copy(step = GleaningStep.KEEP) else it
        }
    }

    fun stepBack() {
        updateState {
            when (it.step) {
                GleaningStep.KEEP -> it.copy(step = GleaningStep.COMPARE)
                GleaningStep.COMPARE -> it.copy(step = GleaningStep.NOTICE)
                GleaningStep.NOTICE -> it
            }
        }
    }

    fun save() {
        val current = state.value
        val source = current.source ?: return
        if (current.saving || current.saved) return
        updateState { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            try {
                container.gleanings.save(
                    GleaningDraft(
                        sessionId = source.sessionId,
                        textId = source.textId,
                        startChar = source.startChar,
                        endChar = source.endChar,
                        takeaway = current.takeaway,
                        sourceContext = source.sourceContext,
                        curatedContext = source.curatedContext,
                    ),
                )
                updateState { it.copy(saved = true, saving = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                updateState {
                    it.copy(saving = false, saveError = "Could not keep this gleaning. Try again.")
                }
            }
        }
    }

    fun startOver() {
        updateState {
            it.copy(
                step = GleaningStep.NOTICE,
                takeaway = "",
                saved = false,
                saveError = null,
                showExitDialog = false,
            )
        }
    }

    private fun updateState(transform: (GleaningUiState) -> GleaningUiState) {
        mutableState.update { current -> transform(current).also(::persist) }
    }

    private fun persist(state: GleaningUiState) {
        savedState[KEY_STEP] = state.step.name
        savedState[KEY_TAKEAWAY] = state.takeaway
        savedState[KEY_SAVED] = state.saved
    }

    private suspend fun loadSource() {
        val session = container.sessions.sessionFor(sessionId)
        val end = session?.endChar
        if (session == null || end == null || end <= session.startChar) {
            updateState { it.copy(loadError = "This reading session is no longer available.") }
            return
        }
        val text = container.library.text(session.textId)
        val source = sourceFor(session.textId, session.startChar, end, text) ?: run {
            updateState { it.copy(loadError = "This session has no readable source context.") }
            return
        }
        updateState { it.copy(source = source) }
    }

    private fun sourceFor(
        textId: String,
        startChar: Long,
        endChar: Long,
        text: String,
    ): GleaningSource? {
        val excerpt = GleaningSourceBuilder.build(text, startChar, endChar) ?: return null
        val meta = container.library.meta(textId)
        return GleaningSource(
            sessionId = sessionId,
            textId = textId,
            title = meta?.title ?: textId,
            author = meta?.author.orEmpty(),
            startChar = excerpt.startChar,
            endChar = excerpt.endChar,
            sourceContext = excerpt.text,
            curatedContext = GleaningLens.forGroup(meta?.group.orEmpty()),
        )
    }

    private companion object {
        const val KEY_STEP = "step"
        const val KEY_TAKEAWAY = "takeaway"
        const val KEY_SAVED = "saved"
    }
}
