package app.vidown.data.model

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PROCESSING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class DownloadTask(
    val id: String,
    val url: String,
    val title: String,
    val thumbnailUrl: String = "",
    val uploader: String = "",
    val durationSeconds: Long = 0L,
    val formatId: String = "bestvideo*+bestaudio/best",
    val formatLabel: String = "Best Quality",
    val isAudioOnly: Boolean = false,
    val audioCodec: String? = null,
    val embedSubtitles: Boolean = false,
    val subtitleLang: String? = null,
    val subtitleDeliveryMode: String = "embed",
    val subtitleFormat: String = "srt",
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val progressPercent: Float = 0f,
    val speedBytesPerSec: Double = 0.0,
    val etaSeconds: Int = 0,
    val fileSize: Long = 0L,
    val localFilePath: String? = null,
    val mediaStoreUri: String? = null,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
