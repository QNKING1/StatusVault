package com.statusvault.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "status_files")
data class StatusFile(
    @PrimaryKey
    val id: String,
    val path: String,
    val type: StatusType,
    val dateAdded: Long,
    val isSaved: Boolean = false,
    val fileSize: Long = 0L,
    val fileName: String = "",
    val sourcePackage: String = ""
) {
    enum class StatusType {
        IMAGE, VIDEO
    }
}
