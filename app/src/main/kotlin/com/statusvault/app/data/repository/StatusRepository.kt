package com.statusvault.app.data.repository

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.statusvault.app.data.db.StatusFileDao
import com.statusvault.app.data.entity.StatusFile
import com.statusvault.app.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatusRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val statusFileDao: StatusFileDao
) {

    fun getImageStatuses(): Flow<List<StatusFile>> =
        statusFileDao.getByType(StatusFile.StatusType.IMAGE)

    fun getVideoStatuses(): Flow<List<StatusFile>> =
        statusFileDao.getByType(StatusFile.StatusType.VIDEO)

    fun getSavedStatuses(): Flow<List<StatusFile>> =
        statusFileDao.getSaved()

    fun getAllStatuses(): Flow<List<StatusFile>> =
        statusFileDao.getAll()

    suspend fun refreshStatuses(): Result<List<StatusFile>> = withContext(Dispatchers.IO) {
        try {
            val foundStatuses = scanWhatsAppStatusFolders()

            // Update database with newly found statuses
            val existingIds = statusFileDao.getAll().first().map { it.id }.toSet()
            val newStatuses = foundStatuses.filter { it.id !in existingIds }

            if (newStatuses.isNotEmpty()) {
                statusFileDao.insertAll(newStatuses)
            }

            // Remove stale entries that no longer exist on disk
            val foundIds = foundStatuses.map { it.id }.toSet()
            val toRemove = existingIds.filter { it !in foundIds }
            toRemove.forEach { statusFileDao.deleteById(it) }

            Result.success(foundStatuses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveStatus(statusFile: StatusFile): Result<String> = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(statusFile.path)
            if (!sourceFile.exists()) {
                return@withContext Result.failure(Exception("Source file not found"))
            }

            val fileName = statusFile.fileName.ifEmpty {
                "SV_${System.currentTimeMillis()}.${sourceFile.extension}"
            }

            val relativeDir = if (statusFile.type == StatusFile.StatusType.IMAGE) {
                Constants.SAVED_IMAGES_DIR
            } else {
                Constants.SAVED_VIDEOS_DIR
            }

            val savedUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveUsingMediaStore(sourceFile, fileName, relativeDir, statusFile.type)
            } else {
                saveUsingDirectFile(sourceFile, fileName, relativeDir)
            }

            if (savedUri != null) {
                statusFileDao.updateSavedStatus(statusFile.id, true)
                Result.success(savedUri.toString())
            } else {
                Result.failure(Exception("Failed to save file"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSavedStatus(statusFile: StatusFile): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                deleteViaMediaStore(statusFile)
            } else {
                val file = File(statusFile.path)
                if (file.exists()) file.delete()
            }
            statusFileDao.updateSavedStatus(statusFile.id, false)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cleanupSavedStatuses(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val saved = statusFileDao.getSaved().first()
            var count = 0

            saved.forEach { status ->
                val file = File(status.path)
                if (status.isSaved) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        deleteViaMediaStore(status)
                    } else {
                        if (file.exists()) file.delete()
                    }
                    count++
                }
            }

            statusFileDao.deleteAllSaved()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun scanWhatsAppStatusFolders(): List<StatusFile> {
        val statuses = mutableListOf<StatusFile>()
        val storageDir = Environment.getExternalStorageDirectory()

        Constants.WHATSAPP_STATUS_PATHS.forEach { relativePath ->
            val fullPath = File(storageDir, relativePath)
            if (fullPath.exists() && fullPath.isDirectory) {
                val sourcePackage = when {
                    relativePath.contains("w4b") -> Constants.WHATSAPP_BUSINESS_PACKAGE
                    else -> Constants.WHATSAPP_PACKAGE
                }
                statuses.addAll(scanDirectory(fullPath, sourcePackage))
            }
        }

        return statuses.distinctBy { it.id }.sortedByDescending { it.dateAdded }
    }

    private fun scanDirectory(dir: File, sourcePackage: String): List<StatusFile> {
        return dir.listFiles { file ->
            file.isFile && !file.name.startsWith(".")
        }?.mapNotNull { file ->
            val ext = file.extension.lowercase()
            val type = when {
                ext in Constants.IMAGE_EXTENSIONS -> StatusFile.StatusType.IMAGE
                ext in Constants.VIDEO_EXTENSIONS -> StatusFile.StatusType.VIDEO
                else -> null
            }

            type?.let {
                StatusFile(
                    id = file.absolutePath.hashCode().toString(),
                    path = file.absolutePath,
                    type = it,
                    dateAdded = file.lastModified(),
                    isSaved = false,
                    fileSize = file.length(),
                    fileName = file.name,
                    sourcePackage = sourcePackage
                )
            }
        } ?: emptyList()
    }

    private fun saveUsingMediaStore(
        sourceFile: File,
        fileName: String,
        relativeDir: String,
        type: StatusFile.StatusType
    ): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, getMimeType(type, sourceFile.extension))
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/$relativeDir")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val collection = if (type == StatusFile.StatusType.IMAGE) {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val uri = context.contentResolver.insert(collection, contentValues) ?: return null

        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                FileInputStream(sourceFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            context.contentResolver.update(uri, contentValues, null, null)
            uri
        } catch (e: Exception) {
            context.contentResolver.delete(uri, null, null)
            null
        }
    }

    private fun saveUsingDirectFile(sourceFile: File, fileName: String, relativeDir: String): Uri? {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val saveDir = File(picturesDir, relativeDir).apply { mkdirs() }
        val destFile = File(saveDir, fileName)

        return try {
            sourceFile.copyTo(destFile, overwrite = true)
            MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), null, null)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", destFile)
        } catch (e: Exception) {
            null
        }
    }

    private fun deleteViaMediaStore(statusFile: StatusFile) {
        val uri = if (statusFile.type == StatusFile.StatusType.IMAGE) {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(statusFile.fileName)

        context.contentResolver.delete(uri, selection, selectionArgs)
    }

    private fun getMimeType(type: StatusFile.StatusType, extension: String): String {
        return when (type) {
            StatusFile.StatusType.IMAGE -> when (extension.lowercase()) {
                "png" -> "image/png"
                "gif" -> "image/gif"
                "webp" -> "image/webp"
                else -> "image/jpeg"
            }
            StatusFile.StatusType.VIDEO -> when (extension.lowercase()) {
                "avi" -> "video/x-msvideo"
                "mkv" -> "video/x-matroska"
                "mov" -> "video/quicktime"
                "3gp" -> "video/3gpp"
                "webm" -> "video/webm"
                else -> "video/mp4"
            }
        }
    }
}
