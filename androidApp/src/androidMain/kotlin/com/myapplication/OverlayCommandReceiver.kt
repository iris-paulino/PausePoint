package com.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class OverlayCommandReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        println("DEBUG: OverlayCommandReceiver.onReceive called - action: ${intent?.action}")
        val action = intent?.action ?: return
        when (action) {
            "com.myapplication.SHOW_BLOCKING_OVERLAY" -> {
                val message = intent.getStringExtra("message") ?: "Take a mindful pause"
                println("DEBUG: OverlayCommandReceiver - SHOW received, message='$message'")
                ForegroundAppAccessibilityService.setPendingShow(message)
                // Fallback: launch blocking activity to ensure user sees overlay
                try {
                    if (context != null) {
                        println("DEBUG: OverlayCommandReceiver - launching PauseOverlayActivity")
                        val activityIntent = Intent(context, PauseOverlayActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            putExtra("message", message)
                        }
                        context.startActivity(activityIntent)
                        println("DEBUG: OverlayCommandReceiver - PauseOverlayActivity launched successfully")
                    } else {
                        println("DEBUG: OverlayCommandReceiver - context is null, cannot launch activity")
                    }
                } catch (e: Exception) {
                    println("DEBUG: OverlayCommandReceiver - error launching activity: ${e.message}")
                }
            }
            "com.myapplication.HIDE_BLOCKING_OVERLAY" -> {
                println("DEBUG: OverlayCommandReceiver - HIDE received")
                ForegroundAppAccessibilityService.setPendingHide()
                // If fallback activity is showing, ensure it closes
                // Activity registers a receiver to finish on this signal
            }
        }
    }
}


