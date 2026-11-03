package app.vidown.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

enum class HistoryFilter {
    ALL, VIDEO, AUDIO, FAILED
}

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
    val selectedSubtitleLangs: Set<String> = setOf("en"),
    val subtitleDeliveryMode: String = "embed",
    val subtitleFormat: String = "srt",
    val isSubtitleSheetOpen: Boolean = false,
    val isStartingDownload: Boolean = false,
    val selectedPlaylistItems: Set<String> = emptySet(),
    val selectedBatchPreset: BatchPreset = DEFAULT_BATCH_PRESETS[0],
    val embedBatchSubtitles: Boolean = false,
    val batchSubtitleLangs: Set<String> = setOf("en"),
    val batchSubtitleDeliveryMode: String = "embed",
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

    private val _historySearchQuery = MutableStateFlow("")
    val historySearchQuery: StateFlow<String> = _historySearchQuery.asStateFlow()

    private val _historyFilter = MutableStateFlow(HistoryFilter.ALL)
    val historyFilter: StateFlow<HistoryFilter> = _historyFilter.asStateFlow()

    val filteredHistoryTasks: StateFlow<List<DownloadTask>> = combine(
        repository.historyTasksFlow,
        _historySearchQuery,
        _historyFilter
    ) { tasks, query, filter ->
        tasks.filter { task ->
            val matchesFilter = when (filter) {
                HistoryFilter.ALL -> true
                HistoryFilter.VIDEO -> !task.isAudioOnly
                HistoryFilter.AUDIO -> task.isAudioOnly
                HistoryFilter.FAILED -> task.status == DownloadStatus.FAILED
            }
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                task.title.contains(query, ignoreCase = true) ||
                    task.uploader.contains(query, ignoreCase = true)
            }
            matchesFilter && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setHistorySearchQuery(query: String) {
        _historySearchQuery.value = query
    }

    fun setHistoryFilter(filter: HistoryFilter) {
        _historyFilter.value = filter
    }

    val maxConcurrentDownloads: StateFlow<Int> = appPreferences.maxConcurrentDownloads
    val useCookies: StateFlow<Boolean> = appPreferences.useCookies
    val useCustomStorage: StateFlow<Boolean> = appPreferences.useCustomStorage
    val customStorageUri: StateFlow<String?> = appPreferences.customStorageUri
    val organizeByCreator: StateFlow<Boolean> = appPreferences.organizeByCreator
    val downloadOnlyOnWifi: StateFlow<Boolean> = appPreferences.downloadOnlyOnWifi
    val downloadSpeedLimitBytes: StateFlow<Long> = appPreferences.downloadSpeedLimitBytes
    val defaultSubtitleDeliveryMode: StateFlow<String> = appPreferences.defaultSubtitleDeliveryMode
    val defaultSubtitleFormat: StateFlow<String> = appPreferences.defaultSubtitleFormat
    val includeAutoCaptions: StateFlow<Boolean> = appPreferences.includeAutoCaptions
    val preferredSubtitleLangs: StateFlow<Set<String>> = appPreferences.preferredSubtitleLangs

    fun setDefaultSubtitleDeliveryMode(mode: String) = appPreferences.setDefaultSubtitleDeliveryMode(mode)
    fun setDefaultSubtitleFormat(format: String) = appPreferences.setDefaultSubtitleFormat(format)
    fun setIncludeAutoCaptions(enabled: Boolean) = appPreferences.setIncludeAutoCaptions(enabled)
    fun setPreferredSubtitleLangs(langs: Set<String>) = appPreferences.setPreferredSubtitleLangs(langs)

    private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val _isWifiConnected = MutableStateFlow(checkWifiConnected())
    val isWifiConnected: StateFlow<Boolean> = _isWifiConnected.asStateFlow()

    private fun checkWifiConnected(): Boolean {
        val net = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(net) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    private val _cookieSummary = MutableStateFlow<CookieSummary?>(cookieManager.getCookieSummary())
    val cookieSummary: StateFlow<CookieSummary?> = _cookieSummary.asStateFlow()

    private val _ytdlpVersion = MutableStateFlow("Loading...")
    val ytdlpVersion: StateFlow<String> = _ytdlpVersion.asStateFlow()

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()

    private val _updateMessage = MutableStateFlow<String?>(null)
    val updateMessage: StateFlow<String?> = _updateMessage.asStateFlow()

    private val _cacheSizeBytes = MutableStateFlow(0L)
    val cacheSizeBytes: StateFlow<Long> = _cacheSizeBytes.asStateFlow()

    init {
        loadVersion()
        syncCookiesToEngine()
        registerNetworkCallback()
        viewModelScope.launch(Dispatchers.IO) {
            pruneOrphanedTempDirs()
            refreshCacheSize()
        }
    }

    private fun registerNetworkCallback() {
        val req = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        try {
            connectivityManager.registerNetworkCallback(req, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isWifiConnected.value = checkWifiConnected()
                }

                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    _isWifiConnected.value = checkWifiConnected()
                }

                override fun onLost(network: Network) {
                    _isWifiConnected.value = checkWifiConnected()
                }
            })
        } catch (_: Exception) {}
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
                        val defaultFormat = probe.formats.firstOrNull { it.id == "bestvideo*+bestaudio/best" }
                            ?: probe.formats.firstOrNull { !it.isAudioOnly }
                            ?: probe.formats.firstOrNull()

                        val preferred = appPreferences.getPreferredSubtitleLangs()
                        val available = probe.subtitleTracks.map { it.code }
                        val matchingLangs = preferred.filter { it in available }.toSet().ifEmpty {
                            available.take(1).toSet().ifEmpty { setOf("en") }
                        }
                        val defaultDelivery = appPreferences.getDefaultSubtitleDeliveryMode()
                        val defaultSubFormat = appPreferences.getDefaultSubtitleFormat()

                        _homeState.value = _homeState.value.copy(
                            probeState = ProbeUiState.Success(probe),
                            selectedFormat = defaultFormat,
                            selectedSubtitleLangs = matchingLangs,
                            subtitleDeliveryMode = defaultDelivery,
                            subtitleFormat = defaultSubFormat,
                            isPlaylistSheetOpen = false
                        )
                    }
                    is ProbeData.Playlist -> {
                        val playlist = probeData.playlist
                        val allIds = playlist.entries.map { it.id }.toSet()
                        val preferred = appPreferences.getPreferredSubtitleLangs()
                        val defaultDelivery = appPreferences.getDefaultSubtitleDeliveryMode()
                        _homeState.value = _homeState.value.copy(
                            probeState = ProbeUiState.Playlist(playlist),
                            selectedPlaylistItems = allIds,
                            selectedBatchPreset = DEFAULT_BATCH_PRESETS[0],
                            embedBatchSubtitles = false,
                            batchSubtitleLangs = preferred,
                            batchSubtitleDeliveryMode = defaultDelivery,
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

    fun setBatchSubtitleDeliveryMode(mode: String) {
        _homeState.value = _homeState.value.copy(batchSubtitleDeliveryMode = mode)
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
                    subtitleLang = if (!preset.isAudioOnly && currentState.embedBatchSubtitles) currentState.batchSubtitleLangs.joinToString(",") else null,
                    subtitleDeliveryMode = currentState.batchSubtitleDeliveryMode,
                    subtitleFormat = currentState.subtitleFormat,
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

    fun openSubtitleSheet() {
        _homeState.value = _homeState.value.copy(isSubtitleSheetOpen = true)
    }

    fun dismissSubtitleSheet() {
        _homeState.value = _homeState.value.copy(isSubtitleSheetOpen = false)
    }

    fun toggleSubtitleLanguage(langCode: String) {
        val current = _homeState.value.selectedSubtitleLangs.toMutableSet()
        if (current.contains(langCode)) {
            current.remove(langCode)
        } else {
            current.add(langCode)
        }
        _homeState.value = _homeState.value.copy(selectedSubtitleLangs = current)
    }

    fun selectAllSubtitleLanguages(langs: List<String>) {
        _homeState.value = _homeState.value.copy(selectedSubtitleLangs = langs.toSet())
    }

    fun clearAllSubtitleLanguages() {
        _homeState.value = _homeState.value.copy(selectedSubtitleLangs = emptySet())
    }

    fun setSubtitleDeliveryMode(mode: String) {
        _homeState.value = _homeState.value.copy(subtitleDeliveryMode = mode)
    }

    fun setSubtitleFormat(format: String) {
        _homeState.value = _homeState.value.copy(subtitleFormat = format)
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
                subtitleLang = if (!format.isAudioOnly && currentState.embedSubtitles) currentState.selectedSubtitleLangs.joinToString(",") else null,
                subtitleDeliveryMode = currentState.subtitleDeliveryMode,
                subtitleFormat = currentState.subtitleFormat,
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

    fun deleteTask(taskId: String, deleteFiles: Boolean = false) {
        viewModelScope.launch {
            repository.deleteTask(taskId, deleteFiles)
        }
    }

    fun deleteTasks(taskIds: Collection<String>, deleteFiles: Boolean = false) {
        viewModelScope.launch {
            repository.deleteTasks(taskIds, deleteFiles)
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

    fun setDownloadOnlyOnWifi(enabled: Boolean) {
        appPreferences.setDownloadOnlyOnWifi(enabled)
    }

    fun setDownloadSpeedLimitBytes(bytes: Long) {
        appPreferences.setDownloadSpeedLimitBytes(bytes)
    }

    fun refreshCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            _cacheSizeBytes.value = calculateDirectorySize(getApplication<Application>().cacheDir)
        }
    }

    fun clearAppCache(onCleared: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val cacheRoot = app.cacheDir
            val activeIds = repository.tasksFlow.value
                .filter {
                    it.status == DownloadStatus.QUEUED ||
                    it.status == DownloadStatus.DOWNLOADING ||
                    it.status == DownloadStatus.PROCESSING ||
                    it.status == DownloadStatus.PAUSED
                }
                .map { it.id }
                .toSet()

            cacheRoot.listFiles()?.forEach { file ->
                if (file.name == "downloads") {
                    file.listFiles()?.forEach { taskDir ->
                        if (taskDir.name !in activeIds) {
                            taskDir.deleteRecursively()
                        }
                    }
                } else {
                    file.deleteRecursively()
                }
            }
            _cacheSizeBytes.value = calculateDirectorySize(cacheRoot)
            launch(Dispatchers.Main) {
                onCleared()
            }
        }
    }

    private fun pruneOrphanedTempDirs() {
        val app = getApplication<Application>()
        val downloadsDir = File(app.cacheDir, "downloads")
        if (!downloadsDir.exists() || !downloadsDir.isDirectory) return

        val activeIds = repository.tasksFlow.value
            .filter {
                it.status == DownloadStatus.QUEUED ||
                it.status == DownloadStatus.DOWNLOADING ||
                it.status == DownloadStatus.PROCESSING ||
                it.status == DownloadStatus.PAUSED
            }
            .map { it.id }
            .toSet()

        downloadsDir.listFiles()?.forEach { taskDir ->
            if (taskDir.isDirectory && taskDir.name !in activeIds) {
                taskDir.deleteRecursively()
            }
        }
    }

    private fun calculateDirectorySize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        if (dir.isFile) return dir.length()
        var size = 0L
        dir.listFiles()?.forEach { child ->
            size += if (child.isDirectory) calculateDirectorySize(child) else child.length()
        }
        return size
    }

    private fun extractUrl(text: String): String? {
        val regex = Regex("""https?://[^\s]+""")
        return regex.find(text)?.value
    }
}
