package app.eruditio.data

import app.eruditio.ui.theme.Palette
import app.eruditio.ui.theme.Palettes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Theme picking, Omarchy-style: named curated palettes, persisted on-device. */
class ThemeStore(context: android.content.Context) {
    private val prefs = context.getSharedPreferences("eruditio_theme", android.content.Context.MODE_PRIVATE)

    private val state = MutableStateFlow(Palettes.byId(prefs.getString("palette_id", null)))
    val palette: StateFlow<Palette> = state

    fun select(palette: Palette) {
        prefs.edit().putString("palette_id", palette.id).apply()
        state.value = palette
    }
}
