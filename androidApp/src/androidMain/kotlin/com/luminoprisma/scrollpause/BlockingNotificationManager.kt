package com.luminoprisma.scrollpause

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class BlockingNotificationManager(private val context: Context) {
    
    companion object {
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "blocking_notifications"
        private const val CHANNEL_NAME = "App Blocking Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications when blocked apps are accessed"
    }
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                setShowBadge(true)
            }
            
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun showPersistentBlockingNotification(trackedApps: List<String>, timeLimit: Int) {
        val intent = Intent(context, PauseActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("message", "Take a mindful pause - you've reached your time limit of ${timeLimit} minutes")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("ScrollPause - Apps Blocked")
            .setContentText("${trackedApps.size} apps are blocked. Tap to take a mindful pause.")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Makes it persistent
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
        println("DEBUG: BlockingNotificationManager - Showed persistent blocking notification")
    }
    
    fun showBlockedAppNotification(appName: String, timeLimit: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("action", "show_dashboard")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val message = if (timeLimit > 0) {
            "You've reached your ${timeLimit}-minute limit for $appName. Take a mindful break!"
        } else {
            "$appName is currently blocked. Take a mindful break!"
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("App Blocked")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Open ScrollPause",
                pendingIntent
            )
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
        println("DEBUG: BlockingNotificationManager - Showed blocked app notification for $appName")
    }
    
    fun showAccessibilityDisabledNotification() {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("action", "enable_accessibility")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("ScrollPause Monitoring Disabled")
            .setContentText("Accessibility service was disabled. Tap to re-enable monitoring.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Re-enable",
                pendingIntent
            )
            .build()
        
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
        println("DEBUG: BlockingNotificationManager - Showed accessibility disabled notification")
    }
    
    fun showUsageAccessDisabledNotification() {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("action", "enable_usage_access")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("ScrollPause Monitoring Disabled")
            .setContentText("Usage access was disabled. Tap to re-enable monitoring.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Re-enable",
                pendingIntent
            )
            .build()
        
        notificationManager.notify(NOTIFICATION_ID + 2, notification)
        println("DEBUG: BlockingNotificationManager - Showed usage access disabled notification")
    }
    
    fun clearPersistentBlockingNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
        println("DEBUG: BlockingNotificationManager - Cleared persistent blocking notification")
    }
    
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
        println("DEBUG: BlockingNotificationManager - Cancelled all notifications")
    }
}
