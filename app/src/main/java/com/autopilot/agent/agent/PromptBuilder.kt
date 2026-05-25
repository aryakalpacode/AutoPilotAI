package com.autopilot.agent.agent

import com.autopilot.agent.domain.model.Message
import com.autopilot.agent.domain.model.MessageRole
import com.autopilot.agent.data.remote.dto.ChatMessage
import com.autopilot.agent.util.currentIso8601
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds prompts for the autonomous agent, including the system prompt
 * and message history formatting.
 */
@Singleton
class PromptBuilder @Inject constructor() {

    companion object {
        private val SYSTEM_PROMPT = """
You are AutoPilot AI, an autonomous AI agent running on an Android device.
You can think step-by-step and use tools to accomplish complex tasks.

AVAILABLE TOOLS:
1. WEB_SEARCH - Search the internet. Input: {"query": "search terms"}
2. WEB_SCRAPE - Read a webpage. Input: {"url": "https://..."}
3. FILE_MANAGER - Manage files. Input: {"operation": "create|read|update|delete|list", "filename": "name.ext", "content": "..."}
4. CODE_EXECUTOR - Run JavaScript. Input: {"code": "console.log('hello')"}
5. CALCULATOR - Math operations. Input: {"expression": "2+2*3"}
6. CLIPBOARD - Copy/paste. Input: {"operation": "copy|paste", "text": "..."}
7. DEVICE_INFO - Get device info. Input: {}
8. NOTES_DATABASE - Save/retrieve notes. Input: {"operation": "create|read|update|delete|list", "title": "...", "content": "...", "tags": ["..."]}
9. REMINDER - Set reminders. Input: {"title": "...", "message": "...", "time": "ISO8601"}
10. TEXT_PROCESSOR - Process text. Input: {"operation": "summarize|translate|extract_keys|format", "text": "...", "target_language": "..."}

RESPONSE FORMAT (you MUST respond in this exact JSON format, nothing else):
{
  "thought": "Your step-by-step reasoning about what to do next",
  "action": "TOOL_NAME or RESPOND or COMPLETE",
  "action_input": { ... parameters for the tool ... },
  "confidence": 0.0 to 1.0
}

If action is "RESPOND", action_input should be: {"message": "your message to user"}
If action is "COMPLETE", action_input should be: {"summary": "final result summary", "details": "detailed output"}

RULES:
- Always think before acting
- Use the minimum number of tool calls needed
- If a tool fails, try an alternative approach
- Never make up information - always verify via tools
- If you cannot complete a task, explain why honestly
- For multi-step tasks, plan all steps first, then execute one at a time
- After using WEB_SEARCH, always WEB_SCRAPE at least one result for details
- Keep responses concise but complete
        """.trimIndent()
    }

    /**
     * Build the system prompt with current context information.
     */
    fun buildSystemPrompt(personality: String = "professional"): String {
        val personalitySuffix = when (personality.lowercase()) {
            "casual" -> "\n\nPersonality: Be friendly and casual in your responses. Use emojis occasionally."
            "creative" -> "\n\nPersonality: Be creative and enthusiastic. Think outside the box."
            "professional" -> "\n\nPersonality: Be professional, precise, and thorough."
            else -> ""
        }

        return "$SYSTEM_PROMPT$personalitySuffix\n\nCurrent date/time: ${currentIso8601()}"
    }

    /**
     * Build the message list for the API call from conversation history.
     */
    fun buildMessages(
        systemPrompt: String,
        history: List<Message>,
        currentUserMessage: String? = null
    ): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()

        // System prompt always first
        messages.add(ChatMessage(role = "system", content = systemPrompt))

        // Conversation history
        for (msg in history) {
            when (msg.role) {
                MessageRole.USER -> {
                    messages.add(ChatMessage(role = "user", content = msg.content))
                }
                MessageRole.ASSISTANT -> {
                    messages.add(ChatMessage(role = "assistant", content = msg.content))
                }
                MessageRole.TOOL_CALL -> {
                    // Include tool calls as assistant messages
                    messages.add(ChatMessage(role = "assistant", content = msg.content))
                }
                MessageRole.TOOL_RESULT -> {
                    // Include tool results as user messages (observation)
                    val toolContent = "Tool result from ${msg.toolName ?: "unknown"}:\n${msg.content}"
                    messages.add(ChatMessage(role = "user", content = toolContent))
                }
                MessageRole.ERROR -> {
                    messages.add(ChatMessage(role = "user", content = "Error: ${msg.content}"))
                }
                MessageRole.SYSTEM -> {
                    // Additional system messages get merged
                    messages.add(ChatMessage(role = "system", content = msg.content))
                }
            }
        }

        // Current user message if not already in history
        if (currentUserMessage != null) {
            messages.add(ChatMessage(role = "user", content = currentUserMessage))
        }

        return messages
    }

    /**
     * Build a task decomposition prompt for the agent.
     */
    fun buildTaskDecompositionPrompt(userGoal: String): String {
        return """
The user wants to accomplish the following goal:
"$userGoal"

Break this down into a series of concrete, ordered sub-tasks that you can execute using the available tools.
Respond with a plan in this JSON format:
{
  "thought": "My analysis of the goal and how to approach it",
  "action": "RESPOND",
  "action_input": {
    "message": "Here's my plan to accomplish your goal:\n1. [First step]\n2. [Second step]\n..."
  },
  "confidence": 0.8
}

After presenting the plan, I will execute each step one at a time.
        """.trimIndent()
    }

    /**
     * Build a re-prompt for when the LLM response was malformed.
     */
    fun buildReprompt(): String {
        return """
Your previous response was not in valid JSON format. Please respond ONLY with a JSON object in this exact format (no markdown, no extra text):
{
  "thought": "your reasoning",
  "action": "TOOL_NAME or RESPOND or COMPLETE",
  "action_input": { ... },
  "confidence": 0.0 to 1.0
}
        """.trimIndent()
    }

    /**
     * Build a context summary request prompt.
     */
    fun buildSummaryPrompt(messagesToSummarize: List<Message>): String {
        val content = messagesToSummarize.joinToString("\n\n") { msg ->
            "${msg.role.value}: ${msg.content}"
        }
        return """
Summarize the following conversation history concisely, preserving all key facts, decisions, and results:

$content

Provide a concise summary that captures all important information.
        """.trimIndent()
    }
}
