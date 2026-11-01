package app.vidown

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ClockCounterClockwise
import com.adamglin.phosphoricons.regular.DownloadSimple
import com.adamglin.phosphoricons.regular.Gear
import com.adamglin.phosphoricons.regular.House
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import app.vidown.ui.MainViewModel
import app.vidown.ui.screens.HistoryRoute
import app.vidown.ui.screens.HomeRoute
import app.vidown.ui.screens.QueueRoute
import app.vidown.ui.screens.SettingsRoute
import app.vidown.ui.theme.VidownTheme

enum class AppDestination(val label: String, val icon: ImageVector) {
    HOME("Home", PhosphorIcons.Regular.House),
    QUEUE("Queue", PhosphorIcons.Regular.DownloadSimple),
    HISTORY("History", PhosphorIcons.Regular.ClockCounterClockwise),
    SETTINGS("Settings", PhosphorIcons.Regular.Gear)
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        checkNotificationPermission()
        handleIncomingIntent(intent)

        setContent {
            VidownTheme {
                MainAppNavScaffold(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                viewModel.handleSharedUrl(sharedText)
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun MainAppNavScaffold(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestination.HOME) }

    NavigationSuiteScaffold(
        modifier = modifier,
        navigationSuiteItems = {
            AppDestination.entries.forEach { destination ->
                item(
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.label
                        )
                    },
                    label = { Text(destination.label) },
                    selected = destination == currentDestination,
                    onClick = { currentDestination = destination }
                )
            }
        }
    ) {
        when (currentDestination) {
            AppDestination.HOME -> {
                HomeRoute(
                    viewModel = viewModel,
                    onNavigateToQueue = { currentDestination = AppDestination.QUEUE },
                    modifier = Modifier.fillMaxSize()
                )
            }
            AppDestination.QUEUE -> {
                QueueRoute(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
            AppDestination.HISTORY -> {
                HistoryRoute(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
            AppDestination.SETTINGS -> {
                SettingsRoute(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
