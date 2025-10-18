import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.statusBarsPadding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CoroutineScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import kotlin.random.Random
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import createAdManager

@OptIn(ExperimentalResourceApi::class)
@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    contentDescription: String = "Scroll Pause Logo"
) {
    Image(
        painter = painterResource("images/scrollpause_new_logo.png"),
        contentDescription = contentDescription,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp)),
        contentScale = ContentScale.Fit
    )
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun BadgeIcon(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    number: Int = 2
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Badge yellow image
        Image(
            painter = painterResource("images/yellowbadge.png"),
            contentDescription = "Achievement Badge",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        
        // Number overlay positioned higher on the badge
        Text(
            text = number.toString(),
            color = Color(0xFF004aad), // Dark blue theme color
            fontSize = (size * 0.4f).value.sp, // Smaller font size to fit nicely on badge
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.offset(y = (-size * 0.1f)) // Move text up by 10% of badge size
        )
    }
}

@Composable
fun getRandomCongratulationMessage(): String {
    val messages = listOf(
        "Look at you go! Your legs just got a better workout than your thumbs! 🦵",
        "Walking to your QR code? You're basically a marathon runner now! 🏃",
        "Your scrolling thumb is probably wondering where you went! 👍",
        "Someone's been skipping leg day... but NOT today! 💪",
        "Breaking News: Local human stands up AND walks. Scientists amazed! 📰",
        "That QR code didn't scan itself! Well done, movement champion! 🎯",
        "Your couch is proud of you for leaving it! 🛋️",
        "Achievement Unlocked: Actually Moving Your Body! 🏆",
        "Who knew standing could feel this good? (Your body did.) 🌟",
        "You've earned the right to sit back down... but maybe walk around first? 😄"
    )
    return messages[Random.nextInt(messages.size)]
}

@Composable
fun getRandomPauseMessage(): String {
    val messages = listOf(
        "Did you know that doomscrolling is linked to higher levels of anxiety and stress? Step away and let yourself recharge.",
        "Pause for a moment — your mind will thank you.",
        "Did you know? Studies show that spending too long scrolling can disrupt your sleep and mood. Stretch your body and give your mind a break.",
        "Too much scrolling floods your brain with stress hormones — take a breather!",
        "Research shows that cutting down screen time can boost happiness and focus.",
        "Even a 5-minute pause can reset your mood and reduce tension.",
        "Your brain needs breaks, too — give it some quiet time to recharge.",
        "You deserve calm, not chaos. Take a short pause.",
        "A quick scroll break can help you feel more grounded and present.",
        "Stepping away now means you'll feel better later.",
        "Less scrolling, more serenity — you've got this.",
        "Research links doomscrolling to higher cortisol (the stress hormone). A pause helps lower it.",
        "People who limit social media use report better focus and less loneliness. Go enjoy something screen-free — your brain will love it.",
        "Scrolling too long keeps your brain in alert mode — breaks help you relax.",
        "Pausing from screens helps your brain process emotions more clearly.",
        "Reducing doomscrolling can improve your sleep quality within days.",
        "Take a breath. Step away. You're choosing peace over panic.",
        "You're in control — not the algorithm.",
        "Your time matters more than endless feeds.",
        "One pause at a time — your mind will thank you.",
        "You've broken the scroll once before — you can do it again.",
        "Go enjoy something screen-free — your brain will love it.",
        "Look up, move, and take in the world around you.",
        "Refocus your energy on something that fills you up.",
        "A few minutes away can lift your mood instantly.",
        "Reconnect with what makes you feel alive.",
        "Take control back — your attention is powerful.",
        "Log off for a bit and see how much clearer you feel.",
        "You’ve got this — put the phone down and breathe.",
        "Do something real — even one small action counts.",
        "Turn off the feed, turn on your focus.",
        "Try a short break — your mind will thank you.",
        "Take a moment offline and feel the difference.",
        "Give yourself five minutes of peace right now.",
        "Pause and reconnect with yourself — you deserve it.",
        "Step away for a bit — your focus will follow."
    )
    return messages[Random.nextInt(messages.size)]
}

@Composable
fun App() {
    MaterialTheme(
        colors = androidx.compose.material.MaterialTheme.colors.copy(
            background = Color(0xFF1A1A1A),
            surface = Color(0xFF2C2C2C),
            primary = Color(0xFF1A1A1A),
            onBackground = Color.White,
            onSurface = Color.White,
            onPrimary = Color.White
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A1A1A),
                            Color(0xFF161616)
                        )
                    )
                ),
            color = Color.Transparent
        ) {
            Box(Modifier.fillMaxSize().statusBarsPadding()) {
                AppRoot()
            }
        }
    }
}

