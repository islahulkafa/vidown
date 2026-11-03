package app.vidown.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vidown.data.model.DownloadStatus
import app.vidown.data.model.DownloadTask
import app.vidown.ui.HistoryFilter
import app.vidown.ui.MainViewModel
import app.vidown.ui.components.MediaThumbnail
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowCounterClockwise
import com.adamglin.phosphoricons.regular.CheckSquare
import com.adamglin.phosphoricons.regular.ClockCounterClockwise
import com.adamglin.phosphoricons.regular.MagnifyingGlass
import com.adamglin.phosphoricons.regular.Play
import com.adamglin.phosphoricons.regular.ShareNetwork
import com.adamglin.phosphoricons.regular.Square
import com.adamglin.phosphoricons.regular.Trash
import com.adamglin.phosphoricons.regular.X

@Composable
fun HistoryRoute(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val allTasks by viewModel.historyTasks.collectAsStateWithLifecycle()
    val filteredTasks by viewModel.filteredHistoryTasks.collectAsStateWithLifecycle()
    val searchQuery by viewModel.historySearchQuery.collectAsStateWithLifecycle()
    val activeFilter by viewModel.historyFilter.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val allCount = allTasks.size
    val videoCount = allTasks.count { !it.isAudioOnly }
    val audioCount = allTasks.count { it.isAudioOnly }
    val failedCount = allTasks.count { it.status == DownloadStatus.FAILED }

    HistoryScreen(
        allTasksCount = allCount,
        videoCount = videoCount,
        audioCount = audioCount,
        failedCount = failedCount,
        historyTasks = filteredTasks,
        searchQuery = searchQuery,
        activeFilter = activeFilter,
        onSearchQueryChange = viewModel::setHistorySearchQuery,
        onFilterChange = viewModel::setHistoryFilter,
        onOpenMedia = { uriStr, isAudio ->
            openMediaFile(context, uriStr, isAudio)
        },
        onShareMedia = { uriStr, isAudio, title ->
            shareMediaFile(context, uriStr, isAudio, title)
        },
        onDeleteTasks = { taskIds, deleteFiles ->
            viewModel.deleteTasks(taskIds, deleteFiles)
        },
        onRetryTask = viewModel::retryTask,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    allTasksCount: Int,
    videoCount: Int,
    audioCount: Int,
    failedCount: Int,
    historyTasks: List<DownloadTask>,
    searchQuery: String,
    activeFilter: HistoryFilter,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (HistoryFilter) -> Unit,
    onOpenMedia: (String, Boolean) -> Unit,
    onShareMedia: (String, Boolean, String) -> Unit,
    onDeleteTasks: (Collection<String>, Boolean) -> Unit,
    onRetryTask: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isSelectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedTaskIds by rememberSaveable { mutableStateOf(setOf<String>()) }
    var pendingDeleteTasks by remember { mutableStateOf<Set<String>?>(null) }
    var alsoDeleteFiles by remember { mutableStateOf(false) }

    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedTaskIds = emptySet()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = "${selectedTaskIds.size} selected",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSelectionMode = false
                            selectedTaskIds = emptySet()
                        }) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.X,
                                contentDescription = "Exit selection mode"
                            )
                        }
                    },
                    actions = {
                        val areAllSelected = historyTasks.isNotEmpty() && selectedTaskIds.size == historyTasks.size
                        IconButton(onClick = {
                            selectedTaskIds = if (areAllSelected) {
                                emptySet()
                            } else {
                                historyTasks.map { it.id }.toSet()
                            }
                        }) {
                            Icon(
                                imageVector = if (areAllSelected) PhosphorIcons.Regular.CheckSquare else PhosphorIcons.Regular.Square,
                                contentDescription = if (areAllSelected) "Deselect all" else "Select all"
                            )
                        }
                        IconButton(
                            onClick = {
                                if (selectedTaskIds.isNotEmpty()) {
                                    pendingDeleteTasks = selectedTaskIds
                                    alsoDeleteFiles = false
                                }
                            },
                            enabled = selectedTaskIds.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.Trash,
                                contentDescription = "Delete selected",
                                tint = if (selectedTaskIds.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = "History",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        if (allTasksCount > 0) {
                            IconButton(onClick = {
                                isSelectionMode = true
                                selectedTaskIds = emptySet()
                            }) {
                                Icon(
                                    imageVector = PhosphorIcons.Regular.CheckSquare,
                                    contentDescription = "Select items"
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (allTasksCount > 0) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    placeholder = {
                        Text("Search by title or creator...", style = MaterialTheme.typography.bodyMedium)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = PhosphorIcons.Regular.MagnifyingGlass,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = PhosphorIcons.Regular.X,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = activeFilter == HistoryFilter.ALL,
                        onClick = { onFilterChange(HistoryFilter.ALL) },
                        label = { Text("All ($allTasksCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    FilterChip(
                        selected = activeFilter == HistoryFilter.VIDEO,
                        onClick = { onFilterChange(HistoryFilter.VIDEO) },
                        label = { Text("Video ($videoCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    FilterChip(
                        selected = activeFilter == HistoryFilter.AUDIO,
                        onClick = { onFilterChange(HistoryFilter.AUDIO) },
                        label = { Text("Audio ($audioCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    FilterChip(
                        selected = activeFilter == HistoryFilter.FAILED,
                        onClick = { onFilterChange(HistoryFilter.FAILED) },
                        label = { Text("Failed ($failedCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    )
                }
            }

            if (allTasksCount == 0) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.ClockCounterClockwise,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "History is empty",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Downloaded files appear here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else if (historyTasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.MagnifyingGlass,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No matching downloads",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try adjusting your search or filter",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = {
                            onSearchQueryChange("")
                            onFilterChange(HistoryFilter.ALL)
                        }) {
                            Text("Reset filters")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = historyTasks,
                        key = { it.id }
                    ) { task ->
                        val isSelected = selectedTaskIds.contains(task.id)
                        HistoryTaskCard(
                            task = task,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            onToggleSelect = {
                                selectedTaskIds = if (isSelected) {
                                    selectedTaskIds - task.id
                                } else {
                                    selectedTaskIds + task.id
                                }
                            },
                            onEnterSelection = {
                                isSelectionMode = true
                                selectedTaskIds = setOf(task.id)
                            },
                            onOpen = { uri -> onOpenMedia(uri, task.isAudioOnly) },
                            onShare = { uri -> onShareMedia(uri, task.isAudioOnly, task.title) },
                            onDelete = {
                                pendingDeleteTasks = setOf(task.id)
                                alsoDeleteFiles = false
                            },
                            onRetry = { onRetryTask(task.id) }
                        )
                    }
                }
            }
        }
    }

    pendingDeleteTasks?.let { taskIds ->
        val isSingle = taskIds.size == 1
        val dialogTitle = if (isSingle) "Delete download?" else "Delete ${taskIds.size} downloads?"

        AlertDialog(
            onDismissRequest = { pendingDeleteTasks = null },
            title = {
                Text(text = dialogTitle, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = if (isSingle) {
                            "Are you sure you want to remove this item from your history?"
                        } else {
                            "Are you sure you want to remove these items from your history?"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = alsoDeleteFiles,
                            onCheckedChange = { alsoDeleteFiles = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Also delete files from device storage",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        onDeleteTasks(taskIds, alsoDeleteFiles)
                        if (isSelectionMode) {
                            selectedTaskIds = selectedTaskIds - taskIds
                            if (selectedTaskIds.isEmpty()) {
                                isSelectionMode = false
                            }
                        }
                        pendingDeleteTasks = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteTasks = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryTaskCard(
    task: DownloadTask,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onEnterSelection: () -> Unit,
    onOpen: (String) -> Unit,
    onShare: (String) -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelect()
                    }
                },
                onLongClick = {
                    if (!isSelectionMode) {
                        onEnterSelection()
                    }
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }

                val isSuccess = task.status == DownloadStatus.COMPLETED

                MediaThumbnail(
                    thumbnailUrl = task.thumbnailUrl,
                    mediaUri = task.mediaStoreUri ?: task.localFilePath,
                    isAudioOnly = task.isAudioOnly,
                    durationSeconds = task.durationSeconds,
                    modifier = Modifier.size(width = 72.dp, height = 48.dp),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val subtitle = if (isSuccess && task.fileSize > 0) {
                        "${task.formatLabel} • ${formatFileSize(task.fileSize)}"
                    } else if (!isSuccess) {
                        task.errorMessage ?: task.status.name
                    } else {
                        task.formatLabel
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSuccess) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!isSelectionMode) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.Trash,
                            contentDescription = "Delete Record",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            if (!isSelectionMode) {
                if (task.status == DownloadStatus.COMPLETED && !task.mediaStoreUri.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { onOpen(task.mediaStoreUri) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(PhosphorIcons.Regular.Play, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open")
                        }

                        OutlinedButton(
                            onClick = { onShare(task.mediaStoreUri) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(PhosphorIcons.Regular.ShareNetwork, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share")
                        }
                    }
                } else if (task.status == DownloadStatus.FAILED) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onRetry,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(PhosphorIcons.Regular.ArrowCounterClockwise, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
        bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
        bytes >= 1024 -> String.format("%.0f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}

private fun openMediaFile(context: Context, uriStr: String, isAudio: Boolean) {
    try {
        val uri = Uri.parse(uriStr)
        val mimeType = if (isAudio) "audio/*" else "video/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Open with"))
    } catch (_: Exception) {}
}

private fun shareMediaFile(context: Context, uriStr: String, isAudio: Boolean, title: String) {
    try {
        val uri = Uri.parse(uriStr)
        val mimeType = if (isAudio) "audio/*" else "video/*"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share media"))
    } catch (_: Exception) {}
}
