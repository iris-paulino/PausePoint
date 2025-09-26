import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
// Using simple JSON string manipulation instead of kotlinx.serialization
import platform.Foundation.NSUserDefaults

class IOSAppStorage : AppStorage {
    private val userDefaults = NSUserDefaults.standardUserDefaults
    private val onboardingCompletedKey = "onboarding_completed"
    private val qrCodesKey = "saved_qr_codes"
    private val selectedAppPackagesKey = "selected_app_packages"
    private val timeLimitMinutesKey = "time_limit_minutes"
    private val notificationsEnabledKey = "notifications_enabled"
    private val trackingStateKey = "tracking_state"
    private val appUsageTimesKey = "app_usage_times"
    private val trackingStartTimeKey = "tracking_start_time"
    private val usageAccessAllowedKey = "usage_access_allowed"
    private val accessibilityAccessAllowedKey = "accessibility_access_allowed"
    private val usageDayEpochKey = "usage_day_epoch"
    private val blockedStateKey = "blocked_state"
    private val timesUnblockedTodayKey = "times_unblocked_today"
    private val timesDismissedTodayKey = "times_dismissed_today"
    private val sessionAppUsageTimesKey = "session_app_usage_times"
    private val sessionStartTimeKey = "session_start_time"
    // Simple JSON serialization without external dependencies
    
    override suspend fun isOnboardingCompleted(): Boolean {
        return userDefaults.boolForKey(onboardingCompletedKey)
    }
    
    override suspend fun setOnboardingCompleted(completed: Boolean) {
        userDefaults.setBool(completed, onboardingCompletedKey)
        userDefaults.synchronize()
    }
    
    override fun isOnboardingCompletedFlow(): Flow<Boolean> {
        // For simplicity, we'll return a flow with the current value
        // In a more sophisticated implementation, you might want to use KVO or similar
        return flowOf(userDefaults.boolForKey(onboardingCompletedKey))
    }
    
    override suspend fun saveNotificationsEnabled(enabled: Boolean) {
        userDefaults.setBool(enabled, notificationsEnabledKey)
        userDefaults.synchronize()
    }

    override suspend fun getNotificationsEnabled(): Boolean {
        // Default to false if unset; boolForKey defaults to false when absent
        val exists = userDefaults.objectForKey(notificationsEnabledKey) != null
        return if (!exists) false else userDefaults.boolForKey(notificationsEnabledKey)
    }

    override suspend fun saveQrCode(qrCode: SavedQrCode) {
        try {
            val existingCodes = getSavedQrCodes().toMutableList()
            // Remove existing code with same ID if it exists
            existingCodes.removeAll { it.id == qrCode.id }
            // Add the new code
            existingCodes.add(qrCode)
            
            val jsonString = serializeQrCodes(existingCodes)
            userDefaults.setObject(jsonString, qrCodesKey)
            userDefaults.synchronize()
        } catch (e: Exception) {
            // Silently fail if there's an error saving
        }
    }
    
