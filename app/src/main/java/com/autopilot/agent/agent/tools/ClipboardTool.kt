package com.autopilot.agent.agent.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.autopilot.agent.domain.model.ToolResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * Clipboard tool for reading from and writing to the system clipboard.
 */
class ClipboardTool @Inject constructor(
    @ApplicationContext private val context: Context
) : Tool {

    override val name = "CLIPBOARD"
    override val description = "Read from or write to the system clipboard"
    override val requiresConfirmation = true

    override suspend fun execute(input: Map<String, Any?>): ToolResult {
        val startTime = System.currentTimeMillis()
        val operation = input["operation"]?.toString()?.lowercase()
        val text = input["text"]?.toString()

        return when (operation) {
            "copy" -> copyToClipboard(text, startTime)
            "paste" -> pasteFromClipboard(startTime)
            else -> ToolResult(
                toolName = name,
                success = false,
                output = "",
                error = "Unknown operation: $operation. Use 'copy' or 'paste'.",
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }

    private suspend fun copyToClipboard(text: String?, startTime: Long): ToolResult {
        if (text.isNullOrBlank()) {
            return ToolResult(name, false, "", "Text is required for copy operation",
                System.currentTimeMillis() - startTime)
        }

        return suspendCancellableCoroutine { continuation ->
            Handler(Looper.getMainLooper()).post {
                try {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("AutoPilot AI", text)
                    clipboard.setPrimaryClip(clip)
                    continuation.resume(
                        ToolResult(name, true, "Text copied to clipboard (${text.length} chars)",
                            executionTimeMs = System.currentTimeMillis() - startTime)
                    )
                } catch (e: Exception) {
                    continuation.resume(
                        ToolResult(name, false, "", "Copy failed: ${e.message}",
                            System.currentTimeMillis() - startTime)
                    )
                }
            }
        }
    }

    private suspend fun pasteFromClipboard(startTime: Long): ToolResult {
        return suspendCancellableCoroutine { continuation ->
            Handler(Looper.getMainLooper()).post {
                try {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = clipboard.primaryClip
                    if (clip != null && clip.itemCount > 0) {
                        val text = clip.getItemAt(0).text?.toString() ?: ""
                        continuation.resume(
                            ToolResult(name, true, "Clipboard content: $text",
                                executionTimeMs = System.currentTimeMillis() - startTime)
                        )
                    } else {
                        continuation.resume(
                            ToolResult(name, true, "Clipboard is empty",
                                executionTimeMs = System.currentTimeMillis() - startTime)
                        )
                    }
                } catch (e: Exception) {
                    continuation.resume(
                        ToolResult(name, false, "", "Paste failed: ${e.message}",
                            System.currentTimeMillis() - startTime)
                    )
                }
            }
        }
    }
}
