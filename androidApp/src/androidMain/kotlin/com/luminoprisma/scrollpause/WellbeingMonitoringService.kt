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

class WellbeingMonitoringService(private val context: Context) {
    
    companion object {
        private const val TAG = "WellbeingMonitoringService"
        private const val PREFS_NAME = "scrollpause_wellbeing_monitoring"
        private const val KEY_IS_MONITORING = "is_monitoring"
        private const val KEY_TRACKED_APPS = "tracked_apps"
        private const val KEY_TIME_LIMIT = "time_limit"
        private const val KEY_MONITORING_START_TIME = "monitoring_start_time"
        private const val NOTIFICATION_ID = 3001
        private const val CHANNEL_ID = "wellbeing_monitoring_channel"
        
        private var instance: WellbeingMonitoringService? = null
        
        fun getInstance(context: Context): WellbeingMonitoringService {
            if (instance == null) {
                instance = WellbeingMonitoringService(context.applicationContext)
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
                "Digital Wellbeing Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for mindful break reminders and time limits"
                enableVibration(true)
                setShowBadge(true)
            }
            
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun startMonitoring(trackedApps: List<String>, timeLimit: Int) {
        Log.d(TAG, "Starting wellbeing monitoring for ${trackedApps.size} apps with ${timeLimit}min limit")
        
        prefs.edit().apply {
            putBoolean(KEY_IS_MONITORING, true)
            putString(KEY_TRACKED_APPS, trackedApps.joinToString(","))
            putInt(KEY_TIME_LIMIT, timeLimit)
            putLong(KEY_MONITORING_START_TIME, System.currentTimeMillis())
            apply()
        }
        
        showMonitoringNotification(trackedApps, timeLimit)
    }
    
    fun stopMonitoring() {
        Log.d(TAG, "Stopping wellbeing monitoring")
        
        prefs.edit().apply {
            putBoolean(KEY_IS_MONITORING, false)
            remove(KEY_TRACKED_APPS)
            remove(KEY_TIME_LIMIT)
            remove(KEY_MONITORING_START_TIME)
            apply()
        }
        
        notificationManager.cancel(NOTIFICATION_ID)
    }
    
    fun isMonitoring(): Boolean {
        return prefs.getBoolean(KEY_IS_MONITORING, false)
    }
    
    fun getTrackedApps(): List<String> {
        val appsString = prefs.getString(KEY_TRACKED_APPS, "")
        return if (appsString.isNullOrEmpty()) {
            emptyList()
        } else {
            appsString.split(",").filter { it.isNotBlank() }
        }
    }
    
    fun getTimeLimit(): Int {
        return prefs.getInt(KEY_TIME_LIMIT, 0)
    }
    
    fun getMonitoringStartTime(): Long {
        return prefs.getLong(KEY_MONITORING_START_TIME, 0L)
    }
    
    private fun showMonitoringNotification(trackedApps: List<String>, timeLimit: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("action", "show_dashboard")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("ScrollPause - Digital Wellbeing Active")
            .setContentText("Monitoring ${trackedApps.size} apps for mindful breaks (${timeLimit}min limit)")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    fun showTimeLimitReachedNotification(appName: String, timeLimit: Int) {
        val intent = Intent(context, PauseActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("message", "Take a mindful pause - you've reached your time limit of ${timeLimit} minutes")
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
            .setContentTitle("Time for a Mindful Break")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Take a Break",
                pendingIntent
            )
            .build()
        
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }
    
    fun clearAllNotifications() {
        notificationManager.cancelAll()
    }
}
