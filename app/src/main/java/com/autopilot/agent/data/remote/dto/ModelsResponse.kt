package com.autopilot.agent.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response from OpenRouter models list API.
 */
data class ModelsResponse(
    @SerializedName("data")
    val data: List<ModelInfo>? = null
)

data class ModelInfo(
    @SerializedName("id")
    val id: String,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("pricing")
    val pricing: ModelPricing? = null,

    @SerializedName("context_length")
    val contextLength: Int? = null,

    @SerializedName("top_provider")
    val topProvider: TopProvider? = null,

    @SerializedName("architecture")
    val architecture: ModelArchitecture? = null
)

data class ModelPricing(
    @SerializedName("prompt")
    val prompt: String? = null,

    @SerializedName("completion")
    val completion: String? = null
)

data class TopProvider(
    @SerializedName("max_completion_tokens")
    val maxCompletionTokens: Int? = null
)

data class ModelArchitecture(
    @SerializedName("tokenizer")
    val tokenizer: String? = null,

    @SerializedName("instruct_type")
    val instructType: String? = null
)
