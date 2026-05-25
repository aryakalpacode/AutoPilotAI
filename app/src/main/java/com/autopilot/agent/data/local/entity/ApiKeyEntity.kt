package com.autopilot.agent.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing encrypted API keys.
 */
@Entity(tableName = "api_keys")
data class ApiKeyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "provider")
    val provider: String = "openrouter",

    @ColumnInfo(name = "encrypted_key")
    val encryptedKey: String,

    @ColumnInfo(name = "iv")
    val iv: String, // initialization vector for AES-GCM decryption

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
