package com.autopilot.agent.agent.tools

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.autopilot.agent.domain.model.ToolResult
import com.autopilot.agent.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * JavaScript code executor using Android WebView.
 * Sandboxed - no access to Android APIs from JavaScript.
 */
class CodeExecutorTool @Inject constructor(
    @ApplicationContext private val context: Context
) : Tool {

    override val name = "CODE_EXECUTOR"
    override val description = "Execute JavaScript code in a sandboxed WebView"
    override val requiresConfirmation = true

    override suspend fun execute(input: Map<String, Any?>): ToolResult {
        val startTime = System.currentTimeMillis()
        val code = input["code"]?.toString()

        if (code.isNullOrBlank()) {
            return ToolResult(
                toolName = name,
                success = false,
                output = "",
                error = "JavaScript code is required",
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }

        return try {
            withTimeout(Constants.CODE_EXECUTION_TIMEOUT_MS) {
                executeJavaScript(code, startTime)
            }
        } catch (e: Exception) {
            ToolResult(
                toolName = name,
                success = false,
                output = "",
                error = "Execution error: ${e.message}",
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }

    private suspend fun executeJavaScript(code: String, startTime: Long): ToolResult {
        return suspendCancellableCoroutine { continuation ->
            Handler(Looper.getMainLooper()).post {
                try {
                    val webView = WebView(context)
                    webView.settings.javaScriptEnabled = true

                    val logs = mutableListOf<String>()
                    var resultValue = ""

                    val jsInterface = object {
                        @JavascriptInterface
                        fun log(message: String) {
                            logs.add(message)
                        }

                        @JavascriptInterface
                        fun returnResult(result: String) {
                            resultValue = result
                            Handler(Looper.getMainLooper()).post {
                                val output = buildString {
                                    if (logs.isNotEmpty()) {
                                        appendLine("Console output:")
                                        logs.forEach { appendLine("  > $it") }
                                    }
                                    if (resultValue.isNotBlank()) {
                                        appendLine("Return value: $resultValue")
                                    }
                                    if (logs.isEmpty() && resultValue.isBlank()) {
                                        appendLine("Code executed successfully (no output)")
                                    }
                                }
                                webView.destroy()
                                if (continuation.isActive) {
                                    continuation.resume(
                                        ToolResult(
                                            toolName = name,
                                            success = true,
                                            output = output.trim(),
                                            executionTimeMs = System.currentTimeMillis() - startTime
                                        )
                                    )
                                }
                            }
                        }

                        @JavascriptInterface
                        fun reportError(error: String) {
                            Handler(Looper.getMainLooper()).post {
                                webView.destroy()
                                if (continuation.isActive) {
                                    continuation.resume(
                                        ToolResult(
                                            toolName = name,
                                            success = false,
                                            output = logs.joinToString("\n"),
                                            error = "JavaScript error: $error",
                                            executionTimeMs = System.currentTimeMillis() - startTime
                                        )
                                    )
                                }
                            }
                        }
                    }

                    webView.addJavascriptInterface(jsInterface, "Android")

                    // Wrap the code with console.log capture and error handling
                    val wrappedCode = """
                        <html><body><script>
                        var console = {
                            log: function() {
                                var args = Array.prototype.slice.call(arguments);
                                Android.log(args.map(function(a) {
                                    return typeof a === 'object' ? JSON.stringify(a) : String(a);
                                }).join(' '));
                            },
                            error: function() { console.log.apply(null, arguments); },
                            warn: function() { console.log.apply(null, arguments); }
                        };
                        try {
                            var __result = (function() { $code })();
                            Android.returnResult(__result !== undefined ? String(__result) : '');
                        } catch(e) {
                            Android.reportError(e.toString());
                        }
                        </script></body></html>
                    """.trimIndent()

                    webView.loadDataWithBaseURL(null, wrappedCode, "text/html", "UTF-8", null)

                    // Timeout fallback
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (continuation.isActive) {
                            webView.destroy()
                            continuation.resume(
                                ToolResult(
                                    toolName = name,
                                    success = false,
                                    output = logs.joinToString("\n"),
                                    error = "Execution timed out after ${Constants.CODE_EXECUTION_TIMEOUT_MS / 1000}s",
                                    executionTimeMs = System.currentTimeMillis() - startTime
                                )
                            )
                        }
                    }, Constants.CODE_EXECUTION_TIMEOUT_MS)

                } catch (e: Exception) {
                    if (continuation.isActive) {
                        continuation.resume(
                            ToolResult(
                                toolName = name,
                                success = false,
                                output = "",
                                error = "Setup error: ${e.message}",
                                executionTimeMs = System.currentTimeMillis() - startTime
                            )
                        )
                    }
                }
            }
        }
    }
}
