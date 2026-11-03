package app.vidown

import app.vidown.python.SubtitleTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SubtitleSelectorTest {

    @Test
    fun subtitleTrack_correctlyIdentifiesManualAndAuto() {
        val manual = SubtitleTrack(code = "en", name = "English", isAuto = false)
        val auto = SubtitleTrack(code = "id", name = "Indonesian (auto)", isAuto = true)

        assertEquals("en", manual.code)
        assertEquals("English", manual.name)
        assertFalse(manual.isAuto)

        assertEquals("id", auto.code)
        assertEquals("Indonesian (auto)", auto.name)
        assertTrue(auto.isAuto)
    }

    @Test
    fun subtitleMatching_prefersConfiguredLanguages() {
        val availableTracks = listOf(
            SubtitleTrack("en", "English", false),
            SubtitleTrack("id", "Indonesian", false),
            SubtitleTrack("ja", "Japanese", false)
        )
        val preferred = setOf("id", "es")

        val availableCodes = availableTracks.map { it.code }
        val matched = preferred.filter { it in availableCodes }.toSet().ifEmpty {
            availableCodes.take(1).toSet().ifEmpty { setOf("en") }
        }

        assertEquals(setOf("id"), matched)
    }

    @Test
    fun subtitleMatching_fallbackToFirstAvailableWhenNoMatch() {
        val availableTracks = listOf(
            SubtitleTrack("es", "Spanish", false),
            SubtitleTrack("fr", "French", false)
        )
        val preferred = setOf("id", "ja")

        val availableCodes = availableTracks.map { it.code }
        val matched = preferred.filter { it in availableCodes }.toSet().ifEmpty {
            availableCodes.take(1).toSet().ifEmpty { setOf("en") }
        }

        assertEquals(setOf("es"), matched)
    }

    @Test
    fun subtitleDeliveryMode_validation() {
        fun normalizeDeliveryMode(mode: String): String {
            return if (mode.lowercase() == "external") "external" else "embed"
        }

        assertEquals("embed", normalizeDeliveryMode("embed"))
        assertEquals("external", normalizeDeliveryMode("external"))
        assertEquals("external", normalizeDeliveryMode("EXTERNAL"))
        assertEquals("embed", normalizeDeliveryMode("invalid"))
    }

    @Test
    fun subtitleFormat_validation() {
        fun normalizeSubtitleFormat(format: String): String {
            return if (format.lowercase() == "vtt") "vtt" else "srt"
        }

        assertEquals("srt", normalizeSubtitleFormat("srt"))
        assertEquals("vtt", normalizeSubtitleFormat("vtt"))
        assertEquals("vtt", normalizeSubtitleFormat("VTT"))
        assertEquals("srt", normalizeSubtitleFormat("unknown"))
    }

    @Test
    fun companionFileFilter_detectsSubtitlesOnly() {
        val files = listOf(
            File("video.mp4"),
            File("video.en.srt"),
            File("video.id.vtt"),
            File("video.mp4.part"),
            File("video.ytdl"),
            File("video.info.json")
        )

        val companionSubtitles = files.filter { file ->
            val ext = file.extension.lowercase()
            ext == "srt" || ext == "vtt"
        }

        assertEquals(2, companionSubtitles.size)
        assertTrue(companionSubtitles.any { it.name == "video.en.srt" })
        assertTrue(companionSubtitles.any { it.name == "video.id.vtt" })
    }
}
