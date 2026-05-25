package com.autopilot.agent.data.remote

import com.autopilot.agent.data.remote.dto.ChatCompletionRequest
import com.autopilot.agent.data.remote.dto.ChatCompletionResponse
import com.autopilot.agent.data.remote.dto.ModelsResponse
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit interface for OpenRouter API endpoints.
 */
interface OpenRouterApi {

    /**
     * Create a chat completion using the specified model.
     */
    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authHeader: String,
        @Header("HTTP-Referer") referer: String = "https://autopilot-ai.app",
        @Header("X-Title") title: String = "AutoPilot AI",
        @Body request: ChatCompletionRequest
    ): Response<ChatCompletionResponse>

    /**
     * Get list of available models from OpenRouter.
     */
    @GET("v1/models")
    suspend fun getModels(
        @Header("Authorization") authHeader: String
    ): Response<ModelsResponse>
}
