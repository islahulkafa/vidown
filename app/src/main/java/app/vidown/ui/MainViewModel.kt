package app.vidown.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.vidown.data.model.DownloadStatus
import app.vidown.data.model.DownloadTask
import app.vidown.data.repository.DownloadRepository
import app.vidown.download.DownloadService
import app.vidown.python.MediaFormat
import app.vidown.python.ProbeResult
import app.vidown.python.PythonBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface ProbeUiState {
    data object Idle : ProbeUiState
    data object Loading : ProbeUiState
    data class Success(val probe: ProbeResult) : ProbeUiState
    data class Error(val message: String) : ProbeUiState
}

data class HomeUiState(
    val urlInput: String = "",
    val probeState: ProbeUiState = ProbeUiState.Idle,
    val selectedFormat: MediaFormat? = null,
    val isStartingDownload: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DownloadRepository.getInstance(application)
    private val pythonBridge = PythonBridge.getInstance(application)

    private val _homeState = MutableStateFlow(HomeUiState())
    val homeState: StateFlow<HomeUiState> = _homeState.asStateFlow()

    val activeTasks: StateFlow<List<DownloadTask>> = repository.activeTasksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historyTasks: StateFlow<List<DownloadTask>> = repository.historyTasksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _ytdlpVersion = MutableStateFlow("Loading...")
    val ytdlpVersion: StateFlow<String> = _ytdlpVersion.asStateFlow()

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()

    private val _updateMessage = MutableStateFlow<String?>(null)
    val updateMessage: StateFlow<String?> = _updateMessage.asStateFlow()

    init {
        loadVersion()
    }

    fun loadVersion() {
        viewModelScope.launch {
            _ytdlpVersion.value = pythonBridge.getYtdlpVersion()
        }
    }

    fun onUrlChanged(newUrl: String) {
        _homeState.value = _homeState.value.copy(
            urlInput = newUrl,
            probeState = ProbeUiState.Idle,
            selectedFormat = null
        )
    }

    fun handleSharedUrl(sharedText: String) {
        val extractedUrl = extractUrl(sharedText)
        if (extractedUrl != null) {
            onUrlChanged(extractedUrl)
            probeUrl()
        }
    }

    fun probeUrl() {
        val url = _homeState.value.urlInput.trim()
        if (url.isBlank()) return

        _homeState.value = _homeState.value.copy(
            probeState = ProbeUiState.Loading,
            selectedFormat = null
        )

        viewModelScope.launch {
            val result = pythonBridge.probe(url)
            result.onSuccess { probe ->
                val defaultFormat = probe.formats.firstOrNull { it.id == "b" }
                    ?: probe.formats.firstOrNull()

                _homeState.value = _homeState.value.copy(
                    probeState = ProbeUiState.Success(probe),
                    selectedFormat = defaultFormat
                )
            }.onFailure { error ->
                _homeState.value = _homeState.value.copy(
                    probeState = ProbeUiState.Error(error.message ?: "Failed to probe media info")
                )
            }
        }
    }

    fun selectFormat(format: MediaFormat) {
        _homeState.value = _homeState.value.copy(selectedFormat = format)
    }

    fun startDownload(onEnqueued: () -> Unit) {
        val currentState = _homeState.value
        val probe = (currentState.probeState as? ProbeUiState.Success)?.probe ?: return
        val format = currentState.selectedFormat ?: return

        viewModelScope.launch {
            val taskId = UUID.randomUUID().toString()
            val task = DownloadTask(
                id = taskId,
                url = probe.webpageUrl,
                title = probe.title,
                thumbnailUrl = probe.thumbnailUrl,
                uploader = probe.uploader,
                durationSeconds = probe.durationSeconds,
                formatId = format.id,
                formatLabel = format.label,
                isAudioOnly = format.isAudioOnly,
                status = DownloadStatus.QUEUED
            )

            repository.enqueue(task)
            DownloadService.enqueue(getApplication())

            _homeState.value = HomeUiState()
            onEnqueued()
        }
    }

    fun cancelTask(taskId: String) {
        viewModelScope.launch {
            DownloadService.cancel(getApplication(), taskId)
            val current = repository.getTask(taskId)
            if (current != null) {
                repository.updateTask(current.copy(status = DownloadStatus.CANCELLED))
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
        }
    }

    fun checkYtdlpUpdate() {
        if (_isUpdating.value) return
        _isUpdating.value = true
        _updateMessage.value = null

        viewModelScope.launch {
            val res = pythonBridge.updateYtdlp()
            _isUpdating.value = false
            if (res.isSuccess) {
                if (res.isUpToDate) {
                    _updateMessage.value = "yt-dlp is already up to date (${res.newVersion})"
                } else {
                    _ytdlpVersion.value = res.newVersion
                    _updateMessage.value = "Updated successfully to ${res.newVersion} (was ${res.previousVersion})"
                }
            } else {
                _updateMessage.value = "Update failed: ${res.errorMessage ?: "Unknown error"}"
            }
        }
    }

    fun clearUpdateMessage() {
        _updateMessage.value = null
    }

    private fun extractUrl(text: String): String? {
        val regex = Regex("""https?://[^\s]+""")
        return regex.find(text)?.value
    }
}
