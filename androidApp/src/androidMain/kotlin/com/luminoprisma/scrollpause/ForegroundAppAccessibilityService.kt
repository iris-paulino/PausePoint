package com.luminoprisma.scrollpause

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import android.content.SharedPreferences

class ForegroundAppAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile
        private var currentForegroundPackage: String? = null
        @Volatile private var pendingShowMessage: String? = null
        @Volatile private var pendingHide: Boolean = false
        @Volatile private var isBlocked: Boolean = false
        @Volatile private var trackedAppNames: List<String> = emptyList()
        @Volatile private var timeLimitMinutes: Int = 0
        
        fun getCurrentForegroundPackage(): String? = currentForegroundPackage
        
        fun setCurrentForegroundPackage(packageName: String?) {
            currentForegroundPackage = packageName
            println("DEBUG: ForegroundAppAccessibilityService - Set foreground app: $packageName")
        }
        
        fun isBlocked(): Boolean = isBlocked

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
                            val intent = Intent(ctx, PauseOverlayActivity::class.java).apply {
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
                val intent = Intent(this, PauseOverlayActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra("message", msg)
                }
                startActivity(intent)
            } catch (e: Exception) {
                println("DEBUG: onServiceConnected - error launching PauseOverlayActivity: ${e.message}")
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
            currentForegroundPackage = pkg
            println("DEBUG: ForegroundAppAccessibilityService - New foreground app: $pkg (event=${event?.eventType})")
            val intent = android.content.Intent("com.luminoprisma.scrollpause.FOREGROUND_APP_CHANGED").apply {
                putExtra("pkg", pkg)
                setPackage(applicationContext.packageName)
            }
            applicationContext.sendBroadcast(intent)
            
            // Check if this app should be blocked and redirect to pause screen
            checkAndRedirectToPauseApp(pkg)
        }
    }

    override fun onInterrupt() {
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
            println("DEBUG: checkAndRedirectToPauseApp - redirecting to PauseOverlayActivity for blocked tracked app: $packageName")
            val message = if (timeLimitMinutes > 0) {
                "Take a mindful pause - you've reached your time limit of ${timeLimitMinutes} minutes"
            } else {
                "Take a mindful pause"
            }
            try {
                // Redirect to our app's Pause screen instead of showing overlay
                val intent = Intent(this, PauseOverlayActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra("message", message)
                }
                startActivity(intent)
            } catch (e: Exception) {
                println("DEBUG: checkAndRedirectToPauseApp - error redirecting to PauseOverlayActivity: ${e.message}")
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
                        val activityIntent = Intent(this@ForegroundAppAccessibilityService, PauseOverlayActivity::class.java).apply {
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
                    // Reload state from SharedPreferences when state changes
                    loadStateFromPreferences()
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
            
            // Always update the state, even if it's the same
            isBlocked = blocked
            trackedAppNames = apps
            timeLimitMinutes = timeLimit
            
            println("DEBUG: loadStateFromPreferences - Updated state: isBlocked=$isBlocked, trackedApps=$trackedAppNames, timeLimit=$timeLimitMinutes")
            
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
                            val intent = Intent(this@ForegroundAppAccessibilityService, PauseOverlayActivity::class.java).apply {
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


