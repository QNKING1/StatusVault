package com.statusvault.app.util

object Constants {
    // WhatsApp Status Paths
    const val WHATSAPP_PACKAGE = "com.whatsapp"
    const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"

    val WHATSAPP_STATUS_PATHS = listOf(
        "Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
        "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses",
        "WhatsApp/Media/.Statuses"
    )

    // Save Directory
    const val SAVED_STATUS_DIR = "StatusVault"
    const val SAVED_IMAGES_DIR = "StatusVault/Images"
    const val SAVED_VIDEOS_DIR = "StatusVault/Videos"

    // Database
    const val DATABASE_NAME = "statusvault_db"

    // WorkManager
    const val STATUS_SCAN_WORK_NAME = "status_scan_work"
    const val STATUS_SCAN_INTERVAL_MINUTES = 15L

    // Notification Channel
    const val CHANNEL_ID_DELETED_MESSAGES = "deleted_messages_channel"
    const val NOTIFICATION_ID_DELETED = 1001

    // DataStore Keys
    const val PREF_DARK_MODE = "dark_mode"
    const val PREF_AUTO_SAVE = "auto_save"
    const val PREF_SYSTEM_THEME = "system_theme"

    // Tab Indices
    const val TAB_IMAGES = 0
    const val TAB_VIDEOS = 1

    // Image extensions
    val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp")
    val VIDEO_EXTENSIONS = setOf("mp4", "avi", "mkv", "mov", "3gp", "webm")
}
