package net.sourceforge.moonstone.android

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import net.sourceforge.moonstone.android.model.LauncherSettings
import net.sourceforge.moonstone.android.repl.ReplServerManager
import net.sourceforge.moonstone.android.service.SampleAppsCreator
import net.sourceforge.moonstone.android.service.SettingsRepository
import net.sourceforge.moonstone.android.webide.WebIdeServerManager
import java.io.File

/**
 * Settings activity for configuring the launcher.
 */
class SettingsActivity : ComponentActivity() {
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var defaultAppsPath: String

    // State - initialized later with correct path
    private var settings = mutableStateOf<LauncherSettings?>(null)
    private var hasAllFilesAccess = mutableStateOf(false)
    private var webIdeServerRunning = mutableStateOf(false)
    private var webIdeServerUrl = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        settingsRepository = SettingsRepository(this)
        defaultAppsPath = settingsRepository.getDefaultAppsPath()
        settings.value = settingsRepository.loadSettings()
        updateAllFilesAccessState()
        updateWebIdeServerState()

        // Auto-start server if enabled
        settings.value?.let { currentSettings ->
            if (currentSettings.webIdeEnabled) {
                startWebIdeServer(currentSettings)
            }
        }

        setContent {
            MoonstoneTheme {
                settings.value?.let { currentSettings ->
                    SettingsScreen(
                        settings = currentSettings,
                        defaultAppsPath = defaultAppsPath,
                        permissionsSettings =
                            PermissionsSettings(
                                hasAllFilesAccess = hasAllFilesAccess.value,
                                showAllFilesAccessOption = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
                                onRequestAllFilesAccess = { requestAllFilesAccess() },
                            ),
                        webIdeSettings =
                            WebIdeSettings(
                                serverRunning = webIdeServerRunning.value,
                                serverUrl = webIdeServerUrl.value,
                                port = currentSettings.webIdePort,
                                onToggle = { enabled -> toggleWebIdeServer(enabled) },
                                onPortChange = { port -> updateWebIdePort(port) },
                                onCopyUrl = { copyUrlToClipboard() },
                            ),
                        replServerSettings =
                            ReplServerSettings(
                                enabled = currentSettings.replServerEnabled,
                                port = currentSettings.replServerPort,
                                onToggle = { enabled -> toggleReplServer(enabled) },
                                onPortChange = { port -> updateReplServerPort(port) },
                            ),
                        onSettingsChange = { newSettings ->
                            settings.value = newSettings
                            settingsRepository.saveSettings(newSettings)
                        },
                        onBackClick = { finish() },
                        onCreateSampleApps = { createSampleApps() },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check permission when returning from settings
        updateAllFilesAccessState()
        updateWebIdeServerState()
    }

    private fun updateAllFilesAccessState() {
        hasAllFilesAccess.value =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                // On older versions, legacy storage mode should work
                true
            }
    }

    private fun requestAllFilesAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent =
                    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = "package:$packageName".toUri()
                    }
                startActivity(intent)
            } catch (e: Exception) {
                // Fallback to general all files access settings
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                } catch (e2: Exception) {
                    Toast
                        .makeText(
                            this,
                            "Unable to open settings. Please grant 'All files access' manually in " +
                                "Settings > Apps > Moonstone > Permissions.",
                            Toast.LENGTH_LONG,
                        ).show()
                }
            }
        }
    }

    private fun createSampleApps() {
        val currentSettings = settings.value ?: return
        val folder = currentSettings.getAppsRootFolder()
        try {
            val created = SampleAppsCreator.createSampleApps(folder)
            if (created > 0) {
                Toast
                    .makeText(
                        this,
                        "Created $created sample app(s)",
                        Toast.LENGTH_SHORT,
                    ).show()
            } else {
                Toast
                    .makeText(
                        this,
                        "Sample apps already exist",
                        Toast.LENGTH_SHORT,
                    ).show()
            }
        } catch (e: Exception) {
            Toast
                .makeText(
                    this,
                    "Failed to create sample apps: ${e.message}",
                    Toast.LENGTH_LONG,
                ).show()
        }
    }

    private fun updateWebIdeServerState() {
        webIdeServerRunning.value = WebIdeServerManager.isRunning
        webIdeServerUrl.value = WebIdeServerManager.serverUrl
    }

    private fun startWebIdeServer(currentSettings: LauncherSettings) {
        val appsFolder = currentSettings.getAppsRootFolder()
        if (!appsFolder.exists()) {
            appsFolder.mkdirs()
        }
        val success = WebIdeServerManager.start(this, appsFolder, currentSettings.webIdePort)
        updateWebIdeServerState()
        if (!success) {
            Toast
                .makeText(
                    this,
                    "Failed to start Web IDE server. Port may be in use.",
                    Toast.LENGTH_LONG,
                ).show()
        }
    }

    private fun toggleWebIdeServer(enabled: Boolean) {
        val currentSettings = settings.value ?: return

        if (enabled) {
            startWebIdeServer(currentSettings)
        } else {
            WebIdeServerManager.stop()
            updateWebIdeServerState()
        }

        // Update settings
        val newSettings = currentSettings.copy(webIdeEnabled = enabled)
        settings.value = newSettings
        settingsRepository.saveSettings(newSettings)
    }

    private fun updateWebIdePort(port: Int) {
        val currentSettings = settings.value ?: return

        // Validate port range
        val validPort = port.coerceIn(1024, 65535)

        // Update settings
        val newSettings = currentSettings.copy(webIdePort = validPort)
        settings.value = newSettings
        settingsRepository.saveSettings(newSettings)

        // If server is running, restart with new port
        if (WebIdeServerManager.isRunning) {
            WebIdeServerManager.stop()
            startWebIdeServer(newSettings)
        }
    }

    private fun copyUrlToClipboard() {
        val url = webIdeServerUrl.value ?: return
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Web IDE URL", url)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "URL copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun toggleReplServer(enabled: Boolean) {
        val currentSettings = settings.value ?: return

        // Update settings
        val newSettings = currentSettings.copy(replServerEnabled = enabled)
        settings.value = newSettings
        settingsRepository.saveSettings(newSettings)

        if (enabled) {
            Toast
                .makeText(
                    this,
                    "REPL server will start when an app is launched",
                    Toast.LENGTH_SHORT,
                ).show()
        } else {
            // Stop any running server
            if (ReplServerManager.isRunning) {
                ReplServerManager.stop()
            }
            Toast
                .makeText(
                    this,
                    "REPL server disabled",
                    Toast.LENGTH_SHORT,
                ).show()
        }
    }

    private fun updateReplServerPort(port: Int) {
        val currentSettings = settings.value ?: return

        // Validate port range
        val validPort = port.coerceIn(1024, 65535)

        // Update settings
        val newSettings = currentSettings.copy(replServerPort = validPort)
        settings.value = newSettings
        settingsRepository.saveSettings(newSettings)
    }
}

