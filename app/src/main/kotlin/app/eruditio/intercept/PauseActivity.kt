package app.eruditio.intercept

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import app.eruditio.EruditioApp
import app.eruditio.core.SessionStart
import app.eruditio.ui.openReader
import app.eruditio.ui.theme.EruditioTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * The friction screen (verified PNAS mechanic, spec): a short delay with a
 * breathing mark, then an explicit choice. "Continue" goes straight into the
 * target app — never a hard block — and waits out the delay. "Read instead" is
 * available immediately: the friction must never obstruct the learning path.
 * Paper styling per the wireframe direction.
 */
class PauseActivity : androidx.activity.ComponentActivity() {

    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val target = intent.getStringExtra(EXTRA_TARGET).orEmpty()
        val container = (application as EruditioApp).container

        // Story 4: a dismissed pause screen still records its outcome. Back
        // means the user proceeds to the app beneath the overlay — the same
        // *continued* outcome the Continue button records. (The system Home
        // button is not interceptable from an Activity and stays unrecorded.)
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    cont(target)
                    finish()
                }
            },
        )

        var enabled by mutableStateOf(false)

        setContent {
            EruditioTheme(container.themes.palette.value) {
                Surface(
                    Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.97f),
                ) {
                    PauseBody(target, enabled, container)
                }
            }
        }

        // Verified mechanic: ~10 s of quiet before the choice enables (spec).
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ enabled = true }, FRICTION_DELAY_MILLIS)
    }

    @Composable
    private fun PauseBody(target: String, enabled: Boolean, container: app.eruditio.AppContainer) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val breath = rememberInfiniteTransition(label = "breath")
            val scale by breath.animateFloat(
                initialValue = 0.82f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
                label = "scale",
            )
            Box(
                Modifier.size(120.dp).graphicsLayer { scaleX = scale; scaleY = scale },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "breathe",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "Opening ${target.substringAfterLast('.')}",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                "No hurry. Stay here, or go on — your call.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Column(
                Modifier.fillMaxWidth().padding(top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(
                    onClick = { replace(target, container) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Read a page instead") }
                OutlinedButton(
                    onClick = { cont(target); finish() },
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Continue to the app") }
            }
        }
    }

    private fun cont(target: String) {
        val container = (application as EruditioApp).container
        scope.launch { container.sessions.continueToApp(target) }
    }

    private fun replace(target: String, container: app.eruditio.AppContainer) {
        scope.launch {
            val defaultText = container.positions.lastReadTextId()
                ?: container.library.texts.firstOrNull()?.id
                ?: return@launch
            when (val start = container.sessions.replaceWithSession(target, defaultText)) {
                is SessionStart.Opened -> {
                    openReader(this@PauseActivity, start.sessionId)
                    finish()
                }
                SessionStart.Locked -> {
                    startActivity(Intent(this@PauseActivity, app.eruditio.MainActivity::class.java))
                    finish()
                }
            }
        }
    }

    companion object {
        const val EXTRA_TARGET = "target_package"
        private const val FRICTION_DELAY_MILLIS = 10_000L
    }
}
