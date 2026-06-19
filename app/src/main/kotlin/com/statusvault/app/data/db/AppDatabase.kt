package com.statusvault.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.statusvault.app.data.entity.RecoveredMessage
import com.statusvault.app.data.entity.StatusFile

@Database(
    entities = [StatusFile::class, RecoveredMessage::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun statusFileDao(): StatusFileDao
    abstract fun recoveredMessageDao(): RecoveredMessageDao
}
