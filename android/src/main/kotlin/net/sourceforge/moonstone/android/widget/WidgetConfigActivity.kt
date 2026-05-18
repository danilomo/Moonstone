package net.sourceforge.moonstone.android.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.sourceforge.moonstone.android.MoonstoneTheme
import net.sourceforge.moonstone.android.model.AppInfo
import net.sourceforge.moonstone.android.service.AppDiscoveryService
import net.sourceforge.moonstone.android.service.SettingsRepository
import net.sourceforge.moonstone.android.ui.AppIconLoader
import java.io.File
import kotlin.math.absoluteValue

class WidgetConfigActivity : ComponentActivity() {
    data class FolderEntry(val folder: File, val name: String)

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var rootFolder: File

    private val navigationStack = mutableStateOf<List<FolderEntry>>(emptyList())
    private val items = mutableStateOf<List<AppInfo>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        enableEdgeToEdge()

        appWidgetId =
            intent?.extras?.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val settings = SettingsRepository(this).loadSettings()
        rootFolder = settings.getAppsRootFolder().also { if (!it.exists()) it.mkdirs() }
        items.value = AppDiscoveryService.discoverApps(rootFolder)

        setupBackPressedCallback()

        setContent {
            MoonstoneTheme {
                WidgetConfigScreen(
                    items = items.value,
                    navigationStack = navigationStack.value,
                    onAppSelected = { app -> saveAndFinish(buildAppConfig(app)) },
                    onFolderSelect = { app -> saveAndFinish(buildFolderConfig(app)) },
                    onFolderNavigate = { app -> navigateInto(app) },
                    onBackClick = { handleBack() },
                )
            }
        }
    }

    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    handleBack()
                }
            },
        )
    }

    private fun handleBack() {
        val stack = navigationStack.value
        if (stack.isNotEmpty()) {
            navigationStack.value = stack.dropLast(1)
            val folder = if (navigationStack.value.isEmpty()) rootFolder else navigationStack.value.last().folder
            items.value = AppDiscoveryService.discoverApps(folder)
        } else {
            finish()
        }
    }

    private fun navigateInto(app: AppInfo) {
        navigationStack.value = navigationStack.value + FolderEntry(app.folder, app.name)
        items.value = AppDiscoveryService.discoverApps(app.folder)
    }

    private fun buildAppConfig(app: AppInfo) =
        WidgetConfig(
            widgetId = appWidgetId,
            appFolder = app.folder.absolutePath,
            appName = app.name,
            iconPath = app.iconPath?.absolutePath,
            emoji = app.emoji,
            isFolder = false,
        )

    private fun buildFolderConfig(app: AppInfo) =
        WidgetConfig(
            widgetId = appWidgetId,
            appFolder = app.folder.absolutePath,
            appName = app.name,
            iconPath = app.iconPath?.absolutePath,
            emoji = app.emoji ?: AppInfo.DEFAULT_FOLDER_EMOJI,
            isFolder = true,
        )

    private fun saveAndFinish(config: WidgetConfig) {
        val repository = WidgetRepository(this)
        repository.saveWidgetConfig(config)

        val appWidgetManager = AppWidgetManager.getInstance(this)
        AppLauncherWidgetProvider.updateWidget(this, appWidgetManager, appWidgetId, repository)

        val resultValue =
            Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
        setResult(RESULT_OK, resultValue)
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigScreen(
    items: List<AppInfo>,
    navigationStack: List<WidgetConfigActivity.FolderEntry>,
    onAppSelected: (AppInfo) -> Unit,
    onFolderSelect: (AppInfo) -> Unit,
    onFolderNavigate: (AppInfo) -> Unit,
    onBackClick: () -> Unit,
) {
    val currentFolderName = navigationStack.lastOrNull()?.name
    val isInSubfolder = navigationStack.isNotEmpty()

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
                            text = currentFolderName ?: "Select App",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = if (isInSubfolder) "Choose an app or folder" else "Choose an app or folder for this widget",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isInSubfolder) "Back" else "Cancel",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        if (items.isEmpty() && !isInSubfolder) {
            EmptyAppsScreen(modifier = Modifier.padding(paddingValues))
        } else {
            WidgetAppSelectionList(
                modifier = Modifier.padding(paddingValues),
                items = items,
                onAppSelected = onAppSelected,
                onFolderSelect = onFolderSelect,
                onFolderNavigate = onFolderNavigate,
            )
        }
    }
}

@Composable
fun WidgetAppSelectionList(
    modifier: Modifier = Modifier,
    items: List<AppInfo>,
    onAppSelected: (AppInfo) -> Unit,
    onFolderSelect: (AppInfo) -> Unit,
    onFolderNavigate: (AppInfo) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items, key = { it.id }) { item ->
            if (item.isFolder) {
                FolderSelectionItem(
                    app = item,
                    onSelect = { onFolderSelect(item) },
                    onNavigate = { onFolderNavigate(item) },
                )
            } else {
                AppSelectionItem(
                    app = item,
                    onClick = { onAppSelected(item) },
                )
            }
        }
    }
}

@Composable
fun EmptyAppsScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Apps Found",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Add some KleinLisp apps to create a widget shortcut.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun AppSelectionList(
    modifier: Modifier = Modifier,
    apps: List<AppInfo>,
    onAppSelected: (AppInfo) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(apps, key = { it.id }) { app ->
            AppSelectionItem(
                app = app,
                onClick = { onAppSelected(app) },
            )
        }
    }
}

@Suppress("LongMethod")
@Composable
fun FolderSelectionItem(
    app: AppInfo,
    onSelect: () -> Unit,
    onNavigate: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onSelect),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppSelectionIcon(app = app)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (app.description != null) {
                    Text(
                        text = app.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text(
                        text = "Folder",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // Navigate-into button
            IconButton(onClick = onNavigate) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open folder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Suppress("LongMethod")
@Composable
fun AppSelectionItem(
    app: AppInfo,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppSelectionIcon(app = app)
            Spacer(modifier = Modifier.width(16.dp))
            AppSelectionInfo(app = app)
        }
    }
}

@Composable
private fun AppSelectionIcon(app: AppInfo) {
    Box(
        modifier =
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp)),
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
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                DefaultAppIconSmall(app.name)
            }
        } else if (app.emoji != null) {
            EmojiIconSmall(app.emoji)
        } else {
            DefaultAppIconSmall(app.name)
        }
    }
}

@Composable
private fun EmojiIconSmall(emoji: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.AppSelectionInfo(app: AppInfo) {
    Column(modifier = Modifier.weight(1f)) {
        Text(
            text = app.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (app.description != null) {
            Text(
                text = app.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun DefaultAppIconSmall(appName: String) {
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
                    shape = RoundedCornerShape(12.dp),
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = appName.take(2).uppercase(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}
