package app.vidown.data.repository

import android.content.Context
import app.vidown.data.db.DownloadDatabase
import app.vidown.data.model.DownloadStatus
import app.vidown.data.model.DownloadTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DownloadRepository private constructor(context: Context) {

    private val db = DownloadDatabase(context.applicationContext)
    private val scope = CoroutineScope(Dispatchers.IO)
    private val mutex = Mutex()

    private val _tasksFlow = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasksFlow: StateFlow<List<DownloadTask>> = _tasksFlow.asStateFlow()

    val activeTasksFlow = _tasksFlow.map { list ->
        list.filter {
            it.status == DownloadStatus.QUEUED ||
            it.status == DownloadStatus.DOWNLOADING ||
            it.status == DownloadStatus.PROCESSING ||
            it.status == DownloadStatus.PAUSED
        }
    }

    val historyTasksFlow = _tasksFlow.map { list ->
        list.filter { it.status == DownloadStatus.COMPLETED || it.status == DownloadStatus.FAILED || it.status == DownloadStatus.CANCELLED }
    }

    init {
        scope.launch {
            refresh()
        }
    }

    private suspend fun refresh() {
        mutex.withLock {
            val list = db.getAllTasks()
            _tasksFlow.value = list
        }
    }

    suspend fun enqueue(task: DownloadTask) {
        mutex.withLock {
            db.insertOrUpdate(task)
            val updated = db.getAllTasks()
            _tasksFlow.value = updated
        }
    }

    suspend fun enqueueAll(tasks: List<DownloadTask>) {
        if (tasks.isEmpty()) return
        mutex.withLock {
            db.insertOrUpdateAll(tasks)
            val updated = db.getAllTasks()
            _tasksFlow.value = updated
        }
    }

    suspend fun updateTask(task: DownloadTask) {
        mutex.withLock {
            db.insertOrUpdate(task)
            val updated = db.getAllTasks()
            _tasksFlow.value = updated
        }
    }

    suspend fun updateProgress(
        taskId: String,
        percent: Float,
        speedBytes: Double,
        etaSeconds: Int,
        status: DownloadStatus
    ) {
        mutex.withLock {
            val current = _tasksFlow.value.find { it.id == taskId } ?: return@withLock
            val updated = current.copy(
                progressPercent = percent,
                speedBytesPerSec = speedBytes,
                etaSeconds = etaSeconds,
                status = status
            )
            db.insertOrUpdate(updated)
            _tasksFlow.value = _tasksFlow.value.map { if (it.id == taskId) updated else it }
        }
    }

    suspend fun pauseTask(taskId: String) {
        mutex.withLock {
            val current = _tasksFlow.value.find { it.id == taskId } ?: return@withLock
            val updated = current.copy(status = DownloadStatus.PAUSED, speedBytesPerSec = 0.0, etaSeconds = 0)
            db.insertOrUpdate(updated)
            _tasksFlow.value = _tasksFlow.value.map { if (it.id == taskId) updated else it }
        }
    }

    suspend fun resumeTask(taskId: String) {
        mutex.withLock {
            val current = _tasksFlow.value.find { it.id == taskId } ?: return@withLock
            val updated = current.copy(status = DownloadStatus.QUEUED)
            db.insertOrUpdate(updated)
            _tasksFlow.value = _tasksFlow.value.map { if (it.id == taskId) updated else it }
        }
    }

    suspend fun retryTask(taskId: String) {
        mutex.withLock {
            val current = _tasksFlow.value.find { it.id == taskId } ?: return@withLock
            val updated = current.copy(
                status = DownloadStatus.QUEUED,
                progressPercent = 0f,
                speedBytesPerSec = 0.0,
                etaSeconds = 0,
                errorMessage = null,
                completedAt = null
            )
            db.insertOrUpdate(updated)
            _tasksFlow.value = _tasksFlow.value.map { if (it.id == taskId) updated else it }
        }
    }

    suspend fun deleteTask(taskId: String) {
        mutex.withLock {
            db.deleteTask(taskId)
            _tasksFlow.value = _tasksFlow.value.filter { it.id != taskId }
        }
    }

    fun getTask(taskId: String): DownloadTask? {
        return _tasksFlow.value.find { it.id == taskId }
    }

    companion object {
        @Volatile
        private var instance: DownloadRepository? = null

        fun getInstance(context: Context): DownloadRepository {
            return instance ?: synchronized(this) {
                instance ?: DownloadRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
