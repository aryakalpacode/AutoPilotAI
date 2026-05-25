package com.autopilot.agent.util

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Safelist

/**
 * Utility for parsing HTML content from web pages and search results.
 */
object HtmlParser {

    /** Data class representing a single search result. */
    data class SearchResult(
        val title: String,
        val url: String,
        val snippet: String
    )

    /**
     * Parse DuckDuckGo HTML search results page.
     * Extracts up to [maxResults] search results with title, URL, and snippet.
     */
    fun parseDuckDuckGoResults(html: String, maxResults: Int = 5): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        try {
            val doc = Jsoup.parse(html)
            val resultElements = doc.select(".result__body")

            for (element in resultElements) {
                if (results.size >= maxResults) break

                val titleElement = element.select(".result__a").firstOrNull()
                val snippetElement = element.select(".result__snippet").firstOrNull()

                val title = titleElement?.text()?.trim() ?: continue
                var url = titleElement.attr("href")?.trim() ?: ""

                // DuckDuckGo uses redirect URLs; try to extract actual URL
                if (url.contains("uddg=")) {
                    url = try {
                        java.net.URLDecoder.decode(
                            url.substringAfter("uddg=").substringBefore("&"),
                            "UTF-8"
                        )
                    } catch (e: Exception) {
                        url
                    }
                }

                val snippet = snippetElement?.text()?.trim() ?: ""

                if (title.isNotBlank() && url.isNotBlank()) {
                    results.add(SearchResult(title = title, url = url, snippet = snippet))
                }
            }

            // Fallback: try alternative selectors if above didn't work
            if (results.isEmpty()) {
                val links = doc.select("a.result__url, a[href*=http]")
                for (link in links) {
                    if (results.size >= maxResults) break
                    val href = link.attr("href")
                    val text = link.text()
                    if (href.startsWith("http") && text.isNotBlank()) {
                        results.add(SearchResult(title = text, url = href, snippet = ""))
                    }
                }
            }
        } catch (e: Exception) {
            // Return empty on parse failure
        }
        return results
    }

    /**
     * Extract readable text from an HTML document.
     * Removes scripts, styles, and HTML tags.
     * Limits output to [maxChars] characters.
     */
    fun extractReadableText(html: String, maxChars: Int = Constants.MAX_SCRAPE_CHARS): String {
        return try {
            val doc = Jsoup.parse(html)
            // Remove non-content elements
            doc.select("script, style, nav, footer, header, aside, iframe, noscript").remove()

            // Get text content
            val text = doc.body()?.text() ?: doc.text()

            // Clean up whitespace
            val cleaned = text
                .replace(Regex("\\s+"), " ")
                .trim()

            if (cleaned.length > maxChars) {
                cleaned.substring(0, maxChars) + "... [truncated]"
            } else {
                cleaned
            }
        } catch (e: Exception) {
            "Error extracting text: ${e.message}"
        }
    }

    /**
     * Extract the page title from HTML.
     */
    fun extractTitle(html: String): String {
        return try {
            Jsoup.parse(html).title()
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Clean HTML by removing all tags but preserving text.
     */
    fun stripHtml(html: String): String {
        return Jsoup.clean(html, Safelist.none())
    }
}
