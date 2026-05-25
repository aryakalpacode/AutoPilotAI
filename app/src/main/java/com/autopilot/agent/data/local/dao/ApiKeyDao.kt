package com.autopilot.agent.data.local.dao

import androidx.room.*
import com.autopilot.agent.data.local.entity.ApiKeyEntity

/**
 * Data access object for API key entities.
 */
@Dao
interface ApiKeyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(apiKey: ApiKeyEntity): Long

    @Update
    suspend fun update(apiKey: ApiKeyEntity)

    @Delete
    suspend fun delete(apiKey: ApiKeyEntity)

    @Query("SELECT * FROM api_keys WHERE provider = :provider AND is_active = 1 LIMIT 1")
    suspend fun getActiveKey(provider: String = "openrouter"): ApiKeyEntity?

    @Query("SELECT * FROM api_keys WHERE provider = :provider")
    suspend fun getKeysByProvider(provider: String = "openrouter"): List<ApiKeyEntity>

    @Query("UPDATE api_keys SET is_active = 0 WHERE provider = :provider")
    suspend fun deactivateAll(provider: String = "openrouter")

    @Query("DELETE FROM api_keys WHERE provider = :provider")
    suspend fun deleteByProvider(provider: String = "openrouter")

    @Query("SELECT COUNT(*) FROM api_keys WHERE is_active = 1")
    suspend fun hasActiveKey(): Int
}
