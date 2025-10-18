package com.prismappsau.screengo

import android.accessibilityservice.AccessibilityService
import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.content.pm.PackageManager
// REMOVED: Overlay-related imports for policy compliance
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
        registerBlockedStateReceiver()
        // REMOVED: Overlay functionality for policy compliance
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
            
            // REMOVED: App blocking functionality for policy compliance
            // Now only tracks usage, doesn't block other apps
        }
    }

    override fun onInterrupt() {
    }
    
    // REMOVED: App blocking overlay functionality for policy compliance
    // Blocking now happens within the app itself

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
    // REMOVED: Overlay-related variables for policy compliance

    // REMOVED: Overlay receiver registration for policy compliance

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

    // REMOVED: System overlay functionality for policy compliance
    // Blocking now happens within the app itself, not as system overlays

    // REMOVED: System overlay hide functionality for policy compliance
}


