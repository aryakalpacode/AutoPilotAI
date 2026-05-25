package com.autopilot.agent.util

/**
 * Utility for estimating token counts for LLM context management.
 * Uses a simple character-based heuristic (~4 chars per token).
 */
object TokenCounter {

    /** Estimate token count for a single string. */
    fun estimate(text: String): Int {
        return (text.length + Constants.TOKEN_CHAR_RATIO - 1) / Constants.TOKEN_CHAR_RATIO
    }

    /** Estimate total token count for a list of messages. */
    fun estimateMessages(messages: List<Pair<String, String>>): Int {
        return messages.sumOf { (role, content) ->
            estimate(role) + estimate(content) + 4 // overhead per message
        }
    }

    /** Check if the messages are approaching the context limit. */
    fun isApproachingLimit(totalTokens: Int, contextWindow: Int): Boolean {
        return totalTokens >= (contextWindow * Constants.CONTEXT_WINDOW_THRESHOLD).toInt()
    }

    /** Calculate remaining tokens available. */
    fun remainingTokens(usedTokens: Int, contextWindow: Int): Int {
        return (contextWindow - usedTokens).coerceAtLeast(0)
    }
}
