package app.eruditio

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Content integrity for the offline curated catalog: every bundled lens must
 * still anchor to a phrase that exists in the corpus it claims to interpret.
 */
class GleaningCatalogTest {

    @Test
    fun `every catalog anchor still exists in its source text`() {
        val catalog = asset("gleaning_context.json")
        val euthyphro = asset("texts/euthyphro.txt")

        val anchor = Regex("\"anchor\"\\s*:\\s*\"([^\"]+)\"")
            .find(catalog)
            ?.groupValues
            ?.get(1)
            ?: error("catalog has no anchor")

        assertTrue(
            "Euthyphro no longer contains the catalogued anchor: $anchor",
            euthyphro.contains(anchor),
        )
    }

    private fun asset(relative: String): String =
        listOf(File("src/main/assets/$relative"), File("app/src/main/assets/$relative"))
            .firstOrNull { it.isFile }
            ?.readText()
            ?: error("asset not found: $relative")
}
