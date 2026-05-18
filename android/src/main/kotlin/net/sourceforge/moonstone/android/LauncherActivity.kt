package net.sourceforge.moonstone.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.sourceforge.moonstone.android.model.AppInfo
import net.sourceforge.moonstone.android.model.LauncherSettings
import net.sourceforge.moonstone.android.service.AppDiscoveryService
import net.sourceforge.moonstone.android.service.SettingsRepository
import net.sourceforge.moonstone.android.ui.AppIconLoader
import java.io.File

class LauncherActivity : ComponentActivity() {
    companion object {
        const val EXTRA_FOLDER_PATH = "net.sourceforge.moonstone.FOLDER_PATH"
        const val EXTRA_FOLDER_NAME = "net.sourceforge.moonstone.FOLDER_NAME"
    }

    data class FolderEntry(
        val folder: File,
        val name: String,
    )

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var rootFolder: File

    // When launched from a widget pointing to a specific folder, initialBreadcrumbs contains
    // that folder as the first entry; otherwise it's empty (normal launcher root).
    private var initialBreadcrumbs: List<FolderEntry> = emptyList()

    private val storagePermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { _ ->
            refreshApps()
        }

    private var apps = mutableStateOf<List<AppInfo>>(emptyList())
    private var settings = mutableStateOf<LauncherSettings?>(null)
    private var hasStoragePermission = mutableStateOf(false)
    private val navigationStack = mutableStateOf<List<FolderEntry>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        settingsRepository = SettingsRepository(this)
        settings.value = settingsRepository.loadSettings()

        val intentFolderPath = intent.getStringExtra(EXTRA_FOLDER_PATH)
        rootFolder =
            if (intentFolderPath != null) {
                val intentFolderName =
                    intent.getStringExtra(EXTRA_FOLDER_NAME) ?: File(intentFolderPath).name
                initialBreadcrumbs = listOf(FolderEntry(File(intentFolderPath), intentFolderName))
                File(intentFolderPath)
            } else {
                settings.value!!.getAppsRootFolder()
            }
        navigationStack.value = initialBreadcrumbs

        setupBackPressedCallback()
        checkStoragePermission()

