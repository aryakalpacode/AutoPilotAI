package com.autopilot.agent.data.repository

import com.autopilot.agent.data.local.dao.ApiKeyDao
import com.autopilot.agent.data.local.entity.ApiKeyEntity
import com.autopilot.agent.data.remote.OpenRouterApi
import com.autopilot.agent.data.remote.dto.ModelInfo
import com.autopilot.agent.security.KeyStoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing API keys and model information.
 */
@Singleton
class ModelRepository @Inject constructor(
    private val openRouterApi: OpenRouterApi,
    private val apiKeyDao: ApiKeyDao,
    private val keyStoreManager: KeyStoreManager
) {

    /** Cached list of free models. */
    private var cachedFreeModels: List<ModelInfo> = emptyList()

    /** Save an API key (encrypted). */
    suspend fun saveApiKey(apiKey: String) {
        withContext(Dispatchers.IO) {
            // Deactivate existing keys
            apiKeyDao.deactivateAll()

            // Encrypt the key
            val (encrypted, iv) = keyStoreManager.encrypt(apiKey)

            // Store in database
            apiKeyDao.insert(
                ApiKeyEntity(
                    encryptedKey = encrypted,
                    iv = iv,
                    isActive = true
                )
            )
        }
    }

    /** Retrieve the active API key (decrypted). */
    suspend fun getApiKey(): String? {
        return withContext(Dispatchers.IO) {
            val entity = apiKeyDao.getActiveKey() ?: return@withContext null
            try {
                keyStoreManager.decrypt(entity.encryptedKey, entity.iv)
            } catch (e: Exception) {
                null
            }
        }
    }

    /** Check if an API key is stored. */
    suspend fun hasApiKey(): Boolean {
        return withContext(Dispatchers.IO) {
            apiKeyDao.hasActiveKey() > 0
        }
    }

    /** Delete all API keys. */
    suspend fun deleteApiKey() {
        withContext(Dispatchers.IO) {
            apiKeyDao.deleteByProvider()
        }
    }

    /** Test the API connection by fetching models. Returns count of free models or error. */
    suspend fun testConnection(apiKey: String): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val response = openRouterApi.getModels("Bearer $apiKey")
                if (response.isSuccessful) {
                    val models = response.body()?.data ?: emptyList()
                    val freeModels = models.filter { model ->
                        model.pricing?.let { pricing ->
                            pricing.prompt == "0" && pricing.completion == "0"
                        } ?: false
                    }
                    cachedFreeModels = freeModels
                    Result.success(freeModels.size)
                } else {
                    val errorBody = response.errorBody()?.string()
                    Result.failure(Exception("API error ${response.code()}: $errorBody"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /** Fetch available free models from OpenRouter. */
    suspend fun fetchFreeModels(): Result<List<ModelInfo>> {
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = getApiKey() ?: return@withContext Result.failure(
                    Exception("No API key configured")
                )

                val response = openRouterApi.getModels("Bearer $apiKey")
                if (response.isSuccessful) {
                    val models = response.body()?.data ?: emptyList()
                    val freeModels = models.filter { model ->
                        model.pricing?.let { pricing ->
                            pricing.prompt == "0" && pricing.completion == "0"
                        } ?: false
                    }
                    cachedFreeModels = freeModels
                    Result.success(freeModels)
                } else {
                    Result.failure(Exception("API error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /** Get cached free models. */
    fun getCachedFreeModels(): List<ModelInfo> = cachedFreeModels

    /** Get the authorization header value. */
    suspend fun getAuthHeader(): String? {
        val key = getApiKey() ?: return null
        return "Bearer $key"
    }

    /** Get context window size for a model. */
    fun getContextWindow(modelId: String): Int {
        return cachedFreeModels.find { it.id == modelId }?.contextLength ?: 32768
    }
}
