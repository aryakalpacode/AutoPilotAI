package com.autopilot.agent.agent.tools

import com.autopilot.agent.data.local.dao.NoteDao
import com.autopilot.agent.data.local.entity.NoteEntity
import com.autopilot.agent.domain.model.ToolResult
import com.autopilot.agent.util.toFormattedDateTime
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * Notes database tool for CRUD operations on local notes.
 * The agent can save research, summaries, and other persistent data.
 */
class NotesDatabaseTool @Inject constructor(
    private val noteDao: NoteDao
) : Tool {

    override val name = "NOTES_DATABASE"
    override val description = "Create, read, update, delete, and list notes in local database"
    override val requiresConfirmation = false

    override suspend fun execute(input: Map<String, Any?>): ToolResult {
        val startTime = System.currentTimeMillis()
        val operation = input["operation"]?.toString()?.lowercase()

        return when (operation) {
            "create" -> createNote(input, startTime)
            "read" -> readNote(input, startTime)
            "update" -> updateNote(input, startTime)
            "delete" -> deleteNote(input, startTime)
            "list" -> listNotes(input, startTime)
            "search" -> searchNotes(input, startTime)
            else -> ToolResult(
                toolName = name,
                success = false,
                output = "",
                error = "Unknown operation: $operation. Use: create, read, update, delete, list, search",
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }

    private suspend fun createNote(input: Map<String, Any?>, startTime: Long): ToolResult {
        val title = input["title"]?.toString() ?: "Untitled"
        val content = input["content"]?.toString() ?: ""
        val tags = when (val t = input["tags"]) {
            is List<*> -> t.joinToString(",") { it.toString() }
            is String -> t
            else -> ""
        }

        return try {
            val id = noteDao.insert(
                NoteEntity(
                    title = title,
                    content = content,
                    tags = tags
                )
            )
            ToolResult(
                toolName = name,
                success = true,
                output = "Note created with ID: $id\nTitle: $title\nTags: $tags",
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            ToolResult(name, false, "", "Failed to create note: ${e.message}",
                System.currentTimeMillis() - startTime)
        }
    }

    private suspend fun readNote(input: Map<String, Any?>, startTime: Long): ToolResult {
        val idStr = input["id"]?.toString()
        val id = idStr?.toLongOrNull()

        if (id == null) {
            return ToolResult(name, false, "", "Note ID is required (numeric)",
                System.currentTimeMillis() - startTime)
        }

        return try {
            val note = noteDao.getById(id)
            if (note != null) {
                val output = buildString {
                    appendLine("Note #${note.id}")
                    appendLine("Title: ${note.title}")
                    appendLine("Tags: ${note.tags}")
                    appendLine("Created: ${note.createdAt.toFormattedDateTime()}")
                    appendLine("Updated: ${note.updatedAt.toFormattedDateTime()}")
                    appendLine("─".repeat(30))
                    append(note.content)
                }
                ToolResult(name, true, output, executionTimeMs = System.currentTimeMillis() - startTime)
            } else {
                ToolResult(name, false, "", "Note not found with ID: $id",
                    System.currentTimeMillis() - startTime)
            }
        } catch (e: Exception) {
            ToolResult(name, false, "", "Failed to read note: ${e.message}",
                System.currentTimeMillis() - startTime)
        }
    }

    private suspend fun updateNote(input: Map<String, Any?>, startTime: Long): ToolResult {
        val id = input["id"]?.toString()?.toLongOrNull()
        if (id == null) {
            return ToolResult(name, false, "", "Note ID is required",
                System.currentTimeMillis() - startTime)
        }

        return try {
            val existing = noteDao.getById(id)
                ?: return ToolResult(name, false, "", "Note not found: $id",
                    System.currentTimeMillis() - startTime)

            val updated = existing.copy(
                title = input["title"]?.toString() ?: existing.title,
                content = input["content"]?.toString() ?: existing.content,
                tags = when (val t = input["tags"]) {
                    is List<*> -> t.joinToString(",") { it.toString() }
                    is String -> t
                    else -> existing.tags
                },
                updatedAt = System.currentTimeMillis()
            )
            noteDao.update(updated)
            ToolResult(name, true, "Note #$id updated successfully",
                executionTimeMs = System.currentTimeMillis() - startTime)
        } catch (e: Exception) {
            ToolResult(name, false, "", "Failed to update note: ${e.message}",
                System.currentTimeMillis() - startTime)
        }
    }

    private suspend fun deleteNote(input: Map<String, Any?>, startTime: Long): ToolResult {
        val id = input["id"]?.toString()?.toLongOrNull()
        if (id == null) {
            return ToolResult(name, false, "", "Note ID is required",
                System.currentTimeMillis() - startTime)
        }

        return try {
            noteDao.deleteById(id)
            ToolResult(name, true, "Note #$id deleted",
                executionTimeMs = System.currentTimeMillis() - startTime)
        } catch (e: Exception) {
            ToolResult(name, false, "", "Failed to delete note: ${e.message}",
                System.currentTimeMillis() - startTime)
        }
    }

    private suspend fun listNotes(input: Map<String, Any?>, startTime: Long): ToolResult {
        return try {
            val notes = noteDao.getAll().firstOrNull() ?: emptyList()
            if (notes.isEmpty()) {
                return ToolResult(name, true, "No notes found.",
                    executionTimeMs = System.currentTimeMillis() - startTime)
            }

            val output = buildString {
                appendLine("Notes (${notes.size} total):")
                appendLine("─".repeat(40))
                notes.forEach { note ->
                    appendLine("#${note.id} | ${note.title}")
                    appendLine("   Tags: ${note.tags.ifBlank { "none" }} | Updated: ${note.updatedAt.toFormattedDateTime()}")
                    appendLine("   Preview: ${note.content.take(80)}${if (note.content.length > 80) "..." else ""}")
                    appendLine()
                }
            }

            ToolResult(name, true, output.trim(),
                executionTimeMs = System.currentTimeMillis() - startTime)
        } catch (e: Exception) {
            ToolResult(name, false, "", "Failed to list notes: ${e.message}",
                System.currentTimeMillis() - startTime)
        }
    }

    private suspend fun searchNotes(input: Map<String, Any?>, startTime: Long): ToolResult {
        val query = input["query"]?.toString() ?: input["text"]?.toString()
        if (query.isNullOrBlank()) {
            return ToolResult(name, false, "", "Search query is required",
                System.currentTimeMillis() - startTime)
        }

        return try {
            val notes = noteDao.search(query).firstOrNull() ?: emptyList()
            if (notes.isEmpty()) {
                return ToolResult(name, true, "No notes found matching: $query",
                    executionTimeMs = System.currentTimeMillis() - startTime)
            }

            val output = buildString {
                appendLine("Search results for \"$query\" (${notes.size} found):")
                appendLine("─".repeat(40))
                notes.forEach { note ->
                    appendLine("#${note.id} | ${note.title}")
                    appendLine("   ${note.content.take(100)}...")
                    appendLine()
                }
            }

            ToolResult(name, true, output.trim(),
                executionTimeMs = System.currentTimeMillis() - startTime)
        } catch (e: Exception) {
            ToolResult(name, false, "", "Search failed: ${e.message}",
                System.currentTimeMillis() - startTime)
        }
    }
}