/**
 * Data class grouping Web IDE server-related settings.
 */
data class WebIdeSettings(
    val serverRunning: Boolean,
    val serverUrl: String?,
    val port: Int,
    val onToggle: (Boolean) -> Unit,
    val onPortChange: (Int) -> Unit,
    val onCopyUrl: () -> Unit,
)

/**
 * Data class grouping REPL server-related settings.
 */
data class ReplServerSettings(
    val enabled: Boolean,
    val port: Int,
    val onToggle: (Boolean) -> Unit,
    val onPortChange: (Int) -> Unit,
)

/**
 * Data class grouping permissions-related settings.
 */
data class PermissionsSettings(
    val hasAllFilesAccess: Boolean,
    val showAllFilesAccessOption: Boolean,
    val onRequestAllFilesAccess: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongParameterList")
@Composable
fun SettingsScreen(
    settings: LauncherSettings,
    defaultAppsPath: String,
    permissionsSettings: PermissionsSettings,
    webIdeSettings: WebIdeSettings,
    replServerSettings: ReplServerSettings,
    onSettingsChange: (LauncherSettings) -> Unit,
    onBackClick: () -> Unit,
    onCreateSampleApps: () -> Unit,
) {
    Scaffold(
        modifier =
            Modifier
                .fillMaxSize()
                .systemBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Permissions Section (Android 11+)
            if (permissionsSettings.showAllFilesAccessOption) {
                PermissionsSection(permissionsSettings)
                HorizontalDivider()
            }

            // Apps Folder Section
            AppsFolderSection(
                settings = settings,
                defaultAppsPath = defaultAppsPath,
                onSettingsChange = onSettingsChange,
            )
            HorizontalDivider()

            // Display Settings Section
            DisplaySettingsSection(
                settings = settings,
                onSettingsChange = onSettingsChange,
            )
            HorizontalDivider()

            // Web IDE Server Section
            WebIdeServerSection(webIdeSettings)
            HorizontalDivider()

            // REPL Server Section
            ReplServerSection(replServerSettings)
            HorizontalDivider()

            // Sample Apps Section
            SampleAppsSection(onCreateSampleApps)
        }
    }
}

