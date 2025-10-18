package com.prismappsau.screengo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class OverlayCommandReceiver : BroadcastReceiver() {
    companion object {
        @Volatile private var lastLaunchMs: Long = 0L
        private const val launchDebounceMs: Long = 1500L
    }
    override fun onReceive(context: Context?, intent: Intent?) {
        println("DEBUG: OverlayCommandReceiver.onReceive called - action: ${intent?.action}")
        val action = intent?.action ?: return
        when (action) {
            "com.prismappsau.screengo.SHOW_BLOCKING_OVERLAY" -> {
                val message = intent.getStringExtra("message") ?: "Take a mindful pause"
                println("DEBUG: OverlayCommandReceiver - SHOW received, message='$message'")
                ForegroundAppAccessibilityService.setPendingShow(message)
                // Fallback: gently launch PauseActivity with debounce if service overlay doesn't appear
                val now = System.currentTimeMillis()
                if (now - lastLaunchMs >= launchDebounceMs && context != null) {
                    try {
                        val activityIntent = Intent(context, PauseActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            putExtra("message", message)
                        }
                        context.startActivity(activityIntent)
                        lastLaunchMs = now
                        println("DEBUG: OverlayCommandReceiver - PauseActivity launched (debounced)")
                    } catch (e: Exception) {
                        println("DEBUG: OverlayCommandReceiver - error launching activity: ${e.message}")
                    }
                }
            }
            "com.prismappsau.screengo.HIDE_BLOCKING_OVERLAY" -> {
                println("DEBUG: OverlayCommandReceiver - HIDE received")
                ForegroundAppAccessibilityService.setPendingHide()
                // If fallback activity is showing, ensure it closes
                // Activity registers a receiver to finish on this signal
            }
            "com.prismappsau.screengo.RESET_TIMER_AND_CONTINUE" -> {
                println("DEBUG: OverlayCommandReceiver - RESET_TIMER_AND_CONTINUE received")
                // This will be handled by the main app's broadcast receiver
                // Just hide the overlay for now
                ForegroundAppAccessibilityService.setPendingHide()
            }
        }
    }
}


