package com.autopilot.agent.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.autopilot.agent.R
import com.autopilot.agent.ui.components.ModelSelectorSheet

/**
 * Settings screen for configuring the app and agent parameters.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ─── API Key ───────────────────────────
            SectionHeader(stringResource(R.string.api_key_section))

            ListItem(
                headlineContent = {
                    Text(if (state.hasApiKey) state.maskedApiKey else "No API key configured")
                },
                supportingContent = { Text("OpenRouter API Key") },
                leadingContent = {
                    Icon(Icons.Default.Key, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                },
                trailingContent = {
                    Row {
                        TextButton(onClick = viewModel::showApiKeyDialog) {
                            Text(stringResource(R.string.edit_api_key))
                        }
                    }
                },
                modifier = Modifier.clickable { viewModel.showApiKeyDialog() }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // ─── Model Management ──────────────────
            SectionHeader(stringResource(R.string.model_management))

            ListItem(
                headlineContent = { Text(stringResource(R.string.default_model)) },
                supportingContent = {
                    Text(state.defaultModel.ifBlank { "Not selected" })
                },
                leadingContent = {
                    Icon(Icons.Default.SmartToy, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                },
                modifier = Modifier.clickable { viewModel.showModelSelector() }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.refresh_models)) },
                supportingContent = {
                    Text("${state.freeModels.size} free models available")
                },
                leadingContent = {
                    Icon(Icons.Default.Refresh, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                },
                trailingContent = {
                    if (state.isLoadingModels) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                },
                modifier = Modifier.clickable { viewModel.refreshModels() }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // ─── Agent Configuration ───────────────
            SectionHeader(stringResource(R.string.agent_configuration))

            // Max Iterations
            ListItem(
                headlineContent = {
                    Text(stringResource(R.string.max_iterations, state.maxIterations))
                },
                supportingContent = {
                    Slider(
                        value = state.maxIterations.toFloat(),
                        onValueChange = { viewModel.setMaxIterations(it.toInt()) },
                        valueRange = 5f..50f,
                        steps = 8
                    )
                },
                leadingContent = {
                    Icon(Icons.Default.Repeat, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                }
            )

            // Temperature
            ListItem(
                headlineContent = {
                    Text(stringResource(R.string.temperature, state.temperature))
                },
                supportingContent = {
                    Slider(
                        value = state.temperature,
                        onValueChange = { viewModel.setTemperature(it) },
                        valueRange = 0f..2f
                    )
                },
                leadingContent = {
                    Icon(Icons.Default.Thermostat, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                }
            )

            // Personality
            ListItem(
                headlineContent = { Text(stringResource(R.string.personality)) },
                supportingContent = {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("professional", "casual", "creative").forEach { p ->
                            FilterChip(
                                selected = state.personality == p,
                                onClick = { viewModel.setPersonality(p) },
                                label = { Text(p.replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                },
                leadingContent = {
                    Icon(Icons.Default.EmojiEmotions, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                }
            )

            // Auto confirm
            ListItem(
                headlineContent = { Text(stringResource(R.string.auto_confirm)) },
                leadingContent = {
                    Icon(Icons.Default.Security, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                },
                trailingContent = {
                    Switch(
                        checked = state.autoConfirm,
                        onCheckedChange = viewModel::setAutoConfirm
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // ─── Appearance ────────────────────────
            SectionHeader(stringResource(R.string.appearance))

            // Theme
            ListItem(
                headlineContent = { Text(stringResource(R.string.theme)) },
                supportingContent = {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            "light" to R.string.theme_light,
                            "dark" to R.string.theme_dark,
                            "system" to R.string.theme_system
                        ).forEach { (key, labelRes) ->
                            FilterChip(
                                selected = state.theme == key,
                                onClick = { viewModel.setTheme(key) },
                                label = { Text(stringResource(labelRes)) }
                            )
                        }
                    }
                },
                leadingContent = {
                    Icon(Icons.Default.Palette, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                }
            )

            // Font size
            ListItem(
                headlineContent = { Text(stringResource(R.string.font_size)) },
                supportingContent = {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            "small" to R.string.font_small,
                            "medium" to R.string.font_medium,
                            "large" to R.string.font_large
                        ).forEach { (key, labelRes) ->
                            FilterChip(
                                selected = state.fontSize == key,
                                onClick = { viewModel.setFontSize(key) },
                                label = { Text(stringResource(labelRes)) }
                            )
                        }
                    }
                },
                leadingContent = {
                    Icon(Icons.Default.TextFields, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // ─── Data Management ───────────────────
            SectionHeader(stringResource(R.string.data_management))

            var showClearDialog by remember { mutableStateOf(false) }

            ListItem(
                headlineContent = { Text(stringResource(R.string.clear_history)) },
                leadingContent = {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null,
                        tint = MaterialTheme.colorScheme.error)
                },
                modifier = Modifier.clickable { showClearDialog = true }
            )

            if (showClearDialog) {
                AlertDialog(
                    onDismissRequest = { showClearDialog = false },
                    title = { Text(stringResource(R.string.clear_history)) },
                    text = { Text("This will permanently delete all conversations. Are you sure?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.clearHistory()
                                showClearDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) { Text(stringResource(R.string.delete)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // ─── About ─────────────────────────────
            SectionHeader(stringResource(R.string.about))

            ListItem(
                headlineContent = { Text(stringResource(R.string.version, "1.0.0")) },
                leadingContent = {
                    Icon(Icons.Default.Info, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // API Key edit dialog
    if (state.showApiKeyDialog) {
        var showPassword by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = viewModel::hideApiKeyDialog,
            title = { Text(stringResource(R.string.api_key_section)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = state.apiKeyInput,
                        onValueChange = viewModel::updateApiKeyInput,
                        label = { Text(stringResource(R.string.api_key_label)) },
                        visualTransformation = if (showPassword) VisualTransformation.None
                            else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = viewModel::testApiKey,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.test_connection))
                    }
                    state.testResult?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = it, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::saveApiKey,
                    enabled = state.apiKeyInput.isNotBlank()
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideApiKeyDialog) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Model selector sheet
    if (state.showModelSelector) {
        ModelSelectorSheet(
            models = state.freeModels,
            selectedModelId = state.defaultModel,
            onModelSelected = { viewModel.setDefaultModel(it.id) },
            onRefresh = viewModel::refreshModels,
            onDismiss = viewModel::hideModelSelector,
            isLoading = state.isLoadingModels
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}
