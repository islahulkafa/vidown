package app.vidown.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.vidown.data.cookies.CookieManager
import app.vidown.data.cookies.CookieSummary
import app.vidown.data.model.DownloadStatus
import app.vidown.data.model.DownloadTask
import app.vidown.data.preferences.AppPreferences
import app.vidown.data.repository.DownloadRepository
import app.vidown.download.DownloadService
import app.vidown.python.BatchPreset
import app.vidown.python.DEFAULT_BATCH_PRESETS
import app.vidown.python.MediaFormat
import app.vidown.python.PlaylistProbeResult
import app.vidown.python.ProbeData
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
    data class Playlist(val playlist: PlaylistProbeResult) : ProbeUiState
    data class Error(val message: String, val isAuthError: Boolean = false) : ProbeUiState
}

data class HomeUiState(
    val urlInput: String = "",
    val probeState: ProbeUiState = ProbeUiState.Idle,
    val selectedFormat: MediaFormat? = null,
    val embedSubtitles: Boolean = false,
    val selectedSubtitleLang: String = "en",
    val isStartingDownload: Boolean = false,
    val selectedPlaylistItems: Set<String> = emptySet(),
    val selectedBatchPreset: BatchPreset = DEFAULT_BATCH_PRESETS[0],
    val embedBatchSubtitles: Boolean = false,
    val isPlaylistSheetOpen: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DownloadRepository.getInstance(application)
    private val pythonBridge = PythonBridge.getInstance(application)
    private val appPreferences = AppPreferences.getInstance(application)
    private val cookieManager = CookieManager.getInstance(application)

    private val _homeState = MutableStateFlow(HomeUiState())
    val homeState: StateFlow<HomeUiState> = _homeState.asStateFlow()

    val activeTasks: StateFlow<List<DownloadTask>> = repository.activeTasksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historyTasks: StateFlow<List<DownloadTask>> = repository.historyTasksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val maxConcurrentDownloads: StateFlow<Int> = appPreferences.maxConcurrentDownloads
    val useCookies: StateFlow<Boolean> = appPreferences.useCookies
    val useCustomStorage: StateFlow<Boolean> = appPreferences.useCustomStorage
    val customStorageUri: StateFlow<String?> = appPreferences.customStorageUri
    val organizeByCreator: StateFlow<Boolean> = appPreferences.organizeByCreator

    private val _cookieSummary = MutableStateFlow<CookieSummary?>(cookieManager.getCookieSummary())
    val cookieSummary: StateFlow<CookieSummary?> = _cookieSummary.asStateFlow()

    private val _ytdlpVersion = MutableStateFlow("Loading...")
    val ytdlpVersion: StateFlow<String> = _ytdlpVersion.asStateFlow()

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()

    private val _updateMessage = MutableStateFlow<String?>(null)
    val updateMessage: StateFlow<String?> = _updateMessage.asStateFlow()

    init {
        loadVersion()
        syncCookiesToEngine()
    }

    private fun syncCookiesToEngine() {
        val summary = _cookieSummary.value
        val path = if (appPreferences.isUseCookies() && summary != null) summary.filePath else null
        pythonBridge.setCookiesPath(path)
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
            selectedFormat = null,
            isPlaylistSheetOpen = false
        )

        viewModelScope.launch {
            val result = pythonBridge.probe(url)
            result.onSuccess { probeData ->
                when (probeData) {
                    is ProbeData.Single -> {
                        val probe = probeData.probe
                        val defaultFormat = probe.formats.firstOrNull { it.id == "b" }
                            ?: probe.formats.firstOrNull()

                        _homeState.value = _homeState.value.copy(
                            probeState = ProbeUiState.Success(probe),
                            selectedFormat = defaultFormat,
                            isPlaylistSheetOpen = false
                        )
                    }
                    is ProbeData.Playlist -> {
                        val playlist = probeData.playlist
                        val allIds = playlist.entries.map { it.id }.toSet()
                        _homeState.value = _homeState.value.copy(
                            probeState = ProbeUiState.Playlist(playlist),
                            selectedPlaylistItems = allIds,
                            selectedBatchPreset = DEFAULT_BATCH_PRESETS[0],
                            embedBatchSubtitles = false,
                            isPlaylistSheetOpen = true
                        )
                    }
                }
            }.onFailure { error ->
                val msg = error.message ?: "Failed to probe media info"
                val lower = msg.lowercase()
                val isAuth = lower.contains("sign in") || lower.contains("bot") ||
                    lower.contains("confirm your age") || lower.contains("login") ||
                    lower.contains("age-restricted") || lower.contains("private video") ||
                    lower.contains("members-only") || lower.contains("cookies")

                _homeState.value = _homeState.value.copy(
                    probeState = ProbeUiState.Error(message = msg, isAuthError = isAuth)
                )
            }
        }
    }

    fun dismissPlaylistSheet() {
        _homeState.value = _homeState.value.copy(isPlaylistSheetOpen = false)
    }

    fun openPlaylistSheet() {
        if (_homeState.value.probeState is ProbeUiState.Playlist) {
            _homeState.value = _homeState.value.copy(isPlaylistSheetOpen = true)
        }
    }

    fun togglePlaylistItem(itemId: String) {
        val current = _homeState.value.selectedPlaylistItems.toMutableSet()
        if (current.contains(itemId)) {
            current.remove(itemId)
        } else {
            current.add(itemId)
        }
        _homeState.value = _homeState.value.copy(selectedPlaylistItems = current)
    }

    fun selectAllPlaylistItems() {
        val playlist = (_homeState.value.probeState as? ProbeUiState.Playlist)?.playlist ?: return
        val allIds = playlist.entries.map { it.id }.toSet()
        _homeState.value = _homeState.value.copy(selectedPlaylistItems = allIds)
    }

    fun deselectAllPlaylistItems() {
        _homeState.value = _homeState.value.copy(selectedPlaylistItems = emptySet())
    }

    fun selectBatchPreset(preset: BatchPreset) {
        _homeState.value = _homeState.value.copy(selectedBatchPreset = preset)
    }

    fun toggleBatchSubtitles(enabled: Boolean) {
        _homeState.value = _homeState.value.copy(embedBatchSubtitles = enabled)
    }

    fun startBatchDownload(onEnqueued: () -> Unit) {
        val currentState = _homeState.value
        val playlist = (currentState.probeState as? ProbeUiState.Playlist)?.playlist ?: return
        val selectedIds = currentState.selectedPlaylistItems
        if (selectedIds.isEmpty()) return

        val selectedEntries = playlist.entries.filter { it.id in selectedIds }
        if (selectedEntries.isEmpty()) return

        val preset = currentState.selectedBatchPreset

        viewModelScope.launch {
            val tasks = selectedEntries.map { entry ->
                DownloadTask(
                    id = UUID.randomUUID().toString(),
                    url = entry.url,
                    title = entry.title,
                    thumbnailUrl = entry.thumbnailUrl,
                    uploader = entry.uploader.ifEmpty { playlist.uploader },
                    durationSeconds = entry.durationSeconds,
                    formatId = preset.id,
                    formatLabel = preset.label,
                    isAudioOnly = preset.isAudioOnly,
                    audioCodec = preset.audioCodec,
                    embedSubtitles = if (!preset.isAudioOnly) currentState.embedBatchSubtitles else false,
                    subtitleLang = if (!preset.isAudioOnly && currentState.embedBatchSubtitles) "en" else null,
                    status = DownloadStatus.QUEUED
                )
            }

            repository.enqueueAll(tasks)
            DownloadService.enqueue(getApplication())

            _homeState.value = HomeUiState()
            onEnqueued()
        }
    }

    fun selectFormat(format: MediaFormat) {
        _homeState.value = _homeState.value.copy(selectedFormat = format)
    }

    fun toggleEmbedSubtitles(enabled: Boolean) {
        _homeState.value = _homeState.value.copy(embedSubtitles = enabled)
    }

    fun selectSubtitleLang(lang: String) {
        _homeState.value = _homeState.value.copy(selectedSubtitleLang = lang)
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
                audioCodec = format.audioCodec,
                embedSubtitles = if (!format.isAudioOnly) currentState.embedSubtitles else false,
                subtitleLang = if (!format.isAudioOnly && currentState.embedSubtitles) currentState.selectedSubtitleLang else null,
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

    fun pauseTask(taskId: String) {
        viewModelScope.launch {
            DownloadService.pause(getApplication(), taskId)
            repository.pauseTask(taskId)
        }
    }

    fun resumeTask(taskId: String) {
        viewModelScope.launch {
            DownloadService.resume(getApplication(), taskId)
        }
    }

    fun retryTask(taskId: String) {
        viewModelScope.launch {
            DownloadService.retry(getApplication(), taskId)
        }
    }

    fun setMaxConcurrentDownloads(limit: Int) {
        appPreferences.setMaxConcurrentDownloads(limit)
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

    fun importCookieFile(uri: Uri, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = cookieManager.importFromFile(uri)
            res.onSuccess { summary ->
                _cookieSummary.value = summary
                syncCookiesToEngine()
                onResult(true, null)
            }.onFailure { err ->
                onResult(false, err.message)
            }
        }
    }

    fun importCookieText(rawText: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = cookieManager.importFromText(rawText)
            res.onSuccess { summary ->
                _cookieSummary.value = summary
                syncCookiesToEngine()
                onResult(true, null)
            }.onFailure { err ->
                onResult(false, err.message)
            }
        }
    }

    fun clearCookies() {
        cookieManager.clearCookies()
        _cookieSummary.value = null
        syncCookiesToEngine()
    }

    fun toggleUseCookies(enabled: Boolean) {
        appPreferences.setUseCookies(enabled)
        syncCookiesToEngine()
    }

    fun setCustomStorage(treeUri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            getApplication<Application>().contentResolver.takePersistableUriPermission(treeUri, flags)
        } catch (_: Exception) {}
        appPreferences.setCustomStorageUri(treeUri.toString())
        appPreferences.setUseCustomStorage(true)
    }

    fun setUseCustomStorage(enabled: Boolean) {
        appPreferences.setUseCustomStorage(enabled)
    }

    fun setOrganizeByCreator(enabled: Boolean) {
        appPreferences.setOrganizeByCreator(enabled)
    }

    private fun extractUrl(text: String): String? {
        val regex = Regex("""https?://[^\s]+""")
        return regex.find(text)?.value
    }
}
