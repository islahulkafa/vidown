package app.vidown

import app.vidown.python.MediaFormat
import app.vidown.python.normalizeFormatId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatSelectorTest {

    @Test
    fun defaultFormat_containsBestStream() {
        val format = MediaFormat(
            id = "bestvideo*+bestaudio/best",
            label = "Best Quality",
            note = "Maximum video and audio quality",
            isAudioOnly = false
        )
        assertEquals("bestvideo*+bestaudio/best", format.id)
        assertFalse(format.isAudioOnly)
    }

    @Test
    fun normalizeFormatId_upgradesLegacyBestSelectors() {
        assertEquals("bestvideo*+bestaudio/best", normalizeFormatId("b", isAudioOnly = false))
        assertEquals("bestvideo*+bestaudio/best", normalizeFormatId("best", isAudioOnly = false))
        assertEquals("bestvideo*+bestaudio/best", normalizeFormatId("", isAudioOnly = false))
        assertEquals("bestaudio/best", normalizeFormatId("b", isAudioOnly = true))
        assertEquals("bestaudio/best", normalizeFormatId("best", isAudioOnly = true))
        assertEquals("bestvideo[height<=1080]+bestaudio/best", normalizeFormatId("bestvideo[height<=1080]+bestaudio/best", isAudioOnly = false))
    }

    @Test
    fun audioFormat_isCorrectlyFlagged() {
        val audioFormat = MediaFormat(
            id = "bestaudio/m4a",
            label = "M4A Audio (AAC)",
            note = "Universal Android compatibility",
            isAudioOnly = true,
            audioCodec = "m4a"
        )
        assertEquals("bestaudio/m4a", audioFormat.id)
        assertTrue(audioFormat.isAudioOnly)
        assertEquals("m4a", audioFormat.audioCodec)
    }

    @Test
    fun flacAudioFormat_isConfigured() {
        val flacFormat = MediaFormat(
            id = "bestaudio/flac",
            label = "FLAC Audio",
            note = "Lossless CD quality",
            isAudioOnly = true,
            audioCodec = "flac"
        )
        assertTrue(flacFormat.isAudioOnly)
        assertEquals("flac", flacFormat.audioCodec)
    }

    @Test
    fun formatSpeed_calculatesCorrectUnits() {
        assertEquals("500 B/s", formatSpeed(500.0))
        assertEquals("150 KB/s", formatSpeed(150 * 1024.0))
        assertEquals("2.5 MB/s", formatSpeed(2.5 * 1024 * 1024))
    }

    @Test
    fun formatEta_calculatesMinutesAndSeconds() {
        assertEquals("--", formatEta(0))
        assertEquals("45s", formatEta(45))
        assertEquals("2m 15s", formatEta(135))
    }

    private fun formatSpeed(bytesPerSec: Double): String {
        return when {
            bytesPerSec >= 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f MB/s", bytesPerSec / (1024 * 1024))
            bytesPerSec >= 1024 -> String.format(java.util.Locale.US, "%.0f KB/s", bytesPerSec / 1024)
            else -> String.format(java.util.Locale.US, "%.0f B/s", bytesPerSec)
        }
    }

    private fun formatEta(seconds: Int): String {
        if (seconds <= 0) return "--"
        val m = seconds / 60
        val s = seconds % 60
        return if (m > 0) "${m}m ${s}s" else "${s}s"
    }
}