@Composable
private fun PermissionsSection(permissionsSettings: PermissionsSettings) {
    SettingsSection(title = "Permissions") {
        Text(
            text = "All Files Access",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text =
                if (permissionsSettings.hasAllFilesAccess) {
                    "Permission granted. You can read and write files from any folder."
                } else {
                    "Required to read and write files outside the app's default folder. " +
                        "Enable this to edit files from other applications."
                },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (permissionsSettings.hasAllFilesAccess) {
            Card(
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "✓ Permission Granted",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        } else {
            Button(
                onClick = permissionsSettings.onRequestAllFilesAccess,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Grant All Files Access")
            }
        }
    }
}

@Composable
private fun AppsFolderSection(
    settings: LauncherSettings,
    defaultAppsPath: String,
    onSettingsChange: (LauncherSettings) -> Unit,
) {
    SettingsSection(title = "Apps Folder") {
        var folderPath by remember { mutableStateOf(settings.appsRootPath) }

        OutlinedTextField(
            value = folderPath,
            onValueChange = { folderPath = it },
            label = { Text("Apps Root Path") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            OutlinedButton(
                onClick = {
                    folderPath = defaultAppsPath
                    onSettingsChange(settings.copy(appsRootPath = folderPath))
                },
            ) {
                Text("Reset to Default")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    // Create folder if it doesn't exist
                    val folder = File(folderPath)
                    if (!folder.exists()) {
                        folder.mkdirs()
                    }
                    onSettingsChange(settings.copy(appsRootPath = folderPath))
                },
            ) {
                Text("Apply")
            }
        }
    }
}

@Composable
private fun DisplaySettingsSection(
    settings: LauncherSettings,
    onSettingsChange: (LauncherSettings) -> Unit,
) {
    SettingsSection(title = "Display") {
        // Grid Columns
        var columns by remember { mutableFloatStateOf(settings.gridColumns.toFloat()) }

        Text("Grid Columns: ${columns.toInt()}")
        Slider(
            value = columns,
            onValueChange = { columns = it },
            onValueChangeFinished = {
                onSettingsChange(settings.copy(gridColumns = columns.toInt()))
            },
            valueRange =
                LauncherSettings.MIN_GRID_COLUMNS.toFloat()..LauncherSettings.MAX_GRID_COLUMNS.toFloat(),
            steps = LauncherSettings.MAX_GRID_COLUMNS - LauncherSettings.MIN_GRID_COLUMNS - 1,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Show App Names
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Show App Names")
            Switch(
                checked = settings.showAppNames,
                onCheckedChange = { checked ->
                    onSettingsChange(settings.copy(showAppNames = checked))
                },
            )
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun WebIdeServerSection(webIdeSettings: WebIdeSettings) {
    SettingsSection(title = "Web IDE Server") {
        Text(
            text = "Enable a web-based IDE to edit apps from any device on your local network.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(12.dp))

        ServerToggle(
            enabled = webIdeSettings.serverRunning,
            onToggle = webIdeSettings.onToggle,
        )

        Spacer(modifier = Modifier.height(12.dp))

        PortConfiguration(
            port = webIdeSettings.port,
            serverRunning = webIdeSettings.serverRunning,
            onPortChange = webIdeSettings.onPortChange,
        )

        if (webIdeSettings.serverRunning && webIdeSettings.serverUrl != null) {
            Spacer(modifier = Modifier.height(12.dp))
            ServerUrlDisplay(
                serverUrl = webIdeSettings.serverUrl,
                onCopyUrl = webIdeSettings.onCopyUrl,
            )
        }
    }
}

@Composable
private fun ServerToggle(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Enable Server")
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
        )
    }
}

@Composable
private fun PortConfiguration(
    port: Int,
    serverRunning: Boolean,
    onPortChange: (Int) -> Unit,
) {
    var portText by remember(port) {
        mutableStateOf(port.toString())
    }

    OutlinedTextField(
        value = portText,
        onValueChange = { newValue ->
            if (newValue.all { it.isDigit() }) {
                portText = newValue
            }
        },
        label = { Text("Port") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        enabled = !serverRunning,
    )

    Spacer(modifier = Modifier.height(4.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        Button(
            onClick = {
                val newPort = portText.toIntOrNull() ?: LauncherSettings.DEFAULT_WEB_IDE_PORT
                onPortChange(newPort)
            },
            enabled = !serverRunning && portText != port.toString(),
        ) {
            Text("Apply Port")
        }
    }
}

@Composable
private fun ServerUrlDisplay(
    serverUrl: String,
    onCopyUrl: () -> Unit,
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = serverUrl,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(onClick = onCopyUrl) {
                Text("Copy")
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Open this URL in a browser on any device connected to the same network.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Suppress("LongMethod")
@Composable
private fun ReplServerSection(replServerSettings: ReplServerSettings) {
    SettingsSection(title = "REPL Server") {
        Text(
            text =
                "Enable a Scheme REPL server for the running app. Connect from Emacs/geiser or " +
                    "any compatible client to interact with app state in real-time.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(12.dp))

        ReplServerToggle(
            enabled = replServerSettings.enabled,
            onToggle = replServerSettings.onToggle,
        )

        Spacer(modifier = Modifier.height(12.dp))

        ReplPortConfiguration(
            port = replServerSettings.port,
            onPortChange = replServerSettings.onPortChange,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text =
                "The REPL server starts when an app is launched and stops when the app is closed. " +
                    "Connect using: (geiser-connect 'kleinlisp \"<device-ip>\" ${replServerSettings.port})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReplServerToggle(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Enable REPL Server")
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
        )
    }
}

@Composable
private fun ReplPortConfiguration(
    port: Int,
    onPortChange: (Int) -> Unit,
) {
    var replPortText by remember(port) {
        mutableStateOf(port.toString())
    }

    OutlinedTextField(
        value = replPortText,
        onValueChange = { newValue ->
            if (newValue.all { it.isDigit() }) {
                replPortText = newValue
            }
        },
        label = { Text("Port (default: 37146)") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )

    Spacer(modifier = Modifier.height(4.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        Button(
            onClick = {
                val newPort = replPortText.toIntOrNull() ?: LauncherSettings.DEFAULT_REPL_SERVER_PORT
                onPortChange(newPort)
            },
            enabled = replPortText != port.toString(),
        ) {
            Text("Apply Port")
        }
    }
}

@Composable
private fun SampleAppsSection(onCreateSampleApps: () -> Unit) {
    SettingsSection(title = "Sample Apps") {
        Text(
            text = "Create sample apps to get started with Moonstone development.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onCreateSampleApps,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Create Sample Apps")
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}
