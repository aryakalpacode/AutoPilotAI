package com.autopilot.agent.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Request body for OpenRouter chat completion API.
 */
data class ChatCompletionRequest(
    @SerializedName("model")
    val model: String,

    @SerializedName("messages")
    val messages: List<ChatMessage>,

    @SerializedName("temperature")
    val temperature: Double = 0.7,

    @SerializedName("max_tokens")
    val maxTokens: Int = 4096,

    @SerializedName("top_p")
    val topP: Double = 0.9,

    @SerializedName("stream")
    val stream: Boolean = false
)

/**
 * A single message in the chat completion messages array.
 */
data class ChatMessage(
    @SerializedName("role")
    val role: String, // "system", "user", "assistant"

    @SerializedName("content")
    val content: String
)
