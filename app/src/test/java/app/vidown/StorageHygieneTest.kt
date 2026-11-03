package app.vidown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class StorageHygieneTest {

    @Test
    fun orphanedFolderFilter_isolatesInactiveFolders() {
        val activeIds = setOf("task-1", "task-2")
        val onDiskFolderNames = listOf("task-1", "task-2", "task-3-orphaned", "task-4-old")

        val toDelete = onDiskFolderNames.filter { it !in activeIds }
        val toKeep = onDiskFolderNames.filter { it in activeIds }

        assertEquals(2, toDelete.size)
        assertTrue(toDelete.contains("task-3-orphaned"))
        assertTrue(toDelete.contains("task-4-old"))

        assertEquals(2, toKeep.size)
        assertTrue(toKeep.contains("task-1"))
        assertTrue(toKeep.contains("task-2"))
    }

    @Test
    fun formatStorageBytes_formatsStandardUnits() {
        fun format(bytes: Long): String {
            return when {
                bytes <= 0L -> "0 B"
                bytes >= 1024L * 1024L * 1024L -> String.format(Locale.US, "%.1f GB", bytes.toDouble() / (1024L * 1024L * 1024L))
                bytes >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes.toDouble() / (1024L * 1024L))
                bytes >= 1024L -> String.format(Locale.US, "%.0f KB", bytes.toDouble() / 1024L)
                else -> "$bytes B"
            }
        }

        assertEquals("0 B", format(0L))
        assertEquals("512 B", format(512L))
        assertEquals("10 KB", format(10L * 1024L))
        assertEquals("4.5 MB", format((4.5 * 1024L * 1024L).toLong()))
        assertEquals("1.2 GB", format((1.2 * 1024L * 1024L * 1024L).toLong()))
    }
}
