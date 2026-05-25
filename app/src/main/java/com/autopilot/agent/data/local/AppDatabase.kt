package com.autopilot.agent.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.autopilot.agent.data.local.dao.*
import com.autopilot.agent.data.local.entity.*

/**
 * Room database definition for AutoPilot AI.
 * Contains all tables for conversations, messages, tasks, notes, and API keys.
 */
@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        TaskEntity::class,
        NoteEntity::class,
        ApiKeyEntity::class,
        TaskTemplateEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun taskDao(): TaskDao
    abstract fun noteDao(): NoteDao
    abstract fun apiKeyDao(): ApiKeyDao
}
