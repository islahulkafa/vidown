package app.vidown.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowLeft
import com.adamglin.phosphoricons.regular.ArrowsClockwise
import com.adamglin.phosphoricons.regular.CaretRight
import com.adamglin.phosphoricons.regular.ClipboardText
import com.adamglin.phosphoricons.regular.FileText
import com.adamglin.phosphoricons.regular.Folder
import com.adamglin.phosphoricons.regular.Info
import com.adamglin.phosphoricons.regular.Key
import com.adamglin.phosphoricons.regular.TerminalWindow
import com.adamglin.phosphoricons.regular.Trash
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vidown.BuildConfig
import app.vidown.data.cookies.CookieSummary
import app.vidown.ui.MainViewModel

enum class SettingsSubSection {
    MAIN,
    ENGINE,
    DOWNLOADS,
    COOKIES,
    ABOUT
}

@Composable
fun SettingsRoute(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val ytdlpVersion by viewModel.ytdlpVersion.collectAsStateWithLifecycle()
    val isUpdating by viewModel.isUpdating.collectAsStateWithLifecycle()
    val updateMessage by viewModel.updateMessage.collectAsStateWithLifecycle()
    val maxConcurrentDownloads by viewModel.maxConcurrentDownloads.collectAsStateWithLifecycle()
    val cookieSummary by viewModel.cookieSummary.collectAsStateWithLifecycle()
    val useCookies by viewModel.useCookies.collectAsStateWithLifecycle()
    val useCustomStorage by viewModel.useCustomStorage.collectAsStateWithLifecycle()
    val customStorageUri by viewModel.customStorageUri.collectAsStateWithLifecycle()
    val organizeByCreator by viewModel.organizeByCreator.collectAsStateWithLifecycle()

    var currentSection by rememberSaveable { mutableStateOf(SettingsSubSection.MAIN) }

    if (currentSection != SettingsSubSection.MAIN) {
        BackHandler { currentSection = SettingsSubSection.MAIN }
    }

    when (currentSection) {
        SettingsSubSection.MAIN -> {
            SettingsMainScreen(
                ytdlpVersion = ytdlpVersion,
                cookieSummary = cookieSummary,
                onNavigateToSection = { currentSection = it },
                modifier = modifier
            )
        }
        SettingsSubSection.ENGINE -> {
            EngineSettingsScreen(
                ytdlpVersion = ytdlpVersion,
                isUpdating = isUpdating,
                updateMessage = updateMessage,
                onCheckUpdate = viewModel::checkYtdlpUpdate,
                onBack = { currentSection = SettingsSubSection.MAIN },
                modifier = modifier
            )
        }
        SettingsSubSection.DOWNLOADS -> {
            DownloadsSettingsScreen(
                maxConcurrentDownloads = maxConcurrentDownloads,
                onSetMaxConcurrentDownloads = viewModel::setMaxConcurrentDownloads,
                useCustomStorage = useCustomStorage,
                onToggleUseCustomStorage = viewModel::setUseCustomStorage,
                customStorageUri = customStorageUri,
                onSelectCustomFolder = viewModel::setCustomStorage,
                organizeByCreator = organizeByCreator,
                onToggleOrganizeByCreator = viewModel::setOrganizeByCreator,
                onBack = { currentSection = SettingsSubSection.MAIN },
                modifier = modifier
            )
        }
        SettingsSubSection.COOKIES -> {
            CookiesSettingsScreen(
                cookieSummary = cookieSummary,
                useCookies = useCookies,
                onToggleUseCookies = viewModel::toggleUseCookies,
                onImportFile = { uri -> viewModel.importCookieFile(uri) { _, _ -> } },
                onImportText = { text -> viewModel.importCookieText(text) { _, _ -> } },
                onClearCookies = viewModel::clearCookies,
                onBack = { currentSection = SettingsSubSection.MAIN },
                modifier = modifier
            )
        }
        SettingsSubSection.ABOUT -> {
            AboutSettingsScreen(
                onBack = { currentSection = SettingsSubSection.MAIN },
                modifier = modifier
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMainScreen(
    ytdlpVersion: String,
    cookieSummary: CookieSummary?,
    onNavigateToSection: (SettingsSubSection) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
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
            item(key = "settings_connected_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column {
                        SettingsRow(
                            icon = PhosphorIcons.Regular.TerminalWindow,
                            title = "Engine",
                            subtitle = "yt-dlp $ytdlpVersion",
                            onClick = { onNavigateToSection(SettingsSubSection.ENGINE) }
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        SettingsRow(
                            icon = PhosphorIcons.Regular.Folder,
                            title = "Storage",
                            subtitle = "Movies & Music",
                            onClick = { onNavigateToSection(SettingsSubSection.DOWNLOADS) }
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        SettingsRow(
                            icon = PhosphorIcons.Regular.Key,
                            title = "Cookies & Auth",
                            subtitle = if (cookieSummary != null) "${cookieSummary.cookieCount} cookies active" else "Not configured",
                            onClick = { onNavigateToSection(SettingsSubSection.COOKIES) }
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        SettingsRow(
                            icon = PhosphorIcons.Regular.Info,
                            title = "About",
                            subtitle = "Version ${BuildConfig.VERSION_NAME}",
                            onClick = { onNavigateToSection(SettingsSubSection.ABOUT) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = PhosphorIcons.Regular.CaretRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EngineSettingsScreen(
    ytdlpVersion: String,
    isUpdating: Boolean,
    updateMessage: String?,
    onCheckUpdate: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Engine", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.ArrowLeft,
                            contentDescription = "Navigate back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
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
            item(key = "engine_status_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Version",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = ytdlpVersion,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onCheckUpdate,
                            enabled = !isUpdating,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isUpdating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Checking PyPI...", color = MaterialTheme.colorScheme.onSurface)
                            } else {
                                Icon(PhosphorIcons.Regular.ArrowsClockwise, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Check for updates")
                            }
                        }

                        if (updateMessage != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = updateMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsSettingsScreen(
    maxConcurrentDownloads: Int,
    onSetMaxConcurrentDownloads: (Int) -> Unit,
    useCustomStorage: Boolean,
    onToggleUseCustomStorage: (Boolean) -> Unit,
    customStorageUri: String?,
    onSelectCustomFolder: (Uri) -> Unit,
    organizeByCreator: Boolean,
    onToggleOrganizeByCreator: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            onSelectCustomFolder(uri)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Storage & Downloads", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.ArrowLeft,
                            contentDescription = "Navigate back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
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
            item(key = "concurrency_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Concurrent Downloads",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Number of active downloads running simultaneously. Higher numbers use more bandwidth.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(1, 2, 3, 4).forEach { count ->
                                FilterChip(
                                    selected = maxConcurrentDownloads == count,
                                    onClick = { onSetMaxConcurrentDownloads(count) },
                                    label = { Text("$count") },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }
            }

            item(key = "storage_mode_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Storage Destination",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = !useCustomStorage,
                                onClick = { onToggleUseCustomStorage(false) },
                                label = { Text("Standard MediaStore") },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
                                )
                            )

                            FilterChip(
                                selected = useCustomStorage,
                                onClick = { onToggleUseCustomStorage(true) },
                                label = { Text("Custom Folder (SAF)") },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }

                        if (useCustomStorage) {
                            val displayPath = customStorageUri?.let {
                                try {
                                    val decoded = Uri.decode(it)
                                    decoded.substringAfterLast(":")
                                } catch (_: Exception) {
                                    it
                                }
                            } ?: "No folder selected"

                            Text(
                                text = "Target: $displayPath",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Downloads will be saved into Movies/ and Music/ subfolders inside the chosen directory.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = { folderPicker.launch(null) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = PhosphorIcons.Regular.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (customStorageUri != null) "Change Folder" else "Choose Folder")
                            }
                        } else {
                            Text(
                                text = "Saved to standard device public storage:\n• Videos: Movies/Vidown\n• Audio: Music/Vidown",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item(key = "organization_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Organize by Creator",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Automatically organize files into subfolders named after the uploader or artist.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Switch(
                            checked = organizeByCreator,
                            onCheckedChange = onToggleOrganizeByCreator
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("About", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.ArrowLeft,
                            contentDescription = "Navigate back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
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
            item(key = "about_info_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Vidown",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Version ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Lightweight media downloader for offline playback.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookiesSettingsScreen(
    cookieSummary: CookieSummary?,
    useCookies: Boolean,
    onToggleUseCookies: (Boolean) -> Unit,
    onImportFile: (Uri) -> Unit,
    onImportText: (String) -> Unit,
    onClearCookies: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPasteDialog by remember { mutableStateOf(false) }
    var pasteText by remember { mutableStateOf("") }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportFile(uri)
        }
    }

    if (showPasteDialog) {
        AlertDialog(
            onDismissRequest = {
                showPasteDialog = false
                pasteText = ""
            },
            title = {
                Text(
                    text = "Paste Cookies",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Paste standard Netscape tab-separated cookies text:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = pasteText,
                        onValueChange = { pasteText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        placeholder = { Text("# Netscape HTTP Cookie File\n.youtube.com\tTRUE\t/...") },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onImportText(pasteText)
                        showPasteDialog = false
                        pasteText = ""
                    },
                    enabled = pasteText.isNotBlank()
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPasteDialog = false
                        pasteText = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Cookies & Auth", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.ArrowLeft,
                            contentDescription = "Navigate back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
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
            item(key = "cookies_toggle_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Use Cookies for Downloads",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Send authenticated session for age-restricted and private videos",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Switch(
                            checked = useCookies,
                            onCheckedChange = onToggleUseCookies
                        )
                    }
                }
            }

            item(key = "cookies_status_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (cookieSummary != null) {
                            Text(
                                text = "${cookieSummary.cookieCount} cookies loaded",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (cookieSummary.domains.isNotEmpty()) {
                                Text(
                                    text = "Domains: ${cookieSummary.domains.take(4).joinToString(", ")}${if (cookieSummary.domains.size > 4) "..." else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = onClearCookies,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = PhosphorIcons.Regular.Trash,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Clear Stored Cookies")
                            }
                        } else {
                            Text(
                                text = "No cookies configured",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Import cookies exported from your browser in Netscape format to authorize requests.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item(key = "cookies_actions_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Import",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Button(
                            onClick = { filePicker.launch(arrayOf("text/plain", "*/*")) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.FileText,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select cookies.txt File")
                        }

                        OutlinedButton(
                            onClick = { showPasteDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.ClipboardText,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Paste Cookie Content")
                        }
                    }
                }
            }
        }
    }
}