@Composable
private fun QrDetailScreen(
    qrText: String,
    message: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
            .statusBarsPadding()
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("←", fontSize = 24.sp, color = Color.White, modifier = Modifier.clickable { onBack() })
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("QR Code", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(message, fontSize = 14.sp, color = Color(0xFFD1D5DB))
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Large QR display (square, using available width)
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(maxWidth)
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                QrCodeDisplay(text = qrText, modifier = Modifier.fillMaxSize())
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

// Simple navigation and app state holder
private enum class Route { Onboarding, QrGenerator, Dashboard, AppSelection, DurationSetting, Pause, Settings, SavedQrCodes, QrDetail, PrivacyPolicy, Permissions, Tutorial }

// Enable simulated app usage increments for testing; rely on platform-specific tracking instead
private const val ENABLE_USAGE_SIMULATION: Boolean = true

// Platform hook to open Accessibility settings (Android) or no-op elsewhere
expect fun openAccessibilitySettings()
expect fun openUsageAccessSettings()
// Platform check for whether our AccessibilityService is enabled
expect fun isAccessibilityServiceEnabled(): Boolean
// Platform function to get the current foreground app package name
expect fun getCurrentForegroundApp(): String?

private data class TrackedApp(
    val name: String,
    val minutesUsed: Int,
    val limitMinutes: Int
)

private data class AvailableApp(
    val name: String,
    val category: String,
    val icon: String,
    val packageName: String,
    val isSelected: Boolean = false
)

// Comprehensive app information with multiple identifiers for robust tracking
private data class AppInfo(
    val displayName: String,
    val primaryPackageName: String,
    val alternativePackageNames: List<String> = emptyList(),
    val alternativeDisplayNames: List<String> = emptyList(),
    val category: String,
    val icon: String
)

// Comprehensive database of popular apps with all possible identifiers
private val COMPREHENSIVE_APP_DATABASE = mapOf(
    // Social Media Apps
    "instagram" to AppInfo(
        displayName = "Instagram",
        primaryPackageName = "com.instagram.android",
        alternativePackageNames = listOf("com.instagram.lite"),
        alternativeDisplayNames = listOf("IG", "Insta"),
        category = "Social Media",
        icon = "📷"
    ),
    "tiktok" to AppInfo(
        displayName = "TikTok",
        primaryPackageName = "com.zhiliaoapp.musically",
        alternativePackageNames = listOf("com.ss.android.ugc.trill", "com.zhiliaoapp.musically.lite"),
        alternativeDisplayNames = listOf("Tik Tok", "Musically"),
        category = "Social Media",
        icon = "🎵"
    ),
    "facebook" to AppInfo(
        displayName = "Facebook",
        primaryPackageName = "com.facebook.katana",
        alternativePackageNames = listOf("com.facebook.lite", "com.facebook.orca"),
        alternativeDisplayNames = listOf("FB", "Meta"),
        category = "Social Media",
        icon = "📘"
    ),
    "twitter" to AppInfo(
        displayName = "Twitter",
        primaryPackageName = "com.twitter.android",
        alternativePackageNames = listOf("com.twitter.android.lite"),
        alternativeDisplayNames = listOf("X", "Tweet"),
        category = "Social Media",
        icon = "🐦"
    ),
    "snapchat" to AppInfo(
        displayName = "Snapchat",
        primaryPackageName = "com.snapchat.android",
        alternativePackageNames = listOf("com.snapchat.android.lite"),
        alternativeDisplayNames = listOf("Snap"),
        category = "Social Media",
        icon = "👻"
    ),
    "reddit" to AppInfo(
        displayName = "Reddit",
        primaryPackageName = "com.reddit.frontpage",
        alternativePackageNames = listOf("com.reddit.frontpage.lite"),
        alternativeDisplayNames = listOf("Reddit"),
        category = "Social Media",
        icon = "🤖"
    ),
    "pinterest" to AppInfo(
        displayName = "Pinterest",
        primaryPackageName = "com.pinterest",
        alternativePackageNames = listOf("com.pinterest.lite"),
        alternativeDisplayNames = listOf("Pin"),
        category = "Social Media",
        icon = "📌"
    ),
    "linkedin" to AppInfo(
        displayName = "LinkedIn",
        primaryPackageName = "com.linkedin.android",
        alternativePackageNames = listOf("com.linkedin.android.lite"),
        alternativeDisplayNames = listOf("Linked In"),
        category = "Professional",
        icon = "💼"
    ),
    "discord" to AppInfo(
        displayName = "Discord",
        primaryPackageName = "com.discord",
        alternativePackageNames = listOf("com.discord.lite"),
        alternativeDisplayNames = listOf("Discord Chat"),
        category = "Social Media",
        icon = "💬"
    ),
    "telegram" to AppInfo(
        displayName = "Telegram",
        primaryPackageName = "org.telegram.messenger",
        alternativePackageNames = listOf("org.telegram.plus"),
        alternativeDisplayNames = listOf("Telegram Messenger"),
        category = "Messaging",
        icon = "✈️"
    ),
    "whatsapp" to AppInfo(
        displayName = "WhatsApp",
        primaryPackageName = "com.whatsapp",
        alternativePackageNames = listOf("com.whatsapp.w4b", "com.whatsapp.business"),
        alternativeDisplayNames = listOf("Whats App", "WA"),
        category = "Messaging",
        icon = "💚"
    ),
    "messenger" to AppInfo(
        displayName = "Messenger",
        primaryPackageName = "com.facebook.orca",
        alternativePackageNames = listOf("com.facebook.mlite"),
        alternativeDisplayNames = listOf("Facebook Messenger", "FB Messenger"),
        category = "Messaging",
        icon = "💙"
    ),
    "signal" to AppInfo(
        displayName = "Signal",
        primaryPackageName = "org.thoughtcrime.securesms",
        alternativePackageNames = listOf("org.thoughtcrime.securesms.lite"),
        alternativeDisplayNames = listOf("Signal Messenger"),
        category = "Messaging",
        icon = "🔒"
    ),
    "viber" to AppInfo(
        displayName = "Viber",
        primaryPackageName = "com.viber.voip",
        alternativePackageNames = listOf("com.viber.voip.lite"),
        alternativeDisplayNames = listOf("Viber Messenger"),
        category = "Messaging",
        icon = "💜"
    ),
    "wechat" to AppInfo(
        displayName = "WeChat",
        primaryPackageName = "com.tencent.mm",
        alternativePackageNames = listOf("com.tencent.mm.lite"),
        alternativeDisplayNames = listOf("We Chat", "微信"),
        category = "Messaging",
        icon = "💚"
    ),
    "line" to AppInfo(
        displayName = "LINE",
        primaryPackageName = "jp.naver.line.android",
        alternativePackageNames = listOf("jp.naver.line.android.lite"),
        alternativeDisplayNames = listOf("Line Messenger"),
        category = "Messaging",
        icon = "💚"
    ),
    
    // Entertainment Apps
    "youtube" to AppInfo(
        displayName = "YouTube",
        primaryPackageName = "com.google.android.youtube",
        alternativePackageNames = listOf("com.google.android.apps.youtube.kids", "com.google.android.youtube.tv"),
        alternativeDisplayNames = listOf("YT", "You Tube"),
        category = "Entertainment",
        icon = "📺"
    ),
    "youtube music" to AppInfo(
        displayName = "YouTube Music",
        primaryPackageName = "com.google.android.apps.youtube.music",
        alternativePackageNames = listOf("com.google.android.apps.youtube.music.lite"),
        alternativeDisplayNames = listOf("YT Music", "YouTube Music"),
        category = "Music",
        icon = "🎵"
    ),
    "spotify" to AppInfo(
        displayName = "Spotify",
        primaryPackageName = "com.spotify.music",
        alternativePackageNames = listOf("com.spotify.music.lite"),
        alternativeDisplayNames = listOf("Spotify Music"),
        category = "Music",
        icon = "🎵"
    ),
    "netflix" to AppInfo(
        displayName = "Netflix",
        primaryPackageName = "com.netflix.mediaclient",
        alternativePackageNames = listOf("com.netflix.mediaclient.lite"),
        alternativeDisplayNames = listOf("Net Flix"),
        category = "Entertainment",
        icon = "🎬"
    ),
    "twitch" to AppInfo(
        displayName = "Twitch",
        primaryPackageName = "tv.twitch.android.app",
        alternativePackageNames = listOf("tv.twitch.android.app.lite"),
        alternativeDisplayNames = listOf("Twitch TV"),
        category = "Entertainment",
        icon = "🎮"
    ),
    "amazon prime" to AppInfo(
        displayName = "Amazon Prime Video",
        primaryPackageName = "com.amazon.avod.thirdpartyclient",
        alternativePackageNames = listOf("com.amazon.avod.thirdpartyclient.lite"),
        alternativeDisplayNames = listOf("Prime Video", "Amazon Prime"),
        category = "Entertainment",
        icon = "📺"
    ),
    "disney plus" to AppInfo(
        displayName = "Disney+",
        primaryPackageName = "com.disney.disneyplus",
        alternativePackageNames = listOf("com.disney.disneyplus.lite"),
        alternativeDisplayNames = listOf("Disney Plus", "Disney+"),
        category = "Entertainment",
        icon = "🏰"
    ),
    "hulu" to AppInfo(
        displayName = "Hulu",
        primaryPackageName = "com.hulu.plus",
        alternativePackageNames = listOf("com.hulu.plus.lite"),
        alternativeDisplayNames = listOf("Hulu Plus"),
        category = "Entertainment",
        icon = "📺"
    ),
    
    // Productivity Apps
    "chrome" to AppInfo(
        displayName = "Chrome",
        primaryPackageName = "com.android.chrome",
        alternativePackageNames = listOf("com.chrome.beta", "com.chrome.dev", "com.chrome.canary"),
        alternativeDisplayNames = listOf("Google Chrome", "Chrome Browser"),
        category = "Browser",
        icon = "🌐"
    ),
    "firefox" to AppInfo(
        displayName = "Firefox",
        primaryPackageName = "org.mozilla.firefox",
        alternativePackageNames = listOf("org.mozilla.firefox_beta", "org.mozilla.fenix"),
        alternativeDisplayNames = listOf("Mozilla Firefox", "Firefox Browser"),
        category = "Browser",
        icon = "🦊"
    ),
    "safari" to AppInfo(
        displayName = "Safari",
        primaryPackageName = "com.apple.safari",
        alternativePackageNames = listOf("com.apple.safari.lite"),
        alternativeDisplayNames = listOf("Safari Browser"),
        category = "Browser",
        icon = "🧭"
    ),
    "edge" to AppInfo(
        displayName = "Microsoft Edge",
        primaryPackageName = "com.microsoft.emmx",
        alternativePackageNames = listOf("com.microsoft.emmx.beta"),
        alternativeDisplayNames = listOf("Edge", "Microsoft Edge Browser"),
        category = "Browser",
        icon = "🌐"
    ),
    "gmail" to AppInfo(
        displayName = "Gmail",
        primaryPackageName = "com.google.android.gm",
        alternativePackageNames = listOf("com.google.android.gm.lite"),
        alternativeDisplayNames = listOf("Google Mail", "GMail"),
        category = "Productivity",
        icon = "📧"
    ),
    "outlook" to AppInfo(
        displayName = "Outlook",
        primaryPackageName = "com.microsoft.office.outlook",
        alternativePackageNames = listOf("com.microsoft.office.outlook.lite"),
        alternativeDisplayNames = listOf("Microsoft Outlook", "Outlook Mail"),
        category = "Productivity",
        icon = "📧"
    ),
    "maps" to AppInfo(
        displayName = "Google Maps",
        primaryPackageName = "com.google.android.apps.maps",
        alternativePackageNames = listOf("com.google.android.apps.maps.lite"),
        alternativeDisplayNames = listOf("Maps", "Google Maps"),
        category = "Navigation",
        icon = "🗺️"
    ),
    "waze" to AppInfo(
        displayName = "Waze",
        primaryPackageName = "com.waze",
        alternativePackageNames = listOf("com.waze.lite"),
        alternativeDisplayNames = listOf("Waze Navigation"),
        category = "Navigation",
        icon = "🗺️"
    ),
    "uber" to AppInfo(
        displayName = "Uber",
        primaryPackageName = "com.ubercab",
        alternativePackageNames = listOf("com.ubercab.lite"),
        alternativeDisplayNames = listOf("Uber Driver", "Uber Eats"),
        category = "Transportation",
        icon = "🚗"
    ),
    "lyft" to AppInfo(
        displayName = "Lyft",
        primaryPackageName = "me.lyft.android",
        alternativePackageNames = listOf("me.lyft.android.lite"),
        alternativeDisplayNames = listOf("Lyft Driver"),
        category = "Transportation",
        icon = "🚗"
    ),
    
    // Gaming Apps
    "minecraft" to AppInfo(
        displayName = "Minecraft",
        primaryPackageName = "com.mojang.minecraftpe",
        alternativePackageNames = listOf("com.mojang.minecraftpe.lite"),
        alternativeDisplayNames = listOf("Minecraft PE", "Minecraft Pocket Edition"),
        category = "Gaming",
        icon = "🧱"
    ),
    "pubg" to AppInfo(
        displayName = "PUBG Mobile",
        primaryPackageName = "com.tencent.ig",
        alternativePackageNames = listOf("com.pubg.krmobile", "com.pubg.newstate"),
        alternativeDisplayNames = listOf("PUBG", "PlayerUnknown's Battlegrounds"),
        category = "Gaming",
        icon = "🎮"
    ),
    "fortnite" to AppInfo(
        displayName = "Fortnite",
        primaryPackageName = "com.epicgames.fortnite",
        alternativePackageNames = listOf("com.epicgames.fortnite.lite"),
        alternativeDisplayNames = listOf("Fortnite Mobile"),
        category = "Gaming",
        icon = "🎮"
    ),
    "clash of clans" to AppInfo(
        displayName = "Clash of Clans",
        primaryPackageName = "com.supercell.clashofclans",
        alternativePackageNames = listOf("com.supercell.clashofclans.lite"),
        alternativeDisplayNames = listOf("COC", "Clash of Clans"),
        category = "Gaming",
        icon = "⚔️"
    ),
    "clash royale" to AppInfo(
        displayName = "Clash Royale",
        primaryPackageName = "com.supercell.clashroyale",
        alternativePackageNames = listOf("com.supercell.clashroyale.lite"),
        alternativeDisplayNames = listOf("CR", "Clash Royale"),
        category = "Gaming",
        icon = "👑"
    ),
    
    // Shopping Apps
    "amazon" to AppInfo(
        displayName = "Amazon",
        primaryPackageName = "com.amazon.mShop.android.shopping",
        alternativePackageNames = listOf("com.amazon.mShop.android.shopping.lite"),
        alternativeDisplayNames = listOf("Amazon Shopping", "Amazon App"),
        category = "Shopping",
        icon = "🛒"
    ),
    "ebay" to AppInfo(
        displayName = "eBay",
        primaryPackageName = "com.ebay.mobile",
        alternativePackageNames = listOf("com.ebay.mobile.lite"),
        alternativeDisplayNames = listOf("eBay Mobile", "eBay App"),
        category = "Shopping",
        icon = "🛒"
    ),
    "shopify" to AppInfo(
        displayName = "Shopify",
        primaryPackageName = "com.shopify.mobile",
        alternativePackageNames = listOf("com.shopify.mobile.lite"),
        alternativeDisplayNames = listOf("Shopify Mobile"),
        category = "Shopping",
        icon = "🛒"
    ),
    
    // Food & Delivery Apps
    "doordash" to AppInfo(
        displayName = "DoorDash",
        primaryPackageName = "com.dd.doordash",
        alternativePackageNames = listOf("com.dd.doordash.lite"),
        alternativeDisplayNames = listOf("Door Dash"),
        category = "Food & Delivery",
        icon = "🍔"
    ),
    "ubereats" to AppInfo(
        displayName = "Uber Eats",
        primaryPackageName = "com.ubercab.eats",
        alternativePackageNames = listOf("com.ubercab.eats.lite"),
        alternativeDisplayNames = listOf("Uber Eats", "UberEats"),
        category = "Food & Delivery",
        icon = "🍔"
    ),
    "grubhub" to AppInfo(
        displayName = "Grubhub",
        primaryPackageName = "com.grubhub.android",
        alternativePackageNames = listOf("com.grubhub.android.lite"),
        alternativeDisplayNames = listOf("Grub Hub", "Grubhub"),
        category = "Food & Delivery",
        icon = "🍔"
    ),
    "postmates" to AppInfo(
        displayName = "Postmates",
        primaryPackageName = "com.postmates.android",
        alternativePackageNames = listOf("com.postmates.android.lite"),
        alternativeDisplayNames = listOf("Post Mates"),
        category = "Food & Delivery",
        icon = "🍔"
    )
)

// Robust app matching functions
private fun findAppByPackageName(packageName: String): AppInfo? {
    return COMPREHENSIVE_APP_DATABASE.values.find { appInfo ->
        appInfo.primaryPackageName == packageName || 
        appInfo.alternativePackageNames.contains(packageName)
    }
}

private fun findAppByDisplayName(displayName: String): AppInfo? {
    val normalizedName = displayName.lowercase().trim()
    return COMPREHENSIVE_APP_DATABASE.values.find { appInfo ->
        appInfo.displayName.lowercase() == normalizedName ||
        appInfo.alternativeDisplayNames.any { it.lowercase() == normalizedName }
    }
}

private fun findAppByAnyIdentifier(identifier: String): AppInfo? {
    val normalizedId = identifier.lowercase().trim()
    
    // First try exact package name match
    findAppByPackageName(identifier)?.let { return it }
    
    // Then try display name match
    findAppByDisplayName(identifier)?.let { return it }
    
    // Then try partial matches in package names
    COMPREHENSIVE_APP_DATABASE.values.find { appInfo ->
        appInfo.primaryPackageName.lowercase().contains(normalizedId) ||
        appInfo.alternativePackageNames.any { it.lowercase().contains(normalizedId) }
    }?.let { return it }
    
    // Finally try partial matches in display names
    COMPREHENSIVE_APP_DATABASE.values.find { appInfo ->
        appInfo.displayName.lowercase().contains(normalizedId) ||
        appInfo.alternativeDisplayNames.any { it.lowercase().contains(normalizedId) }
    }?.let { return it }
    
    return null
}

private fun getAllPossibleIdentifiersForApp(appInfo: AppInfo): List<String> {
    return listOf(appInfo.displayName) + 
           appInfo.alternativeDisplayNames + 
           listOf(appInfo.primaryPackageName) + 
           appInfo.alternativePackageNames
}

private fun getAllTrackedAppIdentifiers(trackedApps: List<TrackedApp>): List<String> {
    return trackedApps.flatMap { trackedApp ->
        val appInfo = findAppByDisplayName(trackedApp.name)
        if (appInfo != null) {
            getAllPossibleIdentifiersForApp(appInfo)
        } else {
            // Fallback for unknown apps - use the name as-is and try to create a package name
            listOf(trackedApp.name, trackedApp.name.lowercase().replace(" ", ""))
        }
    }
}

private fun getPackageNameForTrackedApp(trackedApp: TrackedApp): String {
    val appInfo = findAppByDisplayName(trackedApp.name)
    return appInfo?.primaryPackageName ?: trackedApp.name.lowercase().replace(" ", "")
}

private fun getDefaultTrackedAppsFromDatabase(): List<TrackedApp> {
    val defaultAppKeys = listOf(
        "instagram", "tiktok", "snapchat", "facebook", "twitter", "reddit", 
        "pinterest", "linkedin", "discord", "telegram", "whatsapp",
        "youtube", "chrome"
    )
    
    return defaultAppKeys.mapNotNull { key ->
        COMPREHENSIVE_APP_DATABASE[key]?.let { appInfo ->
            TrackedApp(appInfo.displayName, 0, 15)
        }
    }
}

private fun getDefaultAvailableAppsFromDatabase(): List<AvailableApp> {
    val defaultAppKeys = listOf(
        "instagram", "tiktok", "snapchat", "facebook", "twitter", "reddit", 
        "pinterest", "linkedin", "discord", "telegram", "whatsapp",
        "youtube", "chrome"
    )
    
    return defaultAppKeys.mapNotNull { key ->
        COMPREHENSIVE_APP_DATABASE[key]?.let { appInfo ->
            AvailableApp(
                name = appInfo.displayName,
                category = appInfo.category,
                icon = appInfo.icon,
                packageName = appInfo.primaryPackageName
            )
        }
    }
}

@Composable
private fun AppRoot() {
    var route by remember { mutableStateOf<Route?>(null) } // Start with null to show loading
    var qrMessage by remember { mutableStateOf("Take a mindful pause") }
    var qrId by remember { mutableStateOf<String?>(null) }
    var viewedQrId by remember { mutableStateOf<String?>(null) }
    var viewedQrText by remember { mutableStateOf<String?>(null) }
    var viewedQrMessage by remember { mutableStateOf<String?>(null) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var showTimeRemainingInfoDialog by remember { mutableStateOf(false) }
    var showNoTrackedAppsDialog by remember { mutableStateOf(false) }
    var showNoQrCodeDialog by remember { mutableStateOf(false) }
    var showUsageAccessDialog by remember { mutableStateOf(false) }
    var fromNoQrCodeDialog by remember { mutableStateOf(false) }
    var showCongratulationDialog by remember { mutableStateOf(false) }
    var doNotShowCongratulationAgain by remember { mutableStateOf(false) }
    var showDismissDialog by remember { mutableStateOf(false) }
    var restartTrackingOnUnlock by remember { mutableStateOf(false) }
    var doNotShowDismissAgain by remember { mutableStateOf(false) }
    var autoRestartOnDismiss by remember { mutableStateOf(false) }
    var hasShownNotificationsPromptThisLaunch by remember { mutableStateOf(false) }
    var hasShownUsageAccessPromptThisLaunch by remember { mutableStateOf(false) }
    var hasCheckedPermissionsOnDashboardThisLaunch by remember { mutableStateOf(false) }
    var showAccessibilityConsentDialog by remember { mutableStateOf(false) }
    var showUsageAccessDisableConfirmationDialog by remember { mutableStateOf(false) }
    var showAccessibilityDisableConfirmationDialog by remember { mutableStateOf(false) }
    var showPersistentTrackingConsentDialog by remember { mutableStateOf(false) }
    var persistentTrackingConsent by remember { mutableStateOf(false) }
    var pendingStartTracking by remember { mutableStateOf(false) }
    var userManuallyStoppedTracking by remember { mutableStateOf(false) }
    var trackedApps by remember { mutableStateOf<List<TrackedApp>>(emptyList()) }
    
    var availableApps by remember { mutableStateOf<List<AvailableApp>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(false) }
    
    var timeLimitMinutes by remember { mutableStateOf(15) }
    
    val storage = remember { createAppStorage() }
    fun currentEpochDayUtc(): Long = getCurrentTimeMillis() / 86_400_000L
    val installedAppsProvider = remember { createInstalledAppsProvider() }
    val coroutineScope = rememberCoroutineScope()
    
    // Track the last known day for midnight reset detection
    var lastKnownDay by remember { mutableStateOf(currentEpochDayUtc()) }

    // Load persisted preference for restart-on-unlock so PauseScreen and dialog are consistent
    LaunchedEffect(Unit) {
        try {
            restartTrackingOnUnlock = storage.getAutoRestartOnDismiss()
            persistentTrackingConsent = storage.getPersistentTrackingConsent()
        } catch (_: Exception) {}
    }
    
    var isTracking by remember { mutableStateOf(false) }
    var trackingStartTime by remember { mutableStateOf(0L) }
    var appUsageTimes by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var isPaused by remember { mutableStateOf(false) }
    
    // Session tracking variables that reset on dismiss/QR scan
    var sessionStartTime by remember { mutableStateOf(0L) }
    var sessionAppUsageTimes by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var sessionElapsedSeconds by remember { mutableStateOf(0L) }
    // Tracks how many whole minutes have already been credited to each app during the current session
    var sessionCreditedMinutes by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    // Foreground tracking markers and session-start guard
    var currentForegroundApp by remember { mutableStateOf<String?>(null) }
    var appActiveSince by remember { mutableStateOf(0L) }
    var sessionJustStarted by remember { mutableStateOf(false) }
    
    // Counter for times unblocked today
    var timesUnblockedToday by remember { mutableStateOf(0) }
    // Counter for times dismissed today
    var timesDismissedToday by remember { mutableStateOf(0) }
    // Counter for consecutive days without dismissing
    var dayStreakCounter by remember { mutableStateOf(0) }
    var isSetupMode by remember { mutableStateOf(false) }
    
    // Function to reset all daily statistics
    fun resetDailyStatistics() {
        println("DEBUG: Resetting daily statistics at midnight")
        
        // Reset all daily counters
        timesUnblockedToday = 0
        timesDismissedToday = 0
        appUsageTimes = emptyMap()
        sessionAppUsageTimes = emptyMap()
        sessionStartTime = 0L
        sessionCreditedMinutes = emptyMap()
        
        // Reset tracked apps usage
        trackedApps = trackedApps.map { it.copy(minutesUsed = 0) }
        
        // Update the last known day
        lastKnownDay = currentEpochDayUtc()
        
        // Persist the reset data
        coroutineScope.launch {
            try {
                storage.saveAppUsageTimes(emptyMap())
                storage.saveTimesUnblockedToday(0)
                storage.saveTimesDismissedToday(0)
                storage.saveSessionAppUsageTimes(emptyMap())
                storage.saveSessionStartTime(0L)
                storage.saveUsageDayEpoch(lastKnownDay)
                println("DEBUG: Successfully saved reset daily statistics to storage")
            } catch (e: Exception) {
                println("DEBUG: Error saving reset daily statistics: ${e.message}")
            }
        }
    }
    
    // Background timer to check for midnight reset every minute
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000) // Check every minute
            val currentDay = currentEpochDayUtc()
            if (currentDay != lastKnownDay) {
                println("DEBUG: Day change detected - from $lastKnownDay to $currentDay")
                resetDailyStatistics()
            }
        }
    }

    // Load persisted preferences on first composition
    LaunchedEffect(Unit) {
        try {
            doNotShowCongratulationAgain = storage.getDoNotShowCongratulationAgain()
        } catch (_: Exception) {}
        try {
            doNotShowDismissAgain = storage.getDoNotShowDismissAgain()
        } catch (_: Exception) {}
        try {
            autoRestartOnDismiss = storage.getAutoRestartOnDismiss()
        } catch (_: Exception) {}
        
        // Preload ads when app starts
        try {
            val adManager = createAdManager()
            println("DEBUG: Preloading ads on app start...")
            adManager.loadAd()
        } catch (e: Exception) {
            println("DEBUG: Error preloading ads: ${e.message}")
        }
        
        // No need to sync with system permissions - app works with internal preference only
    }

    // Merge session usage into lifetime usage (minutesUsed and appUsageTimes)
    fun finalizeSessionUsage() {
        println("DEBUG: finalizeSessionUsage() called")
        println("DEBUG: sessionAppUsageTimes: $sessionAppUsageTimes")
        println("DEBUG: trackedApps before: ${trackedApps.map { "${it.name}: ${it.minutesUsed}m" }}")
        
        if (sessionAppUsageTimes.isEmpty()) {
            println("DEBUG: sessionAppUsageTimes is empty, returning")
            return
        }
        
        // Credit remaining uncredited minutes for all apps with any usage this session
        trackedApps = trackedApps.map { app ->
            val sessionSeconds = sessionAppUsageTimes[app.name] ?: 0L
            if (sessionSeconds > 0) {
                val totalSessionMinutes = (sessionSeconds / 60L).toInt()
                val creditedSoFar = sessionCreditedMinutes[app.name] ?: 0
                val remaining = (totalSessionMinutes - creditedSoFar).coerceAtLeast(0)
                if (remaining > 0) {
                    println("DEBUG: Finalize crediting remaining $remaining min to ${app.name}")
                }
                app.copy(minutesUsed = app.minutesUsed + remaining)
            } else app
        }
        
        println("DEBUG: trackedApps after: ${trackedApps.map { "${it.name}: ${it.minutesUsed}m" }}")
        
        // Update lifetime seconds for all apps that recorded seconds this session
        val updatedLifetimeSeconds = appUsageTimes.toMutableMap()
        sessionAppUsageTimes.forEach { (appName, sessionSeconds) ->
            if (sessionSeconds > 0) {
                val current = updatedLifetimeSeconds[appName] ?: 0L
                updatedLifetimeSeconds[appName] = current + sessionSeconds
            }
        }
        appUsageTimes = updatedLifetimeSeconds
        
        // Clear session usage after finalizing to prevent double counting
        sessionAppUsageTimes = emptyMap()
        sessionCreditedMinutes = emptyMap()
        
        println("DEBUG: appUsageTimes after: $appUsageTimes")
        println("DEBUG: Cleared sessionAppUsageTimes to prevent double counting")
    }

    // Helper function to proceed with dismiss logic after ad
    fun proceedWithDismiss() {
        println("DEBUG: proceedWithDismiss called - ad completed, now clearing blocked state")
        // Always finalize
        finalizeSessionUsage()
        // Count dismiss
        timesDismissedToday += 1
        coroutineScope.launch { try { storage.saveTimesDismissedToday(timesDismissedToday) } catch (_: Exception) {} }

        // Auto-restart tracking after dismiss (always restart to allow continued monitoring)
        pendingStartTracking = true
        userManuallyStoppedTracking = false
        
        // Clear blocked state and overlays ONLY after ad is completed
        isPaused = false
        updateAccessibilityServiceBlockedState(isPaused, emptyList(), 0)
        stopWellbeingMonitoring()
        clearPersistentWellbeingNotification()
        coroutineScope.launch { try { storage.saveBlockedState(false) } catch (_: Exception) {} }
        sessionAppUsageTimes = emptyMap()
        sessionCreditedMinutes = emptyMap()
        sessionStartTime = 0L
        sessionElapsedSeconds = 0L
        // Ensure next session begins clean
        currentForegroundApp = null
        appActiveSince = 0L
        sessionJustStarted = true
        
        // Persist reset session data to storage to ensure clean restart
        coroutineScope.launch {
            try { 
                storage.saveSessionAppUsageTimes(emptyMap())
                storage.saveSessionStartTime(0L)
                println("DEBUG: Saved reset session data to storage after ad completion")
            } catch (e: Exception) {
                println("DEBUG: Error saving reset session data: ${e.message}")
            }
        }
        
        dismissPauseScreen()
        
        // Navigate to dashboard
        route = Route.Dashboard
    }

    // Helper function to handle dismiss with mandatory ads
    fun handleDismissWithAd() {
        println("DEBUG: handleDismissWithAd called - showing mandatory ad")
        try {
            println("DEBUG: Creating AdManager...")
            val adManager = createAdManager()
            println("DEBUG: AdManager created successfully")
            
            val isLoaded = adManager.isAdLoaded()
            println("DEBUG: Ad loaded status: $isLoaded")
            
            if (isLoaded) {
                println("DEBUG: Ad is loaded, showing interstitial ad")
                // Show mandatory ad - user must watch it to proceed
                adManager.showInterstitialAd(
                    onAdClosed = {
                        println("DEBUG: Mandatory ad completed, proceeding with dismiss")
                        proceedWithDismiss()
                    },
                    onAdFailedToLoad = {
                        println("DEBUG: Ad failed to show, proceeding with dismiss anyway")
                        proceedWithDismiss()
                    }
                )
            } else {
                println("DEBUG: No ad loaded, attempting to load and show...")
                // Try to load an ad first, then show it
                adManager.loadAd()
                
                // Wait a moment for the ad to load, then try to show it
                coroutineScope.launch {
                    delay(2000) // Wait 2 seconds for ad to load
                    val isLoadedAfterWait = adManager.isAdLoaded()
                    println("DEBUG: Ad loaded status after wait: $isLoadedAfterWait")
                    
                    if (isLoadedAfterWait) {
                        println("DEBUG: Ad loaded after wait, showing interstitial ad")
                        adManager.showInterstitialAd(
                            onAdClosed = {
                                println("DEBUG: Mandatory ad completed after wait, proceeding with dismiss")
                                proceedWithDismiss()
                            },
                            onAdFailedToLoad = {
                                println("DEBUG: Ad failed to show after wait, proceeding with dismiss anyway")
                                proceedWithDismiss()
                            }
                        )
                    } else {
                        println("DEBUG: Ad still not loaded after wait, proceeding without ad")
                        proceedWithDismiss()
                    }
                }
            }
        } catch (e: Exception) {
            println("DEBUG: Error showing ad: ${e.message}, proceeding with dismiss")
            println("DEBUG: Exception details: ${e.stackTraceToString()}")
            proceedWithDismiss()
        }
    }

    // Helper function to handle QR scan success
    fun handleQrScanSuccess() {
        // Finalize session usage before resetting (same as Dismiss)
        println("DEBUG: PauseScreen onScanQr called - finalizing session usage")
        finalizeSessionUsage()
        println("DEBUG: trackedApps after finalize (QR): ${trackedApps.map { "${it.name}: ${it.minutesUsed}m" }}")
        
        // Reset session tracking state and increment unblocked counter
        isTracking = false
        isPaused = false
        // Update accessibility service with unblocked state
        updateAccessibilityServiceBlockedState(isPaused, emptyList(), 0)
        // Stop system-level app blocking
        stopWellbeingMonitoring()
        // Clear persistent blocking notification
        clearPersistentWellbeingNotification()
        // Save unblocked state to storage
        coroutineScope.launch {
            storage.saveBlockedState(false)
        }
        sessionAppUsageTimes = emptyMap()
        sessionStartTime = 0L
        sessionCreditedMinutes = emptyMap()
        sessionElapsedSeconds = 0L
        // Ensure next session begins clean
        currentForegroundApp = null
        appActiveSince = 0L
        sessionJustStarted = true
        timesUnblockedToday += 1
        
        // Persist reset session data to storage to ensure clean restart
        coroutineScope.launch {
            try { 
                storage.saveSessionAppUsageTimes(emptyMap())
                storage.saveSessionStartTime(0L)
                println("DEBUG: Saved reset session data to storage after QR scan success")
            } catch (e: Exception) {
                println("DEBUG: Error saving reset session data after QR scan: ${e.message}")
            }
        }
        
        // Update day streak counter logic
        val todayEpochDay = currentEpochDayUtc()
        
        // Save the incremented times walked counter to storage and update day streak
        coroutineScope.launch {
            try {
                val lastStreakUpdateDay = withTimeoutOrNull(3000) { storage.getLastStreakUpdateDay() } ?: 0L
                
                // Always increment day streak counter on successful QR scan
                // This ensures positive feedback even after dismissals
                if (lastStreakUpdateDay != todayEpochDay) {
                    // First QR scan of a new day: increment streak
                    dayStreakCounter += 1
                    println("DEBUG: handleQrScanSuccess - incremented day streak to: $dayStreakCounter (new day)")
                } else {
                    // Same day: still increment to show progress
                    dayStreakCounter += 1
                    println("DEBUG: handleQrScanSuccess - incremented day streak to: $dayStreakCounter (same day)")
                }
                
                storage.saveTimesUnblockedToday(timesUnblockedToday)
                storage.saveDayStreakCounter(dayStreakCounter)
                storage.saveLastStreakUpdateDay(todayEpochDay)
                println("DEBUG: handleQrScanSuccess - saved times walked to storage: $timesUnblockedToday")
                println("DEBUG: handleQrScanSuccess - saved day streak to storage: $dayStreakCounter")
                
                // Check for milestone achievements
                when (dayStreakCounter) {
                    7 -> showStreakMilestone("1 Week Streak!")
                    30 -> showStreakMilestone("1 Month Streak!")
                    100 -> showStreakMilestone("100 Days!")
                }
            } catch (e: Exception) {
                println("DEBUG: handleQrScanSuccess - error saving counters: ${e.message}")
            }
        }
        route = Route.Dashboard
        
        // Dismiss any blocking overlays
        dismissPauseScreen()
    }

    

    // Track individual app usage when tracking is active
    LaunchedEffect(isTracking) {
        println("DEBUG: *** FIRST LaunchedEffect triggered *** isTracking: $isTracking")
        println("DEBUG: *** FIRST LaunchedEffect - current thread: ${Thread.currentThread().name}")
        if (isTracking) {
            // Start a brand-new session: zero any prior session data and persist the reset
            sessionAppUsageTimes = emptyMap()
            sessionCreditedMinutes = emptyMap()
            sessionElapsedSeconds = 0L
            // Also clear any pre-session foreground tracking markers to avoid pre-counting
            currentForegroundApp = null
            appActiveSince = 0L
            sessionJustStarted = true
            println("DEBUG: Session start reset - cleared currentForegroundApp/appActiveSince")
            coroutineScope.launch {
                try {
                    storage.saveSessionAppUsageTimes(emptyMap())
                    storage.saveSessionStartTime(0L)
                    println("DEBUG: Starting new session - cleared prior session usage in storage")
                } catch (e: Exception) {
                    println("DEBUG: Error clearing prior session usage at start: ${e.message}")
                }
            }
            trackingStartTime = getCurrentTimeMillis()
            sessionStartTime = getCurrentTimeMillis() // Start new session
            println("DEBUG: *** TRACKING STARTED *** trackingStartTime set to: $trackingStartTime")
            
            // Save tracking state and start time to storage
            coroutineScope.launch {
                storage.saveTrackingState(true)
                storage.saveTrackingStartTime(trackingStartTime)
                storage.saveSessionStartTime(sessionStartTime)
            }
            
            // Update tracked apps with current time limit before starting tracking
            trackedApps = trackedApps.map { app ->
                app.copy(limitMinutes = timeLimitMinutes)
            }
            println("DEBUG: Updated tracked apps with current time limit: $timeLimitMinutes minutes")
            
            // Update accessibility service with tracked apps so it can track time and trigger blocking
            updateAccessibilityServiceBlockedState(false, getAllTrackedAppIdentifiers(trackedApps), timeLimitMinutes)
            println("DEBUG: Updated accessibility service with tracked apps for time tracking")
            
            // Start platform-specific usage tracking with robust app identification
            val trackedPackages = trackedApps.map { app ->
                getPackageNameForTrackedApp(app)
            }
            
            startUsageTracking(
                trackedPackages = trackedPackages,
                limitMinutes = timeLimitMinutes,
                onLimitReached = {
                    isTracking = false
                    route = Route.Pause
                }
            )
            
        // Start enhanced services only if user has given consent
            if (persistentTrackingConsent) {
                // Start foreground service for persistent monitoring
                startAppMonitoringForegroundService()
                
                // Save tracking state for restart detection
                saveTrackingStateForRestart(isTracking, isPaused, trackedApps.map { it.name }, timeLimitMinutes)
            } else {
                // Show consent dialog for enhanced tracking
                showPersistentTrackingConsentDialog = true
            }
        } else {
            // Stop tracking and save final state
            println("DEBUG: *** TRACKING STOPPED *** isTracking: $isTracking, trackingStartTime: $trackingStartTime")
            if (trackingStartTime > 0) {
                // Save updated usage times and tracking state to storage
                coroutineScope.launch {
                    storage.saveAppUsageTimes(appUsageTimes)
                    storage.saveTrackingState(false)
                }
            }
            
            // Stop foreground service
            stopAppMonitoringForegroundService()
        }
    }

    // REMOVED: Cross-app monitoring for policy compliance
    // Blocking now happens within the app itself when users return to it

    

    

    // Function to update usage for currently active app
    fun updateCurrentAppUsage() {
        if (!isTracking || route == Route.Pause || isPaused) {
            println("DEBUG: updateCurrentAppUsage - not tracking: isTracking=$isTracking, route=$route, isPaused=$isPaused")
            return
        }
        if (currentForegroundApp == null || appActiveSince == 0L) {
            println("DEBUG: updateCurrentAppUsage - no app or time: currentForegroundApp=$currentForegroundApp, appActiveSince=$appActiveSince")
            return
        }
        
        val currentTime = getCurrentTimeMillis()
        val timeSpent = (currentTime - appActiveSince) / 1000L // Convert to seconds
        
        if (timeSpent > 0) {
            val updatedSessionUsage = sessionAppUsageTimes.toMutableMap()
            
            // Find which tracked app is currently active and add the time
            for (app in trackedApps) {
                val expectedPackage = getPackageNameForTrackedApp(app)
                
                if (currentForegroundApp == expectedPackage) {
                    val currentUsage = updatedSessionUsage[app.name] ?: 0L
                    updatedSessionUsage[app.name] = currentUsage + timeSpent
                    println("DEBUG: Updated current app usage - Added $timeSpent seconds to ${app.name} (total: ${currentUsage + timeSpent})")
                    
                    // Reset the active time to avoid double counting
                    appActiveSince = currentTime
                    break
                }
            }
            
            sessionAppUsageTimes = updatedSessionUsage
            
            // Persist session data
            coroutineScope.launch {
                try { 
                    storage.saveSessionAppUsageTimes(sessionAppUsageTimes)
                    println("DEBUG: Saved session app usage times to storage")
                } catch (e: Exception) {
                    println("DEBUG: Error saving session app usage times: ${e.message}")
                }
            }
        }
    }

    // Event-driven tracking: Handle app changes via accessibility service events
    LaunchedEffect(isTracking, trackingStartTime) {
        println("DEBUG: *** SECOND LaunchedEffect triggered *** isTracking: $isTracking, trackingStartTime: $trackingStartTime")
        println("DEBUG: *** SECOND LaunchedEffect - current thread: ${Thread.currentThread().name}")
        if (isTracking && trackingStartTime > 0) {
            println("DEBUG: Starting event-driven tracking")
        } else {
            println("DEBUG: LaunchedEffect condition failed - isTracking: $isTracking, trackingStartTime: $trackingStartTime")
        }
        
        if (isTracking && trackingStartTime > 0) {
            // Minimal polling only for time limit checks and accessibility monitoring
            while (isTracking) {
                delay(30000) // Check every 30 seconds for time limits and accessibility status
                println("DEBUG: Time limit check - isTracking: $isTracking")

                // If Pause screen is active or blocking overlay is shown, do not accrue usage for any apps
                if (route == Route.Pause || isPaused) {
                    continue
                }

                // Check if Accessibility is still enabled
                val isAccessibilityEnabled = isAccessibilityServiceEnabled()
                println("DEBUG: *** ACCESSIBILITY CHECK *** isAccessibilityServiceEnabled: $isAccessibilityEnabled")
                if (!isAccessibilityEnabled) {
                    println("DEBUG: Accessibility disabled - stopping tracking to maintain data integrity")
                    // Finalize current session usage before stopping
                    finalizeSessionUsage()
                    // Stop tracking
                    isTracking = false
                    isPaused = false
                    // Update accessibility service with unblocked state
                    updateAccessibilityServiceBlockedState(isPaused, emptyList(), 0)
                    // Save unblocked state to storage
                    coroutineScope.launch {
                        try { 
                            storage.saveBlockedState(false)
                            println("DEBUG: Saved unblocked state to storage")
                        } catch (e: Exception) {
                            println("DEBUG: Error saving unblocked state: ${e.message}")
                        }
                    }
                    // Navigate back to dashboard to show user that tracking has stopped
                    route = Route.Dashboard
                    // Show notification to inform user
                    showAccessibilityDisabledNotification()
                    return@LaunchedEffect
                }
                
                // Usage Access optional mode: do not stop tracking on loss
                
                // Update usage for currently active app (in case user stayed in same app)
                updateCurrentAppUsage()
                
                // Check if session usage has reached the limit based on actual accumulated session usage
                var totalSessionSeconds = sessionAppUsageTimes.values.sum()
                // Defensive cap: total session seconds should never exceed real elapsed time since session start
                if (sessionStartTime > 0L) {
                    val elapsedSinceStart = ((getCurrentTimeMillis() - sessionStartTime) / 1000L).coerceAtLeast(0L)
                    if (totalSessionSeconds > elapsedSinceStart) {
                        println("DEBUG: Capping totalSessionSeconds from $totalSessionSeconds to elapsed $elapsedSinceStart to prevent carryover")
                        totalSessionSeconds = elapsedSinceStart
                    }
                }
                val usedMinutes = (totalSessionSeconds / 60L).toInt()
                println("DEBUG: Time limit check - usedMinutes: $usedMinutes, timeLimitMinutes: $timeLimitMinutes, sessionAppUsageTimes: $sessionAppUsageTimes")
                if (usedMinutes >= timeLimitMinutes) {
                    // Before pausing, merge the session into lifetime so UI shows correctly on Pause/Dashboard
                    finalizeSessionUsage()
                    isTracking = false
                    isPaused = true
                    // Update accessibility service with blocked state
                    updateAccessibilityServiceBlockedState(isPaused, getAllTrackedAppIdentifiers(trackedApps), timeLimitMinutes)
                    // Start compliant app blocking using notifications
                    startWellbeingMonitoring(getAllTrackedAppIdentifiers(trackedApps), timeLimitMinutes)
                    // Save blocked state to storage
                    coroutineScope.launch {
                        storage.saveBlockedState(true)
                    }
                    // Show persistent blocking notification
                    showPersistentWellbeingNotification(getAllTrackedAppIdentifiers(trackedApps), timeLimitMinutes)
                    route = Route.Pause
                    // Redirect user to our pause screen when time limit is reached
                    showPauseScreen("Take a mindful pause - you've reached your time limit of ${timeLimitMinutes} minutes")
                }
            }
        }
    }

    // Function to handle app changes and update usage times
    fun handleAppChange(newPackageName: String?) {
        // On the first app seen right after session start, do not attribute any past time
        if (sessionJustStarted) {
            val now = getCurrentTimeMillis()
            currentForegroundApp = newPackageName
            appActiveSince = if (newPackageName != null) now else 0L
            sessionJustStarted = false
            println("DEBUG: handleAppChange - sessionJustStarted: initialized foreground tracking for $newPackageName at $now without attributing past time")
            return
        }
        println("DEBUG: handleAppChange called - newPackageName: $newPackageName, isTracking: $isTracking, route: $route, isPaused: $isPaused")
        if (!isTracking || route == Route.Pause || isPaused) return
        
        val currentTime = getCurrentTimeMillis()
        
        // If we were tracking a previous app, add the time spent to its usage
        println("DEBUG: handleAppChange - checking time tracking: currentForegroundApp=$currentForegroundApp, appActiveSince=$appActiveSince, newPackageName=$newPackageName")
        if (currentForegroundApp != null && appActiveSince > 0) {
            val timeSpent = (currentTime - appActiveSince) / 1000L // Convert to seconds
            println("DEBUG: handleAppChange - timeSpent=$timeSpent seconds")
            if (timeSpent > 0) {
                val updatedSessionUsage = sessionAppUsageTimes.toMutableMap()
                
                // Find which tracked app was active and add the time
                for (app in trackedApps) {
                    val expectedPackage = getPackageNameForTrackedApp(app)
                    
                    if (currentForegroundApp == expectedPackage) {
                        val currentUsage = updatedSessionUsage[app.name] ?: 0L
                        updatedSessionUsage[app.name] = currentUsage + timeSpent
                        println("DEBUG: Added $timeSpent seconds to ${app.name} (total: ${currentUsage + timeSpent})")
                        break
                    }
                }
                
                sessionAppUsageTimes = updatedSessionUsage
                
                // Persist session data
                coroutineScope.launch {
                    try { 
                        storage.saveSessionAppUsageTimes(sessionAppUsageTimes)
                        println("DEBUG: Saved session app usage times to storage")
                    } catch (e: Exception) {
                        println("DEBUG: Error saving session app usage times: ${e.message}")
                    }
                }
            }
        }
        
        // Also track time if we're staying in the same tracked app (for apps like Instagram that generate frequent events)
        if (currentForegroundApp == newPackageName && newPackageName != null && appActiveSince > 0) {
            val elapsedSeconds = (currentTime - appActiveSince) / 1000L // Convert to seconds
            if (elapsedSeconds >= 1L) {
                println("DEBUG: handleAppChange - same app time tracking: timeSpent=$elapsedSeconds seconds for $newPackageName")
                val updatedSessionUsage = sessionAppUsageTimes.toMutableMap()
                
                // Find which tracked app is currently active and add the deduplicated time
                for (app in trackedApps) {
                    val expectedPackage = getPackageNameForTrackedApp(app)
                    
                    if (newPackageName == expectedPackage) {
                        val currentUsage = updatedSessionUsage[app.name] ?: 0L
                         // Only add 1 second per call to prevent massive jumps
                         val secondsToAdd = 1L
                         updatedSessionUsage[app.name] = currentUsage + secondsToAdd
                         println("DEBUG: Added $secondsToAdd seconds to ${app.name} (same app, total: ${currentUsage + secondsToAdd})")
                        break
                    }
                }
                
                sessionAppUsageTimes = updatedSessionUsage
                
                // Persist session data
                coroutineScope.launch {
                    try { 
                        storage.saveSessionAppUsageTimes(sessionAppUsageTimes)
                        println("DEBUG: Saved session app usage times to storage")
                    } catch (e: Exception) {
                        println("DEBUG: Error saving session app usage times: ${e.message}")
                    }
                }
                
                // Move the active-since marker forward only after accruing real elapsed seconds
                appActiveSince = currentTime
            } else {
                // Duplicate/sub-second event; do not reset appActiveSince to preserve elapsed calculation
            }
        }
        
        // Update current app tracking only when the app actually changes
        if (currentForegroundApp != newPackageName) {
            currentForegroundApp = newPackageName
            appActiveSince = currentTime
            println("DEBUG: handleAppChange - set currentForegroundApp: $newPackageName, appActiveSince: $currentTime")
        }
        
        // If the dashboard app itself is the foreground app, do not accrue usage for any apps
        val isDashboardAppForeground = newPackageName != null && (
            newPackageName == "com.luminoprisma.scrollpause" || 
            newPackageName == "com.prismappsau.screengo"
        )
        if (isDashboardAppForeground) {
            println("DEBUG: Dashboard app is foreground ($newPackageName), skipping tracking")
            return
        }
        
        println("DEBUG: App changed to: $newPackageName at $currentTime")
    }

    // Event-driven app usage tracking: Update usage when app changes
    LaunchedEffect(Unit) {
        // Register callback to handle app changes from accessibility service
        setOnAppChangeCallback { newPackageName ->
            handleAppChange(newPackageName)
        }
        
        // Register callback to handle accessibility status changes
        setOnAccessibilityStatusChangeCallback { isEnabled ->
            println("DEBUG: *** ACCESSIBILITY STATUS CHANGED *** isEnabled: $isEnabled")
            if (!isEnabled && isTracking) {
                println("DEBUG: Accessibility disabled - stopping tracking immediately")
                // Finalize current session usage before stopping
                finalizeSessionUsage()
                // Stop tracking
                isTracking = false
                isPaused = false
                // Update accessibility service with unblocked state
                updateAccessibilityServiceBlockedState(isPaused, emptyList(), 0)
                // Save unblocked state to storage
                coroutineScope.launch {
                    try { 
                        storage.saveBlockedState(false)
                        storage.saveTrackingState(false)
                    } catch (e: Exception) {
                        println("DEBUG: Error saving state after accessibility disabled: ${e.message}")
                    }
                }
                // Show notification to user
                showAccessibilityDisabledNotification()
            } else if (isEnabled && !isTracking && !userManuallyStoppedTracking) {
                // Check if both permissions are now granted and auto-start tracking
                coroutineScope.launch {
                    val usagePrefAllowed = withTimeoutOrNull(2000) { storage.getUsageAccessAllowed() } ?: false
                    println("DEBUG: Accessibility enabled - checking for auto-start. Usage pref allowed: $usagePrefAllowed")
                    
                    if (usagePrefAllowed && trackedApps.isNotEmpty()) {
                        // Check if we have QR codes (either current or saved)
                        val hasAnySavedQr = withTimeoutOrNull(2000) {
                            storage.getSavedQrCodes().isNotEmpty()
                        } ?: false
                        
                        if (!qrId.isNullOrBlank() || hasAnySavedQr) {
                            println("DEBUG: Both permissions granted and prerequisites met - auto-starting tracking")
                            pendingStartTracking = true
                        }
                    }
                }
            }
        }
        
        // Register callback to handle usage access status changes
        setOnUsageAccessStatusChangeCallback { isGranted ->
            println("DEBUG: *** USAGE ACCESS STATUS CHANGED *** isGranted: $isGranted")
            if (!isGranted && isTracking) {
                println("DEBUG: Usage access disabled - stopping tracking immediately")
                // Finalize current session usage before stopping
                finalizeSessionUsage()
                // Stop tracking
                isTracking = false
                isPaused = false
                // Update accessibility service with unblocked state
                updateAccessibilityServiceBlockedState(isPaused, emptyList(), 0)
                // Save unblocked state to storage
                coroutineScope.launch {
                    try { 
                        storage.saveBlockedState(false)
                        storage.saveTrackingState(false)
                    } catch (e: Exception) {
                        println("DEBUG: Error saving state after usage access disabled: ${e.message}")
                    }
                }
                // Show notification to user
                showUsageAccessDisabledNotification()
            } else if (isGranted && !isTracking && !userManuallyStoppedTracking) {
                // Check if both permissions are now granted and auto-start tracking
                val accessibilityEnabled = isAccessibilityServiceEnabled()
                println("DEBUG: Usage access granted - checking for auto-start. Accessibility enabled: $accessibilityEnabled")
                
                if (accessibilityEnabled && trackedApps.isNotEmpty()) {
                    coroutineScope.launch {
                        // Check if we have QR codes (either current or saved)
                        val hasAnySavedQr = withTimeoutOrNull(2000) {
                            storage.getSavedQrCodes().isNotEmpty()
                        } ?: false
                        
                        if (!qrId.isNullOrBlank() || hasAnySavedQr) {
                            println("DEBUG: Both permissions granted and prerequisites met - auto-starting tracking")
                            pendingStartTracking = true
                        }
                    }
                }
            }
        }
        
        // Start accessibility monitoring
        startAccessibilityMonitoring()
    }


    // Function to set up default apps
    suspend fun setupDefaultApps() {
        println("DEBUG: setupDefaultApps() called")
        isLoadingApps = true
        try {
            val installedApps = installedAppsProvider.getInstalledApps()
            println("DEBUG: Found ${installedApps.size} installed apps")
            
            // Define default apps to track - social media, YouTube, and Chrome
            val defaultAppNames = listOf(
                "Instagram", "TikTok", "Snapchat", "Facebook", "Twitter", "Reddit", 
                "Pinterest", "LinkedIn", "Discord", "Telegram", "WhatsApp",
                "YouTube", "Chrome"
            )
            
            // Filter to only include installed default apps
            val defaultTrackedApps = installedApps
                .filter { app -> defaultAppNames.any { defaultName -> 
                    app.appName.contains(defaultName, ignoreCase = true) || 
                    defaultName.contains(app.appName, ignoreCase = true)
                }}
                .map { app -> TrackedApp(app.appName, 0, 15) } // 15 minutes default
            
            println("DEBUG: Found ${installedApps.size} total installed apps")
            println("DEBUG: Installed apps: ${installedApps.map { it.appName }}")
            println("DEBUG: Looking for: $defaultAppNames")
            println("DEBUG: Matched ${defaultTrackedApps.size} default apps: ${defaultTrackedApps.map { it.name }}")
            
            trackedApps = defaultTrackedApps
            println("DEBUG: Set up ${trackedApps.size} default tracked apps")
            // Persist selected packages for defaults
            val selectedPackages = installedApps.filter { app ->
                defaultAppNames.any { defaultName ->
                    app.appName.contains(defaultName, ignoreCase = true) ||
                    defaultName.contains(app.appName, ignoreCase = true)
                }
            }.map { it.packageName }
            try { storage.saveSelectedAppPackages(selectedPackages) } catch (_: Exception) {}
            
        } catch (e: Exception) {
            println("DEBUG: Exception occurred while loading apps: ${e.message}")
            e.printStackTrace()
            // Fallback to default apps from comprehensive database
            trackedApps = getDefaultTrackedAppsFromDatabase()
            // Persist fallback package identifiers so the choice survives restarts
            val fallbackPackages = trackedApps.map { getPackageNameForTrackedApp(it) }
            try { storage.saveSelectedAppPackages(fallbackPackages) } catch (_: Exception) {}
        } finally {
            isLoadingApps = false
        }
    }
    
    // Check onboarding completion status on app start
    LaunchedEffect(Unit) {
        try {
            // Register timer reset callback for QR scan (overlay or anywhere)
            setOnTimerResetCallback {
                println("DEBUG: ===== TIMER RESET CALLBACK CALLED (QR SCAN) =====")
                // Ensure overlays are dismissed and user is not considered blocked anymore
                isPaused = false
                // Clear blocked state but preserve tracked apps and time limit for continued tracking
                updateAccessibilityServiceBlockedState(isPaused, getAllTrackedAppIdentifiers(trackedApps), timeLimitMinutes)
                dismissPauseScreen()
                println("DEBUG: QR scan callback - cleared blocked state and dismissed overlay")

                // Use a different approach since coroutineScope might not be available in this context
                try {
                    // 1) Finalize current session usage to credit minutes, then clear session
                    println("DEBUG: QR scan callback - finalizing session usage before dialog")
                    finalizeSessionUsage()

                    // 2) Increment and persist times walked counter
                    timesUnblockedToday += 1
                    println("DEBUG: QR scan callback - incremented times walked to: $timesUnblockedToday")
                    
                    // 2.5) Update day streak counter logic
                    val todayEpochDay = currentEpochDayUtc()
                    val lastStreakUpdateDay = try {
                        runBlocking { withTimeoutOrNull(3000) { storage.getLastStreakUpdateDay() } } ?: 0L
                    } catch (_: Exception) { 0L }
                    
                    // Always increment day streak counter on successful QR scan
                    // This ensures positive feedback even after dismissals
                    if (lastStreakUpdateDay != todayEpochDay) {
                        // First QR scan of a new day: increment streak
                        dayStreakCounter += 1
                        println("DEBUG: QR scan callback - incremented day streak to: $dayStreakCounter (new day)")
                    } else {
                        // Same day: still increment to show progress
                        dayStreakCounter += 1
                        println("DEBUG: QR scan callback - incremented day streak to: $dayStreakCounter (same day)")
                    }
                    
                    try {
                        runBlocking {
                            storage.saveTimesUnblockedToday(timesUnblockedToday)
                            storage.saveDayStreakCounter(dayStreakCounter)
                            storage.saveLastStreakUpdateDay(todayEpochDay)
                        }
                        println("DEBUG: QR scan callback - saved times walked to storage")
                        println("DEBUG: QR scan callback - saved day streak to storage: $dayStreakCounter")
                        
                        // Check for milestone achievements
                        when (dayStreakCounter) {
                            7 -> showStreakMilestone("1 Week Streak!")
                            30 -> showStreakMilestone("1 Month Streak!")
                            100 -> showStreakMilestone("100 Days!")
                        }
                    } catch (_: Exception) {}

                    val doNotShow = try {
                        // Use runBlocking to call suspend function in non-coroutine context
                        runBlocking { storage.getDoNotShowCongratulationAgain() }
                    } catch (_: Exception) { false }
                    println("DEBUG: QR scan callback - doNotShowCongratulationAgain=$doNotShow")

                    // Clear session usage and restart tracking after QR scan
                    sessionAppUsageTimes = emptyMap()
                    sessionStartTime = 0L
                    sessionElapsedSeconds = 0L
                    currentForegroundApp = null
                    appActiveSince = 0L
                    println("DEBUG: QR scan callback - cleared session data for fresh start")
                    
                    // Always auto-restart tracking after QR scan
                    pendingStartTracking = true
                    userManuallyStoppedTracking = false
                    route = Route.Dashboard
                    println("DEBUG: QR scan callback - set pendingStartTracking=true for auto-restart")

                    if (!doNotShow) {
                        // Launch platform overlay immediately so it's visible even outside the app
                        println("DEBUG: QR scan callback - launching Congratulations overlay")
                        showCongratulationsOverlay()
                    } else {
                        println("DEBUG: QR scan callback - preference set to skip dialog; auto-restarting tracking")
                    }
                } catch (e: Exception) {
                    println("DEBUG: QR scan callback - error in callback: ${e.message}")
                }
            }
            
            // Register timer reset callback for dismiss button
            setOnDismissCallback {
                println("DEBUG: ===== DISMISS CALLBACK CALLED =====")
                println("DEBUG: Current state - isPaused: $isPaused, isTracking: $isTracking")
                println("DEBUG: sessionAppUsageTimes before: $sessionAppUsageTimes")
                println("DEBUG: timesDismissedToday before: $timesDismissedToday")
                
                // 1. Finalize session usage before resetting (same as QR scan)
                finalizeSessionUsage()
                println("DEBUG: Finalized session usage after dismiss")
                
                // 2. Increment times dismissed counter (different from QR scan)
                timesDismissedToday += 1
                println("DEBUG: Incremented times dismissed counter to: $timesDismissedToday")
                
                // 2.5. Reset day streak counter when dismissing
                dayStreakCounter = 0
                println("DEBUG: Reset day streak counter to: $dayStreakCounter")
                
                // Persist the updated counters
                coroutineScope.launch {
                    try { 
                        storage.saveTimesDismissedToday(timesDismissedToday)
                        storage.saveDayStreakCounter(dayStreakCounter)
                        println("DEBUG: Saved times dismissed counter to storage")
                        println("DEBUG: Saved reset day streak counter to storage")
                    } catch (e: Exception) {
                        println("DEBUG: Error saving counters: ${e.message}")
                    }
                }
                
                // 3. Reset session state but keep tracking ON and keep tracked apps/time limit
                isPaused = false
                // Notify accessibility service to continue timing for current tracked apps
                updateAccessibilityServiceBlockedState(
                    false,
                    getAllTrackedAppIdentifiers(trackedApps),
                    timeLimitMinutes
                )
                // Save unblocked state to storage
                coroutineScope.launch {
                    try { 
                        storage.saveBlockedState(false)
                        println("DEBUG: Saved unblocked state to storage")
                    } catch (e: Exception) {
                        println("DEBUG: Error saving unblocked state: ${e.message}")
                    }
                }
                sessionAppUsageTimes = emptyMap()
                sessionStartTime = 0L
                sessionElapsedSeconds = 0L
                println("DEBUG: Reset session timer and unblocked user")
                
                // Persist reset session data
                coroutineScope.launch {
                    try { 
                        storage.saveSessionAppUsageTimes(emptyMap())
                        storage.saveSessionStartTime(0L)
                        println("DEBUG: Saved reset session data to storage")
                    } catch (e: Exception) {
                        println("DEBUG: Error saving reset session data: ${e.message}")
                    }
                }
                
                // 4. Stay in the user's app; do not navigate to dashboard
                println("DEBUG: Staying in tracked app after dismiss; no dashboard navigation")
                
                // 5. Dismiss any blocking overlays (same as QR scan)
                dismissPauseScreen()
                println("DEBUG: Dismissed blocking overlays")
                
                // 6. Ensure tracking remains active (no toggle needed)
                pendingStartTracking = false
                userManuallyStoppedTracking = false
                println("DEBUG: Ensured tracking remains active after dismiss")
                
                println("DEBUG: ===== DISMISS CALLBACK COMPLETED =====")
            }
            
            // Add a timeout to prevent hanging
            val isOnboardingCompleted = withTimeoutOrNull(5000) {
                storage.isOnboardingCompleted()
            } ?: false // Default to false if timeout occurs
            
            // Restore tracking state and app usage data
            val savedTrackingState = withTimeoutOrNull(3000) { storage.getTrackingState() } ?: false
            val savedAppUsageTimes = withTimeoutOrNull(3000) { storage.getAppUsageTimes() } ?: emptyMap()
            val savedTrackingStartTime = withTimeoutOrNull(3000) { storage.getTrackingStartTime() } ?: 0L
            val savedUsageDay = withTimeoutOrNull(3000) { storage.getUsageDayEpoch() } ?: 0L
            val savedBlockedState = withTimeoutOrNull(3000) { storage.getBlockedState() } ?: false
            val savedTimesUnblockedToday = withTimeoutOrNull(3000) { storage.getTimesUnblockedToday() } ?: 0
            val savedTimesDismissedToday = withTimeoutOrNull(3000) { storage.getTimesDismissedToday() } ?: 0
            val savedDayStreakCounter = withTimeoutOrNull(3000) { storage.getDayStreakCounter() } ?: 0
            val savedLastStreakUpdateDay = withTimeoutOrNull(3000) { storage.getLastStreakUpdateDay() } ?: 0L
            val savedSessionAppUsageTimes = withTimeoutOrNull(3000) { storage.getSessionAppUsageTimes() } ?: emptyMap()
            val savedSessionStartTime = withTimeoutOrNull(3000) { storage.getSessionStartTime() } ?: 0L
            val todayEpochDay = currentEpochDayUtc()
            
            // Debug logging for app usage data loading
            println("DEBUG: App startup - savedAppUsageTimes: $savedAppUsageTimes")
            println("DEBUG: App startup - savedUsageDay: $savedUsageDay, todayEpochDay: $todayEpochDay")
            
            // Restore tracking state with validation
            // If we're blocked but not tracking, this is an inconsistent state
            // that can happen when the app is killed while in Pause screen
            if (savedBlockedState && !savedTrackingState) {
                println("DEBUG: App startup - Inconsistent state detected: isPaused=true but isTracking=false")
                println("DEBUG: App startup - This typically happens when app was killed while in Pause screen")
                println("DEBUG: App startup - Clearing blocked state and resetting to dashboard")
                // Clear the inconsistent blocked state
                isPaused = false
                isTracking = false
                // Save the corrected state
                withTimeoutOrNull(2000) {
                    try {
                        storage.saveBlockedState(false)
                        storage.saveTrackingState(false)
                    } catch (_: Exception) {}
                }
            } else {
                isTracking = savedTrackingState
                isPaused = savedBlockedState
            }
            
            appUsageTimes = savedAppUsageTimes
            trackingStartTime = savedTrackingStartTime
            
            // If we're restoring a tracking state, reset the manual stop flag
            // This allows auto-start when permissions are granted after app restart
            if (savedTrackingState) {
                userManuallyStoppedTracking = false
            }
            
            // Restore dashboard counters
            timesUnblockedToday = savedTimesUnblockedToday
            timesDismissedToday = savedTimesDismissedToday
            dayStreakCounter = savedDayStreakCounter

            // IMPORTANT: Never reuse prior session usage when app restarts.
            // Always start with a clean session and persist that choice to avoid race conditions
            // where state-loading could reintroduce stale session usage after we clear at start.
            sessionAppUsageTimes = emptyMap()
            sessionStartTime = 0L
            withTimeoutOrNull(2000) {
                try {
                    storage.saveSessionAppUsageTimes(emptyMap())
                    storage.saveSessionStartTime(0L)
                } catch (_: Exception) {}
            }
            
            // Update accessibility service with restored blocked state
            if (isPaused) {
                // If we're blocked but have no tracked apps, this is an inconsistent state
                // This can happen when the app was killed while in Pause route
                if (trackedApps.isEmpty()) {
                    println("DEBUG: App startup - Inconsistent state detected: isPaused=true but trackedApps=empty")
                    println("DEBUG: App startup - Clearing blocked state to prevent stuck state")
                    isPaused = false
                    // Save the corrected state
                    withTimeoutOrNull(2000) {
                        try {
                            storage.saveBlockedState(false)
                        } catch (_: Exception) {}
                    }
                    updateAccessibilityServiceBlockedState(false, emptyList(), 0)
                } else {
                    updateAccessibilityServiceBlockedState(isPaused, getAllTrackedAppIdentifiers(trackedApps), timeLimitMinutes)
                }
            } else if (isTracking && trackedApps.isNotEmpty()) {
                // If we're tracking but not blocked, still send tracked apps to accessibility service
                // so it can track time and trigger blocking when time limit is reached
                println("DEBUG: App startup - Updating accessibility service with tracking state (not blocked yet)")
                updateAccessibilityServiceBlockedState(false, getAllTrackedAppIdentifiers(trackedApps), timeLimitMinutes)
            } else {
                updateAccessibilityServiceBlockedState(isPaused, emptyList(), 0)
            }

            // Daily reset if needed (on app startup)
            if (savedUsageDay == 0L) {
                // First run: set today as the usage day
                lastKnownDay = todayEpochDay
                withTimeoutOrNull(2000) { storage.saveUsageDayEpoch(todayEpochDay) }
            } else if (savedUsageDay != todayEpochDay) {
                // New day detected on app startup: reset today's counters
                println("DEBUG: App startup - new day detected, resetting daily statistics")
                trackedApps = trackedApps.map { it.copy(minutesUsed = 0) }
                appUsageTimes = emptyMap()
                timesUnblockedToday = 0
                timesDismissedToday = 0
                sessionAppUsageTimes = emptyMap()
                sessionStartTime = 0L
                sessionCreditedMinutes = emptyMap()
                lastKnownDay = todayEpochDay
                
                // Check if there's a gap in days that should reset the streak
                val daysDifference = todayEpochDay - savedUsageDay
                if (daysDifference > 1) {
                    // More than 1 day gap: reset day streak
                    dayStreakCounter = 0
                    println("DEBUG: Daily reset - gap of $daysDifference days, reset day streak to 0")
                } else {
                    println("DEBUG: Daily reset - consecutive day, day streak remains: $dayStreakCounter")
                }
                
                withTimeoutOrNull(2000) {
                    storage.saveAppUsageTimes(appUsageTimes)
                    storage.saveUsageDayEpoch(todayEpochDay)
                    storage.saveTimesUnblockedToday(0)
                    storage.saveTimesDismissedToday(0)
                    storage.saveDayStreakCounter(dayStreakCounter)
                    storage.saveSessionAppUsageTimes(emptyMap())
                    storage.saveSessionStartTime(0L)
                }
            } else {
                // Same day: update lastKnownDay to current day
                lastKnownDay = todayEpochDay
            }
            
            // Do not add background elapsed time to usage; only count active foreground session increments
            
            if (isOnboardingCompleted) {
                // Load persisted selections and time limit
                val savedTime = withTimeoutOrNull(3000) { storage.getTimeLimitMinutes() } ?: 15
                timeLimitMinutes = savedTime
                println("DEBUG: Loaded time limit from storage: $timeLimitMinutes minutes")

                val savedPackages = withTimeoutOrNull(3000) { storage.getSelectedAppPackages() } ?: emptyList()
                if (savedPackages.isNotEmpty()) {
                    try {
                        val installed = installedAppsProvider.getInstalledApps()
                        val selected = installed.filter { it.packageName in savedPackages.toSet() }
                        trackedApps = selected.map { TrackedApp(it.appName, 0, timeLimitMinutes) }
                        // Rehydrate minutes used for today from persisted seconds
                        println("DEBUG: Rehydrating tracked apps - appUsageTimes: $appUsageTimes")
                        println("DEBUG: Current timeLimitMinutes: $timeLimitMinutes")
                        trackedApps = trackedApps.map { app ->
                            val seconds = appUsageTimes[app.name] ?: 0L
                            val minutes = (seconds / 60L).toInt() // Remove the cap - show actual usage
                            println("DEBUG: App ${app.name} - seconds: $seconds, minutes: $minutes, limit: ${app.limitMinutes}")
                            app.copy(minutesUsed = minutes)
                        }
                        println("DEBUG: Rehydrated tracked apps: ${trackedApps.map { "${it.name}: ${it.minutesUsed}m" }}")
                        // Pre-populate available apps to reflect saved selection when opening selection screen later
                        availableApps = installed.map { installedApp ->
                            val selectedSet = savedPackages.toSet()
                            AvailableApp(
                                name = installedApp.appName,
                                category = installedApp.category,
                                icon = installedApp.icon,
                                packageName = installedApp.packageName,
                                isSelected = installedApp.packageName in selectedSet
                            )
                        }
                    } catch (_: Exception) {
                        // Fallback to defaults if loading installed apps fails
                        setupDefaultApps()
                    }
                } else {
                    // No saved selection: set up defaults
                    setupDefaultApps()
                    // Rehydrate minutes used for today from persisted seconds
                    println("DEBUG: Rehydrating default tracked apps - appUsageTimes: $appUsageTimes")
                    println("DEBUG: Current timeLimitMinutes: $timeLimitMinutes")
                    trackedApps = trackedApps.map { app ->
                        val seconds = appUsageTimes[app.name] ?: 0L
                        val minutes = (seconds / 60L).toInt() // Remove the cap - show actual usage
                        println("DEBUG: Default app ${app.name} - seconds: $seconds, minutes: $minutes, limit: ${app.limitMinutes}")
                        app.copy(minutesUsed = minutes)
                    }
                    println("DEBUG: Rehydrated default tracked apps: ${trackedApps.map { "${it.name}: ${it.minutesUsed}m" }}")
                }
                
                // Check if notifications are disabled and show permission dialog
                val notificationsEnabled = withTimeoutOrNull(3000) { storage.getNotificationsEnabled() } ?: false
                if (!notificationsEnabled) {
                    showNotificationDialog = true
                }
                
                // Ensure we have tracked apps even if somehow we don't
                if (trackedApps.isEmpty()) {
                    setupDefaultApps()
                }
                route = Route.Dashboard
            } else {
                // If onboarding is not completed, show onboarding
                route = Route.Onboarding
            }
        } catch (e: Exception) {
            // If storage fails, default to onboarding
            route = Route.Onboarding
        }
    }
    
    // Load installed apps when navigating to AppSelection
    LaunchedEffect(route) {
        if (route == Route.AppSelection && !isLoadingApps) {
            // Reset apps when navigating to AppSelection to ensure fresh load
            if (availableApps.isEmpty()) {
                isLoadingApps = true
                try {
                    val installedApps = installedAppsProvider.getInstalledApps()
                    // Debug: Print the number of apps found
                    println("DEBUG: Found ${installedApps.size} installed apps")
                    if (installedApps.isNotEmpty()) {
                        // Use persisted package selections if available
                        val savedPackages = try { storage.getSelectedAppPackages() } catch (_: Exception) { emptyList() }
                        val savedSet = savedPackages.toSet()
                        availableApps = installedApps.map { installedApp ->
                            val isSelected = if (savedSet.isNotEmpty()) installedApp.packageName in savedSet else {
                                // Fallback to matching by current tracked apps
                                trackedApps.any { tracked ->
                                    tracked.name.equals(installedApp.appName, ignoreCase = true) ||
                                    tracked.name.contains(installedApp.appName, ignoreCase = true) ||
                                    installedApp.appName.contains(tracked.name, ignoreCase = true)
                                }
                            }
                            AvailableApp(
                                name = installedApp.appName,
                                category = installedApp.category,
                                icon = installedApp.icon,
                                packageName = installedApp.packageName,
                                isSelected = isSelected
                            )
                        }
                        println("DEBUG: Loaded ${availableApps.size} apps for selection")
                    } else {
                        // If no apps are detected, provide fallback apps from comprehensive database
                        println("DEBUG: No apps detected, using fallback apps from database")
                        val fallback = getDefaultAvailableAppsFromDatabase()
                        availableApps = fallback.map { app ->
                            val isTracked = trackedApps.any { tracked ->
                                tracked.name.equals(app.name, ignoreCase = true) ||
                                tracked.name.contains(app.name, ignoreCase = true) ||
                                app.name.contains(tracked.name, ignoreCase = true)
                            }
                            app.copy(isSelected = isTracked)
                        }
                    }
                } catch (e: Exception) {
                    // If loading fails, provide fallback apps from comprehensive database
                    println("DEBUG: Exception occurred while loading apps: ${e.message}")
                    e.printStackTrace()
                    val fallback = getDefaultAvailableAppsFromDatabase()
                    availableApps = fallback.map { app ->
                        val isTracked = trackedApps.any { tracked ->
                            tracked.name.equals(app.name, ignoreCase = true) ||
                            tracked.name.contains(app.name, ignoreCase = true) ||
                            app.name.contains(tracked.name, ignoreCase = true)
                        }
                        app.copy(isSelected = isTracked)
                    }
                } finally {
                    isLoadingApps = false
                }
            }
        }
    }

    // On first landing on Dashboard per app launch, prompt for disabled permissions
    LaunchedEffect(route) {
        if (route == Route.Dashboard && !hasCheckedPermissionsOnDashboardThisLaunch) {
            val accessibilityAllowed = isAccessibilityServiceEnabled()
            val usagePrefAllowed = withTimeoutOrNull(2000) { storage.getUsageAccessAllowed() } ?: false
            
            // Check if both permissions are already granted and auto-start tracking
            if (accessibilityAllowed && usagePrefAllowed && !isTracking && !userManuallyStoppedTracking && trackedApps.isNotEmpty()) {
                coroutineScope.launch {
                    val hasAnySavedQr = withTimeoutOrNull(2000) {
                        storage.getSavedQrCodes().isNotEmpty()
                    } ?: false
                    
                    if (!qrId.isNullOrBlank() || hasAnySavedQr) {
                        println("DEBUG: Both permissions already granted on dashboard - auto-starting tracking")
                        pendingStartTracking = true
                    }
                }
            } else {
                // Show permission dialogs if needed
                if (!usagePrefAllowed) {
                    showUsageAccessDialog = true
                }
                if (!accessibilityAllowed) {
                    showAccessibilityConsentDialog = true
                }
            }
            hasCheckedPermissionsOnDashboardThisLaunch = true
        }
    }

    // Sequentially handle Start Tracking prerequisites
    LaunchedEffect(pendingStartTracking, showNotificationDialog, showUsageAccessDialog, showNoQrCodeDialog, showNoTrackedAppsDialog, showAccessibilityConsentDialog, showUsageAccessDisableConfirmationDialog, showAccessibilityDisableConfirmationDialog) {
        if (!pendingStartTracking) return@LaunchedEffect

        // If any dialog is currently open, wait until user acts
        if (showNotificationDialog || showUsageAccessDialog || showNoQrCodeDialog || showNoTrackedAppsDialog || showAccessibilityConsentDialog || showUsageAccessDisableConfirmationDialog || showAccessibilityDisableConfirmationDialog) return@LaunchedEffect

        // Check both permissions and show relevant dialogs so the user can allow them
        val accessibilityAllowed = isAccessibilityServiceEnabled()
        val usagePrefAllowed = withTimeoutOrNull(2000) { storage.getUsageAccessAllowed() } ?: false
        println("DEBUG: StartTracking checks - accessibility: $accessibilityAllowed, usagePref: $usagePrefAllowed")
        var showedAnyDialog = false
        
        // Only check internal preference - don't require system permission
        if (!usagePrefAllowed) {
            println("DEBUG: Usage access preference off, showing dialog - usagePrefAllowed: $usagePrefAllowed")
            showUsageAccessDialog = true
            showedAnyDialog = true
        }
        if (!accessibilityAllowed) {
            println("DEBUG: Accessibility off, showing dialog")
            showAccessibilityConsentDialog = true
            showedAnyDialog = true
        }
        if (showedAnyDialog) return@LaunchedEffect

        // 3) QR code - allow existing saved codes to satisfy this
        run {
            val hasAnySavedQr = withTimeoutOrNull(2000) {
                storage.getSavedQrCodes().isNotEmpty()
            } ?: false
            println("DEBUG: Checking QR codes - qrId: '$qrId', hasAnySavedQr: $hasAnySavedQr")
            if (qrId.isNullOrBlank() && !hasAnySavedQr) {
                println("DEBUG: No QR codes available, showing dialog")
                showNoQrCodeDialog = true
                return@LaunchedEffect
            }
        }

        // 4) Also ensure there are tracked apps
        println("DEBUG: Checking tracked apps - count: ${trackedApps.size}")
        if (trackedApps.isEmpty()) {
            println("DEBUG: No tracked apps, showing dialog")
            showNoTrackedAppsDialog = true
            return@LaunchedEffect
        }

        // All checks passed: toggle tracking
        println("DEBUG: All prerequisites passed, toggling tracking from $isTracking to ${!isTracking}")
        if (isTracking) {
            finalizeSessionUsage()
        }
        isTracking = !isTracking
        pendingStartTracking = false
        // Reset manual stop flag when tracking is successfully started
        if (isTracking) {
            userManuallyStoppedTracking = false
            println("DEBUG: *** TRACKING RESTARTED *** sessionStartTime: $sessionStartTime, sessionAppUsageTimes: $sessionAppUsageTimes")
            
            // Start enhanced services only if user has given consent
            if (persistentTrackingConsent) {
                // Start foreground service for persistent monitoring
                startAppMonitoringForegroundService()
                
                // Save tracking state for restart detection
                saveTrackingStateForRestart(isTracking, isPaused, trackedApps.map { it.name }, timeLimitMinutes)
                
                // Update accessibility service with current tracking state
                updateAccessibilityServiceBlockedState(isPaused, trackedApps.map { it.name }, timeLimitMinutes)
            }
        }
        println("DEBUG: Tracking state updated to: $isTracking")
    }

    when (route) {
        null -> {
            // Show loading state while checking onboarding status
            Box(
                modifier = Modifier.fillMaxSize().statusBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AppLogo(size = 120.dp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Loading...", color = Color.White)
                }
            }
        }
        Route.Onboarding -> OnboardingFlow(
            onGetStarted = { 
                coroutineScope.launch {
                    storage.setOnboardingCompleted(true)
                    setupDefaultApps()
                    isSetupMode = true
                    route = Route.QrGenerator
                }
            },
            onSkip = {
                coroutineScope.launch {
                    storage.setOnboardingCompleted(true)
                    setupDefaultApps()
                    isSetupMode = true
                    route = Route.QrGenerator
                }
            }
        )
        Route.QrGenerator -> QrGeneratorScreen(
            message = qrMessage,
            onMessageChange = { qrMessage = it },
            onQrCreated = { id ->
                qrId = id
                route = Route.AppSelection
            },
            onClose = { 
                fromNoQrCodeDialog = false
                route = Route.Dashboard 
            },
            isSetupMode = isSetupMode,
            fromNoQrCodeDialog = fromNoQrCodeDialog
        )
        Route.Dashboard -> DashboardScreen(
            qrId = qrId,
            message = qrMessage,
            trackedApps = trackedApps,
            isTracking = isTracking,
            timeLimitMinutes = timeLimitMinutes,
            sessionAppUsageTimes = sessionAppUsageTimes,
            timesUnblockedToday = timesUnblockedToday,
            timesDismissedToday = timesDismissedToday,
            dayStreakCounter = dayStreakCounter,
            sessionElapsedSeconds = sessionElapsedSeconds,
            onToggleTracking = { 
                println("DEBUG: onToggleTracking called, current isTracking: $isTracking")
                if (isTracking) {
                    // Pause tracking - no dialogs needed
                    println("DEBUG: Pausing tracking")
                    if (isTracking) { finalizeSessionUsage() }
                    isTracking = false
                    pendingStartTracking = false
                    userManuallyStoppedTracking = true
                } else {
                    // Start tracking - check permissions first
                    println("DEBUG: Starting tracking, setting pendingStartTracking = true")
                    pendingStartTracking = true
                    userManuallyStoppedTracking = false
                }
            },
            onOpenQrGenerator = { 
                isSetupMode = false
                route = Route.QrGenerator 
            },
            onOpenAppSelection = { route = Route.AppSelection },
            onOpenPause = { route = Route.Pause },
            onOpenDurationSetting = { route = Route.DurationSetting },
            onOpenSettings = { route = Route.Settings },
            onRemoveTrackedApp = { appName ->
                // Remove from tracked list (robust name matching)
                trackedApps = trackedApps.filterNot { tracked ->
                    tracked.name.equals(appName, ignoreCase = true) ||
                    tracked.name.contains(appName, ignoreCase = true) ||
                    appName.contains(tracked.name, ignoreCase = true)
                }
                // Also toggle off in available apps list so AppSelection reflects it
                availableApps = availableApps.map { app ->
                    val matches = app.name.equals(appName, ignoreCase = true) ||
                        app.name.contains(appName, ignoreCase = true) ||
                        appName.contains(app.name, ignoreCase = true)
                    if (matches) app.copy(isSelected = false) else app
                }
                // Persist updated selection
                coroutineScope.launch {
                    val selectedPackages = availableApps.filter { it.isSelected }.map { it.packageName }
                    storage.saveSelectedAppPackages(selectedPackages)
                }
            },
            onShowTimeRemainingInfo = { showTimeRemainingInfoDialog = true }
        )
        Route.Settings -> SettingsScreen(
            onBack = { route = Route.Dashboard },
            onOpenSavedQrCodes = { route = Route.SavedQrCodes },
            onNotificationsTurnedOff = { },
            onOpenPrivacyPolicy = { route = Route.PrivacyPolicy },
            onOpenPermissions = { route = Route.Permissions },
            onOpenTutorial = { route = Route.Tutorial }
        )
        Route.SavedQrCodes -> SavedQrCodesScreen(
            onBack = { route = Route.Settings },
            onOpenQrGenerator = { route = Route.QrGenerator },
            onViewQr = { id, text, message ->
                viewedQrId = id
                viewedQrText = text
                viewedQrMessage = message
                route = Route.QrDetail
            }
        )
        Route.QrDetail -> QrDetailScreen(
            qrText = viewedQrText.orEmpty(),
            message = viewedQrMessage.orEmpty(),
            onBack = {
                route = Route.SavedQrCodes
            }
        )
        Route.AppSelection -> AppSelectionScreen(
            availableApps = availableApps,
            isLoading = isLoadingApps,
            onAppToggle = { packageName ->
                availableApps = availableApps.map { app ->
                    if (app.packageName == packageName) app.copy(isSelected = !app.isSelected) else app
                }
                
                // Immediately update trackedApps to reflect changes on dashboard
                val selectedApps = availableApps.filter { it.isSelected }
                trackedApps = selectedApps.map { app ->
                    // Try to preserve existing usage data if app was already tracked
                    val existingApp = trackedApps.find { it.name == app.name }
                    TrackedApp(
                        name = app.name,
                        minutesUsed = existingApp?.minutesUsed ?: 0,
                        limitMinutes = existingApp?.limitMinutes ?: timeLimitMinutes
                    )
                }
                // Persist updated selection
                coroutineScope.launch {
                    storage.saveSelectedAppPackages(selectedApps.map { it.packageName })
                }
            },
            onContinue = {
                // Ensure dashboard reflects the current selections even if user didn't toggle
                val selectedApps = availableApps.filter { it.isSelected }
                trackedApps = selectedApps.map { app ->
                    val existingApp = trackedApps.find { it.name == app.name }
                    TrackedApp(
                        name = app.name,
                        minutesUsed = existingApp?.minutesUsed ?: 0,
                        limitMinutes = existingApp?.limitMinutes ?: timeLimitMinutes
                    )
                }
                // Persist selection
                coroutineScope.launch {
                    storage.saveSelectedAppPackages(selectedApps.map { it.packageName })
                }
                route = Route.Dashboard
            },
            onBack = { route = Route.Dashboard }
        )
        Route.DurationSetting -> DurationSettingScreen(
            timeLimitMinutes = timeLimitMinutes,
            onTimeLimitChange = { 
                timeLimitMinutes = it
                // Persist time limit immediately
                coroutineScope.launch { storage.saveTimeLimitMinutes(it) }
                // If tracking is active, immediately push updated limit to Accessibility Service
                if (isTracking) {
                    try {
                        updateAccessibilityServiceBlockedState(
                            isPaused = false,
                            trackedAppNames = getAllTrackedAppIdentifiers(trackedApps),
                            timeLimitMinutes = timeLimitMinutes
                        )
                        // Keep restart state in sync
                        saveTrackingStateForRestart(
                            isTracking,
                            isPaused,
                            trackedApps.map { it.name },
                            timeLimitMinutes
                        )
                    } catch (_: Exception) {}
                }
            },
            onCompleteSetup = {
                // Check if we have selected apps from app selection flow
                val selectedApps = availableApps.filter { it.isSelected }
                if (selectedApps.isNotEmpty()) {
                    // Coming from app selection flow - create new tracked apps
                    trackedApps = selectedApps.map { app ->
                        // Try to preserve existing usage data if app was already tracked
                        val existingApp = trackedApps.find { it.name == app.name }
                        TrackedApp(
                            name = app.name,
                            minutesUsed = existingApp?.minutesUsed ?: 0,
                            limitMinutes = timeLimitMinutes
                        )
                    }
                    // Persist the new selection
                    coroutineScope.launch {
                        storage.saveSelectedAppPackages(selectedApps.map { it.packageName })
                        storage.saveTimeLimitMinutes(timeLimitMinutes)
                    }
                } else {
                    // Coming from dashboard - just update the time limit for existing tracked apps
                    trackedApps = trackedApps.map { app ->
                        app.copy(limitMinutes = timeLimitMinutes)
                    }
                    // Persist the time limit change
                    coroutineScope.launch {
                        storage.saveTimeLimitMinutes(timeLimitMinutes)
                    }
                }
                route = Route.Dashboard
            },
            onBack = { route = Route.Dashboard }
        )
        Route.Pause -> {
            val totalTrackedAppUsageSeconds = sessionAppUsageTimes.values.sum()
            val elapsedMinutes = (totalTrackedAppUsageSeconds / 60L).toInt()
            val hours = elapsedMinutes / 60
            val minutes = elapsedMinutes % 60
            val durationText = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
            
            PauseScreen(
                durationText = durationText,
                timeLimitMinutes = timeLimitMinutes,
                dayStreakCounter = dayStreakCounter,
                onScanQr = {
                    coroutineScope.launch {
                        val ok = scanQrAndDismiss(qrMessage)
                        println("DEBUG: onScanQr - scan result ok=$ok")
                        if (ok) {
                            val doNotShow = try {
                                storage.getDoNotShowCongratulationAgain()
                            } catch (e: Exception) {
                                false
                            }
                            println("DEBUG: onScanQr - doNotShowCongratulationAgain=$doNotShow")

                            // Force show for now to confirm UX; preference still saved from dialog
                            showCongratulationDialog = true
                            println("DEBUG: onScanQr - showCongratulationDialog set to true")
                        }
                    }
                },
                onClose = { 
                    println("DEBUG: PauseScreen onClose called - showing ad before dismiss")
                    handleDismissWithAd()
                }
            )
        }
        Route.PrivacyPolicy -> PrivacyPolicyScreen(
            onBack = { route = Route.Settings }
        )
        Route.Permissions -> PermissionsScreen(
            onBack = { route = Route.Settings },
            isTracking = isTracking,
            showUsageAccessDisableConfirmationDialog = showUsageAccessDisableConfirmationDialog,
            showAccessibilityDisableConfirmationDialog = showAccessibilityDisableConfirmationDialog,
            onShowUsageAccessDisableConfirmationDialog = { showUsageAccessDisableConfirmationDialog = true },
            onShowAccessibilityDisableConfirmationDialog = { showAccessibilityDisableConfirmationDialog = true },
            onShowUsageAccessDialog = { showUsageAccessDialog = true }
        )
        Route.Tutorial -> HowToUseScreen(
            onBack = { route = Route.Settings }
        )
    }
    
    // Notification Dialog (existing)
    if (showNotificationDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showNotificationDialog = false; pendingStartTracking = false }) {
            Card(
                backgroundColor = Color(0xFF1A1A1A),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        "🔔",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Enable Notifications?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Column {
                        Text(
                            "Stay informed about your app usage limits and take mindful breaks when needed.",
                            color = Color.White,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Start
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "You can change this setting anytime in Settings.",
                            color = Color(0xFFD1D5DB),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Start
                        )
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    
                    // Buttons with custom 3dp spacing
                    Button(
                        onClick = { 
                            coroutineScope.launch {
                                try { storage.saveNotificationsEnabled(true) } catch (_: Exception) {}
                            }
                            showNotificationDialog = false 
                            if (pendingStartTracking) {
                                // Continue Start Tracking flow
                                pendingStartTracking = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2C4877)),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Enable now", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(Modifier.height(3.dp))
                    
                    TextButton(
                        onClick = { showNotificationDialog = false; pendingStartTracking = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Not now", color = Color.White, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }

    // Usage Access Permission Dialog
    if (showUsageAccessDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showUsageAccessDialog = false; pendingStartTracking = false }) {
            Card(
                backgroundColor = Color(0xFF1A1A1A),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        "\uD83D\uDCC9",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Allow App Usage Access?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Column {
                        Text(
                            "We need permission to read your app usage so tracking works.",
                            color = Color.White,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Start
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "You can change this anytime in Settings.",
                            color = Color(0xFFD1D5DB),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Start
                        )
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    
                    // Buttons with custom 3dp spacing
                    Button(
                        onClick = {
                            // In-app enable: set preference true and close
                            coroutineScope.launch { try { storage.saveUsageAccessAllowed(true) } catch (_: Exception) {} }
                            showUsageAccessDialog = false
                            if (pendingStartTracking) {
                                // Continue Start Tracking flow
                                pendingStartTracking = true
                            } else {
                                // Check if we should auto-start tracking now that both permissions are granted
                                val accessibilityEnabled = isAccessibilityServiceEnabled()
                                if (accessibilityEnabled && !isTracking && !userManuallyStoppedTracking && trackedApps.isNotEmpty()) {
                                    // Check if we have QR codes (either current or saved)
                                    coroutineScope.launch {
                                        val hasAnySavedQr = withTimeoutOrNull(2000) {
                                            storage.getSavedQrCodes().isNotEmpty()
                                        } ?: false
                                        
                                        if (!qrId.isNullOrBlank() || hasAnySavedQr) {
                                            println("DEBUG: Both permissions granted via dialog - auto-starting tracking")
                                            pendingStartTracking = true
                                        }
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2C4877)),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Allow", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(Modifier.height(3.dp))
                    
                    TextButton(
                        onClick = { showUsageAccessDialog = false; pendingStartTracking = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Not now", color = Color.White, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }

    // Accessibility Consent Dialog (shown at end of Start Tracking flow)
    if (showAccessibilityConsentDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showAccessibilityConsentDialog = false; pendingStartTracking = false }) {
            Card(
                backgroundColor = Color(0xFF1A1A1A),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        "\uD83D\uDC20",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Enable Accessibility?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Column {
                        Text(
                            "We use accessibility to detect which app is in the foreground for accurate tracking.",
                            color = Color.White,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "You can turn this off anytime in Settings.",
                            color = Color(0xFFD1D5DB),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    
                    // Buttons with custom 3dp spacing
                    Button(
                        onClick = {
                            openAccessibilitySettings()
                            showAccessibilityConsentDialog = false 
                            if (pendingStartTracking) {
                                // Continue Start Tracking flow
                                pendingStartTracking = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2C4877)),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) { 
                        Text("Allow", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(Modifier.height(3.dp))
                    
                    TextButton(
                        onClick = { showAccessibilityConsentDialog = false; pendingStartTracking = false },
                        modifier = Modifier.fillMaxWidth()
                    ) { 
                        Text("Not now", color = Color.White, textAlign = TextAlign.Center) 
                    }
                }
            }
        }
    }

    // Persistent Tracking Consent Dialog
    PersistentTrackingConsentDialog(
        isVisible = showPersistentTrackingConsentDialog,
        onAllow = {
            persistentTrackingConsent = true
            showPersistentTrackingConsentDialog = false
            coroutineScope.launch {
                try {
                    storage.savePersistentTrackingConsent(true)
                    // Now start the enhanced services
                    startAppMonitoringForegroundService()
                    saveTrackingStateForRestart(isTracking, isPaused, trackedApps.map { it.name }, timeLimitMinutes)
                } catch (e: Exception) {
                    println("DEBUG: Error saving persistent tracking consent: ${e.message}")
                }
            }
        },
        onDeny = {
            persistentTrackingConsent = false
            showPersistentTrackingConsentDialog = false
            coroutineScope.launch {
                try {
                    storage.savePersistentTrackingConsent(false)
                } catch (e: Exception) {
                    println("DEBUG: Error saving persistent tracking consent: ${e.message}")
                }
            }
        },
        onDismiss = {
            showPersistentTrackingConsentDialog = false
        }
    )

    // Usage Access Disable Confirmation Dialog
    if (showUsageAccessDisableConfirmationDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showUsageAccessDisableConfirmationDialog = false }) {
            Card(
                backgroundColor = Color(0xFF1A1A1A),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        "Stop Tracking?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Text(
                        "Turning off App Usage Access will stop tracking immediately. You won't be able to monitor your app usage or enforce time limits until you re-enable this permission.",
                        color = Color.White,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Start
                    )
                    
                    Spacer(Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            // Stop tracking and clear state in App scope, stay in-app
                            try { finalizeSessionUsage() } catch (_: Exception) {}
                            isTracking = false
                            isPaused = false
                            updateAccessibilityServiceBlockedState(isPaused, emptyList(), 0)
                            coroutineScope.launch {
                                try {
                                    storage.saveBlockedState(false)
                                    storage.saveTrackingState(false)
                                    storage.saveUsageAccessAllowed(false)
                                } catch (_: Exception) {}
                            }
                            route = Route.Dashboard
                            showUsageAccessDisableConfirmationDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2C4877)),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Continue", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(Modifier.height(3.dp))
                    
                    TextButton(
                        onClick = { showUsageAccessDisableConfirmationDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) { 
                        Text("Cancel", color = Color.White, textAlign = TextAlign.Center) 
                    }
                }
            }
        }
    }

    // Accessibility Disable Confirmation Dialog
    if (showAccessibilityDisableConfirmationDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showAccessibilityDisableConfirmationDialog = false }) {
            Card(
                backgroundColor = Color(0xFF1A1A1A),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        "Stop Tracking?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Text(
                        "Turning off Accessibility Access will stop tracking immediately. You won't be able to monitor your app usage or enforce time limits until you re-enable this permission.",
                        color = Color.White,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Start
                    )
                    
                    Spacer(Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            showAccessibilityDisableConfirmationDialog = false
                            openAccessibilitySettings()
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2C4877)),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Continue", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(Modifier.height(3.dp))
                    
                    TextButton(
                        onClick = { showAccessibilityDisableConfirmationDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) { 
                        Text("Cancel", color = Color.White, textAlign = TextAlign.Center) 
                    }
                }
            }
        }
    }

    // No Tracked Apps Dialog
    if (showNoTrackedAppsDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showNoTrackedAppsDialog = false }) {
            Card(
                backgroundColor = Color(0xFF1A1A1A),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        "📱 No tracked apps",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Text(
                        "You haven't selected any apps to track yet. Choose which apps to track to start.",
                        color = Color.White,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Start
                    )
                    
                    Spacer(Modifier.height(24.dp))
                    
                    // Buttons with custom 3dp spacing
                    Button(
                        onClick = {
                            showNoTrackedAppsDialog = false
                            pendingStartTracking = false
                            route = Route.AppSelection
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2C4877)),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Choose apps", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(Modifier.height(3.dp))
                    
                    TextButton(
                        onClick = { showNoTrackedAppsDialog = false; pendingStartTracking = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Not now", color = Color.White, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }

    // No QR Code Dialog
    if (showNoQrCodeDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showNoQrCodeDialog = false }) {
            Card(
                backgroundColor = Color(0xFF1A1A1A),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        "🧾 QR code required",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Text(
                        "You need a QR code to track your apps. Generate one to get started.",
                        color = Color.White,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Start
                    )
                    
                    Spacer(Modifier.height(24.dp))
                    
                    // Buttons with custom 3dp spacing
                    Button(
                        onClick = {
                            showNoQrCodeDialog = false
                            pendingStartTracking = false
                            fromNoQrCodeDialog = true
                            route = Route.QrGenerator
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2C4877)),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Create QR code", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(Modifier.height(3.dp))
                    
                    TextButton(
                        onClick = { 
                            showNoQrCodeDialog = false 
                            pendingStartTracking = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Not now", color = Color.White, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
    
    // Time Remaining Info Dialog
    if (showTimeRemainingInfoDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showTimeRemainingInfoDialog = false }) {
            Card(
                backgroundColor = Color(0xFF1A1A1A),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        "Time Remaining",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Text(
                        "This is the time until your selected apps are blocked. When the time runs out, you will have to physically walk and scan your QR code to unblock your apps, then you can use them again.",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    
                    Spacer(Modifier.height(24.dp))
                    
                    Button(
                        onClick = { showTimeRemainingInfoDialog = false },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1E3A5F)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Got it", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    
    // Congratulatory Dialog
    if (showCongratulationDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { /* disabled */ },
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            // Full screen dialog with dark blue background only
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1A1A1A)) // Dark blue background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Congratulations title
                    Text(
                        "Congratulations!",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(Modifier.height(32.dp))
                    
                    // Badge icon
                    BadgeIcon(
                        size = 150.dp,
                        number = dayStreakCounter
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Days without doomscrolling text
                    Text(
                        if (dayStreakCounter == 1) "day without doomscrolling" else "days without doomscrolling",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(Modifier.height(32.dp))
                    
                    // Message subheading with random variation
                    val congratulationMessage = getRandomCongratulationMessage()
                    Text(
                        congratulationMessage,
                        color = Color.White,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    
                    Spacer(Modifier.height(48.dp))
                    
                    // Close button -> back to last tracked app
                    Button(
                        onClick = {
                            showCongratulationDialog = false
                            // Finalize session and auto-restart tracking
                            handleQrScanSuccess()
                            pendingStartTracking = true
                            userManuallyStoppedTracking = false
                            // Attempt to return to last tracked app
                            val identifiers = getAllTrackedAppIdentifiers(trackedApps)
                            openLastTrackedApp(identifiers)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF2C2C2C)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "Close",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    // Ensure overlay version is shown even if app is backgrounded
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        try { showCongratulationsOverlay() } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    // Dismiss Dialog
    if (showDismissDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = {
            showDismissDialog = false
        }) {
            Card(
                backgroundColor = Color(0xFF1A1A1A),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        "Dismiss Pause?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Column {
                        Text(
                            "Do you want the app to auto-restart tracking after dismissing?",
                            color = Color.White,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Start
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { doNotShowDismissAgain = !doNotShowDismissAgain }
                        ) {
                            RadioButton(
                                selected = doNotShowDismissAgain,
                                onClick = { doNotShowDismissAgain = !doNotShowDismissAgain },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF2C4877),
                                    unselectedColor = Color(0xFF9CA3AF)
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Do not show again", color = Color.White, fontSize = 14.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material.Checkbox(
                                checked = autoRestartOnDismiss,
                                onCheckedChange = { checked -> autoRestartOnDismiss = checked },
                                colors = androidx.compose.material.CheckboxDefaults.colors(checkedColor = Color(0xFF2C4877), uncheckedColor = Color(0xFF9CA3AF))
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Auto-restart tracking after dismiss", color = Color.White, fontSize = 14.sp)
                        }
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    
                    // Buttons with custom 3dp spacing
                    Button(
                        onClick = {
                            showDismissDialog = false
                            coroutineScope.launch {
                                try { storage.saveDoNotShowDismissAgain(doNotShowDismissAgain) } catch (_: Exception) {}
                            }
                            // Show mandatory ad - user remains blocked until ad completes
                            handleDismissWithAd()
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2C4877)),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Continue", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(Modifier.height(3.dp))
                    
                    TextButton(
                        onClick = {
                            showDismissDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel", color = Color.White, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}


// QR generator screen
@Composable
private fun QrGeneratorScreen(
    message: String,
    onMessageChange: (String) -> Unit,
    onQrCreated: (String) -> Unit,
    onClose: () -> Unit,
    isSetupMode: Boolean,
    fromNoQrCodeDialog: Boolean = false
) {
    val storage = remember { createAppStorage() }
    val coroutineScope = rememberCoroutineScope()
    
    QrGeneratorContent(
        message = message,
        onMessageChange = onMessageChange,
        onDownloadPdf = { text, pauseId ->
            // Save a PDF using a platform stub and then continue
            val filePath = saveQrPdf(qrText = text, message = message)
            
            // Save the QR code to storage for later validation
            coroutineScope.launch {
                val qrCode = SavedQrCode(
                    id = pauseId,
                    qrText = text,
                    message = message,
                    createdAt = getCurrentTimeMillis(),
                    isActive = true
                )
                storage.saveQrCode(qrCode)
            }
            
            // Note: In a real app, you might want to show a toast or notification here
            // indicating the file was saved to the specified path
        },
        onGenerate = {
            // Create a simple unique id
            val id = "pause-${kotlin.random.Random.nextLong()}"
            onQrCreated(id)
        },
        onClose = onClose,
        isSetupMode = isSetupMode,
        fromNoQrCodeDialog = fromNoQrCodeDialog
    )
}

// Dashboard
@Composable
private fun DashboardScreen(
    qrId: String?,
    message: String,
    trackedApps: List<TrackedApp>,
    isTracking: Boolean,
    timeLimitMinutes: Int,
    sessionAppUsageTimes: Map<String, Long>,
    timesUnblockedToday: Int,
    timesDismissedToday: Int,
    dayStreakCounter: Int,
    sessionElapsedSeconds: Long,
    onToggleTracking: () -> Unit,
    onOpenQrGenerator: () -> Unit,
    onOpenAppSelection: () -> Unit,
    onOpenPause: () -> Unit,
    onOpenDurationSetting: () -> Unit,
    onOpenSettings: () -> Unit,
    onRemoveTrackedApp: (String) -> Unit,
    onShowTimeRemainingInfo: () -> Unit
) {
    DashboardContent(
        qrId = qrId ?: "",
        message = message,
        trackedApps = trackedApps,
        isTracking = isTracking,
        timeLimitMinutes = timeLimitMinutes,
        sessionAppUsageTimes = sessionAppUsageTimes,
        timesUnblockedToday = timesUnblockedToday,
        timesDismissedToday = timesDismissedToday,
        sessionElapsedSeconds = sessionElapsedSeconds,
        onToggleTracking = onToggleTracking,
        onOpenQrGenerator = onOpenQrGenerator,
        onOpenAppSelection = onOpenAppSelection,
        onOpenPause = onOpenPause,
        onOpenDurationSetting = onOpenDurationSetting,
        onOpenSettings = onOpenSettings,
        onRemoveTrackedApp = onRemoveTrackedApp,
        onShowTimeRemainingInfo = onShowTimeRemainingInfo
    )
}

// Expect declarations implemented per platform
expect fun getPlatformName(): String
expect fun saveQrPdf(qrText: String, message: String): String
// Extension points for platform features (no-op defaults in platform sources)
expect fun startUsageTracking(
    trackedPackages: List<String>,
    limitMinutes: Int,
    onLimitReached: () -> Unit
)
expect fun showPauseScreen(message: String)
expect fun dismissPauseScreen()
expect fun checkAndRedirectToPauseIfBlocked(trackedAppNames: List<String>, isPaused: Boolean, timeLimitMinutes: Int)
expect suspend fun scanQrAndDismiss(expectedMessage: String): Boolean
expect fun getCurrentTimeMillis(): Long
expect fun setOnTimerResetCallback(callback: (() -> Unit)?)
expect fun setOnDismissCallback(callback: (() -> Unit)?)
expect fun showCongratulationsOverlay()
expect fun showStreakMilestone(milestone: String)
expect fun updateAccessibilityServiceBlockedState(isPaused: Boolean, trackedAppNames: List<String>, timeLimitMinutes: Int)
expect fun openLastTrackedApp(trackedAppIdentifiers: List<String>)
expect fun openEmailClient(recipient: String)
expect fun hasCameraPermission(): Boolean
expect fun requestCameraPermission(): Boolean
expect fun openAppSettingsForCamera()
expect fun showAccessibilityDisabledNotification()
expect fun showUsageAccessDisabledNotification()
expect fun setOnAppChangeCallback(callback: ((String?) -> Unit)?)
expect fun setOnAccessibilityStatusChangeCallback(callback: ((Boolean) -> Unit)?)
expect fun startAccessibilityMonitoring()
expect fun isUsageAccessPermissionGranted(): Boolean
expect fun setOnUsageAccessStatusChangeCallback(callback: ((Boolean) -> Unit)?)
expect fun startAppMonitoringForegroundService()
expect fun stopAppMonitoringForegroundService()
expect fun saveTrackingStateForRestart(isTracking: Boolean, isPaused: Boolean, trackedApps: List<String>, timeLimit: Int)
expect fun showPersistentWellbeingNotification(trackedApps: List<String>, timeLimit: Int)
expect fun clearPersistentWellbeingNotification()
expect fun startWellbeingMonitoring(trackedApps: List<String>, timeLimit: Int)
expect fun stopWellbeingMonitoring()
expect fun isWellbeingMonitoringEnabled(): Boolean

// Enhanced QR scanning function that validates against saved QR codes
suspend fun scanQrAndValidate(storage: AppStorage): Boolean {
    // This would be called from platform-specific implementations
    // For now, we'll implement a simple validation flow
    return false // Placeholder - will be implemented in platform layers
}

// Simple date formatting function
private fun formatDate(timestamp: Long): String {
    // Simple date formatting - just show relative time for now
    val now = getCurrentTimeMillis()
    val diff = now - timestamp
    val days = diff / (24 * 60 * 60 * 1000)
    
    return when {
        days == 0L -> "Today"
        days == 1L -> "Yesterday"
        days < 7L -> "$days days ago"
        days < 30L -> "${days / 7} weeks ago"
        else -> "${days / 30} months ago"
    }
}

// Expect actual QR code display. Implemented per platform.
@Composable
expect fun QrCodeDisplay(
    text: String,
    modifier: Modifier = Modifier
)

// UI implementations for onboarding, QR, and dashboard live in this file for brevity
// Lightweight, dependency-free visuals only

@Composable
private fun OnboardingFlow(
    onGetStarted: () -> Unit,
    onSkip: () -> Unit
) {
    OnboardingPager(
        pages = listOf(
            OnboardingPage(
                title = "Welcome to\nScroll Pause",
                description = "Take control of your screen time.\n" +
                        "Set limits for your apps and get moving when it's time for a break",
                showLogo = true
            ),
            OnboardingPage(
                title = "Walk to Unlock Apps",
                description = "Place QR codes around your space. \nWhen it’s time to pause, walk to scan a QR code and unlock your apps — a quick break, a clearer mind",
                imagePath = "images/onboarding/walking_onboarding.png"
            ),
            OnboardingPage(
                title = "Pause Partners",
                description = "Invite trusted friends to hold your QR codes and keep you accountable. They’ll hold your QR codes and help you stay mindful when unlocking apps.",
                imagePath = "images/onboarding/two_people.png"
            ),
            OnboardingPage(
                title = "Ad Free when you Pause Mindfully",
                description = "You'll never see ads if you walk to scan your QR code!",
                primaryCta = "Get Started",
                imagePath = "images/onboarding/ad free.png"
            )
        ),
        onDone = onGetStarted,
        onSkip = onSkip
    )
}

@Immutable
private data class OnboardingPage(
    val title: String,
    val description: String,
    val primaryCta: String = "Next",
    val showLogo: Boolean = false,
    val imagePath: String? = null
)

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun OnboardingPager(
    pages: List<OnboardingPage>,
    onDone: () -> Unit,
    onSkip: () -> Unit
) {
    var index by remember { mutableStateOf(0) }
    var offsetX by remember { mutableStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
            .statusBarsPadding()
            .padding(24.dp)
    ) {
        // Progress indicators at top
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pages.size) { i ->
                Box(
                    modifier = Modifier
                        .width(if (i == index) 24.dp else 8.dp)
                        .height(8.dp)
                        .background(
                            if (i == index) Color(0xFF1E3A5F) else Color(0xFF4B5563),
                            RoundedCornerShape(4.dp)
                        )
                )
                if (i < pages.lastIndex) Spacer(Modifier.width(8.dp))
            }
        }

        Spacer(Modifier.height(32.dp))

        // Swipeable content area (centered)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            // Determine if swipe was significant enough to change page
                            val threshold = size.width * 0.2f // 20% of screen width
                            when {
                                offsetX > threshold && index > 0 -> {
                                    // Swipe right - go to previous page
                                    index--
                                }
                                offsetX < -threshold && index < pages.lastIndex -> {
                                    // Swipe left - go to next page
                                    index++
                                }
                            }
                            offsetX = 0f
                        }
                    ) { _, dragAmount ->
                        // Only allow horizontal swiping
                        offsetX += dragAmount.x
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Main content card with swipe offset
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(offsetX.roundToInt(), 0) },
                backgroundColor = Color(0xFF1E3A5F),
                shape = RoundedCornerShape(16.dp)
            ) {
                val page = pages[index]
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Show logo on first page, otherwise show illustration
                    if (page.showLogo) {
                        AppLogo(size = 200.dp)
                    } else if (page.imagePath != null) {
                        // Show custom image for the page
                        Image(
                            painter = painterResource(page.imagePath),
                            contentDescription = "Onboarding illustration",
                            modifier = Modifier.size(200.dp)
                        )
                    } else {
                        // Fallback illustration
                        Image(
                            painter = painterResource("images/onboarding/mindful_breaks.png"),
                            contentDescription = "Mindful breaks illustration",
                            modifier = Modifier.size(200.dp)
                        )
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    
                    Text(
                        text = page.title,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Text(
                        text = page.description,
                        fontSize = 16.sp,
                        color = Color(0xFFD1D5DB),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Action buttons at bottom
        Column(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    if (index < pages.lastIndex) index++ else onDone()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1E3A5F)),
                contentPadding = PaddingValues(vertical = 16.dp),
                shape = RoundedCornerShape(12.dp)
            ) { 
                Text(
                    pages[index].primaryCta,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                ) 
            }
            // Only show Skip button if not on the last page
            if (index < pages.lastIndex) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Skip",
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable { onSkip() },
                    color = Color(0xFFD1D5DB),
                    fontSize = 14.sp
                )
            }
            
            // Add spacer at bottom to push buttons up (1/8 of screen height)
            val screenHeight = LocalConfiguration.current.screenHeightDp.dp
            Spacer(Modifier.height(screenHeight / 8))
        }
    }
}

@Composable
private fun QrGeneratorContent(
    message: String,
    onMessageChange: (String) -> Unit,
    onDownloadPdf: (String, String) -> Unit,
    onGenerate: () -> Unit,
    onClose: () -> Unit,
    isSetupMode: Boolean,
    fromNoQrCodeDialog: Boolean = false
) {
    val storage = remember { createAppStorage() }
    val coroutineScope = rememberCoroutineScope()
    var qrVersion by remember { mutableStateOf(1) } // Start at 1 so QR shows initially
    var hasGeneratedQr by remember { mutableStateOf(true) } // Auto-generate QR on page load
    var downloadSuccess by remember { mutableStateOf(false) }
    var showAccountabilityDialog by remember { mutableStateOf(false) }
    var isFirstVisit by remember { mutableStateOf(true) }
    
    // Generate a unique pause ID for this session
    var pauseId by remember { mutableStateOf("pause-${kotlin.random.Random.nextLong()}") }
    val qrText = remember(pauseId, qrVersion) { "QR:$pauseId:v$qrVersion" }

    // Check if this is the first visit
    LaunchedEffect(Unit) {
        isFirstVisit = !storage.getQrGeneratorVisited()
        if (isFirstVisit) {
            coroutineScope.launch {
                storage.saveQrGeneratorVisited(true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
            .statusBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "×",
                fontSize = 24.sp,
                color = Color.White,
                modifier = Modifier.clickable { onClose() }
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(if (isSetupMode) "Set Up: QR Code Generator" else "QR Code Generator", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Create QR codes to place around your home or share with your Scroll Pause partner", fontSize = 14.sp, color = Color(0xFFD1D5DB))
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // QR Code Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(13.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("▦", fontSize = 16.sp, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Your QR Code", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                
                Spacer(Modifier.height(16.dp))
                
                // QR Code - only shows after generation
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .background(Color.White, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasGeneratedQr) {
                        QrCodeDisplay(
                            text = qrText,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text("Click 'Generate New QR Code' to create your QR", 
                             color = Color(0xFF8C9C8D), 
                             fontSize = 14.sp,
                             textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
                
                if (hasGeneratedQr) {
                    Spacer(Modifier.height(8.dp))
                    Text(message, color = Color.White, fontSize = 16.sp)
                    
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ID: ${pauseId.takeLast(6)}...", color = Color(0xFFD1D5DB), fontSize = 12.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("⧉", color = Color(0xFFD1D5DB), fontSize = 12.sp)
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    // Generate New QR Code Button inside the card
                    Button(
                        onClick = { 
                            // Generate a new pause ID for a new QR code
                            pauseId = "pause-${kotlin.random.Random.nextLong()}"
                            qrVersion++
                            hasGeneratedQr = true
                            downloadSuccess = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1A1A1A)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text("↻", color = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Generate New QR Code", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Customize Message Card (more condensed)
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Customize Message", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = onMessageChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    maxLines = 1,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF1E3A5F),
                        unfocusedBorderColor = Color(0xFF4B5563),
                        textColor = Color.White,
                        cursorColor = Color(0xFF1E3A5F)
                    ),
                    shape = RoundedCornerShape(6.dp)
                )
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Download Button - only enabled when QR is generated
        Button(
            onClick = { 
                if (downloadSuccess) {
                    // If already downloaded, navigate to dashboard
                    onClose()
                } else {
                    // Download PDF first
                    onDownloadPdf(qrText, pauseId)
                    downloadSuccess = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if (hasGeneratedQr) Color(0xFF1E3A5F) else Color(0xFF4B5563)
            ),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            enabled = hasGeneratedQr
        ) {
            Text(
                if (downloadSuccess) {
                    if (isSetupMode) "Continue" else "Go to Dashboard"
                } else if (isFirstVisit) "Save QR Code" else "Download", 
                color = Color.White, 
                fontWeight = FontWeight.Bold
            )
        }
        
        // Success message
        if (downloadSuccess) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (isFirstVisit) "✓ QR code saved to Downloads folder" else "✓ QR code downloaded",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
        
        Spacer(Modifier.height(16.dp))
        
        // How ScrollFree QR Code Works Section - show for first visit or when coming from no QR code dialog
        if (isFirstVisit || fromNoQrCodeDialog) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFF2C2C2C),
                shape = RoundedCornerShape(12.dp)
            ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    "How Scroll Pause QR Code Works:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(Modifier.height(8.dp))
                
                Text(
                    "1. Print your QR code and place it somewhere you have to walk to (kitchen, bedroom, upstairs, etc.).\n\n" +
                    "2. No printer? Share a screenshot of your QR code with a family member, friend, or housemate—your ScrollPause accountability partner—and ask them to keep it on their phone.\n\n" +
                    "3. When your time limit ends, you'll need to scan the QR code—either where you placed it or from your partner—to unlock your apps.\n\n" +
                    "4. This makes you step away from your phone for a natural pause, and if scanning from your Scroll Pause partner, adds a little extra social time!",
                    fontSize = 14.sp,
                    color = Color(0xFFD1D5DB),
                    lineHeight = 18.sp
                )
            }
        }
        }
    }
    
}

@Composable
private fun DashboardContent(
    qrId: String,
    message: String,
    trackedApps: List<TrackedApp>,
    isTracking: Boolean,
    timeLimitMinutes: Int,
    sessionAppUsageTimes: Map<String, Long>,
    timesUnblockedToday: Int,
    timesDismissedToday: Int,
    sessionElapsedSeconds: Long,
    onToggleTracking: () -> Unit,
    onOpenQrGenerator: () -> Unit,
    onOpenAppSelection: () -> Unit,
    onOpenPause: () -> Unit,
    onOpenDurationSetting: () -> Unit,
    onOpenSettings: () -> Unit,
    onRemoveTrackedApp: (String) -> Unit,
    onShowTimeRemainingInfo: () -> Unit
) {
    var showAccountabilityDialog by remember { mutableStateOf(false) }
    var savedQrCodes by remember { mutableStateOf<List<SavedQrCode>>(emptyList()) }
    var savedQrLoaded by remember { mutableStateOf(false) }
    val storage = remember { createAppStorage() }
    val coroutineScope = rememberCoroutineScope()
    
    // Load saved QR codes when component mounts
    LaunchedEffect(Unit) {
        savedQrCodes = storage.getSavedQrCodes()
        savedQrLoaded = true
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
            .statusBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Scroll Pause", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Monday, Sep 22", fontSize = 14.sp, color = Color(0xFFD1D5DB))
            }
            Row {
                Text("⚙", fontSize = 24.sp, color = Color(0xFFD1D5DB), modifier = Modifier.clickable { onOpenSettings() })
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Tracking State Label
        if (isTracking) {
            var animationScale by remember { mutableStateOf(1f) }
            val infiniteTransition = rememberInfiniteTransition(label = "tracking_pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                backgroundColor = Color(0xFF385eff),
                shape = RoundedCornerShape(12.dp),
                elevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pulsing dot
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color.White, CircleShape)
                            .graphicsLayer {
                                alpha = (scale - 1f) * 2f + 0.5f
                            }
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "TRACKING YOUR APPS",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.width(12.dp))
                    // Pulsing dot
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color.White, CircleShape)
                            .graphicsLayer {
                                alpha = (scale - 1f) * 2f + 0.5f
                            }
                    )
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Current Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                
                // Time remaining display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E3A5F), RoundedCornerShape(12.dp))
                        .clickable { onOpenDurationSetting() }
                        .padding(24.dp)
                ) {
                    // Info button in top-left corner
                    IconButton(
                        onClick = { onShowTimeRemainingInfo() },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info about time remaining",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    // Pencil emoji in top-right corner
                    Text(
                        "✎",
                        fontSize = 20.sp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .graphicsLayer(scaleX = -1f)
                    )
                    
                    // Main content centered
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        // Calculate remaining time from actual tracked app usage
                        val totalTrackedAppUsageSeconds = sessionAppUsageTimes.values.sum()
                        val totalTrackedAppUsageMinutes = (totalTrackedAppUsageSeconds / 60L).toInt()
                        val remaining = (timeLimitMinutes - totalTrackedAppUsageMinutes).coerceAtLeast(0)
                        // Debug logging
                        println("DEBUG: Time remaining - totalTrackedAppUsageSeconds: $totalTrackedAppUsageSeconds, totalTrackedAppUsageMinutes: $totalTrackedAppUsageMinutes, remaining: $remaining")
                        val baseFontSize = 36.sp
                        Text(
                            text = androidx.compose.ui.text.buildAnnotatedString {
                                append("${remaining}m ")
                                pushStyle(androidx.compose.ui.text.SpanStyle(fontSize = baseFontSize * 0.7f))
                                pop()
                            },
                            fontSize = baseFontSize,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        val minuteLabel = if (remaining == 1) "minute" else "minutes"
                        Text("$minuteLabel remaining until pause time", fontSize = 14.sp, color = Color.White)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val timeLimitLabel = if (timeLimitMinutes == 1) "minute" else "minutes"
                            Text("Time Limit: ${timeLimitMinutes} $timeLimitLabel", fontSize = 12.sp, color = Color.White)
                            Spacer(Modifier.width(6.dp))
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Stats title row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("Your Stats Today", fontSize = 16.sp, color = Color.White)
                }
                
                Spacer(Modifier.height(12.dp))
                
                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${timesUnblockedToday}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFFBFDEDA))
                        Text("times walked", fontSize = 12.sp, color = Color(0xFFD1D5DB))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${timesDismissedToday}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB347))
                        Text("times dismissed", fontSize = 12.sp, color = Color(0xFFD1D5DB))
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Start tracking button
                Button(
                    onClick = onToggleTracking,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(backgroundColor = if (isTracking) Color(0xFF6B7B8C) else Color(0xFF1E3A5F)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    if (isTracking) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(Color(0xFF1E3A5F), RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(12.dp)
                                        .background(Color.White, RoundedCornerShape(1.dp))
                                )
                                Spacer(Modifier.width(3.dp))
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(12.dp)
                                        .background(Color.White, RoundedCornerShape(1.dp))
                                )
                            }
                        }
                    } else {
                        Text("▶", color = Color.White)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(if (isTracking) "Pause Tracking" else "Start Tracking", color = Color.White, fontWeight = FontWeight.Bold)
                }

            }
        }
        
        Spacer(Modifier.height(24.dp))

        // Saved QR Codes at top
        
        
        // Ready to Walk Card (show only if no saved QR codes after load)
        if (savedQrLoaded && savedQrCodes.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFF2C2C2C),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("▦", fontSize = 16.sp, color = Color(0xFF6EE7B7))
                        Spacer(Modifier.width(8.dp))
                        Text("Ready to Walk for Your Apps?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    Text(
                        "Print your personal QR codes and place them around your home. \nNo printer? Take a screenshot and share it with a trusted friend. \nWhen your time limit ends, walk to scan a code and take a healthy movement break.",
                        fontSize = 14.sp,
                        color = Color(0xFFD1D5DB),
                        lineHeight = 20.sp
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Button(
                        onClick = onOpenQrGenerator,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1E3A5F)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text("▦", color = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Generate QR Codes", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    
                    // Removed scan action button per requirements
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Saved QR Codes card removed from Dashboard; now lives in Settings
        
        // App Usage Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Centered title row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("Your selected apps to track", fontSize = 19.sp, color = Color.White)
                }
                
                Spacer(Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📊", fontSize = 16.sp, color = Color(0xFF1E3A5F))
                        Spacer(Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            val totalTodayMinutesUsed = trackedApps.sumOf { app ->
                                val sessionMinutes = ((sessionAppUsageTimes[app.name] ?: 0L) / 60L).toInt()
                                app.minutesUsed + sessionMinutes
                            }
                            val hours = totalTodayMinutesUsed / 60
                            val minutes = totalTodayMinutesUsed % 60
                            Text("${hours}h ${minutes}m", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("total usage today", fontSize = 12.sp, color = Color(0xFFD1D5DB))
                        }
                    }
                    Text(
                        text = "+", 
                        fontSize = 24.sp, 
                        color = Color.White,
                        modifier = Modifier.clickable { onOpenAppSelection() }
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Empty state when no apps are selected
                if (trackedApps.isEmpty()) {
                    Text(
                        "No apps selected. Add apps to track",
                        color = Color(0xFFD1D5DB),
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { onOpenAppSelection() }
                    )
                }

                trackedApps.forEach { app ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(app.name, fontWeight = FontWeight.SemiBold, color = Color.White)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val sessionMinutes = ((sessionAppUsageTimes[app.name] ?: 0L) / 60L).toInt()
                                val liveMinutes = app.minutesUsed + sessionMinutes
                                // Debug logging
                                println("DEBUG: App ${app.name} - sessionMinutes: $sessionMinutes, app.minutesUsed: ${app.minutesUsed}, liveMinutes: $liveMinutes")
                                println("DEBUG: App ${app.name} - sessionAppUsageTimes[${app.name}]: ${sessionAppUsageTimes[app.name]}")
                                Text("${liveMinutes}m today", color = Color(0xFFD1D5DB), fontSize = 12.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "🗑",
                                    color = Color(0xFFFF5252),
                                    fontSize = 12.sp,
                                    modifier = Modifier.clickable { onRemoveTrackedApp(app.name) }
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        val percent = (app.minutesUsed.toFloat() / app.limitMinutes.toFloat()).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .background(Color(0xFF334155), RoundedCornerShape(8.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = percent)
                                    .height(8.dp)
                                    .background(Color(0xFF1E3A5F), RoundedCornerShape(8.dp))
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Pause Partners Dialog
    if (showAccountabilityDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showAccountabilityDialog = false }) {
            Card(
                backgroundColor = Color(0xFF1A1A1A),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("👥", fontSize = 24.sp)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Coming Soon: Pause Partners",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Column {
                        Text(
                            "We're working on a feature that lets someone you trust generate QR codes on their phone for you to scan.",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Your pause partner can help you think twice about your app usage by being the \"gatekeeper\" of your unlock codes.",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    
                    Button(
                        onClick = { showAccountabilityDialog = false },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Got it!", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    onBack: () -> Unit,
    onOpenSavedQrCodes: () -> Unit,
    onNotificationsTurnedOff: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenTutorial: () -> Unit
) {
    val storage = remember { createAppStorage() }
    val coroutineScope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A1A),
                        Color(0xFF2A2A2A)
                    )
                )
            )
            .statusBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("←", fontSize = 24.sp, color = Color.White, modifier = Modifier.clickable { onBack() })
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Customize your ScrollPause experience", fontSize = 14.sp, color = Color(0xFFD1D5DB))
            }
        }
        Spacer(Modifier.height(24.dp))

        // Saved QR Codes at top
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenSavedQrCodes() },
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Saved QR Codes", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Text("Manage, share, and protect your QR codes", color = Color(0xFFD1D5DB), fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(8.dp))

        // How to use
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenTutorial() },
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("How to use", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Text("Learn how to get the most out of ScrollPause", color = Color(0xFFD1D5DB), fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Permissions
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenPermissions() },
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Permissions", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Text("Manage app permissions and access settings", color = Color(0xFFD1D5DB), fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { openEmailClient("hello@scroll-pause.com") },
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Contact us", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Text("Get in touch for suggestions, comments, support, complaints, and more", color = Color(0xFFD1D5DB), fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenPrivacyPolicy() },
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Privacy Policy", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Text("Learn how we collect, use, and protect your data", color = Color(0xFFD1D5DB), fontSize = 14.sp)
            }
        }

        
    }
}

@Composable
private fun SavedQrCodesScreen(
    onBack: () -> Unit,
    onOpenQrGenerator: () -> Unit,
    onViewQr: (id: String, qrText: String, message: String) -> Unit
) {
    val storage = remember { createAppStorage() }
    val coroutineScope = rememberCoroutineScope()
    var savedQrCodes by remember { mutableStateOf<List<SavedQrCode>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var downloadConfirmations by remember { mutableStateOf<Set<String>>(emptySet()) }
    var downloadAllConfirmation by remember { mutableStateOf(false) }

    fun refreshList() {
        coroutineScope.launch {
            isLoading = true
            savedQrCodes = try { storage.getSavedQrCodes() } catch (_: Exception) { emptyList() }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { refreshList() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
            .statusBarsPadding()
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("←", fontSize = 24.sp, color = Color.White, modifier = Modifier.clickable { onBack() })
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Saved QR Codes", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("View and reprint your previously generated QR codes", fontSize = 14.sp, color = Color(0xFFD1D5DB))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading...", color = Color.White)
                }
            }
            savedQrCodes.isEmpty() -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0xFF2C2C2C),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("No saved QR codes yet", color = Color(0xFFD1D5DB), modifier = Modifier.padding(24.dp))
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { onOpenQrGenerator() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1E3A5F)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Text("＋", color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Add New", color = Color.White, fontWeight = FontWeight.Bold)
                }
                
                Spacer(Modifier.height(16.dp))
                
                // How ScrollFree QR Code Works Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0xFF2C2C2C),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                    "How Scroll Pause QR Code Works:",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        
                        Spacer(Modifier.height(12.dp))
                        
                        Text(
                            "1. Print your QR code and place it somewhere you have to walk to (kitchen, bedroom, upstairs, etc.).\n\n" +
                            "2. No printer? Share a screenshot of your QR code with a family member, friend, or housemate—your ScrollPause accountability partner—and ask them to keep it on their phone.\n\n" +
                            "3. When your time limit ends, you'll need to scan the QR code—either where you placed it or from your partner—to unlock your apps.\n\n" +
                            "4. This makes you step away from your phone for a natural pause, and if scanning from your Scroll Pause partner, adds a little extra social time!",
                            fontSize = 14.sp,
                            color = Color(0xFFD1D5DB),
                            lineHeight = 20.sp
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
                ) {
                    items(savedQrCodes) { qrCode ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = Color(0xFF1E3A5F),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(qrCode.message, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                                    Row {
                                        Text(
                                            "↻",
                                            color = Color(0xFFD1D5DB),
                                            modifier = Modifier.clickable {
                                                coroutineScope.launch {
                                                    try { storage.removeQrCode(qrCode.id) } catch (_: Exception) {}
                                                    val newCode = SavedQrCode(
                                                        id = "pause-${kotlin.random.Random.nextLong()}",
                                                        qrText = "QR:${qrCode.message}:v${kotlin.random.Random.nextInt(1, 1_000_000)}",
                                                        message = qrCode.message,
                                                        createdAt = getCurrentTimeMillis(),
                                                        isActive = true
                                                    )
                                                    try { storage.saveQrCode(newCode) } catch (_: Exception) {}
                                                    refreshList()
                                                }
                                            }
                                        )
                                        Spacer(Modifier.width(16.dp))
                                        Text(
                                            "🗑",
                                            color = Color(0xFFFF5252),
                                            modifier = Modifier.clickable {
                                                coroutineScope.launch {
                                                    try { storage.removeQrCode(qrCode.id) } catch (_: Exception) {}
                                                    refreshList()
                                                }
                                            }
                                        )
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(96.dp)
                                            .background(Color.White, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        QrCodeDisplay(text = qrCode.qrText, modifier = Modifier.fillMaxSize())
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("QR Code ID", color = Color(0xFFD1D5DB), fontSize = 12.sp)
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(qrCode.id, color = Color.White, fontSize = 14.sp)
                                        Spacer(Modifier.height(8.dp))
                                        Text("Generated ${formatDate(qrCode.createdAt)}", color = Color(0xFFD1D5DB), fontSize = 12.sp)
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                Button(
                                    onClick = { onViewQr(qrCode.id, qrCode.qrText, qrCode.message) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4A90E2)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(vertical = 12.dp)
                                ) {
                                    Text("👁", color = Color.White)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "View",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            try { 
                                                saveQrPdf(qrText = qrCode.qrText, message = qrCode.message)
                                                downloadConfirmations = downloadConfirmations + qrCode.id
                                                // Hide confirmation after 3 seconds
                                                kotlinx.coroutines.delay(3000)
                                                downloadConfirmations = downloadConfirmations - qrCode.id
                                            } catch (_: Exception) {}
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2C2C2C)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(vertical = 12.dp)
                                ) {
                                    Text(if (qrCode.id in downloadConfirmations) "✓" else "⇩", color = Color.White)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (qrCode.id in downloadConfirmations) "Downloaded" else "Download this QR code", 
                                        color = Color.White, 
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { onOpenQrGenerator() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1E3A5F)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 14.dp)
                        ) {
                            Text("＋", color = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Add New", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (savedQrCodes.size > 1) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        savedQrCodes.forEach { code ->
                                            try { 
                                                saveQrPdf(qrText = code.qrText, message = code.message)
                                                // Note: Each QR code PDF will be saved to the Downloads folder
                                            } catch (_: Exception) {}
                                        }
                                        downloadAllConfirmation = true
                                        // Hide confirmation after 3 seconds
                                        kotlinx.coroutines.delay(3000)
                                        downloadAllConfirmation = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2C2C2C)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(vertical = 14.dp)
                            ) {
                                Text(if (downloadAllConfirmation) "✓" else "⇩", color = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (downloadAllConfirmation) "All downloaded" else "Download all", 
                                    color = Color.White, 
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    // How AntiScroll QR Code Works Section
                    item {
                        Spacer(Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = Color(0xFF2C2C2C),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    "How Scroll Pause QR Code Works:",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                
                                Spacer(Modifier.height(12.dp))
                                
                                Text(
                                    "1. Print your QR code and place it somewhere you have to walk to (kitchen, bedroom, upstairs, etc.).\n\n" +
                                    "2. No printer? Share a screenshot of your QR code with a family member, friend, or housemate—your ScrollPause accountability partner—and ask them to keep it on their phone.\n\n" +
                                    "3. When your time limit ends, you'll need to scan the QR code—either where you placed it or from your partner—to unlock your apps.\n\n" +
                                    "4. This makes you step away from your phone for a natural pause, and if scanning from your Scroll Pause partner, adds a little extra social time!",
                                    fontSize = 14.sp,
                                    color = Color(0xFFD1D5DB),
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PauseScreen(
    durationText: String,
    timeLimitMinutes: Int,
    dayStreakCounter: Int,
    onScanQr: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Card-like container
            Card(
                backgroundColor = Color(0xFF1E3A5F),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFF1E3A5F), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(16.dp)
                                    .background(Color.White, RoundedCornerShape(1.dp))
                            )
                            Spacer(Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(16.dp)
                                    .background(Color.White, RoundedCornerShape(1.dp))
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Time for a Pause",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "You have used your tracked apps for",
                        color = Color.White
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = durationText,
                        color = Color.White,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = getRandomPauseMessage(),
                        color = Color.White,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onScanQr,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4A90E2)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Text("▣", color = Color.White)
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Scan My QR Code", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Walk to your QR code to unlock your apps", color = Color.White, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6B7280)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("× Dismiss", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    if (dayStreakCounter > 0) {
                        Text(
                            "[You will lose your doomscroll-free streak\nand have to watch ads!]",
                            color = Color.White,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            "[You will have to watch ads!]",
                            color = Color.White,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppSelectionScreen(
    availableApps: List<AvailableApp>,
    isLoading: Boolean,
    onAppToggle: (String) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    // Filter apps based on search query
    val filteredApps = if (searchQuery.isBlank()) {
        availableApps
    } else {
        availableApps.filter { app ->
            app.name.contains(searchQuery, ignoreCase = true) ||
            app.category.contains(searchQuery, ignoreCase = true)
        }
    }
    
    val selectedCount = availableApps.count { it.isSelected }
    
    // Show loading state if apps are being loaded
    if (isLoading || availableApps.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Loading...", color = Color.White, fontSize = 18.sp)
                Text("Scanning for installed apps...", color = Color(0xFFD1D5DB), fontSize = 14.sp)
            }
        }
        return
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
            .statusBarsPadding()
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "←",
                fontSize = 24.sp,
                color = Color.White,
                modifier = Modifier.clickable { onBack() }
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    "Select Apps to Track",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "Choose which apps you'd like to set limits for.",
                    fontSize = 14.sp,
                    color = Color(0xFFD1D5DB)
                )
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Selection count
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📱", fontSize = 16.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                "$selectedCount apps selected",
                fontSize = 16.sp,
                color = Color(0xFFD1D5DB)
            )
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Search field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    "Search apps...",
                    color = Color(0xFF9CA3AF),
                    fontSize = 16.sp
                )
            },
            textStyle = androidx.compose.ui.text.TextStyle(
                color = Color.White,
                fontSize = 16.sp
            ),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFF1E3A5F),
                unfocusedBorderColor = Color(0xFF4B5563),
                cursorColor = Color(0xFF1E3A5F)
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        
        Spacer(Modifier.height(16.dp))
        
        // Apps list
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredApps) { app ->
                AppSelectionItem(
                    app = app,
                    onToggle = { onAppToggle(app.packageName) }
                )
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Done button
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1E3A5F)),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            Text(
                "Done",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Spacer to prevent blocking by Samsung native buttons
        Spacer(Modifier.height(LocalConfiguration.current.screenHeightDp.dp / 16))
    }
}

@Composable
private fun AppSelectionItem(
    app: AvailableApp,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFF2C2C2C),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon
            Text(
                text = app.icon,
                fontSize = 24.sp,
                modifier = Modifier.size(40.dp)
            )
            
            Spacer(Modifier.width(16.dp))
            
            // App info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = app.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = app.category,
                    fontSize = 12.sp,
                    color = Color(0xFFD1D5DB)
                )
            }
            
            // Toggle switch
            Box(
                modifier = Modifier
                    .size(48.dp, 28.dp)
                    .background(
                        color = if (app.isSelected) Color(0xFF4A90E2) else Color(0xFF4B5563),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp, 24.dp)
                        .background(
                            color = Color.White,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .offset(
                            x = if (app.isSelected) 10.dp else (-10).dp
                        )
                )
            }
        }
    }
}

@Composable
private fun PrivacyPolicyScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
            .statusBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "←",
                fontSize = 24.sp,
                color = Color.White,
                modifier = Modifier.clickable { onBack() }
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    "Privacy Policy",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "Last updated: January 2025",
                    fontSize = 14.sp,
                    color = Color(0xFFD1D5DB)
                )
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        // Privacy Policy Content
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    "Introduction",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Scroll Pause (\"we,\" \"our,\" or \"us\") is committed to protecting your privacy. This Privacy Policy explains how we collect, use, disclose, and safeguard your information when you use our application.",
                    color = Color(0xFFD1D5DB),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                
                Spacer(Modifier.height(24.dp))
                
                Text(
                    "Information We Collect",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "• App Usage Data: We track which apps you use and for how long to help you manage your digital pause\n" +
                    "• Device Information: Basic device information necessary for app functionality\n" +
                    "• QR Code Data: QR codes you generate and scan for pause functionality\n" +
                    "• Settings Preferences: Your app settings and preferences\n" +
                    "• Device Identifiers: For advertising purposes through Google AdMob",
                    color = Color(0xFFD1D5DB),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                
                Spacer(Modifier.height(24.dp))
                
                Text(
                    "How We Use Your Information",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "• Provide apps tracking and pause functionality\n" +
                    "• Generate and manage QR codes for your pause system\n" +
                    "• Redirect you to our app when time limits are reached\n" +
                    "• Improve app performance and user experience\n" +
                    "• Display advertisements through Google AdMob",
                    color = Color(0xFFD1D5DB),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                
                Spacer(Modifier.height(24.dp))
                
                Text(
                    "Data Storage and Security",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "• Most data is stored locally on your device\n" +
                    "• Device identifiers are shared with Google AdMob for advertising\n" +
                    "• Your app usage data remains private and under your control\n" +
                    "• We implement appropriate security measures to protect your information",
                    color = Color(0xFFD1D5DB),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                
                Spacer(Modifier.height(24.dp))
                
                Text(
                    "Permissions",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "• Usage Access: Required to track app usage for wellness features\n" +
                    "• Accessibility Service: Used to detect app usage and redirect you to our pause screen when time limits are reached\n" +
                    "• Camera: Used to scan QR codes for pause functionality\n" +
                    "• Notifications: Used to alert you when time limits are reached\n" +
                    "• Internet Access: Used to display advertisements through Google AdMob",
                    color = Color(0xFFD1D5DB),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                
                Spacer(Modifier.height(24.dp))
                
                Text(
                    "Advertising and Third-Party Services",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "• We use Google AdMob to display advertisements in our app\n" +
                    "• AdMob may collect device identifiers and usage data for ad targeting\n" +
                    "• This data is shared with Google and advertising partners\n" +
                    "• You can opt out of personalized ads in your device settings\n" +
                    "• AdMob's privacy policy applies to advertising data collection",
                    color = Color(0xFFD1D5DB),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                
                Spacer(Modifier.height(24.dp))
                
                Text(
                    "Your Rights",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "• Access your data stored in the app\n" +
                    "• Delete your data by uninstalling the app\n" +
                    "• Modify your privacy settings at any time\n" +
                    "• Contact us with privacy concerns",
                    color = Color(0xFFD1D5DB),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                
                Spacer(Modifier.height(24.dp))
                
                Text(
                    "Contact Us",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "If you have any questions about this Privacy Policy, please contact us at:\n\n" +
                    "Email: hello@scroll-pause.com",
                    color = Color(0xFFD1D5DB),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                
                Spacer(Modifier.height(24.dp))
                
                Text(
                    "Changes to This Policy",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "We may update this Privacy Policy from time to time. We will notify you of any changes by posting the new Privacy Policy in the app. You are advised to review this Privacy Policy periodically for any changes.",
                    color = Color(0xFFD1D5DB),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun DurationSettingScreen(
    timeLimitMinutes: Int,
    onTimeLimitChange: (Int) -> Unit,
    onCompleteSetup: () -> Unit,
    onBack: () -> Unit
) {
    val quickSelectOptions = listOf(5, 10, 15, 45, 60)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
            .statusBarsPadding()
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "←",
                fontSize = 24.sp,
                color = Color.White,
                modifier = Modifier.clickable { onBack() }
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    "Set Time Limit",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "How long before you take a pause?",
                    fontSize = 14.sp,
                    color = Color(0xFFD1D5DB)
                )
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        // Current Limit Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🕒", fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Current Limit",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Time display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeLimitMinutes.toString(),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    run {
                        val currentLimitLabel = if (timeLimitMinutes == 1) "minute" else "minutes"
                        Text(
                            currentLimitLabel,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Adjust controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { 
                            if (timeLimitMinutes > 1) {
                                onTimeLimitChange(timeLimitMinutes - 1)
                            }
                        },
                        modifier = Modifier.size(40.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2C2C2C)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            "-",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(Modifier.width(16.dp))
                    Text(
                        "adjust",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.width(16.dp))
                    
                    Button(
                        onClick = { 
                            if (timeLimitMinutes < 480) { // Max 8 hours
                                onTimeLimitChange(timeLimitMinutes + 1)
                            }
                        },
                        modifier = Modifier.size(40.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2C2C2C)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            "+",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Quick Select
        Text(
            "Quick Select",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Spacer(Modifier.height(12.dp))
        
        // Quick select buttons
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickSelectOptions.chunked(3)) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { minutes ->
                        Button(
                            onClick = { onTimeLimitChange(minutes) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = if (timeLimitMinutes == minutes) 
                                    Color(0xFF1E3A5F) else Color(0xFF2C2C2C)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Text(
                                "${minutes}m",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    // Fill remaining space if row has less than 3 items
                    repeat(3 - row.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Information card
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "All selected apps will be blocked after $timeLimitMinutes minutes of combined use. You have to walk and scan your saved QR codes to unblock them.",
                modifier = Modifier.padding(16.dp),
                color = Color.White,
                fontSize = 14.sp
            )
        }
        
        Spacer(Modifier.height(14.dp))
        
        // Complete Setup button
        Button(
            onClick = onCompleteSetup,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1E3A5F)),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            Text(
                "Done",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PermissionsScreen(
    onBack: () -> Unit,
    isTracking: Boolean,
    showUsageAccessDisableConfirmationDialog: Boolean,
    showAccessibilityDisableConfirmationDialog: Boolean,
    onShowUsageAccessDisableConfirmationDialog: () -> Unit,
    onShowAccessibilityDisableConfirmationDialog: () -> Unit,
    onShowUsageAccessDialog: () -> Unit
) {
    val storage = remember { createAppStorage() }
    val coroutineScope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A1A),
                        Color(0xFF2A2A2A)
                    )
                )
            )
            .statusBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("←", fontSize = 24.sp, color = Color.White, modifier = Modifier.clickable { onBack() })
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Permissions", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Manage app permissions and access settings", fontSize = 14.sp, color = Color(0xFFD1D5DB))
            }
        }
        Spacer(Modifier.height(24.dp))

        // Notifications
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(16.dp)
        ) {
            var notificationsEnabled by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                try {
                    notificationsEnabled = storage.getNotificationsEnabled()
                } catch (_: Exception) { notificationsEnabled = false }
            }
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Notifications", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { enabled ->
                            notificationsEnabled = enabled
                            // Persist change
                            coroutineScope.launch {
                                try { storage.saveNotificationsEnabled(enabled) } catch (_: Exception) {}
                            }
                            // Do not trigger permission dialog here; prompts appear only
                            // on app start, landing on Dashboard, or when starting tracking
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF1A1A1A),
                            checkedTrackColor = Color(0xFF1E3A5F),
                            uncheckedThumbColor = Color(0xFF1A1A1A),
                            uncheckedTrackColor = Color(0xFF4B5563)
                        )
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("Push Notifications — Get notified when you reach time limits", color = Color(0xFFD1D5DB), fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Allow App Usage Access
        UsageAccessCard(
            isTracking = isTracking,
            showUsageAccessDisableConfirmationDialog = showUsageAccessDisableConfirmationDialog,
            onShowUsageAccessDisableConfirmationDialog = onShowUsageAccessDisableConfirmationDialog,
            openUsageAccessSettings = { openUsageAccessSettings() },
            onRequestEnableInApp = { onShowUsageAccessDialog() },
            isUsageAccessPermissionGranted = { isUsageAccessPermissionGranted() },
            storage = storage,
            coroutineScope = coroutineScope
        )

        Spacer(Modifier.height(8.dp))

        // Allow Accessibility Access
        AccessibilityCard(
            isTracking = isTracking,
            showAccessibilityDisableConfirmationDialog = showAccessibilityDisableConfirmationDialog,
            onShowAccessibilityDisableConfirmationDialog = onShowAccessibilityDisableConfirmationDialog,
            openAccessibilitySettings = { openAccessibilitySettings() },
            isAccessibilityServiceEnabled = { isAccessibilityServiceEnabled() },
            storage = storage,
            coroutineScope = coroutineScope
        )

        Spacer(Modifier.height(8.dp))

        // Camera Access
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(16.dp)
        ) {
            var cameraAllowed by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()
            LaunchedEffect(Unit) {
                // Initialize and then poll to reflect async permission changes
                cameraAllowed = hasCameraPermission()
                while (true) {
                    kotlinx.coroutines.delay(500)
                    val current = try { hasCameraPermission() } catch (_: Exception) { false }
                    if (cameraAllowed != current) cameraAllowed = current
                }
            }
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Camera Access", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Switch(
                        checked = cameraAllowed,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                scope.launch {
                                    // Request permission if not granted
                                    if (!hasCameraPermission()) {
                                        requestCameraPermission()
                                    }
                                    // state will update via polling loop
                                }
                            } else {
                                // Cannot revoke permission programmatically; guide user to settings
                                openAppSettingsForCamera()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF1A1A1A),
                            checkedTrackColor = Color(0xFF1E3A5F),
                            uncheckedThumbColor = Color(0xFF1A1A1A),
                            uncheckedTrackColor = Color(0xFF4B5563)
                        )
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("Required to scan QR codes for pause functionality.", color = Color(0xFFD1D5DB), fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun UsageAccessCard(
    isTracking: Boolean,
    showUsageAccessDisableConfirmationDialog: Boolean,
    onShowUsageAccessDisableConfirmationDialog: () -> Unit,
    openUsageAccessSettings: () -> Unit,
    onRequestEnableInApp: () -> Unit,
    isUsageAccessPermissionGranted: () -> Boolean,
    storage: AppStorage,
    coroutineScope: CoroutineScope
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFF2C2C2C),
        shape = RoundedCornerShape(16.dp)
    ) {
        var usageAccessAllowed by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            try {
                // Reflect app preference (in-app, not OS setting)
                usageAccessAllowed = storage.getUsageAccessAllowed()
            } catch (_: Exception) { usageAccessAllowed = false }
        }
        
        // Periodically reflect stored preference to keep UI in sync
        LaunchedEffect(Unit) {
            while (true) {
                kotlinx.coroutines.delay(2000) // Check every 2 seconds
                try {
                    val pref = storage.getUsageAccessAllowed()
                    usageAccessAllowed = pref
                } catch (_: Exception) { /* ignore */ }
            }
        }
        
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Allow App Usage Access", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Switch(
                    checked = usageAccessAllowed,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            // Enable path stays in-app: show dialog; CTA will set preference true
                            onRequestEnableInApp()
                        } else {
                            // Disable path: if tracking, confirm; otherwise turn off preference in-app
                            if (isTracking) {
                                onShowUsageAccessDisableConfirmationDialog()
                            } else {
                                // Not tracking: update preference in-app and reflect UI
                                coroutineScope.launch { try { storage.saveUsageAccessAllowed(false) } catch (_: Exception) {} }
                                usageAccessAllowed = false
                            }
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF1A1A1A),
                        checkedTrackColor = Color(0xFF1E3A5F),
                        uncheckedThumbColor = Color(0xFF1A1A1A),
                        uncheckedTrackColor = Color(0xFF4B5563)
                    )
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("Permit the app to access your app usage to enable tracking.", color = Color(0xFFD1D5DB), fontSize = 14.sp)
        }
    }
}

@Composable
fun AccessibilityCard(
    isTracking: Boolean,
    showAccessibilityDisableConfirmationDialog: Boolean,
    onShowAccessibilityDisableConfirmationDialog: () -> Unit,
    openAccessibilitySettings: () -> Unit,
    isAccessibilityServiceEnabled: () -> Boolean,
    storage: AppStorage,
    coroutineScope: CoroutineScope
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFF2C2C2C),
        shape = RoundedCornerShape(16.dp)
    ) {
        var accessibilityAccessAllowed by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            try {
                // Check actual system permission status instead of app preference
                accessibilityAccessAllowed = isAccessibilityServiceEnabled()
            } catch (_: Exception) { accessibilityAccessAllowed = false }
        }
        
        // Periodically check system status to catch changes when user returns from settings
        LaunchedEffect(Unit) {
            while (true) {
                kotlinx.coroutines.delay(2000) // Check every 2 seconds
                try {
                    val actualStatus = isAccessibilityServiceEnabled()
                    // Always update to match actual system state
                    accessibilityAccessAllowed = actualStatus
                    // Update storage to match actual system state
                    coroutineScope.launch {
                        try { storage.saveAccessibilityAccessAllowed(actualStatus) } catch (_: Exception) {}
                    }
                } catch (_: Exception) { /* ignore */ }
            }
        }
        
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Allow Accessibility Access", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Switch(
                    checked = accessibilityAccessAllowed,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            // User is trying to enable - just open settings
                            openAccessibilitySettings()
                        } else {
                            // User is trying to disable - check if tracking is active
                            if (isTracking) {
                                onShowAccessibilityDisableConfirmationDialog()
                            } else {
                                // Not tracking, so safe to open settings
                                openAccessibilitySettings()
                            }
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF1A1A1A),
                        checkedTrackColor = Color(0xFF1E3A5F),
                        uncheckedThumbColor = Color(0xFF1A1A1A),
                        uncheckedTrackColor = Color(0xFF4B5563)
                    )
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("Permit the app to access accessibility services for enhanced tracking features.", color = Color(0xFFD1D5DB), fontSize = 14.sp)
        }
    }
}

