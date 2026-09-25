package app.eruditio.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.eruditio.EruditioApp
import app.eruditio.ui.theme.EruditioTheme

/** Variant C: Notice → Compare → Keep, with no grade or recall mechanic. */
class GleaningActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as EruditioApp).container
        val sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1L)
        setContent {
            EruditioTheme(container.themes.palette.value) {
                val vm: GleaningViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { GleaningViewModel(container, sessionId, createSavedStateHandle()) }
                    },
                )
                val state by vm.state.collectAsState()
                GleaningFlow(
                    state = state,
                    onTakeawayChange = vm::setTakeaway,
                    onCompare = vm::compare,
                    onKeepPair = vm::keepPair,
                    onSave = vm::save,
                    onStartOver = vm::startOver,
                    onStepBack = vm::stepBack,
                    onRequestExit = vm::requestExit,
                    onKeepEditing = vm::keepEditing,
                    onDiscard = vm::discard,
                    onFinished = ::returnToToday,
                )
            }
        }
    }

    private fun returnToToday() {
        returnToTodayAndSelect(this)
        finish()
    }

    companion object {
        const val EXTRA_SESSION_ID = "gleaning_session_id"

        fun intent(context: Context, sessionId: Long): Intent =
            Intent(context, GleaningActivity::class.java)
                .putExtra(EXTRA_SESSION_ID, sessionId)
    }
}

@Composable
private fun GleaningFlow(
    state: GleaningUiState,
    onTakeawayChange: (String) -> Unit,
    onCompare: () -> Unit,
    onKeepPair: () -> Unit,
    onSave: () -> Unit,
    onStartOver: () -> Unit,
    onStepBack: () -> Unit,
    onRequestExit: () -> Unit,
    onKeepEditing: () -> Unit,
    onDiscard: () -> Unit,
    onFinished: () -> Unit,
) {
    val source = state.source

    LaunchedEffect(state.discarded) {
        if (state.discarded) onFinished()
    }

    BackHandler {
        when {
            state.saved -> onFinished()
            state.saving -> Unit
            state.step != GleaningStep.NOTICE -> onStepBack()
            state.takeaway.isNotBlank() -> onRequestExit()
            state.loadError != null -> onFinished()
            else -> onDiscard()
        }
    }

    if (state.showExitDialog) {
        AlertDialog(
            onDismissRequest = onKeepEditing,
            title = { Text("Leave this gleaning?") },
            text = { Text("Your takeaway has not been kept yet.") },
            confirmButton = {
                TextButton(onClick = onKeepEditing) { Text("Keep editing") }
            },
            dismissButton = {
                TextButton(onClick = onDiscard) { Text("Discard") }
            },
        )
    }

    when {
        source == null && state.loadError == null -> LoadingBody()
        source == null -> MessageBody("Gleaning unavailable", state.loadError.orEmpty(), onFinished)
        else -> NoticeCompareKeep(
            state = state,
            source = source,
            onTakeawayChange = onTakeawayChange,
            onCompare = onCompare,
            onKeepPair = onKeepPair,
            onSave = onSave,
            onStartOver = onStartOver,
            onFinished = onFinished,
        )
    }
}

@Composable
private fun NoticeCompareKeep(
    state: GleaningUiState,
    source: GleaningSource,
    onTakeawayChange: (String) -> Unit,
    onCompare: () -> Unit,
    onKeepPair: () -> Unit,
    onSave: () -> Unit,
    onStartOver: () -> Unit,
    onFinished: () -> Unit,
) {
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        when (state.step) {
            GleaningStep.NOTICE -> {
                PaperHeader(
                    eyebrow = "Gleaning / ${source.title}",
                    title = "What does this passage give you?",
                    subtitle = "Notice first. Nothing is graded.",
                )
                Spacer(Modifier.height(18.dp))
                SourceContext(source)
                Spacer(Modifier.height(18.dp))
                OutlinedTextField(
                    value = state.takeaway,
                    onValueChange = onTakeawayChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Your takeaway") },
                    placeholder = { Text("Write in your own words") },
                    minLines = 4,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "One idea is enough. Keep the one you want to carry.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onCompare,
                    enabled = state.takeaway.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Compare my reading →") }
            }
            GleaningStep.COMPARE -> {
                PaperHeader(
                    eyebrow = "Gleaning / ${source.title}",
                    title = "Hold your reading beside the source.",
                    subtitle = "Compare, don't grade.",
                )
                Spacer(Modifier.height(18.dp))
                PaperSurface(Modifier.fillMaxWidth()) {
                    Text("YOUR TAKEAWAY", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Text(state.takeaway, style = MaterialTheme.typography.bodyLarge)
                    source.curatedContext?.let { curated ->
                        Spacer(Modifier.height(16.dp))
                        Text("CURATED LENS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        Text(curated, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("SOURCE CONTEXT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Text(source.sourceContext, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = onKeepPair, modifier = Modifier.fillMaxWidth()) { Text("Keep this pair →") }
            }
            GleaningStep.KEEP -> {
                PaperHeader(
                    eyebrow = "Gleaning / ${source.title}",
                    title = "A small thing worth carrying.",
                    subtitle = "Kept locally, with its source attached.",
                )
                Spacer(Modifier.height(18.dp))
                PaperSurface(Modifier.fillMaxWidth()) {
                    Text(state.takeaway, style = MaterialTheme.typography.headlineSmall)
                    source.curatedContext?.let { curated ->
                        Spacer(Modifier.height(12.dp))
                        Text("Lens: $curated", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Source: ${source.title}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (state.saved) {
                    Spacer(Modifier.height(18.dp))
                    Text("Saved to your Gleanings.", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onStartOver, modifier = Modifier.fillMaxWidth()) { Text("Start over") }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onFinished, modifier = Modifier.fillMaxWidth()) { Text("Back to Today") }
                } else {
                    if (state.saveError != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(state.saveError, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.height(18.dp))
                    Button(onClick = onSave, enabled = !state.saving, modifier = Modifier.fillMaxWidth()) {
                        if (state.saving) CircularProgressIndicator(Modifier.height(18.dp)) else Text("Keep this gleaning →")
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceContext(source: GleaningSource) {
    PaperSurface(Modifier.fillMaxWidth()) {
        Text("SOURCE CONTEXT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Text(source.sourceContext, style = MaterialTheme.typography.bodyLarge)
        if (source.author.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(source.author, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LoadingBody() {
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Preparing your gleaning…", style = MaterialTheme.typography.titleLarge)
        CircularProgressIndicator()
    }
}

@Composable
private fun MessageBody(title: String, message: String, onFinished: () -> Unit) {
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(message, style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onFinished) { Text("Back") }
    }
}
