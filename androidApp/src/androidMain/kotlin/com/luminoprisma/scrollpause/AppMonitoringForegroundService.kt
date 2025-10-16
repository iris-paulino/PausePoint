package com.luminoprisma.scrollpause

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class AppMonitoringForegroundService : Service() {
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "app_monitoring_channel"
        private const val CHANNEL_NAME = "App Monitoring"
        
        private var isServiceRunning = false
        
        fun startService(context: Context) {
            if (!isServiceRunning) {
                val intent = Intent(context, AppMonitoringForegroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, AppMonitoringForegroundService::class.java)
            context.stopService(intent)
        }
        
        fun isRunning(): Boolean = isServiceRunning
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        isServiceRunning = true
        println("DEBUG: AppMonitoringForegroundService - Service created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        println("DEBUG: AppMonitoringForegroundService - Service started")
        
        // Start periodic data persistence
        startPeriodicDataPersistence()
        
        // Start accessibility service monitoring
        startAccessibilityServiceMonitoring()
        
        // Start service health monitoring
        startServiceHealthMonitoring()
        
        return START_STICKY // Restart if killed by system
    }
    
    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        serviceScope.cancel()
        println("DEBUG: AppMonitoringForegroundService - Service destroyed")
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors app usage and maintains data integrity"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🛡️ ScrollPause Active")
            .setContentText("Protecting your digital wellness - monitoring app usage")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("ScrollPause is actively monitoring your app usage to help you maintain healthy digital habits. This service ensures your time limits are respected even if the main app is closed."))
            .setColor(android.graphics.Color.parseColor("#6200EE"))
            .build()
    }
    
    
    private fun startPeriodicDataPersistence() {
        serviceScope.launch {
            while (isServiceRunning) {
                try {
                    // Persist data from accessibility service every 30 seconds
                    persistAccessibilityServiceData()
                    delay(30_000) // 30 seconds
                } catch (e: Exception) {
                    println("DEBUG: Error in periodic data persistence: ${e.message}")
                    delay(60_000) // Wait longer on error
                }
            }
        }
    }
    
    private suspend fun persistAccessibilityServiceData() {
        try {
            // Get current state from accessibility service
            val currentApp = ForegroundAppAccessibilityService.getCurrentForegroundPackage()
            val isBlocked = ForegroundAppAccessibilityService.isBlocked()
            
            // Save to shared preferences for persistence
            val prefs = getSharedPreferences("scrollpause_monitoring", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString("last_foreground_app", currentApp)
                putBoolean("is_blocked", isBlocked)
                putLong("last_persist_time", System.currentTimeMillis())
                apply()
            }
            
            println("DEBUG: Persisted monitoring data - app: $currentApp, blocked: $isBlocked")
        } catch (e: Exception) {
            println("DEBUG: Error persisting accessibility service data: ${e.message}")
        }
    }
    
    private fun startAccessibilityServiceMonitoring() {
        serviceScope.launch {
            while (isServiceRunning) {
                try {
                    // Check if accessibility service is running and restore state if needed
                    checkAndRestoreAccessibilityService()
                    
                    // Check if we need to show blocking overlay
                    checkAndShowBlockingOverlay()
                    
                    delay(3000) // Check every 3 seconds for responsive blocking
                } catch (e: Exception) {
                    println("DEBUG: Error in accessibility service monitoring: ${e.message}")
                    delay(5000) // Wait longer on error
                }
            }
        }
    }
    
    private suspend fun checkAndRestoreAccessibilityService() {
        try {
            // Check if accessibility service is running
            val isAccessibilityEnabled = isAccessibilityServiceEnabled()
            
            if (!isAccessibilityEnabled) {
                println("DEBUG: Accessibility service is not enabled, cannot maintain blocking")
                return
            }
            
            // Check if we have a blocked state that needs to be restored
            val prefs = getSharedPreferences("scrollpause_prefs", Context.MODE_PRIVATE)
            val isBlocked = prefs.getBoolean("blocked", false)
            val trackedAppsCsv = prefs.getString("tracked_apps_csv", "") ?: ""
            val timeLimit = prefs.getInt("time_limit_minutes", 0)
            
            if (isBlocked && trackedAppsCsv.isNotEmpty()) {
                val trackedApps = trackedAppsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                println("DEBUG: Found blocked state to restore - apps: $trackedApps, limit: $timeLimit")
                
                // Restore the blocking state in accessibility service
                ForegroundAppAccessibilityService.setBlockedState(isBlocked, trackedApps, timeLimit)
                
                // Check if user is currently in a tracked app and show Pause screen
                val currentApp = ForegroundAppAccessibilityService.getCurrentForegroundPackage()
                if (isPackageTracked(currentApp, trackedApps)) {
                    println("DEBUG: User in tracked app, showing Pause screen")
                    showPauseScreen("Take a mindful pause - you've reached your time limit of ${timeLimit} minutes")
                }
            }
        } catch (e: Exception) {
            println("DEBUG: Error checking accessibility service: ${e.message}")
        }
    }
    
    private suspend fun checkAndShowBlockingOverlay() {
        try {
            val prefs = getSharedPreferences("scrollpause_prefs", Context.MODE_PRIVATE)
            val isBlocked = prefs.getBoolean("blocked", false)
            val trackedAppsCsv = prefs.getString("tracked_apps_csv", "") ?: ""
            
            if (isBlocked && trackedAppsCsv.isNotEmpty()) {
                val trackedApps = trackedAppsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val currentApp = ForegroundAppAccessibilityService.getCurrentForegroundPackage()
                
                if (isPackageTracked(currentApp, trackedApps)) {
                    // User is in a tracked app while blocked, ensure Pause screen is shown
                    val timeLimit = prefs.getInt("time_limit_minutes", 0)
                    showPauseScreen("Take a mindful pause - you've reached your time limit of ${timeLimit} minutes")
                }
            }
        } catch (e: Exception) {
            println("DEBUG: Error checking blocking overlay: ${e.message}")
        }
    }
    
    private fun isAccessibilityServiceEnabled(): Boolean {
        return try {
            // Simple check - if we can get the current foreground package, accessibility service is working
            val currentApp = ForegroundAppAccessibilityService.getCurrentForegroundPackage()
            currentApp != null
        } catch (e: Exception) {
            println("DEBUG: Error checking accessibility service: ${e.message}")
            false
        }
    }
    
    private fun isPackageTracked(packageName: String?, trackedApps: List<String>): Boolean {
        if (packageName.isNullOrBlank()) return false
        return trackedApps.any { appName ->
            val normalized = appName.trim()
            if (normalized.contains('.')) {
                packageName == normalized
            } else {
                when (normalized.lowercase()) {
                    "chrome" -> packageName == "com.android.chrome"
                    "youtube" -> packageName == "com.google.android.youtube"
                    "messages" -> packageName == "com.google.android.apps.messaging"
                    "gmail" -> packageName == "com.google.android.gm"
                    "whatsapp" -> packageName == "com.whatsapp"
                    "youtube music" -> packageName == "com.google.android.apps.youtube.music"
                    else -> packageName.contains(normalized.lowercase())
                }
            }
        }
    }
    
    private fun showPauseScreen(message: String) {
        try {
            val intent = Intent(this, PauseOverlayActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("message", message)
            }
            startActivity(intent)
            println("DEBUG: Launched Pause screen from foreground service")
        } catch (e: Exception) {
            println("DEBUG: Error launching Pause screen: ${e.message}")
        }
    }
    
    private fun startServiceHealthMonitoring() {
        serviceScope.launch {
            while (isServiceRunning) {
                try {
                    // Update notification with current status
                    updateNotificationWithStatus()
                    
                    // Check if we need to restart the service
                    checkServiceHealth()
                    
                    delay(25 * 60 * 1000L) // Check every 25 minutes
                } catch (e: Exception) {
                    println("DEBUG: Error in service health monitoring: ${e.message}")
                    delay(5 * 60 * 1000L) // Wait 5 minutes on error
                }
            }
        }
    }
    
    
    private fun updateNotificationWithStatus() {
        try {
            val prefs = getSharedPreferences("scrollpause_prefs", Context.MODE_PRIVATE)
            val isBlocked = prefs.getBoolean("blocked", false)
            val trackedAppsCsv = prefs.getString("tracked_apps_csv", "") ?: ""
            
            val statusText = if (isBlocked && trackedAppsCsv.isNotEmpty()) {
                "🛑 Apps are currently blocked - monitoring active"
            } else {
                "🛡️ Monitoring app usage - ready to enforce limits"
            }
            
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("🛡️ ScrollPause Active")
                .setContentText(statusText)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(PendingIntent.getActivity(
                    this, 0, Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                ))
                .setOngoing(true)
                .setSilent(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("ScrollPause is actively monitoring your app usage to help you maintain healthy digital habits. This service ensures your time limits are respected even if the main app is closed."))
                .setColor(android.graphics.Color.parseColor("#6200EE"))
                .build()
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
            
        } catch (e: Exception) {
            println("DEBUG: Error updating notification: ${e.message}")
        }
    }
    
    private fun checkServiceHealth() {
        try {
            // Check if accessibility service is still working
            val isAccessibilityWorking = isAccessibilityServiceEnabled()
            if (!isAccessibilityWorking) {
                println("DEBUG: Accessibility service not working, attempting to restore...")
                // The accessibility service monitoring will handle restoration
            }
            
            // Check if we're still in foreground
            if (!isServiceRunning) {
                println("DEBUG: Service health check failed - service not running")
                // Restart the service
                startService(Intent(this, AppMonitoringForegroundService::class.java))
            }
            
        } catch (e: Exception) {
            println("DEBUG: Error in service health check: ${e.message}")
        }
    }
    
}
