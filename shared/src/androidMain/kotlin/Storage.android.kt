import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
// Using simple JSON string manipulation instead of kotlinx.serialization

class AndroidAppStorage(private val context: Context) : AppStorage {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
    private val ONBOARDING_COMPLETED_KEY = "onboarding_completed"
    private val QR_CODES_KEY = "saved_qr_codes"
    private val SELECTED_APP_PACKAGES_KEY = "selected_app_packages"
    private val TIME_LIMIT_MINUTES_KEY = "time_limit_minutes"
    private val NOTIFICATIONS_ENABLED_KEY = "notifications_enabled"
    private val TRACKING_STATE_KEY = "tracking_state"
    private val APP_USAGE_TIMES_KEY = "app_usage_times"
    private val TRACKING_START_TIME_KEY = "tracking_start_time"
    private val USAGE_ACCESS_ALLOWED_KEY = "usage_access_allowed"
    private val ACCESSIBILITY_ACCESS_ALLOWED_KEY = "accessibility_access_allowed"
    private val USAGE_DAY_EPOCH_KEY = "usage_day_epoch"
    private val BLOCKED_STATE_KEY = "blocked_state"
    private val TIMES_UNBLOCKED_TODAY_KEY = "times_unblocked_today"
    private val TIMES_DISMISSED_TODAY_KEY = "times_dismissed_today"
    private val SESSION_APP_USAGE_TIMES_KEY = "session_app_usage_times"
    private val SESSION_START_TIME_KEY = "session_start_time"
    private val QR_GENERATOR_VISITED_KEY = "qr_generator_visited"
    private val DO_NOT_SHOW_CONGRATS_KEY = "do_not_show_congratulation_again"
    private val DO_NOT_SHOW_DISMISS_KEY = "do_not_show_dismiss_again"
    private val AUTO_RESTART_ON_DISMISS_KEY = "auto_restart_on_dismiss"
    private val DAY_STREAK_COUNTER_KEY = "day_streak_counter"
    private val LAST_STREAK_UPDATE_DAY_KEY = "last_streak_update_day"
    // Simple JSON serialization without external dependencies
    
    override suspend fun isOnboardingCompleted(): Boolean {
        return try {
            prefs.getBoolean(ONBOARDING_COMPLETED_KEY, false)
        } catch (e: Exception) {
            false // Default to not completed if there's an error
        }
    }
    
    override suspend fun setOnboardingCompleted(completed: Boolean) {
        try {
            prefs.edit().putBoolean(ONBOARDING_COMPLETED_KEY, completed).apply()
        } catch (e: Exception) {
            // Silently fail if there's an error saving
        }
    }
    
    override fun isOnboardingCompletedFlow(): Flow<Boolean> {
        return flowOf(prefs.getBoolean(ONBOARDING_COMPLETED_KEY, false))
    }
    
    override suspend fun saveNotificationsEnabled(enabled: Boolean) {
        try {
            prefs.edit().putBoolean(NOTIFICATIONS_ENABLED_KEY, enabled).apply()
        } catch (_: Exception) {}
    }

    override suspend fun getNotificationsEnabled(): Boolean {
        return try { prefs.getBoolean(NOTIFICATIONS_ENABLED_KEY, false) } catch (_: Exception) { false }
    }

    override suspend fun saveQrCode(qrCode: SavedQrCode) {
        try {
            val existingCodes = getSavedQrCodes().toMutableList()
            // Remove existing code with same ID if it exists
            existingCodes.removeAll { it.id == qrCode.id }
            // Add the new code
            existingCodes.add(qrCode)
            
            val jsonString = serializeQrCodes(existingCodes)
            prefs.edit().putString(QR_CODES_KEY, jsonString).apply()
        } catch (e: Exception) {
            // Silently fail if there's an error saving
        }
    }
    
    override suspend fun getSavedQrCodes(): List<SavedQrCode> {
        return try {
            val jsonString = prefs.getString(QR_CODES_KEY, null)
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
            prefs.edit().putString(QR_CODES_KEY, jsonString).apply()
        } catch (e: Exception) {
            // Silently fail if there's an error removing
        }
    }

    override suspend fun saveSelectedAppPackages(packageNames: List<String>) {
        try {
            val csv = packageNames.joinToString(",")
            prefs.edit().putString(SELECTED_APP_PACKAGES_KEY, csv).apply()
        } catch (e: Exception) {
            // ignore
        }
    }

