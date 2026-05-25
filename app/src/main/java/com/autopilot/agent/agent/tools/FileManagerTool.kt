package com.autopilot.agent.agent.tools

import android.content.Context
import com.autopilot.agent.domain.model.ToolResult
import com.autopilot.agent.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/**
 * File management tool for creating, reading, updating, deleting, and listing files
 * in the app's workspace directory.
 */
class FileManagerTool @Inject constructor(
    @ApplicationContext private val context: Context
) : Tool {

    override val name = "FILE_MANAGER"
    override val description = "Create, read, update, delete, and list files in workspace"
    override val requiresConfirmation = true // Modifies files

    private val workspaceDir: File by lazy {
        File(context.filesDir, Constants.WORKSPACE_DIR).also { it.mkdirs() }
    }

    private val allowedExtensions = setOf(
        "txt", "md", "json", "csv", "py", "js", "html", "css", "xml", "yaml", "yml",
        "kt", "java", "sh", "sql", "log", "ini", "cfg", "toml"
    )

    override suspend fun execute(input: Map<String, Any?>): ToolResult {
        val startTime = System.currentTimeMillis()
        val operation = input["operation"]?.toString()?.lowercase()
        val filename = input["filename"]?.toString()
        val content = input["content"]?.toString()

        return when (operation) {
            "create" -> createFile(filename, content, startTime)
            "read" -> readFile(filename, startTime)
            "update" -> updateFile(filename, content, startTime)
            "delete" -> deleteFile(filename, startTime)
            "list" -> listFiles(startTime)
            else -> ToolResult(
                toolName = name,
                success = false,
                output = "",
                error = "Unknown operation: $operation. Use: create, read, update, delete, list",
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }

    private fun createFile(filename: String?, content: String?, startTime: Long): ToolResult {
        if (filename.isNullOrBlank()) {
            return ToolResult(name, false, "", "Filename is required", System.currentTimeMillis() - startTime)
        }
        if (!isValidFilename(filename)) {
            return ToolResult(name, false, "", "Invalid filename or extension: $filename", System.currentTimeMillis() - startTime)
        }

        return try {
            val file = File(workspaceDir, filename)
            file.parentFile?.mkdirs()
            file.writeText(content ?: "")
            ToolResult(name, true, "File created: $filename (${file.length()} bytes)", executionTimeMs = System.currentTimeMillis() - startTime)
        } catch (e: Exception) {
            ToolResult(name, false, "", "Failed to create file: ${e.message}", System.currentTimeMillis() - startTime)
        }
    }

    private fun readFile(filename: String?, startTime: Long): ToolResult {
        if (filename.isNullOrBlank()) {
            return ToolResult(name, false, "", "Filename is required", System.currentTimeMillis() - startTime)
        }

        return try {
            val file = File(workspaceDir, filename)
            if (!file.exists()) {
                return ToolResult(name, false, "", "File not found: $filename", System.currentTimeMillis() - startTime)
            }

            val text = file.readText()
            val truncated = if (text.length > 8000) {
                text.substring(0, 8000) + "\n... [truncated, ${text.length} total chars]"
            } else text

            ToolResult(name, true, "Content of $filename:\n$truncated", executionTimeMs = System.currentTimeMillis() - startTime)
        } catch (e: Exception) {
            ToolResult(name, false, "", "Failed to read file: ${e.message}", System.currentTimeMillis() - startTime)
        }
    }

    private fun updateFile(filename: String?, content: String?, startTime: Long): ToolResult {
        if (filename.isNullOrBlank()) {
            return ToolResult(name, false, "", "Filename is required", System.currentTimeMillis() - startTime)
        }

        return try {
            val file = File(workspaceDir, filename)
            if (!file.exists()) {
                return ToolResult(name, false, "", "File not found: $filename", System.currentTimeMillis() - startTime)
            }
            file.writeText(content ?: "")
            ToolResult(name, true, "File updated: $filename (${file.length()} bytes)", executionTimeMs = System.currentTimeMillis() - startTime)
        } catch (e: Exception) {
            ToolResult(name, false, "", "Failed to update file: ${e.message}", System.currentTimeMillis() - startTime)
        }
    }

    private fun deleteFile(filename: String?, startTime: Long): ToolResult {
        if (filename.isNullOrBlank()) {
            return ToolResult(name, false, "", "Filename is required", System.currentTimeMillis() - startTime)
        }

        return try {
            val file = File(workspaceDir, filename)
            if (!file.exists()) {
                return ToolResult(name, false, "", "File not found: $filename", System.currentTimeMillis() - startTime)
            }
            val deleted = file.delete()
            if (deleted) {
                ToolResult(name, true, "File deleted: $filename", executionTimeMs = System.currentTimeMillis() - startTime)
            } else {
                ToolResult(name, false, "", "Failed to delete: $filename", System.currentTimeMillis() - startTime)
            }
        } catch (e: Exception) {
            ToolResult(name, false, "", "Failed to delete file: ${e.message}", System.currentTimeMillis() - startTime)
        }
    }

    private fun listFiles(startTime: Long): ToolResult {
        return try {
            val files = workspaceDir.listFiles()
            if (files.isNullOrEmpty()) {
                return ToolResult(name, true, "Workspace is empty. No files found.", executionTimeMs = System.currentTimeMillis() - startTime)
            }

            val listing = buildString {
                appendLine("Files in workspace:")
                appendLine("─".repeat(40))
                files.sortedBy { it.name }.forEach { file ->
                    val size = when {
                        file.length() < 1024 -> "${file.length()} B"
                        file.length() < 1048576 -> "${file.length() / 1024} KB"
                        else -> "${file.length() / 1048576} MB"
                    }
                    val type = if (file.isDirectory) "DIR" else file.extension.uppercase()
                    appendLine("  [$type] ${file.name} ($size)")
                }
                appendLine("─".repeat(40))
                appendLine("Total: ${files.size} items")
            }

            ToolResult(name, true, listing, executionTimeMs = System.currentTimeMillis() - startTime)
        } catch (e: Exception) {
            ToolResult(name, false, "", "Failed to list files: ${e.message}", System.currentTimeMillis() - startTime)
        }
    }

    private fun isValidFilename(filename: String): Boolean {
        if (filename.contains("..") || filename.startsWith("/")) return false
        val ext = filename.substringAfterLast(".", "")
        return ext.lowercase() in allowedExtensions || ext.isEmpty()
    }
}
