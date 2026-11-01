package app.vidown.download

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object MediaStoreWriter {

    suspend fun saveMediaFile(
        context: Context,
        sourceFile: File,
        fileName: String,
        isAudioOnly: Boolean
    ): Uri? = withContext(Dispatchers.IO) {
        if (!sourceFile.exists()) return@withContext null

        val mimeType = resolveMimeType(fileName, isAudioOnly)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentUri = if (isAudioOnly) {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

            val relativePath = if (isAudioOnly) {
                "${Environment.DIRECTORY_MUSIC}/Vidown"
            } else {
                "${Environment.DIRECTORY_MOVIES}/Vidown"
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(contentUri, contentValues) ?: return@withContext null

            try {
                resolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(sourceFile).use { input ->
                        input.copyTo(out, bufferSize = 64 * 1024)
                    }
                }
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
                uri
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                null
            }
        } else {
            val publicDir = if (isAudioOnly) {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            } else {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            }
            val targetDir = File(publicDir, "Vidown").apply { mkdirs() }
            val targetFile = File(targetDir, fileName)

            FileInputStream(sourceFile).use { input ->
                FileOutputStream(targetFile).use { out ->
                    input.copyTo(out, bufferSize = 64 * 1024)
                }
            }

            var scannedUri: Uri? = null
            MediaScannerConnection.scanFile(
                context,
                arrayOf(targetFile.absolutePath),
                arrayOf(mimeType)
            ) { _, uri ->
                scannedUri = uri
            }
            scannedUri ?: Uri.fromFile(targetFile)
        }
    }

    private fun resolveMimeType(fileName: String, isAudioOnly: Boolean): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "mp4" -> "video/mp4"
            "webm" -> if (isAudioOnly) "audio/webm" else "video/webm"
            "mkv" -> "video/x-matroska"
            "m4a" -> "audio/mp4"
            "mp3" -> "audio/mpeg"
            "opus" -> "audio/opus"
            "aac" -> "audio/aac"
            "flac" -> "audio/flac"
            else -> if (isAudioOnly) "audio/*" else "video/*"
        }
    }
}
