package com.autopilot.agent.ui.screens.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.autopilot.agent.R
import com.autopilot.agent.domain.model.TaskStatus
import com.autopilot.agent.ui.theme.*
import com.autopilot.agent.util.toFormattedDateTime

/**
 * Tasks screen showing all agent task executions grouped by status.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: TasksViewModel = hiltViewModel(),
    onNavigateToChat: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.tasks_title)) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "all" to R.string.filter_all,
                    "running" to R.string.filter_running,
                    "completed" to R.string.filter_completed,
                    "failed" to R.string.filter_failed
                ).forEach { (key, labelRes) ->
                    FilterChip(
                        selected = state.filter == key,
                        onClick = { viewModel.setFilter(key) },
                        label = { Text(stringResource(labelRes)) }
                    )
                }
            }

            if (state.tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.TaskAlt,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.no_tasks),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.tasks, key = { it.id }) { task ->
                        val statusColor = when (task.status) {
                            TaskStatus.COMPLETED -> Success
                            TaskStatus.RUNNING -> Info
                            TaskStatus.FAILED -> Error
                            TaskStatus.CANCELLED -> Warning
                            TaskStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        val statusIcon = when (task.status) {
                            TaskStatus.COMPLETED -> Icons.Default.CheckCircle
                            TaskStatus.RUNNING -> Icons.Default.PlayCircle
                            TaskStatus.FAILED -> Icons.Default.Cancel
                            TaskStatus.CANCELLED -> Icons.Default.RemoveCircle
                            TaskStatus.PENDING -> Icons.Default.Schedule
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToChat(task.conversationId) }
                        ) {
                            ListItem(
                                headlineContent = { Text(task.title) },
                                supportingContent = {
                                    Column {
                                        Text(
                                            text = task.description.take(100),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = "Created: ${task.createdAt.toFormattedDateTime()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                leadingContent = {
                                    Icon(
                                        imageVector = statusIcon,
                                        contentDescription = null,
                                        tint = statusColor
                                    )
                                },
                                trailingContent = {
                                    Text(
                                        text = task.status.value.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = statusColor
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
