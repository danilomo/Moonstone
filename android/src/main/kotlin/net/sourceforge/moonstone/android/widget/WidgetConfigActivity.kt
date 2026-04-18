package net.sourceforge.moonstone.android.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import kotlin.math.absoluteValue

/**
 * Configuration activity for selecting which app a widget should launch.
 *
 * This activity is launched when the user adds a new widget to their home screen.
 */
class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set result to CANCELED initially - will be changed if user completes config
        setResult(RESULT_CANCELED)

        enableEdgeToEdge()

        // Get the widget ID from the intent
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // If no valid widget ID, finish
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // Discover apps
        val settings = SettingsRepository(this).loadSettings()
        val rootFolder = settings.getAppsRootFolder()
        if (!rootFolder.exists()) {
            rootFolder.mkdirs()
        }
        val apps = AppDiscoveryService.discoverApps(rootFolder)

        setContent {
            MoonstoneTheme {
                WidgetConfigScreen(
                    apps = apps,
                    onAppSelected = { app -> onAppSelected(app) },
                    onBackClick = { finish() }
                )
            }
        }
    }

    private fun onAppSelected(app: AppInfo) {
        // Save widget configuration
        val config = WidgetConfig(
            widgetId = appWidgetId,
            appFolder = app.folder.absolutePath,
            appName = app.name,
            iconPath = app.iconPath?.absolutePath
        )

        val repository = WidgetRepository(this)
        repository.saveWidgetConfig(config)

        // Update the widget
        val appWidgetManager = AppWidgetManager.getInstance(this)
        AppLauncherWidgetProvider.updateWidget(
            this,
            appWidgetManager,
            appWidgetId,
            repository
        )

        // Return success result
        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(RESULT_OK, resultValue)
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigScreen(
    apps: List<AppInfo>,
    onAppSelected: (AppInfo) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Select App",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Choose an app for this widget",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Cancel"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (apps.isEmpty()) {
            EmptyAppsScreen(
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            AppSelectionList(
                modifier = Modifier.padding(paddingValues),
                apps = apps,
                onAppSelected = onAppSelected
            )
        }
    }
}

@Composable
fun EmptyAppsScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Apps Found",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Add some KleinLisp apps to create a widget shortcut.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AppSelectionList(
    modifier: Modifier = Modifier,
    apps: List<AppInfo>,
    onAppSelected: (AppInfo) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(apps, key = { it.id }) { app ->
            AppSelectionItem(
                app = app,
                onClick = { onAppSelected(app) }
            )
        }
    }
}

@Composable
fun AppSelectionItem(
    app: AppInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (app.iconPath != null) {
                    val bitmap = remember(app.iconPath) {
                        AppIconLoader.loadBitmap(app.iconPath)
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = app.name,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        DefaultAppIconSmall(app.name)
                    }
                } else {
                    DefaultAppIconSmall(app.name)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // App info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (app.description != null) {
                    Text(
                        text = app.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun DefaultAppIconSmall(appName: String) {
    val hue = (appName.hashCode() % 360).absoluteValue.toFloat()
    val gradientColors = listOf(
        Color.hsl(hue, 0.55f, 0.65f),
        Color.hsl((hue + 30) % 360, 0.60f, 0.50f)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(gradientColors),
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = appName.take(2).uppercase(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
