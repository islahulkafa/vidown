package app.vidown.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.adamglin.phosphoricons.regular.ClipboardText
import com.adamglin.phosphoricons.regular.DownloadSimple
import com.adamglin.phosphoricons.regular.MagnifyingGlass
import com.adamglin.phosphoricons.regular.MusicNote
import com.adamglin.phosphoricons.regular.Video
import com.adamglin.phosphoricons.regular.X
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vidown.python.MediaFormat
import app.vidown.python.ProbeResult
import app.vidown.ui.HomeUiState
import app.vidown.ui.MainViewModel
import app.vidown.ui.ProbeUiState

@Composable
fun HomeRoute(
    viewModel: MainViewModel,
    onNavigateToQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.homeState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    HomeScreen(
        uiState = uiState,
        onUrlChanged = viewModel::onUrlChanged,
        onProbeClicked = viewModel::probeUrl,
        onPasteClicked = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = clipboard.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val text = clipData.getItemAt(0).text?.toString() ?: ""
                viewModel.onUrlChanged(text)
                viewModel.probeUrl()
            }
        },
        onFormatSelected = viewModel::selectFormat,
        onDownloadClicked = {
            viewModel.startDownload(onEnqueued = onNavigateToQueue)
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onUrlChanged: (String) -> Unit,
    onProbeClicked: () -> Unit,
    onPasteClicked: () -> Unit,
    onFormatSelected: (MediaFormat) -> Unit,
    onDownloadClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Vidown",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "url_input_section") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.urlInput,
                        onValueChange = onUrlChanged,
                        placeholder = { Text("https://...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            if (uiState.urlInput.isNotEmpty()) {
                                IconButton(onClick = { onUrlChanged("") }) {
                                    Icon(PhosphorIcons.Regular.X, contentDescription = "Clear URL")
                                }
                            }
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onPasteClicked,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(PhosphorIcons.Regular.ClipboardText, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Paste")
                        }

                        Button(
                            onClick = onProbeClicked,
                            enabled = uiState.urlInput.isNotBlank() && uiState.probeState !is ProbeUiState.Loading,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (uiState.probeState is ProbeUiState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyzing...", color = MaterialTheme.colorScheme.onSurface)
                            } else {
                                Icon(PhosphorIcons.Regular.MagnifyingGlass, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyze")
                            }
                        }
                    }
                }
            }

            if (uiState.probeState is ProbeUiState.Error) {
                item(key = "probe_error") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Error",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = uiState.probeState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            if (uiState.probeState is ProbeUiState.Success) {
                val probe = uiState.probeState.probe

                item(key = "probe_media_header") {
                    MediaHeaderCard(probe = probe)
                }

                item(key = "formats_header") {
                    Text(
                        text = "Formats",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                item(key = "formats_connected_card") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column {
                            probe.formats.forEachIndexed { index, format ->
                                FormatRowItem(
                                    format = format,
                                    isSelected = uiState.selectedFormat?.id == format.id,
                                    onSelect = { onFormatSelected(format) }
                                )
                                if (index < probe.formats.lastIndex) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "download_action") {
                    Button(
                        onClick = onDownloadClicked,
                        enabled = uiState.selectedFormat != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(PhosphorIcons.Regular.DownloadSimple, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Download",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaHeaderCard(probe: ProbeResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.Video,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = probe.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (probe.uploader.isNotBlank()) {
                    Text(
                        text = probe.uploader,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (probe.durationSeconds > 0) {
                    val min = probe.durationSeconds / 60
                    val sec = probe.durationSeconds % 60
                    Text(
                        text = String.format("%02d:%02d", min, sec),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun FormatRowItem(
    format: MediaFormat,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .background(
                if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect
        )
        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = if (format.isAudioOnly) PhosphorIcons.Regular.MusicNote else PhosphorIcons.Regular.Video,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = format.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (format.note.isNotBlank()) {
                Text(
                    text = format.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
