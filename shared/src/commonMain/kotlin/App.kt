import androidx.compose.foundation.layout.Box
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp

@OptIn(ExperimentalResourceApi::class)
@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    contentDescription: String = "AntiScroll Logo"
) {
    Image(
        painter = painterResource("images/bigger_logo.png"),
        contentDescription = contentDescription,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp)),
        contentScale = ContentScale.Fit
    )
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
            Box(Modifier.fillMaxSize()) {
                AppRoot()
            }
        }
    }
}

// Simple navigation and app state holder
private enum class Route { Onboarding, QrGenerator, Dashboard, AppSelection, DurationSetting, Pause, Settings, SavedQrCodes, PrivacyPolicy }

// Enable simulated app usage increments for testing; rely on platform-specific tracking instead
private const val ENABLE_USAGE_SIMULATION: Boolean = true

// Platform hook to open Accessibility settings (Android) or no-op elsewhere
expect fun openAccessibilitySettings()
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

@Composable
private fun AppRoot() {
    var route by remember { mutableStateOf<Route?>(null) } // Start with null to show loading
    var qrMessage by remember { mutableStateOf("Take a mindful pause") }
    var qrId by remember { mutableStateOf<String?>(null) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var showTimeRemainingInfoDialog by remember { mutableStateOf(false) }
    var showNoTrackedAppsDialog by remember { mutableStateOf(false) }
    var showNoQrCodeDialog by remember { mutableStateOf(false) }
    var showUsageAccessDialog by remember { mutableStateOf(false) }
    var fromNoQrCodeDialog by remember { mutableStateOf(false) }
    var hasShownNotificationsPromptThisLaunch by remember { mutableStateOf(false) }
    var hasShownUsageAccessPromptThisLaunch by remember { mutableStateOf(false) }
    var hasCheckedPermissionsOnDashboardThisLaunch by remember { mutableStateOf(false) }
    var showAccessibilityConsentDialog by remember { mutableStateOf(false) }
    var pendingStartTracking by remember { mutableStateOf(false) }
    var trackedApps by remember { mutableStateOf<List<TrackedApp>>(emptyList()) }
    
    var availableApps by remember { mutableStateOf<List<AvailableApp>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(false) }
    
    var timeLimitMinutes by remember { mutableStateOf(15) }
    
    val storage = remember { createAppStorage() }
    fun currentEpochDayUtc(): Long = getCurrentTimeMillis() / 86_400_000L
    val installedAppsProvider = remember { createInstalledAppsProvider() }
    val coroutineScope = rememberCoroutineScope()
    
    var isTracking by remember { mutableStateOf(false) }
    var trackingStartTime by remember { mutableStateOf(0L) }
    var appUsageTimes by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var isBlocked by remember { mutableStateOf(false) }
    
    // Session tracking variables that reset on dismiss/QR scan
    var sessionStartTime by remember { mutableStateOf(0L) }
    var sessionAppUsageTimes by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var sessionElapsedSeconds by remember { mutableStateOf(0L) }
    
    // Counter for times unblocked today
    var timesUnblockedToday by remember { mutableStateOf(0) }
    // Counter for times dismissed today
    var timesDismissedToday by remember { mutableStateOf(0) }
    var isSetupMode by remember { mutableStateOf(false) }

    // Merge session usage into lifetime usage (minutesUsed and appUsageTimes)
    fun finalizeSessionUsage() {
        println("DEBUG: finalizeSessionUsage() called")
        println("DEBUG: sessionAppUsageTimes: $sessionAppUsageTimes")
        println("DEBUG: trackedApps before: ${trackedApps.map { "${it.name}: ${it.minutesUsed}m" }}")
        
        if (sessionAppUsageTimes.isEmpty()) {
            println("DEBUG: sessionAppUsageTimes is empty, returning")
            return
        }
        
        // Credit ALL apps that have any usage this session
        trackedApps = trackedApps.map { app ->
            val sessionSeconds = sessionAppUsageTimes[app.name] ?: 0L
            if (sessionSeconds > 0) {
                val sessionMinutes = (sessionSeconds / 60L).toInt()
                println("DEBUG: Crediting ${sessionSeconds} seconds (${sessionMinutes} min) to ${app.name}")
                app.copy(minutesUsed = app.minutesUsed + sessionMinutes)
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
        
        println("DEBUG: appUsageTimes after: $appUsageTimes")
        println("DEBUG: Cleared sessionAppUsageTimes to prevent double counting")
    }

    // Track individual app usage when tracking is active
    LaunchedEffect(isTracking) {
        if (isTracking) {
            trackingStartTime = getCurrentTimeMillis()
            sessionStartTime = getCurrentTimeMillis() // Start new session
            
            // Save tracking state and start time to storage
            coroutineScope.launch {
                storage.saveTrackingState(true)
                storage.saveTrackingStartTime(trackingStartTime)
                storage.saveSessionStartTime(sessionStartTime)
            }
            
            // Start platform-specific usage tracking
            val trackedPackages = trackedApps.map { app ->
                // Map app names to package names - this is a simplified mapping
                // In a real implementation, you'd have proper package name mapping
                when (app.name.lowercase()) {
                    "instagram" -> "com.instagram.android"
                    "tiktok" -> "com.zhiliaoapp.musically"
                    "facebook" -> "com.facebook.katana"
                    "snapchat" -> "com.snapchat.android"
                    "youtube" -> "com.google.android.youtube"
                    "twitter" -> "com.twitter.android"
                    "reddit" -> "com.reddit.frontpage"
                    "linkedin" -> "com.linkedin.android"
                    "whatsapp" -> "com.whatsapp"
                    "spotify" -> "com.spotify.music"
                    "netflix" -> "com.netflix.mediaclient"
                    "discord" -> "com.discord"
                    "telegram" -> "org.telegram.messenger"
                    "chrome" -> "com.android.chrome"
                    else -> app.name.lowercase().replace(" ", "")
                }
            }
            
            startUsageTracking(
                trackedPackages = trackedPackages,
                limitMinutes = timeLimitMinutes,
                onLimitReached = {
                    isTracking = false
                    route = Route.Pause
                }
            )
        } else {
            // Stop tracking and save final state
            if (trackingStartTime > 0) {
                // Save updated usage times and tracking state to storage
                coroutineScope.launch {
                    storage.saveAppUsageTimes(appUsageTimes)
                    storage.saveTrackingState(false)
                }
            }
        }
    }

    // Monitor foreground app changes to show overlay when blocked user tries to use tracked apps
    LaunchedEffect(isBlocked, trackedApps, timeLimitMinutes) {
        if (isBlocked) {
            // Check periodically if user is trying to use a tracked app while blocked
            while (isBlocked) {
                delay(2000) // Check every 2 seconds
                checkAndShowOverlayIfBlocked(trackedApps.map { it.name }, isBlocked, timeLimitMinutes)
            }
        }
    }

    // Real-time tracking update while tracking is active
    LaunchedEffect(isTracking, trackingStartTime) {
        if (isTracking && trackingStartTime > 0) {
            println("DEBUG: Starting tracking loop")
            while (isTracking) {
                delay(1000) // Update every second
                println("DEBUG: Tracking loop iteration - isTracking: $isTracking")

                // If Pause screen is active, do not accrue usage for any apps
                if (route == Route.Pause) {
                    continue
                }

                // Session elapsed time should only count when tracked apps are active
                // We'll calculate this based on the actual tracked app usage time
                val totalTrackedAppUsageSeconds = sessionAppUsageTimes.values.sum()
                sessionElapsedSeconds = totalTrackedAppUsageSeconds
                
                // If elapsed minutes reached limit, pause regardless of per-app accrual
                val elapsedMinutes = (sessionElapsedSeconds / 60L).toInt()
                if (elapsedMinutes >= timeLimitMinutes) {
                    finalizeSessionUsage()
                    isTracking = false
                    isBlocked = true
                    // Update accessibility service with blocked state
                    updateAccessibilityServiceBlockedState(isBlocked, trackedApps.map { it.name }, timeLimitMinutes)
                    // Save blocked state to storage
                    coroutineScope.launch {
                        storage.saveBlockedState(true)
                    }
                    route = Route.Pause
                    // Show the blocking overlay to prevent further app usage
                    showBlockingOverlay("Take a mindful pause - you've reached your time limit of ${timeLimitMinutes} minutes")
                    continue
                }

                // If Accessibility is not enabled, fall back to simulation to keep UI responsive
                val isAccessibilityEnabled = isAccessibilityServiceEnabled()
                println("DEBUG: *** TRACKING LOOP DEBUG *** isAccessibilityServiceEnabled: $isAccessibilityEnabled")
                if (!isAccessibilityEnabled) {
                    println("DEBUG: Using simulation logic - isAccessibilityServiceEnabled: ${isAccessibilityServiceEnabled()}, ENABLE_USAGE_SIMULATION: $ENABLE_USAGE_SIMULATION")
                    // Get the current foreground app to determine which app should get usage time
                    val currentForegroundApp = getCurrentForegroundApp()
                    val updatedSessionUsage = sessionAppUsageTimes.toMutableMap()
                    
                    // Calculate session duration for debug logging
                    val currentTime = getCurrentTimeMillis()
                    val sessionDuration = (currentTime - trackingStartTime) / 1000 // in seconds
                    
                    // Only increment usage for the app that is currently in the foreground
                    trackedApps.forEach { app ->
                        val currentSessionUsage = updatedSessionUsage[app.name] ?: 0L
                        
                        // Check if this app is currently in the foreground
                        val isAppActive = when {
                            // If we can detect the foreground app, only increment for that app
                            currentForegroundApp != null -> {
                                // Map app name to package name for comparison
                                val expectedPackage = when (app.name.lowercase()) {
                                    "instagram" -> "com.instagram.android"
                                    "tiktok" -> "com.zhiliaoapp.musically"
                                    "facebook" -> "com.facebook.katana"
                                    "snapchat" -> "com.snapchat.android"
                                    "youtube" -> "com.google.android.youtube"
                                    "twitter" -> "com.twitter.android"
                                    "chrome" -> "com.android.chrome"
                                    "messages" -> "com.google.android.apps.messaging"
                                    "gmail" -> "com.google.android.gm"
                                    else -> app.name.lowercase().replace(" ", "")
                                }
                                currentForegroundApp == expectedPackage
                            }
                            // If we can't detect foreground app, fall back to simulation for testing
                            else -> {
                                // For testing: Simulate realistic foreground app behavior
                                // Chrome is "foreground" for 60 seconds, then "background" for 30 seconds, then repeat
                                // This simulates user actively using Chrome, then switching to other apps
                                when (app.name.lowercase()) {
                                    "chrome" -> {
                                        val cyclePosition = sessionDuration % 90 // 90-second cycle
                                        cyclePosition < 60 // Chrome is foreground for first 60 seconds of each cycle
                                    }
                                    else -> false // Other apps are never foreground in this simulation
                                }
                            }
                        }
                        
                        val usageIncrement = if (isAppActive) 1L else 0L
                        updatedSessionUsage[app.name] = currentSessionUsage + usageIncrement
                        
                        if (usageIncrement > 0) {
                            println("DEBUG: App ${app.name} got usage increment: $usageIncrement, total session: ${currentSessionUsage + usageIncrement}")
                        }
                        println("DEBUG: App ${app.name} - isAppActive: $isAppActive, sessionDuration: $sessionDuration, currentSessionUsage: $currentSessionUsage")
                    }
                    
                    sessionAppUsageTimes = updatedSessionUsage
                    println("DEBUG: Updated sessionAppUsageTimes: $sessionAppUsageTimes")
                    
                    // Persist session data
                    coroutineScope.launch {
                        try { 
                            storage.saveSessionAppUsageTimes(sessionAppUsageTimes)
                            println("DEBUG: Saved session app usage times to storage")
                        } catch (e: Exception) {
                            println("DEBUG: Error saving session app usage times: ${e.message}")
                        }
                    }
                } else {
                    // Real foreground app detection using accessibility service
                    println("DEBUG: Using real foreground app detection")
                    val currentForegroundApp = getCurrentForegroundApp()
                    val updatedSessionUsage = sessionAppUsageTimes.toMutableMap()
                    
                    for (app in trackedApps) {
                        val currentSessionUsage = sessionAppUsageTimes[app.name] ?: 0L
                        
                        // Check if this app is currently in the foreground
                        val isAppActive = when {
                            currentForegroundApp != null -> {
                                // Map app names to expected package names
                                val expectedPackage = when (app.name.lowercase()) {
                                    "chrome" -> "com.android.chrome"
                                    "youtube" -> "com.google.android.youtube"
                                    "youtube music" -> "com.google.android.apps.youtube.music"
                                    "messages" -> "com.google.android.apps.messaging"
                                    "gmail" -> "com.google.android.gm"
                                    "maps" -> "com.google.android.apps.maps"
                                    "whatsapp" -> "com.whatsapp"
                                    "jira" -> "com.atlassian.android.jira.core"
                                    "kttipay" -> "com.kttipay"
                                    else -> {
                                        // Try to find the package name from availableApps
                                        val matchingApp = availableApps.find { availableApp ->
                                            availableApp.name.equals(app.name, ignoreCase = true)
                                        }
                                        matchingApp?.packageName ?: app.name.lowercase().replace(" ", "")
                                    }
                                }
                                currentForegroundApp == expectedPackage
                            }
                            else -> false // If we can't detect foreground app, don't give usage to any app
                        }
                        
                        val usageIncrement = if (isAppActive) 1L else 0L
                        updatedSessionUsage[app.name] = currentSessionUsage + usageIncrement
                        
                        if (usageIncrement > 0) {
                            println("DEBUG: App ${app.name} got usage increment: $usageIncrement, total session: ${currentSessionUsage + usageIncrement}")
                        }
                        println("DEBUG: App ${app.name} - isAppActive: $isAppActive, currentForegroundApp: $currentForegroundApp, currentSessionUsage: $currentSessionUsage")
                    }
                    
                    sessionAppUsageTimes = updatedSessionUsage
                    println("DEBUG: Updated sessionAppUsageTimes: $sessionAppUsageTimes")
                    
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
                
                // Check if session usage has reached the limit based on actual accumulated session usage
                val totalSessionSeconds = sessionAppUsageTimes.values.sum()
                val usedMinutes = (totalSessionSeconds / 60L).toInt()
                if (usedMinutes >= timeLimitMinutes) {
                    // Before pausing, merge the session into lifetime so UI shows correctly on Pause/Dashboard
                    finalizeSessionUsage()
                    isTracking = false
                    isBlocked = true
                    // Update accessibility service with blocked state
                    updateAccessibilityServiceBlockedState(isBlocked, trackedApps.map { it.name }, timeLimitMinutes)
                    // Save blocked state to storage
                    coroutineScope.launch {
                        storage.saveBlockedState(true)
                    }
                    route = Route.Pause
                    // Show the blocking overlay to prevent further app usage
                    showBlockingOverlay("Take a mindful pause - you've reached your time limit of ${timeLimitMinutes} minutes")
                }
            }
        }
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
            // Fallback to some default apps even if detection fails
            trackedApps = listOf(
                TrackedApp("Instagram", 0, 15),
                TrackedApp("TikTok", 0, 15),
                TrackedApp("Snapchat", 0, 15),
                TrackedApp("Facebook", 0, 15),
                TrackedApp("Twitter", 0, 15),
                TrackedApp("Reddit", 0, 15),
                TrackedApp("YouTube", 0, 15),
                TrackedApp("Chrome", 0, 15)
            )
            // Persist fallback package identifiers so the choice survives restarts
            val fallbackPackages = listOf(
                "com.instagram.android",
                "com.zhiliaoapp.musically",
                "com.snapchat.android",
                "com.facebook.katana",
                "com.twitter.android",
                "com.reddit.frontpage",
                "com.google.android.youtube",
                "com.android.chrome"
            )
            try { storage.saveSelectedAppPackages(fallbackPackages) } catch (_: Exception) {}
        } finally {
            isLoadingApps = false
        }
    }
    
    // Check onboarding completion status on app start
    LaunchedEffect(Unit) {
        try {
            // Register timer reset callback for QR scan
            setOnTimerResetCallback {
                println("DEBUG: ===== TIMER RESET CALLBACK CALLED (QR SCAN) =====")
                println("DEBUG: Current state - isBlocked: $isBlocked, isTracking: $isTracking")
                println("DEBUG: sessionAppUsageTimes before: $sessionAppUsageTimes")
                println("DEBUG: timesUnblockedToday before: $timesUnblockedToday")
                
                // 1. Finalize session usage before resetting (same as Dismiss)
                finalizeSessionUsage()
                println("DEBUG: Finalized session usage after QR scan")
                
                // 2. Increment times walked counter (different from dismiss)
                timesUnblockedToday += 1
                println("DEBUG: Incremented times walked counter to: $timesUnblockedToday")
                
                // Persist the updated counter
                coroutineScope.launch {
                    try { 
                        storage.saveTimesUnblockedToday(timesUnblockedToday)
                        println("DEBUG: Saved times unblocked counter to storage")
                    } catch (e: Exception) {
                        println("DEBUG: Error saving times unblocked counter: ${e.message}")
                    }
                }
                
                // 3. Reset session tracking state when QR scanning (same as dismiss)
                isTracking = false
                isBlocked = false
                // Update accessibility service with unblocked state
                updateAccessibilityServiceBlockedState(isBlocked, emptyList(), 0)
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
                
                // 4. Navigate back to dashboard (same as dismiss)
                route = Route.Dashboard
                println("DEBUG: Set route to Dashboard")
                
                // 5. Dismiss any blocking overlays (same as dismiss)
                dismissBlockingOverlay()
                println("DEBUG: Dismissed blocking overlays")
                
                println("DEBUG: ===== TIMER RESET CALLBACK COMPLETED (QR SCAN) =====")
            }
            
            // Register timer reset callback for dismiss button
            setOnDismissCallback {
                println("DEBUG: ===== DISMISS CALLBACK CALLED =====")
                println("DEBUG: Current state - isBlocked: $isBlocked, isTracking: $isTracking")
                println("DEBUG: sessionAppUsageTimes before: $sessionAppUsageTimes")
                println("DEBUG: timesDismissedToday before: $timesDismissedToday")
                
                // 1. Finalize session usage before resetting (same as QR scan)
                finalizeSessionUsage()
                println("DEBUG: Finalized session usage after dismiss")
                
                // 2. Increment times dismissed counter (different from QR scan)
                timesDismissedToday += 1
                println("DEBUG: Incremented times dismissed counter to: $timesDismissedToday")
                
                // Persist the updated counter
                coroutineScope.launch {
                    try { 
                        storage.saveTimesDismissedToday(timesDismissedToday)
                        println("DEBUG: Saved times dismissed counter to storage")
                    } catch (e: Exception) {
                        println("DEBUG: Error saving times dismissed counter: ${e.message}")
                    }
                }
                
                // 3. Reset session tracking state when dismissing (same as QR scan)
                isTracking = false
                isBlocked = false
                // Update accessibility service with unblocked state
                updateAccessibilityServiceBlockedState(isBlocked, emptyList(), 0)
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
                
                // 4. Navigate back to dashboard (same as QR scan)
                route = Route.Dashboard
                println("DEBUG: Set route to Dashboard")
                
                // 5. Dismiss any blocking overlays (same as QR scan)
                dismissBlockingOverlay()
                println("DEBUG: Dismissed blocking overlays")
                
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
            val savedSessionAppUsageTimes = withTimeoutOrNull(3000) { storage.getSessionAppUsageTimes() } ?: emptyMap()
            val savedSessionStartTime = withTimeoutOrNull(3000) { storage.getSessionStartTime() } ?: 0L
            val todayEpochDay = currentEpochDayUtc()
            
            // Debug logging for app usage data loading
            println("DEBUG: App startup - savedAppUsageTimes: $savedAppUsageTimes")
            println("DEBUG: App startup - savedUsageDay: $savedUsageDay, todayEpochDay: $todayEpochDay")
            
            // Restore tracking state
            isTracking = savedTrackingState
            appUsageTimes = savedAppUsageTimes
            trackingStartTime = savedTrackingStartTime
            isBlocked = savedBlockedState
            
            // Restore dashboard counters and session data
            timesUnblockedToday = savedTimesUnblockedToday
            timesDismissedToday = savedTimesDismissedToday
            sessionAppUsageTimes = savedSessionAppUsageTimes
            sessionStartTime = savedSessionStartTime
            
            // Update accessibility service with restored blocked state
            if (isBlocked) {
                updateAccessibilityServiceBlockedState(isBlocked, trackedApps.map { it.name }, timeLimitMinutes)
            } else {
                updateAccessibilityServiceBlockedState(isBlocked, emptyList(), 0)
            }

            // Daily reset if needed
            if (savedUsageDay == 0L) {
                // First run: set today as the usage day
                withTimeoutOrNull(2000) { storage.saveUsageDayEpoch(todayEpochDay) }
            } else if (savedUsageDay != todayEpochDay) {
                // New day: reset today's counters
                trackedApps = trackedApps.map { it.copy(minutesUsed = 0) }
                appUsageTimes = emptyMap()
                timesUnblockedToday = 0
                timesDismissedToday = 0
                sessionAppUsageTimes = emptyMap()
                sessionStartTime = 0L
                withTimeoutOrNull(2000) {
                    storage.saveAppUsageTimes(appUsageTimes)
                    storage.saveUsageDayEpoch(todayEpochDay)
                    storage.saveTimesUnblockedToday(0)
                    storage.saveTimesDismissedToday(0)
                    storage.saveSessionAppUsageTimes(emptyMap())
                    storage.saveSessionStartTime(0L)
                }
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
                        // If no apps are detected, provide some common fallback apps
                        println("DEBUG: No apps detected, using fallback apps")
                        val fallback = listOf(
                            AvailableApp("Instagram", "Social Media", "📷", "com.instagram.android"),
                            AvailableApp("TikTok", "Social Media", "🎵", "com.zhiliaoapp.musically"),
                            AvailableApp("Facebook", "Social Media", "📘", "com.facebook.katana"),
                            AvailableApp("Twitter", "Social Media", "🐦", "com.twitter.android"),
                            AvailableApp("YouTube", "Entertainment", "📺", "com.google.android.youtube"),
                            AvailableApp("Snapchat", "Social Media", "👻", "com.snapchat.android"),
                            AvailableApp("Reddit", "Social Media", "🤖", "com.reddit.frontpage"),
                            AvailableApp("LinkedIn", "Professional", "💼", "com.linkedin.android")
                        )
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
                    // If loading fails, provide fallback apps
                    println("DEBUG: Exception occurred while loading apps: ${e.message}")
                    e.printStackTrace()
                    val fallback = listOf(
                        AvailableApp("Instagram", "Social Media", "📷", "com.instagram.android"),
                        AvailableApp("TikTok", "Social Media", "🎵", "com.zhiliaoapp.musically"),
                        AvailableApp("Facebook", "Social Media", "📘", "com.facebook.katana"),
                        AvailableApp("Twitter", "Social Media", "🐦", "com.twitter.android"),
                        AvailableApp("YouTube", "Entertainment", "📺", "com.google.android.youtube"),
                        AvailableApp("Snapchat", "Social Media", "👻", "com.snapchat.android"),
                        AvailableApp("Reddit", "Social Media", "🤖", "com.reddit.frontpage"),
                        AvailableApp("LinkedIn", "Professional", "💼", "com.linkedin.android")
                    )
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
            val enabled = withTimeoutOrNull(2000) { storage.getNotificationsEnabled() } ?: false
            if (!enabled) {
                showNotificationDialog = true
            }
            val usageAllowed = withTimeoutOrNull(2000) { storage.getUsageAccessAllowed() } ?: false
            if (!usageAllowed) {
                showUsageAccessDialog = true
            }
            val accessibilityAllowed = isAccessibilityServiceEnabled()
            if (!accessibilityAllowed) {
                showAccessibilityConsentDialog = true
            }
            hasCheckedPermissionsOnDashboardThisLaunch = true
        }
    }

    // Sequentially handle Start Tracking prerequisites
    LaunchedEffect(pendingStartTracking, showNotificationDialog, showUsageAccessDialog, showNoQrCodeDialog, showNoTrackedAppsDialog, showAccessibilityConsentDialog) {
        if (!pendingStartTracking) return@LaunchedEffect

        // If any dialog is currently open, wait until user acts
        if (showNotificationDialog || showUsageAccessDialog || showNoQrCodeDialog || showNoTrackedAppsDialog || showAccessibilityConsentDialog) return@LaunchedEffect

        // 1) Notifications
        val notificationsEnabled = withTimeoutOrNull(2000) { storage.getNotificationsEnabled() } ?: false
        println("DEBUG: Checking notifications - enabled: $notificationsEnabled")
        if (!notificationsEnabled) {
            println("DEBUG: Notifications not enabled, showing dialog")
            showNotificationDialog = true
            return@LaunchedEffect
        }

        // 2) Usage access
        val usageAllowed = withTimeoutOrNull(2000) { storage.getUsageAccessAllowed() } ?: false
        println("DEBUG: Checking usage access - allowed: $usageAllowed")
        if (!usageAllowed) {
            println("DEBUG: Usage access not allowed, showing dialog")
            showUsageAccessDialog = true
            return@LaunchedEffect
        }

        // 3) Accessibility access
        val accessibilityAllowed = isAccessibilityServiceEnabled()
        println("DEBUG: Checking accessibility - enabled: $accessibilityAllowed")
        if (!accessibilityAllowed) {
            println("DEBUG: Accessibility not enabled, showing dialog")
            showAccessibilityConsentDialog = true
            return@LaunchedEffect
        }

        // 4) QR code - allow existing saved codes to satisfy this
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

        // 5) Also ensure there are tracked apps
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
        println("DEBUG: Tracking state updated to: $isTracking")
    }

    when (route) {
        null -> {
            // Show loading state while checking onboarding status
            Box(
                modifier = Modifier.fillMaxSize(),
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
            sessionElapsedSeconds = sessionElapsedSeconds,
            onToggleTracking = { 
                println("DEBUG: onToggleTracking called, current isTracking: $isTracking")
                if (isTracking) {
                    // Pause tracking - no dialogs needed
                    println("DEBUG: Pausing tracking")
                    if (isTracking) { finalizeSessionUsage() }
                    isTracking = false
                    pendingStartTracking = false
                } else {
                    // Start tracking - check permissions first
                    println("DEBUG: Starting tracking, setting pendingStartTracking = true")
                    pendingStartTracking = true
                }
            },
            onOpenQrGenerator = { 
                isSetupMode = false
                route = Route.QrGenerator 
            },
            onOpenAppSelection = { route = Route.AppSelection },
            onScanQrCode = {
                // Trigger QR code scanning to unblock apps
                coroutineScope.launch {
                    val isValid = scanQrAndDismiss(qrMessage)
                    if (isValid) {
                        // QR code is valid, unblock apps
                        // In a real implementation, this would dismiss the blocking overlay
                        // and allow app usage to continue
                    }
                }
            },
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
            onOpenPrivacyPolicy = { route = Route.PrivacyPolicy }
        )
        Route.SavedQrCodes -> SavedQrCodesScreen(
            onBack = { route = Route.Settings },
            onOpenQrGenerator = { route = Route.QrGenerator }
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
                onScanQr = {
                    coroutineScope.launch {
                        val ok = scanQrAndDismiss(qrMessage)
                        if (ok) {
                            // Finalize session usage before resetting (same as Dismiss)
                            println("DEBUG: PauseScreen onScanQr called - finalizing session usage")
                            finalizeSessionUsage()
                            println("DEBUG: trackedApps after finalize (QR): ${trackedApps.map { "${it.name}: ${it.minutesUsed}m" }}")
                            
                            // Reset session tracking state and increment unblocked counter
                            isTracking = false
                            isBlocked = false
                            // Update accessibility service with unblocked state
                            updateAccessibilityServiceBlockedState(isBlocked, emptyList(), 0)
                            // Save unblocked state to storage
                            coroutineScope.launch {
                                storage.saveBlockedState(false)
                            }
                            sessionAppUsageTimes = emptyMap()
                            sessionStartTime = 0L
                            sessionElapsedSeconds = 0L
                            timesUnblockedToday += 1
                            route = Route.Dashboard
                            
                            // Dismiss any blocking overlays
                            dismissBlockingOverlay()
                        }
                    }
                },
                onClose = { 
                    println("DEBUG: PauseScreen onClose called")
                    println("DEBUG: trackedApps before finalize: ${trackedApps.map { "${it.name}: ${it.minutesUsed}m" }}")
                    println("DEBUG: sessionAppUsageTimes before finalize: $sessionAppUsageTimes")
                    
                    // Finalize session usage before resetting (same as QR code)
                    finalizeSessionUsage()
                    
                    println("DEBUG: trackedApps after finalize: ${trackedApps.map { "${it.name}: ${it.minutesUsed}m" }}")
                    
                    // Increment dismiss counter
                    timesDismissedToday += 1
                    
                    // Reset session tracking state when dismissing (no counter increment)
                    isTracking = false
                    isBlocked = false
                    // Update accessibility service with unblocked state
                    updateAccessibilityServiceBlockedState(isBlocked, emptyList(), 0)
                    // Save unblocked state to storage
                    coroutineScope.launch {
                        storage.saveBlockedState(false)
                    }
                    sessionAppUsageTimes = emptyMap()
                    sessionStartTime = 0L
                    sessionElapsedSeconds = 0L
                    
                    println("DEBUG: trackedApps after reset: ${trackedApps.map { "${it.name}: ${it.minutesUsed}m" }}")
                    route = Route.Dashboard 
                    
                    // Dismiss any blocking overlays
                    dismissBlockingOverlay()
                }
            )
        }
        Route.PrivacyPolicy -> PrivacyPolicyScreen(
            onBack = { route = Route.Settings }
        )
    }
    
    // Notification Permission Dialog
    if (showNotificationDialog) {
        androidx.compose.material.AlertDialog(
            onDismissRequest = { showNotificationDialog = false; pendingStartTracking = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔔", fontSize = 24.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Enable Notifications?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            text = {
                Column {
                    Text(
                        "Stay informed about your app usage limits and take mindful breaks when needed.",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "You can change this setting anytime in Settings.",
                        color = Color(0xFFD1D5DB),
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
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
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1E3A5F)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Enable now", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showNotificationDialog = false; pendingStartTracking = false },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4B5563)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Not now", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            backgroundColor = Color(0xFF1A1A1A),
            contentColor = Color.White
        )
    }

    // Usage Access Permission Dialog
    if (showUsageAccessDialog) {
        androidx.compose.material.AlertDialog(
            onDismissRequest = { showUsageAccessDialog = false; pendingStartTracking = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📈", fontSize = 24.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Allow App Usage Access?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            text = {
                Column {
                    Text(
                        "We need permission to read your app usage so tracking works.",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "You can change this anytime in Settings.",
                        color = Color(0xFFD1D5DB),
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try { storage.saveUsageAccessAllowed(true) } catch (_: Exception) {}
                        }
                        showUsageAccessDialog = false
                        if (pendingStartTracking) {
                            // Continue Start Tracking flow
                            pendingStartTracking = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1E3A5F)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Allow now", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showUsageAccessDialog = false; pendingStartTracking = false },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4B5563)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Not now", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            backgroundColor = Color(0xFF1A1A1A),
            contentColor = Color.White
        )
    }

    // Accessibility Consent Dialog (shown at end of Start Tracking flow)
    if (showAccessibilityConsentDialog) {
        androidx.compose.material.AlertDialog(
            onDismissRequest = { showAccessibilityConsentDialog = false; pendingStartTracking = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🧩", fontSize = 24.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Enable Accessibility?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            text = {
                Column {
                    Text(
                        "We use Accessibility to detect which app is in the foreground for accurate tracking.",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "You can turn this off anytime in Settings.",
                        color = Color(0xFFD1D5DB),
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        openAccessibilitySettings()
                        // Don't save the preference yet - let the user actually enable it in settings
                        // The toggle will reflect the actual system state when they return
                        showAccessibilityConsentDialog = false 
                        if (pendingStartTracking) {
                            // Continue Start Tracking flow
                            pendingStartTracking = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1E3A5F)),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Allow now", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                Button(
                    onClick = { showAccessibilityConsentDialog = false; pendingStartTracking = false },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4B5563)),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Not now", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            backgroundColor = Color(0xFF1A1A1A),
            contentColor = Color.White
        )
    }


    // No Tracked Apps Dialog
    if (showNoTrackedAppsDialog) {
        androidx.compose.material.AlertDialog(
            onDismissRequest = { showNoTrackedAppsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📱", fontSize = 24.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "No tracked apps",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            text = {
                Text(
                    "You haven't selected any apps to track yet. Choose which apps to track to start.",
                    color = Color.White,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNoTrackedAppsDialog = false
                        route = Route.AppSelection
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1E3A5F)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Choose apps", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showNoTrackedAppsDialog = false },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4B5563)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Not now", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            backgroundColor = Color(0xFF1A1A1A),
            contentColor = Color.White
        )
    }

    // No QR Code Dialog
    if (showNoQrCodeDialog) {
        androidx.compose.material.AlertDialog(
            onDismissRequest = { showNoQrCodeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🧾", fontSize = 24.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "QR code required",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            text = {
                Text(
                    "You need a QR code to track your apps. Generate one to get started.",
                    color = Color.White,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNoQrCodeDialog = false
                        pendingStartTracking = false
                        fromNoQrCodeDialog = true
                        route = Route.QrGenerator
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1E3A5F)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Create QR code", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { 
                        showNoQrCodeDialog = false 
                        pendingStartTracking = false
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4B5563)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Not now", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            backgroundColor = Color(0xFF1A1A1A),
            contentColor = Color.White
        )
    }
    
    // Time Remaining Info Dialog
    if (showTimeRemainingInfoDialog) {
        androidx.compose.material.AlertDialog(
            onDismissRequest = { showTimeRemainingInfoDialog = false },
            title = {
                Text(
                    "Time Remaining",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "This is the time until your selected apps are blocked. When the time runs out, you will have to physically walk and scan your QR code to unblock your apps, then you can use them again.",
                    color = Color.White,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showTimeRemainingInfoDialog = false },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1E3A5F)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Got it", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            backgroundColor = Color(0xFF1A1A1A),
            contentColor = Color.White
        )
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
        onDownloadPdf = { text ->
            // Save a PDF using a platform stub and then continue
            val filePath = saveQrPdf(qrText = text, message = message)
            
            // Save the QR code to storage for later validation
            coroutineScope.launch {
                val qrCode = SavedQrCode(
                    id = "pause-${kotlin.random.Random.nextLong()}",
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
    sessionElapsedSeconds: Long,
    onToggleTracking: () -> Unit,
    onOpenQrGenerator: () -> Unit,
    onOpenAppSelection: () -> Unit,
    onScanQrCode: () -> Unit,
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
        onScanQrCode = onScanQrCode,
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
expect fun showBlockingOverlay(message: String)
expect fun dismissBlockingOverlay()
expect fun checkAndShowOverlayIfBlocked(trackedAppNames: List<String>, isBlocked: Boolean, timeLimitMinutes: Int)
expect suspend fun scanQrAndDismiss(expectedMessage: String): Boolean
expect fun getCurrentTimeMillis(): Long
expect fun setOnTimerResetCallback(callback: (() -> Unit)?)
expect fun setOnDismissCallback(callback: (() -> Unit)?)
expect fun updateAccessibilityServiceBlockedState(isBlocked: Boolean, trackedAppNames: List<String>, timeLimitMinutes: Int)
expect fun openEmailClient(recipient: String)

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
                title = "Welcome to AntiScroll",
                description = "Create boundaries for your app time, and walk or move around when you hit them.",
                showLogo = true
            ),
            OnboardingPage(
                title = "Walk to Unlock Apps",
                description = "Print QR codes and place them around your home and office. When time is up, you'll need to physically walk to scan them.",
                imagePath = "images/onboarding/walking_onboarding.png"
            ),
            OnboardingPage(
                title = "Pause Partners",
                description = "Ask trusted persons to be digital break partners. They can save the QR codes to help you unlock apps.",
                primaryCta = "Get Started",
                imagePath = "images/onboarding/two_people.png"
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
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
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
                    color = Color(0xFFD1D5DB)
                )
            }
        }
    }
}

@Composable
private fun QrGeneratorContent(
    message: String,
    onMessageChange: (String) -> Unit,
    onDownloadPdf: (String) -> Unit,
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
    val qrText = remember(message, qrVersion) { "QR:$message:v$qrVersion" }

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
                Text("Create QR codes to place around your home or share with your digital pause partner", fontSize = 14.sp, color = Color(0xFFD1D5DB))
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
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
                        .size(200.dp)
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
                    Spacer(Modifier.height(12.dp))
                    Text(message, color = Color.White, fontSize = 16.sp)
                    
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ID: pause-${kotlin.random.Random.nextLong().toString().takeLast(6)}...", color = Color(0xFFD1D5DB), fontSize = 12.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("⧉", color = Color(0xFFD1D5DB), fontSize = 12.sp)
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Generate New QR Code Button inside the card
                    Button(
                        onClick = { 
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
        
        // Customize Message Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(13.dp)
            ) {
                Text("Customize Message", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = onMessageChange,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF1E3A5F),
                        unfocusedBorderColor = Color(0xFF4B5563),
                        textColor = Color.White,
                        cursorColor = Color(0xFF1E3A5F)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Download Button - only enabled when QR is generated
        Button(
            onClick = { 
                if (downloadSuccess) {
                    // If already downloaded, navigate to dashboard
                    onClose()
                } else {
                    // Download PDF first
                    onDownloadPdf(qrText)
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
            Text(if (downloadSuccess) "✓" else "↓", color = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(
                if (downloadSuccess) {
                    if (isSetupMode) "Done" else "Go to Dashboard"
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
        
        Spacer(Modifier.height(24.dp))
        
        // How ScrollFree QR Code Works Section - show for first visit or when coming from no QR code dialog
        if (isFirstVisit || fromNoQrCodeDialog) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFF2C2C2C),
                shape = RoundedCornerShape(12.dp)
            ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "How ScrollFree QR Code Works:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(Modifier.height(12.dp))
                
                Text(
                    "1. Print your QR code and place it somewhere you have to walk to (kitchen, bedroom, upstairs, etc.).\n\n" +
                    "2. No printer? Share a screenshot of your QR code with a family member, friend, or housemate—your digital pause partner—and ask them to keep it on their phone.\n\n" +
                    "3. When your time limit ends, you'll need to scan the QR code—either where you placed it or from your partner—to unlock your apps.\n\n" +
                    "4. This makes you step away from your phone for a natural pause, and if scanning from your digital pause partner, adds a little extra social time!",
                    fontSize = 14.sp,
                    color = Color(0xFFD1D5DB),
                    lineHeight = 20.sp
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
    onScanQrCode: () -> Unit,
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
                Text("AntiScroll", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Monday, Sep 22", fontSize = 14.sp, color = Color(0xFFD1D5DB))
            }
            Row {
                Text("⚙", fontSize = 24.sp, color = Color(0xFFD1D5DB), modifier = Modifier.clickable { onOpenSettings() })
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
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
                    // Info button in top-right corner
                    IconButton(
                        onClick = { onShowTimeRemainingInfo() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info about time remaining",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
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
                        Text("${remaining}m", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        val minuteLabel = if (remaining == 1) "minute" else "minutes"
                        Text("$minuteLabel remaining until pause time", fontSize = 14.sp, color = Color.White)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val timeLimitLabel = if (timeLimitMinutes == 1) "minute" else "minutes"
                            Text("Time Limit: ${timeLimitMinutes} $timeLimitLabel", fontSize = 12.sp, color = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "✏",
                                fontSize = 12.sp,
                                color = Color(0xFFD1D5DB),
                                modifier = Modifier.clickable { onOpenDurationSetting() }
                            )
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
                        "Print your personal QR codes and place them around your home. When your time limit is reached, you'll need to walk to scan one - encouraging healthy movement breaks!",
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
        androidx.compose.material.AlertDialog(
            onDismissRequest = { showAccountabilityDialog = false },
            title = {
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
            },
            text = {
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
            },
            confirmButton = {
                Button(
                    onClick = { showAccountabilityDialog = false },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Got it!", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            backgroundColor = Color(0xFF1A1A1A),
            contentColor = Color.White
        )
    }
}

@Composable
private fun SettingsScreen(
    onBack: () -> Unit,
    onOpenSavedQrCodes: () -> Unit,
    onNotificationsTurnedOff: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit
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
                Text("Customize your digital wellness experience", fontSize = 14.sp, color = Color(0xFFD1D5DB))
            }
        }
        Spacer(Modifier.height(24.dp))

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

        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(16.dp)
        ) {
            var usageAccessAllowed by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                try {
                    usageAccessAllowed = storage.getUsageAccessAllowed()
                } catch (_: Exception) { usageAccessAllowed = false }
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
                            usageAccessAllowed = enabled
                            // Persist change
                            coroutineScope.launch {
                                try { storage.saveUsageAccessAllowed(enabled) } catch (_: Exception) {}
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

        Spacer(Modifier.height(8.dp))

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
                    } catch (_: Exception) {}
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
                            // Always open system settings - let user manage the permission there
                            // The toggle will reflect actual system state when user returns
                            openAccessibilitySettings()
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

        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Camera Access", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("📷", fontSize = 24.sp, color = Color(0xFF1E3A5F))
                }
                Spacer(Modifier.height(8.dp))
                Text("Camera permission is required to scan QR codes for pause functionality. When you first scan a QR code, you'll be prompted to allow camera access. You can change this permission anytime in your device's app settings.", color = Color(0xFFD1D5DB), fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(8.dp))

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

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { openEmailClient("contact.antiscroll@gmail.com") },
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
    onOpenQrGenerator: () -> Unit
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
                    "How AntiScroll QR Code Works:",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        
                        Spacer(Modifier.height(12.dp))
                        
                        Text(
                            "1. Print your QR code and place it somewhere you have to walk to (kitchen, bedroom, upstairs, etc.).\n\n" +
                            "2. No printer? Share a screenshot of your QR code with a family member, friend, or housemate—your digital pause partner—and ask them to keep it on their phone.\n\n" +
                            "3. When your time limit ends, you'll need to scan the QR code—either where you placed it or from your partner—to unlock your apps.\n\n" +
                            "4. This makes you step away from your phone for a natural pause, and if scanning from your digital pause partner, adds a little extra social time!",
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
                                    "How AntiScroll QR Code Works:",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                
                                Spacer(Modifier.height(12.dp))
                                
                                Text(
                                    "1. Print your QR code and place it somewhere you have to walk to (kitchen, bedroom, upstairs, etc.).\n\n" +
                                    "2. No printer? Share a screenshot of your QR code with a family member, friend, or housemate—your digital pause partner—and ask them to keep it on their phone.\n\n" +
                                    "3. When your time limit ends, you'll need to scan the QR code—either where you placed it or from your partner—to unlock your apps.\n\n" +
                                    "4. This makes you step away from your phone for a natural pause, and if scanning from your digital pause partner, adds a little extra social time!",
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
    onScanQr: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A)),
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
                        text = "You have used your tracked apps for $durationText",
                        color = Color(0xFFBFC7C2)
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
                        text = "Walk to your QR code to unlock your apps",
                        color = Color(0xFFBFC7C2),
                        fontSize = 14.sp
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
                Column(horizontalAlignment = Alignment.Start) {
                    Text("Scan My QR Code", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Get up and scan your printed QR code", color = Color(0xFFE3F2FD), fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "× Dismiss",
                color = Color(0xFF9CA3AF),
                modifier = Modifier.clickable { onClose() }
            )
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
    val selectedCount = availableApps.count { it.isSelected }
    
    // Show loading state if apps are being loaded
    if (isLoading || availableApps.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
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
        
        // Apps list
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(availableApps) { app ->
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
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50)),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            Text(
                "Done",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
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
                        color = if (app.isSelected) Color(0xFF4CAF50) else Color(0xFF4B5563),
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
                    "Last updated: December 2024",
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
                    "AntiScroll (\"we,\" \"our,\" or \"us\") is committed to protecting your privacy. This Privacy Policy explains how we collect, use, disclose, and safeguard your information when you use our digital wellness application.",
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
                    "• App Usage Data: We track which apps you use and for how long to help you manage your digital wellness\n" +
                    "• Device Information: Basic device information necessary for app functionality\n" +
                    "• QR Code Data: QR codes you generate and scan for pause functionality\n" +
                    "• Settings Preferences: Your app settings and preferences",
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
                    "• Provide digital wellness tracking and pause functionality\n" +
                    "• Generate and manage QR codes for your pause system\n" +
                    "• Send notifications when time limits are reached\n" +
                    "• Improve app performance and user experience",
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
                    "• All data is stored locally on your device\n" +
                    "• We do not transmit your personal data to external servers\n" +
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
                    "• Accessibility Service: Used to show pause overlays when time limits are reached\n" +
                    "• Camera: Used to scan QR codes for pause functionality\n" +
                    "• Notifications: Used to alert you when time limits are reached",
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
                    "Email: contact.antiscroll@gmail.com",
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
    val quickSelectOptions = listOf(5, 10, 15, 45, 60, 90, 120)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
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
                        color = Color(0xFF1E3A5F)
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
        
        Spacer(Modifier.weight(1f))
        
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
    }
}