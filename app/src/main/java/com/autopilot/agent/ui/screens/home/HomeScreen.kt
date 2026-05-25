package com.autopilot.agent.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.autopilot.agent.R
import com.autopilot.agent.ui.components.TemplateCard
import com.autopilot.agent.util.toRelativeTime

/**
 * Home screen - the main hub showing quick actions and recent conversations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToChat: (Long, String?) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showTemplateDialog by remember { mutableStateOf(false) }
    var selectedTemplate by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.home_title))
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.createNewConversation { id ->
                        onNavigateToChat(id, null)
                    }
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.new_task)) },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Quick Actions section
            item {
                Text(
                    text = stringResource(R.string.quick_actions),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val templates = listOf(
                        Triple("research", R.string.template_research, R.string.template_research_desc),
                        Triple("news", R.string.template_news, R.string.template_news_desc),
                        Triple("code", R.string.template_code, R.string.template_code_desc),
                        Triple("write", R.string.template_write, R.string.template_write_desc),
                        Triple("analyze", R.string.template_analyze, R.string.template_analyze_desc),
                        Triple("study", R.string.template_study, R.string.template_study_desc),
                        Triple("travel", R.string.template_travel, R.string.template_travel_desc),
                        Triple("custom", R.string.template_custom, R.string.template_custom_desc)
                    )

                    templates.forEach { (key, titleRes, descRes) ->
                        TemplateCard(
                            title = stringResource(titleRes),
                            description = stringResource(descRes),
                            iconName = key,
                            onClick = {
                                selectedTemplate = key
                                showTemplateDialog = true
                            }
                        )
                    }
                }
            }

            // Recent conversations
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.recent_conversations),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (state.recentConversations.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.no_conversations),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(state.recentConversations, key = { it.id }) { conversation ->
                    ListItem(
                        modifier = Modifier.clickable {
                            onNavigateToChat(conversation.id, null)
                        },
                        headlineContent = {
                            Text(
                                text = conversation.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        supportingContent = {
                            Text(
                                text = "${conversation.modelUsed} • ${conversation.updatedAt.toRelativeTime()}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        leadingContent = {
                            Icon(
                                Icons.Default.Chat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }

    // Template dialog
    if (showTemplateDialog) {
        var userInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showTemplateDialog = false },
            title = {
                Text("${selectedTemplate.replaceFirstChar { it.uppercase() }} Task")
            },
            text = {
                Column {
                    Text(
                        text = "Describe what you want to ${selectedTemplate}:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = userInput,
                        onValueChange = { userInput = it },
                        placeholder = { Text("Enter details...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showTemplateDialog = false
                        val prompt = viewModel.getTemplatePrompt(selectedTemplate) + userInput
                        val title = "${selectedTemplate.replaceFirstChar { it.uppercase() }}: ${userInput.take(30)}"
                        viewModel.createNewConversation(title) { id ->
                            onNavigateToChat(id, prompt)
                        }
                    },
                    enabled = userInput.isNotBlank()
                ) {
                    Text("Start")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTemplateDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
