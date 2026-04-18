package net.sourceforge.moonstone.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Configuration for debug visualization.
 */
data class DebugConfig(
    val showComponentBorders: Boolean = false,
    val showComponentNames: Boolean = false,
    val borderColor: Color = Color(0xFF2196F3),
    val nameBackgroundColor: Color = Color(0xFF2196F3),
    val nameTextColor: Color = Color.White
)

/**
 * Debug panel displayed at the top of the screen.
 * Shows reload count, last error, and control buttons.
 */
@Composable
fun DebugPanel(
    reloadCount: Int,
    lastError: Throwable?,
    hotReloadEnabled: Boolean,
    onReload: () -> Unit,
    onToggleInspector: () -> Unit,
    inspectorVisible: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E1E))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Status info
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "[DEBUG]",
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )

                if (hotReloadEnabled) {
                    Text(
                        text = "Hot Reload: ON",
                        color = Color(0xFF8BC34A),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = "Reloads: $reloadCount",
                    color = Color(0xFFBDBDBD),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Right side: Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DebugButton(
                    text = "Reload",
                    onClick = onReload
                )
                DebugButton(
                    text = if (inspectorVisible) "Hide Inspector" else "Inspector",
                    onClick = onToggleInspector,
                    active = inspectorVisible
                )
            }
        }

        // Error display
        if (lastError != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .background(
                        Color(0xFF4A1515),
                        RoundedCornerShape(4.dp)
                    )
                    .border(
                        1.dp,
                        Color(0xFFE53935),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(8.dp)
            ) {
                Column {
                    Text(
                        text = "Error: ${lastError::class.simpleName}",
                        color = Color(0xFFE57373),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = lastError.message ?: "Unknown error",
                        color = Color(0xFFFFCDD2),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 4.dp),
                        maxLines = 3
                    )
                }
            }
        }
    }
}

/**
 * A styled debug button.
 */
@Composable
private fun DebugButton(
    text: String,
    onClick: () -> Unit,
    active: Boolean = false
) {
    Box(
        modifier = Modifier
            .background(
                if (active) Color(0xFF1565C0) else Color(0xFF424242),
                RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Wrapper that adds debug borders and labels to components.
 * Use this to wrap individual components for debugging.
 */
@Composable
fun DebugOverlayWrapper(
    componentName: String,
    config: DebugConfig,
    content: @Composable () -> Unit
) {
    if (!config.showComponentBorders && !config.showComponentNames) {
        content()
        return
    }

    Box {
        // Content with optional border
        Box(
            modifier = if (config.showComponentBorders) {
                Modifier.border(1.dp, config.borderColor, RoundedCornerShape(2.dp))
            } else {
                Modifier
            }
        ) {
            content()
        }

        // Component name label
        if (config.showComponentNames) {
            Text(
                text = componentName,
                color = config.nameTextColor,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .background(
                        config.nameBackgroundColor.copy(alpha = 0.8f),
                        RoundedCornerShape(2.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
    }
}
