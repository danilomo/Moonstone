package net.sourceforge.moonstone.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import net.sourceforge.moonstone.android.model.AppInfo
import net.sourceforge.moonstone.android.model.LauncherSettings
import net.sourceforge.moonstone.android.service.AppDiscoveryService
import net.sourceforge.moonstone.android.service.SettingsRepository
import net.sourceforge.moonstone.android.ui.AppIconLoader
import kotlin.math.absoluteValue

/**
 * Main launcher activity for KleinLisp apps.
 *
 * Displays an iOS-like grid of installed KleinLisp apps that can be launched.
 */
class LauncherActivity : ComponentActivity() {
    private lateinit var settingsRepository: SettingsRepository

    private val storagePermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { _ ->
            // Refresh apps regardless of result
            refreshApps()
        }

    // State - settings initialized after settingsRepository is created
    private var apps = mutableStateOf<List<AppInfo>>(emptyList())
    private var settings = mutableStateOf<LauncherSettings?>(null)
    private var hasStoragePermission = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        settingsRepository = SettingsRepository(this)
        settings.value = settingsRepository.loadSettings()

        checkStoragePermission()

        setContent {
            MoonstoneTheme {
                settings.value?.let { currentSettings ->
                    LauncherScreen(
                        apps = apps.value,
                        settings = currentSettings,
                        hasPermission = hasStoragePermission.value,
                        onAppClick = { app -> launchApp(app) },
                        onSettingsClick = { openSettings() },
                        onRequestPermission = { requestStoragePermission() },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh settings and apps when returning from settings
        settings.value = settingsRepository.loadSettings()
        checkStoragePermission()
        refreshApps()
    }

    private fun checkStoragePermission() {
        val currentSettings = settings.value ?: return

        // App-specific external storage doesn't require permissions
        // Only check permission for custom paths on older Android versions
        val defaultPath = settingsRepository.getDefaultAppsPath()
        val currentPath = currentSettings.appsRootPath

        hasStoragePermission.value =
            if (currentPath.startsWith(defaultPath) ||
                currentPath.startsWith(getExternalFilesDir(null)?.absolutePath ?: "")
            ) {
                // Using app-specific storage - no permissions needed
                true
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                // Android 7-10: Need READ_EXTERNAL_STORAGE for public directories
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                // Android 11+
                true
            }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun refreshApps() {
        val currentSettings = settings.value ?: return
        val rootFolder = currentSettings.getAppsRootFolder()
        // Create folder if it doesn't exist
        if (!rootFolder.exists()) {
            rootFolder.mkdirs()
        }
        apps.value = AppDiscoveryService.discoverApps(rootFolder)
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
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongParameterList", "LongMethod", "CognitiveComplexMethod")
@Composable
fun LauncherScreen(
    apps: List<AppInfo>,
    settings: LauncherSettings,
    hasPermission: Boolean,
    onAppClick: (AppInfo) -> Unit,
    onSettingsClick: () -> Unit,
    onRequestPermission: () -> Unit,
) {
    Scaffold(
        modifier =
            Modifier
                .fillMaxSize()
                .systemBarsPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "KleinLisp Apps",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (hasPermission && apps.isNotEmpty()) {
                            Text(
                                text = "${apps.size} app${if (apps.size != 1) "s" else ""} installed",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            )
        },
    ) { paddingValues ->
        if (!hasPermission) {
            PermissionRequestScreen(
                modifier = Modifier.padding(paddingValues),
                onRequestPermission = onRequestPermission,
            )
        } else if (apps.isEmpty()) {
            EmptyStateScreen(
                modifier = Modifier.padding(paddingValues),
                appsFolder = settings.appsRootPath,
                onSettingsClick = onSettingsClick,
            )
        } else {
            AppGrid(
                modifier = Modifier.padding(paddingValues),
                apps = apps,
                columns = settings.gridColumns,
                showNames = settings.showAppNames,
                onAppClick = onAppClick,
            )
        }
    }
}

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
                text = "KleinLisp Apps needs permission to read apps from your storage.",
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

@Composable
fun AppGrid(
    modifier: Modifier = Modifier,
    apps: List<AppInfo>,
    columns: Int,
    showNames: Boolean,
    onAppClick: (AppInfo) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        itemsIndexed(apps, key = { _, app -> app.id }) { index, app ->
            AnimatedAppIcon(
                app = app,
                index = index,
                showName = showNames,
                onClick = { onAppClick(app) },
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

    val hue = (app.name.hashCode() % 360).absoluteValue.toFloat()
    val glowColor = Color.hsl(hue, 0.7f, 0.55f)
    val glowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.35f else 0.18f,
        animationSpec = tween(durationMillis = 100),
        label = "glowAlpha",
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
        AppIconCard(app = app, glowColor = glowColor, glowAlpha = glowAlpha)

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
private fun AppIconCard(
    app: AppInfo,
    glowColor: Color,
    glowAlpha: Float,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .drawBehind {
                    // Draw soft ambient glow centered behind the card
                    val glowExpand = 12.dp.toPx()
                    val cornerRadius = 26.dp.toPx()

                    // Outer soft layer - evenly centered
                    drawRoundRect(
                        color = glowColor.copy(alpha = glowAlpha * 0.5f),
                        topLeft = Offset(-glowExpand, -glowExpand * 0.6f),
                        size = Size(size.width + glowExpand * 2f, size.height + glowExpand * 2f),
                        cornerRadius = CornerRadius(cornerRadius + 4.dp.toPx(), cornerRadius + 4.dp.toPx()),
                    )
                },
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(22.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
        ) {
            AppIconContent(app = app)
        }
    }
}

@Composable
private fun AppIconContent(app: AppInfo) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (app.iconPath != null) {
            val bitmap =
                remember(app.iconPath) {
                    AppIconLoader.loadBitmap(app.iconPath)
                }
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
                DefaultAppIcon(app.name)
            }
        } else {
            DefaultAppIcon(app.name)
        }
    }
}

/**
 * Generates a visually appealing gradient for default app icons
 * based on the app name for consistent, unique colors per app.
 */
@Composable
fun DefaultAppIcon(appName: String) {
    val hue = (appName.hashCode() % 360).absoluteValue.toFloat()
    val gradientColors =
        listOf(
            Color.hsl(hue, 0.55f, 0.65f),
            Color.hsl((hue + 30) % 360, 0.60f, 0.50f),
        )

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(gradientColors),
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = appName.take(2).uppercase(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}
