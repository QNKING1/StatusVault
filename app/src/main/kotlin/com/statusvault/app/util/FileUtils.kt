package com.statusvault.app.util

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File

object FileUtils {

    fun getFileExtension(fileName: String): String {
        return fileName.substringAfterLast('.', "")
    }

    fun isImageFile(fileName: String): Boolean {
        return getFileExtension(fileName).lowercase() in Constants.IMAGE_EXTENSIONS
    }

    fun isVideoFile(fileName: String): Boolean {
        return getFileExtension(fileName).lowercase() in Constants.VIDEO_EXTENSIONS
    }

    fun getFileSizeString(sizeInBytes: Long): String {
        return when {
            sizeInBytes >= 1_073_741_824 -> String.format("%.1f GB", sizeInBytes / 1_073_741_824f)
            sizeInBytes >= 1_048_576 -> String.format("%.1f MB", sizeInBytes / 1_048_576f)
            sizeInBytes >= 1024 -> String.format("%.1f KB", sizeInBytes / 1024f)
            else -> "$sizeInBytes B"
        }
    }

    fun getMimeType(fileName: String): String {
        val extension = getFileExtension(fileName)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: "*/*"
    }

    fun getStatusVaultSaveDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), Constants.SAVED_STATUS_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getFileFromUri(context: Context, uri: Uri): File? {
        return try {
            val projection = arrayOf(android.provider.MediaStore.MediaColumns.DATA)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val path = cursor.getString(0)
                    File(path).takeIf { it.exists() }
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun formatDurationMillis(durationMs: Long): String {
        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / (1000 * 60)) % 60
        val hours = durationMs / (1000 * 60 * 60)

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
}
