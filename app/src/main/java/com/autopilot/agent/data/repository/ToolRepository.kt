package com.autopilot.agent.data.repository

import com.autopilot.agent.agent.tools.*
import com.autopilot.agent.domain.model.ToolResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository providing access to all available tools.
 */
@Singleton
class ToolRepository @Inject constructor(
    private val webSearchTool: WebSearchTool,
    private val webScrapeTool: WebScrapeTool,
    private val fileManagerTool: FileManagerTool,
    private val codeExecutorTool: CodeExecutorTool,
    private val calculatorTool: CalculatorTool,
    private val clipboardTool: ClipboardTool,
    private val deviceInfoTool: DeviceInfoTool,
    private val notesDatabaseTool: NotesDatabaseTool,
    private val reminderTool: ReminderTool,
    private val textProcessorTool: TextProcessorTool
) {

    private val tools: Map<String, Tool> by lazy {
        mapOf(
            "WEB_SEARCH" to webSearchTool,
            "WEB_SCRAPE" to webScrapeTool,
            "FILE_MANAGER" to fileManagerTool,
            "CODE_EXECUTOR" to codeExecutorTool,
            "CALCULATOR" to calculatorTool,
            "CLIPBOARD" to clipboardTool,
            "DEVICE_INFO" to deviceInfoTool,
            "NOTES_DATABASE" to notesDatabaseTool,
            "REMINDER" to reminderTool,
            "TEXT_PROCESSOR" to textProcessorTool
        )
    }

    /** Get all available tools. */
    fun getAllTools(): Map<String, Tool> = tools

    /** Get a specific tool by name. */
    fun getTool(name: String): Tool? = tools[name.uppercase()]

    /** Execute a tool by name. */
    suspend fun executeTool(name: String, input: Map<String, Any?>): ToolResult {
        val tool = getTool(name)
            ?: return ToolResult(
                toolName = name,
                success = false,
                output = "",
                error = "Tool not found: $name"
            )
        return tool.execute(input)
    }

    /** Get list of tool names. */
    fun getToolNames(): List<String> = tools.keys.toList()

    /** Check if a tool requires confirmation. */
    fun requiresConfirmation(toolName: String): Boolean {
        return getTool(toolName)?.requiresConfirmation ?: false
    }
}
