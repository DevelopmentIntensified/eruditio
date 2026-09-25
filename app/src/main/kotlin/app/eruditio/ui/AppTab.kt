package app.eruditio.ui

/** The five destinations in the pinned Paper shell. */
internal enum class AppTab(val glyph: String, val label: String) {
    TODAY("⌂", "Today"),
    LIBRARY("☰", "Library"),
    GLEANINGS("✦", "Gleanings"),
    PROGRESS("◉", "Progress"),
    SETTINGS("⚙", "Settings"),
}
