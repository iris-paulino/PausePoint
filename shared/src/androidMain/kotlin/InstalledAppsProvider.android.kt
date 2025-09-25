import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import java.io.ByteArrayOutputStream

class AndroidInstalledAppsProvider(private val context: Context) : InstalledAppsProvider {
    
    private val packageManager: PackageManager = context.packageManager
    
    // List of commonly tracked app package names
    private val trackableAppPackages = setOf(
        "com.instagram.android",
        "com.zhiliaoapp.musically", // TikTok
        "com.facebook.katana", // Facebook
        "com.twitter.android",
        "com.google.android.youtube",
        "com.snapchat.android",
        "com.reddit.frontpage",
        "com.linkedin.android",
        "com.whatsapp",
        "com.tencent.mm", // WeChat
        "com.spotify.music",
        "com.netflix.mediaclient",
        "com.amazon.avod.thirdpartyclient",
        "com.disney.disneyplus",
        "com.hulu.plus",
        "com.pinterest",
        "com.tumblr",
        "com.viber.voip",
        "com.telegram.messenger",
        "com.discord",
        "com.slack.android",
        "com.microsoft.teams",
        "com.zoom.videomeetings"
    )
    
    // App name to emoji mapping for better visual representation
    private val appEmojiMap = mapOf(
        "Instagram" to "📷",
        "TikTok" to "🎵",
        "Facebook" to "📘",
        "Twitter" to "🐦",
        "YouTube" to "📺",
        "Snapchat" to "👻",
        "Reddit" to "🤖",
        "LinkedIn" to "💼",
        "WhatsApp" to "💬",
        "WeChat" to "💬",
        "Spotify" to "🎵",
        "Netflix" to "🎬",
        "Amazon Prime Video" to "🎬",
        "Disney+" to "🏰",
        "Hulu" to "🎬",
        "Pinterest" to "📌",
        "Tumblr" to "📝",
        "Viber" to "💬",
        "Telegram" to "✈️",
        "Discord" to "🎮",
        "Slack" to "💼",
        "Microsoft Teams" to "💼",
        "Zoom" to "📹"
    )
    
