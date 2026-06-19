package com.statusvault.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recovered_messages",
    indices = [
        Index(value = ["sender"]),
        Index(value = ["timestamp"]),
        Index(value = ["isDeleted"])
    ]
)
data class RecoveredMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String,
    val content: String,
    val timestamp: Long,
    val isDeleted: Boolean = false,
    val packageName: String = "",
    val groupName: String? = null
) {
    fun getDisplayName(): String {
        return groupName?.takeIf { it.isNotBlank() } ?: sender
    }
}
