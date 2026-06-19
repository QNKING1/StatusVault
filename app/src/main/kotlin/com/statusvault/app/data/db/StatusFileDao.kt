package com.statusvault.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.statusvault.app.data.entity.StatusFile
import kotlinx.coroutines.flow.Flow

@Dao
interface StatusFileDao {

    @Query("SELECT * FROM status_files WHERE type = :type ORDER BY dateAdded DESC")
    fun getByType(type: StatusFile.StatusType): Flow<List<StatusFile>>

    @Query("SELECT * FROM status_files ORDER BY dateAdded DESC")
    fun getAll(): Flow<List<StatusFile>>

    @Query("SELECT * FROM status_files WHERE isSaved = 1 ORDER BY dateAdded DESC")
    fun getSaved(): Flow<List<StatusFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(statuses: List<StatusFile>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(statusFile: StatusFile)

    @Update
    suspend fun update(statusFile: StatusFile)

    @Query("UPDATE status_files SET isSaved = :isSaved WHERE id = :id")
    suspend fun updateSavedStatus(id: String, isSaved: Boolean)

    @Query("DELETE FROM status_files WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM status_files WHERE isSaved = 1")
    suspend fun deleteAllSaved()

    @Query("SELECT COUNT(*) FROM status_files WHERE isSaved = 1")
    suspend fun getSavedCount(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM status_files WHERE id = :id)")
    suspend fun exists(id: String): Boolean

    @Query("DELETE FROM status_files")
    suspend fun clearAll()
}
