package app.vidown.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowCounterClockwise
import com.adamglin.phosphoricons.regular.DownloadSimple
import com.adamglin.phosphoricons.regular.Hourglass
import com.adamglin.phosphoricons.regular.Pause
import com.adamglin.phosphoricons.regular.Play
import com.adamglin.phosphoricons.regular.WifiSlash
import com.adamglin.phosphoricons.regular.X
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vidown.data.model.DownloadStatus
import app.vidown.data.model.DownloadTask
import app.vidown.ui.MainViewModel
import app.vidown.ui.components.MediaThumbnail

@Composable
fun QueueRoute(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val activeTasks by viewModel.activeTasks.collectAsStateWithLifecycle()
    val downloadOnlyOnWifi by viewModel.downloadOnlyOnWifi.collectAsStateWithLifecycle()
    val isWifiConnected by viewModel.isWifiConnected.collectAsStateWithLifecycle()

    QueueScreen(
        activeTasks = activeTasks,
        downloadOnlyOnWifi = downloadOnlyOnWifi,
        isWifiConnected = isWifiConnected,
        onCancelTask = viewModel::cancelTask,
        onPauseTask = viewModel::pauseTask,
        onResumeTask = viewModel::resumeTask,
        onRetryTask = viewModel::retryTask,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    activeTasks: List<DownloadTask>,
    downloadOnlyOnWifi: Boolean = false,
    isWifiConnected: Boolean = true,
    onCancelTask: (String) -> Unit,
    onPauseTask: (String) -> Unit,
    onResumeTask: (String) -> Unit,
    onRetryTask: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeCount = activeTasks.count { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PROCESSING }
    val queuedCount = activeTasks.count { it.status == DownloadStatus.QUEUED }
    val pausedCount = activeTasks.count { it.status == DownloadStatus.PAUSED }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Queue",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (activeTasks.isNotEmpty()) {
                            val subtitleParts = mutableListOf<String>()
                            if (activeCount > 0) subtitleParts.add("$activeCount active")
                            if (queuedCount > 0) subtitleParts.add("$queuedCount queued")
                            if (pausedCount > 0) subtitleParts.add("$pausedCount paused")
                            Text(
                                text = subtitleParts.joinToString(" • "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
    ) { innerPadding ->
        if (activeTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.Hourglass,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Queue is empty",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Paste a link to start",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (downloadOnlyOnWifi && !isWifiConnected) {
                    item(key = "wifi_warning_banner") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = PhosphorIcons.Regular.WifiSlash,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Waiting for Wi-Fi",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Downloads paused on mobile data. Reconnect to Wi-Fi to resume.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                items(
                    items = activeTasks,
                    key = { it.id }
                ) { task ->
                    ActiveTaskCard(
                        task = task,
                        onCancel = { onCancelTask(task.id) },
                        onPause = { onPauseTask(task.id) },
                        onResume = { onResumeTask(task.id) },
                        onRetry = { onRetryTask(task.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveTaskCard(
    task: DownloadTask,
    onCancel: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MediaThumbnail(
                    thumbnailUrl = task.thumbnailUrl,
                    mediaUri = task.localFilePath,
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
                    Text(
                        text = task.formatLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (task.status) {
                        DownloadStatus.DOWNLOADING -> {
                            IconButton(onClick = onPause) {
                                Icon(
                                    imageVector = PhosphorIcons.Regular.Pause,
                                    contentDescription = "Pause Download",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        DownloadStatus.PAUSED -> {
                            IconButton(onClick = onResume) {
                                Icon(
                                    imageVector = PhosphorIcons.Regular.Play,
                                    contentDescription = "Resume Download",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        DownloadStatus.FAILED -> {
                            IconButton(onClick = onRetry) {
                                Icon(
                                    imageVector = PhosphorIcons.Regular.ArrowCounterClockwise,
                                    contentDescription = "Retry Download",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        else -> {}
                    }

                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.X,
                            contentDescription = "Cancel Download",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (task.status) {
                DownloadStatus.PROCESSING -> {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                }
                DownloadStatus.PAUSED -> {
                    LinearProgressIndicator(
                        progress = { task.progressPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.outline,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                }
                else -> {
                    LinearProgressIndicator(
                        progress = { task.progressPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val statusText = when (task.status) {
                    DownloadStatus.QUEUED -> "Queued"
                    DownloadStatus.DOWNLOADING -> String.format("%.1f%%", task.progressPercent)
                    DownloadStatus.PROCESSING -> "Processing..."
                    DownloadStatus.PAUSED -> String.format("Paused (%.1f%%)", task.progressPercent)
                    DownloadStatus.FAILED -> "Failed"
                    else -> task.status.name
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (task.status == DownloadStatus.PAUSED) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                )

                if (task.status == DownloadStatus.DOWNLOADING && task.speedBytesPerSec > 0) {
                    val speedText = formatSpeed(task.speedBytesPerSec)
                    val etaText = formatEta(task.etaSeconds)
                    Text(
                        text = "$speedText • ETA $etaText",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
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
