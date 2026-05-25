package com.autopilot.agent.ui.screens.chat

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.autopilot.agent.domain.model.AgentState
import com.autopilot.agent.domain.model.MessageRole
import com.autopilot.agent.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Main chat screen for interacting with the AI agent.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: Long,
    initialPrompt: String? = null,
    viewModel: ChatViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Initialize once
    LaunchedEffect(conversationId) {
        viewModel.initialize(conversationId, initialPrompt)
        // Auto-send initial prompt after a short delay to let things initialize
        if (initialPrompt != null && initialPrompt.isNotBlank()) {
            delay(500)
            viewModel.sendMessage()
        }
    }

    // Auto-scroll to bottom on new messages
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(state.messages.size - 1)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.conversationTitle,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                        if (state.currentModel.isNotBlank()) {
                            Text(
                                text = state.currentModel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleDebugView) {
                        Icon(
                            imageVector = if (state.showDebugView) Icons.Default.Code else Icons.Default.CodeOff,
                            contentDescription = "Debug",
                            tint = if (state.showDebugView) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.rename_chat)) },
                                onClick = { showMenu = false; showRenameDialog = true },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.export_chat)) },
                                onClick = { showMenu = false; viewModel.exportConversation() },
                                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete_chat)) },
                                onClick = { showMenu = false; showDeleteDialog = true },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error)
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Agent status indicator
            val agentState = state.agentState
            if (agentState !is AgentState.Idle) {
                AgentStatusIndicator(state = agentState)
            }

            // Error banner
            if (state.error != null && !state.isLoading) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "⚠️ ${state.error}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Task progress
            if (state.tasks.isNotEmpty()) {
                TaskProgressView(
                    tasks = state.tasks,
                    expanded = state.showTaskProgress,
                    onToggle = viewModel::toggleTaskProgress
                )
            }

            // Messages list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                if (state.messages.isEmpty() && !state.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "How can I help you today?",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                items(
                    state.messages.filter { it.role != MessageRole.SYSTEM },
                    key = { it.id }
                ) { message ->
                    MessageBubble(
                        message = message,
                        showDebug = state.showDebugView
                    )
                }

                // Loading indicator
                if (state.isLoading) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val statusText = when (val s = state.agentState) {
                                is AgentState.Thinking -> "Thinking..."
                                is AgentState.ExecutingTool -> "Using ${s.toolName}..."
                                is AgentState.Responding -> "Responding..."
                                else -> "Processing..."
                            }
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Iteration info
            if (state.isLoading && state.currentIteration > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.iteration_count, state.currentIteration, state.maxIterations),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Input area
            Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = state.inputText,
                        onValueChange = viewModel::updateInput,
                        placeholder = { Text(stringResource(R.string.type_message)) },
                        modifier = Modifier.weight(1f),
                        maxLines = 5,
                        enabled = !state.isLoading
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (state.isLoading) {
                        FilledTonalIconButton(
                            onClick = viewModel::stopAgent,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = stringResource(R.string.stop_agent),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        FilledIconButton(
                            onClick = viewModel::sendMessage,
                            enabled = state.inputText.isNotBlank()
                        ) {
                            Icon(Icons.Default.Send, contentDescription = stringResource(R.string.send))
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog
    if (state.showConfirmDialog && state.confirmAction != null) {
        AlertDialog(
            onDismissRequest = { viewModel.confirmAction(false) },
            title = { Text(stringResource(R.string.action_confirmation_title)) },
            text = {
                Text(stringResource(
                    R.string.action_confirmation_message,
                    "${state.confirmAction?.action}: ${state.confirmReason}"
                ))
            },
            confirmButton = {
                Button(onClick = { viewModel.confirmAction(true) }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.confirmAction(false) }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Rename dialog
    if (showRenameDialog) {
        var newTitle by remember { mutableStateOf(state.conversationTitle) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(stringResource(R.string.rename_chat)) },
            text = {
                OutlinedTextField(
                    value = newTitle, onValueChange = { newTitle = it },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.renameConversation(newTitle); showRenameDialog = false }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // Delete confirmation
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_chat)) },
            text = { Text(stringResource(R.string.confirm_delete)) },
            confirmButton = {
                Button(
                    onClick = { showDeleteDialog = false; viewModel.deleteConversation { onNavigateBack() } },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
