package com.luminoprisma.scrollpause

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class WellbeingNotificationManager(private val context: Context) {
    
    companion object {
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "wellbeing_notifications"
        private const val CHANNEL_NAME = "Digital Wellbeing Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for mindful break reminders and time limits"
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
    
    fun showPersistentWellbeingNotification(trackedApps: List<String>, timeLimit: Int) {
        val intent = Intent(context, PauseActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("message", "Take a mindful pause - you've reached your time limit of ${timeLimit} minutes")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("ScrollPause - Time for a Mindful Break")
            .setContentText("You've reached your time limit for ${trackedApps.size} apps. Tap to take a mindful pause.")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Makes it persistent
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
        println("DEBUG: WellbeingNotificationManager - Showed persistent wellbeing notification")
    }
    
    fun showTimeLimitReachedNotification(appName: String, timeLimit: Int) {
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
            "Time for a mindful break from $appName!"
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Time Limit Reached")
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
        println("DEBUG: WellbeingNotificationManager - Showed time limit reached notification for $appName")
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
        println("DEBUG: WellbeingNotificationManager - Showed accessibility disabled notification")
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
        println("DEBUG: WellbeingNotificationManager - Showed usage access disabled notification")
    }
    
    fun clearPersistentWellbeingNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
        println("DEBUG: WellbeingNotificationManager - Cleared persistent wellbeing notification")
    }
    
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
        println("DEBUG: WellbeingNotificationManager - Cancelled all notifications")
    }
}
