package com.luminoprisma.scrollpause

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.*

class AppRestartDetector(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "app_restart_detector"
        private const val KEY_LAST_APP_START = "last_app_start_time"
        private const val KEY_WAS_TRACKING = "was_tracking_before_restart"
        private const val KEY_WAS_BLOCKED = "was_blocked_before_restart"
        private const val KEY_TRACKED_APPS = "tracked_apps_before_restart"
        private const val KEY_TIME_LIMIT = "time_limit_before_restart"
        private const val RESTART_THRESHOLD_MS = 30_000L // 30 seconds
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    fun onAppStart() {
        val currentTime = System.currentTimeMillis()
        val lastStartTime = prefs.getLong(KEY_LAST_APP_START, 0L)
        
        // Save current start time
        prefs.edit().putLong(KEY_LAST_APP_START, currentTime).apply()
        
        // Check if this is a restart (app was killed and restarted)
        val isRestart = lastStartTime > 0 && (currentTime - lastStartTime) > RESTART_THRESHOLD_MS
        
        if (isRestart) {
            println("DEBUG: AppRestartDetector - Detected app restart")
            handleAppRestart()
        } else {
            println("DEBUG: AppRestartDetector - Normal app start")
        }
    }
    
    fun saveTrackingState(isTracking: Boolean, isBlocked: Boolean, trackedApps: List<String>, timeLimit: Int) {
        prefs.edit().apply {
            putBoolean(KEY_WAS_TRACKING, isTracking)
            putBoolean(KEY_WAS_BLOCKED, isBlocked)
            putString(KEY_TRACKED_APPS, trackedApps.joinToString(","))
            putInt(KEY_TIME_LIMIT, timeLimit)
            apply()
        }
        println("DEBUG: AppRestartDetector - Saved tracking state: tracking=$isTracking, blocked=$isBlocked")
    }
    
    private fun handleAppRestart() {
        scope.launch {
            try {
                val wasTracking = prefs.getBoolean(KEY_WAS_TRACKING, false)
                val wasBlocked = prefs.getBoolean(KEY_WAS_BLOCKED, false)
                val trackedAppsCsv = prefs.getString(KEY_TRACKED_APPS, "") ?: ""
                val timeLimit = prefs.getInt(KEY_TIME_LIMIT, 0)
                
                val trackedApps = if (trackedAppsCsv.isBlank()) {
                    emptyList()
                } else {
                    trackedAppsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                }
                
                println("DEBUG: AppRestartDetector - Restart state: wasTracking=$wasTracking, wasBlocked=$wasBlocked, apps=$trackedApps")
                
                if (wasTracking) {
                    // Auto-resume tracking after restart
                    autoResumeTracking(wasBlocked, trackedApps, timeLimit)
                }
                
                // Clear saved state after handling restart
                clearSavedState()
                
            } catch (e: Exception) {
                println("DEBUG: AppRestartDetector - Error handling restart: ${e.message}")
            }
        }
    }
    
    private suspend fun autoResumeTracking(wasBlocked: Boolean, trackedApps: List<String>, timeLimit: Int) {
        try {
            // Start the foreground service to maintain monitoring
            AppMonitoringForegroundService.startService(context)
            
            // Update accessibility service with previous state
            ForegroundAppAccessibilityService.setBlockedState(wasBlocked, trackedApps, timeLimit)
            
            // Show notification to inform user about auto-resume
            val notificationManager = WellbeingNotificationManager(context)
            if (wasBlocked) {
                notificationManager.showTimeLimitReachedNotification("tracked apps", timeLimit)
            }
            
            println("DEBUG: AppRestartDetector - Auto-resumed tracking: blocked=$wasBlocked, apps=${trackedApps.size}")
            
        } catch (e: Exception) {
            println("DEBUG: AppRestartDetector - Error auto-resuming tracking: ${e.message}")
        }
    }
    
    private fun clearSavedState() {
        prefs.edit().apply {
            remove(KEY_WAS_TRACKING)
            remove(KEY_WAS_BLOCKED)
            remove(KEY_TRACKED_APPS)
            remove(KEY_TIME_LIMIT)
            apply()
        }
        println("DEBUG: AppRestartDetector - Cleared saved state")
    }
    
    fun cleanup() {
        scope.cancel()
    }
}
