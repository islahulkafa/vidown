package app.vidown.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import app.vidown.MainActivity
import app.vidown.data.model.DownloadStatus
import app.vidown.data.repository.DownloadRepository
import app.vidown.python.ProgressCallback
import app.vidown.python.PythonBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class DownloadService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: DownloadRepository
    private lateinit var pythonBridge: PythonBridge
    private lateinit var notificationManager: NotificationManager

    private val activeJobs = ConcurrentHashMap<String, Job>()
    private var isRunningQueue = false

    override fun onCreate() {
        super.onCreate()
        repository = DownloadRepository.getInstance(this)
        pythonBridge = PythonBridge.getInstance(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ENQUEUE -> {
                startForegroundNotification()
                triggerQueueProcessing()
            }
            ACTION_CANCEL -> {
                val taskId = intent.getStringExtra(EXTRA_TASK_ID)
                if (taskId != null) {
                    cancelTask(taskId)
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_DOWNLOADS)
            .setContentTitle("Vidown")
            .setContentText("Processing downloads...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }

        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, foregroundServiceType)
    }

    private fun triggerQueueProcessing() {
        if (isRunningQueue) return
        isRunningQueue = true

        serviceScope.launch {
            while (true) {
                val tasks = repository.tasksFlow.first()
                val queuedTask = tasks.firstOrNull { it.status == DownloadStatus.QUEUED }
                if (queuedTask == null) {
                    break
                }
                processTask(queuedTask.id)
            }
            isRunningQueue = false
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
        }
    }

    private suspend fun processTask(taskId: String) {
        val task = repository.getTask(taskId) ?: return
        val job = serviceScope.launch {
            try {
                repository.updateProgress(taskId, 0f, 0.0, 0, DownloadStatus.DOWNLOADING)
                updateTaskNotification(task.title, 0, "Starting download...")

                val tempDir = File(cacheDir, "downloads/$taskId").apply { mkdirs() }

                val callback = object : ProgressCallback {
                    private var lastNotificationTime = 0L

                    override fun onProgress(
                        taskId: String,
                        percent: Double,
                        speedBytes: Double,
                        etaSeconds: Int,
                        status: String
                    ) {
                        serviceScope.launch {
                            val dlStatus = if (status == "processing") DownloadStatus.PROCESSING else DownloadStatus.DOWNLOADING
                            repository.updateProgress(taskId, percent.toFloat(), speedBytes, etaSeconds, dlStatus)

                            val now = System.currentTimeMillis()
                            if (now - lastNotificationTime > 800) {
                                lastNotificationTime = now
                                val speedText = formatSpeed(speedBytes)
                                val statusText = if (dlStatus == DownloadStatus.PROCESSING) {
                                    "Processing / Multiplexing..."
                                } else {
                                    "$speedText • ETA ${formatEta(etaSeconds)}"
                                }
                                updateTaskNotification(task.title, percent.toInt(), statusText, taskId)
                            }
                        }
                    }
                }

                val result = pythonBridge.download(
                    taskId = taskId,
                    url = task.url,
                    formatId = task.formatId,
                    outputDir = tempDir,
                    callback = callback,
                    audioCodec = task.audioCodec,
                    embedSubtitles = task.embedSubtitles,
                    subtitleLang = task.subtitleLang
                )

                if (result.isSuccess && result.filePath != null) {
                    repository.updateProgress(taskId, 100f, 0.0, 0, DownloadStatus.PROCESSING)
                    updateTaskNotification(task.title, 100, "Saving to media storage...", taskId)

                    val downloadedFile = File(result.filePath)
                    val fileName = result.fileName ?: downloadedFile.name
                    val savedUri = MediaStoreWriter.saveMediaFile(
                        context = this@DownloadService,
                        sourceFile = downloadedFile,
                        fileName = fileName,
                        isAudioOnly = task.isAudioOnly
                    )

                    downloadedFile.delete()
                    tempDir.deleteRecursively()

                    val completedTask = task.copy(
                        status = DownloadStatus.COMPLETED,
                        progressPercent = 100f,
                        fileSize = result.fileSize,
                        localFilePath = result.filePath,
                        mediaStoreUri = savedUri?.toString(),
                        completedAt = System.currentTimeMillis()
                    )
                    repository.updateTask(completedTask)
                    showCompletionNotification(task.title, true)
                } else if (result.isCancelled) {
                    tempDir.deleteRecursively()
                    val cancelledTask = task.copy(
                        status = DownloadStatus.CANCELLED,
                        completedAt = System.currentTimeMillis()
                    )
                    repository.updateTask(cancelledTask)
                } else {
                    tempDir.deleteRecursively()
                    val failedTask = task.copy(
                        status = DownloadStatus.FAILED,
                        errorMessage = result.errorMessage ?: "Unknown error",
                        completedAt = System.currentTimeMillis()
                    )
                    repository.updateTask(failedTask)
                    showCompletionNotification(task.title, false, result.errorMessage)
                }
            } catch (e: Exception) {
                val failedTask = task.copy(
                    status = DownloadStatus.FAILED,
                    errorMessage = e.message ?: e.toString(),
                    completedAt = System.currentTimeMillis()
                )
                repository.updateTask(failedTask)
                showCompletionNotification(task.title, false, e.message)
            } finally {
                activeJobs.remove(taskId)
            }
        }

        activeJobs[taskId] = job
        job.join()
    }

    private fun cancelTask(taskId: String) {
        pythonBridge.cancel(taskId)
        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)
    }

    private fun updateTaskNotification(title: String, progress: Int, statusText: String, taskId: String? = null) {
        val cancelPendingIntent = taskId?.let {
            val cancelIntent = Intent(this, DownloadService::class.java).apply {
                action = ACTION_CANCEL
                putExtra(EXTRA_TASK_ID, it)
            }
            PendingIntent.getService(
                this,
                it.hashCode(),
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_DOWNLOADS)
            .setContentTitle(title)
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, progress <= 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        if (cancelPendingIntent != null) {
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent)
        }

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun showCompletionNotification(title: String, success: Boolean, error: String? = null) {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_COMPLETED)
            .setContentTitle(if (success) "Download Complete" else "Download Failed")
            .setContentText(if (success) title else "$title: ${error ?: "Unknown error"}")
            .setSmallIcon(if (success) android.R.drawable.stat_sys_download_done else android.R.drawable.stat_notify_error)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notifyId = (System.currentTimeMillis() % 10000).toInt() + 100
        notificationManager.notify(notifyId, builder.build())
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val downloadChannel = NotificationChannel(
                CHANNEL_DOWNLOADS,
                "Active Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time download progress"
                setShowBadge(false)
            }

            val completedChannel = NotificationChannel(
                CHANNEL_COMPLETED,
                "Completed Downloads",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for finished or failed downloads"
            }

            notificationManager.createNotificationChannel(downloadChannel)
            notificationManager.createNotificationChannel(completedChannel)
        }
    }

    private fun formatSpeed(bytesPerSec: Double): String {
        return when {
            bytesPerSec >= 1024 * 1024 -> String.format("%.1f MB/s", bytesPerSec / (1024 * 1024))
            bytesPerSec >= 1024 -> String.format("%.0f KB/s", bytesPerSec / 1024)
            else -> String.format("%.0f B/s", bytesPerSec)
        }
    }

    private fun formatEta(seconds: Int): String {
        if (seconds <= 0) return "--"
        val m = seconds / 60
        val s = seconds % 60
        return if (m > 0) "${m}m ${s}s" else "${s}s"
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_DOWNLOADS = "downloads_channel"
        const val CHANNEL_COMPLETED = "completed_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_ENQUEUE = "app.vidown.action.ENQUEUE"
        const val ACTION_CANCEL = "app.vidown.action.CANCEL"
        const val EXTRA_TASK_ID = "extra_task_id"

        fun enqueue(context: Context) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_ENQUEUE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun cancel(context: Context, taskId: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_CANCEL
                putExtra(EXTRA_TASK_ID, taskId)
            }
            context.startService(intent)
        }
    }
}
