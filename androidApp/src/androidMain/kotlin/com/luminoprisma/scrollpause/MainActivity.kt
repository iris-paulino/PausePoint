package com.luminoprisma.scrollpause

import MainView
import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import initializeAppStorage
import initializeInstalledAppsProvider
import registerCurrentActivity

class MainActivity : AppCompatActivity() {
    private var isDismissing = false // Flag to prevent camera permission requests during dismiss
    
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isDismissing) {
            println("DEBUG: MainActivity - camera permission result received but app is dismissing, ignoring")
            return@registerForActivityResult
        }
        // Permission result is handled by the QR scanner itself
        // This launcher is just to ensure we have the permission before scanning
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize the app storage with the activity context
        initializeAppStorage(this)
        
        // Initialize the installed apps provider with the activity context
        initializeInstalledAppsProvider(this)

        registerCurrentActivity(this)

        setContent {
            MainView()
        }
    }
    
    fun requestCameraPermissionIfNeeded(): Boolean {
        if (isDismissing) {
            println("DEBUG: MainActivity - requestCameraPermissionIfNeeded called but app is dismissing, skipping")
            return false
        }
        return when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                true
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                false
            }
        }
    }
    
    fun setDismissingState(dismissing: Boolean) {
        isDismissing = dismissing
        println("DEBUG: MainActivity - setDismissingState: $dismissing")
    }
    
    fun resetDismissingStateIfNeeded() {
        if (isDismissing) {
            isDismissing = false
            println("DEBUG: MainActivity - resetDismissingStateIfNeeded: reset dismissing state to false")
        }
    }

    // Accessibility & Overlay helpers
    fun isAccessibilityServiceEnabled(): Boolean {
        return try {
            val expected = ComponentName(this, ForegroundAppAccessibilityService::class.java)
            val enabledServices = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            enabledServices.split(":").any { it.equals(expected.flattenToString(), ignoreCase = true) }
        } catch (_: Exception) {
            false
        }
    }

    fun openAccessibilitySettings() {
        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
        }
    }

    fun showBlockingOverlay(message: String) {
        val intent = Intent("com.luminoprisma.scrollpause.SHOW_BLOCKING_OVERLAY").apply {
            setPackage(packageName)
            putExtra("message", message)
        }
        sendBroadcast(intent)
    }

    fun hideBlockingOverlay() {
        val intent = Intent("com.luminoprisma.scrollpause.HIDE_BLOCKING_OVERLAY").apply {
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

}