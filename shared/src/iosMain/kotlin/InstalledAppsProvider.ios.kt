import platform.Foundation.NSBundle
import platform.UIKit.UIApplication

class IOSInstalledAppsProvider : InstalledAppsProvider {
    
    // List of commonly tracked app bundle identifiers
    private val trackableAppBundles = setOf(
        "com.burbn.instagram",
        "com.zhiliaoapp.musically", // TikTok
        "com.facebook.Facebook",
        "com.atebits.Tweetie2", // Twitter
        "com.google.ios.youtube",
        "com.toyopagroup.picaboo", // Snapchat
        "com.reddit.Reddit",
        "com.linkedin.LinkedIn",
        "net.whatsapp.WhatsApp",
        "com.tencent.xin", // WeChat
        "com.spotify.client",
        "com.netflix.Netflix",
        "com.amazon.aiv.AIVApp", // Amazon Prime Video
        "com.disney.disneyplus",
        "com.hulu.plus",
        "com.pinterest",
        "com.tumblr.tumblr",
        "com.viber",
        "ph.telegra.Telegraph", // Telegram
        "com.hammerandchisel.discord",
        "com.tinyspeck.chatlyio", // Slack
        "com.microsoft.skype.teams",
        "us.zoom.videomeetings"
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
            // Note: iOS doesn't allow apps to query installed apps for security reasons
            // This is a limitation of iOS - we can only detect if specific apps are installed
            // by checking if they can be opened via URL schemes
            getTrackableApps()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    override suspend fun getTrackableApps(): List<InstalledApp> {
        return try {
            val installedApps = mutableListOf<InstalledApp>()
            
            // Check each trackable app by trying to open it
            trackableAppBundles.forEach { bundleId ->
                if (canOpenApp(bundleId)) {
                    val appName = getAppNameFromBundleId(bundleId)
                    val emoji = appEmojiMap[appName] ?: "📱"
                    
                    installedApps.add(
                        InstalledApp(
                            packageName = bundleId,
                            appName = appName,
                            icon = emoji,
                            category = getAppCategory(bundleId)
                        )
                    )
                }
            }
            
            installedApps.sortedBy { it.appName }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun canOpenApp(bundleId: String): Boolean {
        return try {
            // Try to open the app using URL scheme
            val url = when (bundleId) {
                "com.burbn.instagram" -> "instagram://"
                "com.zhiliaoapp.musically" -> "tiktok://"
                "com.facebook.Facebook" -> "fb://"
                "com.atebits.Tweetie2" -> "twitter://"
                "com.google.ios.youtube" -> "youtube://"
                "com.toyopagroup.picaboo" -> "snapchat://"
                "com.reddit.Reddit" -> "reddit://"
                "com.linkedin.LinkedIn" -> "linkedin://"
                "net.whatsapp.WhatsApp" -> "whatsapp://"
                "com.tencent.xin" -> "wechat://"
                "com.spotify.client" -> "spotify://"
                "com.netflix.Netflix" -> "nflx://"
                "com.amazon.aiv.AIVApp" -> "aiv://"
                "com.disney.disneyplus" -> "disneyplus://"
                "com.hulu.plus" -> "hulu://"
                "com.pinterest" -> "pinterest://"
                "com.tumblr.tumblr" -> "tumblr://"
                "com.viber" -> "viber://"
                "ph.telegra.Telegraph" -> "tg://"
                "com.hammerandchisel.discord" -> "discord://"
                "com.tinyspeck.chatlyio" -> "slack://"
                "com.microsoft.skype.teams" -> "msteams://"
                "us.zoom.videomeetings" -> "zoomus://"
                else -> null
            }
            
            if (url != null) {
                val nsUrl = platform.Foundation.NSURL.URLWithString(url)
                nsUrl?.let { UIApplication.sharedApplication.canOpenURL(it) } ?: false
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    private fun getAppNameFromBundleId(bundleId: String): String {
        return when (bundleId) {
            "com.burbn.instagram" -> "Instagram"
            "com.zhiliaoapp.musically" -> "TikTok"
            "com.facebook.Facebook" -> "Facebook"
            "com.atebits.Tweetie2" -> "Twitter"
            "com.google.ios.youtube" -> "YouTube"
            "com.toyopagroup.picaboo" -> "Snapchat"
            "com.reddit.Reddit" -> "Reddit"
            "com.linkedin.LinkedIn" -> "LinkedIn"
            "net.whatsapp.WhatsApp" -> "WhatsApp"
            "com.tencent.xin" -> "WeChat"
            "com.spotify.client" -> "Spotify"
            "com.netflix.Netflix" -> "Netflix"
            "com.amazon.aiv.AIVApp" -> "Amazon Prime Video"
            "com.disney.disneyplus" -> "Disney+"
            "com.hulu.plus" -> "Hulu"
            "com.pinterest" -> "Pinterest"
            "com.tumblr.tumblr" -> "Tumblr"
            "com.viber" -> "Viber"
            "ph.telegra.Telegraph" -> "Telegram"
            "com.hammerandchisel.discord" -> "Discord"
            "com.tinyspeck.chatlyio" -> "Slack"
            "com.microsoft.skype.teams" -> "Microsoft Teams"
            "us.zoom.videomeetings" -> "Zoom"
            else -> bundleId
        }
    }
    
    private fun getAppCategory(bundleId: String): String {
        return when {
            bundleId.contains("instagram") ||
            bundleId.contains("facebook") ||
            bundleId.contains("twitter") ||
            bundleId.contains("snapchat") ||
            bundleId.contains("tiktok") ||
            bundleId.contains("reddit") ||
            bundleId.contains("pinterest") ||
            bundleId.contains("tumblr") -> "Social Media"
            
            bundleId.contains("spotify") ||
            bundleId.contains("youtube") ||
            bundleId.contains("netflix") ||
            bundleId.contains("disney") ||
            bundleId.contains("hulu") -> "Entertainment"
            
            bundleId.contains("whatsapp") ||
            bundleId.contains("wechat") ||
            bundleId.contains("viber") ||
            bundleId.contains("telegram") -> "Communication"
            
            bundleId.contains("linkedin") ||
            bundleId.contains("slack") ||
            bundleId.contains("teams") ||
            bundleId.contains("zoom") -> "Productivity"
            
            else -> "Other"
        }
    }
}

actual fun createInstalledAppsProvider(): InstalledAppsProvider {
    return IOSInstalledAppsProvider()
}

