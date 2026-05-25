package com.autopilot.agent.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.autopilot.agent.domain.model.AgentState
import com.autopilot.agent.ui.theme.*

/**
 * Animated status indicator showing the agent's current state.
 */
@Composable
fun AgentStatusIndicator(
    state: AgentState,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "status")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate"
    )

    val (icon, label, color, animate) = remember(state) {
        when (state) {
            is AgentState.Idle -> StatusInfo(Icons.Default.CheckCircle, "Ready", Success, false)
            is AgentState.Planning -> StatusInfo(Icons.Default.AccountTree, "Planning...", Info, true)
            is AgentState.Thinking -> StatusInfo(Icons.Default.Psychology, "Thinking...", Primary, true)
            is AgentState.ExecutingTool -> StatusInfo(
                when (state.toolName) {
                    "WEB_SEARCH" -> Icons.Default.Search
                    "WEB_SCRAPE" -> Icons.Default.Description
                    "FILE_MANAGER" -> Icons.Default.CreateNewFolder
                    "CODE_EXECUTOR" -> Icons.Default.Terminal
                    "CALCULATOR" -> Icons.Default.Calculate
                    "CLIPBOARD" -> Icons.Default.ContentPaste
                    "DEVICE_INFO" -> Icons.Default.PhoneAndroid
                    "NOTES_DATABASE" -> Icons.Default.Note
                    "REMINDER" -> Icons.Default.Alarm
                    "TEXT_PROCESSOR" -> Icons.Default.TextFields
                    else -> Icons.Default.Build
                },
                "Using ${state.toolName}...",
                Secondary,
                true
            )
            is AgentState.Responding -> StatusInfo(Icons.Default.Chat, "Responding...", Primary, true)
            is AgentState.WaitingForConfirmation -> StatusInfo(Icons.Default.HelpOutline, "Confirm?", Warning, true)
            is AgentState.Complete -> StatusInfo(Icons.Default.TaskAlt, "Complete", Success, false)
            is AgentState.Error -> StatusInfo(Icons.Default.ErrorOutline, "Error", Error, false)
            is AgentState.Paused -> StatusInfo(Icons.Default.PauseCircle, "Paused", Warning, false)
            is AgentState.Cancelled -> StatusInfo(Icons.Default.Cancel, "Cancelled", Error, false)
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color.copy(alpha = if (animate) alpha else 1f),
                modifier = Modifier
                    .size(20.dp)
                    .then(if (animate && state is AgentState.ExecutingTool) Modifier.rotate(rotation) else Modifier)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
            if (animate) {
                Spacer(modifier = Modifier.width(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp),
                    color = color,
                    trackColor = color.copy(alpha = 0.2f)
                )
            }
        }
    }
}

private data class StatusInfo(
    val icon: ImageVector,
    val label: String,
    val color: androidx.compose.ui.graphics.Color,
    val animate: Boolean
)
