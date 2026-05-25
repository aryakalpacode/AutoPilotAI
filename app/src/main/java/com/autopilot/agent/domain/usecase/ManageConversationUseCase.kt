package com.autopilot.agent.domain.usecase

import com.autopilot.agent.data.repository.ConversationRepository
import com.autopilot.agent.domain.model.Conversation
import com.autopilot.agent.domain.model.Message
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for managing conversations and their messages.
 */
class ManageConversationUseCase @Inject constructor(
    private val conversationRepo: ConversationRepository
) {
    /** Create a new conversation. */
    suspend fun create(title: String, model: String): Long {
        return conversationRepo.createConversation(title, model)
    }

    /** Get all conversations as Flow. */
    fun getAll(): Flow<List<Conversation>> {
        return conversationRepo.getAllConversations()
    }

    /** Get messages for a conversation. */
    fun getMessages(conversationId: Long): Flow<List<Message>> {
        return conversationRepo.getMessages(conversationId)
    }

    /** Add a message to a conversation. */
    suspend fun addMessage(message: Message): Long {
        return conversationRepo.addMessage(message)
    }

    /** Delete a conversation. */
    suspend fun delete(id: Long) {
        conversationRepo.deleteConversation(id)
    }

    /** Rename a conversation. */
    suspend fun rename(id: Long, title: String) {
        conversationRepo.updateTitle(id, title)
    }
}
