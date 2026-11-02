package app.vidown.python

import android.content.Context
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

interface ProgressCallback {
    fun onProgress(taskId: String, percent: Double, speedBytes: Double, etaSeconds: Int, status: String)
}

data class ProbeResult(
    val id: String,
    val title: String,
    val durationSeconds: Long,
    val thumbnailUrl: String,
    val uploader: String,
    val webpageUrl: String,
    val formats: List<MediaFormat>,
    val hasSubtitles: Boolean = false,
    val availableSubtitles: List<String> = emptyList()
)

data class MediaFormat(
    val id: String,
    val label: String,
    val note: String,
    val isAudioOnly: Boolean,
    val audioCodec: String? = null
)

data class DownloadResult(
    val isSuccess: Boolean,
    val isCancelled: Boolean,
    val filePath: String? = null,
    val fileName: String? = null,
    val fileSize: Long = 0L,
    val errorMessage: String? = null
)

data class UpdateResult(
    val isSuccess: Boolean,
    val isUpToDate: Boolean = false,
    val previousVersion: String = "",
    val newVersion: String = "",
    val errorMessage: String? = null
)

class PythonBridge private constructor(private val appContext: Context) {

    private val bridgeModule: PyObject

    init {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(appContext))
        }
        val py = Python.getInstance()
        bridgeModule = py.getModule("vidown_bridge")

        val nativeDir = appContext.applicationInfo.nativeLibraryDir
        val ffmpegPath = File(nativeDir, "libffmpeg.so").absolutePath
        val qjsPath = File(nativeDir, "libqjs.so").absolutePath
        val cacheDir = File(appContext.cacheDir, "downloads").apply { mkdirs() }.absolutePath
        val updateDir = File(appContext.filesDir, "ytdlp_update").apply { mkdirs() }.absolutePath

        bridgeModule.callAttr("configure", ffmpegPath, qjsPath, cacheDir, updateDir)
    }

    suspend fun getYtdlpVersion(): String = withContext(Dispatchers.IO) {
        bridgeModule.callAttr("get_ytdlp_version").toString()
    }

    suspend fun probe(url: String): Result<ProbeResult> = withContext(Dispatchers.IO) {
        try {
            val jsonStr = bridgeModule.callAttr("probe_info", url).toString()
            val json = JSONObject(jsonStr)

            if (json.optString("status") != "ok") {
                val err = json.optString("error", "Unknown extraction error")
                return@withContext Result.failure(Exception(err))
            }

            val data = json.getJSONObject("data")
            val formatsJson = data.getJSONArray("formats")
            val formats = mutableListOf<MediaFormat>()
            for (i in 0 until formatsJson.length()) {
                val f = formatsJson.getJSONObject(i)
                val audioCodecStr = if (!f.isNull("audio_codec")) f.optString("audio_codec").ifEmpty { null } else null
                formats.add(
                    MediaFormat(
                        id = f.getString("id"),
                        label = f.getString("label"),
                        note = f.optString("note", ""),
                        isAudioOnly = f.optBoolean("is_audio_only", false),
                        audioCodec = audioCodecStr
                    )
                )
            }

            val subLangsJson = data.optJSONArray("available_subtitles")
            val subLangs = mutableListOf<String>()
            if (subLangsJson != null) {
                for (j in 0 until subLangsJson.length()) {
                    subLangs.add(subLangsJson.getString(j))
                }
            }
            val hasSubtitles = data.optBoolean("has_subtitles", false)

            val probe = ProbeResult(
                id = data.optString("id", ""),
                title = data.optString("title", "Unknown Title"),
                durationSeconds = data.optLong("duration", 0L),
                thumbnailUrl = data.optString("thumbnail", ""),
                uploader = data.optString("uploader", ""),
                webpageUrl = data.optString("webpage_url", url),
                formats = formats,
                hasSubtitles = hasSubtitles,
                availableSubtitles = subLangs
            )
            Result.success(probe)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun download(
        taskId: String,
        url: String,
        formatId: String,
        outputDir: File,
        callback: ProgressCallback?,
        audioCodec: String? = null,
        embedSubtitles: Boolean = false,
        subtitleLang: String? = null
    ): DownloadResult = withContext(Dispatchers.IO) {
        try {
            val pyCallback: Any? = callback?.let { cb ->
                object : Any() {
                    @Suppress("unused")
                    fun onProgress(tId: String, percent: Double, speed: Double, eta: Int, status: String) {
                        cb.onProgress(tId, percent, speed, eta, status)
                    }
                }
            }

            val jsonStr = bridgeModule.callAttr(
                "download_media",
                taskId,
                url,
                formatId,
                outputDir.absolutePath,
                pyCallback,
                audioCodec,
                embedSubtitles,
                subtitleLang
            ).toString()

            val json = JSONObject(jsonStr)
            val status = json.optString("status")

            when (status) {
                "ok" -> DownloadResult(
                    isSuccess = true,
                    isCancelled = false,
                    filePath = json.optString("filepath"),
                    fileName = json.optString("filename"),
                    fileSize = json.optLong("filesize", 0L)
                )
                "cancelled" -> DownloadResult(
                    isSuccess = false,
                    isCancelled = true,
                    errorMessage = "Download cancelled by user"
                )
                else -> DownloadResult(
                    isSuccess = false,
                    isCancelled = false,
                    errorMessage = json.optString("error", "Unknown download failure")
                )
            }
        } catch (e: Exception) {
            DownloadResult(
                isSuccess = false,
                isCancelled = false,
                errorMessage = e.message ?: e.toString()
            )
        }
    }

    fun cancel(taskId: String) {
        bridgeModule.callAttr("cancel_task", taskId)
    }

    suspend fun updateYtdlp(): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val jsonStr = bridgeModule.callAttr("update_ytdlp").toString()
            val json = JSONObject(jsonStr)
            val status = json.optString("status")

            when (status) {
                "ok" -> UpdateResult(
                    isSuccess = true,
                    isUpToDate = false,
                    previousVersion = json.optString("previous_version"),
                    newVersion = json.optString("new_version")
                )
                "up_to_date" -> UpdateResult(
                    isSuccess = true,
                    isUpToDate = true,
                    previousVersion = json.optString("version"),
                    newVersion = json.optString("version")
                )
                else -> UpdateResult(
                    isSuccess = false,
                    isUpToDate = false,
                    errorMessage = json.optString("error", "Update failed")
                )
            }
        } catch (e: Exception) {
            UpdateResult(
                isSuccess = false,
                previousVersion = "",
                newVersion = "",
                errorMessage = e.message ?: e.toString()
            )
        }
    }

    companion object {
        @Volatile
        private var instance: PythonBridge? = null

        fun getInstance(context: Context): PythonBridge {
            return instance ?: synchronized(this) {
                instance ?: PythonBridge(context.applicationContext).also { instance = it }
            }
        }
    }
}
