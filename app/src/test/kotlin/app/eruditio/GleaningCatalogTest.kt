package app.eruditio

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * Content integrity for the offline curated catalog. A lens is an editorial
 * claim ABOUT a passage, so it may only ship if the passage it names is still
 * in the corpus. Every entry is checked — a stale second entry fails too.
 */
class GleaningCatalogTest {

    @Test
    fun `every catalog anchor still exists in its source text`() {
        val entries = catalogEntries()

        assertTrue("catalog should not be empty", entries.isNotEmpty())

        entries.forEach { (textId, anchor) ->
            val text = canonicalProse(textAsset(textId))
            if (!text.contains(canonicalProse(anchor))) {
                fail("catalog entry for '$textId' is stale; corpus no longer contains: $anchor")
            }
        }
    }

    @Test
    fun `catalog has no duplicate anchors for the same text`() {
        val pairs = catalogEntries()
        assertTrue("duplicate catalog entries", pairs.size == pairs.toSet().size)
    }

    private fun catalogEntries(): List<Pair<String, String>> {
        val json = asset("gleaning_context.json")
        val textIds = Regex("\"textId\"\\s*:\\s*\"([^\"]+)\"").findAll(json).map { it.groupValues[1] }.toList()
        val anchors = Regex("\"anchor\"\\s*:\\s*\"([^\"]+)\"").findAll(json).map { it.groupValues[1] }.toList()
        assertTrue("every entry needs an anchor", textIds.size == anchors.size)
        return textIds.zip(anchors)
    }

    /** Text ids are file stems in `assets/texts/`, e.g. `euthyphro`. */
    private fun textAsset(textId: String): String = asset("texts/$textId.txt")

    /** The same rule production uses, so this test cannot pass while the app fails. */
    private fun canonicalProse(value: String) = app.eruditio.core.canonicalProse(value)

    private fun asset(relative: String): String {
        val file = assetRoot().resolve(relative)
        if (!file.isFile) fail("asset not found: $relative")
        return file.readText()
    }

    /** Walks up to the module root so the test is independent of the Gradle cwd. */
    private fun assetRoot(): File = generateSequence(File(".").absoluteFile) { it.parentFile }
        .map { File(it, "src/main/assets") }
        .firstOrNull { it.isDirectory }
        ?: File("src/main/assets")
}
