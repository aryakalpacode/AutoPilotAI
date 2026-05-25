package com.autopilot.agent.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response from OpenRouter chat completion API.
 */
data class ChatCompletionResponse(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName("choices")
    val choices: List<Choice>? = null,

    @SerializedName("usage")
    val usage: Usage? = null,

    @SerializedName("error")
    val error: ApiError? = null
)

data class Choice(
    @SerializedName("message")
    val message: ChatMessage? = null,

    @SerializedName("finish_reason")
    val finishReason: String? = null,

    @SerializedName("index")
    val index: Int? = null
)

data class Usage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int = 0,

    @SerializedName("completion_tokens")
    val completionTokens: Int = 0,

    @SerializedName("total_tokens")
    val totalTokens: Int = 0
)

data class ApiError(
    @SerializedName("message")
    val message: String? = null,

    @SerializedName("type")
    val type: String? = null,

    @SerializedName("code")
    val code: Int? = null
)