        setContent {
            MoonstoneTheme {
                settings.value?.let { currentSettings ->
                    LauncherScreen(
                        state =
                            LauncherState(
                                apps = apps.value,
                                settings = currentSettings,
                                hasPermission = hasStoragePermission.value,
                                navigationStack = navigationStack.value,
                                initialBreadcrumbsSize = initialBreadcrumbs.size,
                            ),
                        callbacks =
                            LauncherCallbacks(
                                onAppClick = { app -> launchApp(app) },
                                onFolderClick = { app -> navigateIntoFolder(app) },
                                onSettingsClick = { openSettings() },
                                onBackClick = { navigateBack() },
                                onRequestPermission = { requestStoragePermission() },
                            ),
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        settings.value = settingsRepository.loadSettings()

        if (intent.getStringExtra(EXTRA_FOLDER_PATH) == null) {
            rootFolder = settings.value!!.getAppsRootFolder()
            initialBreadcrumbs = emptyList()
        }
        navigationStack.value = initialBreadcrumbs
        checkStoragePermission()
        refreshApps()
    }

    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (navigationStack.value.size > initialBreadcrumbs.size) {
                        navigateBack()
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            },
        )
    }

    private fun navigateIntoFolder(app: AppInfo) {
        navigationStack.value = navigationStack.value + FolderEntry(app.folder, app.name)
        refreshApps()
    }

    private fun navigateBack() {
        if (navigationStack.value.size > initialBreadcrumbs.size) {
            navigationStack.value = navigationStack.value.dropLast(1)
            refreshApps()
        }
    }

    private fun currentFolder(): File =
        if (navigationStack.value.isEmpty()) rootFolder else navigationStack.value.last().folder

    private fun checkStoragePermission() {
        val currentSettings = settings.value ?: return
        val defaultPath = settingsRepository.getDefaultAppsPath()
        val currentPath = currentSettings.appsRootPath

        hasStoragePermission.value =
            if (currentPath.startsWith(defaultPath) ||
                currentPath.startsWith(getExternalFilesDir(null)?.absolutePath ?: "")
            ) {
                true
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun refreshApps() {
        val folder = currentFolder()
        if (!folder.exists()) folder.mkdirs()
        apps.value = AppDiscoveryService.discoverApps(folder)
    }

    private fun launchApp(app: AppInfo) {
        val intent =
            Intent(this, AppActivity::class.java).apply {
                putExtra(AppActivity.EXTRA_APP_FOLDER, app.folder.absolutePath)
                putExtra(AppActivity.EXTRA_APP_NAME, app.name)
            }
        startActivity(intent)
    }

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }
}

data class LauncherState(
    val apps: List<AppInfo>,
    val settings: LauncherSettings,
    val hasPermission: Boolean,
    val navigationStack: List<LauncherActivity.FolderEntry>,
    val initialBreadcrumbsSize: Int,
)

data class LauncherCallbacks(
    val onAppClick: (AppInfo) -> Unit,
    val onFolderClick: (AppInfo) -> Unit,
    val onSettingsClick: () -> Unit,
    val onBackClick: () -> Unit,
    val onRequestPermission: () -> Unit,
)

data class AppGridConfig(
    val columns: Int,
    val showNames: Boolean,
    val onAppClick: (AppInfo) -> Unit,
    val onFolderClick: (AppInfo) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherScreen(
    state: LauncherState,
    callbacks: LauncherCallbacks,
) {
    Scaffold(
        modifier =
            Modifier
                .fillMaxSize()
                .systemBarsPadding(),
        topBar = { LauncherTopBar(state = state, callbacks = callbacks) },
    ) { paddingValues ->
        when {
            !state.hasPermission ->
                PermissionRequestScreen(
                    modifier = Modifier.padding(paddingValues),
                    onRequestPermission = callbacks.onRequestPermission,
                )
            state.apps.isEmpty() ->
                EmptyStateScreen(
                    modifier = Modifier.padding(paddingValues),
                    appsFolder = state.settings.appsRootPath,
                    onSettingsClick = callbacks.onSettingsClick,
                )
            else ->
                AppGrid(
                    modifier = Modifier.padding(paddingValues),
                    apps = state.apps,
                    config =
                        AppGridConfig(
                            columns = state.settings.gridColumns,
                            showNames = state.settings.showAppNames,
                            onAppClick = callbacks.onAppClick,
                            onFolderClick = callbacks.onFolderClick,
                        ),
                )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LauncherTopBar(
    state: LauncherState,
    callbacks: LauncherCallbacks,
) {
    val isInSubfolder = state.navigationStack.size > state.initialBreadcrumbsSize
    val currentFolderName = state.navigationStack.lastOrNull()?.name
    val appCount = state.apps.count { !it.isFolder }
    val folderCount = state.apps.count { it.isFolder }
    val subtitle =
        if (state.hasPermission && state.apps.isNotEmpty()) buildSubtitle(appCount, folderCount) else null

    TopAppBar(
        title = { LauncherTitle(currentFolderName = currentFolderName, subtitle = subtitle) },
        navigationIcon = {
            if (isInSubfolder) {
                IconButton(onClick = callbacks.onBackClick) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = {
            IconButton(onClick = callbacks.onSettingsClick) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
    )
}

@Composable
private fun LauncherTitle(
    currentFolderName: String?,
    subtitle: String?,
) {
    Column {
        Text(
            text = currentFolderName ?: "Moonstone Apps",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun buildSubtitle(
    appCount: Int,
    folderCount: Int,
): String =
    buildString {
        if (appCount > 0) append(formatCount(appCount, "app"))
        if (appCount > 0 && folderCount > 0) append(", ")
        if (folderCount > 0) append(formatCount(folderCount, "folder"))
    }

private fun formatCount(
    count: Int,
    noun: String,
) = "$count $noun${if (count != 1) "s" else ""}"

@Composable
fun PermissionRequestScreen(
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Storage Permission Required",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Moonstone Apps needs permission to read apps from your storage.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(32.dp))
            FilledTonalButton(onClick = onRequestPermission) {
                Text("Grant Permission")
            }
        }
    }
}

@Suppress("LongMethod")
@Composable
fun EmptyStateScreen(
    modifier: Modifier = Modifier,
    appsFolder: String,
    onSettingsClick: () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        EmptyStateContent(appsFolder = appsFolder, onSettingsClick = onSettingsClick)
    }
}

@Composable
private fun EmptyStateContent(
    appsFolder: String,
    onSettingsClick: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No Apps Yet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Place your KleinLisp apps in:",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        AppsFolderLabel(appsFolder = appsFolder)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Each app should be in its own folder with an app.scm file.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(32.dp))
        SettingsButton(onClick = onSettingsClick)
    }
}

@Composable
private fun AppsFolderLabel(appsFolder: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Text(
            text = appsFolder,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsButton(onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text("Open Settings")
    }
}

@Suppress("LongParameterList")
@Composable
fun AppGrid(
    modifier: Modifier = Modifier,
    apps: List<AppInfo>,
    config: AppGridConfig,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(config.columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        itemsIndexed(apps, key = { _, app -> app.id }) { index, app ->
            AnimatedAppIcon(
                app = app,
                index = index,
                showName = config.showNames,
                onClick = { if (app.isFolder) config.onFolderClick(app) else config.onAppClick(app) },
            )
        }
    }
}

@Composable
fun AnimatedAppIcon(
    app: AppInfo,
    index: Int,
    showName: Boolean,
    onClick: () -> Unit,
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec =
            tween(
                durationMillis = 300,
                delayMillis = index * 50,
                easing = FastOutSlowInEasing,
            ),
        label = "alphaAnimation",
    )

    val animatedOffsetY by animateDpAsState(
        targetValue = if (isVisible) 0.dp else 24.dp,
        animationSpec =
            tween(
                durationMillis = 300,
                delayMillis = index * 50,
                easing = FastOutSlowInEasing,
            ),
        label = "offsetAnimation",
    )

    Box(
        modifier =
            Modifier
                .alpha(animatedAlpha)
                .offset { IntOffset(0, animatedOffsetY.roundToPx()) },
    ) {
        AppIcon(
            app = app,
            showName = showName,
            onClick = onClick,
        )
    }
}

@Suppress("LongMethod")
@Composable
fun AppIcon(
    app: AppInfo,
    showName: Boolean,
    onClick: () -> Unit,
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "iconScale",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .scale(scale)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                        },
                        onTap = { onClick() },
                    )
                },
    ) {
        AppIconCard(app = app)

        if (showName) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = app.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AppIconCard(app: AppInfo) {
    var bitmap by remember(app.iconPath) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(app.iconPath) {
        bitmap =
            withContext(Dispatchers.IO) {
                app.iconPath?.let { AppIconLoader.loadBitmap(it) }
            }
    }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(22.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = if (bitmap != null) MaterialTheme.colorScheme.surface else Color.Transparent,
                ),
        ) {
            AppIconContent(app = app, bitmap = bitmap)
        }
    }
}

private const val DEFAULT_APP_EMOJI = "🤖" // U+1F916

@Composable
private fun AppIconContent(
    app: AppInfo,
    bitmap: android.graphics.Bitmap?,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = app.name,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(22.dp)),
            )
        } else {
            val emoji = app.emoji ?: if (app.isFolder) AppInfo.DEFAULT_FOLDER_EMOJI else DEFAULT_APP_EMOJI
            EmojiAppIcon(emoji)
        }
    }
}

@Composable
private fun EmojiAppIcon(emoji: String) {
    Text(
        text = emoji,
        fontSize = 48.sp,
        textAlign = TextAlign.Center,
    )
}