    override suspend fun getInstalledApps(): List<InstalledApp> {
        return try {
            val allApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            println("DEBUG: Total installed apps: ${allApps.size}")
            
            val installedApps = allApps
                .filter { appInfo ->
                    val isThisApp = appInfo.packageName == context.packageName
                    val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val isUpdatedSystemApp = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                    
                    // Include user-installed apps and updated system apps (like Gmail, Chrome, etc.)
                    // Exclude core system services but include user-facing system apps
                    val shouldInclude = !isThisApp && (
                        !isSystemApp || // Include all non-system apps
                        isUpdatedSystemApp || // Include updated system apps (user-installed updates)
                        isUserFacingSystemApp(appInfo) // Include specific user-facing system apps
                    )
                    
                    if (shouldInclude) {
                        println("DEBUG: Including app: ${packageManager.getApplicationLabel(appInfo)} (${appInfo.packageName}) - System: $isSystemApp, Updated: $isUpdatedSystemApp")
                    }
                    shouldInclude
                }
                .take(100) // Limit to first 100 apps to avoid overwhelming the UI
                .map { appInfo ->
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    val emoji = appEmojiMap[appName] ?: getDefaultEmojiForCategory(getAppCategory(appInfo))
                    
                    InstalledApp(
                        packageName = appInfo.packageName,
                        appName = appName,
                        icon = emoji,
                        category = getAppCategory(appInfo)
                    )
                }
                .sortedBy { it.appName }
            
            println("DEBUG: Returning ${installedApps.size} apps")
            installedApps
        } catch (e: Exception) {
            // Return empty list if there's an error
            println("DEBUG: Error getting installed apps: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
    
    override suspend fun getTrackableApps(): List<InstalledApp> {
        return try {
            val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { appInfo ->
                    // Filter to only trackable apps
                    trackableAppPackages.contains(appInfo.packageName) &&
                    (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
                }
                .map { appInfo ->
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    val emoji = appEmojiMap[appName] ?: "📱"
                    
                    InstalledApp(
                        packageName = appInfo.packageName,
                        appName = appName,
                        icon = emoji,
                        category = getAppCategory(appInfo)
                    )
                }
                .sortedBy { it.appName }
            
            installedApps
        } catch (e: Exception) {
            // Return empty list if there's an error
            emptyList()
        }
    }
    
    private fun getAppCategory(appInfo: ApplicationInfo): String {
        val packageName = appInfo.packageName.lowercase()
        val appName = packageManager.getApplicationLabel(appInfo).toString().lowercase()
        
        return when {
            // Social Media
            packageName.contains("social") || 
            packageName.contains("facebook") ||
            packageName.contains("instagram") ||
            packageName.contains("twitter") ||
            packageName.contains("snapchat") ||
            packageName.contains("tiktok") ||
            packageName.contains("reddit") ||
            packageName.contains("linkedin") ||
            packageName.contains("pinterest") ||
            packageName.contains("tumblr") ||
            packageName.contains("discord") ||
            packageName.contains("whatsapp") ||
            packageName.contains("telegram") ||
            packageName.contains("viber") ||
            appName.contains("social") -> "Social Media"
            
            // Entertainment
            packageName.contains("music") ||
            packageName.contains("spotify") ||
            packageName.contains("youtube") ||
            packageName.contains("netflix") ||
            packageName.contains("amazon") ||
            packageName.contains("disney") ||
            packageName.contains("hulu") ||
            packageName.contains("twitch") ||
            packageName.contains("movies") ||
            packageName.contains("books") ||
            appName.contains("music") ||
            appName.contains("video") ||
            appName.contains("stream") ||
            appName.contains("movie") -> "Entertainment"
            
            // Games
            packageName.contains("game") ||
            packageName.contains("gaming") ||
            packageName.contains("steam") ||
            packageName.contains("epic") ||
            appName.contains("game") -> "Games"
            
            // Productivity & Communication
            packageName.contains("work") ||
            packageName.contains("office") ||
            packageName.contains("slack") ||
            packageName.contains("teams") ||
            packageName.contains("zoom") ||
            packageName.contains("meet") ||
            packageName.contains("calendar") ||
            packageName.contains("email") ||
            packageName.contains("mail") ||
            packageName.contains("gmail") ||
            packageName.contains("outlook") ||
            packageName.contains("docs") ||
            packageName.contains("sheets") ||
            packageName.contains("drive") ||
            packageName.contains("keep") ||
            packageName.contains("translate") ||
            appName.contains("work") ||
            appName.contains("office") ||
            appName.contains("productivity") ||
            appName.contains("email") ||
            appName.contains("calendar") -> "Productivity"
            
            // Browser
            packageName.contains("browser") ||
            packageName.contains("chrome") ||
            packageName.contains("firefox") ||
            packageName.contains("safari") ||
            packageName.contains("edge") ||
            appName.contains("browser") ||
            appName.contains("internet") -> "Browser"
            
            // Camera & Photos
            packageName.contains("camera") ||
            packageName.contains("photo") ||
            packageName.contains("gallery") ||
            packageName.contains("photos") ||
            appName.contains("camera") ||
            appName.contains("photo") ||
            appName.contains("gallery") -> "Camera & Photos"
            
            // Communication
            packageName.contains("messaging") ||
            packageName.contains("mms") ||
            packageName.contains("dialer") ||
            packageName.contains("phone") ||
            packageName.contains("contacts") ||
            appName.contains("message") ||
            appName.contains("phone") ||
            appName.contains("contact") -> "Communication"
            
            // Shopping
            packageName.contains("shopping") ||
            packageName.contains("amazon") ||
            packageName.contains("ebay") ||
            packageName.contains("store") ||
            packageName.contains("vending") ||
            appName.contains("shop") ||
            appName.contains("store") -> "Shopping"
            
            // Maps & Navigation
            packageName.contains("maps") ||
            packageName.contains("navigation") ||
            appName.contains("map") ||
            appName.contains("navigation") -> "Maps & Navigation"
            
            // Utilities
            packageName.contains("calculator") ||
            packageName.contains("filemanager") ||
            packageName.contains("documentsui") ||
            packageName.contains("settings") ||
            packageName.contains("packageinstaller") ||
            appName.contains("calculator") ||
            appName.contains("file") ||
            appName.contains("setting") -> "Utilities"
            
            else -> "Other"
        }
    }
    
    private fun getDefaultEmojiForCategory(category: String): String {
        return when (category) {
            "Social Media" -> "👥"
            "Entertainment" -> "🎬"
            "Games" -> "🎮"
            "Productivity" -> "💼"
            "Browser" -> "🌐"
            "Camera & Photos" -> "📷"
            "Communication" -> "💬"
            "Shopping" -> "🛒"
            "Maps & Navigation" -> "🗺️"
            "Utilities" -> "🔧"
            else -> "📱"
        }
    }
    
    private fun isUserFacingSystemApp(appInfo: ApplicationInfo): Boolean {
        val packageName = appInfo.packageName.lowercase()
        
        // List of user-facing system apps that should be included
        val userFacingSystemApps = setOf(
            // Google Apps
            "com.google.android.gm", // Gmail
            "com.android.chrome", // Chrome
            "com.google.android.apps.photos", // Google Photos
            "com.google.android.youtube", // YouTube
            "com.google.android.apps.maps", // Google Maps
            "com.google.android.apps.docs", // Google Docs
            "com.google.android.apps.sheets", // Google Sheets
            "com.google.android.apps.slides", // Google Slides
            "com.google.android.apps.drive", // Google Drive
            "com.google.android.calendar", // Google Calendar
            "com.google.android.keep", // Google Keep
            "com.google.android.apps.translate", // Google Translate
            "com.google.android.apps.tachyon", // Google Meet
            "com.google.android.apps.messaging", // Google Messages
            "com.google.android.apps.contacts", // Google Contacts
            "com.google.android.apps.phone", // Google Phone
            "com.google.android.apps.camera", // Google Camera
            "com.google.android.apps.music", // Google Play Music
            "com.google.android.apps.books", // Google Play Books
            "com.google.android.apps.magazines", // Google Play Newsstand
            "com.google.android.apps.movies", // Google Play Movies
            "com.google.android.apps.games", // Google Play Games
            
            // Samsung Apps
            "com.samsung.android.email.provider", // Samsung Email
            "com.samsung.android.browser", // Samsung Internet
            "com.samsung.android.gallery", // Samsung Gallery
            "com.samsung.android.camera", // Samsung Camera
            "com.samsung.android.music", // Samsung Music
            "com.samsung.android.video", // Samsung Video
            "com.samsung.android.calendar", // Samsung Calendar
            "com.samsung.android.contacts", // Samsung Contacts
            "com.samsung.android.messaging", // Samsung Messages
            "com.samsung.android.phone", // Samsung Phone
            
            // Other common system apps
            "com.android.chrome", // Chrome (alternative package name)
            "com.android.email", // Email
            "com.android.camera2", // Camera
            "com.android.gallery3d", // Gallery
            "com.android.music", // Music
            "com.android.calendar", // Calendar
            "com.android.contacts", // Contacts
            "com.android.mms", // Messages
            "com.android.dialer", // Phone
            "com.android.calculator2", // Calculator
            "com.android.calculator", // Calculator (alternative)
            "com.android.filemanager", // File Manager
            "com.android.documentsui", // Files
            "com.android.settings", // Settings (if user wants to track)
            "com.android.packageinstaller", // Package Installer
            "com.android.vending", // Google Play Store
            
            // Microsoft Apps (often pre-installed)
            "com.microsoft.office.outlook", // Outlook
            "com.microsoft.office.excel", // Excel
            "com.microsoft.office.word", // Word
            "com.microsoft.office.powerpoint", // PowerPoint
            "com.microsoft.skype.teams", // Teams
            "com.microsoft.skype.raider", // Skype
            
            // Other popular pre-installed apps
            "com.whatsapp", // WhatsApp
            "com.facebook.orca", // Facebook Messenger
            "com.spotify.music", // Spotify
            "com.netflix.mediaclient", // Netflix
            "com.amazon.avod.thirdpartyclient", // Amazon Prime Video
            "com.disney.disneyplus", // Disney+
            "com.hulu.plus", // Hulu
            "com.pinterest", // Pinterest
            "com.tumblr", // Tumblr
            "com.viber.voip", // Viber
            "ph.telegra.Telegraph", // Telegram
            "com.discord", // Discord
            "com.slack.android", // Slack
            "us.zoom.videomeetings" // Zoom
        )
        
        return userFacingSystemApps.contains(packageName)
    }
}

// Global provider instance - will be initialized in MainActivity
private var globalInstalledAppsProvider: InstalledAppsProvider? = null

// Fallback provider that returns empty list
class FallbackInstalledAppsProvider : InstalledAppsProvider {
    override suspend fun getInstalledApps(): List<InstalledApp> {
        println("DEBUG: FallbackInstalledAppsProvider.getInstalledApps() called - returning empty list")
        return emptyList()
    }
    
    override suspend fun getTrackableApps(): List<InstalledApp> {
        println("DEBUG: FallbackInstalledAppsProvider.getTrackableApps() called - returning empty list")
        return emptyList()
    }
}

actual fun createInstalledAppsProvider(): InstalledAppsProvider {
    val provider = globalInstalledAppsProvider ?: FallbackInstalledAppsProvider()
    println("DEBUG: createInstalledAppsProvider returning: ${if (provider is FallbackInstalledAppsProvider) "Fallback" else "Android"}")
    return provider
}

fun initializeInstalledAppsProvider(context: Context) {
    println("DEBUG: Initializing InstalledAppsProvider with context: ${context.packageName}")
    globalInstalledAppsProvider = AndroidInstalledAppsProvider(context)
    println("DEBUG: InstalledAppsProvider initialized successfully")
}
