package com.myapplication

import android.accessibilityservice.AccessibilityService
import android.content.Intent
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
    }

    private fun isIgnoredPackage(pkg: String?): Boolean {
        if (pkg.isNullOrBlank()) return true
        val self = applicationContext.packageName
        return pkg == self ||
            pkg.startsWith("com.google.android.inputmethod") ||
            pkg.startsWith("com.android.inputmethod") ||
            pkg.startsWith("com.google.android.gms") ||
            pkg.startsWith("com.android.systemui")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        println("DEBUG: ForegroundAppAccessibilityService - service connected")
        registerOverlayReceiver()
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
                // Ignore this update to avoid overwriting with keyboard/system
                return
            }
        }
        // Only accept non-ignored packages
        if (!isIgnoredPackage(pkg)) {
            currentForegroundPackage = pkg
            println("DEBUG: ForegroundAppAccessibilityService - New foreground app: $pkg (event=${event?.eventType})")
            val intent = android.content.Intent("com.myapplication.FOREGROUND_APP_CHANGED").apply {
                putExtra("pkg", pkg)
                setPackage(applicationContext.packageName)
            }
            applicationContext.sendBroadcast(intent)
        }
    }

    override fun onInterrupt() {
    }

    // Overlay management
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var overlayShown: Boolean = false

    private fun registerOverlayReceiver() {
        try {
            val showFilter = IntentFilter("com.myapplication.SHOW_BLOCKING_OVERLAY")
            val hideFilter = IntentFilter("com.myapplication.HIDE_BLOCKING_OVERLAY")

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

    private fun showOverlay(message: String) {
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
                    val intent = Intent("com.myapplication.RESET_TIMER_AND_CONTINUE").apply {
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