    override suspend fun getSelectedAppPackages(): List<String> {
        return try {
            val csv = prefs.getString(SELECTED_APP_PACKAGES_KEY, null)
            if (csv.isNullOrBlank()) emptyList() else csv.split(',').filter { it.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun saveTimeLimitMinutes(minutes: Int) {
        try {
            prefs.edit().putInt(TIME_LIMIT_MINUTES_KEY, minutes).apply()
        } catch (e: Exception) {
            // ignore
        }
    }

    override suspend fun getTimeLimitMinutes(): Int {
        return try {
            prefs.getInt(TIME_LIMIT_MINUTES_KEY, 15)
        } catch (e: Exception) {
            15
        }
    }

    override suspend fun saveTrackingState(isTracking: Boolean) {
        try {
            prefs.edit().putBoolean(TRACKING_STATE_KEY, isTracking).apply()
        } catch (e: Exception) {
            // ignore
        }
    }

    override suspend fun getTrackingState(): Boolean {
        return try {
            prefs.getBoolean(TRACKING_STATE_KEY, false)
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun saveAppUsageTimes(usageTimes: Map<String, Long>) {
        try {
            val jsonString = serializeUsageTimes(usageTimes)
            prefs.edit().putString(APP_USAGE_TIMES_KEY, jsonString).apply()
        } catch (e: Exception) {
            // ignore
        }
    }

    override suspend fun getAppUsageTimes(): Map<String, Long> {
        return try {
            val jsonString = prefs.getString(APP_USAGE_TIMES_KEY, null)
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
            prefs.edit().putLong(TRACKING_START_TIME_KEY, startTime).apply()
        } catch (e: Exception) {
            // ignore
        }
    }

    override suspend fun getTrackingStartTime(): Long {
        return try {
            prefs.getLong(TRACKING_START_TIME_KEY, 0L)
        } catch (e: Exception) {
            0L
        }
    }

    override suspend fun saveUsageAccessAllowed(allowed: Boolean) {
        try {
            prefs.edit().putBoolean(USAGE_ACCESS_ALLOWED_KEY, allowed).apply()
        } catch (_: Exception) {}
    }

    override suspend fun getUsageAccessAllowed(): Boolean {
        return try { prefs.getBoolean(USAGE_ACCESS_ALLOWED_KEY, false) } catch (_: Exception) { false }
    }

    override suspend fun saveAccessibilityAccessAllowed(allowed: Boolean) {
        try {
            prefs.edit().putBoolean(ACCESSIBILITY_ACCESS_ALLOWED_KEY, allowed).apply()
        } catch (_: Exception) {}
    }

    override suspend fun getAccessibilityAccessAllowed(): Boolean {
        return try { prefs.getBoolean(ACCESSIBILITY_ACCESS_ALLOWED_KEY, false) } catch (_: Exception) { false }
    }
    
    override suspend fun saveUsageDayEpoch(epochDay: Long) {
        try {
            prefs.edit().putLong(USAGE_DAY_EPOCH_KEY, epochDay).apply()
        } catch (_: Exception) {}
    }

    override suspend fun getUsageDayEpoch(): Long {
        return try { prefs.getLong(USAGE_DAY_EPOCH_KEY, 0L) } catch (_: Exception) { 0L }
    }

    override suspend fun saveBlockedState(isBlocked: Boolean) {
        try {
            prefs.edit().putBoolean(BLOCKED_STATE_KEY, isBlocked).apply()
        } catch (e: Exception) {
            // ignore
        }
    }

    override suspend fun getBlockedState(): Boolean {
        return try {
            prefs.getBoolean(BLOCKED_STATE_KEY, false)
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun saveTimesUnblockedToday(count: Int) {
        try {
            prefs.edit().putInt(TIMES_UNBLOCKED_TODAY_KEY, count).apply()
        } catch (e: Exception) {
            // ignore
        }
    }

    override suspend fun getTimesUnblockedToday(): Int {
        return try {
            prefs.getInt(TIMES_UNBLOCKED_TODAY_KEY, 0)
        } catch (e: Exception) {
            0
        }
    }

    override suspend fun saveTimesDismissedToday(count: Int) {
        try {
            prefs.edit().putInt(TIMES_DISMISSED_TODAY_KEY, count).apply()
        } catch (e: Exception) {
            // ignore
        }
    }

    override suspend fun getTimesDismissedToday(): Int {
        return try {
            prefs.getInt(TIMES_DISMISSED_TODAY_KEY, 0)
        } catch (e: Exception) {
            0
        }
    }

    override suspend fun saveSessionAppUsageTimes(usageTimes: Map<String, Long>) {
        try {
            val jsonString = serializeUsageTimes(usageTimes)
            prefs.edit().putString(SESSION_APP_USAGE_TIMES_KEY, jsonString).apply()
        } catch (e: Exception) {
            // ignore
        }
    }

    override suspend fun getSessionAppUsageTimes(): Map<String, Long> {
        return try {
            val jsonString = prefs.getString(SESSION_APP_USAGE_TIMES_KEY, null)
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
            prefs.edit().putLong(SESSION_START_TIME_KEY, startTime).apply()
        } catch (e: Exception) {
            // ignore
        }
    }

    override suspend fun getSessionStartTime(): Long {
        return try {
            prefs.getLong(SESSION_START_TIME_KEY, 0L)
        } catch (e: Exception) {
            0L
        }
    }

    override suspend fun getDoNotShowCongratulationAgain(): Boolean {
        return try {
            prefs.getBoolean(DO_NOT_SHOW_CONGRATS_KEY, false)
        } catch (_: Exception) { false }
    }

    override suspend fun saveDoNotShowCongratulationAgain(doNotShow: Boolean) {
        try {
            prefs.edit().putBoolean(DO_NOT_SHOW_CONGRATS_KEY, doNotShow).apply()
        } catch (_: Exception) {}
    }

    override suspend fun getDoNotShowDismissAgain(): Boolean {
        return try { prefs.getBoolean(DO_NOT_SHOW_DISMISS_KEY, false) } catch (_: Exception) { false }
    }

    override suspend fun saveDoNotShowDismissAgain(doNotShow: Boolean) {
        try {
            prefs.edit().putBoolean(DO_NOT_SHOW_DISMISS_KEY, doNotShow).apply()
        } catch (_: Exception) {}
    }

    override suspend fun getAutoRestartOnDismiss(): Boolean {
        return try { prefs.getBoolean(AUTO_RESTART_ON_DISMISS_KEY, false) } catch (_: Exception) { false }
    }

    override suspend fun saveAutoRestartOnDismiss(autoRestart: Boolean) {
        try {
            prefs.edit().putBoolean(AUTO_RESTART_ON_DISMISS_KEY, autoRestart).apply()
        } catch (_: Exception) {}
    }

    override suspend fun saveDayStreakCounter(count: Int) {
        try {
            prefs.edit().putInt(DAY_STREAK_COUNTER_KEY, count).apply()
        } catch (e: Exception) {
            // ignore
        }
    }

    override suspend fun getDayStreakCounter(): Int {
        return try {
            prefs.getInt(DAY_STREAK_COUNTER_KEY, 0)
        } catch (e: Exception) {
            0
        }
    }

    override suspend fun saveLastStreakUpdateDay(epochDay: Long) {
        try {
            prefs.edit().putLong(LAST_STREAK_UPDATE_DAY_KEY, epochDay).apply()
        } catch (e: Exception) {
            // ignore
        }
    }

    override suspend fun getLastStreakUpdateDay(): Long {
        return try {
            prefs.getLong(LAST_STREAK_UPDATE_DAY_KEY, 0L)
        } catch (e: Exception) {
            0L
        }
    }

    override suspend fun saveQrGeneratorVisited(visited: Boolean) {
        try {
            prefs.edit().putBoolean(QR_GENERATOR_VISITED_KEY, visited).apply()
        } catch (e: Exception) {
            // ignore
        }
    }

    override suspend fun getQrGeneratorVisited(): Boolean {
        return try {
            prefs.getBoolean(QR_GENERATOR_VISITED_KEY, false)
        } catch (e: Exception) {
            false
        }
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
    
    override suspend fun savePersistentTrackingConsent(consent: Boolean) {
        try {
            prefs.edit().putBoolean("persistent_tracking_consent", consent).apply()
        } catch (e: Exception) {
            // ignore
        }
    }
    
    override suspend fun getPersistentTrackingConsent(): Boolean {
        return try {
            prefs.getBoolean("persistent_tracking_consent", false)
        } catch (e: Exception) {
            false
        }
    }
}

// Global storage instance - will be initialized in MainActivity
private var globalStorage: AppStorage? = null

// Fallback storage that uses SharedPreferences without context
class FallbackAppStorage : AppStorage {
    override suspend fun isOnboardingCompleted(): Boolean {
        return false // Default to not completed for fallback
    }
    
    override suspend fun setOnboardingCompleted(completed: Boolean) {
        // No-op for fallback
    }
    
    override fun isOnboardingCompletedFlow(): Flow<Boolean> {
        return kotlinx.coroutines.flow.flowOf(false)
    }
    
    override suspend fun saveNotificationsEnabled(enabled: Boolean) { }

    override suspend fun getNotificationsEnabled(): Boolean { return true }

    override suspend fun saveQrCode(qrCode: SavedQrCode) {
        // No-op for fallback
    }
    
    override suspend fun getSavedQrCodes(): List<SavedQrCode> {
        return emptyList()
    }
    
    override suspend fun getQrCodeById(id: String): SavedQrCode? {
        return null
    }
    
    override suspend fun validateQrCode(scannedText: String): SavedQrCode? {
        return null
    }
    
    override suspend fun removeQrCode(id: String) {
        // No-op for fallback
    }

    override suspend fun saveSelectedAppPackages(packageNames: List<String>) {
        // No-op for fallback
    }

    override suspend fun getSelectedAppPackages(): List<String> {
        return emptyList()
    }

    override suspend fun saveTimeLimitMinutes(minutes: Int) {
        // No-op for fallback
    }

    override suspend fun getTimeLimitMinutes(): Int {
        return 15
    }

    override suspend fun saveTrackingState(isTracking: Boolean) {
        // No-op for fallback
    }

    override suspend fun getTrackingState(): Boolean {
        return false
    }

    override suspend fun saveAppUsageTimes(usageTimes: Map<String, Long>) {
        // No-op for fallback
    }

    override suspend fun getAppUsageTimes(): Map<String, Long> {
        return emptyMap()
    }

    override suspend fun saveTrackingStartTime(startTime: Long) {
        // No-op for fallback
    }

    override suspend fun getTrackingStartTime(): Long {
        return 0L
    }

    override suspend fun saveUsageAccessAllowed(allowed: Boolean) {
        // No-op for fallback
    }

    override suspend fun getUsageAccessAllowed(): Boolean {
        return false
    }

    override suspend fun saveAccessibilityAccessAllowed(allowed: Boolean) {
        // No-op for fallback
    }

    override suspend fun getAccessibilityAccessAllowed(): Boolean {
        return false
    }

    override suspend fun saveUsageDayEpoch(epochDay: Long) {
        // No-op for fallback
    }

    override suspend fun getUsageDayEpoch(): Long {
        return 0L
    }

    override suspend fun saveBlockedState(isBlocked: Boolean) {
        // No-op for fallback
    }

    override suspend fun getBlockedState(): Boolean {
        return false
    }

    override suspend fun saveTimesUnblockedToday(count: Int) {
        // No-op for fallback
    }

    override suspend fun getTimesUnblockedToday(): Int {
        return 0
    }

    override suspend fun saveTimesDismissedToday(count: Int) {
        // No-op for fallback
    }

    override suspend fun getTimesDismissedToday(): Int {
        return 0
    }

    override suspend fun saveSessionAppUsageTimes(usageTimes: Map<String, Long>) {
        // No-op for fallback
    }

    override suspend fun getSessionAppUsageTimes(): Map<String, Long> {
        return emptyMap()
    }

    override suspend fun saveSessionStartTime(startTime: Long) {
        // No-op for fallback
    }

    override suspend fun getSessionStartTime(): Long {
        return 0L
    }

    override suspend fun getDoNotShowCongratulationAgain(): Boolean {
        return false
    }

    override suspend fun saveDoNotShowCongratulationAgain(doNotShow: Boolean) {
        // No-op for fallback
    }

    override suspend fun getDoNotShowDismissAgain(): Boolean {
        return false
    }

    override suspend fun saveDoNotShowDismissAgain(doNotShow: Boolean) {
        // No-op for fallback
    }

    override suspend fun getAutoRestartOnDismiss(): Boolean {
        return false
    }

    override suspend fun saveAutoRestartOnDismiss(autoRestart: Boolean) {
        // No-op for fallback
    }

    override suspend fun saveQrGeneratorVisited(visited: Boolean) {
        // No-op for fallback
    }

    override suspend fun getQrGeneratorVisited(): Boolean {
        return false
    }

    override suspend fun saveDayStreakCounter(count: Int) {
        // No-op for fallback
    }

    override suspend fun getDayStreakCounter(): Int {
        return 0
    }

    override suspend fun saveLastStreakUpdateDay(epochDay: Long) {
        // No-op for fallback
    }

    override suspend fun getLastStreakUpdateDay(): Long {
        return 0L
    }
    
    override suspend fun savePersistentTrackingConsent(consent: Boolean) {
        // No-op for fallback
    }
    
    override suspend fun getPersistentTrackingConsent(): Boolean {
        return false
    }
}

actual fun createAppStorage(): AppStorage {
    return globalStorage ?: FallbackAppStorage()
}

fun initializeAppStorage(context: Context) {
    globalStorage = AndroidAppStorage(context)
}
