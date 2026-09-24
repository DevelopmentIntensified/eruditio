package app.eruditio.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Reader chrome persisted on-device: the margin rail is togglable (user direction). */
class ReadingSettings(context: Context) {
    private val prefs = context.getSharedPreferences("eruditio_reading", Context.MODE_PRIVATE)

    private val state = MutableStateFlow(prefs.getBoolean("rail_enabled", true))
    val railEnabled: StateFlow<Boolean> = state

    fun setRailEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("rail_enabled", enabled).apply()
        state.value = enabled
    }
}
