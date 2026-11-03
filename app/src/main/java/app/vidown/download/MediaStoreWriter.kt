package app.vidown.download

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
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
        isAudioOnly: Boolean,
        uploader: String = "",
        customStorageTreeUri: Uri? = null,
        organizeByCreator: Boolean = false,
        companionFiles: List<File> = emptyList()
    ): Uri? = withContext(Dispatchers.IO) {
        if (!sourceFile.exists()) return@withContext null

        if (customStorageTreeUri != null) {
            val safResult = saveToSafTree(
                context = context,
                sourceFile = sourceFile,
                fileName = fileName,
                isAudioOnly = isAudioOnly,
                uploader = uploader,
                treeUri = customStorageTreeUri,
                organizeByCreator = organizeByCreator,
                companionFiles = companionFiles
            )
            if (safResult != null) {
                return@withContext safResult
            }
        }

        saveToMediaStore(context, sourceFile, fileName, isAudioOnly, companionFiles)
    }

    private fun saveToSafTree(
        context: Context,
        sourceFile: File,
        fileName: String,
        isAudioOnly: Boolean,
        uploader: String,
        treeUri: Uri,
        organizeByCreator: Boolean,
        companionFiles: List<File> = emptyList()
    ): Uri? {
        return try {
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
            if (rootDoc == null || !rootDoc.canWrite()) {
                return null
            }

            val mediaFolderName = if (isAudioOnly) "Music" else "Movies"
            val mediaDir = rootDoc.findFile(mediaFolderName)
                ?: rootDoc.createDirectory(mediaFolderName)
                ?: return null

            val targetDir = if (organizeByCreator && uploader.isNotBlank()) {
                val safeCreatorName = sanitizeFolderName(uploader)
                mediaDir.findFile(safeCreatorName)
                    ?: mediaDir.createDirectory(safeCreatorName)
                    ?: mediaDir
            } else {
                mediaDir
            }

            val mimeType = resolveMimeType(fileName, isAudioOnly)
            val existing = targetDir.findFile(fileName)
            if (existing != null && existing.isFile) {
                existing.delete()
            }

            val targetDoc = targetDir.createFile(mimeType, fileName) ?: return null

            context.contentResolver.openOutputStream(targetDoc.uri)?.use { outStream ->
                FileInputStream(sourceFile).use { inStream ->
                    inStream.copyTo(outStream, bufferSize = 64 * 1024)
                }
            } ?: return null

            for (comp in companionFiles) {
                if (comp.exists()) {
                    val compName = comp.name
                    val compMime = resolveMimeType(compName, false)
                    val existingComp = targetDir.findFile(compName)
                    if (existingComp != null && existingComp.isFile) {
                        existingComp.delete()
                    }
                    val compDoc = targetDir.createFile(compMime, compName)
                    if (compDoc != null) {
                        context.contentResolver.openOutputStream(compDoc.uri)?.use { outStream ->
                            FileInputStream(comp).use { inStream ->
                                inStream.copyTo(outStream, bufferSize = 64 * 1024)
                            }
                        }
                    }
                }
            }

            targetDoc.uri
        } catch (e: Exception) {
            null
        }
    }

    private fun sanitizeFolderName(name: String): String {
        return name.replace(Regex("""[\\/:*?"<>|]"""), "_").trim().ifEmpty { "Unknown" }
    }

    private fun saveToMediaStore(
        context: Context,
        sourceFile: File,
        fileName: String,
        isAudioOnly: Boolean,
        companionFiles: List<File> = emptyList()
    ): Uri? {
        val mimeType = resolveMimeType(fileName, isAudioOnly)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
            val uri = resolver.insert(contentUri, contentValues) ?: return null

            try {
                resolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(sourceFile).use { input ->
                        input.copyTo(out, bufferSize = 64 * 1024)
                    }
                }
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)

                for (comp in companionFiles) {
                    if (comp.exists()) {
                        val compValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, comp.name)
                            put(MediaStore.MediaColumns.MIME_TYPE, resolveMimeType(comp.name, false))
                            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                            put(MediaStore.MediaColumns.IS_PENDING, 1)
                        }
                        val compUri = resolver.insert(MediaStore.Files.getContentUri("external"), compValues)
                        if (compUri != null) {
                            try {
                                resolver.openOutputStream(compUri)?.use { out ->
                                    FileInputStream(comp).use { inp -> inp.copyTo(out, bufferSize = 64 * 1024) }
                                }
                                compValues.clear()
                                compValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                                resolver.update(compUri, compValues, null, null)
                            } catch (_: Exception) {
                                resolver.delete(compUri, null, null)
                            }
                        }
                    }
                }

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
            "srt" -> "application/x-subrip"
            "vtt" -> "text/vtt"
            else -> if (isAudioOnly) "audio/*" else "video/*"
        }
    }
}