@Composable
private fun HowToUseScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
            .statusBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("←", fontSize = 24.sp, color = Color.White, modifier = Modifier.clickable { onBack() })
            Spacer(Modifier.width(16.dp))
            Column {
                Text("How to Use ScrollPause", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Your guide to digital wellness", fontSize = 14.sp, color = Color(0xFFD1D5DB))
            }
        }
        
        Spacer(Modifier.height(24.dp))

        // Welcome Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🏠", fontSize = 24.sp)
                    Spacer(Modifier.width(12.dp))
                    Text("Welcome to ScrollPause!", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.height(12.dp))
                Text("ScrollPause helps you build healthier digital habits by encouraging physical movement when you've spent too much time on selected apps.", color = Color(0xFFD1D5DB), fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Text("Instead of just blocking apps, we coach you to make better choices—one mindful pause at a time.", color = Color(0xFFD1D5DB), fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        // How It Works Section
        Text("How It Works", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        
        Spacer(Modifier.height(16.dp))

        // Step 1: Generate QR Code
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("\uD83C\uDFAC", fontSize = 24.sp)
                    Spacer(Modifier.width(12.dp))
                    Text("1. Generate Your QR Code", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.height(12.dp))
                Text("Create a QR code and place it somewhere that requires movement—across the room, upstairs, or outside.", color = Color(0xFFD1D5DB), fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Card(
                    backgroundColor = Color(0xFF3C3C3C),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("💡", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("The further away, the more effective!", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Step 2: Select Apps
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📱", fontSize = 24.sp)
                    Spacer(Modifier.width(12.dp))
                    Text("2. Select Your Apps", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.height(12.dp))
                Text("Choose which apps trigger mindful pauses. Set a duration for how long you can use them before a break is needed.", color = Color(0xFFD1D5DB), fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Card(
                    backgroundColor = Color(0xFF3C3C3C),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("💡", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("Start with your most addictive apps first.", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Step 3: Pause Activates
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⏰", fontSize = 24.sp)
                    Spacer(Modifier.width(12.dp))
                    Text("3. The Pause Activates", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.height(12.dp))
                Text("When your app time is up, ScrollPause pauses your access and encourages you to take a healthy break.", color = Color(0xFFD1D5DB), fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Card(
                    backgroundColor = Color(0xFF3C3C3C),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("💡", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("This is your moment to choose wellness!", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Step 4: Scan or Choose Activity
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("\uD83C\uDFAB", fontSize = 24.sp)
                    Spacer(Modifier.width(12.dp))
                    Text("4. Scan QR code", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.height(12.dp))
                Text("Walk to your QR code and scan it. Movement over scrolling!", color = Color(0xFFD1D5DB), fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Card(
                    backgroundColor = Color(0xFF3C3C3C),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("💡", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("Each scan builds your streak!", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Step 5: Build Your Streak
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔥", fontSize = 24.sp)
                    Spacer(Modifier.width(12.dp))
                    Text("5. Build Your Streak", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.height(12.dp))
                Text("Every day you choose movement over dismissing builds your consecutive days streak. Stay consistent!", color = Color(0xFFD1D5DB), fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Card(
                    backgroundColor = Color(0xFF3C3C3C),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("💡", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("Streaks reset if you dismiss instead of moving.", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Step 6: Celebrate Success
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🏆", fontSize = 24.sp)
                    Spacer(Modifier.width(12.dp))
                    Text("6. Celebrate Success", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.height(12.dp))
                Text("Get encouraging messages and track your progress. See your QR scans, estimated steps, and consecutive days!", color = Color(0xFFD1D5DB), fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Card(
                    backgroundColor = Color(0xFF3C3C3C),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("💡", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("Your future self will thank you!", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Key Features Section
        Text("Key Features", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        
        Spacer(Modifier.height(16.dp))

        // Key Features List
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // QR Code Movement
                Row(verticalAlignment = Alignment.Top) {
                    Text("✓", fontSize = 16.sp, color = Color(0xFF1E3A5F), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("QR Code Movement", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Physical activity required to unlock apps", color = Color(0xFFD1D5DB), fontSize = 14.sp)
                    }
                }
                
                Spacer(Modifier.height(16.dp))

                
                // Trusted Contacts
                Row(verticalAlignment = Alignment.Top) {
                    Text("✓", fontSize = 16.sp, color = Color(0xFF1E3A5F), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Trusted Contacts", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Accountability partners to help you stay on track", color = Color(0xFFD1D5DB), fontSize = 14.sp)
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Dismiss Tracking
                Row(verticalAlignment = Alignment.Top) {
                    Text("✓", fontSize = 16.sp, color = Color(0xFF1E3A5F), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Dismiss Tracking", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("See how often you skip your pauses (resets streaks)", color = Color(0xFFD1D5DB), fontSize = 14.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Pro Tips Section
        Text("Pro Tips", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        
        Spacer(Modifier.height(16.dp))

        // Pro Tips Cards
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Start small
                Row(verticalAlignment = Alignment.Top) {
                    Text("🎯", fontSize = 20.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Start small", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Begin with just 1-2 apps and 15-minute intervals", color = Color(0xFFD1D5DB), fontSize = 14.sp)
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // QR placement
                Row(verticalAlignment = Alignment.Top) {
                    Text("📍", fontSize = 20.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("QR placement", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Put codes where you naturally need to go (kitchen, bathroom)", color = Color(0xFFD1D5DB), fontSize = 14.sp)
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Protect streaks
                Row(verticalAlignment = Alignment.Top) {
                    Text("🌱", fontSize = 20.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Protect streaks", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Scan daily to maintain your consecutive days streak", color = Color(0xFFD1D5DB), fontSize = 14.sp)
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Get support
                Row(verticalAlignment = Alignment.Top) {
                    Text("👥", fontSize = 20.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Get support", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Add trusted contacts for accountability when tempted to dismiss", color = Color(0xFFD1D5DB), fontSize = 14.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Common Questions Section
        Text("Common Questions", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        
        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // What happens if I dismiss?
                Column {
                    Text("What happens if I dismiss?", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(4.dp))
                    Text("Your streaks reset to zero. The goal is to choose movement instead!", color = Color(0xFFD1D5DB), fontSize = 14.sp)
                }
                
                Spacer(Modifier.height(16.dp))


                // How do I maintain my streak?
                Column {
                    Text("How do I maintain my streak?", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(4.dp))
                    Text("Scan your QR code when pauses activate. Avoid dismissing!", color = Color(0xFFD1D5DB), fontSize = 14.sp)
                }

                Spacer(Modifier.height(16.dp))

                // Can I use activities instead of QR codes?
                Column {
                    Text("I’m outside and don’t have access to my QR code! I don’t want to lose my streak!", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(4.dp))
                    Text("You’ll still lose your streak — but hey, you’re outside! You shouldn't be doomscrolling! Enjoy the moment, the sunshine, and the real world around you \uD83C\uDF3F✨", color = Color(0xFFD1D5DB), fontSize = 14.sp)
                }
                


            }
        }

        Spacer(Modifier.height(32.dp))
    }
}
