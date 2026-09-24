package app.eruditio.intercept

import android.content.Context

/** Which apps get intercepted. Sensible defaults present on first run (ticket 04). */
data class TargetApp(
    val packageId: String,
    val label: String,
)

object TargetApps {
    private const val PREFS_FILE = "target_apps"
    private val defaultTargets = listOf(
        TargetApp("com.instagram.android", "Instagram"),
        TargetApp("com.zhiliaoapp.musically", "TikTok"),
        TargetApp("com.twitter.android", "X"),
        TargetApp("com.reddit.frontpage", "Reddit"),
        TargetApp("com.facebook.katana", "Facebook"),
        TargetApp("com.snapchat.android", "Snapchat"),
        TargetApp("com.google.android.youtube", "YouTube"),
        TargetApp("com.pinterest", "Pinterest"),
        TargetApp("com.linkedin.android", "LinkedIn"),
    )

    fun defaults(): List<TargetApp> = defaultTargets

    fun selected(context: Context): List<TargetApp> {
        val p = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        if (!p.contains("configured")) return defaultTargets
        val chosen = p.getStringSet("packages", emptySet()) ?: emptySet()
        return defaultTargets.filter { it.packageId in chosen }
    }

    fun save(context: Context, packages: Set<String>) {
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .edit().putBoolean("configured", true).putStringSet("packages", packages).apply()
    }

    val isTarget: (Context, String) -> Boolean = { c, pkg -> selected(c).any { it.packageId == pkg } }
}
