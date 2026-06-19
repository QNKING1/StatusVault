package com.statusvault.app.data.repository

import com.statusvault.app.data.db.RecoveredMessageDao
import com.statusvault.app.data.entity.RecoveredMessage
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
    private val messageDao: RecoveredMessageDao
) {

    fun getAllMessages(): Flow<List<RecoveredMessage>> =
        messageDao.getAll()

    fun getDeletedMessages(): Flow<List<RecoveredMessage>> =
        messageDao.getDeleted()

    fun getMessagesBySender(sender: String): Flow<List<RecoveredMessage>> =
        messageDao.getBySender(sender)

    fun searchMessages(query: String, deletedOnly: Boolean = false): Flow<List<RecoveredMessage>> {
        return if (deletedOnly) {
            messageDao.searchDeleted(query)
        } else {
            messageDao.search(query)
        }
    }

    suspend fun insertMessage(message: RecoveredMessage): Long {
        return messageDao.insert(message)
    }

    suspend fun markAsDeleted(messageId: Long) {
        messageDao.markAsDeleted(messageId)
    }

    suspend fun markAsDeletedByContent(sender: String, content: String, timestamp: Long) {
        // Allow 5-second window for timestamp matching
        val window = 5000L
        messageDao.markAsDeletedByContent(
            sender = sender,
            content = content,
            minTimestamp = timestamp - window,
            maxTimestamp = timestamp + window
        )
    }

    suspend fun findMessageByContent(sender: String, content: String): RecoveredMessage? {
        return messageDao.findByContent(sender, content)
    }

    suspend fun deleteMessage(messageId: Long) {
        messageDao.deleteById(messageId)
    }

    suspend fun clearAllMessages() {
        messageDao.clearAll()
    }

    fun getDeletedCount(): Flow<Int> = messageDao.getDeletedCount()
    fun getTotalCount(): Flow<Int> = messageDao.getTotalCount()
}
