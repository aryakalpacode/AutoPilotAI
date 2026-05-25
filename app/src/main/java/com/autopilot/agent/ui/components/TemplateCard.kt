package com.autopilot.agent.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Card component for quick action templates on the home screen.
 */
@Composable
fun TemplateCard(
    title: String,
    description: String,
    iconName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = getIconForTemplate(iconName)

    Card(
        modifier = modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

private fun getIconForTemplate(name: String): ImageVector {
    return when (name.lowercase()) {
        "research" -> Icons.Default.Science
        "news" -> Icons.Default.Newspaper
        "code" -> Icons.Default.Code
        "write" -> Icons.Default.Edit
        "analyze" -> Icons.Default.Analytics
        "study" -> Icons.Default.School
        "travel" -> Icons.Default.Flight
        "custom" -> Icons.Default.AutoAwesome
        else -> Icons.Default.PlayArrow
    }
}
