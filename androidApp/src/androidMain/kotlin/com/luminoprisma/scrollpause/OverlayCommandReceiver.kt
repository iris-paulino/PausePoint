package com.luminoprisma.scrollpause

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.ComponentName
import android.provider.Settings

class OverlayCommandReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        println("DEBUG: OverlayCommandReceiver.onReceive called - action: ${intent?.action}")
        val action = intent?.action ?: return
        when (action) {
            "com.luminoprisma.scrollpause.SHOW_BLOCKING_OVERLAY" -> {
                val message = intent.getStringExtra("message") ?: "Take a mindful pause"
                println("DEBUG: OverlayCommandReceiver - SHOW received, message='$message'")
                ForegroundAppAccessibilityService.setPendingShow(message)
                // Only if accessibility service is NOT enabled, fall back to activity overlay
                if (context != null && !isAccessibilityServiceEnabled(context)) {
                    try {
                        println("DEBUG: OverlayCommandReceiver - accessibility disabled, launching PauseOverlayActivity fallback")
                        val activityIntent = Intent(context, PauseOverlayActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            putExtra("message", message)
                        }
                        context.startActivity(activityIntent)
                    } catch (e: Exception) {
                        println("DEBUG: OverlayCommandReceiver - fallback launch error: ${e.message}")
                    }
                } else {
                    println("DEBUG: OverlayCommandReceiver - accessibility enabled, relying on TYPE_ACCESSIBILITY_OVERLAY")
                }
            }
            "com.luminoprisma.scrollpause.HIDE_BLOCKING_OVERLAY" -> {
                println("DEBUG: OverlayCommandReceiver - HIDE received")
                ForegroundAppAccessibilityService.setPendingHide()
                // If fallback activity is showing, ensure it closes
                // Activity registers a receiver to finish on this signal
            }
            "com.luminoprisma.scrollpause.RESET_TIMER_AND_CONTINUE" -> {
                println("DEBUG: OverlayCommandReceiver - RESET_TIMER_AND_CONTINUE received")
                // This will be handled by the main app's broadcast receiver
                // Just hide the overlay for now
                ForegroundAppAccessibilityService.setPendingHide()
            }
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        return try {
            val expected = ComponentName(context, ForegroundAppAccessibilityService::class.java)
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            enabled.split(":").any { it.equals(expected.flattenToString(), ignoreCase = true) }
        } catch (_: Exception) {
            false
        }
    }
}


