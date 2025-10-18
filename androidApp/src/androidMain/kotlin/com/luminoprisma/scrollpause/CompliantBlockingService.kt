package com.luminoprisma.scrollpause

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

class CompliantBlockingService(private val context: Context) {
    
    companion object {
        private const val TAG = "CompliantBlockingService"
        private const val PREFS_NAME = "scrollpause_compliant_blocking"
        private const val KEY_IS_BLOCKING = "is_blocking"
        private const val KEY_BLOCKED_APPS = "blocked_apps"
        private const val KEY_TIME_LIMIT = "time_limit"
        private const val KEY_BLOCKING_START_TIME = "blocking_start_time"
        private const val NOTIFICATION_ID = 3001
        private const val CHANNEL_ID = "compliant_blocking_channel"
        
        private var instance: CompliantBlockingService? = null
        
        fun getInstance(context: Context): CompliantBlockingService {
            if (instance == null) {
                instance = CompliantBlockingService(context.applicationContext)
            }
            return instance!!
        }
    }
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Blocking Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when apps are blocked"
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun startBlocking(trackedApps: List<String>, timeLimit: Int) {
        try {
            // Save blocking state
            prefs.edit().apply {
                putBoolean(KEY_IS_BLOCKING, true)
                putStringSet(KEY_BLOCKED_APPS, trackedApps.toSet())
                putInt(KEY_TIME_LIMIT, timeLimit)
                putLong(KEY_BLOCKING_START_TIME, System.currentTimeMillis())
                apply()
            }
            
            Log.d(TAG, "Started compliant blocking for ${trackedApps.size} apps")
            
            // Show persistent notification with blocking info
            showBlockingNotification(trackedApps, timeLimit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting compliant blocking: ${e.message}")
        }
    }
    
    fun stopBlocking() {
        try {
            // Clear blocking state
            prefs.edit().apply {
                putBoolean(KEY_IS_BLOCKING, false)
                putStringSet(KEY_BLOCKED_APPS, emptySet())
                putInt(KEY_TIME_LIMIT, 0)
                remove(KEY_BLOCKING_START_TIME)
                apply()
            }
            
            // Hide blocking notification
            notificationManager.cancel(NOTIFICATION_ID)
            
            Log.d(TAG, "Stopped compliant blocking")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping compliant blocking: ${e.message}")
        }
    }
    
    fun isBlocking(): Boolean {
        return prefs.getBoolean(KEY_IS_BLOCKING, false)
    }
    
    fun getBlockedApps(): List<String> {
        return prefs.getStringSet(KEY_BLOCKED_APPS, emptySet())?.toList() ?: emptyList()
    }
    
    fun getTimeLimit(): Int {
        return prefs.getInt(KEY_TIME_LIMIT, 0)
    }
    
    fun isBlockingExpired(): Boolean {
        val blockingStartTime = prefs.getLong(KEY_BLOCKING_START_TIME, 0L)
        val timeLimit = prefs.getInt(KEY_TIME_LIMIT, 0)
        val currentTime = System.currentTimeMillis()
        val blockingDuration = currentTime - blockingStartTime
        val timeLimitMs = timeLimit * 60 * 1000L
        
        return blockingDuration >= timeLimitMs
    }
    
    private fun showBlockingNotification(trackedApps: List<String>, timeLimit: Int) {
        try {
            // Main intent to launch Pause Screen
            val pauseIntent = Intent(context, PauseActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra("message", "Take a mindful pause - you've reached your time limit of ${timeLimit} minutes")
            }
            
            val pausePendingIntent = PendingIntent.getActivity(
                context, 0, pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Intent to open main app
            val appIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            
            val appPendingIntent = PendingIntent.getActivity(
                context, 1, appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("🛑 ScrollPause - Apps Blocked")
                .setContentText("${trackedApps.size} apps are blocked. Tap to take a mindful pause.")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentIntent(pausePendingIntent) // Default action launches Pause Screen
                .setOngoing(true) // Makes it persistent
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("🛑 The following apps are blocked: ${trackedApps.joinToString(", ")}\n\n⏰ Time limit: ${timeLimit} minutes\n\n💡 Tap to take a mindful pause and reflect on your usage."))
                // Add action buttons
                .addAction(
                    android.R.drawable.ic_media_pause,
                    "Take Pause",
                    pausePendingIntent
                )
                .addAction(
                    android.R.drawable.ic_menu_manage,
                    "Open App",
                    appPendingIntent
                )
                .setColor(android.graphics.Color.RED)
                .setLights(android.graphics.Color.RED, 1000, 1000)
                .setVibrate(longArrayOf(0, 500, 200, 500))
                .build()
            
            notificationManager.notify(NOTIFICATION_ID, notification)
            Log.d(TAG, "Showed enhanced blocking notification for ${trackedApps.size} apps")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error showing blocking notification: ${e.message}")
        }
    }
    
    fun showAppUsageReminder(appName: String) {
        try {
            val intent = Intent(context, PauseActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra("message", "You're trying to use $appName, but it's currently blocked. Take a mindful pause!")
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context, 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("App Blocked - $appName")
                .setContentText("This app is currently blocked. Tap to take a mindful pause.")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .build()
            
            notificationManager.notify(NOTIFICATION_ID + 1, notification)
            Log.d(TAG, "Showed app usage reminder for $appName")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error showing app usage reminder: ${e.message}")
        }
    }
}
