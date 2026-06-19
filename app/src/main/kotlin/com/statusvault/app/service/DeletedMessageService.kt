package com.statusvault.app.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.statusvault.app.R
import com.statusvault.app.data.entity.RecoveredMessage
import com.statusvault.app.data.repository.MessageRepository
import com.statusvault.app.ui.MainActivity
import com.statusvault.app.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DeletedMessageService : NotificationListenerService() {

    @Inject
    lateinit var messageRepository: MessageRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeNotifications = mutableMapOf<String, NotificationData>()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "DeletedMessageService created")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "DeletedMessageService destroyed")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        val packageName = sbn.packageName
        if (packageName !in WHATSAP_PACKAGES) return

        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: extras.get("android.title")?.toString() ?: "Unknown"
        val text = extras.getCharSequence("android.text")?.toString()
            ?: extras.getCharSequence("android.bigText")?.toString()
            ?: ""

        // Skip empty or system messages
        if (text.isBlank() || title.isBlank()) return

        // Skip "N new messages" summaries
        if (text.contains("new message", ignoreCase = true) ||
            text.contains("missed calls", ignoreCase = true) ||
            title == "WhatsApp" || title == "WhatsApp Business"
        ) return

        // Extract group name if present (format: "Sender: Group Name" or similar)
        val (sender, groupName) = extractSenderAndGroup(title)

        val notificationData = NotificationData(
            key = sbn.key,
            sender = sender,
            content = text,
            timestamp = sbn.postTime,
            packageName = packageName,
            groupName = groupName
        )

        activeNotifications[sbn.key] = notificationData

        // Save to database
        serviceScope.launch {
            try {
                val message = RecoveredMessage(
                    sender = sender,
                    content = text,
                    timestamp = sbn.postTime,
                    isDeleted = false,
                    packageName = packageName,
                    groupName = groupName
                )
                messageRepository.insertMessage(message)
                Log.d(TAG, "Saved message from $sender: $text")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving message", e)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn ?: return

        val packageName = sbn.packageName
        if (packageName !in WHATSAP_PACKAGES) return

        val removedData = activeNotifications.remove(sbn.key)

        if (removedData != null) {
            // A notification we tracked was removed - likely a message deletion
            serviceScope.launch {
                try {
                    messageRepository.markAsDeletedByContent(
                        sender = removedData.sender,
                        content = removedData.content,
                        timestamp = removedData.timestamp
                    )

                    // Check if any message was marked as deleted
                    val matchingMsg = messageRepository.findMessageByContent(
                        removedData.sender,
                        removedData.content
                    )

                    if (matchingMsg?.isDeleted == true || matchingMsg == null) {
                        showDeletedMessageNotification(removedData.sender, removedData.groupName)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error marking message as deleted", e)
                }
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "Notification listener disconnected")
    }

    private fun extractSenderAndGroup(title: String): Pair<String, String?> {
        // Handle group messages: "Sender: Group Name" or "Sender in Group Name"
        val groupDelimiters = listOf(": ", " in ", " @ ")
        for (delimiter in groupDelimiters) {
            val index = title.lastIndexOf(delimiter)
            if (index > 0) {
                val sender = title.substring(0, index).trim()
                val group = title.substring(index + delimiter.length).trim()
                return Pair(sender, group)
            }
        }
        return Pair(title.trim(), null)
    }

    private fun showDeletedMessageNotification(sender: String, groupName: String?) {
        val displayName = groupName?.let { "$sender in $it" } ?: sender

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "messages")
            putExtra("filter_deleted", true)
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, pendingIntentFlags
        )

        val notification = NotificationCompat.Builder(this, Constants.CHANNEL_ID_DELETED_MESSAGES)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(getString(R.string.message_deleted_notification))
            .setContentText(getString(R.string.message_deleted_content, displayName))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(
            Constants.NOTIFICATION_ID_DELETED + sender.hashCode(),
            notification
        )
    }

    private data class NotificationData(
        val key: String,
        val sender: String,
        val content: String,
        val timestamp: Long,
        val packageName: String,
        val groupName: String? = null
    )

    companion object {
        private const val TAG = "DeletedMessageService"
        private val WHATSAP_PACKAGES = setOf(
            Constants.WHATSAPP_PACKAGE,
            Constants.WHATSAPP_BUSINESS_PACKAGE
        )
    }
}
