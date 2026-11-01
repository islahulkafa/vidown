package app.vidown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlExtractorTest {

    private fun extractUrl(text: String): String? {
        val regex = Regex("""https?://[^\s]+""")
        return regex.find(text)?.value
    }

    @Test
    fun extractUrl_fromDirectLink_returnsExactUrl() {
        val input = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        assertEquals(input, extractUrl(input))
    }

    @Test
    fun extractUrl_fromShareSheetMessage_extractsEmbeddedUrl() {
        val input = "Check this out! https://youtu.be/dQw4w9WgXcQ?si=abcdef via YouTube"
        assertEquals("https://youtu.be/dQw4w9WgXcQ?si=abcdef", extractUrl(input))
    }

    @Test
    fun extractUrl_fromArbitraryTextWithoutUrl_returnsNull() {
        val input = "Just some text with no links here"
        assertNull(extractUrl(input))
    }
}
