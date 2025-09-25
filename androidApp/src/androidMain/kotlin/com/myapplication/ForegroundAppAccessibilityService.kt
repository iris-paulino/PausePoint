package com.myapplication

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class ForegroundAppAccessibilityService : AccessibilityService() {
    
    companion object {
        private var pendingShowMessage: String? = null
        private var pendingHide = false
        
        fun setPendingShow(message: String) {
            pendingShowMessage = message
        }
        
        fun setPendingHide() {
            pendingHide = true
        }
        
        fun clearPendingShow(): String? {
            val message = pendingShowMessage
            pendingShowMessage = null
            return message
        }
        
        fun clearPendingHide(): Boolean {
            val hide = pendingHide
            pendingHide = false
            return hide
        }
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Handle accessibility events if needed
    }
    
    override fun onInterrupt() {
        // Handle service interruption
    }
}