package com.autopilot.agent.agent.tools

import com.autopilot.agent.data.remote.DuckDuckGoApi
import com.autopilot.agent.domain.model.ToolResult
import com.autopilot.agent.util.HtmlParser
import javax.inject.Inject

/**
 * Web search tool using DuckDuckGo HTML search.
 * No API key needed — parses HTML results directly.
 */
class WebSearchTool @Inject constructor(
    private val duckDuckGoApi: DuckDuckGoApi
) : Tool {

    override val name = "WEB_SEARCH"
    override val description = "Search the internet using DuckDuckGo"
    override val requiresConfirmation = false

    override suspend fun execute(input: Map<String, Any?>): ToolResult {
        val startTime = System.currentTimeMillis()
        val query = input["query"]?.toString()

        if (query.isNullOrBlank()) {
            return ToolResult(
                toolName = name,
                success = false,
                output = "",
                error = "Search query is required",
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }

        return try {
            val response = duckDuckGoApi.search(query)
            if (response.isSuccessful) {
                val html = response.body()?.string() ?: ""
                val results = HtmlParser.parseDuckDuckGoResults(html, maxResults = 5)

                if (results.isEmpty()) {
                    ToolResult(
                        toolName = name,
                        success = true,
                        output = "No results found for: $query",
                        executionTimeMs = System.currentTimeMillis() - startTime
                    )
                } else {
                    val formatted = buildString {
                        appendLine("Search results for: \"$query\"")
                        appendLine("─".repeat(40))
                        results.forEachIndexed { index, result ->
                            appendLine("${index + 1}. ${result.title}")
                            appendLine("   URL: ${result.url}")
                            if (result.snippet.isNotBlank()) {
                                appendLine("   ${result.snippet}")
                            }
                            appendLine()
                        }
                    }
                    ToolResult(
                        toolName = name,
                        success = true,
                        output = formatted,
                        executionTimeMs = System.currentTimeMillis() - startTime
                    )
                }
            } else {
                ToolResult(
                    toolName = name,
                    success = false,
                    output = "",
                    error = "Search request failed with code: ${response.code()}",
                    executionTimeMs = System.currentTimeMillis() - startTime
                )
            }
        } catch (e: Exception) {
            ToolResult(
                toolName = name,
                success = false,
                output = "",
                error = "Search error: ${e.message}",
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }
}
