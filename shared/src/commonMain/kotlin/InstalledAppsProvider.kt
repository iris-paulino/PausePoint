/**
 * Data class representing an installed app
 */
data class InstalledApp(
    val packageName: String,
    val appName: String,
    val icon: String = "📱", // Default icon, can be overridden by platform-specific implementations
    val category: String = "Unknown"
)

/**
 * Platform-specific interface for getting installed apps
 */
interface InstalledAppsProvider {
    /**
     * Get list of installed apps on the device
     * @return List of installed apps
     */
    suspend fun getInstalledApps(): List<InstalledApp>
    
    /**
     * Get list of installed apps that are commonly tracked (social media, entertainment, etc.)
     * @return List of commonly tracked apps
     */
    suspend fun getTrackableApps(): List<InstalledApp>
}

/**
 * Expect declaration for platform-specific installed apps provider implementation
 */
expect fun createInstalledAppsProvider(): InstalledAppsProvider
