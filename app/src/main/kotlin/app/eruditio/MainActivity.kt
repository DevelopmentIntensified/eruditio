package app.eruditio

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import app.eruditio.intercept.UsageStatsWatcherService
import app.eruditio.intercept.WatcherRuntime
import app.eruditio.ui.App
import app.eruditio.ui.EruditioViewModel

/**
 * Single-activity app shell: Today / Library / Gleanings / Progress / Settings.
 * Reader is its own quiet activity; the pause overlay lives in intercept/.
 * The Paper editorial direction is now the production shell.
 */
class MainActivity : androidx.activity.ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as EruditioApp).container
        UsageStatsWatcherService.ensureRunning(this)
        setContent {
            MaterialTheme {
                val vm = viewModel { EruditioViewModel(container) }
                App(vm)
            }
        }
    }

    companion object {
        fun hasUsageAccess(context: Context): Boolean = WatcherRuntime.hasUsageAccess(context)

        fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)
    }
}
