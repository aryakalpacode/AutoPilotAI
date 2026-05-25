package com.autopilot.agent.agent.tools

import com.autopilot.agent.domain.model.ToolResult
import com.autopilot.agent.util.Constants
import com.autopilot.agent.util.HtmlParser
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Named

/**
 * Web scraping tool that fetches and extracts readable text from URLs.
 */
class WebScrapeTool @Inject constructor(
    @Named("scraperClient") private val httpClient: OkHttpClient
) : Tool {

    override val name = "WEB_SCRAPE"
    override val description = "Fetch and read content from a web page URL"
    override val requiresConfirmation = false

    override suspend fun execute(input: Map<String, Any?>): ToolResult {
        val startTime = System.currentTimeMillis()
        val url = input["url"]?.toString()

        if (url.isNullOrBlank()) {
            return ToolResult(
                toolName = name,
                success = false,
                output = "",
                error = "URL is required",
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }

        // Basic URL validation
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return ToolResult(
                toolName = name,
                success = false,
                output = "",
                error = "Invalid URL: must start with http:// or https://",
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }

        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android) AutoPilotAI/1.0")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val html = response.body?.string() ?: ""
                val title = HtmlParser.extractTitle(html)
                val text = HtmlParser.extractReadableText(html, Constants.MAX_SCRAPE_CHARS)

                val output = buildString {
                    if (title.isNotBlank()) {
                        appendLine("Page Title: $title")
                        appendLine("URL: $url")
                        appendLine("─".repeat(40))
                    }
                    append(text)
                }

                ToolResult(
                    toolName = name,
                    success = true,
                    output = output,
                    executionTimeMs = System.currentTimeMillis() - startTime
                )
            } else {
                ToolResult(
                    toolName = name,
                    success = false,
                    output = "",
                    error = "HTTP ${response.code}: Failed to fetch $url",
                    executionTimeMs = System.currentTimeMillis() - startTime
                )
            }
        } catch (e: Exception) {
            ToolResult(
                toolName = name,
                success = false,
                output = "",
                error = "Scrape error: ${e.message}",
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }
}
