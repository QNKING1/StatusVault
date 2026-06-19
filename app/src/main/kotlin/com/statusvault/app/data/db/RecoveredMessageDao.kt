package com.statusvault.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.statusvault.app.data.entity.RecoveredMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface RecoveredMessageDao {

    @Query("SELECT * FROM recovered_messages ORDER BY timestamp DESC")
    fun getAll(): Flow<List<RecoveredMessage>>

    @Query("SELECT * FROM recovered_messages WHERE isDeleted = 1 ORDER BY timestamp DESC")
    fun getDeleted(): Flow<List<RecoveredMessage>>

    @Query("SELECT * FROM recovered_messages WHERE sender = :sender OR groupName = :sender ORDER BY timestamp DESC")
    fun getBySender(sender: String): Flow<List<RecoveredMessage>>

    @Query("SELECT * FROM recovered_messages WHERE (sender LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR groupName LIKE '%' || :query || '%') ORDER BY timestamp DESC")
    fun search(query: String): Flow<List<RecoveredMessage>>

    @Query("SELECT * FROM recovered_messages WHERE isDeleted = 1 AND (sender LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR groupName LIKE '%' || :query || '%') ORDER BY timestamp DESC")
    fun searchDeleted(query: String): Flow<List<RecoveredMessage>>

    @Query("SELECT DISTINCT sender FROM recovered_messages ORDER BY sender ASC")
    fun getAllSenders(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: RecoveredMessage): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<RecoveredMessage>)

    @Update
    suspend fun update(message: RecoveredMessage)

    @Query("UPDATE recovered_messages SET isDeleted = 1 WHERE id = :id")
    suspend fun markAsDeleted(id: Long)

    @Query("UPDATE recovered_messages SET isDeleted = 1 WHERE sender = :sender AND content = :content AND timestamp >= :minTimestamp AND timestamp <= :maxTimestamp")
    suspend fun markAsDeletedByContent(sender: String, content: String, minTimestamp: Long, maxTimestamp: Long)

    @Query("SELECT * FROM recovered_messages WHERE sender = :sender AND content = :content ORDER BY timestamp DESC LIMIT 1")
    suspend fun findByContent(sender: String, content: String): RecoveredMessage?

    @Query("DELETE FROM recovered_messages WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM recovered_messages")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM recovered_messages WHERE isDeleted = 1")
    fun getDeletedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM recovered_messages")
    fun getTotalCount(): Flow<Int>
}
