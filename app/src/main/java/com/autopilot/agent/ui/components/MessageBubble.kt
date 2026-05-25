package com.autopilot.agent.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autopilot.agent.domain.model.Message
import com.autopilot.agent.domain.model.MessageRole
import com.autopilot.agent.ui.theme.*
import com.autopilot.agent.util.toFormattedTime
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser

/**
 * Composable for rendering different message types in the chat.
 */
@Composable
fun MessageBubble(
    message: Message,
    isDarkTheme: Boolean = false,
    showDebug: Boolean = false,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current

    when (message.role) {
        MessageRole.USER -> UserMessageBubble(message, clipboardManager, modifier)
        MessageRole.ASSISTANT -> AssistantMessageBubble(message, isDarkTheme, showDebug, clipboardManager, modifier)
        MessageRole.TOOL_CALL -> ToolCallBubble(message, isDarkTheme, modifier)
        MessageRole.TOOL_RESULT -> ToolResultBubble(message, isDarkTheme, modifier)
        MessageRole.ERROR -> ErrorBubble(message, modifier)
        MessageRole.SYSTEM -> SystemMessageBubble(message, modifier)
    }
}

@Composable
private fun UserMessageBubble(
    message: Message,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    modifier: Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp),
                color = UserBubble,
                modifier = Modifier.clickable {
                    clipboardManager.setText(AnnotatedString(message.content))
                }
            ) {
                Text(
                    text = message.content,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Text(
                text = message.timestamp.toFormattedTime(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, end = 4.dp)
            )
        }
    }
}

@Composable
private fun AssistantMessageBubble(
    message: Message,
    isDarkTheme: Boolean,
    showDebug: Boolean,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    modifier: Modifier
) {
    // Try to parse as agent action JSON
    val displayContent = remember(message.content, showDebug) {
        if (showDebug) {
            message.content
        } else {
            tryExtractDisplayContent(message.content)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.SmartToy,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(28.dp)
                .padding(top = 4.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
                color = if (isDarkTheme) AssistantBubbleDark else AssistantBubble,
                modifier = Modifier
                    .animateContentSize()
                    .clickable {
                        clipboardManager.setText(AnnotatedString(displayContent))
                    }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = displayContent,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (message.model != null) {
                        Text(
                            text = message.model,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.padding(top = 2.dp, start = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message.timestamp.toFormattedTime(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    modifier = Modifier
                        .size(14.dp)
                        .clickable {
                            clipboardManager.setText(AnnotatedString(displayContent))
                        },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ToolCallBubble(
    message: Message,
    isDarkTheme: Boolean,
    modifier: Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = if (isDarkTheme) ToolCallBubbleDark else ToolCallBubble
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🔧 ${message.toolName ?: "Tool"}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = message.timestamp.toFormattedTime(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (expanded && message.toolInput != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = tryPrettyJson(message.toolInput),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolResultBubble(
    message: Message,
    isDarkTheme: Boolean,
    modifier: Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme) SurfaceVariantDark else SurfaceVariantLight
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "📋 Result: ${message.toolName ?: ""}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Success
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = if (expanded) "▲" else "▼",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                val preview = message.content.take(150)
                Text(
                    text = if (expanded) message.content else "$preview${if (message.content.length > 150) "..." else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ErrorBubble(
    message: Message,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ErrorBubble)
        ) {
            Text(
                text = "⚠️ ${message.content}",
                style = MaterialTheme.typography.bodySmall,
                color = Error,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
private fun SystemMessageBubble(
    message: Message,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message.content,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Try to extract readable content from agent JSON response. */
private fun tryExtractDisplayContent(content: String): String {
    return try {
        val json = JsonParser.parseString(content).asJsonObject
        val thought = json.get("thought")?.asString
        val action = json.get("action")?.asString
        val actionInput = json.get("action_input")?.asJsonObject

        when (action) {
            "RESPOND" -> actionInput?.get("message")?.asString ?: content
            "COMPLETE" -> {
                val summary = actionInput?.get("summary")?.asString ?: ""
                val details = actionInput?.get("details")?.asString ?: ""
                if (details.isNotBlank()) "$summary\n\n$details" else summary
            }
            else -> {
                if (thought?.isNotBlank() == true) {
                    "💭 $thought"
                } else content
            }
        }
    } catch (e: Exception) {
        content
    }
}

/** Try to pretty-print JSON. */
private fun tryPrettyJson(json: String?): String {
    if (json == null) return ""
    return try {
        val parsed = JsonParser.parseString(json)
        GsonBuilder().setPrettyPrinting().create().toJson(parsed)
    } catch (e: Exception) {
        json
    }
}
