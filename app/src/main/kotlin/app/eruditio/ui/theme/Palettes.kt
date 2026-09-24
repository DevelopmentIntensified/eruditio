package app.eruditio.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Curated named palettes (Omarchy / nvim colorscheme style): a small, finished
 * set of themes, each a complete surface. Chosen once, persisted on-device.
 */
data class Palette(
    val id: String,
    val name: String,
    val bg: Color,
    val surface: Color,
    val ink: Color,
    val muted: Color,
    val rule: Color,
    val accent: Color,
    val dark: Boolean,
)

val palettes = listOf(
    Palette(
        id = "paper", name = "Paper",
        bg = Color(0xFFFAF7F0), surface = Color(0xFFFFFEFB),
        ink = Color(0xFF221F1A), muted = Color(0xFF8A8272),
        rule = Color(0xFFE4DFD2), accent = Color(0xFF221F1A),
        dark = false,
    ),
    Palette(
        id = "ink", name = "Ink",
        bg = Color(0xFF12130F), surface = Color(0xFF1B1D15),
        ink = Color(0xFFE8E4D8), muted = Color(0xFF77746A),
        rule = Color(0xFF2C2E26), accent = Color(0xFFC8A24A),
        dark = true,
    ),
    Palette(
        id = "sepia", name = "Sepia",
        bg = Color(0xFFEFE2CC), surface = Color(0xFFF7EEDD),
        ink = Color(0xFF3B2F1E), muted = Color(0xFF8A7A5E),
        rule = Color(0xFFD9C8A9), accent = Color(0xFF8A5A2B),
        dark = false,
    ),
    Palette(
        id = "terminal", name = "Terminal",
        bg = Color(0xFF0B120C), surface = Color(0xFF101A11),
        ink = Color(0xFFD3E8D3), muted = Color(0xFF5F7A5F),
        rule = Color(0xFF1B2A1C), accent = Color(0xFF4CAF50),
        dark = true,
    ),
    Palette(
        id = "one-dark", name = "One Dark",
        bg = Color(0xFF282C34), surface = Color(0xFF21252B),
        ink = Color(0xFFABB2BF), muted = Color(0xFF5C6370),
        rule = Color(0xFF3E4451), accent = Color(0xFF61AFEF),
        dark = true,
    ),
)

object Palettes {
    val all: List<Palette> get() = app.eruditio.ui.theme.palettes
    val default: Palette get() = all.first()
    fun byId(id: String?): Palette = all.firstOrNull { it.id == id } ?: default
}

private val notchGold = Color(0xFFC8A24A)

private fun schemeOf(palette: Palette): ColorScheme {
    val colors = mapOf(
        "primary" to palette.accent,
        "onPrimary" to palette.bg,
        "background" to palette.bg,
        "onBackground" to palette.ink,
        "surface" to palette.surface,
        "onSurface" to palette.ink,
        "surfaceVariant" to palette.surface,
        "onSurfaceVariant" to palette.muted,
        "outline" to palette.rule,
    )
    return if (palette.dark) {
        darkColorScheme().copy(
            primary = colors.getValue("primary"),
            onPrimary = colors.getValue("onPrimary"),
            background = colors.getValue("background"),
            onBackground = colors.getValue("onBackground"),
            surface = colors.getValue("surface"),
            onSurface = colors.getValue("onSurface"),
            surfaceVariant = colors.getValue("surfaceVariant"),
            onSurfaceVariant = colors.getValue("onSurfaceVariant"),
            outline = colors.getValue("outline"),
            tertiary = notchGold,
        )
    } else {
        lightColorScheme(
            primary = colors.getValue("primary"),
            onPrimary = colors.getValue("onPrimary"),
            background = colors.getValue("background"),
            onBackground = colors.getValue("onBackground"),
            surface = colors.getValue("surface"),
            onSurface = colors.getValue("onSurface"),
            surfaceVariant = colors.getValue("surfaceVariant"),
            onSurfaceVariant = colors.getValue("onSurfaceVariant"),
            outline = colors.getValue("outline"),
            tertiary = notchGold,
        )
    }
}

@Composable
fun EruditioTheme(palette: Palette, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = schemeOf(palette), content = content)
}
