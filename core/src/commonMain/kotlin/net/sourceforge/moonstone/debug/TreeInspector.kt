package net.sourceforge.moonstone.debug

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.sourceforge.moonstone.components.ComponentElement
import net.sourceforge.moonstone.components.TextElement
import net.sourceforge.moonstone.components.UIElement

/**
 * Tree inspector side panel for debugging UIElement hierarchies.
 * Shows an expandable tree view of the UI structure with property details.
 */
@Composable
fun TreeInspector(
    rootElement: UIElement?,
    modifier: Modifier = Modifier
) {
    var selectedElement by remember { mutableStateOf<UIElement?>(null) }
    val scrollState = rememberScrollState()

    Row(modifier = modifier) {
        // Tree view
        Column(
            modifier = Modifier
                .width(200.dp)
                .fillMaxHeight()
                .background(Color(0xFF252526))
                .verticalScroll(scrollState)
                .padding(8.dp)
        ) {
            Text(
                text = "Component Tree",
                color = Color(0xFF569CD6),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (rootElement != null) {
                TreeNode(
                    element = rootElement,
                    depth = 0,
                    selectedElement = selectedElement,
                    onSelect = { selectedElement = it }
                )
            } else {
                Text(
                    text = "No UI loaded",
                    color = Color(0xFF808080),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color(0xFF3E3E42))
        )

        // Property inspector
        PropertyInspector(
            element = selectedElement,
            modifier = Modifier
                .width(200.dp)
                .fillMaxHeight()
        )
    }
}

/**
 * A single node in the tree view.
 */
@Composable
private fun TreeNode(
    element: UIElement,
    depth: Int,
    selectedElement: UIElement?,
    onSelect: (UIElement) -> Unit
) {
    var expanded by remember { mutableStateOf(depth < 2) }
    val isSelected = element === selectedElement
    val hasChildren = element.children.isNotEmpty()

    Column {
        // Node row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isSelected) Color(0xFF094771) else Color.Transparent,
                    RoundedCornerShape(2.dp)
                )
                .clickable { onSelect(element) }
                .padding(start = (depth * 12).dp, top = 2.dp, bottom = 2.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Expand/collapse indicator
            if (hasChildren) {
                Text(
                    text = if (expanded) "▼" else "▶",
                    color = Color(0xFF808080),
                    fontSize = 8.sp,
                    modifier = Modifier
                        .clickable { expanded = !expanded }
                        .padding(end = 4.dp)
                )
            } else {
                Spacer(modifier = Modifier.width(12.dp))
            }

            // Element type
            Text(
                text = element.type,
                color = getTypeColor(element.type),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )

            // Brief info
            val brief = getBriefInfo(element)
            if (brief != null) {
                Text(
                    text = " $brief",
                    color = Color(0xFF808080),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }
        }

        // Children
        if (expanded && hasChildren) {
            element.children.forEach { child ->
                TreeNode(
                    element = child,
                    depth = depth + 1,
                    selectedElement = selectedElement,
                    onSelect = onSelect
                )
            }
        }
    }
}

/**
 * Property inspector panel showing details of the selected element.
 */
@Composable
private fun PropertyInspector(
    element: UIElement?,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .background(Color(0xFF1E1E1E))
            .verticalScroll(scrollState)
            .padding(8.dp)
    ) {
        Text(
            text = "Properties",
            color = Color(0xFF569CD6),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (element == null) {
            Text(
                text = "Select a component",
                color = Color(0xFF808080),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            return@Column
        }

        // Element type
        PropertyRow("type", element.type)

        Spacer(modifier = Modifier.height(8.dp))

        // Props
        if (element.props.isNotEmpty()) {
            Text(
                text = "Props:",
                color = Color(0xFFDCDCAA),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            element.props.forEach { (key, value) ->
                PropertyRow(key, formatValue(value))
            }
        }

        // Children count
        if (element.children.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            PropertyRow("children", "${element.children.size} element(s)")
        }
    }
}

/**
 * A single property row in the inspector.
 */
@Composable
private fun PropertyRow(name: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = "$name:",
            color = Color(0xFF9CDCFE),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            color = Color(0xFFCE9178),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 2
        )
    }
}

/**
 * Get color for element type (syntax highlighting style).
 */
private fun getTypeColor(type: String): Color {
    return when (type) {
        "text" -> Color(0xFFCE9178)
        "button" -> Color(0xFF4EC9B0)
        "column", "row", "box" -> Color(0xFFDCDCAA)
        "surface", "scaffold" -> Color(0xFF569CD6)
        else -> Color(0xFFD4D4D4)
    }
}

/**
 * Get brief info to display next to the element type.
 */
private fun getBriefInfo(element: UIElement): String? {
    return when (element.type) {
        "text" -> {
            val value = element.props["value"]
            when (value) {
                is String -> "\"${value.take(15)}${if (value.length > 15) "..." else ""}\""
                else -> null
            }
        }
        "button" -> element.props["style"]?.toString()
        else -> null
    }
}

/**
 * Format a property value for display.
 */
private fun formatValue(value: Any?): String {
    return when (value) {
        null -> "null"
        is String -> "\"$value\""
        is Number -> value.toString()
        is Boolean -> value.toString()
        is UIElement -> "<${value.type}>"
        is List<*> -> "[${value.size} items]"
        else -> value::class.simpleName ?: value.toString()
    }
}