    override suspend fun getSavedQrCodes(): List<SavedQrCode> {
        return try {
            val jsonString = userDefaults.stringForKey(qrCodesKey)
            if (jsonString != null) {
                deserializeQrCodes(jsonString)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    override suspend fun getQrCodeById(id: String): SavedQrCode? {
        return getSavedQrCodes().find { it.id == id }
    }
    
    override suspend fun validateQrCode(scannedText: String): SavedQrCode? {
        return getSavedQrCodes().find { it.qrText == scannedText && it.isActive }
    }
    
    override suspend fun removeQrCode(id: String) {
        try {
            val existingCodes = getSavedQrCodes().toMutableList()
            existingCodes.removeAll { it.id == id }
            
            val jsonString = serializeQrCodes(existingCodes)
            userDefaults.setObject(jsonString, qrCodesKey)
            userDefaults.synchronize()
        } catch (e: Exception) {
            // Silently fail if there's an error removing
        }
    }

    override suspend fun saveSelectedAppPackages(packageNames: List<String>) {
        try {
            val csv = packageNames.joinToString(",")
            userDefaults.setObject(csv, selectedAppPackagesKey)
            userDefaults.synchronize()
        } catch (e: Exception) {
            // ignore
        }
    }

    override suspend fun getSelectedAppPackages(): List<String> {
        return try {
            val csv = userDefaults.stringForKey(selectedAppPackagesKey)
            if (csv.isNullOrBlank()) emptyList() else csv.split(',').filter { it.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun saveTimeLimitMinutes(minutes: Int) {
        try {
            userDefaults.setInteger(minutes.toLong(), timeLimitMinutesKey)
            userDefaults.synchronize()
        } catch (e: Exception) {
            // ignore
        }
    }

    override suspend fun getTimeLimitMinutes(): Int {
        return try {
            val value = userDefaults.integerForKey(timeLimitMinutesKey)
            if (value == 0L) 15 else value.toInt()
        } catch (e: Exception) {
            15
        }
    }

    override suspend fun saveTrackingState(isTracking: Boolean) {
        try {
            userDefaults.setBool(isTracking, trackingStateKey)
            userDefaults.synchronize()
        } catch (e: Exception) {
            // ignore
        }
    }

    override suspend fun getTrackingState(): Boolean {
        return try {
            userDefaults.boolForKey(trackingStateKey)
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun saveAppUsageTimes(usageTimes: Map<String, Long>) {
        try {
            val jsonString = serializeUsageTimes(usageTimes)
            userDefaults.setObject(jsonString, appUsageTimesKey)
            userDefaults.synchronize()
        } catch (e: Exception) {
            // ignore
        }
    }

    override suspend fun getAppUsageTimes(): Map<String, Long> {
        return try {
            val jsonString = userDefaults.stringForKey(appUsageTimesKey)
            if (jsonString != null) {
                deserializeUsageTimes(jsonString)
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    override suspend fun saveTrackingStartTime(startTime: Long) {
        try {
            userDefaults.setDouble(startTime.toDouble(), trackingStartTimeKey)
            userDefaults.synchronize()
        } catch (e: Exception) {
            // ignore
        }
    }

    override suspend fun getTrackingStartTime(): Long {
        return try {
            userDefaults.doubleForKey(trackingStartTimeKey).toLong()
        } catch (e: Exception) {
            0L
        }
    }

    override suspend fun saveUsageAccessAllowed(allowed: Boolean) {
        userDefaults.setBool(allowed, usageAccessAllowedKey)
        userDefaults.synchronize()
    }

    override suspend fun getUsageAccessAllowed(): Boolean {
        val exists = userDefaults.objectForKey(usageAccessAllowedKey) != null
        return if (!exists) false else userDefaults.boolForKey(usageAccessAllowedKey)
    }

    override suspend fun saveAccessibilityAccessAllowed(allowed: Boolean) {
        userDefaults.setBool(allowed, accessibilityAccessAllowedKey)
        userDefaults.synchronize()
    }

    override suspend fun getAccessibilityAccessAllowed(): Boolean {
        val exists = userDefaults.objectForKey(accessibilityAccessAllowedKey) != null
        return if (!exists) false else userDefaults.boolForKey(accessibilityAccessAllowedKey)
    }
    
    // Simple JSON serialization functions
    private fun serializeQrCodes(codes: List<SavedQrCode>): String {
        val jsonArray = codes.joinToString(",", "[", "]") { code ->
            """{"id":"${code.id}","qrText":"${code.qrText}","message":"${code.message}","createdAt":${code.createdAt},"isActive":${code.isActive}}"""
        }
        return jsonArray
    }
    
    private fun deserializeQrCodes(jsonString: String): List<SavedQrCode> {
        if (jsonString.isEmpty() || jsonString == "[]") return emptyList()
        
        val codes = mutableListOf<SavedQrCode>()
        val cleanJson = jsonString.removePrefix("[").removeSuffix("]")
        
        if (cleanJson.isEmpty()) return emptyList()
        
        // Simple parsing - split by },{ and parse each object
        val objects = cleanJson.split("},{")
        for (obj in objects) {
            val cleanObj = obj.removePrefix("{").removeSuffix("}")
            val fields = cleanObj.split(",")
            
            var id = ""
            var qrText = ""
            var message = ""
            var createdAt = 0L
            var isActive = true
            
            for (field in fields) {
                val parts = field.split(":", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim().removeSurrounding("\"")
                    val value = parts[1].trim()
                    
                    when (key) {
                        "id" -> id = value.removeSurrounding("\"")
                        "qrText" -> qrText = value.removeSurrounding("\"")
                        "message" -> message = value.removeSurrounding("\"")
                        "createdAt" -> createdAt = value.toLongOrNull() ?: 0L
                        "isActive" -> isActive = value.toBoolean()
                    }
                }
            }
            
            if (id.isNotEmpty()) {
                codes.add(SavedQrCode(id, qrText, message, createdAt, isActive))
            }
        }
        
        return codes
    }

    // Serialization functions for usage times
    private fun serializeUsageTimes(usageTimes: Map<String, Long>): String {
        val jsonArray = usageTimes.entries.joinToString(",", "[", "]") { entry ->
            """{"app":"${entry.key}","seconds":${entry.value}}"""
        }
        return jsonArray
    }
    
    private fun deserializeUsageTimes(jsonString: String): Map<String, Long> {
        if (jsonString.isEmpty() || jsonString == "[]") return emptyMap()
        
        val usageTimes = mutableMapOf<String, Long>()
        val cleanJson = jsonString.removePrefix("[").removeSuffix("]")
        
        if (cleanJson.isEmpty()) return emptyMap()
        
        // Simple parsing - split by },{ and parse each object
        val objects = cleanJson.split("},{")
        for (obj in objects) {
            val cleanObj = obj.removePrefix("{").removeSuffix("}")
            val fields = cleanObj.split(",")
            
            var app = ""
            var seconds = 0L
            
            for (field in fields) {
                val parts = field.split(":", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim().removeSurrounding("\"")
                    val value = parts[1].trim()
                    
                    when (key) {
                        "app" -> app = value.removeSurrounding("\"")
                        "seconds" -> seconds = value.toLongOrNull() ?: 0L
                    }
                }
            }
            
            if (app.isNotEmpty()) {
                usageTimes[app] = seconds
            }
        }
        
        return usageTimes
    }

    override suspend fun saveBlockedState(isBlocked: Boolean) {
        try {
            userDefaults.setBool(isBlocked, blockedStateKey)
            userDefaults.synchronize()
        } catch (e: Exception) {
            // ignore
        }
    }

    override suspend fun getBlockedState(): Boolean {
        return try {
            userDefaults.boolForKey(blockedStateKey)
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun saveUsageDayEpoch(epochDay: Long) {
        try {
            userDefaults.setDouble(epochDay.toDouble(), usageDayEpochKey)
            userDefaults.synchronize()
        } catch (e: Exception) {
            // ignore
        }
    }

    override suspend fun getUsageDayEpoch(): Long {
        return try {
            userDefaults.doubleForKey(usageDayEpochKey).toLong()
        } catch (e: Exception) {
            0L
        }
    }

    override suspend fun saveTimesUnblockedToday(count: Int) {
        try {
            userDefaults.setInteger(count.toLong(), timesUnblockedTodayKey)
            userDefaults.synchronize()
        } catch (e: Exception) {
            // ignore
        }
    }

    override suspend fun getTimesUnblockedToday(): Int {
        return try {
            userDefaults.integerForKey(timesUnblockedTodayKey).toInt()
        } catch (e: Exception) {
            0
        }
    }

    override suspend fun saveTimesDismissedToday(count: Int) {
        try {
            userDefaults.setInteger(count.toLong(), timesDismissedTodayKey)
            userDefaults.synchronize()
        } catch (e: Exception) {
            // ignore
        }
    }

    override suspend fun getTimesDismissedToday(): Int {
        return try {
            userDefaults.integerForKey(timesDismissedTodayKey).toInt()
        } catch (e: Exception) {
            0
        }
    }

    override suspend fun saveSessionAppUsageTimes(usageTimes: Map<String, Long>) {
        try {
            val jsonString = serializeUsageTimes(usageTimes)
            userDefaults.setObject(jsonString, sessionAppUsageTimesKey)
            userDefaults.synchronize()
        } catch (e: Exception) {
            // ignore
        }
    }

    override suspend fun getSessionAppUsageTimes(): Map<String, Long> {
        return try {
            val jsonString = userDefaults.stringForKey(sessionAppUsageTimesKey)
            if (jsonString != null) {
                deserializeUsageTimes(jsonString)
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    override suspend fun saveSessionStartTime(startTime: Long) {
        try {
            userDefaults.setDouble(startTime.toDouble(), sessionStartTimeKey)
            userDefaults.synchronize()
        } catch (e: Exception) {
            // ignore
        }
    }

    override suspend fun getSessionStartTime(): Long {
        return try {
            userDefaults.doubleForKey(sessionStartTimeKey).toLong()
        } catch (e: Exception) {
            0L
        }
    }
}

actual fun createAppStorage(): AppStorage {
    return IOSAppStorage()
}
