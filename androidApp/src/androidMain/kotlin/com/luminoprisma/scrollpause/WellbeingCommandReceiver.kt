package com.luminoprisma.scrollpause

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.ComponentName
import android.provider.Settings

class WellbeingCommandReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        println("DEBUG: WellbeingCommandReceiver.onReceive called - action: ${intent?.action}")
        val action = intent?.action ?: return
        when (action) {
            "com.luminoprisma.scrollpause.SHOW_PAUSE_SCREEN" -> {
                val message = intent.getStringExtra("message") ?: "Take a mindful pause"
                println("DEBUG: WellbeingCommandReceiver - SHOW_PAUSE_SCREEN received, message='$message'")
                ForegroundAppAccessibilityService.setPendingShow(message)
                // Always redirect to our pause screen (no more overlays)
                if (context != null) {
                    try {
                        println("DEBUG: WellbeingCommandReceiver - redirecting to PauseActivity")
                        val activityIntent = Intent(context, PauseActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            putExtra("message", message)
                        }
                        context.startActivity(activityIntent)
                    } catch (e: Exception) {
                        println("DEBUG: WellbeingCommandReceiver - redirect error: ${e.message}")
                    }
                }
            }
            "com.luminoprisma.scrollpause.HIDE_PAUSE_SCREEN" -> {
                println("DEBUG: WellbeingCommandReceiver - HIDE_PAUSE_SCREEN received")
                ForegroundAppAccessibilityService.setPendingHide()
                // Pause screen will handle its own dismissal
            }
            "com.luminoprisma.scrollpause.RESET_TIMER_AND_CONTINUE" -> {
                println("DEBUG: WellbeingCommandReceiver - RESET_TIMER_AND_CONTINUE received")
                // This will be handled by the main app's broadcast receiver
                // Just hide the pause screen for now
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
