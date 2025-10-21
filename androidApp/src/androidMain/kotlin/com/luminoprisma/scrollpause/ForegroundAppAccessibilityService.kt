package com.luminoprisma.scrollpause

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import android.content.SharedPreferences
import createAppStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

class ForegroundAppAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile
        private var currentForegroundPackage: String? = null
        @Volatile private var pendingShowMessage: String? = null
        @Volatile private var pendingHide: Boolean = false
        @Volatile private var isBlocked: Boolean = false
        @Volatile private var trackedAppNames: List<String> = emptyList()
        @Volatile private var timeLimitMinutes: Int = 0
        
        // Time tracking variables
        @Volatile private var appActiveSince: Long = 0L
        @Volatile private var appUsageTimes: MutableMap<String, Long> = mutableMapOf() // app name -> seconds used
        @Volatile private var hasLoadedExistingUsage: Boolean = false // Flag to prevent multiple loads
        
        fun getCurrentForegroundPackage(): String? = currentForegroundPackage
        
        fun setCurrentForegroundPackage(packageName: String?) {
            currentForegroundPackage = packageName
            println("DEBUG: ForegroundAppAccessibilityService - Set foreground app: $packageName")
        }
        
        fun isBlocked(): Boolean = isBlocked
        
        fun getCurrentUsageTimes(): Map<String, Long> = appUsageTimes.toMap()

        fun setPendingShow(message: String) {
            pendingShowMessage = message
            println("DEBUG: PendingCommands - setPendingShow('$message')")
        }

        fun setPendingHide() {
            pendingHide = true
            println("DEBUG: PendingCommands - setPendingHide()")
        }
        
        @Volatile private var appContextRef: Context? = null

        fun setBlockedState(blocked: Boolean, trackedApps: List<String>, timeLimit: Int) {
            // Only update state if we're actually setting a blocking state or if we have tracked apps
            // This prevents the main app from clearing our state when it's killed
            if (blocked || trackedApps.isNotEmpty() || timeLimit > 0) {
                isBlocked = blocked
                trackedAppNames = trackedApps
                timeLimitMinutes = timeLimit
                println("DEBUG: ForegroundAppAccessibilityService - Set blocked state: blocked=$blocked, apps=$trackedApps, limit=$timeLimit")

                // Persist the blocking state so it survives app kills
                appContextRef?.let { context ->
                    try {
                        val prefs = context.getSharedPreferences("scrollpause_prefs", Context.MODE_PRIVATE)
                        prefs.edit().apply {
                            putBoolean(KEY_BLOCKED, blocked)
                            putString(KEY_TRACKED_APPS, trackedApps.joinToString(","))
                            putInt(KEY_TIME_LIMIT, timeLimit)
                            putLong("blocking_start_time", System.currentTimeMillis())
                            apply()
                        }
                        println("DEBUG: ForegroundAppAccessibilityService - Persisted blocking state: blocked=$blocked")
                    } catch (e: Exception) {
                        println("DEBUG: ForegroundAppAccessibilityService - Error persisting state: ${e.message}")
                    }
                }
            } else {
                println("DEBUG: ForegroundAppAccessibilityService - Ignoring state clear attempt (blocked=$blocked, apps=$trackedApps, limit=$timeLimit)")
            }

            // If blocking just turned on while user is already in a tracked app,
            // immediately launch the Pause screen without waiting for another event
            if (blocked) {
                val currentPkg = currentForegroundPackage
                val isTrackedNow = isPackageTracked(currentPkg, trackedApps)
                println("DEBUG: setBlockedState - currentPkg=$currentPkg, isTrackedNow=$isTrackedNow")
                if (isTrackedNow) {
                    val ctx = appContextRef
                    if (ctx != null) {
                        try {
                            val message = if (timeLimit > 0) {
                                "Take a mindful pause - you've reached your time limit of ${timeLimit} minutes"
                            } else {
                                "Take a mindful pause"
                            }
                            val intent = Intent(ctx, PauseActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                putExtra("message", message)
                            }
                            ctx.startActivity(intent)
                        } catch (e: Exception) {
                            println("DEBUG: setBlockedState - failed to start activity from companion: ${e.message}")
                        }
                    }
                }
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

        private const val PREFS_NAME = "scrollpause_prefs"
        private const val KEY_BLOCKED = "blocked"
        private const val KEY_TRACKED_APPS = "tracked_apps_csv"
        private const val KEY_TIME_LIMIT = "time_limit_minutes"
    }

    private fun isIgnoredPackage(pkg: String?): Boolean {
        if (pkg.isNullOrBlank()) return true
        val self = applicationContext.packageName
        // Do NOT ignore our own app – we need to broadcast when our UI is foregrounded
        if (pkg == self) return false
        return pkg.startsWith("com.google.android.inputmethod") ||
            pkg.startsWith("com.android.inputmethod") ||
            pkg.startsWith("com.google.android.gms") ||
            pkg.startsWith("com.android.systemui")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        println("DEBUG: ForegroundAppAccessibilityService - service connected")
        // capture app context for companion usage
        appContextRef = applicationContext
        registerRedirectReceiver()
        loadStateFromPreferences()
        
        // Load existing usage from storage to restore accumulated usage
        try {
            val storage = createAppStorage()
            val existingUsage = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
                kotlinx.coroutines.withTimeoutOrNull(2000) { storage.getAppUsageTimes() } ?: emptyMap()
            }
            println("DEBUG: ForegroundAppAccessibilityService - Loading existing usage on service start: $existingUsage")
            if (existingUsage.isNotEmpty()) {
                // Load existing usage into in-memory map (replace, don't add, since this is the source of truth)
                appUsageTimes.clear()
                for ((key, seconds) in existingUsage) {
                    appUsageTimes[key] = seconds
                }
                println("DEBUG: ForegroundAppAccessibilityService - Loaded existing usage on service start: $existingUsage")
                println("DEBUG: ForegroundAppAccessibilityService - Updated in-memory usage: $appUsageTimes")
            } else {
                println("DEBUG: ForegroundAppAccessibilityService - No existing usage found in storage")
            }
            // Mark as loaded to prevent multiple loads
            hasLoadedExistingUsage = true
        } catch (e: Exception) {
            println("DEBUG: ForegroundAppAccessibilityService - Error loading existing usage on service start: ${e.message}")
        }
        
        // Additional debugging for persistent blocking
        println("DEBUG: ForegroundAppAccessibilityService - Service connected, checking if we should restore blocking")
        println("DEBUG: ForegroundAppAccessibilityService - Current state: isBlocked=$isBlocked, trackedApps=$trackedAppNames, timeLimit=$timeLimitMinutes")
        try {
            android.widget.Toast.makeText(
                this,
                "ScrollPause connected (blocked=" + isBlocked + ", apps=" + trackedAppNames.size + ")",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        } catch (_: Exception) {}
        
        // Start periodic connection monitoring to ensure service stays connected
        startConnectionMonitoring()
        
        // Apply any pending command
        val msg = pendingShowMessage
        if (msg != null) {
            println("DEBUG: onServiceConnected - applying pending SHOW")
            try {
                val intent = Intent(this, PauseActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra("message", msg)
                }
                startActivity(intent)
            } catch (e: Exception) {
                println("DEBUG: onServiceConnected - error launching PauseActivity: ${e.message}")
            }
            pendingShowMessage = null
        }
        if (pendingHide) {
            println("DEBUG: onServiceConnected - applying pending HIDE")
            // No action needed - pause screen handles its own dismissal
            pendingHide = false
        }
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        println("DEBUG: onAccessibilityEvent - eventType=" + event?.eventType + ", isBlocked=" + isBlocked + ", trackedApps=" + trackedAppNames)
        var pkg = event?.packageName?.toString()
        if (isIgnoredPackage(pkg)) {
            // Fallback: try active window root
            val root = rootInActiveWindow
            val rootPkg = root?.packageName?.toString()
            if (!isIgnoredPackage(rootPkg)) {
                pkg = rootPkg
            } else {
                // Ignore this update to avoid overwriting with keyboard/system
                return
            }
        }
        // Only accept non-ignored packages
        if (!isIgnoredPackage(pkg)) {
            val previousPkg = currentForegroundPackage
            // Only flush and reset if the foreground package actually changed
            if (previousPkg != pkg) {
                // Track time for the previous app before switching
                trackTimeForCurrentApp()

                currentForegroundPackage = pkg
                appActiveSince = System.currentTimeMillis()
                println("DEBUG: ForegroundAppAccessibilityService - New foreground app: $pkg (event=${event?.eventType})")
                val intent = android.content.Intent("com.luminoprisma.scrollpause.FOREGROUND_APP_CHANGED").apply {
                    putExtra("pkg", pkg)
                    setPackage(applicationContext.packageName)
                }
                applicationContext.sendBroadcast(intent)

                // Check if this app should be blocked and redirect to pause screen
                checkAndRedirectToPauseApp(pkg)
            } else {
                // Same app; do not reset appActiveSince here. Periodic accrual will handle time tracking.
                // However, if periodic monitor hasn't run yet, flush on event when >=5s elapsed.
                val now = System.currentTimeMillis()
                val elapsedSeconds = if (appActiveSince > 0L) (now - appActiveSince) / 1000L else 0L
                if (elapsedSeconds >= 5L && !isBlocked) {
                    println("DEBUG: onAccessibilityEvent - same app elapsed=$elapsedSeconds s; flushing via trackTimeForCurrentApp()")
                    trackTimeForCurrentApp()
                    appActiveSince = now
                }
            }
        }
    }

    override fun onInterrupt() {
    }
    
    private fun trackTimeForCurrentApp() {
        if (currentForegroundPackage == null || appActiveSince == 0L) {
            println("DEBUG: AccessibilityService.trackTimeForCurrentApp - early return: currentForegroundPackage=$currentForegroundPackage, appActiveSince=$appActiveSince")
            return
        }
        
        // Load existing usage from storage only once when service starts tracking
        if (!hasLoadedExistingUsage) {
            try {
                val storage = createAppStorage()
                val existingUsage = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
                    kotlinx.coroutines.withTimeoutOrNull(2000) { storage.getAppUsageTimes() } ?: emptyMap()
                }
                println("DEBUG: AccessibilityService - Loading existing usage from storage: $existingUsage")
                if (existingUsage.isNotEmpty()) {
                    // Load existing usage into in-memory map
                    for ((key, seconds) in existingUsage) {
                        appUsageTimes[key] = seconds
                    }
                    println("DEBUG: AccessibilityService - Loaded existing usage: $appUsageTimes")
                } else {
                    println("DEBUG: AccessibilityService - No existing usage found in storage")
                }
                hasLoadedExistingUsage = true
            } catch (e: Exception) {
                println("DEBUG: AccessibilityService - Error loading existing usage: ${e.message}")
            }
        } else {
            println("DEBUG: AccessibilityService - Usage already loaded, skipping reload: $appUsageTimes")
        }
        
        val currentTime = System.currentTimeMillis()
        val timeSpent = (currentTime - appActiveSince) / 1000L // Convert to seconds
        println("DEBUG: AccessibilityService.trackTimeForCurrentApp - timeSpent=$timeSpent, currentForegroundPackage=$currentForegroundPackage, trackedAppNames=$trackedAppNames")
        
        if (timeSpent > 0) {
            // Find which tracked app was active and add the time
            for (appName in trackedAppNames) {
                val expectedPackage = getPackageNameForTrackedApp(appName)
                println("DEBUG: AccessibilityService.trackTimeForCurrentApp - checking appName=$appName, expectedPackage=$expectedPackage")
                
                if (currentForegroundPackage == expectedPackage) {
                    // Always store by package ID to avoid alias mismatches (e.g., IG/Insta)
                    val key = expectedPackage
                    val currentUsage = appUsageTimes[key] ?: 0L
                    val newUsage = currentUsage + timeSpent
                    appUsageTimes[key] = newUsage
                    println("DEBUG: AccessibilityService - Added $timeSpent seconds to $key (total: $newUsage)")
                    
                    // Persist the updated usage time using shared AppStorage
                    try {
                        val storage = createAppStorage()
                        // Use runBlocking to ensure synchronous save
                        kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                // Load existing usage from storage to avoid overwriting other apps' data
                                val existingUsage = storage.getAppUsageTimes()
                                println("DEBUG: AccessibilityService - Existing usage from storage: $existingUsage")
                                
                                // Merge current session usage with existing usage
                                val mergedUsage = existingUsage.toMutableMap()
                                for ((nameOrPkg, secs) in appUsageTimes) {
                                    // Only save by package ID to avoid double counting
                                    val pkg = getPackageNameForTrackedApp(nameOrPkg)
                                    if (pkg.isNotEmpty()) {
                                        mergedUsage[pkg] = secs
                                    }
                                }
                                
                                storage.saveAppUsageTimes(mergedUsage)
                                val epochDay = System.currentTimeMillis() / 86_400_000L
                                storage.saveUsageDayEpoch(epochDay)
                                println("DEBUG: AccessibilityService - Merged and persisted usage: $mergedUsage (updated $key=$newUsage)")
                            } catch (e: Exception) {
                                println("DEBUG: AccessibilityService - Error persisting usage via AppStorage (sync): ${e.message}")
                            }
                        }
                    } catch (e: Exception) {
                        println("DEBUG: AccessibilityService - Error scheduling persist via AppStorage: ${e.message}")
                    }
                    
                    // Check if time limit has been reached
                    checkTimeLimitForApp(key, newUsage)
                    break
                }
            }
        }
    }
    
    private fun getPackageNameForTrackedApp(appName: String): String {
        val normalized = appName.trim()
        return when (normalized.lowercase()) {
            "chrome" -> "com.android.chrome"
            "youtube" -> "com.google.android.youtube"
            "messages" -> "com.google.android.apps.messaging"
            "gmail" -> "com.google.android.gm"
            "whatsapp" -> "com.whatsapp"
            "youtube music" -> "com.google.android.apps.youtube.music"
            "instagram" -> "com.instagram.android"
            "facebook" -> "com.facebook.katana"
            "tiktok" -> "com.zhiliaoapp.musically"
            else -> if (normalized.contains('.')) normalized else ""
        }
    }
    
    private fun checkTimeLimitForApp(appNameOrPkgKey: String, totalSeconds: Long) {
        if (timeLimitMinutes <= 0) {
            // Sanity reload: tracked apps exist but limit is zero → try reloading once
            if (trackedAppNames.isNotEmpty()) {
                println("DEBUG: AccessibilityService - timeLimitMinutes=0 with tracked apps present, reloading prefs (totalSeconds=" + totalSeconds + ")")
                loadStateFromPreferences()
            }
            if (timeLimitMinutes <= 0) {
                println("DEBUG: AccessibilityService - still no timeLimit after reload, skipping check")
                return
            }
        }
        val timeLimitSeconds = timeLimitMinutes * 60L
        println("DEBUG: AccessibilityService - checking time limit: app=" + appNameOrPkgKey + ", used=" + totalSeconds + "s, limitMinutes=" + timeLimitMinutes + ", limitSeconds=" + timeLimitSeconds)
        if (totalSeconds >= timeLimitSeconds) {
            println("DEBUG: AccessibilityService - Time limit reached for $appNameOrPkgKey: $totalSeconds seconds >= $timeLimitSeconds seconds")
            
            // Ensure persisted usage reflects at least the full limit duration for this app
            try {
                val current = appUsageTimes[appNameOrPkgKey] ?: 0L
                val normalized = kotlin.math.max(current, timeLimitSeconds)
                appUsageTimes[appNameOrPkgKey] = normalized
                val storage = createAppStorage()
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    try {
                        val toSave = mutableMapOf<String, Long>()
                        for ((nameOrPkg, secs) in appUsageTimes) {
                            toSave[nameOrPkg] = secs
                            val pkg = getPackageNameForTrackedApp(nameOrPkg)
                            if (pkg.isNotEmpty()) toSave[pkg] = secs
                        }
                        storage.saveAppUsageTimes(toSave)
                        val epochDay = System.currentTimeMillis() / 86_400_000L
                        storage.saveUsageDayEpoch(epochDay)
                        println("DEBUG: AccessibilityService - Normalized persisted usage for limit: $appNameOrPkgKey=$normalized")
                    } catch (e: Exception) {
                        println("DEBUG: AccessibilityService - Error normalizing persisted usage: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                println("DEBUG: AccessibilityService - Error ensuring minimum persisted usage: ${e.message}")
            }
            
            // Set blocked state and trigger PauseScreen
            isBlocked = true
            
            // Persist the blocked state
            appContextRef?.let { context ->
                try {
                    val prefs = context.getSharedPreferences("scrollpause_prefs", Context.MODE_PRIVATE)
                    prefs.edit().apply {
                        putBoolean(KEY_BLOCKED, true)
                        apply()
                    }
                    println("DEBUG: AccessibilityService - Persisted blocked state: blocked=true")
                } catch (e: Exception) {
                    println("DEBUG: AccessibilityService - Error persisting blocked state: ${e.message}")
                }
            }
            
            // Launch PauseScreen immediately
            val message = "Take a mindful pause - you've reached your time limit of ${timeLimitMinutes} minutes"
            try {
                val intent = Intent(this, PauseActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra("message", message)
                }
                startActivity(intent)
                println("DEBUG: AccessibilityService - Launched PauseScreen for time limit reached")
            } catch (e: Exception) {
                println("DEBUG: AccessibilityService - Error launching PauseScreen: ${e.message}")
            }
        }
    }
    
    private fun startConnectionMonitoring() {
        // Check connection every 5 seconds
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                try {
                    // Check if we have a valid connection by trying to access service info
                    val serviceInfo = serviceInfo
                    if (serviceInfo == null) {
                        println("DEBUG: startConnectionMonitoring - Service connection lost, attempting to restore")
                        // Try to restore connection by calling onServiceConnected again
                        onServiceConnected()
                    } else {
                        // Connection is good, check if we need to restore state
                        if (!isBlocked && trackedAppNames.isEmpty()) {
                            println("DEBUG: startConnectionMonitoring - Service connected but no blocking state, checking SharedPreferences")
                            loadStateFromPreferences()
                        } else if (!isBlocked && trackedAppNames.isNotEmpty() && timeLimitMinutes <= 0) {
                            // We have tracked apps but no time limit; reload to get the latest limit
                            println("DEBUG: startConnectionMonitoring - Tracked apps present but timeLimitMinutes=0, reloading prefs")
                            loadStateFromPreferences()
                        }
                        
                        // Also track time for the current app if it's been active for a while
                        if (!isBlocked && currentForegroundPackage != null && appActiveSince > 0) {
                            val currentTime = System.currentTimeMillis()
                            val elapsedSeconds = (currentTime - appActiveSince) / 1000L
                            if (elapsedSeconds >= 5L) { // Track every 5 seconds for apps that stay in foreground
                                trackTimeForCurrentApp()
                                appActiveSince = currentTime // Reset the timer
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("DEBUG: startConnectionMonitoring - Error checking connection: ${e.message}")
                }
                // Schedule next check in 5 seconds
                handler.postDelayed(this, 5000)
            }
        }
        handler.postDelayed(runnable, 5000)
    }
    
    private fun checkAndRedirectToPauseApp(packageName: String?) {
        if (!isBlocked || packageName.isNullOrBlank()) return
        
        println("DEBUG: checkAndRedirectToPauseApp - checking package: $packageName, blocked: $isBlocked")
        
        // Check if the current foreground app is one of the tracked apps
        val isTrackedApp = trackedAppNames.any { appName ->
            val normalized = appName.trim()
            // If it looks like a package name (has a dot), compare directly
            if (normalized.contains('.')) {
                return@any packageName == normalized
            }
            // Map common names to package IDs
            val expectedPackage = when (normalized.lowercase()) {
                "chrome" -> "com.android.chrome"
                "youtube" -> "com.google.android.youtube"
                "messages" -> "com.google.android.apps.messaging"
                "gmail" -> "com.google.android.gm"
                "whatsapp" -> "com.whatsapp"
                "youtube music" -> "com.google.android.apps.youtube.music"
                else -> null
            }
            if (expectedPackage != null) {
                packageName == expectedPackage
            } else {
                // Last resort: loose match by substring
                packageName.contains(normalized.lowercase())
            }
        }
        
        println("DEBUG: checkAndRedirectToPauseApp - isTrackedApp: $isTrackedApp for package: $packageName")
        
        if (isTrackedApp) {
            // User is trying to use a tracked app while blocked, redirect them to our Pause screen
            println("DEBUG: checkAndRedirectToPauseApp - redirecting to PauseActivity for blocked tracked app: $packageName")
            val message = if (timeLimitMinutes > 0) {
                "Take a mindful pause - you've reached your time limit of ${timeLimitMinutes} minutes"
            } else {
                "Take a mindful pause"
            }
            try {
                // Redirect to our app's Pause screen instead of showing overlay
                val intent = Intent(this, PauseActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra("message", message)
                }
                startActivity(intent)
            } catch (e: Exception) {
                println("DEBUG: checkAndRedirectToPauseApp - error redirecting to PauseActivity: ${e.message}")
            }
        }
    }

    // Redirect management - no more overlays, just redirects to our app
    private fun registerRedirectReceiver() {
        try {
            val showFilter = IntentFilter("com.luminoprisma.scrollpause.SHOW_PAUSE_SCREEN")
            val hideFilter = IntentFilter("com.luminoprisma.scrollpause.HIDE_PAUSE_SCREEN")
            val stateFilter = IntentFilter("com.luminoprisma.scrollpause.STATE_CHANGED")

            println("DEBUG: registerRedirectReceiver - registering receivers")
            val showReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    println("DEBUG: receiver SHOW_PAUSE_SCREEN received - intent=$intent")
                    val message = intent?.getStringExtra("message") ?: "Take a mindful pause"
                    try {
                        val activityIntent = Intent(this@ForegroundAppAccessibilityService, PauseActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            putExtra("message", message)
                        }
                        startActivity(activityIntent)
                    } catch (e: Exception) {
                        println("DEBUG: SHOW_PAUSE_SCREEN - failed to launch activity: ${e.message}")
                    }
                }
            }
            val hideReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    println("DEBUG: receiver HIDE_PAUSE_SCREEN received - intent=$intent")
                    // No action needed - the pause screen will handle its own dismissal
                }
            }
            val stateReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    println("DEBUG: receiver STATE_CHANGED received - intent=$intent")
                    try {
                        // Prefer extras if provided
                        val extrasAppsCsv = intent?.getStringExtra("trackedApps") ?: ""
                        val extrasLimit = intent?.getIntExtra("timeLimit", -1) ?: -1
                        val extrasBlocked = intent?.getBooleanExtra("isBlocked", false) ?: false
                        if (extrasAppsCsv.isNotBlank() || extrasLimit >= 0) {
                            val apps = if (extrasAppsCsv.isBlank()) emptyList() else extrasAppsCsv.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                            println("DEBUG: STATE_CHANGED - adopting extras: blocked=" + extrasBlocked + ", apps=" + apps.size + ", limit=" + extrasLimit)
                            // Update in-memory
                            if (apps.isNotEmpty()) trackedAppNames = apps
                            if (extrasLimit >= 0) timeLimitMinutes = extrasLimit
                            isBlocked = extrasBlocked
                            // Persist so it survives UI death
                            val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            prefs.edit().apply {
                                putBoolean(KEY_BLOCKED, isBlocked)
                                putString(KEY_TRACKED_APPS, trackedAppNames.joinToString(","))
                                putInt(KEY_TIME_LIMIT, timeLimitMinutes)
                                apply()
                            }

                            // If unblocked, persist current in-memory usage before clearing to avoid losing accumulated time
                            if (!isBlocked) {
                                // Reset the flag so we reload usage data on next tracking
                                hasLoadedExistingUsage = false
                                println("DEBUG: STATE_CHANGED - Reset hasLoadedExistingUsage flag to reload usage on next tracking")
                                
                                // Persist current in-memory usage before clearing
                                if (appUsageTimes.isNotEmpty()) {
                                    try {
                                        val storage = createAppStorage()
                                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                            try {
                                                // Load existing usage from storage to avoid overwriting other apps' data
                                                val existingUsage = storage.getAppUsageTimes()
                                                println("DEBUG: STATE_CHANGED - Loading existing usage from storage: $existingUsage")
                                                val toSave = existingUsage.toMutableMap()
                                                for ((nameOrPkg, secs) in appUsageTimes) {
                                                    // Update usage for this app (replace, not add, since appUsageTimes contains cumulative session data)
                                                    toSave[nameOrPkg] = secs
                                                    val pkg = getPackageNameForTrackedApp(nameOrPkg)
                                                    if (pkg.isNotEmpty()) toSave[pkg] = secs
                                                }
                                                storage.saveAppUsageTimes(toSave)
                                                val epochDay = System.currentTimeMillis() / 86_400_000L
                                                storage.saveUsageDayEpoch(epochDay)
                                                println("DEBUG: STATE_CHANGED - persisted merged usage before clearing: $toSave (from in-memory: $appUsageTimes)")
                                            } catch (e: Exception) {
                                                println("DEBUG: STATE_CHANGED - Error persisting usage before clearing: ${e.message}")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        println("DEBUG: STATE_CHANGED - Error scheduling persist before clearing: ${e.message}")
                                    }
                                }
                                appUsageTimes.clear()
                                println("DEBUG: STATE_CHANGED - unblocked; persisted and cleared in-memory usage")
                            }
                        } else {
                            // Fallback to SharedPreferences
                            loadStateFromPreferences()
                        }
                    } catch (_: Exception) {
                        loadStateFromPreferences()
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(showReceiver, showFilter, Context.RECEIVER_NOT_EXPORTED)
                registerReceiver(hideReceiver, hideFilter, Context.RECEIVER_NOT_EXPORTED)
                registerReceiver(stateReceiver, stateFilter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                registerReceiver(showReceiver, showFilter)
                @Suppress("DEPRECATION")
                registerReceiver(hideReceiver, hideFilter)
                @Suppress("DEPRECATION")
                registerReceiver(stateReceiver, stateFilter)
            }
        } catch (_: Exception) {
        }
    }

    private fun loadStateFromPreferences() {
        try {
            val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val blocked = prefs.getBoolean(KEY_BLOCKED, false)
            val trackedCsv = prefs.getString(KEY_TRACKED_APPS, "") ?: ""
            val timeLimit = prefs.getInt(KEY_TIME_LIMIT, 0)
            val apps = if (trackedCsv.isBlank()) emptyList() else trackedCsv.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            
            println("DEBUG: loadStateFromPreferences - Loading state from SharedPreferences")
            println("DEBUG: loadStateFromPreferences - Raw data: blocked=$blocked, csv='$trackedCsv', limit=$timeLimit")
            println("DEBUG: loadStateFromPreferences - Parsed apps: $apps")
            println("DEBUG: loadStateFromPreferences - All preferences: ${prefs.all}")
            
            // Debug: Check what's in storage before loading
            try {
                val storage = createAppStorage()
                val debugSavedMap = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
                    kotlinx.coroutines.withTimeoutOrNull(2000) { storage.getAppUsageTimes() } ?: emptyMap()
                }
                println("DEBUG: loadStateFromPreferences - Storage contains: $debugSavedMap")
            } catch (e: Exception) {
                println("DEBUG: loadStateFromPreferences - Error checking storage: ${e.message}")
            }
            
            // Always update the state, even if it's the same
            isBlocked = blocked
            trackedAppNames = apps
            timeLimitMinutes = timeLimit
            
            // Usage data is already loaded in onServiceConnected, no need to reload here
            // This prevents clearing the usage data that was loaded on service start
            println("DEBUG: loadStateFromPreferences - Using existing appUsageTimes: $appUsageTimes")
            
            println("DEBUG: loadStateFromPreferences - Updated state: isBlocked=$isBlocked, trackedApps=$trackedAppNames, timeLimit=$timeLimitMinutes, usageTimes=$appUsageTimes")
            
            // If we're restoring a blocked state, check if user is currently in a tracked app
            if (blocked && apps.isNotEmpty()) {
                println("DEBUG: loadStateFromPreferences - Restoring blocked state, checking current app")
                
                // Wait a moment for the service to be fully initialized
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    val currentPkg = currentForegroundPackage
                    println("DEBUG: loadStateFromPreferences - Current foreground package: $currentPkg")
                    
                    if (isPackageTracked(currentPkg, apps)) {
                        println("DEBUG: loadStateFromPreferences - User in tracked app, showing Pause screen immediately")
                        val message = if (timeLimit > 0) {
                            "Take a mindful pause - you've reached your time limit of ${timeLimit} minutes"
                        } else {
                            "Take a mindful pause"
                        }
                        
                        try {
                            val intent = Intent(this@ForegroundAppAccessibilityService, PauseActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                putExtra("message", message)
                            }
                            startActivity(intent)
                            println("DEBUG: loadStateFromPreferences - Successfully launched Pause screen")
                        } catch (e: Exception) {
                            println("DEBUG: loadStateFromPreferences - Error launching Pause screen: ${e.message}")
                        }
                    } else {
                        println("DEBUG: loadStateFromPreferences - User not in tracked app, blocking will activate when they switch to one")
                    }
                }, 1000) // Wait 1 second for service initialization
            } else if (!blocked) {
                println("DEBUG: loadStateFromPreferences - No blocking state to restore")
            } else {
                println("DEBUG: loadStateFromPreferences - Blocked state but no tracked apps")
            }
        } catch (e: Exception) {
            println("DEBUG: loadStateFromPreferences - Error: ${e.message}")
        }
    }
}


