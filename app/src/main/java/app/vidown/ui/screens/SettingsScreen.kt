package app.vidown.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
    bottomBarPadding: Dp = 80.dp,
    onSubSectionChange: (Boolean) -> Unit = {},
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
    val downloadOnlyOnWifi by viewModel.downloadOnlyOnWifi.collectAsStateWithLifecycle()
    val downloadSpeedLimitBytes by viewModel.downloadSpeedLimitBytes.collectAsStateWithLifecycle()
    val defaultSubtitleDeliveryMode by viewModel.defaultSubtitleDeliveryMode.collectAsStateWithLifecycle()
    val defaultSubtitleFormat by viewModel.defaultSubtitleFormat.collectAsStateWithLifecycle()
    val includeAutoCaptions by viewModel.includeAutoCaptions.collectAsStateWithLifecycle()
    val cacheSizeBytes by viewModel.cacheSizeBytes.collectAsStateWithLifecycle()

    var currentSection by rememberSaveable { mutableStateOf(SettingsSubSection.MAIN) }

    fun updateSection(newSection: SettingsSubSection) {
        currentSection = newSection
        onSubSectionChange(newSection != SettingsSubSection.MAIN)
    }

    LaunchedEffect(currentSection) {
        onSubSectionChange(currentSection != SettingsSubSection.MAIN)
    }

    if (currentSection != SettingsSubSection.MAIN) {
        BackHandler { updateSection(SettingsSubSection.MAIN) }
    }

    AnimatedContent(
        targetState = currentSection,
        transitionSpec = {
            if (targetState != SettingsSubSection.MAIN) {
                (slideInHorizontally(animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)) { width -> width } +
                    fadeIn(animationSpec = tween(durationMillis = 200)))
                    .togetherWith(
                        slideOutHorizontally(animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)) { width -> -width / 4 } +
                            fadeOut(animationSpec = tween(durationMillis = 150))
                    )
            } else {
                (slideInHorizontally(animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)) { width -> -width / 4 } +
                    fadeIn(animationSpec = tween(durationMillis = 200)))
                    .togetherWith(
                        slideOutHorizontally(animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)) { width -> width } +
                            fadeOut(animationSpec = tween(durationMillis = 150))
                    )
            }
        },
        contentAlignment = Alignment.TopStart,
        label = "settings_subsections"
    ) { section ->
        when (section) {
            SettingsSubSection.MAIN -> {
                SettingsMainScreen(
                    ytdlpVersion = ytdlpVersion,
                    cookieSummary = cookieSummary,
                    onNavigateToSection = { updateSection(it) },
                    modifier = modifier.padding(bottom = bottomBarPadding)
                )
            }
            SettingsSubSection.ENGINE -> {
                EngineSettingsScreen(
                    ytdlpVersion = ytdlpVersion,
                    isUpdating = isUpdating,
                    updateMessage = updateMessage,
                    onCheckUpdate = viewModel::checkYtdlpUpdate,
                    onBack = { updateSection(SettingsSubSection.MAIN) },
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
                    downloadOnlyOnWifi = downloadOnlyOnWifi,
                    onToggleDownloadOnlyOnWifi = viewModel::setDownloadOnlyOnWifi,
                    downloadSpeedLimitBytes = downloadSpeedLimitBytes,
                    onSetDownloadSpeedLimitBytes = viewModel::setDownloadSpeedLimitBytes,
                    defaultSubtitleDeliveryMode = defaultSubtitleDeliveryMode,
                    onSetDefaultSubtitleDeliveryMode = viewModel::setDefaultSubtitleDeliveryMode,
                    defaultSubtitleFormat = defaultSubtitleFormat,
                    onSetDefaultSubtitleFormat = viewModel::setDefaultSubtitleFormat,
                    includeAutoCaptions = includeAutoCaptions,
                    onToggleIncludeAutoCaptions = viewModel::setIncludeAutoCaptions,
                    cacheSizeBytes = cacheSizeBytes,
                    onClearCache = viewModel::clearAppCache,
                    onBack = { updateSection(SettingsSubSection.MAIN) },
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
                    onBack = { updateSection(SettingsSubSection.MAIN) },
                    modifier = modifier
                )
            }
            SettingsSubSection.ABOUT -> {
                AboutSettingsScreen(
                    onBack = { updateSection(SettingsSubSection.MAIN) },
                    modifier = modifier
                )
            }
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
    downloadOnlyOnWifi: Boolean,
    onToggleDownloadOnlyOnWifi: (Boolean) -> Unit,
    downloadSpeedLimitBytes: Long,
    onSetDownloadSpeedLimitBytes: (Long) -> Unit,
    defaultSubtitleDeliveryMode: String = "embed",
    onSetDefaultSubtitleDeliveryMode: (String) -> Unit = {},
    defaultSubtitleFormat: String = "srt",
    onSetDefaultSubtitleFormat: (String) -> Unit = {},
    includeAutoCaptions: Boolean = true,
    onToggleIncludeAutoCaptions: (Boolean) -> Unit = {},
    cacheSizeBytes: Long = 0L,
    onClearCache: () -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomSpeedDialog by remember { mutableStateOf(false) }
    var customSpeedInput by remember { mutableStateOf("") }
    var customSpeedIsMb by remember { mutableStateOf(true) }
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

            item(key = "wifi_constraint_card") {
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
                                text = "Download over Wi-Fi only",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Automatically pauses downloads when connected to mobile cellular data.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Switch(
                            checked = downloadOnlyOnWifi,
                            onCheckedChange = onToggleDownloadOnlyOnWifi
                        )
                    }
                }
            }

            item(key = "speed_limit_card") {
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
                            text = "Download Speed Limit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Throttle transfer rate to save bandwidth for other apps and devices.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val presetsRow1 = listOf(
                            0L to "Unlimited",
                            1_048_576L to "1 MB/s",
                            2_097_152L to "2 MB/s"
                        )
                        val presetsRow2 = listOf(
                            5_242_880L to "5 MB/s",
                            10_485_760L to "10 MB/s"
                        )
                        val isCustom = downloadSpeedLimitBytes > 0L &&
                            presetsRow1.none { it.first == downloadSpeedLimitBytes } &&
                            presetsRow2.none { it.first == downloadSpeedLimitBytes }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            presetsRow1.forEach { (bytes, label) ->
                                FilterChip(
                                    selected = downloadSpeedLimitBytes == bytes,
                                    onClick = { onSetDownloadSpeedLimitBytes(bytes) },
                                    label = { Text(label) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            presetsRow2.forEach { (bytes, label) ->
                                FilterChip(
                                    selected = downloadSpeedLimitBytes == bytes,
                                    onClick = { onSetDownloadSpeedLimitBytes(bytes) },
                                    label = { Text(label) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                            FilterChip(
                                selected = isCustom,
                                onClick = { showCustomSpeedDialog = true },
                                label = {
                                    Text(
                                        if (isCustom) {
                                            formatSpeedBytes(downloadSpeedLimitBytes)
                                        } else {
                                            "Custom"
                                        }
                                    )
                                },
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

            item(key = "subtitles_settings_card") {
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
                            text = "Subtitles",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Default delivery mode and format for downloaded subtitles.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "Default Delivery Mode",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = defaultSubtitleDeliveryMode == "embed",
                                onClick = { onSetDefaultSubtitleDeliveryMode("embed") },
                                label = { Text("Soft-Embed (in video)") },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            FilterChip(
                                selected = defaultSubtitleDeliveryMode == "external",
                                onClick = { onSetDefaultSubtitleDeliveryMode("external") },
                                label = { Text("External File") },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }

                        if (defaultSubtitleDeliveryMode == "external") {
                            Text(
                                text = "External File Format",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = defaultSubtitleFormat == "srt",
                                    onClick = { onSetDefaultSubtitleFormat("srt") },
                                    label = { Text(".SRT (SubRip)") },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                FilterChip(
                                    selected = defaultSubtitleFormat == "vtt",
                                    onClick = { onSetDefaultSubtitleFormat("vtt") },
                                    label = { Text(".VTT (WebVTT)") },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Include auto-generated captions",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Lists and allows downloading speech-to-text captions when available.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Switch(
                                checked = includeAutoCaptions,
                                onCheckedChange = onToggleIncludeAutoCaptions
                            )
                        }
                    }
                }
            }

            item(key = "cache_management_card") {
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Temporary Cache",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Image previews and temporary stream download data.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = formatStorageBytes(cacheSizeBytes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Button(
                            onClick = onClearCache,
                            enabled = cacheSizeBytes > 0L,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.Trash,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear Cache")
                        }
                    }
                }
            }
        }
    }

    if (showCustomSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showCustomSpeedDialog = false },
            title = {
                Text(text = "Custom Speed Limit", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter maximum download speed (0 for unlimited).",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = customSpeedInput,
                        onValueChange = { customSpeedInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Speed Limit") },
                        placeholder = { Text("e.g. 5") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = customSpeedIsMb,
                            onClick = { customSpeedIsMb = true },
                            label = { Text("MB/s") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !customSpeedIsMb,
                            onClick = { customSpeedIsMb = false },
                            label = { Text("KB/s") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        val num = customSpeedInput.toLongOrNull() ?: 0L
                        val multiplier = if (customSpeedIsMb) 1024L * 1024L else 1024L
                        val totalBytes = if (num <= 0) 0L else num * multiplier
                        onSetDownloadSpeedLimitBytes(totalBytes)
                        showCustomSpeedDialog = false
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomSpeedDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatStorageBytes(bytes: Long): String {
    return when {
        bytes <= 0L -> "0 B"
        bytes >= 1024L * 1024L * 1024L -> String.format(java.util.Locale.US, "%.1f GB", bytes.toDouble() / (1024L * 1024L * 1024L))
        bytes >= 1024L * 1024L -> String.format(java.util.Locale.US, "%.1f MB", bytes.toDouble() / (1024L * 1024L))
        bytes >= 1024L -> String.format(java.util.Locale.US, "%.0f KB", bytes.toDouble() / 1024L)
        else -> "$bytes B"
    }
}

private fun formatSpeedBytes(bytes: Long): String {
    return when {
        bytes <= 0L -> "Unlimited"
        bytes >= 1024L * 1024L -> "${bytes / (1024L * 1024L)} MB/s"
        else -> "${bytes / 1024L} KB/s"
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
