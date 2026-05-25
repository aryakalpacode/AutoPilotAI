package com.autopilot.agent.agent

import android.util.Log
import com.autopilot.agent.domain.model.AgentAction
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parses LLM responses into structured AgentAction objects.
 * Handles malformed responses with multiple fallback strategies.
 */
@Singleton
class ResponseParser @Inject constructor() {

    private val gson = Gson()

    companion object {
        private const val TAG = "ResponseParser"
    }

    /**
     * Parse the LLM response text into an AgentAction.
     * Tries multiple strategies if the response is not clean JSON.
     *
     * @param responseText Raw response text from the LLM.
     * @return Parsed AgentAction, or a fallback RESPOND action.
     */
    fun parse(responseText: String): AgentAction {
        // Strategy 1: Direct JSON parse
        tryParseJson(responseText)?.let {
            Log.d(TAG, "Parsed via direct JSON")
            return it
        }

        // Strategy 2: Extract JSON from markdown code blocks
        tryExtractFromCodeBlock(responseText)?.let {
            Log.d(TAG, "Parsed via code block extraction")
            return it
        }

        // Strategy 3: Regex to find JSON object
        tryRegexExtract(responseText)?.let {
            Log.d(TAG, "Parsed via regex extraction")
            return it
        }

        // Strategy 4: Try to find partial JSON elements
        tryPartialParse(responseText)?.let {
            Log.d(TAG, "Parsed via partial extraction")
            return it
        }

        // Fallback: Wrap raw text as RESPOND action
        Log.w(TAG, "All parsing strategies failed, using fallback")
        return AgentAction(
            thought = "I generated a free-form response.",
            action = "RESPOND",
            actionInput = mapOf("message" to responseText.trim()),
            confidence = 0.3
        )
    }

    private fun tryParseJson(text: String): AgentAction? {
        return try {
            val trimmed = text.trim()
            val jsonObject = JsonParser.parseString(trimmed).asJsonObject
            extractAction(jsonObject)
        } catch (e: Exception) {
            null
        }
    }

    private fun tryExtractFromCodeBlock(text: String): AgentAction? {
        val patterns = listOf(
            Regex("```json\\s*\\n?(.*?)\\n?```", RegexOption.DOT_MATCHES_ALL),
            Regex("```\\s*\\n?(\\{.*?})\\n?```", RegexOption.DOT_MATCHES_ALL)
        )

        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                val json = match.groupValues[1].trim()
                try {
                    val jsonObject = JsonParser.parseString(json).asJsonObject
                    return extractAction(jsonObject)
                } catch (e: Exception) {
                    continue
                }
            }
        }
        return null
    }

    private fun tryRegexExtract(text: String): AgentAction? {
        // Find the outermost JSON object
        val startIdx = text.indexOf('{')
        if (startIdx == -1) return null

        var depth = 0
        var endIdx = -1
        for (i in startIdx until text.length) {
            when (text[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        endIdx = i
                        break
                    }
                }
            }
        }

        if (endIdx == -1) return null

        return try {
            val json = text.substring(startIdx, endIdx + 1)
            val jsonObject = JsonParser.parseString(json).asJsonObject
            extractAction(jsonObject)
        } catch (e: Exception) {
            null
        }
    }

    private fun tryPartialParse(text: String): AgentAction? {
        var thought = ""
        var action = ""
        var confidence = 0.5

        // Try to extract thought
        Regex(""""thought"\s*:\s*"(.*?)"""", RegexOption.DOT_MATCHES_ALL)
            .find(text)?.let { thought = it.groupValues[1] }

        // Try to extract action
        Regex(""""action"\s*:\s*"(\w+)"""")
            .find(text)?.let { action = it.groupValues[1] }

        // Try to extract confidence
        Regex(""""confidence"\s*:\s*([0-9.]+)""")
            .find(text)?.let { confidence = it.groupValues[1].toDoubleOrNull() ?: 0.5 }

        if (action.isNotBlank()) {
            // Try to extract action_input
            val actionInput = mutableMapOf<String, Any?>()
            when (action) {
                "RESPOND" -> {
                    Regex(""""message"\s*:\s*"(.*?)"""", RegexOption.DOT_MATCHES_ALL)
                        .find(text)?.let { actionInput["message"] = it.groupValues[1] }
                }
                "COMPLETE" -> {
                    Regex(""""summary"\s*:\s*"(.*?)"""", RegexOption.DOT_MATCHES_ALL)
                        .find(text)?.let { actionInput["summary"] = it.groupValues[1] }
                    Regex(""""details"\s*:\s*"(.*?)"""", RegexOption.DOT_MATCHES_ALL)
                        .find(text)?.let { actionInput["details"] = it.groupValues[1] }
                }
                else -> {
                    Regex(""""query"\s*:\s*"(.*?)"""").find(text)?.let { actionInput["query"] = it.groupValues[1] }
                    Regex(""""url"\s*:\s*"(.*?)"""").find(text)?.let { actionInput["url"] = it.groupValues[1] }
                    Regex(""""expression"\s*:\s*"(.*?)"""").find(text)?.let { actionInput["expression"] = it.groupValues[1] }
                    Regex(""""code"\s*:\s*"(.*?)"""", RegexOption.DOT_MATCHES_ALL).find(text)?.let { actionInput["code"] = it.groupValues[1] }
                    Regex(""""operation"\s*:\s*"(.*?)"""").find(text)?.let { actionInput["operation"] = it.groupValues[1] }
                    Regex(""""filename"\s*:\s*"(.*?)"""").find(text)?.let { actionInput["filename"] = it.groupValues[1] }
                    Regex(""""content"\s*:\s*"(.*?)"""", RegexOption.DOT_MATCHES_ALL).find(text)?.let { actionInput["content"] = it.groupValues[1] }
                    Regex(""""text"\s*:\s*"(.*?)"""", RegexOption.DOT_MATCHES_ALL).find(text)?.let { actionInput["text"] = it.groupValues[1] }
                    Regex(""""title"\s*:\s*"(.*?)"""").find(text)?.let { actionInput["title"] = it.groupValues[1] }
                }
            }

            return AgentAction(
                thought = thought,
                action = action,
                actionInput = actionInput,
                confidence = confidence
            )
        }

        return null
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractAction(json: JsonObject): AgentAction {
        val thought = json.get("thought")?.asString ?: ""
        val action = json.get("action")?.asString ?: "RESPOND"
        val confidence = json.get("confidence")?.asDouble ?: 0.5

        val actionInputJson = json.get("action_input")
        val actionInput: Map<String, Any?> = if (actionInputJson != null && actionInputJson.isJsonObject) {
            gson.fromJson(actionInputJson, Map::class.java) as Map<String, Any?>
        } else {
            emptyMap()
        }

        return AgentAction(
            thought = thought,
            action = action.uppercase(),
            actionInput = actionInput,
            confidence = confidence.coerceIn(0.0, 1.0)
        )
    }
}
