package app.eruditio.data

import android.content.Context
import app.eruditio.core.SourceTextReader
import org.json.JSONArray

/** One bundled classic (assets/texts/<file>); carries its content group for handoff. */
data class TextMeta(
    val id: String,
    val title: String,
    val author: String,
    val file: String,
    val group: String,
)

class TextLibrary(private val context: Context) : SourceTextReader {

    val texts: List<TextMeta> by lazy { loadIndex() }

    fun meta(textId: String): TextMeta? = texts.firstOrNull { it.id == textId }

    override fun text(textId: String): String {
        val file = requireMeta(textId).file
        // PG plain text comes in three line-ending vintages (\r\n, old \r\r\n,
        // rare bare \r); normalize all so splitting and offset arithmetic see
        // one consistent separator.
        return context.assets.open("texts/$file").bufferedReader().use { it.readText() }
            .replace("\r\r\n", "\n").replace("\r\n", "\n").replace('\r', '\n')
    }

    private fun requireMeta(textId: String): TextMeta =
        texts.firstOrNull { it.id == textId } ?: error("Unknown text $textId")

    private fun loadIndex(): List<TextMeta> {
        val json = JSONArray(context.assets.open("texts/index.json").bufferedReader().use { it.readText() })
        return (0 until json.length()).map { i ->
            val o = json.getJSONObject(i)
            TextMeta(
                o.getString("id"),
                o.getString("title"),
                o.getString("author"),
                o.getString("file"),
                o.getString("group"),
            )
        }
    }
}
