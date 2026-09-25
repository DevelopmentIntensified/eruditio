package app.eruditio.data

import android.app.Application
import app.eruditio.core.GleaningContextSource
import app.eruditio.core.canonicalProse
import org.json.JSONArray

/** Reads the small offline catalog of passage-specific editorial lenses. */
class AssetGleaningContextSource(private val app: Application) : GleaningContextSource {

    private data class Entry(
        val textId: String,
        val canonicalAnchor: String,
        val context: String,
    )

    private val entries: List<Entry> by lazy { load() }

    override fun forSource(textId: String, sourceContext: String): String? {
        val source = canonicalProse(sourceContext)
        return entries
            .firstOrNull { it.textId == textId && source.contains(it.canonicalAnchor) }
            ?.context
    }

    private fun load(): List<Entry> = try {
        val json = app.assets.open(CATALOG).bufferedReader().use { it.readText() }
        val array = JSONArray(json)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val entry = Entry(
                    textId = item.optString("textId"),
                    canonicalAnchor = canonicalProse(item.optString("anchor")),
                    context = item.optString("context"),
                )
                if (entry.textId.isNotBlank() && entry.canonicalAnchor.isNotBlank() && entry.context.isNotBlank()) {
                    add(entry)
                }
            }
        }
    } catch (_: Exception) {
        // A missing/corrupt catalog means the flow falls back to source context.
        emptyList()
    }

    private companion object {
        const val CATALOG = "gleaning_context.json"
    }
}
