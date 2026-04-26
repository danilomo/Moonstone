package net.sourceforge.moonstone.debug

import androidx.compose.ui.graphics.Color

/**
 * Configuration for debug visualization.
 */
data class DebugConfig(
    val showComponentBorders: Boolean = false,
    val showComponentNames: Boolean = false,
    val borderColor: Color = Color(0xFF2196F3),
    val nameBackgroundColor: Color = Color(0xFF2196F3),
    val nameTextColor: Color = Color.White,
)
