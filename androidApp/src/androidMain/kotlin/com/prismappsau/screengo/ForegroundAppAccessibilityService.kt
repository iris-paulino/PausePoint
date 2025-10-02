package com.prismappsau.screengo

import android.accessibilityservice.AccessibilityService
import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.content.pm.PackageManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.os.Build
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent

class ForegroundAppAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile
        private var currentForegroundPackage: String? = null
        @Volatile private var pendingShowMessage: String? = null
        @Volatile private var pendingHide: Boolean = false
        @Volatile private var isBlocked: Boolean = false
        @Volatile private var trackedAppNames: List<String> = emptyList()
        @Volatile private var timeLimitMinutes: Int = 0
        @Volatile private var trackedPackages: Set<String> = emptySet()
        @Volatile private var appContext: Context? = null
        
        fun getCurrentForegroundPackage(): String? = currentForegroundPackage
        
        fun setCurrentForegroundPackage(packageName: String?) {
            currentForegroundPackage = packageName
            println("DEBUG: ForegroundAppAccessibilityService - Set foreground app: $packageName")
        }

        fun setPendingShow(message: String) {
            pendingShowMessage = message
            println("DEBUG: PendingCommands - setPendingShow('$message')")
        }

        fun setPendingHide() {
            pendingHide = true
            println("DEBUG: PendingCommands - setPendingHide()")
        }
        
        fun setBlockedState(blocked: Boolean, trackedApps: List<String>, timeLimit: Int) {
            isBlocked = blocked
            trackedAppNames = trackedApps
            timeLimitMinutes = timeLimit
            // Resolve app names to installed package names for broad matching
            try {
                val ctx = appContext
                if (ctx != null) {
                    trackedPackages = resolveTrackedPackagesFromNames(ctx, trackedApps)
                }
            } catch (_: Exception) {
            }
            println("DEBUG: ForegroundAppAccessibilityService - Set blocked state: blocked=$blocked, apps=$trackedApps, resolvedPackages=${trackedPackages.size}, limit=$timeLimit")
        }

        private fun normalizeName(name: String): String {
            return name.lowercase()
                .replace(" ", "")
                .replace("-", "")
                .replace("_", "")
                .replace(".", "")
        }

        private fun resolveTrackedPackagesFromNames(context: Context, names: List<String>): Set<String> {
            return try {
                val pm = context.packageManager
                val launcherIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
                val activities = pm.queryIntentActivities(launcherIntent, 0)

                val normalizedTargets = names.mapNotNull { it?.trim() }
                    .filter { it.isNotEmpty() }
                    .map { normalizeName(it) }
                    .toSet()

                val matches = mutableSetOf<String>()
                for (ri in activities) {
                    val appInfo = ri.activityInfo?.applicationInfo ?: continue
                    val pkg = appInfo.packageName ?: continue
                    if (pkg == context.packageName) continue
                    val label = pm.getApplicationLabel(appInfo)?.toString() ?: continue
                    val normalizedLabel = normalizeName(label)

                    // Direct label contains check both ways
                    if (normalizedTargets.any { t ->
                            normalizedLabel.contains(t) || t.contains(normalizedLabel)
                        }) {
                        matches.add(pkg)
                        continue
                    }

                    // Also try comparing against package name segments (some users type package-ish names)
                    val normalizedPkg = normalizeName(pkg)
                    if (normalizedTargets.any { t ->
                            normalizedPkg.contains(t) || t.contains(normalizedPkg)
                        }) {
                        matches.add(pkg)
                    }
                }
                matches
            } catch (_: Exception) {
                emptySet()
            }
        }
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
        // Cache a context for static resolver use
        Companion.appContext = applicationContext
        registerOverlayReceiver()
        registerBlockedStateReceiver()
        // Apply any pending command
        val msg = pendingShowMessage
        if (msg != null) {
            println("DEBUG: onServiceConnected - applying pending SHOW")
            showOverlay(msg)
            pendingShowMessage = null
        }
        if (pendingHide) {
            println("DEBUG: onServiceConnected - applying pending HIDE")
            hideOverlay()
            pendingHide = false
        }
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        var pkg = event?.packageName?.toString()
        if (isIgnoredPackage(pkg)) {
            // Fallback: try active window root
            val root = rootInActiveWindow
            val rootPkg = root?.packageName?.toString()
            if (!isIgnoredPackage(rootPkg)) {
                pkg = rootPkg
            } else {
                // Final fallback: query recent UsageEvents/UsageStats for the latest foreground app
                pkg = getRecentForegroundPackageFromUsageStats()
                if (pkg.isNullOrBlank() || isIgnoredPackage(pkg)) {
                    // Ignore this update to avoid overwriting with keyboard/system
                    return
                }
            }
        }
        // Only accept non-ignored packages
        if (!isIgnoredPackage(pkg)) {
            currentForegroundPackage = pkg
            println("DEBUG: ForegroundAppAccessibilityService - New foreground app: $pkg (event=${event?.eventType})")
            val intent = android.content.Intent("com.prismappsau.screengo.FOREGROUND_APP_CHANGED").apply {
                putExtra("pkg", pkg)
                setPackage(applicationContext.packageName)
            }
            applicationContext.sendBroadcast(intent)
            
            // Check if this app should be blocked and show overlay immediately
            checkAndShowOverlayForApp(pkg)
        }
    }

    override fun onInterrupt() {
    }
    
    private fun checkAndShowOverlayForApp(packageName: String?) {
        if (!isBlocked || packageName.isNullOrBlank()) return
        
        println("DEBUG: checkAndShowOverlayForApp - checking package: $packageName, blocked: $isBlocked")
        
        // Check if the current foreground app's package is among resolved tracked packages
        val isTrackedApp = trackedPackages.contains(packageName)
        
        println("DEBUG: checkAndShowOverlayForApp - isTrackedApp: $isTrackedApp for package: $packageName")
        
        if (isTrackedApp) {
            // User is trying to use a tracked app while blocked, show overlay immediately
            println("DEBUG: checkAndShowOverlayForApp - showing overlay for blocked tracked app: $packageName")
            val message = "Take a mindful pause - you've reached your time limit of ${timeLimitMinutes} minutes"
            showOverlay(message)
        }
    }

    private fun hasUsageAccessPermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.checkOpNoThrow(
                "android:get_usage_stats",
                android.os.Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    private fun getRecentForegroundPackageFromUsageStats(): String? {
        return try {
            if (!hasUsageAccessPermission(applicationContext)) return null
            val usm = applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val end = System.currentTimeMillis()
            val start = end - 60_000 // look back 60s
            val events: UsageEvents = usm.queryEvents(start, end)
            var lastPkg: String? = null
            val evt = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(evt)
                if (evt.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                    evt.eventType == UsageEvents.Event.ACTIVITY_PAUSED ||
                    evt.eventType == UsageEvents.Event.ACTIVITY_STOPPED ||
                    evt.eventType == UsageEvents.Event.ACTIVITY_STOPPED) {
                    lastPkg = evt.packageName
                }
                if (evt.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    // Prefer the most recent ACTIVITY_RESUMED
                    lastPkg = evt.packageName
                }
            }
            lastPkg
        } catch (_: Exception) {
            null
        }
    }

    // (Helpers moved into companion object; single companion enforced)

    // Overlay management
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var overlayShown: Boolean = false
    private var lastOverlayShowTimeMs: Long = 0L
    private val overlayDebounceMs: Long = 1500L

    private fun registerOverlayReceiver() {
        try {
            val showFilter = IntentFilter("com.prismappsau.screengo.SHOW_BLOCKING_OVERLAY")
            val hideFilter = IntentFilter("com.prismappsau.screengo.HIDE_BLOCKING_OVERLAY")

            println("DEBUG: registerOverlayReceiver - registering receivers")
            registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    println("DEBUG: receiver SHOW_BLOCKING_OVERLAY received - intent=$intent")
                    val message = intent?.getStringExtra("message") ?: "Take a mindful pause"
                    showOverlay(message)
                }
            }, showFilter)

            registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    println("DEBUG: receiver HIDE_BLOCKING_OVERLAY received - intent=$intent")
                    hideOverlay()
                }
            }, hideFilter)
        } catch (_: Exception) {
        }
    }

    private fun registerBlockedStateReceiver() {
        try {
            val filter = IntentFilter("com.prismappsau.screengo.APPLY_BLOCKED_STATE")
            registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val blocked = intent?.getBooleanExtra("blocked", false) ?: false
                    val apps = intent?.getStringArrayListExtra("apps") ?: arrayListOf()
                    val limit = intent?.getIntExtra("timeLimitMinutes", 0) ?: 0
                    println("DEBUG: BlockedStateReceiver - applying blocked=$blocked apps=$apps limit=$limit")
                    setBlockedState(blocked, apps, limit)
                }
            }, filter)
        } catch (_: Exception) {
        }
    }

    private fun showOverlay(message: String) {
        // Grace window after QR success: skip overlay if within cooldown
        try {
            val prefs = applicationContext.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
            val until = prefs.getLong("qr_success_cooldown_until", 0L)
            if (System.currentTimeMillis() < until) {
                println("DEBUG: showOverlay - QR cooldown active, skipping overlay show")
                return
            }
        } catch (_: Exception) {}
        // Debounce to avoid rapid re-show churn
        val now = System.currentTimeMillis()
        if (now - lastOverlayShowTimeMs < overlayDebounceMs) {
            println("DEBUG: showOverlay - debounced, skipping overlay show")
            return
        }
        if (overlayShown) { println("DEBUG: showOverlay - already shown"); return }
        try {
            val wm = windowManager ?: getSystemService(WINDOW_SERVICE) as WindowManager
            windowManager = wm

            val overlayType = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                overlayType,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                android.graphics.PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
            }

            val container = FrameLayout(this)
            container.setBackgroundColor(0xCC000000.toInt())
            val padding = (24 * resources.displayMetrics.density).toInt()
            container.setPadding(padding, padding * 3, padding, padding)
            container.isClickable = true
            container.isFocusable = true

            val title = TextView(this).apply {
                text = "Pause reached"
                textSize = 22f
                setTextColor(android.graphics.Color.WHITE)
            }
            val body = TextView(this).apply {
                text = message
                textSize = 16f
                setTextColor(android.graphics.Color.WHITE)
            }
            val scanBtn = Button(this).apply {
                text = "Scan QR to continue"
                setOnClickListener {
                    try {
                        val intent = Intent(this@ForegroundAppAccessibilityService, PauseOverlayActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            putExtra("message", message)
                        }
                        startActivity(intent)
                        // Hide the system overlay since we're showing the activity
                        hideOverlay()
                    } catch (e: Exception) {
                        println("DEBUG: showOverlay - error launching PauseOverlayActivity: ${e.message}")
                    }
                }
            }
            val dismissBtn = Button(this).apply {
                text = "Dismiss"
                setOnClickListener { 
                    // Send dismiss broadcast to trigger the dismiss callback
                    val intent = Intent("com.prismappsau.screengo.RESET_TIMER_AND_CONTINUE").apply {
                        setPackage(applicationContext.packageName)
                    }
                    applicationContext.sendBroadcast(intent)
                    hideOverlay()
                }
            }

            val layout = FrameLayout(this).apply {
                addView(title)
                addView(body)
                addView(scanBtn)
                addView(dismissBtn)
            }
            container.addView(layout)

            println("DEBUG: showOverlay - adding view to window manager")
            wm.addView(container, params)
            overlayView = container
            overlayShown = true
            lastOverlayShowTimeMs = now
            println("DEBUG: showOverlay - overlay shown")
        } catch (_: Exception) {
            println("DEBUG: showOverlay - error showing overlay")
        }
    }

    private fun hideOverlay() {
        try {
            val wm = windowManager
            val view = overlayView
            if (wm != null && view != null) {
                println("DEBUG: hideOverlay - removing view")
                wm.removeView(view)
            }
        } catch (_: Exception) {
        } finally {
            overlayView = null
            overlayShown = false
            println("DEBUG: hideOverlay - overlay hidden")
        }
    }
}


