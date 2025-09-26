import kotlinx.coroutines.flow.Flow

/**
 * Data class representing a saved QR code
 */
data class SavedQrCode(
    val id: String,
    val qrText: String,
    val message: String,
    val createdAt: Long,
    val isActive: Boolean = true
)

/**
 * Platform-specific storage interface for persisting app preferences
 */
interface AppStorage {
    /**
     * Get whether onboarding has been completed
     */
    suspend fun isOnboardingCompleted(): Boolean
    
    /**
     * Set onboarding completion status
     */
    suspend fun setOnboardingCompleted(completed: Boolean)
    
    /**
     * Flow to observe onboarding completion status changes
     */
    fun isOnboardingCompletedFlow(): Flow<Boolean>

    /**
     * Persist whether notifications are enabled
     */
    suspend fun saveNotificationsEnabled(enabled: Boolean)

    /**
     * Retrieve whether notifications are enabled. Default to false if unset.
     */
    suspend fun getNotificationsEnabled(): Boolean

    /**
     * Save a QR code for later scanning
     */
    suspend fun saveQrCode(qrCode: SavedQrCode)
    
    /**
     * Get all saved QR codes
     */
    suspend fun getSavedQrCodes(): List<SavedQrCode>
    
    /**
     * Get a specific QR code by ID
     */
    suspend fun getQrCodeById(id: String): SavedQrCode?
    
    /**
     * Validate if a scanned QR code matches any saved QR codes
     */
    suspend fun validateQrCode(scannedText: String): SavedQrCode?
    
    /**
     * Remove a QR code by ID
     */
    suspend fun removeQrCode(id: String)

    /**
     * Persist selected app package identifiers
     */
    suspend fun saveSelectedAppPackages(packageNames: List<String>)

    /**
     * Retrieve previously selected app package identifiers
     */
    suspend fun getSelectedAppPackages(): List<String>

    /**
     * Persist the global time limit (in minutes)
     */
    suspend fun saveTimeLimitMinutes(minutes: Int)

    /**
     * Retrieve the global time limit (in minutes). Return 15 if unset.
     */
    suspend fun getTimeLimitMinutes(): Int

    /**
     * Persist tracking state (whether tracking is currently active)
     */
    suspend fun saveTrackingState(isTracking: Boolean)

    /**
     * Retrieve tracking state. Return false if unset.
     */
    suspend fun getTrackingState(): Boolean

    /**
     * Persist app usage times (in seconds)
     */
    suspend fun saveAppUsageTimes(usageTimes: Map<String, Long>)

    /**
     * Retrieve app usage times. Return empty map if unset.
     */
    suspend fun getAppUsageTimes(): Map<String, Long>

    /**
     * Persist tracking start time
     */
    suspend fun saveTrackingStartTime(startTime: Long)

    /**
     * Retrieve tracking start time. Return 0 if unset.
     */
    suspend fun getTrackingStartTime(): Long

    /**
     * Persist whether app usage access is allowed by the user
     */
    suspend fun saveUsageAccessAllowed(allowed: Boolean)

    /**
     * Retrieve whether app usage access is allowed. Return false if unset.
     */
    suspend fun getUsageAccessAllowed(): Boolean

    /**
     * Persist whether accessibility access is allowed by the user
     */
    suspend fun saveAccessibilityAccessAllowed(allowed: Boolean)

    /**
     * Retrieve whether accessibility access is allowed. Return false if unset.
     */
    suspend fun getAccessibilityAccessAllowed(): Boolean

    /**
     * Persist the epoch day (UTC) that current usage totals correspond to
     */
    suspend fun saveUsageDayEpoch(epochDay: Long)

    /**
     * Retrieve the saved epoch day (UTC). Return 0 if unset.
     */
    suspend fun getUsageDayEpoch(): Long

    /**
     * Persist blocked state (whether user is currently blocked from using tracked apps)
     */
    suspend fun saveBlockedState(isBlocked: Boolean)

    /**
     * Retrieve blocked state. Return false if unset.
     */
    suspend fun getBlockedState(): Boolean

    /**
     * Persist times unblocked today counter
     */
    suspend fun saveTimesUnblockedToday(count: Int)

    /**
     * Retrieve times unblocked today counter. Return 0 if unset.
     */
    suspend fun getTimesUnblockedToday(): Int

    /**
     * Persist times dismissed today counter
     */
    suspend fun saveTimesDismissedToday(count: Int)

    /**
     * Retrieve times dismissed today counter. Return 0 if unset.
     */
    suspend fun getTimesDismissedToday(): Int

    /**
     * Persist session app usage times (for persistence across app restarts)
     */
    suspend fun saveSessionAppUsageTimes(usageTimes: Map<String, Long>)

    /**
     * Retrieve session app usage times. Return empty map if unset.
     */
    suspend fun getSessionAppUsageTimes(): Map<String, Long>

    /**
     * Persist session start time
     */
    suspend fun saveSessionStartTime(startTime: Long)

    /**
     * Retrieve session start time. Return 0 if unset.
     */
    suspend fun getSessionStartTime(): Long
}

/**
 * Expect declaration for platform-specific storage implementation
 */
expect fun createAppStorage(): AppStorage
