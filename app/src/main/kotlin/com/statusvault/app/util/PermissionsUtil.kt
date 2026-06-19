package com.statusvault.app.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object PermissionsUtil {

    private const val TAG = "PermissionsUtil"

    // Storage Permissions
    fun hasStoragePermission(context: Context): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.READ_MEDIA_VIDEO
                ) == PackageManager.PERMISSION_GRANTED
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
            else -> {
                ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    fun hasAllFilesPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            hasStoragePermission(context)
        }
    }

    fun requestStoragePermission(activity: Activity) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(
                        android.Manifest.permission.READ_MEDIA_IMAGES,
                        android.Manifest.permission.READ_MEDIA_VIDEO
                    ),
                    REQUEST_STORAGE
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_STORAGE
                )
            }
            else -> {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    REQUEST_STORAGE
                )
            }
        }
    }

    /**
     * Safely open the "Manage All Files" settings page.
     * Uses a fallback chain to handle devices that don't support the intent:
     * 1. Try ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION with package URI
     * 2. Fall back to ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION (no package filter)
     * 3. Fall back to app details settings page
     */
    fun openManageAllFilesSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Attempt 1: App-specific all-files-access settings
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            } catch (e: ActivityNotFoundException) {
                Log.w(TAG, "ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION not supported", e)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to open app-specific all-files settings", e)
            }

            // Attempt 2: General all-files-access settings (no package filter)
            try {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            } catch (e: ActivityNotFoundException) {
                Log.w(TAG, "ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION not supported", e)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to open all-files settings", e)
            }
        }

        // Fallback: App details settings page
        openAppDetailsSettings(context)
    }

    /**
     * Safely open the app's details settings page.
     */
    fun openAppDetailsSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open app details settings", e)
            // Last resort: open general settings
            try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to open any settings page", e2)
            }
        }
    }

    /**
     * Safely open the notification listener settings page.
     */
    fun openNotificationListenerSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open notification listener settings", e)
            openAppDetailsSettings(context)
        }
    }

    // Notification Access Permission
    fun hasNotificationAccess(context: Context): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)
    }

    // Post Notification Permission (Android 13+)
    fun hasPostNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun requestPostNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_POST_NOTIFICATION
            )
        }
    }

    // Check if WhatsApp is installed
    fun isWhatsAppInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(Constants.WHATSAPP_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            try {
                context.packageManager.getPackageInfo(Constants.WHATSAPP_BUSINESS_PACKAGE, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    fun getRequiredPermissionsArray(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                arrayOf(
                    android.Manifest.permission.READ_MEDIA_IMAGES,
                    android.Manifest.permission.READ_MEDIA_VIDEO,
                    android.Manifest.permission.POST_NOTIFICATIONS
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }
            else -> {
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
        }
    }

    // Request codes
    const val REQUEST_STORAGE = 1001
    const val REQUEST_MANAGE_ALL_FILES = 1002
    const val REQUEST_NOTIFICATION_ACCESS = 1003
    const val REQUEST_POST_NOTIFICATION = 1004
}
