package app.vidown.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.vidown.python.SubtitleTrack
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Check
import com.adamglin.phosphoricons.regular.ClosedCaptioning
import com.adamglin.phosphoricons.regular.FileText
import com.adamglin.phosphoricons.regular.MagnifyingGlass
import com.adamglin.phosphoricons.regular.Translate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleSelectionSheet(
    availableTracks: List<SubtitleTrack>,
    selectedLangs: Set<String>,
    deliveryMode: String,
    subtitleFormat: String,
    onToggleLanguage: (String) -> Unit,
    onSelectAll: (List<String>) -> Unit,
    onClearAll: () -> Unit,
    onDeliveryModeChanged: (String) -> Unit,
    onSubtitleFormatChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var includeAutoCaptions by rememberSaveable { mutableStateOf(true) }

    val filteredTracks = remember(availableTracks, searchQuery, includeAutoCaptions) {
        availableTracks.filter { track ->
            if (!includeAutoCaptions && track.isAuto) return@filter false
            if (searchQuery.isBlank()) true
            else {
                track.code.contains(searchQuery.trim(), ignoreCase = true) ||
                    track.displayName.contains(searchQuery.trim(), ignoreCase = true)
            }
        }
    }

    val manualTracks = remember(filteredTracks) { filteredTracks.filter { !it.isAuto } }
    val autoTracks = remember(filteredTracks) { filteredTracks.filter { it.isAuto } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = PhosphorIcons.Regular.ClosedCaptioning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Subtitles & Captions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${selectedLangs.size} selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Delivery Mode Selection
            Text(
                text = "Delivery Mode",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = deliveryMode == "embed",
                    onClick = { onDeliveryModeChanged("embed") },
                    label = { Text("Soft-Embed in video") },
                    leadingIcon = {
                        Icon(
                            imageVector = PhosphorIcons.Regular.ClosedCaptioning,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = deliveryMode == "external",
                    onClick = { onDeliveryModeChanged("external") },
                    label = { Text("External .srt/.vtt") },
                    leadingIcon = {
                        Icon(
                            imageVector = PhosphorIcons.Regular.FileText,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Subtitle format selector if external
            AnimatedVisibility(visible = deliveryMode == "external") {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = subtitleFormat == "srt",
                            onClick = { onSubtitleFormatChanged("srt") },
                            label = { Text(".SRT (Universal)") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = subtitleFormat == "vtt",
                            onClick = { onSubtitleFormatChanged("vtt") },
                            label = { Text(".VTT (WebVTT)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search language or code...") },
                leadingIcon = {
                    Icon(
                        imageVector = PhosphorIcons.Regular.MagnifyingGlass,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Auto-captions toggle & Selection actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { includeAutoCaptions = !includeAutoCaptions }
                ) {
                    Switch(
                        checked = includeAutoCaptions,
                        onCheckedChange = { includeAutoCaptions = it },
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Auto-captions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            val allVisible = filteredTracks.map { it.code }
                            onSelectAll(allVisible)
                        }
                    ) {
                        Text("Select All", style = MaterialTheme.typography.labelSmall)
                    }

                    TextButton(onClick = onClearAll) {
                        Text("Clear", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Track list
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (filteredTracks.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No subtitles match \"$searchQuery\"" else "No subtitles available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        if (manualTracks.isNotEmpty()) {
                            item(key = "header_manual") {
                                SectionLabel(text = "Manual Subtitles (${manualTracks.size})")
                            }
                            items(manualTracks, key = { "manual_${it.code}" }) { track ->
                                SubtitleTrackItem(
                                    track = track,
                                    isSelected = selectedLangs.contains(track.code),
                                    onToggle = { onToggleLanguage(track.code) }
                                )
                            }
                        }

                        if (autoTracks.isNotEmpty() && includeAutoCaptions) {
                            if (manualTracks.isNotEmpty()) {
                                item(key = "divider_sections") {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                            item(key = "header_auto") {
                                SectionLabel(text = "Auto-Generated Captions (${autoTracks.size})")
                            }
                            items(autoTracks, key = { "auto_${it.code}" }) { track ->
                                SubtitleTrackItem(
                                    track = track,
                                    isSelected = selectedLangs.contains(track.code),
                                    onToggle = { onToggleLanguage(track.code) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = PhosphorIcons.Regular.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Done", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

@Composable
private fun SubtitleTrackItem(
    track: SubtitleTrack,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (track.isAuto) {
                MaterialTheme.colorScheme.surfaceContainerHighest
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
        ) {
            Text(
                text = if (track.isAuto) "auto • ${track.code}" else track.code,
                style = MaterialTheme.typography.labelSmall,
                color = if (track.isAuto) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                },
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
