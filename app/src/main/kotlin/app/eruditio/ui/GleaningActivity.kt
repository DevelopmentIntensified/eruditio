package app.eruditio.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.eruditio.AppContainer
import app.eruditio.EruditioApp
import app.eruditio.core.GleaningDraft
import app.eruditio.ui.theme.EruditioTheme
import kotlinx.coroutines.launch

/** Variant C: Notice → Compare → Keep, with no grade or recall mechanic. */
class GleaningActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as EruditioApp).container
        val sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1L)
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = returnToToday()
            },
        )
        setContent {
            EruditioTheme(container.themes.palette.value) {
                GleaningFlow(container, sessionId, ::returnToToday)
            }
        }
    }

    private fun returnToToday() {
        startActivity(
            Intent(this, app.eruditio.MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        )
        finish()
    }

    companion object {
        const val EXTRA_SESSION_ID = "gleaning_session_id"

        fun intent(context: Context, sessionId: Long): Intent =
            Intent(context, GleaningActivity::class.java)
                .putExtra(EXTRA_SESSION_ID, sessionId)
    }
}

private data class GleaningSource(
    val sessionId: Long,
    val textId: String,
    val title: String,
    val author: String,
    val startChar: Long,
    val endChar: Long,
    val sourceContext: String,
)

@Composable
private fun GleaningFlow(
    container: AppContainer,
    sessionId: Long,
    onFinished: () -> Unit,
) {
    var source by remember { mutableStateOf<GleaningSource?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(sessionId) {
        val session = container.sessions.sessionFor(sessionId)
        val end = session?.endChar
        if (session == null || end == null || end <= session.startChar) {
            loadError = "This reading session is no longer available."
            return@LaunchedEffect
        }
        val text = container.library.text(session.textId)
        val textLength = text.length.toLong()
        if (textLength == 0L) {
            loadError = "This session has no readable source context."
            return@LaunchedEffect
        }
        val start = session.startChar.coerceIn(0L, textLength - 1L).toInt()
        val finish = end.coerceIn(start.toLong() + 1L, textLength).toInt()
        val excerpt = text.substring(start, finish)
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(MAX_CONTEXT_CHARS)
        if (excerpt.isBlank()) {
            loadError = "This session has no readable source context."
            return@LaunchedEffect
        }
        val meta = container.library.meta(session.textId)
        source = GleaningSource(
            sessionId = session.sessionId,
            textId = session.textId,
            title = meta?.title ?: session.textId,
            author = meta?.author.orEmpty(),
            startChar = session.startChar,
            endChar = end,
            sourceContext = excerpt,
        )
    }

    val loaded = source
    when {
        loaded == null && loadError == null -> LoadingBody()
        loaded == null -> MessageBody("Gleaning unavailable", loadError.orEmpty(), onFinished)
        else -> NoticeCompareKeep(loaded, container, onFinished)
    }
}

@Composable
private fun NoticeCompareKeep(
    source: GleaningSource,
    container: AppContainer,
    onFinished: () -> Unit,
) {
    var step by remember { mutableIntStateOf(1) }
    var takeaway by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        when (step) {
            1 -> {
                PaperHeader(
                    eyebrow = "Gleaning / ${source.title}",
                    title = "What does this passage give you?",
                    subtitle = "Notice first. Nothing is graded.",
                )
                Spacer(Modifier.height(18.dp))
                SourceContext(source)
                Spacer(Modifier.height(18.dp))
                OutlinedTextField(
                    value = takeaway,
                    onValueChange = { takeaway = it },
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
                    onClick = { step = 2 },
                    enabled = takeaway.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Compare my reading →") }
            }
            2 -> {
                PaperHeader(
                    eyebrow = "Gleaning / ${source.title}",
                    title = "Hold your reading beside the source.",
                    subtitle = "Compare, don't grade.",
                )
                Spacer(Modifier.height(18.dp))
                PaperSurface(Modifier.fillMaxWidth()) {
                    Text("YOUR TAKEAWAY", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Text(takeaway, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(16.dp))
                    Text("SOURCE CONTEXT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Text(source.sourceContext, style = MaterialTheme.typography.bodyMedium)
                }
                if (saveError != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(saveError.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        saving = true
                        saveError = null
                        scope.launch {
                            runCatching {
                                container.gleanings.save(
                                    GleaningDraft(
                                        sessionId = source.sessionId,
                                        textId = source.textId,
                                        startChar = source.startChar,
                                        endChar = source.endChar,
                                        takeaway = takeaway,
                                        sourceContext = source.sourceContext,
                                    ),
                                )
                            }.onSuccess { step = 3 }
                                .onFailure { saveError = "Could not keep this gleaning. Try again." }
                            saving = false
                        }
                    },
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (saving) CircularProgressIndicator(Modifier.height(18.dp)) else Text("Keep this gleaning →")
                }
            }
            else -> {
                PaperHeader(
                    eyebrow = "Gleaning / ${source.title}",
                    title = "A small thing worth carrying.",
                    subtitle = "Kept locally, with its source attached.",
                )
                Spacer(Modifier.height(18.dp))
                PaperSurface(Modifier.fillMaxWidth()) {
                    Text(takeaway, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(12.dp))
                    Text("Source: ${source.title}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(18.dp))
                Button(onClick = onFinished, modifier = Modifier.fillMaxWidth()) { Text("Back to Today") }
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

private const val MAX_CONTEXT_CHARS = 1_200
